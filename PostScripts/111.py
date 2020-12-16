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

def get_neb_info():
    result = {}
    print("Start neb info ... ")
    if neb_server == 'localhost':
        if os.path.exists(neb_map):
            with open(neb_map, encoding="utf8") as read_file:
                result = json.load(read_file)
        else:
            print("File: "+neb_map+" not exist.")
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
info = get_neb_info()

all_size = 0
for area in info:
    if info.get(area) and info[area].get("nodes_information"):
        size = len(info[area].get("nodes_information"))
        all_size = all_size + size
        print(area+' -'+str(size))
print(str(all_size))

exclude_sysname = []
exclude_sysname.append('^[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]\.[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]\.[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]$')
exclude_sysname.append('^[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]$')

sysname = '40:b0:76:d7:cf:11'
if sysname:
    found = False
    for filter in exclude_sysname:
        p = re.compile(filter.lower())
        if p.match(sysname.lower()):
            found = True
            break
    if found:
        print("1111")



area = "area_chermk"
node = "10.96.115.200"
url_node_delete = "http://" + neb_server + ":" + neb_server_port + "/delete?file=neb.map.pre&key=/" + area + "/nodes_information/" + node
# print(url_node_delete)
result = requests.get(url_node_delete, headers={"user": user, "passwd": passwd}, verify=False)

commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)


interface = 'GigabitEthernet 1/0/1'
res = re.match(r'^\D+\s*(\d+)/\d+/\d+$', interface)
if res:
    print(res.group(1))


print("Starting get Neb node info ...")

neb_info = get_neb_info()

if neb_info.get("area_chermk") and neb_info.get("area_chermk").get("nodes_information"):
    for node in neb_info.get("area_chermk").get("nodes_information"):
        node_info = neb_info.get("area_chermk").get("nodes_information").get(node)
        interfaces = node_info.get("interfaces")
        if interfaces:
            for interface in interfaces:
                access_vlan = interfaces.get(interface).get("access_vlan")
                if access_vlan:
                    if re.match("^500.*", access_vlan):
                        if interfaces.get(interface).get("admin_status") != 'down':
                            print(node+", "+interface+": "+str(interfaces.get(interface)))
print("Stop get Neb node info.")