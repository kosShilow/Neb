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
raw_ip_mac_path = "."

user = "noc"
passwd = "1qaz2wsx"

DEBUG = True

filters = []
filters.append('.*APC Web/SNMP Management Card.*')
filters.append('.*Phone.*')
filters.append('.*armv5tejl.*')
filters.append('.*DMC\-.*')
filters.append('.*Windows.*')
filters.append('.*MULTI\-ENVIRONMENT.*')
filters.append('.*Storage.*')
filters.append('.*UPS.*')
filters.append('.*APS.*')
filters.append('.*APC.*')
filters.append('.*Integrated Lights-Out.*')
filters.append('.*Printing.*')
filters.append('.*Powerware.*')
filters.append('.*PowerAP.*')
filters.append('.*Power.*')
filters.append('.*VMware.*')
filters.append('.*Communicator.*')
filters.append('.*EX 3000.*')
filters.append('.*EX 2200.*')
filters.append('.*Cisco ATA 190.*')
filters.append('.*Cisco IP Phone.*')
filters.append('.*sip.*')
filters.append('.*T46S.*')
filters.append('.*T19P.*')
filters.append('.*ISE-VM-K9.*')
filters.append('.*NP\d+.*')
filters.append('.*NPIA-\d+.*')
filters.append('.*XM.v5.5.6.*')
filters.append('.*Releasebuild-\d+.*')
filters.append('.*SIMATIC.*')
filters.append('.*Xerox.*')
filters.append('.*HP EliteDesk.*')
filters.append('Demo Software for CPK Snr.*')
filters.append('Gi0')
filters.append('port-001')
filters.append('E1212-T')
filters.append('MOTOTRBO Repeater')

exclude_mac = []
exclude_mac.append('c8:87:3b:00:25:bb')

exclude_sysname = []
exclude_sysname.append('.*PowerBeam.*')
exclude_sysname.append('SEP[0-9a-f]+.*')
exclude_sysname.append('0')
exclude_sysname.append('NPIA-.*')
exclude_sysname.append('T19P-.*')
exclude_sysname.append('Managed Redundant Switch.*')
exclude_sysname.append('port-001')
exclude_sysname.append('NP5410')
exclude_sysname.append('NP\d+.*')
exclude_sysname.append('4d:47:2d:4d:42:33:31:37:30:5f:c1:e0:f8:ed:ff:5f:d6:d2:c4')
exclude_sysname.append('tp132Bm1')
exclude_sysname.append('KPP-12')
exclude_sysname.append('NP5230A.*')
exclude_sysname.append('idrac-.*')
exclude_sysname.append('infra04.severstal.severstalgroup.com')
exclude_sysname.append('not_advertised.*')
exclude_sysname.append('not advertised.*')
exclude_sysname.append('^[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]\.[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]\.[0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]$')
exclude_sysname.append('^[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]:[0-9a-fA-F][0-9a-fA-F]$')
exclude_sysname.append('--')
###################################################

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

def get_raw_ip_mac(raw_ip_mac_path):
    area_ip_mac = {}
    area_mac_ip = {}
    if os.path.exists(raw_ip_mac_path):
        if os.path.isdir(raw_ip_mac_path):
            for file in os.listdir(path=raw_ip_mac_path):
                result = re.match("^arp_mac_table_(.+)", file)
                if result:
                    area = result.group(1)
                    f = open(raw_ip_mac_path+'/'+file)
                    for line in f:
                        mas = line.split(',')
                        ip = mas[3]
                        mac = mas[4].rstrip()
                        if ip != "unknown_ip" and mac != "unknown_mac":
                            if area_ip_mac.get(area):
                                area_ip_mac.get(area)[ip] = mac
                                area_mac_ip.get(area)[mac] = ip
                            else:
                                ip_mac = {}
                                ip_mac[ip] = mac
                                area_ip_mac[area] = ip_mac
                                mac_ip = {}
                                mac_ip[mac] = ip
                                area_mac_ip[area] = mac_ip
                    f.close()
    return [area_ip_mac, area_mac_ip]

