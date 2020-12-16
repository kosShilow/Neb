#!/usr/bin/env python
# -*- coding: utf-8 -*-
# vim:fileencoding=utf-8


import json
import re
import urllib3
import requests
import os.path
import logging
import socket

DEBUG = True

# urllib3.disable_warnings()

# neb_server = '10.120.63.3'
neb_server = 'localhost'
neb_server_port = '8080'
url_neb_info = "http://" + neb_server + ":" + neb_server_port + "/get?file=neb.map.pre&key=/"
url_neb_list = "http://" + neb_server + ":" + neb_server_port + "/list?file=neb.map.pre&key=/"

# neb_map = "C:/Users/Public/eee/PostScripts/neb.map.pre"
neb_map = "neb.map.pre"

user = "noc"
passwd = "1qaz2wsx"


################################################################
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

def WriteChunkToServer(url, str, limit_size):
    result = True
    out = ""
    length = 0
    for line in str.split("\n"):
        out = out + line + "\n"
        length = length + len(line)
        if length > limit_size:
            res = requests.post(url, out, headers={"user": user, "passwd": passwd}, verify = False)
            if res.status_code != 200:
                print("Error post CHUNK request: url - " + url + " val - " + out)
                if DEBUG: logging.debug("Error post CHUNK request: url - " + url + " val - " + out)
                break
            out = ""
            length = 0
    if out:
        res = requests.post(url, out, headers={"user": user, "passwd": passwd}, verify=False)
        if res.status_code != 200:
            print("Error post CHUNK request: url - " + url + " val - " + out)
            if DEBUG: logging.debug("Error post CHUNK request: url - " + url + " val - " + out)

    if res.status_code == 200:
        result = True
    else:
        result = False
    return result

############################################################################
############################################################################
############################################################################
logging.basicConfig(filename="PostScript.log", format='%(asctime)s - %(message)s', level=logging.DEBUG)

print("Starting get Neb node info ...")
if DEBUG: logging.debug("Starting get Neb node info ...")
neb_info = get_neb_info()
print("Stop get Neb node info.")
if DEBUG: logging.debug("Stop get Neb node info.")

for area in neb_info:
    # logging.info("Starting area - " + area + " ...")
    print("Starting area - " + area + " ...")
    # if area != "area_olkon":
    #     continue
    if DEBUG: logging.debug("Starting area - " + area + " ...")
    nodes_information = neb_info.get(area).get("nodes_information")
    name_ip = {}
    mac_ip_port = neb_info.get(area).get("mac_ip_port")
    if nodes_information:
        for node in nodes_information:
            if re.match("^\d+\.\d+\.\d+\.\d+$", node):
                # print(node)
                try:
                    name = socket.gethostbyaddr(node)[0]
                    name = name.split(".")[0].lower()
                    name_ip[name] = node
                except:
                    pass
                    # print("ip "+node+" not resolve name!!!")

    if mac_ip_port:
        for item in mac_ip_port:
            if re.match("^\d+\.\d+\.\d+\.\d+$", item[1]):
                # print(node)
                try:
                    name = socket.gethostbyaddr(item[1])[0]
                    name = name.split(".")[0].lower()
                    name_ip[name] = item[1]
                except:
                    pass
                    # print("ip "+node+" not resolve name!!!")

    neb_server = '10.120.63.3'

    url_set = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/names"
    result = requests.post(url_set, "{}", headers={"user": user, "passwd": passwd}, verify=False)
    commit_url = "http://" + neb_server + ":" + neb_server_port + "/commit"
    res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)

    if result.status_code == 200:
        url_set_chunk = "http://" + neb_server + ":" + neb_server_port + "/setchunk?file=neb.map.pre&key=/" + area + "/names"

        data = ""
        prefix = "/" + area + "/names"
        for name in name_ip:
            data = data + prefix+"/"+name+";"+name_ip[name] + "\n"

        if not WriteChunkToServer(url_set_chunk, data, 1000):
            print("SetChunk is error!!!")
            if DEBUG: logging.debug("SetChunk is error!!!")
        commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
        res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)
        print("111")
print("End.")
