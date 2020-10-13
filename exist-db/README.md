# eXist-db

This folder holds a proof of concept for an interaction between Knora and eXist-db [DSP-742](https://dasch.myjetbrains.com/youtrack/issue/DSP-742).




## Setup

### eXist-db

Be sure that you have exist-db running:
* Either download as described [here](https://www.exist-db.org/exist/apps/doc/basic-installation).  
Note, however: Installing it from `.jar`, as stated in the description, doesn't seem to work. The `.dmg` on the other hand works perfectly fine.
* Otherwise you can run it in docker:  
First time, call `docker pull existdb/existdb:release`  
Then simply run `docker run --rm -it  -p 8080:8080/tcp -p 8443:8443/tcp existdb/existdb:release`


### eXist Apps

#### knora-exist

Once eXist-db is running, you have to install the `knora-exist` app in eXist.  
(Note: If you have eXist running in docker, you'll need to to this each time you run it.)  
Open `localhost:/8080` in your browser. Log in with `user="admin"`, `password=""`, then click "Package manager" on the left. Install `knora-exist-0.1.xar` (in this folder) with "upload"-button or drag'n'drop.

To check if eXist-db and knora-exist are up and running, open `http://localhost:8080/exist/apps/knora-exist/modules/test.xql` in your browser.  
This may ask you for credentials: Again `user="admin"` and `password=""`.

Note: If anything in the app is changed, it must be exported to a new .xar again.  
(There should be a better workflow for this, but for now it should do.)

#### exist-jwt

Furthermore, you need `exist-jwt` installed. The `.xar` can be downloaded [here](https://github.com/eXistSolutions/exist-jwt).

TODO: more info?


## Requirements

To run the client and knora-imitation, you need the following python packages installed:

* requests (`pip3 install requests`)

TODO: more

## How to use

For now, when everything is set up, just run `client.py` (e.g. `python3 client.py` or in the IDE of your choice).

* It will first check if everything is set up correctly.
* then it will upload the `sample.xml` tile to `db/apps/knora-exist/data/tmp/sample.xml` (open `http://localhost:8080/exist/apps/knora-exist/data/tmp/sample.xml` in your browser).

TODO: more

(eventually, you'll need `knora.py running`)


## Development

Simple changes can be made in eXist's built-in IDE "eXide". This is not very practical however.

Accessing the database with Finer's WebDAV works quite well. (At times I couldn't access the uploaded XMLs. A restart helps.) When opening the WebDAV folder in VS Code with the exist-extension installed works really well to work on the XQuery right in the database.

Oxygen XML has good WebDAV support too.


## Exporting the App to .xar

eXist-db doesn't really work well with git. I therefor chose to simply have the .xar in the repo.

> Note: .xar is essentially a .zip archive of the app. It can be un-zipped by simply changing the file extension to `.zip` and you should see all files in the archive.

After every change in the app, the .xar should be generated and pushed to the repo. To generate the .xar, open eXide and in there, open a file that is within the app (e.g. an XQuery script) - for file navigation, use the bar on the left.  
Then, once a file from the app is selected, in the menu on top, select "application" > "Download app".

