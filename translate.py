#!/usr/bin/python

import os
import optparse
import urllib2
import json
import base64

parser = optparse.OptionParser()
parser.add_option("-u", "--user", dest="username", help="transifex user login")
parser.add_option("-p", "--password", dest="password", help="transifex user password")

(options, args) = parser.parse_args()

if not options.username or not options.password:
    parser.error('User name and password are required')

os.chdir(os.path.dirname(os.path.abspath(__file__)))

path = "./app/src/main/res/"

def request(url):
    req = urllib2.Request(url)
    auth = base64.encodestring("%s:%s" % (options.username, options.password)).replace("\n", "")
    req.add_header("Authorization", "Basic %s" % auth)
    return urllib2.urlopen(req)

resource = json.load(request("https://www.transifex.com/api/2/project/traccar/resource/client/?details"))

for language in resource["available_languages"]:
    code = language["code"]
    data = request("https://www.transifex.com/api/2/project/traccar/resource/client/translation/" + code + "?file")
    if code == "en":
        filename = path + "values/strings.xml"
    else:
        filename = path + "values-" + code.replace("_", "-r") + "/strings.xml"
    if not os.path.exists(os.path.dirname(filename)):
        os.makedirs(os.path.dirname(filename))
    file = open(filename, "wb")
    file.write(data.read())
    file.close()
