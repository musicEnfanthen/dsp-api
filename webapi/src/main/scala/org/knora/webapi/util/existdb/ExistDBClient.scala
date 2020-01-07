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

import scala.util.{Failure, Success, Try}

class ExistDBClient(host: String,
                    port: Int,
                    username: String,
                    password: String,
                    queryTimeoutMillis: Int = 360000,
                    updateTimeoutMillis: Int = 360000) {

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
    // Get the command-line options.

    val conf = new ExistDBClientConf(args)

    // Construct the client.

    val client = new ExistDBClient(
        host = "localhost",
        port = 8080,
        username = "admin",
        password = ""
    )

    // Create Lucene indexes.

    val luceneConfig: String =
        """<collection xmlns="http://exist-db.org/collection-config/1.0">
          |    <index xmlns:wiki="http://exist-db.org/xquery/wiki" xmlns:html="http://www.w3.org/1999/xhtml" xmlns:atom="http://www.w3.org/2005/Atom">
          |        <!-- Disable the old full text index -->
          |        <fulltext default="none" attributes="false"/>
          |	<!-- Lucene index is configured below -->
          |        <lucene>
          |	        <analyzer class="org.apache.lucene.analysis.standard.StandardAnalyzer"/>
          |	        <text qname="noun"/>
          |         <text qname="verb"/>
          |         <text qname="adj"/>
          |        </lucene>
          |    </index>
          |</collection>""".stripMargin

    client.updateFile(fileContent = luceneConfig, filePath = "system/config/db/books/collection.xconf")

    println("Created Lucene indexes.")

    // Get the list of files to upload.

    val dir = new File(conf.dir())

    val files = dir.listFiles.filter {
        file =>
            val fileName = file.getName
            fileName.endsWith(".xml") && !file.getName.startsWith(".")
    }

    // Upload the files.

    for (file <- files) {
        val fileName = file.getName
        val bookName = fileName.substring(0, fileName.lastIndexOf('.'))
        val fileContent = FileUtil.readTextFile(file)
        val filePath = s"books/$bookName"

        val startTime = System.currentTimeMillis()
        val updateTry: Try[String] = client.updateFile(fileContent = fileContent, filePath = filePath)
        val endTime = System.currentTimeMillis()

        updateTry match {
            case Success(_) =>
                println(s"Uploaded $bookName (${file.length()} bytes, ${endTime - startTime} ms)")

            case Failure(exception) =>
                println(s"Failed to upload $bookName: $exception")
        }
    }

    /**
     * Parses command-line arguments.
     */
    class ExistDBClientConf(arguments: Seq[String]) extends ScallopConf(arguments) {
        banner(
            s"""
               |Uploads a file to eXist-db.
               |
               |Usage: org.knora.webapi.util.existdb.ExistDBClient dir
            """.stripMargin)

        val dir: ScallopOption[String] = trailArg[String](required = true, descr = "The directory containing files to be uploaded")
        verify()
    }
}
