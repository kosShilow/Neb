# -*- coding: utf-8 -*-
import re
import time
import urllib
import urllib2

url = 'http://localhost:12345/some-url'

###########################################################
def post_request(url, data):
  req = urllib2.Request(url, data)
  response = urllib2.urlopen(req)
  the_page = response.read()
  return the_page

###########################################################

data = "<request><command>REINDEX</command></request>"
response = post_request(url, data)
result = re.finditer(ur"<node>(.*)</node>",response)


