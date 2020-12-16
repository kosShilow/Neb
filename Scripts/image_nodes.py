# -*- coding: utf-8 -*-
import re
import time
import urllib
import urllib2

url = 'http://localhost:12345/some-url'

replaces = []
mas = ['cisco\s+AIR', 'image/Cisco/CS_AP.png'] 
replaces.append(mas)
mas = ['Cisco\s+IOS\s+Software\s+C1200', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['C1140', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['AIR-BR350', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['C1310', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['C1260', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['C1530', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['CAP\d+', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['AP\d+', 'image/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['Blade', 'image/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['CBS31X0', 'image/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['CIGESM', 'image/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['WS-CBS3020', 'image/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['WS-CBS3012', 'image/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['cisco\s+1720', 'image/Cisco/CS1720.png']
replaces.append(mas)
mas = ['C1841', 'image/Cisco/CS1800.png']
replaces.append(mas)
mas = ['C870', 'image/Cisco/CS1800.png']
replaces.append(mas)
mas = ['C1900', 'image/Cisco/CS1900.png']
replaces.append(mas)
mas = ['CISCO1921', 'image/Cisco/CS1900.png']
replaces.append(mas)
mas = ['C2600', 'image/Cisco/CS2600.png']
replaces.append(mas)
mas = ['Cisco\s+SG\s+200-26', 'image/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['Cisco\s+SF200-24', 'image/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['cisco\s+2651', 'image/Cisco/CS2600.png']
replaces.append(mas)
mas = ['cisco\s+AS2511-RJ', 'image/Cisco/AS5400.png']
replaces.append(mas)
mas = ['C2800', 'image/Cisco/CS2800.png']
replaces.append(mas)
mas = ['Cisco\s+2811', 'image/Cisco/CS2800.png']
replaces.append(mas)
mas = ['Cisco\s+2821', 'image/Cisco/CS2800.png']
replaces.append(mas)
mas = ['Cisco\s+2851', 'image/Cisco/CS2800.png']
replaces.append(mas)
mas = ['C2940', 'image/Cisco/CS2940.png']
replaces.append(mas)
mas = ['C2950', 'image/Cisco/CS2950.png']
replaces.append(mas)
mas = ['CISCO2951', 'image/Cisco/CS2950.png']
replaces.append(mas)
mas = ['CISCO2921', 'image/Cisco/CS2921.png']
replaces.append(mas)
mas = ['CISCO2911', 'image/Cisco/CS2921.png']
replaces.append(mas)
mas = ['C2911', 'image/Cisco/CS2921.png']
replaces.append(mas)
mas = ['C2960', 'image/Cisco/CS2960.png']
replaces.append(mas)
mas = ['Cisco\s+SF200-24P', 'image/Cisco/CS2960.png']
replaces.append(mas)
mas = ['C2970', 'image/Cisco/CS2960.png']
replaces.append(mas)
mas = ['WS-CE500-24PC', 'image/Cisco/CS2960.png']
replaces.append(mas)
mas = ['C3500', 'image/Cisco/CS3500.png']
replaces.append(mas)
mas = ['C3550', 'image/Cisco/CS3550.png']
replaces.append(mas)
mas = ['C3548', 'image/Cisco/CS3550.png']
replaces.append(mas)
mas = ['C3560', 'image/Cisco/CS3550.png']
replaces.append(mas)
mas = ['Cisco\s+3640', 'image/Cisco/CS3640.png']
replaces.append(mas)
mas = ['C3725', 'image/Cisco/CS3725.png']
replaces.append(mas)
mas = ['C3750', 'image/Cisco/CS3750.png']
replaces.append(mas)
mas = ['C3825', 'image/Cisco/CS3800.png']
replaces.append(mas)
mas = ['CS3850', 'image/Cisco/CS3800.png']
replaces.append(mas)
mas = ['C3900', 'image/Cisco/CS3900.png']
replaces.append(mas)
mas = ['CISCO39', 'image/Cisco/CS3900.png']
replaces.append(mas)
mas = ['Catalyst\s+4500', 'image/Cisco/CS4500.png']
replaces.append(mas)
mas = ['CS4503', 'image/Cisco/CS4503.png']
replaces.append(mas)
mas = ['CS4506', 'image/Cisco/CS6506.png']
replaces.append(mas)
mas = ['CS4507', 'image/Cisco/CS6506.png']
replaces.append(mas)
mas = ['CS6503', 'image/Cisco/CS6503.png']
replaces.append(mas)
mas = ['C6503', 'image/Cisco/CS6503.png']
replaces.append(mas)
mas = ['CS6506', 'image/Cisco/CS6506.png']
replaces.append(mas)
mas = ['Cisco6506', 'image/Cisco/CS6506.png']
replaces.append(mas)
mas = ['CS6509', 'image/Cisco/CS6509.png']
replaces.append(mas)
mas = ['Cisco6509', 'image/Cisco/CS6509.png']
replaces.append(mas)
mas = ['CS6513', 'image/Cisco/CS6513.png']
replaces.append(mas)
mas = ['CS800', 'image/Cisco/CS800.png']
replaces.append(mas)
mas = ['Cisco\s+881G', 'image/Cisco/CS800.png']
replaces.append(mas)
mas = ['Cisco\s+C881', 'image/Cisco/CS800.png']
replaces.append(mas)
mas = ['Cisco AS5400XM', 'image/Cisco/AS5400.png']
replaces.append(mas)
mas = ['ISR4331', 'image/Cisco/ISR4331.png']
replaces.append(mas)
mas = ['ASR1001-X', 'image/Cisco/ASR-1000.png']
replaces.append(mas)
mas = ['N5K-C5548UP', 'image/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['n5000', 'image/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['n4000', 'image/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['WS-C4948', 'image/Cisco/CS4948.png']
replaces.append(mas)
mas = ['AnywhereUSB', 'image/Cisco/CS4948.png']
replaces.append(mas)
mas = ['WLC', 'image/Cisco/WLC.png']
replaces.append(mas)
mas = ['Cisco\s+Controller', 'image/Cisco/WLC.png']
replaces.append(mas)
mas = ['Cisco\s+SF300-24MP', 'image/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['SG200-26', 'image/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['Cisco\s+IP\s+Conference\s+Station', 'image/Cisco/ip_conferenc.png']
replaces.append(mas)
mas = ['Cisco\s+CSS', 'image/Cisco/CSS.png']
replaces.append(mas)
mas = ['Cisco\s+SF\s+302-08', 'image/Cisco/SF-302-8.png']
replaces.append(mas)
mas = ['DES-', 'image/Dlink/3226S.png']
replaces.append(mas)
mas = ['DES-3526', 'image/Dlink/3526.png']
replaces.append(mas)
mas = ['DGS-', 'image/Dlink/DGS.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+2424', 'image/HP/2424.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+2524', 'image/HP/2424.png']
replaces.append(mas)
mas = ['HP\s+2524', 'image/HP/2424.png']
replaces.append(mas)
mas = ['2910al-24G', 'image/HP/2424.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+2512', 'image/HP/2512.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+1600M', 'image/HP/2512.png']
replaces.append(mas)
mas = ['2530-24', 'image/HP/2530.png']
replaces.append(mas)
mas = ['3400cl-24G', 'image/HP/2530.png']
replaces.append(mas)
mas = ['5308xl', 'image/HP/5308.png']
replaces.append(mas)
mas = ['Switch 5304XL', 'image/HP/5308.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+8000M', 'image/HP/8000.png']
replaces.append(mas)
mas = ['HP\s+8000M', 'image/HP/8000.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+4000M', 'image/HP/8000.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+4108GL', 'image/HP/8000.png']
replaces.append(mas)
mas = ['Switch\s+4104GL', 'image/HP/8000.png']
replaces.append(mas)
mas = ['ProCurve\s+j9020a\s+Switch\s+2510', 'image/HP/2510.png']
replaces.append(mas)
mas = ['HP\s+-\s+1910-8', 'image/HP/2510.png']
replaces.append(mas)
mas = ['HPE\s+V1910-24G', 'image/HP/2510.png']
replaces.append(mas)
mas = ['HP\s+V1910-24G', 'image/HP/2510.png']
replaces.append(mas)
mas = ['1920-24G', 'image/HP/2510.png']
replaces.append(mas)
mas = ['1920-16G', 'image/HP/2510.png']
replaces.append(mas)
mas = ['Switch\s+2650', 'image/HP/2510.png']
replaces.append(mas)
mas = ['Switch\s+2610', 'image/HP/2510.png']
replaces.append(mas)
mas = ['HP 2512', 'image/HP/2510.png']
replaces.append(mas)
mas = ['HP\s+2424M', 'image/HP/2424.png']
replaces.append(mas)
mas = ['HP\s+2626', 'image/HP/2424.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+2626', 'image/HP/2424.png']
replaces.append(mas)
mas = ['ProCurve\s+Switch\s+6108', 'image/HP/6108.png']
replaces.append(mas)
mas = ['ProCurve\s+J4903A\s+Switch\s+2824', 'image/HP/2424.png']
replaces.append(mas)
mas = ['ProCurve\s+J9019B\s+Switch\s+2510B-24', 'image/HP/2424.png']
replaces.append(mas)
mas = ['Switch\s+2848', 'image/HP/2848.png']
replaces.append(mas)
mas = ['HP\s+VC\s+Flex', 'image/HP/1910-8.png']
replaces.append(mas)
mas = ['J9029A', 'image/HP/2424.png']
replaces.append(mas)
mas = ['srx210', 'image/Juniper/SRX210.png']
replaces.append(mas)
mas = ['EDS-', 'image/MOXA/MOXA.png']
replaces.append(mas)
mas = ['MOXA', 'image/MOXA/MOXA.png']
replaces.append(mas)
mas = ['AWK4121', 'image/MOXA/MOXA.png']
replaces.append(mas)
mas = ['Nortel', 'image/Nortel/nortel.png']
replaces.append(mas)
mas = ['Palo\s+Alto\s+Networks', 'image/PaloAlto/3000.png']
replaces.append(mas)
mas = ['Linux\s+2\.6\.32', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['NanoStation M2', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['N2N', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['N5N', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['N2B', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['N5B', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['B2N', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['B2T', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['PB5', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['NB5', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['B5N', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['P5B', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['T07', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['Rocket\s+M5', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['NanoStation\s+M5', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['PowerBeam\s+M2', 'image/Ubnt/AP.png']
replaces.append(mas)
mas = ['ZyXEL', 'image/Zyxel/zyxel.png']
replaces.append(mas)
mas = ['Zyx-IES612', 'image/Zyxel/zyxel.png']
replaces.append(mas)
mas = ['ATI\s+8000S', 'image/AT/8000.png']
replaces.append(mas)
mas = ['AT-8000S', 'image/AT/8000.png']
replaces.append(mas)
mas = ['AT8000', 'image/AT/8000.png']
replaces.append(mas)
mas = ['MikroTik', 'image/AT/8000.png']
replaces.append(mas)
mas = ['AT-8516F', 'image/AT/8000.png']
replaces.append(mas)
mas = ['Allied\s+Telesyn', 'image/AT/8000.png']
replaces.append(mas)
mas = ['3Com\s+SuperStack\s+3', 'image/AT/8000.png']
replaces.append(mas)
mas = ['netbotz', 'image/APC/rackmonitor.png']
replaces.append(mas)
mas = ['Linux\s+rvbd', 'image/Riverbed/riverbed.png']
replaces.append(mas)
mas = ['TDMoIP', 'image/Riverbed/riverbed.png']
replaces.append(mas)
mas = ['SHDSL', 'image/DSL.png']
replaces.append(mas)
mas = ['ISCOM2118-MA', 'image/RiseCom/risecom.png']
replaces.append(mas)
mas = ['R2812', 'image/RiseCom/risecom.png']
replaces.append(mas)
mas = ['IBM\s+Flex\s+System', 'image/IBM/IBM_Flex.png']
replaces.append(mas)
mas = ['Lenovo\s+RackSwitch', 'image/Lenovo/G8264.png ']
replaces.append(mas)
mas = ['IBM\s+Networking\s+Operating\s+System\s+RackSwitch', 'image/IBM/IBM_Flex.png']
replaces.append(mas)


replaces1 = []
mas1 = ['br\d', 'image/AP.png'] 
replaces1.append(mas1)


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
  mas = node.split(";")
#  print mas[0]
  image=''
  for m in replaces:
    if len(mas) > 7:
      p = re.compile('^hub_.*')
      if p.match(mas[0]):
        image='image/hub.png'
        break

      p = re.compile('.*'+m[0].lower()+'.*')
      if p.match(mas[3].split("|")[0].lower()) or p.match(mas[6].lower()) or p.match(mas[7].lower()):
        image=m[1]
        break
  if image != '':
    data = "<request><command>UPDATE</command><node>"+mas[0]+"</node><image>"+image+"</image></request>"
    response = post_request(url, data)

# if iface brN image AP
for node in nodes:
  mas = node.split(";")
  if len(mas) > 11:
    image=''
    for m in replaces1:
      for iface in mas[10].split("|"):
        p = re.compile('.*'+m[0].lower()+'.*')
        if len(iface.split(",")) > 1 and p.match(iface.split(",")[1]):
          image=m[1]
#          print mas[0]+" - "+iface.split(",")[1]+" "+image
          break
      if image != '':
        break;

    if image != '':
      data = "<request><command>UPDATE</command><node>"+mas[0]+"</node><image>"+image+"</image></request>"
      response = post_request(url, data)


# find stack
for node in nodes:
  mas = node.split(";")
  if len(mas) > 11:
    image=''
    stack_members = []
    for iface in mas[10].split("|"):
      if len(iface.split(",")) > 1:
        iface_name=iface.split(",")[1]
        if len(iface_name.split("/")) == 3:
          stack_number = re.findall(r'\D+(\d+)/\d+/\d+', iface_name)[0]
          find=False
          if len(stack_members) > 0:
            for member in stack_members:
              if stack_number == member:
                find=True
                break
            if not find:
              stack_members.append(stack_number)
          else:
            stack_members.append(stack_number)


    if len(stack_members) == 2:
      image='image/Cisco/Stack-2.png' 
      data = "<request><command>UPDATE</command><node>"+mas[0]+"</node><image>"+image+"</image></request>"
      response = post_request(url, data)
    if len(stack_members) == 3:
      image='image/Cisco/Stack-3.png' 
      data = "<request><command>UPDATE</command><node>"+mas[0]+"</node><image>"+image+"</image></request>"
      response = post_request(url, data)
    if len(stack_members) == 4:
      image='image/Cisco/Stack-4.png' 
      data = "<request><command>UPDATE</command><node>"+mas[0]+"</node><image>"+image+"</image></request>"
      response = post_request(url, data)