def node_ip_mac(neb_info):
    area_node_mac_ip = {}
    for area in neb_info:
        nodes_information = neb_info.get(area).get("nodes_information")
        if nodes_information:
            node_mac_ip = {}
            for node in nodes_information:
                # if node == "not_advertised(00:10:9b:25:18:e2)":
                #     print("node - " + node)
                mac_ip = []
                # print("node - " + node)
                # if node == "10.96.139.220":
                #     print("node - "+node)
                general = nodes_information.get(node).get("general")
                if general:
                    mac_base = general.get("base_address")
                    # if mac_base and re.match("\\d+\\.\\d+\\.\\d+\\.\\d+", node):
                    #     mac_ip.append([normalize_mac(mac_base), node])
                    # else:

                    ip = None
                    if re.match("\\d+\\.\\d+\\.\\d+\\.\\d+", node):
                        ip = node
                    result = re.match(".*(1\\d+\\.\\d+\\.\\d+\\.\\d+).*", node)
                    if result:
                        if result.group(1):
                            ip = result.group(1)
                    else:
                        result = re.match(".*(2\\d+\\.\\d+\\.\\d+\\.\\d+).*", node)
                        if result:
                            if result.group(1):
                                ip = result.group(1)
                    mac = None
                    if mac_base:
                        mac = mac_base
                    result = re.match(".*([0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}[:-][0-9A-Fa-f]{2}).*", node)
                    if result:
                        if result.group(1):
                            mac = result.group(1)

                    if mac and ip:
                        mac_ip.append([mac, ip])
                    elif mac and not ip:
                        if area_mac_ip.get(area) and area_mac_ip.get(area).get(mac):
                            ip = area_mac_ip.get(area).get(mac)
                            mac_ip.append([mac, ip])
                            # print("Find ip for mac: "+mac+"/"+ip+" area - " + area)
                        else:
                            mac_ip.append([mac, "unknown_ip"])
                            # print("Not find ip for mac: "+mac+" area - "+area)
                    elif not mac and ip:
                        if area_ip_mac.get(area) and area_ip_mac.get(area).get(ip):
                            mac = area_ip_mac.get(area).get(ip)
                            mac_ip.append([mac, ip])
                            # print("Find mac for ip: " + mac + "/" + ip + " area - " + area)
                        else:
                            mac_ip.append(["unknown_mac", ip])
                            # print("Not find mac for ip: "+ip+" area - " + area)

                interfaces = nodes_information.get(node).get("interfaces")
                if interfaces:
                    for iface in interfaces:
                        ip_list = interfaces[iface].get("ip")
                        mac = interfaces[iface].get("mac")
                        if ip_list and mac:
                            mac = normalize_mac(mac)
                            for ip in ip_list:
                                ip = ip.split(' ')[0].split('/')[0]
                                if ip == node or normalize_mac(mac_base) == mac:
                                    del mac_ip[0]
                                    mac_ip.append([mac, ip])
                                else:
                                    mac_ip.append([mac, ip])
                        elif ip_list and not mac:
                            for ip in ip_list:
                                ip = ip.split(' ')[0].split('/')[0]
                                if ip != node:
                                    mac_ip.append(["unknown_mac", ip])
                        elif not ip_list and mac:
                            mac = normalize_mac(mac)
                            if normalize_mac(mac_base) == mac:
                                del mac_ip[0]
                                mac_ip.append([mac, node])
                            else:
                                mac_ip.append([mac, "unknown_ip"])
                if mac_ip:
                    node_mac_ip[node] = mac_ip
            if node_mac_ip:
                area_node_mac_ip[area] = node_mac_ip
    return area_node_mac_ip

