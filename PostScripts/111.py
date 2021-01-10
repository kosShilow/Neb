import urllib3
import urllib
import requests
import json
import re
import os


# neb_server = '10.120.63.3'
neb_server = 'localhost'
neb_server_port = '8080'
url_neb_info = "http://" + neb_server + ":" + neb_server_port + "/get?file=neb.map.pre&key=/"
url_neb_list = "http://" + neb_server + ":" + neb_server_port + "/list?file=neb.map.pre&key=/"

neb_map = "neb.map"

user = "noc"
passwd = "1qaz2wsx"

def get_neb_info(info):
    result = {}
    print("Start neb info ... ")
    if neb_server == 'localhost':
        if os.path.exists(info):
            with open(info, encoding="utf8") as read_file:
                result = json.load(read_file)
        else:
            print("File: "+info+" not exist.")
    else:
        area_list = requests.get(url_neb_list, headers={"user": user, "passwd": passwd}, verify=False)
        # print(area_list.text)
        mas = area_list.text.split("\n")
        for area in mas:
            print("\tStart - "+area)
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
    print("Stop neb info.")
    return result

####################################

info1 = get_neb_info("neb.map.old")

all_size = 0
for area in info1:
    if info1.get(area) and info1[area].get("nodes_information"):
        size = len(info1[area].get("nodes_information"))
        all_size = all_size + size
        print(area+' -'+str(size))
print(str(all_size))
