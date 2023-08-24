#!/usr/bin/env python3

import os
import optparse
import requests
import shutil
from transifex.api import transifex_api

parser = optparse.OptionParser()
parser.add_option("-t", "--token", dest="token", help="transifex token")

(options, args) = parser.parse_args()

if not options.token:
    parser.error('Token is required')

os.chdir(os.path.dirname(os.path.abspath(__file__)))

path = "./app/src/main/res/"

transifex_api.setup(auth=options.token)

organization = transifex_api.Organization.get(slug='traccar')
project = organization.fetch('projects').get(slug='traccar')
resource = project.fetch('resources').get(slug='client')
languages = project.fetch('languages')

for language in languages:
    print(language.code)
    url = transifex_api.ResourceTranslationsAsyncDownload.download(resource=resource, language=language)
    result = requests.get(url)
    if language.code == "en":
        filename = path + "values/strings.xml"
    else:
        filename = path + "values-" + language.code.replace("_", "-r") + "/strings.xml"
    if not os.path.exists(os.path.dirname(filename)):
        os.makedirs(os.path.dirname(filename))
    with open(filename, "w") as file:
        file.write(result.text)

filename = path + "values-iw/strings.xml"
if not os.path.exists(os.path.dirname(filename)):
    os.makedirs(os.path.dirname(filename))
shutil.copyfile(path + "values-he/strings.xml", filename)
