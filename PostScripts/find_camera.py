import json
import logging
import re
import urllib3
import requests
import time
import os.path

# urllib3.disable_warnings()

# neb_server = '10.120.63.3'
neb_server = 'localhost'
neb_server_port = '8080'
url_neb_info = "http://" + neb_server + ":" + neb_server_port + "/get?file=neb.map.pre&key=/"
url_neb_list = "http://" + neb_server + ":" + neb_server_port + "/list?file=neb.map.pre&key=/"
url_neb_info_old = "http://" + neb_server + ":" + neb_server_port + "/get?file=neb.map.pre&key=/"
url_neb_list_old = "http://" + neb_server + ":" + neb_server_port + "/list?file=neb.map.pre&key=/"

neb_map = "neb.map.pre"
neb_map_old = "neb.map"

user = "noc"
passwd = "1qaz2wsx"

DEBUG = True

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
            print("\tStart - "+area)
            if DEBUG: logging.debug("\tStart - "+area)
            url_item = url_neb_list + area
            item_list = requests.get(url_item, headers={"user": user, "passwd": passwd}, verify=False)
            mas1 = item_list.text.split("\n")
            item_map = {}
            for item in mas1:
                # print(" - " + item)
                url = url_neb_info+area+"/"+item
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

def get_neb_info_old():
    result = {}
    print("Start neb info ... ")
    if DEBUG: logging.debug("Start neb info ... ")
    if neb_server == 'localhost':
        if os.path.exists(neb_map_old):
            with open(neb_map_old, "r", encoding="utf-8") as read_file:
                result = json.load(read_file)
        else:
            print("File: "+neb_map_old+" not exist.")
            if DEBUG: logging.debug("File: "+neb_map_old+" not exist.")
    else:
        area_list = requests.get(url_neb_list_old, headers={"user": user, "passwd": passwd}, verify=False)
        # print(area_list.text)
        mas = area_list.text.split("\n")
        for area in mas:
            print("\tStart - "+area)
            if DEBUG: logging.debug("\tStart - "+area)
            url_item = url_neb_list_old + area
            item_list = requests.get(url_item, headers={"user": user, "passwd": passwd}, verify=False)
            mas1 = item_list.text.split("\n")
            item_map = {}
            for item in mas1:
                # print(" - " + item)
                url = url_neb_info_old+area+"/"+item
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

############################################################################
############################################################################
############################################################################
logging.basicConfig(filename="PostScript.log", format='%(asctime)s - %(message)s', level=logging.DEBUG)

# logging.info("Starting get Neb node info ...")
print("Starting get Neb node info ...")
if DEBUG: logging.debug("Starting get Neb node info ...")
neb_info = get_neb_info()
neb_info_old = get_neb_info_old()
# logging.info("Stop get Neb node info.")
print("Stop get Neb node info.")
if DEBUG: logging.debug("Stop get Neb node info.")

