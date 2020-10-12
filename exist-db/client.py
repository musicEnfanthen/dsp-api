import os
import requests
import urllib.parse as parse
from lxml import etree

# variables
exist_server = 'http://localhost:8080/exist'
app_path = 'apps/knora-exist'
modules = 'modules'

# turn off proxy for localhost
os.environ['NO_PROXY'] = '127.0.0.1'

def run():
    """
    Run client requests
    """
    print('Client starting')
    print(f'eXist-db status: {db_status()}')
    print(f'App "knora-exist" status: {app_status()}')
    print('initiate upload')
    upload()

def upload():
    """
    Upload a pre-defined file
    """
    url = '/'.join((exist_server, app_path, modules, 'upload-file.xql'))
    # TODO: make this dynamic?
    file_name = 'exist-db/sample.xml'
    xml = etree.parse(file_name)
    # parsing, validation, etc. could happen here
    data = etree.tostring(xml)
    r = requests.post(url, params={'file': file_name}, data=data, auth=('admin', ''))
    # print result... sould not stay like this, make something reasonable with the result
    print(r.text)

def app_status():
    """
    Tests if the `knora-exist` App is up and running
    """
    url = '/'.join((exist_server, app_path, modules, 'test.xql'))
    try:
        r = requests.get(url)
        return r.status_code
    except Exception as e:
        return 'app not found'

def db_status():
    """
    Tests if exist-db is up and running.
    """
    stat_url = exist_server + '/status'
    # print(stat_url)
    try:
        r = requests.get(stat_url)
        return r.status_code
    except Exception as e:
        return 'Could not connect to database.'

if __name__ == "__main__":
    run()