import requests
import json
import logging
import re
import urllib3
import time
import os.path

# urllib3.disable_warnings()

# neb_server = '10.120.63.3'
neb_server = 'localhost'
neb_server_port = '8080'
url_neb_info = "http://" + neb_server + ":" + neb_server_port + "/get?file=neb.map.pre&key=/"
url_neb_list = "http://" + neb_server + ":" + neb_server_port + "/list?file=neb.map.pre&key=/"

neb_map = "neb.map.pre"

user = "noc"
passwd = "1qaz2wsx"

DEBUG = True

replaces = []
mas = ['.*cisco\s+AIR.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*Cisco\s+IOS\s+Software\s+C1200.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*C1140.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*AIR-BR350.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*C1310.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*CS1310.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*C1240.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*CS1240.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*Cisco3702.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*C1260.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*C1530.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*CAP\d+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*AP\d+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*AP-\d+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*AP_\d+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*Blade.*', 'images/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['.*CBS31X0.*', 'images/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['.*CIGESM.*', 'images/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['.*WS-CBS3020.*', 'images/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['.*WS-CBS3012.*', 'images/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['HP6120XG.*', 'images/Cisco/CS_Blade.png']
replaces.append(mas)
mas = ['.*cisco\s+1720.*', 'images/Cisco/CS1720.png']
replaces.append(mas)
mas = ['.*C1841.*', 'images/Cisco/CS1800.png']
replaces.append(mas)
mas = ['.*C870.*', 'images/Cisco/CS1800.png']
replaces.append(mas)
mas = ['.*C1900.*', 'images/Cisco/CS1900.png']
replaces.append(mas)
mas = ['.*CISCO1921.*', 'images/Cisco/CS1900.png']
replaces.append(mas)
mas = ['.*C2600.*', 'images/Cisco/CS2600.png']
replaces.append(mas)
mas = ['.*Cisco\s+SG\s+200-26.*', 'images/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['.*Cisco\s+SF200-24.*', 'images/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['.*cisco\s+2651.*', 'images/Cisco/CS2600.png']
replaces.append(mas)
mas = ['.*cisco\s+AS2511-RJ.*', 'images/Cisco/AS5400.png']
replaces.append(mas)
mas = ['.*C2800.*', 'images/Cisco/CS2800.png']
replaces.append(mas)
mas = ['.*Cisco\s+281.*1', 'images/Cisco/CS2800.png']
replaces.append(mas)
mas = ['.*Cisco\s+2821.*', 'images/Cisco/CS2800.png']
replaces.append(mas)
mas = ['.*Cisco\s+2851.*', 'images/Cisco/CS2800.png']
replaces.append(mas)
mas = ['.*C2940.*', 'images/Cisco/CS2940.png']
replaces.append(mas)
mas = ['.*C2950.*', 'images/Cisco/CS2950.png']
replaces.append(mas)
mas = ['.*CS2950.*', 'images/Cisco/CS2950.png']
replaces.append(mas)
mas = ['.*CS2950.*', 'images/Cisco/CS2950.png']
replaces.append(mas)
mas = ['.*CISCO2951.*', 'images/Cisco/CS2950.png']
replaces.append(mas)
mas = ['.*CISCO2921.*', 'images/Cisco/CS2921.png']
replaces.append(mas)
mas = ['.*C2900.*', 'images/Cisco/CS2921.png']
replaces.append(mas)
mas = ['.*CISCO2911.*', 'images/Cisco/CS2921.png']
replaces.append(mas)
mas = ['.*C2911.*', 'images/Cisco/CS2921.png']
replaces.append(mas)
mas = ['.*CS2911.*', 'images/Cisco/CS2921.png']
replaces.append(mas)
mas = ['.*C2960.*', 'images/Cisco/CS2960.png']
replaces.append(mas)
mas = ['CS2960.*', 'images/Cisco/CS2960.png']
replaces.append(mas)
mas = ['.*Cisco\s+SF200-24P.*', 'images/Cisco/CS2960.png']
replaces.append(mas)
mas = ['.*C2970.*', 'images/Cisco/CS2960.png']
replaces.append(mas)
mas = ['.*WS-CE500-24PC.*', 'images/Cisco/CS2960.png']
replaces.append(mas)
mas = ['.*WS-C4506.*', 'images/Cisco/CS4506.png']
replaces.append(mas)
mas = ['.*CS4500.*', 'images/Cisco/CS4500.png']
replaces.append(mas)
mas = ['.*C3500.*', 'images/Cisco/CS3500.png']
replaces.append(mas)
mas = ['.*C3550.*', 'images/Cisco/CS3550.png']
replaces.append(mas)
mas = ['.*C3548.*', 'images/Cisco/CS3550.png']
replaces.append(mas)
mas = ['.*C3560.*', 'images/Cisco/CS3550.png']
replaces.append(mas)
mas = ['CS3650.*', 'images/Cisco/CS3550.png']
replaces.append(mas)
mas = ['.*CS3560.*', 'images/Cisco/CS3550.png']
replaces.append(mas)
mas = ['.*Cisco\s+3640.*', 'images/Cisco/CS3640.png']
replaces.append(mas)
mas = ['.*C3725.*', 'images/Cisco/CS3725.png']
replaces.append(mas)
mas = ['.*C3750.*', 'images/Cisco/CS3750.png']
replaces.append(mas)
mas = ['.*CS3750.*', 'images/Cisco/CS3750.png']
replaces.append(mas)
mas = ['.*C3825.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['.*CS3850.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['.*3850.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['C9200.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['C9300.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['C9500.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['CS9200.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['CS9300.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['CS9500.*', 'images/Cisco/CS3800.png']
replaces.append(mas)
mas = ['.*C3900.*', 'images/Cisco/CS3900.png']
replaces.append(mas)
mas = ['.*CISCO39.*', 'images/Cisco/CS3900.png']
replaces.append(mas)
mas = ['.*Catalyst\s+4500.*', 'images/Cisco/CS4500.png']
replaces.append(mas)
mas = ['.*CS4503.*', 'images/Cisco/CS4503.png']
replaces.append(mas)
mas = ['.*CS4506.*', 'images/Cisco/CS6506.png']
replaces.append(mas)
mas = ['.*CS4507.*', 'images/Cisco/CS6506.png']
replaces.append(mas)
mas = ['.*CS6503.*', 'images/Cisco/CS6503.png']
replaces.append(mas)
mas = ['.*C6503.*', 'images/Cisco/CS6503.png']
replaces.append(mas)
mas = ['.*CS6506.*', 'images/Cisco/CS6506.png']
replaces.append(mas)
mas = ['.*Cisco6506.*', 'images/Cisco/CS6506.png']
replaces.append(mas)
mas = ['.*CS6509.*', 'images/Cisco/CS6509.png']
replaces.append(mas)
mas = ['.*Cisco6509.*', 'images/Cisco/CS6509.png']
replaces.append(mas)
mas = ['.*Cisco6807.*', 'images/Cisco/CS6807.png']
replaces.append(mas)
mas = ['.*Cisco6807.*', 'images/Cisco/CS6807.png']
replaces.append(mas)
mas = ['.*C6807.*', 'images/Cisco/CS6807.png']
replaces.append(mas)
mas = ['.*CS6807.*', 'images/Cisco/CS6807.png']
replaces.append(mas)
mas = ['.*CS6513.*', 'images/Cisco/CS6513.png']
replaces.append(mas)
mas = ['.*CS800.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*C800.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*CS500.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*C500.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*CS880.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*Cisco\s+881G.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*Cisco\s+C881.*', 'images/Cisco/CS800.png']
replaces.append(mas)
mas = ['.*Cisco AS5400XM.*', 'images/Cisco/AS5400.png']
replaces.append(mas)
mas = ['.*ISR4331.*', 'images/Cisco/ISR4331.png']
replaces.append(mas)
mas = ['.*ISR.*', 'images/Cisco/ISR.png']
replaces.append(mas)
mas = ['.*ASR1001-X.*', 'images/Cisco/ASR-1000.png']
replaces.append(mas)
mas = ['.*ASR1000.*', 'images/Cisco/ASR-1000.png']
replaces.append(mas)
mas = ['.*N5K-C5548UP.*', 'images/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['.*n5000.*', 'images/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['.*n6000.*', 'images/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['.*n4000.*', 'images/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['Nexus.*', 'images/Cisco/Nexus5000.png']
replaces.append(mas)
mas = ['.*WS-C4948.*', 'images/Cisco/CS4948.png']
replaces.append(mas)
mas = ['.*AnywhereUSB.*', 'images/Cisco/CS4948.png']
replaces.append(mas)
mas = ['.*WLC.*', 'images/Cisco/WLC.png']
replaces.append(mas)
mas = ['.*OEM + engineering.*', 'images/Cisco/WLC.png']
replaces.append(mas)
mas = ['.*Cisco\s+Controller.*', 'images/Cisco/WLC.png']
replaces.append(mas)
mas = ['.*Cisco\s+SF300-24MP.*', 'images/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['.*SG200-26.*', 'images/Cisco/SG-200-24.png']
replaces.append(mas)
mas = ['.*Cisco\s+IP\s+Conference\s+Station.*', 'images/Cisco/ip_conferenc.png']
replaces.append(mas)
mas = ['.*Cisco\s+CSS.*', 'images/Cisco/CSS.png']
replaces.append(mas)
mas = ['.*Cisco\s+SF\s+302-08.*', 'images/Cisco/SF-302-8.png']
replaces.append(mas)
mas = ['.*DES-.*', 'images/Dlink/3226S.png']
replaces.append(mas)
mas = ['.*DES-3526.*', 'images/Dlink/3526.png']
replaces.append(mas)
mas = ['.*DGS-.*', 'images/Dlink/DGS.png']
replaces.append(mas)
mas = ['.*DL100.*', 'images/Dlink/DGS.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+2424.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['HP', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+2524.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*HP_2424.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*HP-2424.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*HP2424.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*HP\s+2524.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*2910al-24G.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+2512.*', 'images/HP/2512.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+1600M.*', 'images/HP/2512.png']
replaces.append(mas)
mas = ['.*2530-24.*', 'images/HP/2530.png']
replaces.append(mas)
mas = ['.*3400cl-24G.*', 'images/HP/2530.png']
replaces.append(mas)
mas = ['.*5308xl.*', 'images/HP/5308.png']
replaces.append(mas)
mas = ['.*Switch 5304XL.*', 'images/HP/5308.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+8000M.*', 'images/HP/8000.png']
replaces.append(mas)
mas = ['.*HP\s+8000M.*', 'images/HP/8000.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+4000M.*', 'images/HP/8000.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+4108GL.*', 'images/HP/8000.png']
replaces.append(mas)
mas = ['.*Switch\s+4104GL.*', 'images/HP/8000.png']
replaces.append(mas)
mas = ['.*ProCurve\s+j9020a\s+Switch\s+2510.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*HP\s+-\s+1910-8.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*HPE\s+V1910-24G.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*HP\s+V1910-24G.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*1920-24G.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*1920-16G.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*Switch\s+2650.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*Switch\s+2610.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*HP 2512.*', 'images/HP/2510.png']
replaces.append(mas)
mas = ['.*HP\s+2424M.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*HP\s+2626.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+2626.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*ProCurve\s+Switch\s+6108.*', 'images/HP/6108.png']
replaces.append(mas)
mas = ['.*ProCurve\s+J4903A\s+Switch\s+2824.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*ProCurve Switch 2824.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['slujba', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*ProCurve\s+J9019B\s+Switch\s+2510B-24.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*Switch\s+2848.*', 'images/HP/2848.png']
replaces.append(mas)
mas = ['.*HP\s+VC\s+Flexv', 'images/HP/1910-8.png']
replaces.append(mas)
mas = ['.*J9029A.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*srx210.*', 'images/Juniper/SRX210.png']
replaces.append(mas)
mas = ['.*EDS-.*', 'images/MOXA/MOXA.png']
replaces.append(mas)
mas = ['EDSP.*', 'images/MOXA/MOXA.png']
replaces.append(mas)
mas = ['.*MOXA.*', 'images/MOXA/MOXA.png']
replaces.append(mas)
mas = ['.*AWK4121.*', 'images/MOXA/MOXA.png']
replaces.append(mas)
mas = ['.*Nortel.*', 'images/Nortel/nortel.png']
replaces.append(mas)
mas = ['.*Palo\s+Alto\s+Networks.*', 'images/PaloAlto/3000.png']
replaces.append(mas)
mas = ['.*Linux\s+2\.6\.32.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*NanoStation M2.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*N2N.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*N5N.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*N2B.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*N5B.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*B2N.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*B2T.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*PB5.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*NB5.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*B5N.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*P5B.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*T07.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*XM\.v5\.5\.8.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*Rocket\s+M5.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*NanoStation\s+M5.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*PowerBeam\s+M2.*', 'images/Ubnt/AP.png']
replaces.append(mas)
mas = ['.*ZyXEL.*', 'images/Zyxel/zyxel.png']
replaces.append(mas)
mas = ['.*Zyx-IES612.*', 'images/Zyxel/zyxel.png']
replaces.append(mas)
mas = ['.*ATI\s+8000S.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*AT-8000S.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*AT8000.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*MikroTik.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*AT-8516F.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*Allied\s+Telesyn.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*3Com\s+SuperStack\s+3.*', 'images/AT/8000.png']
replaces.append(mas)
mas = ['.*netbotz.*', 'images/APC/rackmonitor.png']
replaces.append(mas)
mas = ['.*Linux\s+rvbd.*', 'images/Riverbed/riverbed.png']
replaces.append(mas)
mas = ['.*TDMoIP.*', 'images/Riverbed/riverbed.png']
replaces.append(mas)
mas = ['.*SHDSL.*', 'images/DSL.png']
replaces.append(mas)
mas = ['.*ISCOM2118-MA.*', 'images/RiseCom/risecom.png']
replaces.append(mas)
mas = ['.*R2812.*', 'images/RiseCom/risecom.png']
replaces.append(mas)
mas = ['.*IBM\s+Flex\s+System.*', 'images/IBM/IBM_Flex.png']
replaces.append(mas)
mas = ['.*Lenovo\s+RackSwitch.*', 'images/Lenovo/G8264.png ']
replaces.append(mas)
mas = ['.*IBM\s+Networking\s+Operating\s+System\s+RackSwitch.*', 'images/IBM/IBM_Flex.png']
replaces.append(mas)
mas = ['.*Axis camera.*', 'images/Camera/axis.png']
replaces.append(mas)
mas = ['.*AXIS.+Network Camera.*', 'images/Camera/axis.png']
replaces.append(mas)
mas = ['.*Beward camera.*', 'images/Camera/beward.png']
replaces.append(mas)
mas = ['.*NE2572v', 'images/Lenovo/LNV2572.png']
replaces.append(mas)
mas = ['.*NE10032.*', 'images/Lenovo/LNV2572.png']
replaces.append(mas)
mas = ['.*EN4093R.*', 'images/Lenovo/LNV2572.png']
replaces.append(mas)
mas = ['.*LNV\d+.*', 'images/Lenovo/LNV2572.png']
replaces.append(mas)
mas = ['.*AP[0-9a-f]+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*AP-[0-9a-f]+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*AP_[0-9a-f]+.*', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['AP', 'images/Cisco/CS_AP.png']
replaces.append(mas)
mas = ['.*SCALANCE XF208.*', 'images/Siemens/XF208.png']
replaces.append(mas)
mas = ['Revision F\.\d+\.\d+.*', 'images/HP/2424.png']
replaces.append(mas)
mas = ['.*AC6508.*', 'images/Huawei/AC6508.png']
replaces.append(mas)

replaces1 = []
mas1 = ['br\d', 'images/AP.png']
replaces1.append(mas1)


########################################################################################
def get_neb_info():
    result = {}
    print("Start neb info ... ")
    if DEBUG: logging.debug("Start neb info ... ")
    if neb_server == 'localhost':
        if os.path.exists(neb_map):
            with open(neb_map, "r", encoding="utf-8") as read_file:
                result = json.load(read_file)
        else:
            print("File: "+neb_map+" not exist.")
            if DEBUG: logging.debug("File: "+neb_map+" not exist.")
    else:
        area_list = requests.get(url_neb_list, headers={"user": user, "passwd": passwd}, verify=False)
        # print(area_list.text)
        mas = area_list.text.split("\n")
        for area in mas:
            print("\tStart - " + area)
            if DEBUG: logging.debug("\tStart - " + area)
            url_item = url_neb_list + area
            item_list = requests.get(url_item, headers={"user": user, "passwd": passwd}, verify=False)
            mas1 = item_list.text.split("\n")
            item_map = {}
            for item in mas1:
                # print(" - " + item)
                url = url_neb_info + area + "/" + item
                response = requests.get(url, headers={"user": user, "passwd": passwd}, verify=False)
                if response.status_code == 200:
                    json_item = json.loads(response.text)
                    item_map[item] = json_item
            result[area] = item_map
            print("\tStop - " + area)
            if DEBUG: logging.debug("\tStop - " + area)
    print("Stop neb info.")
    if DEBUG: logging.debug("Stop neb info.")
    return result


########################################################################################
logging.basicConfig(filename="PostScript.log", format='%(asctime)s - %(message)s', level=logging.DEBUG)

# logging.info("Starting get Neb node info ...")
print("Starting get Neb node info ...")
if DEBUG: logging.debug("Starting get Neb node info ...")
neb_info = get_neb_info()
# logging.info("Stop get Neb node info.")
print("Stop get Neb node info.")
if DEBUG: logging.debug("Stop get Neb node info.")

if neb_info:
    for area in neb_info:
        # logging.info("Starting area - " + area + " ...")
        print("Starting area - " + area + " ...")
        if DEBUG: logging.debug("Starting area - " + area + " ...")
        nodes_information = neb_info.get(area).get("nodes_information")
        # node_protocol_accounts = neb_info.get(area).get("node_protocol_accounts")
        # links = neb_info.get(area).get("links")
        # mac_ip_port = neb_info.get(area).get("mac_ip_port")

        if nodes_information:
            for node in nodes_information:
                # if node == "10.96.115.50":
                #     print(node)
                image_old = nodes_information.get(node).get("image")
                general = nodes_information.get(node).get("general")
                sysname = general.get("sysname")
                if not sysname:
                    sysname = ""
                interfaces = nodes_information.get(node).get("interfaces")
                image = None
                if interfaces and not re.match(r'.*68\d\d.*', sysname):
                    stack = {}
                    for interface in interfaces:
                        res = re.match(r'^\D+\s*(\d+)/\d+/\d+$', interface)
                        if res:
                            stack[res.group(1)] = res.group(1)
                    if len(stack) == 2:
                        image = 'images/Cisco/Stack-2.png'
                    elif len(stack) == 3:
                        image = 'images/Cisco/Stack-3.png'
                    elif len(stack) == 4:
                        image = 'images/Cisco/Stack-4.png'
                    if image and image != image_old:
                        url_set_image = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/image"
                        data = image
                        result = requests.post(url_set_image, data, headers={"user": user, "passwd": passwd}, verify=False)
                        # print(url_set_image)
                if not image:
                    if general:
                        sysdescription = general.get("sysDescription")
                        if not sysdescription:
                            sysdescription = general.get("model")

                        image = None
                        if sysdescription:
                            sysdescription = sysdescription.lower()
                            for m in replaces:
                                p = re.compile(m[0].lower())
                                if p.match(sysdescription):
                                    image = m[1]
                                    break
                            if image and image != image_old:
                                url_set_image = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/image"
                                data = image
                                result = requests.post(url_set_image, data, headers={"user": user, "passwd": passwd}, verify=False)
                                # print(url_set_image)
                        if not image:
                            sysname = general.get("sysname")
                            if sysname:
                                sysname = sysname.lower()
                                image = None
                                for m in replaces:
                                    p = re.compile(m[0].lower())
                                    if p.match(sysname):
                                        image = m[1]
                                        break
                                if image and image != image_old:
                                    url_set_image = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/image"
                                    data = image
                                    result = requests.post(url_set_image, data, headers={"user": user, "passwd": passwd}, verify=False)
                                    # print(url_set_image)

    commit_url = "http://" + neb_server + ":" + neb_server_port + "/commit"
    res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)


