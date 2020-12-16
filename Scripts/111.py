# -*- coding: utf-8 -*-
import re

import urllib
import urllib2

url = 'http://localhost:12345/some-url'

filters = []
filters.append('APC Web/SNMP Management Card')
filters.append('Siemens')
filters.append('Phone')
filters.append('AXIS')
filters.append('armv5tejl')
filters.append('Camera')
filters.append('DMC\-')
filters.append('Windows')
filters.append('MULTI\-ENVIRONMENT')
filters.append('SAS')
filters.append('Storage')
filters.append('UPS')
filters.append('APS')
filters.append('APC')
filters.append('Integrated Lights-Out')
filters.append('Printing')
filters.append('Powerware')
filters.append('PowerAP')
filters.append('VMware')


filters1 = []
filters1.append('port-001')

###########################################################
def post_request(url, data):
  req = urllib2.Request(url, data)
  response = urllib2.urlopen(req)
  the_page = response.read()
  return the_page

###########################################################

data = "<request><command>GETLIST</command><param>nodes</param></request>"
response = post_request(url, data)

result = re.finditer(ur"<node>(.*)</node>",response)
nodes = []
for node in result:
  print node

