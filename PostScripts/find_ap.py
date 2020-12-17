#!/usr/bin/env python
# -*- coding: utf-8 -*-
# vim:fileencoding=utf-8


import json
import re
import urllib3
import requests
from pysnmp.entity.rfc3413.oneliner import cmdgen
from pysnmp.proto.rfc1902 import Integer, IpAddress, OctetString
from ipaddress import ip_network
from ipaddress import ip_address
from ipaddress import IPv4Address
from ipaddress import ip_interface
import locale
import os.path
import logging

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

wlc_controllers = [
    ["10.123.255.49", '20fyufhf80', 2],
    ["10.123.255.55", '20fyufhf80', 2],
    ["10.123.255.59", '20fyufhf80', 2],
    ["10.72.9.100", 'SeverRead', 2],
    ["10.22.1.250", 'SeverRead', 2],
    ["10.73.100.25", 'SeverRead', 2],
    ["10.150.60.253", 'SeverRead', 2],
    ["10.150.82.200", 'SeverRead', 2],
    ["10.150.64.252", 'SeverRead', 2],
    ["10.44.248.253", 'SeverRead', 2],
    ["10.36.7.251", 'SeverRead', 2],
    ["10.180.3.19", 'SeverRead', 2],
    ["10.76.11.251", 'SeverRead', 2],
    ["10.151.254.251", 'SeverRead', 2],
    ["10.117.32.123", 'SeverRead', 2],
    ["10.4.132.1", 'SeverRead', 2],
    ["10.112.171.251", 'SeverRead', 2],
    ["10.112.171.248", 'SeverRead', 2],
    ["10.112.162.241", 'SeverRead', 2],
    ["10.112.162.242", 'SeverRead', 2],
    ["10.72.64.13", 'SeverRead', 2],
    ["10.18.8.254", 'SeverRead', 2],
    ["10.1.18.6", 'SeverRead', 2],
    ["10.120.63.16", 'SeverRead', 2],
    ["10.4.119.22", 'SeverRead', 2],
    ["10.96.45.230", '20fyufhf80', 2]
]
oid_mac_sysname = (1, 3, 6, 1, 4, 1, 14179, 2, 2, 1, 1, 3)
oid_mac_ip = (1, 3, 6, 1, 4, 1, 14179, 2, 2, 1, 1, 19)

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

def sysname_serials_from_controllers(wlc_controllers):
    # sysname_serial = {}
    ip_mac = {}
    for controller in wlc_controllers:
        print("WLC_controller: " + controller[0])
        if DEBUG: logging.debug("WLC_controller: " + controller[0])
        res = snmp_walk(controller[0], controller[1], controller[2], oid_mac_ip)
        mac_ip = {}
        for item in res:
            m = item[0].split(".")[12:]
            mac = ""
            for it in m:
                mac = mac + ":" + it
            ip = item[1]
            mac_ip[mac] = ip

            ip1 = ip.asNumbers()
            ip = str(ip1[0])+"."+str(ip1[1])+"."+str(ip1[2])+"."+str(ip1[3])

            mac_mas = mac.split(":")[1:]
            mac_normalize = ""
            for mac_it in mac_mas:
                mac_it = hex(int(mac_it)).split('x')[-1]
                if len(mac_it) == 1:
                    mac_it = '0'+mac_it
                mac_normalize=mac_normalize+":"+mac_it
            mac_normalize = mac_normalize[1:]

            ip_mac[ip] = mac_normalize

    return ip_mac

def snmp_walk(server, community, version, oid):
    result = []
    try:
        generator = cmdgen.CommandGenerator()
        if version == 2:
            comm_data = cmdgen.CommunityData('server', community, mpModel=1)  # 1 means version SNMP v2c
        else:
            comm_data = cmdgen.CommunityData('server', community, mpModel=0)
        transport = cmdgen.UdpTransportTarget((server, 161))

        real_fun = getattr(generator, 'nextCmd')
        res = (errorIndication, errorStatus, errorIndex, varBinds) \
            = real_fun(comm_data, transport, oid)

        if not errorIndication is None or errorStatus is True:
            pass
        else:
            for item in varBinds:
                oid = str(item[0][0])
                val = IpAddress(item[0][1])
                result.append([oid, val])
    except Exception:
        return result
    return result

############################################################################
############################################################################
############################################################################
logging.basicConfig(filename="PostScript.log", format='%(asctime)s - %(message)s', level=logging.DEBUG)

print("Starting get information from WLC controllers ...")
if DEBUG: logging.debug("Starting get information from WLC controllers ...")
ip_mac = sysname_serials_from_controllers(wlc_controllers)
print("Stop get information from WLC controllers.")
if DEBUG: logging.debug("Stop get information from WLC controllers.")


print("Starting get Neb node info ...")
if DEBUG: logging.debug("Starting get Neb node info ...")
neb_info = get_neb_info()
print("Stop get Neb node info.")
if DEBUG: logging.debug("Stop get Neb node info.")

for area in neb_info:
    # logging.info("Starting area - " + area + " ...")
    print("Starting area - " + area + " ...")
    if DEBUG: logging.debug("Starting area - " + area + " ...")
    nodes_information = neb_info.get(area).get("nodes_information")
    # links = neb_info.get(area).get("links")
    # mac_ip_port = neb_info.get(area).get("mac_ip_port")
    if nodes_information:
        for node in nodes_information:
            node_info = nodes_information[node]
            if node_info.get("external") and node_info.get("external").get("information") and \
                node_info.get("external").get("information").get("antena"):
                # set nodes_informations
                url_add_node = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/general/model"
                data = "AP"
                result = requests.post(url_add_node, data, headers={"user": user, "passwd": passwd}, verify=False)

            if ip_mac.get(node):
                ip = node
                mac = ip_mac.get(node)
                if node_info.get("general"):
                    # node_info.get("general")["model"] = "AP"
                    # node_info.get("general")["base_address"] = mac

                    # set nodes_informations
                    url_add_node = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/general/model"
                    data = "AP"
                    result = requests.post(url_add_node, data, headers={"user": user, "passwd": passwd}, verify=False)
                    url_add_node = "http://" + neb_server + ":" + neb_server_port + "/set?file=neb.map.pre&key=/" + area + "/nodes_information/" + node + "/general/base_address"
                    data = mac
                    result = requests.post(url_add_node, data, headers={"user": user, "passwd": passwd}, verify=False)

commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)
# print("Wait...")
# time.sleep(60)
# print("End.")