for area in neb_info:
    # logging.info("Starting area - " + area + " ...")
    print("Starting area - " + area + " ...")
    if DEBUG: logging.debug("Starting area - " + area + " ...")
    nodes_information = neb_info.get(area).get("nodes_information")
    nodes_information_old = neb_info_old.get(area).get("nodes_information")
    links = neb_info.get(area).get("links")
    mac_ip_port = neb_info.get(area).get("mac_ip_port")
    if nodes_information and links and mac_ip_port:
        for node in nodes_information:
            # if node != "10.96.200.23":
            #     continue
            node_info = nodes_information[node]

            node_descr = ""
            if node_info.get("general") and node_info.get("general").get("sysname"):
                sysname = node_info.get("general").get("sysname")
                if sysname:
                    if sysname == "Axis Camera" or re.match(".*axis.*", sysname.lower()) or \
                            re.match(".*00:40:8c.*", sysname.lower()) or re.match(".*0040\.8c.*", sysname.lower()) or re.match(".*00408c.*", sysname.lower()) or \
                            re.match(".*b8:a4:4f.*", sysname.lower()) or re.match(".*b8a4\.4f.*",sysname.lower()) or re.match(".*b8a44f.*", sysname.lower()) or \
                            re.match(".*00:4b:40.*", sysname.lower()) or re.match(".*004b\.40.*",sysname.lower()) or re.match(".*004b40.*", sysname.lower()) or \
                            re.match(".*ac:cc:8e.*", sysname.lower()) or re.match(".*accc\.8e.*", sysname.lower()) or re.match(".*accc8e.*", sysname.lower()):
                        # adding nodes_informations
                        node_descr = "Axis camera"
                    else:
                        result = re.match(".*([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}).*",sysname)
                        if result:
                            if result.group(1):
                                mac = result.group(1)
                                if re.match("^00:40:8c:.+", mac) or re.match("^ac:cc:8e:.+", mac) or re.match("^b8:a4:4f:.+", mac) or re.match("^00:4b:40:.+", mac) or \
                                        re.match("^0040\.8c.+", mac) or re.match("^accc\.8e.+", mac) or re.match("^b8a4\.4f.+", mac) or re.match("^004b\.40.+", mac):
                                    node_descr = "Axis camera"
                                elif re.match("^18:68:82:.+", mac) or re.match("^1868\.82.+", mac) or re.match("^e0:61:b2:.+", mac) or re.match(
                                        "^e061\.b2.+", mac):
                                    node_descr = "Beward camera"

                else:
                    if node_info.get("general") and node_info.get("general").get("model"):
                        model = node_info.get("general").get("model")
                        result = re.match(".*([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}).*",model)
                        if result:
                            if result.group(1):
                                mac = result.group(1)
                                if re.match("^00:40:8c:.+", mac) or re.match("^ac:cc:8e:.+", mac) or re.match("^b8:a4:4f:.+", mac) or re.match("^00:4b:40:.+", mac) \
                                        or re.match("^0040\.8c.+", mac) or re.match("^accc\.8e.+", mac) or re.match("^b8a4\.4f.+", mac) or re.match("^004b\.40.+", mac):
                                    node_descr = "Axis camera"
                                elif re.match("^18:68:82:.+", mac) or re.match("^1868\.82.+", mac) or re.match("^e0:61:b2:.+", mac) or re.match(
                                        "^e061\.b2.+", mac):
                                    node_descr = "Beward camera"

            if not node_descr and node_info.get("general") and node_info.get("general").get("base_address"):
                base_address = node_info.get("general").get("base_address")
                if base_address:
                    node_descr = ""
                    if re.match("^00:40:8c:.+", base_address) or re.match("^ac:cc:8e:.+", base_address) or re.match("^b8:a4:4f:.+", base_address) or re.match("^00:4b:40:.+", base_address) \
                            or re.match("^0040\.8c.+", base_address) or re.match("^accc\.8e.+", base_address) or re.match("^b8a4\.4f.+", base_address) or re.match("^004b\.40.+", base_address):
                        node_descr = "Axis camera"
                    elif re.match("^18:68:82:.+", base_address) or re.match("^1868\.82.+", base_address) or re.match("^e0:61:b2:.+", base_address) or re.match(
                            "^e061\.b2.+", base_address):
                        node_descr = "Beward camera"

            if node_descr:
                url_add_node = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/general/sysDescription"
                result = requests.post(url_add_node, node_descr, headers={"user": user, "passwd": passwd}, verify=False)

        for mip in mac_ip_port:
            node_id = ""
            node_descr = ""

            if re.match("^00:40:8c:.+", mip[0]) or re.match("^ac:cc:8e:.+", mip[0]) or re.match("^b8:a4:4f:.+", mip[0]) or re.match("^00:4b:40:.+", mip[0]) \
                    or re.match("^0040\.8c.+", mip[0]) or re.match("^accc\.8e.+", mip[0]) or re.match("^b8a4\.4f.+", mip[0]) or re.match("^004b\.40.+", mip[0]):
                if re.match("\d+\.\d+\.\d+\.\d+", mip[1]):
                    node_id = mip[1]
                else:
                    node_id = mip[0]
                node_descr = "Axis camera"
            elif re.match("^18:68:82:.+", mip[0]) or re.match("^1868\.82.+", mip[0]) or re.match("^e0:61:b2:.+", mip[0]) or re.match(
                            "^e061\.b2.+", mip[0]):
                if re.match("\d+\.\d+\.\d+\.\d+", mip[1]):
                    node_id = mip[1]
                else:
                    node_id = mip[0]
                node_descr = "Beward camera"


            if node_id and node_descr:
                # check exist camera am this link
                found = False
                for link1 in links:
                    if (mip[2] == link1[0] and mip[4] == link1[2]) or (mip[2] == link1[3] and mip[4] == link1[5]):
                        found = True
                        break
                    if mip[1] == link1[0] or mip[1] == link1[3]:
                        found = True
                        break

                if not found:
                    # adding nodes_informations
                    url_add_node = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node_id
                    sysname = "Camera_" + mip[1] + "(" + mip[0] + ")"
                    data = {}
                    if nodes_information_old and nodes_information_old.get(mip[1]) and nodes_information_old.get(mip[1]).get("xy"):
                        xy = nodes_information_old.get(mip[1]).get("xy")
                        data = dict(general={"sysname": sysname, "sysDescription": node_descr}, xy=xy, interfaces={"Port": {}})
                    else:
                        data = dict(general={"sysname": sysname, "sysDescription": node_descr}, interfaces={"Port": {}})
                    data_str = json.dumps(data)
                    result = requests.post(url_add_node, data_str, headers={"user": user, "passwd": passwd}, verify=False)
                    # logging.info(url_add_node)
                    # print(url_add_node)

                    # adding links
                    link = [mip[2], mip[3], mip[4], node_id, "", "Port", ]
                    url_link_add = "http://" + neb_server + ":" + neb_server_port + "/add_to_list?file=neb.map.pre&key=/" + area + "/links"
                    data_str = json.dumps(link)
                    result = requests.post(url_link_add, data_str, headers={"user": user, "passwd": passwd},verify=False)
                    # logging.info(url_link_add)
                    # print(url_link_add)

                    # remove from mac_ip_ports
                    url_mac_ip_port_delete = "http://" + neb_server + ":" + neb_server_port + "/del_from_list?file=neb.map.pre&key=/" + area + "/mac_ip_port"
                    data = mip[0] + ';' + mip[1] + ';' + mip[2] + ';' + mip[3] + ';' + mip[4] + ';' + mip[5]
                    result = requests.post(url_mac_ip_port_delete, data, headers={"user": user, "passwd": passwd}, verify=False)
                    # logging.info(url_mac_ip_port_delete)
                    # print(url_mac_ip_port_delete)

commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)
