package org.knora.webapi.util.existdb

import java.io.File

import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.AuthCache
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost, HttpPut}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.{BasicAuthCache, BasicCredentialsProvider, CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.knora.webapi.util.FileUtil
import org.knora.webapi.{ExistDBConnectionException, ExistDBResponseException, TriplestoreResponseException}
import org.rogach.scallop.{ScallopConf, ScallopOption}

import scala.util.Try

class ExistDBClient(host: String,
                    port: Int,
                    username: String,
                    password: String,
                    queryTimeoutMillis: Int = 10000,
                    updateTimeoutMillis: Int = 10000) {

    private val mimeTypeApplicationXml = "application/xml"

    private val targetHost: HttpHost = new HttpHost(host, port, "http")

    private val credsProvider: BasicCredentialsProvider = new BasicCredentialsProvider
    credsProvider.setCredentials(new AuthScope(targetHost.getHostName, targetHost.getPort), new UsernamePasswordCredentials(username, password))

    private val queryRequestConfig = RequestConfig.custom()
        .setConnectTimeout(queryTimeoutMillis)
        .setConnectionRequestTimeout(queryTimeoutMillis)
        .setSocketTimeout(queryTimeoutMillis)
        .build

    private val queryHttpClient: CloseableHttpClient = HttpClients.custom
        .setDefaultCredentialsProvider(credsProvider)
        .setDefaultRequestConfig(queryRequestConfig)
        .build

    private val updateTimeoutConfig = RequestConfig.custom()
        .setConnectTimeout(updateTimeoutMillis)
        .setConnectionRequestTimeout(updateTimeoutMillis)
        .setSocketTimeout(updateTimeoutMillis)
        .build

    private val updateHttpClient: CloseableHttpClient = HttpClients.custom
        .setDefaultCredentialsProvider(credsProvider)
        .setDefaultRequestConfig(updateTimeoutConfig)
        .build

    def updateFile(fileContent: String, filePath: String): Try[String] = {
        val authCache: AuthCache = new BasicAuthCache
        val basicAuth: BasicScheme = new BasicScheme
        authCache.put(targetHost, basicAuth)

        val httpContext: HttpClientContext = HttpClientContext.create
        httpContext.setCredentialsProvider(credsProvider)
        httpContext.setAuthCache(authCache)

        val requestEntity = new StringEntity(fileContent, ContentType.create(mimeTypeApplicationXml, "UTF-8"))
        val updateHttpPut = new HttpPut(s"/exist/rest/db/$filePath")
        updateHttpPut.setEntity(requestEntity)

        var maybeResponse: Option[CloseableHttpResponse] = None

        val existDBResponseTry = Try {
            try {
                maybeResponse = Some(updateHttpClient.execute(targetHost, updateHttpPut, httpContext))

                val responseEntityStr: String = Option(maybeResponse.get.getEntity) match {
                    case Some(responseEntity) => EntityUtils.toString(responseEntity)
                    case None => ""
                }

                val statusCode: Int = maybeResponse.get.getStatusLine.getStatusCode
                val statusCategory: Int = statusCode / 100

                if (statusCategory != 2) {
                    throw ExistDBResponseException(s"Triplestore responded with HTTP code $statusCode: $responseEntityStr")
                }

                responseEntityStr
            } finally {
                maybeResponse.foreach(_.close)
            }
        }

        existDBResponseTry.recover {
            case tre: TriplestoreResponseException => throw tre

            case e: Exception =>
                e.printStackTrace()
                throw ExistDBConnectionException(s"Failed to connect to eXist-db", Some(e))
        }
    }
}

/**
 * A command-line program for testing the eXist-db client.
 */
object ExistDBClient extends App {
    val conf = new ExistDBClientConf(args)
    val fileContent = FileUtil.readTextFile(new File(conf.file()))
    val filePath = conf.path()

    val client = new ExistDBClient(
        host = "localhost",
        port = 8080,
        username = "admin",
        password = ""
    )

    println(client.updateFile(fileContent = fileContent, filePath = filePath))

    /**
     * Parses command-line arguments.
     */
    class ExistDBClientConf(arguments: Seq[String]) extends ScallopConf(arguments) {
        banner(
            s"""
               |Uploads a file to eXist-db.
               |
               |Usage: org.knora.webapi.util.existdb.ExistDBClient file path
            """.stripMargin)

        val file: ScallopOption[String] = trailArg[String](required = true, descr = "The file to be uploaded")
        val path: ScallopOption[String] = trailArg[String](required = true, descr = "The path in which the file is to be stored")
        verify()
    }
}
