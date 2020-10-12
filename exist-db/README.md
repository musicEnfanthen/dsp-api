# eXist-db

This folder holds a proof of concept for an interaction between Knora and eXist-db [DSP-742](https://dasch.myjetbrains.com/youtrack/issue/DSP-742).




## Setup

Be sure that you have exist-db running:
* Either download as described [here](https://www.exist-db.org/exist/apps/doc/basic-installation).  
Note, however: Installing it from `.jar`, as stated in the description, doesn't seem to work. The `.dmg` on the other hand works perfectly fine.
* Otherwise you can run it in docker:  
First time, call `docker pull existdb/existdb:release`  
Then simply run `docker run --rm -it  -p 8080:8080/tcp -p 8443:8443/tcp existdb/existdb:release`

Once eXist-db is running, you have to install the `knora-exist` app in eXist.  
(Note: If you have eXist running in docker, you'll need to to this each time you run it.)  
Open `localhost:/8080` in your browser. Log in with `user="admin"`, `password=""`, then click "Package manager" on the left. Install `knora-exist-0.1.xar` (in this folder) with "upload"-button or drag'n'drop.

To check if eXist-db and knora-exist are up and running, open `http://localhost:8080/exist/apps/knora-exist/modules/test.xql` in your browser.  
This may ask you for credentials: Again `user="admin"` and `password=""`.

Note: If anything in the app is changed, it must be exported to a new .xar again.  
(There should be a better workflow for this, but for now it should do.)




## Requirements

* requests (`pip3 install requests`)
