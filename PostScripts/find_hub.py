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
    if area != "area_chermk":
        continue
    # logging.info("Starting area - " + area + " ...")
    print("Starting area - " + area + " ...")
    if DEBUG: logging.debug("Starting area - " + area + " ...")
    nodes_information = neb_info.get(area).get("nodes_information")
    nodes_information_old = neb_info_old.get(area).get("nodes_information")
    links = neb_info.get(area).get("links")
    # mac_ip_port = neb_info.get(area).get("mac_ip_port")
    if nodes_information and links:
        node_iface_neighbors = []
        i = 0
        for link in links:
            neighbors = []
            node = None
            id_iface = None
            iface = None
            for link1 in links[i+1:]:
                if link[0] == link1[0] and link[2] == link1[2]:
                    node = link[0]
                    id_iface = link[1]
                    iface = link[2]
                    neighbors.append([link1[3], link1[4], link1[5]])
                if link[0] == link1[3] and link[2] == link1[5]:
                    node = link[0]
                    id_iface = link[1]
                    iface = link[2]
                    neighbors.append([link1[0], link1[1], link1[2]])
                if link[3] == link1[0] and link[5] == link1[2]:
                    node = link[3]
                    id_iface = link[4]
                    iface = link[5]
                    neighbors.append([link1[3], link1[4], link1[5]])
                if link[3] == link1[3] and link[5] == link1[5]:
                    node = link[3]
                    id_iface = link[4]
                    iface = link[5]
                    neighbors.append([link1[0], link1[1], link1[2]])
            if node and id_iface and iface and neighbors:
                node_iface_neighbors.append([node, id_iface, iface, neighbors])
            i = i + 1

        print("1111")


# commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
# res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)
# print("End.")