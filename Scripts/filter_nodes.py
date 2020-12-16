# -*- coding: utf-8 -*-
import re
import time
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
filters.append('Communicator')
filters.append('EX 3000')
filters.append('EX 2200')
filters.append('Cisco ATA 190')
filters.append('Cisco IP Phone')

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
  nodes.append(node.group(1))

for node in nodes:
#  print node.split(';')[0]
  is_deleted_node=False
  mas = node.split(";")
  if not re.match(r"^\s*\([\da-f]{2}(?:[-:][\da-f]{2}){5}\)$", mas[0]) and not re.match(r"^not\s+advertised\([\da-f]{2}(?:[-:][\da-f]{2}){5}\)$", mas[0]) and mas[3] != "" and not re.match(r"^\s*[\da-f]{2}(?:[-:][\da-f]{2}){5}$", mas[3]):
    found=False
    for filter in filters:
      p = re.compile('.*'+filter.lower()+'.*')
      if p.match(mas[3].split("|")[0].lower()):
        found=True
        break
    if found:
      is_deleted_node=True
#      print "Deleted node: "+node  
    else:
      for filter in filters1:
        p = re.compile('.*'+filter.lower()+'.*')
        if p.match(mas[10].lower()):
          found=True
          break
      if found:
        is_deleted_node=True
#        print "Deleted node: "+node  
  else:
     is_deleted_node=True
#     print "Deleted node: "+node  
  
  if(is_deleted_node):
    data = "<request><command>DELETE</command><node>"+node.split(';')[0]+"</node></request>"
    response = post_request(url, data)
#    print data

#  time.sleep(0.01)
