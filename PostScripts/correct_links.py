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

neb_map = "neb.map.pre"

user = "noc"
passwd = "1qaz2wsx"

DEBUG = True

def get_neb_info():
    result = {}
    print("Start neb info ... ")
    if DEBUG: logging.debug("Start neb info ... ")
    if neb_server == 'localhost':
        if os.path.exists(neb_map):
            with open(neb_map, "r") as read_file:
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

############################################################################
############################################################################
############################################################################
logging.basicConfig(filename="PostScript.log", format='%(asctime)s - %(message)s', level=logging.DEBUG)

# logging.info("Starting get Neb node info ...")
print("Starting get Neb node info ...")
if DEBUG: logging.debug("Starting get Neb node info ...")
neb_info = get_neb_info()
# logging.info("Stop get Neb node info.")
print("Stop get Neb node info.")
if DEBUG: logging.debug("Stop get Neb node info.")

for area in neb_info:
    # logging.info("Starting area - " + area + " ...")
    print("Starting area - " + area + " ...")
    if DEBUG: logging.debug("Starting area - " + area + " ...")
    nodes_information = neb_info.get(area).get("nodes_information")
    links = neb_info.get(area).get("links")

    links_map = {}
    for node in nodes_information:
        for link in links:
            if link[0] == node:
                if links_map.get(node):
                    links_map[node].append([link[0], link[1], link[2], link[3], link[4], link[5], link])
                else:
                    links_map[node] = [ [link[0], link[1], link[2], link[3], link[4], link[5], link] ]
            elif link[3] == node:
                if links_map.get(node):
                    links_map[node].append([link[3], link[4], link[5], link[0], link[1], link[2], link])
                else:
                    links_map[node] = [ [link[3], link[4], link[5], link[0], link[1], link[2], link] ]

    for link in links_map:
        if len(links_map[link]) == 2:
            iface1 = links_map[link][0][2]
            iface2 = links_map[link][1][2]
            res1 = re.match(r'^([A-Za-z]*)([\d\.\/]+)$', iface1)
            res2 = re.match(r'^([A-Za-z]*)([\d\.\/]+)$', iface2)
            if res1 and res2:
                prefix1 = res1.group(1).lower()
                prefix2 = res2.group(1).lower()
                if res1.group(2) == res2.group(2) and (prefix1.find(prefix2) == 0 or prefix2.find(prefix1) == 0):
                    node = link
                    image1 = None
                    if nodes_information.get(links_map[link][0][3]) and nodes_information.get(links_map[link][0][3]).get("image"):
                        image1 = nodes_information.get(links_map[link][0][3]).get("image")
                    neighbor1 = [links_map[link][0][3], links_map[link][0][4], links_map[link][0][5], image1]
                    image2 = None
                    if nodes_information.get(links_map[link][1][3]) and nodes_information.get(links_map[link][1][3]).get("image"):
                        image2 = nodes_information.get(links_map[link][1][3]).get("image")
                    neighbor2 = [links_map[link][1][3], links_map[link][1][4], links_map[link][1][5], image2]
                    # print(iface1 + " - " + iface2)
                    # print("\t --- "+res1.group(2)+" - "+res2.group(2))
                    # print(str(neighbor1)+" - "+str(neighbor2))

                    url_link_delete = "http://" + neb_server + ":" + neb_server_port + "/del_from_list?file=neb.map.pre&key=/" + area + "/links"
                    data = links_map[link][0][6][0] + ';' + links_map[link][0][6][1] + ';' + links_map[link][0][6][2] + ';' + links_map[link][0][6][3] + ';' + links_map[link][0][6][4] + ';' + links_map[link][0][6][5]
                    result = requests.post(url_link_delete, data, headers={"user": user, "passwd": passwd}, verify=False)
                    print("Delete link1: " + str(links_map[link][0][6]))
                    data = links_map[link][1][6][0] + ';' + links_map[link][1][6][1] + ';' + links_map[link][1][6][2] + ';' + links_map[link][1][6][3] + ';' + links_map[link][1][6][4] + ';' + links_map[link][1][6][5]
                    result = requests.post(url_link_delete, data, headers={"user": user, "passwd": passwd}, verify=False)
                    print("Delete link2: " + str(links_map[link][1][6]))

                    url_link_add = "http://" + neb_server + ":" + neb_server_port + "/add_to_list?file=neb.map.pre&key=/" + area + "/links"
                    if neighbor2[3] and re.match(".*MOXA\.png", neighbor2[3]):
                        add_link = [neighbor1[0], neighbor1[1], neighbor1[2], neighbor2[0], "", "unknown"]
                        data = json.dumps(add_link)
                        result = requests.post(url_link_add, data, headers={"user": user, "passwd": passwd}, verify=False)
                        print("Adding link: "+str(add_link))
                        if len(links_map[link][0][2]) > len(links_map[link][1][2]):
                            add_link = [neighbor2[0], neighbor2[1], neighbor2[2], links_map[link][0][0], links_map[link][0][1], links_map[link][0][2]]
                            data = json.dumps(add_link)
                            result = requests.post(url_link_add, data, headers={"user": user, "passwd": passwd}, verify=False)
                            print("Adding link: " + str(add_link))
                        else:
                            add_link = [neighbor2[0], neighbor2[1], neighbor2[2], links_map[link][0][0], links_map[link][1][1],links_map[link][1][2]]
                            data = json.dumps(add_link)
                            result = requests.post(url_link_add, data, headers={"user": user, "passwd": passwd}, verify=False)
                            print("Adding link: " + str(add_link))
                    elif neighbor1[3] and re.match(".*MOXA\.png", neighbor1[3]):
                        add_link = [neighbor2[0], neighbor2[1], neighbor2[2], neighbor1[0], "", "unknown"]
                        data = json.dumps(add_link)
                        result = requests.post(url_link_add, data, headers={"user": user, "passwd": passwd}, verify=False)
                        print("Adding link: "+str(add_link))
                        if len(links_map[link][0][2]) > len(links_map[link][1][2]):
                            add_link = [neighbor1[0], neighbor1[1], neighbor1[2], links_map[link][0][0], links_map[link][0][1], links_map[link][0][2]]
                            data = json.dumps(add_link)
                            result = requests.post(url_link_add, data, headers={"user": user, "passwd": passwd}, verify=False)
                            print("Adding link: " + str(add_link))
                        else:
                            add_link = [neighbor1[0], neighbor1[1], neighbor1[2], links_map[link][0][0], links_map[link][1][1],links_map[link][1][2]]
                            data = json.dumps(add_link)
                            result = requests.post(url_link_add, data, headers={"user": user, "passwd": passwd}, verify=False)
                            print("Adding link: " + str(add_link))

commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)
print("End.")