def normalize_mac(mac):
    result = ""
    if mac:
        mac = mac.replace(":", "").replace(".", "").replace("-", "").lower()
        if len(mac) == 12:
            result = mac[0]+mac[1]+":"+mac[2]+mac[3]+":"+mac[4]+mac[5]+":"+mac[6]+mac[7]+":"+mac[8]+mac[9]+":"+mac[10]+mac[11];
    return result

######################################################
logging.basicConfig(filename="PostScript.log", format='%(asctime)s - %(message)s', level=logging.DEBUG)

[area_ip_mac, area_mac_ip] = get_raw_ip_mac(raw_ip_mac_path)

# logging.info("Starting get Neb node info ...")
print("Starting get Neb node info ...")
if DEBUG: logging.debug("Starting get Neb node info ...")
neb_info = get_neb_info()
# logging.info("Stop get Neb node info.")
print("Stop get Neb node info.")
if DEBUG: logging.debug("Stop get Neb node info.")

area_node_mac_ip = node_ip_mac(neb_info)

out1 = open("no.out", "w", encoding='utf-8')
out2 = open("select.out", "w", encoding='utf-8')

area_node_delete = {}
for area in neb_info:
    # if area != 'area_chermk':
    #     continue
    # logging.info("Starting area - " + area + " ...")
    print("Starting area - " + area + " ...")
    if DEBUG: logging.debug("Starting area - " + area + " ...")
    nodes_information = neb_info.get(area).get("nodes_information")
    # node_protocol_accounts = neb_info.get(area).get("node_protocol_accounts")
    links = neb_info.get(area).get("links")
    mac_ip_port = neb_info.get(area).get("mac_ip_port")

    nodes_delete = []
    if nodes_information:
        for node in nodes_information:
            # print("node - "+node)
            # if node == "10.96.64.88":
            #     print("node - " + node)
            general = nodes_information.get(node).get("general")
            if general:
                sysdescription = nodes_information.get(node).get("general").get("sysDescription")
                model = nodes_information.get(node).get("general").get("model")
                if not (sysdescription == "Axis camera" or sysdescription == "Beward camera" or model == "AP"):
                    sys_ident = ""
                    if sysdescription:
                        sys_ident = sysdescription
                    if model:
                        sys_ident = model
                    if sys_ident:
                        found = False
                        for filter in filters:
                            p = re.compile(filter.lower())
                            if p.match(sys_ident.lower()):
                                found = True
                                break
                        if found:
                            # found_mip = False
                            # for mip in mac_ip_port:
                            #     if mip[2] == node and mip[1] != mip[2]:
                            #         found_mip = True
                            #         break
                            neighbour_node = {}
                            if links:
                                for link in links:
                                    if link[0] == node:
                                        neighbour_node[link[3]] = link[3]
                                    if link[3] == node:
                                        neighbour_node[link[0]] = link[0]

                            # if not found_mip and num_link <= 1:
                            if len(neighbour_node) <= 1:
                                nodes_delete.append(node)
                                # print("node delete: " + node + ": " + str(general))
                                out1.write("node: " + node + ": " + str(general)+'\n')
                            else:
                                out2.write("node: " + node + ": " + str(general) + '\n')
                        else:
                            out2.write("node: " + node + ": " + str(general)+'\n')

                    # filter am mac address
                    base_address = nodes_information.get(node).get("general").get("base_address")
                    if base_address:
                        found = False
                        for filter in exclude_mac:
                            p = re.compile(filter.lower())
                            if p.match(base_address.lower()):
                                found = True
                                break
                        if found:
                            # found_mip = False
                            # for mip in mac_ip_port:
                            #     if mip[2] == node and mip[1] != mip[2]:
                            #         found_mip = True
                            #         break
                            neighbour_node = {}
                            if links:
                                for link in links:
                                    if link[0] == node:
                                        neighbour_node[link[3]] = link[3]
                                    if link[3] == node:
                                        neighbour_node[link[0]] = link[0]

                            # if not found_mip and num_link <= 1:
                            if len(neighbour_node) <= 1:
                                nodes_delete.append(node)
                                # print("node delete: " + node + ": " + str(general))
                                out1.write("node: " + node + ": " + str(general)+'\n')
                            else:
                                out2.write("node: " + node + ": " + str(general) + '\n')
                        else:
                            out2.write("node: " + node + ": " + str(general)+'\n')

                    # filter am sysname
                    sysname = nodes_information.get(node).get("general").get("sysname")
                    if sysname:
                        found = False
                        for filter in exclude_sysname:
                            p = re.compile(filter.lower())
                            if p.match(sysname.lower()):
                                found = True
                                break
                        if found:
                            # found_mip = False
                            # for mip in mac_ip_port:
                            #     if mip[2] == node and mip[1] != mip[2]:
                            #         found_mip = True
                            #         break
                            neighbour_node = {}
                            if links:
                                for link in links:
                                    if link[0] == node:
                                        neighbour_node[link[3]] = link[3]
                                    if link[3] == node:
                                        neighbour_node[link[0]] = link[0]

                            # if not found_mip and num_link <= 1:
                            if len(neighbour_node) <= 1:
                                nodes_delete.append(node)
                                # print("node delete: " + node + ": " + str(general))
                                out1.write("node: " + node + ": " + str(general)+'\n')
                            else:
                                out2.write("node: " + node + ": " + str(general) + '\n')
                        else:
                            out2.write("node: " + node + ": " + str(general)+'\n')
                    else:
                        # found_mip = False
                        # for mip in mac_ip_port:
                        #     if mip[2] == node and mip[1] != mip[2]:
                        #         found_mip = True
                        #         break
                        neighbour_node = {}
                        if links:
                            for link in links:
                                if link[0] == node:
                                    neighbour_node[link[3]] = link[3]
                                if link[3] == node:
                                    neighbour_node[link[0]] = link[0]

                        # if not found_mip and num_link <= 1:
                        if len(neighbour_node) <= 1:
                            nodes_delete.append(node)
                            # print("node delete: " + node + ": " + str(general))
                            out1.write("node: " + node + ": " + str(general) + '\n')
    if nodes_delete:
        area_node_delete[area] = nodes_delete



out1.close()
out2.close()

for area in area_node_delete:
    links = neb_info.get(area).get("links")
    mac_ip_port = neb_info.get(area).get("mac_ip_port")
    for node in area_node_delete[area]:
        # print(node)
        # if node == "not_advertised(00:10:9b:25:18:e2)":
        #     print("node - " + node)
        # delete node
        url_node_delete = "http://"+neb_server+":"+neb_server_port+"/delete?file=neb.map.pre&key=/"+area+"/nodes_information/"+node
        # print(url_node_delete)
        result = requests.get(url_node_delete, headers={"user": user, "passwd": passwd}, verify=False)

        # get clients from deleted nodes to mac_ip_port
        mac_ip_node_iface = []
        for mip in mac_ip_port:
            if mip[2] == node and mip[1] != mip[2]:
                mac_ip_node_iface.append(mip)

        # delete from links and add to mac_ip_port
        if neb_info.get(area):
            if links:
                for link in links:
                    if link[0] == node or link[3] == node:
                        url_link_delete = "http://"+neb_server+":"+neb_server_port+"/del_from_list?file=neb.map.pre&key=/"+area+"/links"
                        data = link[0]+';'+link[1]+';'+link[2]+';'+link[3]+';'+link[4]+';'+link[5]
                        result = requests.post(url_link_delete, data, headers={"user": user, "passwd": passwd}, verify=False)

                    if link[0] == node:
                        if area_node_mac_ip.get(area) and area_node_mac_ip.get(area).get(node):
                            mac_ip_list = area_node_mac_ip.get(area).get(node)
                            for m_i in mac_ip_list:
                                mip = []
                                mip.append(m_i[0])
                                mip.append(m_i[1])
                                mip.append(link[3])
                                mip.append(link[4])
                                mip.append(link[5])
                                mip.append("1")
                                url_mac_ip_port_add = "http://"+neb_server+":"+neb_server_port+"/add_to_list?file=neb.map.pre&key=/"+area+"/mac_ip_port"
                                data = json.dumps(mip)
                                result = requests.post(url_mac_ip_port_add, data, headers={"user": user, "passwd": passwd}, verify=False)

                            # adding clients from deleted nodes to mac_ip_port
                            if mac_ip_node_iface:
                                for m_i in mac_ip_node_iface:
                                    # delete old clients from mac_ip_port
                                    url_mac_ip_port_delete = "http://" + neb_server + ":" + neb_server_port + "/del_from_list?file=neb.map.pre&key=/" + area + "/mac_ip_port"
                                    data = m_i[0] + ';' + m_i[1] + ';' + m_i[2] + ';' + m_i[3] + ';' + m_i[4] + ';' + m_i[5]
                                    result = requests.post(url_mac_ip_port_delete, data, headers={"user": user, "passwd": passwd}, verify=False)
                                    # adding new clients to mac_ip_port
                                    mip = []
                                    mip.append(m_i[0])
                                    mip.append(m_i[1])
                                    mip.append(link[3])
                                    mip.append(link[4])
                                    mip.append(link[5])
                                    mip.append("1")
                                    url_mac_ip_port_add = "http://" + neb_server + ":" + neb_server_port + "/add_to_list?file=neb.map.pre&key=/" + area + "/mac_ip_port"
                                    data = json.dumps(mip)
                                    result = requests.post(url_mac_ip_port_add, data, headers={"user": user, "passwd": passwd},
                                                           verify=False)


                    if link[3] == node:
                        if area_node_mac_ip.get(area) and area_node_mac_ip.get(area).get(node):
                            mac_ip_list = area_node_mac_ip.get(area).get(node)
                            for m_i in mac_ip_list:
                                mip = []
                                mip.append(m_i[0])
                                mip.append(m_i[1])
                                mip.append(link[0])
                                mip.append(link[1])
                                mip.append(link[2])
                                mip.append("1")
                                url_mac_ip_port_add = "http://"+neb_server+":"+neb_server_port+"/add_to_list?file=neb.map.pre&key=/"+area+"/mac_ip_port"
                                data = json.dumps(mip)
                                result = requests.post(url_mac_ip_port_add, data, headers={"user": user, "passwd": passwd}, verify=False)

                            # adding clients from deleted nodes to mac_ip_port
                            if mac_ip_node_iface:
                                for m_i in mac_ip_node_iface:
                                    # delete old clients from mac_ip_port
                                    url_link_delete = "http://" + neb_server + ":" + neb_server_port + "/del_from_list?file=neb.map.pre&key=/" + area + "/mac_ip_port"
                                    data = m_i[0] + ';' + m_i[1] + ';' + m_i[2] + ';' + m_i[3] + ';' + m_i[4] + ';' + m_i[5]
                                    result = requests.post(url_link_delete, data, headers={"user": user, "passwd": passwd}, verify=False)
                                    # adding new clients to mac_ip_port
                                    mip = []
                                    mip.append(m_i[0])
                                    mip.append(m_i[1])
                                    mip.append(link[0])
                                    mip.append(link[1])
                                    mip.append(link[2])
                                    mip.append("1")
                                    url_mac_ip_port_add = "http://" + neb_server + ":" + neb_server_port + "/add_to_list?file=neb.map.pre&key=/" + area + "/mac_ip_port"
                                    data = json.dumps(mip)
                                    result = requests.post(url_mac_ip_port_add, data, headers={"user": user, "passwd": passwd},
                                                           verify=False)

commit_url = "http://"+neb_server+":"+neb_server_port+"/commit"
res = requests.get(commit_url, headers={"user": user, "passwd": passwd}, verify=False)

# print("Wait...")
# time.sleep(60)
# print("End.")