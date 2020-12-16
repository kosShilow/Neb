package ru.kos.snmp_information;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.kos.neb.neb_lib.*;
import json.JSON;

public class SNMP_information {
    private final Map<String, String> oids = new HashMap<>();
    private static int timeout=10; // 3 sec.
    private static int retries=2;
    private static int port=161;
    public static String cmd = "";
    private static Log log;
    
    private static boolean DEBUG = false;
    private static boolean CHECK_PARENT = true;

    public static void main(String[] args) {
        log = new Log("out.out");
        log.SetLevel(log.DEBUG);        
        
        BufferedReader br_STDIN = null;
        SNMP_information snmp_information = new SNMP_information();
        if(DEBUG) ru.kos.neb.neb_lib.Utils.DEBUG=true;
        else ru.kos.neb.neb_lib.Utils.DEBUG=false;
        if(CHECK_PARENT) {
            Check_Parent check_Parent = new Check_Parent();
            check_Parent.start();
        }        
        try {
            br_STDIN = new BufferedReader(new InputStreamReader(System.in));
            while (true) {     
                try {
                    if(DEBUG) System.out.print("Type command: ");
                    cmd = br_STDIN.readLine();
                    if(DEBUG) System.out.println("command: "+cmd);                    

                    if(cmd != null && !cmd.equals("")) {
                        log.Println("command: "+cmd, log.DEBUG); 
                        if ("QUIT".equals(cmd)) {
                            log.Println("Exit!", log.DEBUG);
                            System.out.println("Exit!");
                            System.exit(0);
                        }
                        String[] mas = cmd.split("\\s+");
                        if(mas.length == 3) {
                            String node = mas[0];
                            String protocol = mas[1];
                            String command = mas[2];
                            String output = "";
                            if(protocol.toLowerCase().equals("snmp")) {
                                command = command.replace("\\;", "<REPLACE_SEMICOLON>").replace("\\:", "<REPLACE_COLON>");
                                String[] mas1 = command.split(";");
                                for(String account : mas1) {
                                    String[] mas2 = account.split(":");
                                    if(mas2.length >= 2 && mas2.length <= 4) {
                                        String community = mas2[0].replace("<REPLACE_SEMICOLON>", ";").replace("<REPLACE_COLON>", ":");
                                        String version = mas2[1].replace("<REPLACE_SEMICOLON>", ";").replace("<REPLACE_COLON>", ":");
                                        if(mas2.length >= 3) {
                                            timeout=Integer.parseInt(mas2[2]);
                                        }
                                        if(mas2.length >= 4) retries=Integer.parseInt(mas2[3]);

                                        String[] mas3 = new String[3];
                                        mas3[0]=node;
                                        mas3[1]=community;
                                        mas3[2]=version;

                                        ArrayList<String[]> node_community_version = new ArrayList();
                                        node_community_version.add(mas3);

                                        Map out1 = new HashMap();
                                        if(protocol.toLowerCase().equals("snmp")) out1 = snmp_information.GetInformationFromNodes(node_community_version);

                                        if(out1.size() > 0) {
                                            Map out = new HashMap();
                                            out.put(node, out1);

                                            JSON jSON = new JSON();
                                    //            String output = cisco_information.PrettyOut(jSON.mapToJSON(out));
                                            output = jSON.mapToJSON(out);

                                            if(!output.equals("")) break;
                                        }
                                    }
                                }
                            }
                            if(!output.equals("")) System.out.println("<result>"+output+"</result>\n");
                            else System.out.println("<result></result>\n");                            
                            log.Println("--- End command: "+cmd, log.DEBUG);
                        } else System.out.println("<result></result>\n");
                    }
                } catch (Exception ex) {
                    for(StackTraceElement it : ex.getStackTrace()) {
                        log.Println("ex="+it.toString(), log.DEBUG);
                    }
                    log.Println("ex="+ex.toString(), log.DEBUG);                    
                    System.out.println("<result></result>\n");
//                    if(DEBUG) ex.printStackTrace();           
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) {  }
            }
        }   
        finally {
            if (br_STDIN != null) {
                try {
                    br_STDIN.close();
                } catch (IOException e) {}
            }
        } 
    }
    
    private Map GetInformationFromNodes(ArrayList<String[]> node_community_version) {
        Map result = new HashMap<>();
        if(DEBUG) System.out.println("Start GetCommonInformationFromNodes ...");
        Map<String, Map<String, String>> commonInformationFromNodes = GetCommonInformationFromNodes(node_community_version);
        if(commonInformationFromNodes.size() > 0) {
            result.putAll(commonInformationFromNodes);
            if(DEBUG) System.out.println("Stop GetCommonInformationFromNodes.");
            if(DEBUG) System.out.println("Start GetWalkInformationFromNodes ...");
            Map<String, Map<String, ArrayList>> walkInformationFromNodes = GetWalkInformationFromNodes(node_community_version);
//            Map<String, ArrayList> ifaceMaping = GetIfaceMaping(node_community_version, walkInformationFromNodes);
//            walkInformationFromNodes.put("IfaceMaping", ifaceMaping);
            if(DEBUG) System.out.println("Stop GetWalkInformationFromNodes.");
            Map<String, String> serialnumber_map = GetSerialNumberFromNodes(walkInformationFromNodes);
            if(serialnumber_map != null && serialnumber_map.size() > 0) {
                Map general_map = (Map)result.get("general");
                if(general_map != null) {
                    general_map.put("serialnumber", serialnumber_map);
                }
            }
            if(DEBUG) System.out.println("Start GetDuplexMode ...");
            Map<String, Map<String, String>> duplex_mode = GetDuplexMode(walkInformationFromNodes);
            if(DEBUG) System.out.println("Stop GetDuplexMode.");
            if(DEBUG) System.out.println("Start GetIpAddressFromInterface ...");
            Map<String, Map<String, ArrayList<String>>> interface_ipaddress = GetIpAddressFromInterface(walkInformationFromNodes);
            if(DEBUG) System.out.println("Stop GetIpAddressFromInterface.");
            if(DEBUG) System.out.println("Start GetVlanInform ...");
            Map<String, Map<String, String>> getVlanInform = GetVlanInform(walkInformationFromNodes);
            if(DEBUG) System.out.println("Stop GetVlanInform.");
            if(DEBUG) System.out.println("Start GetVlanPortUntagTag ...");
            Map<String, Map<String, String[]>> getVlanPortUntagTag = GetVlanPortUntagTag(walkInformationFromNodes);
            if(DEBUG) System.out.println("Stop GetVlanPortUntagTag.");

            if(DEBUG) System.out.println("Start GetInterfacesInformationFromNodes ...");
            Map<String, Map<String, String>> getInterfacesInformationFromNodes = GetInterfacesInformationFromNodes(
                    walkInformationFromNodes, duplex_mode, interface_ipaddress, getVlanPortUntagTag);
            result.put("interfaces", getInterfacesInformationFromNodes);
            if(DEBUG) System.out.println("Stop GetInterfacesInformationFromNodes.");
            if(DEBUG) System.out.println("Start GetRoutesInformationFromNodes ...");
            ArrayList routes_list = new ArrayList();
            Map<String, String> getRoutesInformationFromNodes = GetRoutesInformationFromNodes(walkInformationFromNodes);
            for (Map.Entry<String, String> entry : getRoutesInformationFromNodes.entrySet()) {
                String val=entry.getValue();
                String[] mas=val.split("\\|", -1);
                for(String str : mas) {
                    String[] mas1=str.split(",");
                    if(mas1.length == 6) {
                        routes_list.add(mas1[0]+" "+mas1[5]+" "+mas1[1]);
                    }
                }

            }
            result.put("routes", routes_list);
            if(DEBUG) System.out.println("Stop GetRoutesInformationFromNodes.");
            if(DEBUG) System.out.println("Start GetVlansInformationFromNodes ...");
            Map<String, String> getVlansInformationFromNodes = GetVlansInformationFromNodes(getVlanInform);
            Map vlans_map = new HashMap<>();
            for (Map.Entry<String, String> entry : getVlansInformationFromNodes.entrySet()) {
                String val=entry.getValue();
                String[] mas=val.split("\\|", -1);
                for(String str : mas) {
                    String[] mas1=str.split(",");
                    if(mas1.length == 2) vlans_map.put(mas1[0], mas1[1]);
                }
            }
            result.put("vlans", vlans_map);
            if(DEBUG) System.out.println("Stop GetVlansInformationFromNodes.");

            if(DEBUG) System.out.println("Start get discoverer protocol...");
            Map<String, ArrayList<Map<String, String>>> dplinks = GetDP(commonInformationFromNodes, walkInformationFromNodes);
            if(dplinks.size() > 0) {
                if(result.get("advanced") != null) {
                    ((Map)result.get("advanced")).put("links", dplinks);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("links", dplinks);
                    result.put("advanced", tmp);
                }
            }            
            if(DEBUG) System.out.println("Stop get discoverer protocol.");
            
            if(DEBUG) System.out.println("Start GetARP...");
            Map<String, String> arp_table = GetARP(node_community_version);
            if(arp_table.size() > 0) {
                if(result.get("advanced") != null) {
                    ((Map)result.get("advanced")).put("arp", arp_table);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("arp", arp_table);
                    result.put("advanced", tmp);
                }
            }
            if(DEBUG) System.out.println("Stop GetARP.");
            
//            System.out.println("Start GetMAC...");
//            Map<String, ArrayList> mac = GetMAC(node_community_version, walkInformationFromNodes);
//            result.put("mac", mac);
//            System.out.println("Stop GetMAC...");            
        }
        
        return result;
    }
    
    // output: node ---> ArrayList(String,String)
    private Map<String, Map<String, String>> GetCommonInformationFromNodes(ArrayList<String[]> node_community_version) {
        String sysDescription = "1.3.6.1.2.1.1.1.0";
        String sysUptime = "1.3.6.1.2.1.1.3.0";
        String sysContact = "1.3.6.1.2.1.1.4.0";
        String sysName = "1.3.6.1.2.1.1.5.0";
        String sysLocation = "1.3.6.1.2.1.1.6.0";
        String defaultTTL = "1.3.6.1.2.1.4.2.0";
        String dot1dBaseBridgeAddress = "1.3.6.1.2.1.17.1.1.0";
        ArrayList oid_list = new ArrayList();
        oid_list.add(sysDescription);
        oid_list.add(sysUptime);
        oid_list.add(sysContact);
        oid_list.add(sysName);
        oid_list.add(sysLocation);
        oid_list.add(defaultTTL);
        oid_list.add(dot1dBaseBridgeAddress);

        Map<String, Map<String, String>> result = new HashMap<>();
        
        GetPool getPool = new GetPool();
        Map<String, ArrayList> res = getPool.Get(node_community_version, oid_list, port, timeout/4, retries);
        for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> map_tmp = new HashMap<>();
            for (String[] val : val_list) {
                if(val[0].equals(sysDescription)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("sysDescription", HexStringToUTF8(val[1]));
                } else if(val[0].equals(sysUptime)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("uptime", val[1]);                
                } else if(val[0].equals(sysContact)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("contact", val[1]);                
                } else if(val[0].equals(sysName)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("sysname", val[1]);                
                } else if(val[0].equals(sysLocation)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("syslocation", HexStringToUTF8(val[1]));                
                } else if(val[0].equals(defaultTTL)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("ttl", val[1]);                
                } else if(val[0].equals(dot1dBaseBridgeAddress)) {
                    val[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(val[1]));
                    if(!val[1].equals("")) map_tmp.put("base_address", val[1]);                
                }
            }
            if(map_tmp.size() > 0) result.put("general", map_tmp);
        }
        
        return result;
    }
    
    // output: oid_key ---> node ---> ArrayList(String,String) 
    private Map<String, Map<String, ArrayList>> GetWalkInformationFromNodes(ArrayList<String[]> node_community_version) {
//        Map<String, String> oids = new HashMap<>();
        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
        oids.put("ifMTU", "1.3.6.1.2.1.2.2.1.4");
        oids.put("ifSpeed", "1.3.6.1.2.1.2.2.1.5");
        oids.put("ifMAC", "1.3.6.1.2.1.2.2.1.6");
        oids.put("ifAdminStatus", "1.3.6.1.2.1.2.2.1.7");
        oids.put("ifOperStatus", "1.3.6.1.2.1.2.2.1.8");
        oids.put("ifIpAddress", "1.3.6.1.2.1.4.20.1.2");
        oids.put("ifIpNetMask", "1.3.6.1.2.1.4.20.1.3");
        oids.put("NetRoute", "1.3.6.1.2.1.4.21.1.1");
        oids.put("RouteMetric", "1.3.6.1.2.1.4.21.1.3");
        oids.put("RouteDestination", "1.3.6.1.2.1.4.21.1.7");
        oids.put("RouteType", "1.3.6.1.2.1.4.21.1.8");
        oids.put("RouteProto", "1.3.6.1.2.1.4.21.1.9");
        oids.put("RouteAge", "1.3.6.1.2.1.4.21.1.10");
        oids.put("RouteMask", "1.3.6.1.2.1.4.21.1.11");
        oids.put("IdNameVlan", "1.3.6.1.2.1.17.7.1.4.3.1.1");
        oids.put("IdVlanToNumberInterface", "1.3.6.1.2.1.16.22.1.1.1.1.4.1.3.6.1.2.1.16.22.1.4.1");
        oids.put("TaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.4");
        oids.put("UnTaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.5");
        oids.put("IdNameVlanCisco", "1.3.6.1.4.1.9.9.46.1.3.1.1.4.1");
        oids.put("IdVlanToNumberInterfaceCisco", "1.3.6.1.4.1.9.9.128.1.1.1.1.3");
        oids.put("VlanType", "1.3.6.1.4.1.9.9.46.1.6.1.1.3");
        oids.put("VlanPortAccessModeCisco", "1.3.6.1.4.1.9.9.68.1.2.1.1.2");

        oids.put("PortTrunkNativeVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.5");
        oids.put("PortTrunkVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.11");
        oids.put("vlanTrunkPortDynamicStatus","1.3.6.1.4.1.9.9.46.1.6.1.1.14");
        oids.put("VlanNameHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.2");
        oids.put("VlanIdHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.5");
        oids.put("VlanPortStateHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.8.1.1");
        oids.put("VlanNameTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.2");
        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("Duplex_Allied", "1.3.6.1.4.1.207.8.10.3.1.1.5");
        oids.put("Duplex_Asante", "1.3.6.1.4.1.298.1.5.1.2.6.1.7");
        oids.put("Duplex_Dell", "1.3.6.1.4.1.89.43.1.1.4");
        oids.put("Duplex_Foundry", "1.3.6.1.4.1.1991.1.1.3.3.1.1.4");
        oids.put("Duplex_Cisco2900", "1.3.6.1.4.1.9.9.87.1.4.1.1.32");
        oids.put("Duplex_HP", "1.3.6.1.2.1.26.2.1.1.3");
        oids.put("Duplex_Cisco", "1.3.6.1.2.1.10.7.2.1.19");
        oids.put("IpAddress", "1.3.6.1.2.1.4.20.1.1");
        oids.put("lldpRemManAddrIfId", "1.0.8802.1.1.2.1.4.2.1.4");
        oids.put("lldpRemPortId", "1.0.8802.1.1.2.1.4.1.1.7");
        oids.put("lldpRemChassisId","1.0.8802.1.1.2.1.4.1.1.5");
        oids.put("lldpRemManAddrIfSubtype","1.0.8802.1.1.2.1.4.2.1.3");
        oids.put("lldpRemSysName","1.0.8802.1.1.2.1.4.1.1.9");
        oids.put("lldpRemSysDesc", "1.0.8802.1.1.2.1.4.1.1.10");
        oids.put("ldpLocPortId", "1.0.8802.1.1.2.1.3.7.1.3");
        oids.put("cdpCacheAddress", "1.3.6.1.4.1.9.9.23.1.2.1.1.4");
        oids.put("cdpCacheDevicePort", "1.3.6.1.4.1.9.9.23.1.2.1.1.7");
        oids.put("cdpRemSysName", "1.3.6.1.4.1.9.9.23.1.2.1.1.6");
        oids.put("cdpRemSysDesc", "1.3.6.1.4.1.9.9.23.1.2.1.1.8");
        oids.put("IfaceMaping", "1.3.6.1.2.1.17.1.4.1.2");
        oids.put("entPhysicalDescr", "1.3.6.1.2.1.47.1.1.1.1.2");
        oids.put("entPhysicalSerialNumber", "1.3.6.1.2.1.47.1.1.1.1.11");
        oids.put("cisco-vlan-membership", "1.3.6.1.4.1.9.9.68.1.2.2.1.2");

        Map<String, Map<String, ArrayList>> result = new HashMap<>();

        WalkPool walkPool = new WalkPool();
        for (Map.Entry<String, String> entry : oids.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if(DEBUG) System.out.println("key="+key);
            Map<String, ArrayList> res = walkPool.Get(node_community_version, val, port, timeout, retries);
            Map tmp = new HashMap(res);
            result.put(key, tmp);
        }
        return result;
    }    
    
//    private Map<String, ArrayList> GetIfaceMaping(ArrayList<String[]> node_community_version, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
////        Map<String, String> result = new HashMap();
//        String ifaceMaping = "1.3.6.1.2.1.17.1.4.1.2";
//        
//        ArrayList<String[]> tmp_node_community_version = new ArrayList();
//        for(String[] item : node_community_version) {
//            ArrayList<String> getVlanCommunity = GetVlanCommunity(walkInformationFromNodes, item[0]);
//            if(getVlanCommunity.size() > 0) {
//                for(String item1 : getVlanCommunity) {
//                    String[] mas = new String[3];
//                    mas[0]=item[0];
//                    mas[1]=item1;
//                    mas[2]=item[2];
//                    tmp_node_community_version.add(mas);
//                }
//            } else {
//                    String[] mas = new String[3];
//                    mas[0]=item[0];
//                    mas[1]=item[1];
//                    mas[2]=item[2];
//                    tmp_node_community_version.add(mas);
//            }
//        }
//        
//        WalkPool walkPool = new WalkPool();
//        Map<String, ArrayList> res = walkPool.Get(tmp_node_community_version, ifaceMaping);
//
//        return res;
//    }
    
    // Output format: ArrayList(String)
    private ArrayList GetVlanCommunity(Map<String, Map<String, ArrayList>> walkInformationFromNodes, String node) {
        ArrayList result = new ArrayList();
        
        ArrayList<String[]> res = walkInformationFromNodes.get("VlanCommunity").get(node);
        if(res != null) {
            res = SetUniqueList(res, 1);
            for(String[] item : res) {
                String[] mas = item[1].split(":");
                String community = "";
                if(mas.length > 1) {
                    for(String item1 : mas) {
                        String ch = convertHexToString(item1);
                        community=community+ch;
                    }
                } else community=item[1];
                if(!community.equals("") && !community.startsWith("@")) result.add(community);
            }
        }
        return result;
    }  
    
    //  output: node ---> id_iface ---> duplex_mode
    private Map<String, Map<String, String>> GetDuplexMode(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        if (walkInformationFromNodes.get("Duplex_Allied") != null && walkInformationFromNodes.get("Duplex_Allied").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_Allied").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    if (val[1].equals("1")) {
                        val[1] = "full-duplex";
                    } else if (val[1].equals("2")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Asante") != null && walkInformationFromNodes.get("Duplex_Asante").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_Asante").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    if (val[1].equals("3")) {
                        val[1] = "full-duplex";
                    } else if (val[1].equals("2")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Dell") != null && walkInformationFromNodes.get("Duplex_Dell").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_Dell").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    if (val[1].equals("2")) {
                        val[1] = "full-duplex";
                    } else if (val[1].equals("1")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Foundry") != null && walkInformationFromNodes.get("Duplex_Foundry").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_Foundry").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    if (val[1].equals("3")) {
                        val[1] = "full-duplex";
                    } else if (val[1].equals("2")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Cisco2900") != null && walkInformationFromNodes.get("Duplex_Cisco2900").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_Cisco2900").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    if (val[1].equals("1")) {
                        val[1] = "full-duplex";
                    } else if (val[1].equals("2")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_HP") != null && walkInformationFromNodes.get("Duplex_HP").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_HP").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 2];
                    String var = val[1].split("\\.")[val[1].split("\\.").length - 1];
                    if (var.equals("11") || var.equals("13") || var.equals("16") || var.equals("18") || var.equals("20")) {
                        val[1] = "full-duplex";
                    } else if (var.equals("10") || var.equals("12") || var.equals("15") || var.equals("17") || var.equals("19")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        if (walkInformationFromNodes.get("Duplex_Cisco") != null && walkInformationFromNodes.get("Duplex_Cisco").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("Duplex_Cisco").entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp = new HashMap<>();
                for (String[] val : val_list) {
                    String id_iface = val[0].split("\\.")[val[0].split("\\.").length - 1];
                    if (val[1].equals("3")) {
                        val[1] = "full-duplex";
                    } else if (val[1].equals("2")) {
                        val[1] = "half-duplex";
                    } else {
                        val[1] = "unknown";
                    }
                    tmp.put(id_iface, val[1]);
                }
                result.put(node, tmp);
            }
        }
        return result;
    }    
    
    // Output format node ---> id_iface ---> ip, mask    
    private Map<String, Map<String, ArrayList<String>>> GetIpAddressFromInterface(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, ArrayList<String>>> result = new HashMap<>();
        Map<String, ArrayList<String[]>> id_ip = new HashMap<>();
        Map<String, ArrayList<String[]>> ip_mask = new HashMap<>();

        // output: node ---> ArrayList(id, ip)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIndex").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> val_list_sec = walkInformationFromNodes.get("ifIpAddress").get(node);
            ArrayList<String[]> tmp_list = new ArrayList<>();
            for (String[] item1 : val_list) {
                if(val_list_sec != null) {
                    boolean find=false;
                    for (String[] item2 : val_list_sec) {
                        if (item1[1].equals(item2[1])) {
                            String[] tmp = item2[0].split("\\.");
                            String[] mas = new String[2];
                            mas[0] = item1[1];
                            mas[1] = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
                            tmp_list.add(mas);
                            find=true;
//                            break;
                        }
                    }
                }
            }

            id_ip.put(node, tmp_list);
        }

        // output: node ---> ArrayList(ip, mask)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIpNetMask").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> tmp_list = new ArrayList<>();
            for (String[] item1 : val_list) {
                String[] mas = new String[2];
                String[] tmp = item1[0].split("\\.");
                mas[0] = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
                mas[1] = item1[1];
                tmp_list.add(mas);
            }
            ip_mask.put(node, tmp_list);

        }

        // output: node --> id ---> ip, mask
        for (Map.Entry<String, ArrayList<String[]>> entry : id_ip.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> val_list_sec = ip_mask.get(node);
            ArrayList<String[]> tmp_list = new ArrayList();
            for (String[] item1 : val_list) {
                if(val_list_sec != null) {
                    for (String[] item2 : val_list_sec) {
                        if (item1[1].equals(item2[0])) {
                            String[] mas = new String[3];
                            mas[0] = item1[0];
                            mas[1] = item1[1];
                            mas[2] = item2[1];
                            tmp_list.add(mas);
//                            break;
                        }
                    }
                }
            }
            
            Map<String, ArrayList<String>> tmp_map1 = new HashMap<>();
            for (int i=0; i<tmp_list.size(); i++) {
                ArrayList<String> tmp_tmp_list = new ArrayList();
                String[] mas1=tmp_list.get(i);
                tmp_tmp_list.add(mas1[1]+" "+mas1[2]);
                for (int j=i+1; j<tmp_list.size(); j++) {
                    String[] mas2=tmp_list.get(j);
                    if(mas1[0].equals(mas2[0])) {
                        String[] mas_tmp1 = new String[2];
                        mas_tmp1[0]=mas2[1]; mas_tmp1[1]=mas2[2];
                        tmp_tmp_list.add(mas2[1]+" "+mas2[2]);
                        tmp_list.remove(j);
                        j=j-1;
                    }
                }
                tmp_map1.put(mas1[0], tmp_tmp_list);
            }
            
            result.put(node, tmp_map1);
        }

        return result;
    }
    
    private Map<String, Map<String, String>> GetVlanInform(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        Map<String, Map<String, String>> getVlanInformCisco = GetVlanInformCisco(walkInformationFromNodes);
        Map<String, Map<String, String>> getVlanInformHP = GetVlanInformHP(walkInformationFromNodes);
        Map<String, Map<String, String>> getVlanInformRFC2674 = GetVlanInformRFC2674(walkInformationFromNodes);

        result.putAll(getVlanInformCisco);
        result.putAll(getVlanInformHP);
        result.putAll(getVlanInformRFC2674);
        return result;
    }    
    
    //  output: node ---> id_iface ---> name_vlan
    private Map<String, Map<String, String>> GetVlanInformCisco(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // get node ---> id ---> namevlan
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("IdNameVlanCisco").entrySet()) {
            Map<String, String> id_namevlan = new HashMap<>();
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> tmp_list = new ArrayList<>();
            for (String[] item1 : val_list) {
                String[] tmp = item1[0].split("\\.");
                String id = tmp[tmp.length - 1];
                String namevlan = item1[1];
                id_namevlan.put(id, namevlan);
            }
            result.put(node, id_namevlan);
        }

        return result;
    }    
    
    //  output: node ---> id_iface ---> name_vlan
    private Map<String, Map<String, String>> GetVlanInformHP(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // output: node ---> id_vlan ---> name_vlan)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("VlanIdHP").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> val_list_sec = walkInformationFromNodes.get("VlanNameHP").get(node);
            Map<String, String> id_namevlan = new HashMap<>();
            for (String[] item1 : val_list) {
                String num1 = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                if(val_list_sec != null) {
                    for (String[] item2 : val_list_sec) {
                        String num2 = item2[0].split("\\.")[item2[0].split("\\.").length - 1];
                        if (num1.equals(num2)) {
                            id_namevlan.put(item1[1], item2[1]);
                            break;
                        }
                    }
                }
            }
            result.put(node, id_namevlan);
        }

        return result;
    }    
    
    //  output: node ---> id_iface ---> name_vlan
    private Map<String, Map<String, String>> GetVlanInformRFC2674(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String>> result = new HashMap<>();

        // output: node ---> ArrayList(id_vlan, name_vlan)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("IdNameVlan").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> id_namevlan = new HashMap<>();
            for (String[] item1 : val_list) {
                String id_vlan = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                id_namevlan.put(id_vlan, item1[1]);
            }
            result.put(node, id_namevlan);
        }

        return result;
    }    
    
    // Output format node ---> id_iface ---> untag, tag(vlan1:vlan2...)
    private Map<String, Map<String, String[]>> GetVlanPortUntagTag(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();

        Map<String, Map<String, String[]>> getVlanCisco = GetVlanCisco(walkInformationFromNodes);
        Map getVlanHP = new HashMap();
        Map getVlanRFC2674 = new HashMap();
        if(getVlanCisco.size() == 0) getVlanHP = GetVlanHP(walkInformationFromNodes);
        if(getVlanHP.size() == 0) getVlanRFC2674 = GetVlanRFC2674(walkInformationFromNodes);

        result.putAll(getVlanCisco);
        result.putAll(getVlanHP);
        result.putAll(getVlanRFC2674);
        return result;
    }    
    
    // Output format node ---> id_iface ---> Untaget_vlan, Taget_vlans(vlan1:vlan2, ...)
    private Map<String, Map<String, String[]>> GetVlanCisco(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();
        Map<String, Map<String, String>> node_id_untag = new HashMap<>();
        Map<String, Map<String, String>> node_id_tag = new HashMap<>();

        // output: node ---> id_iface ---> trunk_access
        Map<String, Map<String, String>> node_id_trunk_access_mode = new HashMap<>();
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("vlanTrunkPortDynamicStatus").entrySet()) {       
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> trunk_access = new HashMap<>();
            for (String[] item : val_list) {
                if(item[1].equals("1")) {
                    trunk_access.put(item[0].split("\\.")[item[0].split("\\.").length - 1], "trunk");
                } else {
                    trunk_access.put(item[0].split("\\.")[item[0].split("\\.").length - 1], "access");
                }
            }
            node_id_trunk_access_mode.put(node, trunk_access);
        } 
        
        for(Map.Entry<String, Map<String, String>> entry : node_id_trunk_access_mode.entrySet()) {
            String node = entry.getKey();
//            System.out.println(node);
            Map<String, String> val_list = entry.getValue();
            Map<String, String[]> tmp_map = new HashMap<>();
            for(Map.Entry<String, String> entry1 : val_list.entrySet()) {
                String id_iface = entry1.getKey();
                String trunk_access_mode = entry1.getValue();
//                System.out.println(id_iface+" - "+trunk_access_mode);
                String native_vlan="";
                String tag_vlans="";
                String access_vlan="";
                if(trunk_access_mode.equals("trunk")) {
                    if(walkInformationFromNodes.get("PortTrunkNativeVlanCisco").get(node) != null) {
                        for(String[] item : (ArrayList<String[]>)walkInformationFromNodes.get("PortTrunkNativeVlanCisco").get(node)) {
                            if(id_iface.equals(item[0].split("\\.")[item[0].split("\\.").length - 1])) {
                                native_vlan=item[1]+":trunk";
                                break;
                            }

                        }
                    }
                    
                    if(walkInformationFromNodes.get("PortTrunkVlanCisco").get(node) != null) {
                        for(String[] item : (ArrayList<String[]>)walkInformationFromNodes.get("PortTrunkVlanCisco").get(node)) {
                            if(id_iface.equals(item[0].split("\\.")[item[0].split("\\.").length - 1])) {
                                tag_vlans=HexMapToVlans(item[1]);
                                break;
                            }
                        }
                    }
                } else {
                    if(walkInformationFromNodes.get("cisco-vlan-membership").get(node) != null) {
                        for(String[] item : (ArrayList<String[]>)walkInformationFromNodes.get("cisco-vlan-membership").get(node)) {
                            String id_iface1 = item[0].split("\\.")[item[0].split("\\.").length - 1];
                            String id_vlan = item[1];
                            
                            if(id_iface.equals(id_iface1)) {
                                access_vlan=id_vlan+":access";
                                break;
                            }

                        }
                    }
                }
                
                String[] mas = new String[2];
                if(trunk_access_mode.equals("trunk")) {
                    mas[0] = native_vlan;
                    mas[1] = tag_vlans;                     
                } else {
                    mas[0] = access_vlan;
                    mas[1] = "";                     
                }
                tmp_map.put(id_iface, mas);
            }
            result.put(node, tmp_map);
        }

        return result;
    }
    
    // Output format node ---> id_iface ---> Untaget_vlan, Taget_vlans(vlan1:vlan2, ...)
    private Map<String, Map<String, String[]>> GetVlanHP(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();
        Map<String, Map<String, String>> index_idvlan = new HashMap<>();
        Map<String, ArrayList> index_idifaceUntag = new HashMap<>();
        Map<String, ArrayList> index_idifaceTag = new HashMap<>();
        Map<String, ArrayList> idiface_idvlanUntag = new HashMap<>();
        Map<String, ArrayList> idiface_idvlanTag = new HashMap<>();

        // output: node ---> index ---> id_vlan
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("VlanIdHP").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> map_tmp = new HashMap<>();
            for (String[] item1 : val_list) {
                String index = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                map_tmp.put(index, item1[1]);
            }
            index_idvlan.put(node, map_tmp);
        }

        // output: node ---> ArrayList(index, id_iface)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("VlanPortStateHP").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] item1 : val_list) {
                if (item1[1].equals("2")) {
                    String[] mas = new String[2];
                    mas[0] = item1[0].split("\\.")[item1[0].split("\\.").length - 2];
                    mas[1] = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                    list_tmp.add(mas);
                }
            }
            index_idifaceUntag.put(node, list_tmp);
        }

        // output: node ---> ArrayList(index, id_iface)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("VlanPortStateHP").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] item1 : val_list) {
                if (item1[1].equals("1")) {
                    String[] mas = new String[2];
                    mas[0] = item1[0].split("\\.")[item1[0].split("\\.").length - 2];
                    mas[1] = item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                    list_tmp.add(mas);
                }
            }
            index_idifaceTag.put(node, list_tmp);
        }

        // output: node ---> ArrayList(id_iface, id_vlanUntag)
        for (Map.Entry<String, ArrayList> entry : index_idifaceUntag.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] item1 : val_list) {
                String[] mas = new String[2];
                mas[0] = item1[1];
                if(index_idvlan.get(node) != null) {
                    mas[1] = index_idvlan.get(node).get(item1[0]);
                } else mas[1] = "";
                list_tmp.add(mas);
            }
            idiface_idvlanUntag.put(node, list_tmp);
        }

        // output: node ---> ArrayList(id_iface, id_vlanTag)
        for (Map.Entry<String, ArrayList> entry : index_idifaceTag.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] item1 : val_list) {
                String[] mas = new String[2];
                mas[0] = item1[1];
                if(index_idvlan.get(node) != null) {
                    mas[1] = index_idvlan.get(node).get(item1[0]);
                } else mas[1] = "";
                list_tmp.add(mas);
            }
            idiface_idvlanTag.put(node, list_tmp);
        }

        for (Map.Entry<String, ArrayList> entry : idiface_idvlanUntag.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String[]> map_tmp = new HashMap<>();
            for (String[] item1 : val_list) {
                String id_iface = item1[0];
                String untag = item1[1];
                ArrayList<String[]> tmp = idiface_idvlanTag.get(node);
                String tag = "";
                if(tmp != null) {
                    for (String[] item2 : tmp) {
                        if (item2[0].equals(id_iface)) {
                            if (tag.equals("")) {
                                tag = item2[1];
                            } else {
                                tag = tag + ":" + item2[1];
                            }
                            break;
                        }
                    }
                }
                String[] mas = new String[2];
                if(tag.equals("")) {
                   mas[0]=untag+":access";
                   mas[1]="";
                } else {
                   mas[0]=untag+":trunk";
                   mas[1]=tag;
                }
                map_tmp.put(id_iface, mas);
            }
            result.put(node, map_tmp);
        }

        return result;
    }    
    
    // Output format node ---> id_iface ---> Untaget_vlan, Taget_vlans(vlan1:vlan2, ...)
    private Map<String, Map<String, String[]>> GetVlanRFC2674(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, Map<String, String[]>> result = new HashMap<>();
        Map<String, ArrayList<String[]>> idvlan_untagport = new HashMap<>();
        Map<String, ArrayList<String[]>> idvlan_tagport = new HashMap<>();

        // output: node ---> list(id_vlan, Untag_ports)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("UnTaggedVlan").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> tmp_list = new ArrayList();
            for (String[] item : val_list) {
                String[] tmp = new String[2];
                tmp[0] = item[0].split("\\.")[item[0].split("\\.").length - 1];
                tmp[1] = HexMapToVlans(item[1]);
                if(!tmp[1].equals("")) {
                    String[] fields = tmp[1].split(":");
                    tmp[1] = "";
                    for (String field : fields) {
                        if (tmp[1].equals("")) {
                            tmp[1] = String.valueOf(Integer.parseInt(field) + 1);
                        } else {
                            tmp[1] = tmp[1] + ":" + String.valueOf(Integer.parseInt(field) + 1);
                        }
                    }
                    boolean found=false;
                    for(String[] item1 : tmp_list) {
                        if(item1[0].equals(tmp[0])) {
                            found=true;
                            String[] mas = new String[2];
                            mas[0]=item1[0]; mas[1]=item1[1]+":"+tmp[1];
                            tmp_list.remove(item1);
                            tmp_list.add(mas);
                            break;
                        }
                    }
                    if(!found) tmp_list.add(tmp);
                }
            }
            idvlan_untagport.put(node, tmp_list);
        }

        // output: node ---> list(id_vlan, tag_ports)
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("TaggedVlan").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> tmp_list = new ArrayList();
            for (String[] item : val_list) {
                String[] tmp = new String[2];
                tmp[0] = item[0].split("\\.")[item[0].split("\\.").length - 1];
                tmp[1] = HexMapToVlans(item[1]);
                if(!tmp[1].equals("")) {
                    String[] fields = tmp[1].split(":");
                    tmp[1] = "";
                    for (String field : fields) {
                        if (tmp[1].equals("")) {
                            tmp[1] = String.valueOf(Integer.parseInt(field) + 1);
                        } else {
                            tmp[1] = tmp[1] + ":" + String.valueOf(Integer.parseInt(field) + 1);
                        }
                    }
                    boolean found=false;
                    for(String[] item1 : tmp_list) {
                        if(item1[0].equals(tmp[0])) {
                            found=true;
                            String[] mas = new String[2];
                            mas[0]=item1[0]; mas[1]=item1[1]+":"+tmp[1];
                            tmp_list.remove(item1);
                            tmp_list.add(mas);
                            break;
                        }
                    }
                    if(!found) tmp_list.add(tmp);                
                }
            }
            idvlan_tagport.put(node, tmp_list);
        }

        for (Map.Entry<String, ArrayList<String[]>> entry : idvlan_untagport.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list_untag = entry.getValue();
            ArrayList<String[]> val_list_tag = idvlan_tagport.get(node);
            ArrayList<String[]> port_vlan_untag = new ArrayList();
            for (String[] item : val_list_untag) {
                for (String id_port : item[1].split(":")) {
                    String[] mas = new String[2];
                    mas[0] = id_port;
                    mas[1] = item[0];
                    port_vlan_untag.add(mas);
                }
            }
            ArrayList<String[]> port_vlan_tag = new ArrayList();
            if(val_list_tag != null) {
                for (String[] item : val_list_tag) {
                    for (String id_port : item[1].split(":")) {
                        String[] mas = new String[2];
                        mas[0] = id_port;
                        mas[1] = item[0];
                        port_vlan_tag.add(mas);
                    }
                }
            }

            Map<String, String> idport_vlan_untag = new HashMap<>();
            for (int i = 0; i < port_vlan_untag.size(); i++) {
                String[] item1 = port_vlan_untag.get(i);
                String id_port = item1[0];
                String vlan = item1[1];
                for (int j = i + 1; j < port_vlan_untag.size(); j++) {
                    String[] item2 = port_vlan_untag.get(j);
                    if (item1[0].equals(item2[0])) {
                        boolean found=false;
                        for(String it : vlan.split(":")) {
                            if(it.equals(item2[1])) {
                                found=true;
                                break;
                            }
                        }
                        if(!found) {
                            vlan = vlan + ":" + item2[1];
                        }
                        port_vlan_untag.remove(j);
                        j--;
                    }
                }
                idport_vlan_untag.put(id_port, vlan);
            }

            Map<String, String> idport_vlan_tag = new HashMap<>();
            for (int i = 0; i < port_vlan_tag.size(); i++) {
                String[] item1 = port_vlan_tag.get(i);
                String id_port = item1[0];
                String vlan = "";
                if (!item1[1].equals(idport_vlan_untag.get(id_port))) {
                    vlan = item1[1];
                }
                for (int j = i + 1; j < port_vlan_tag.size(); j++) {
                    String[] item2 = port_vlan_tag.get(j);
                    if (item1[0].equals(item2[0])) {
                        if (!item2[1].equals(idport_vlan_untag.get(item2[0]))) {
                            if (vlan.equals("")) {
                                vlan = item2[1];
                            } else {
                                boolean found=false;
                                for(String it : vlan.split(":")) {
                                    if(it.equals(item2[1])) {
                                        found=true;
                                        break;
                                    }
                                }
                                if(!found) {
                                    vlan = vlan + ":" + item2[1];
                                }
//                                
//                                
//                                
//                                vlan = vlan + ":" + item2[1];
                            }
                        }
                        port_vlan_tag.remove(j);
                        j--;
                    }
                }
                idport_vlan_tag.put(id_port, vlan);
            }

            Map<String, String[]> idport_untag_tag = new HashMap<>();
            for (Map.Entry<String, String> entry1 : idport_vlan_untag.entrySet()) {
                String ip_port = entry1.getKey();
                String untag = entry1.getValue();
                String tag = idport_vlan_tag.get(ip_port);
                String[] mas = new String[2];
                mas[0] = untag;
                mas[1] = tag;
                idport_untag_tag.put(ip_port, mas);
            }
            result.put(node, idport_untag_tag);
        }

        return result;
    }    
    
    private String[] GetIfaceName(String node, String id_iface, Map<String, Map<String, ArrayList>> walkInformationFromNodes, boolean with_interface_maping) {
        String[] result = new String[2];
        result[0]=""; result[1]="";

        boolean found_IfaceMaping = false;
        if(with_interface_maping) {
            if(walkInformationFromNodes.get("IfaceMaping").size() > 0 && walkInformationFromNodes.get("IfaceMaping").get(node) != null) {
                for(String[] item0 : (ArrayList<String[]>)walkInformationFromNodes.get("IfaceMaping").get(node)) {
                    if(item0[0].split("\\.")[item0[0].split("\\.").length - 1].equals(id_iface)) {
                        id_iface=item0[1];
                        found_IfaceMaping = true;
                        break;
                    }
                }
            }
        }
        
        if(!with_interface_maping || (with_interface_maping && found_IfaceMaping)) {
            boolean uniqal_all_interface=true;
            if(walkInformationFromNodes.get("ifDescr").get(node) != null) {
                ArrayList<String[]> list = walkInformationFromNodes.get("ifDescr").get(node);

                if(list != null) {
                    int i=0;
                    for (String[] mas : list) {
                        boolean found=false;
                        for(int j=i+1; j<list.size(); j++) {
                            String[] mas1=list.get(j);
                            if(mas1[1].equals(mas[1])) {
                                found=true;
                                uniqal_all_interface=false;
                                break;
                            }
                        }
                        if(found) break;
                        i++;
                    }
                }

                if(list != null) {
                    for (String[] mas : list) {
                        if (mas[0].split("\\.").length > 0 && mas[0].split("\\.")[mas[0].split("\\.").length - 1].equals(id_iface)) {
                            result[0]=id_iface;
                            result[1] = ReplaceDelimiter(TranslateHexString_to_SymbolString(mas[1]));
                            break;
                        }
                    }
                }
                if(!uniqal_all_interface) {
                    if(walkInformationFromNodes.get("IfNameExtendedIfName").get(node) != null) {
                        ArrayList<String[]> list_ext = walkInformationFromNodes.get("IfNameExtendedIfName").get(node);
                        if(list_ext != null) {
                            for (String[] mas : list_ext) {
                                if (mas[0].split("\\.").length > 0 && mas[0].split("\\.")[mas[0].split("\\.").length - 1].equals(id_iface)) {
                                    result[1] = ReplaceDelimiter(TranslateHexString_to_SymbolString(mas[1]));
                                    break;
                                }
                            }
                        }

                    }

                }
            }
        }

        return result;
    }    

    //  output: PhysicalDescr ---> serialnumber
    private Map<String, String> GetSerialNumberFromNodes(
            Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, String> result = new HashMap<>();

        if(walkInformationFromNodes.get("entPhysicalDescr") != null) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("entPhysicalDescr").entrySet()) {
                String node = entry.getKey();
    //            System.out.println(node);
                ArrayList<String[]> physicalDescr_list = entry.getValue();
                ArrayList<String[]> serialnumber_list = walkInformationFromNodes.get("entPhysicalSerialNumber").get(node);
                if(serialnumber_list != null) {
                    for(int i=0; i<physicalDescr_list.size(); i++) {
                        if(physicalDescr_list.get(i) != null && serialnumber_list.get(i) != null) {
                            String physicalDescr = physicalDescr_list.get(i)[1];
                            String serialnumber = serialnumber_list.get(i)[1];
                            if(!serialnumber.equals(""))result.put(physicalDescr, serialnumber);
                        }
                    }
                }
            }
        }
        return result;
    }


    //  output: node ---> id_iface ---> interface_information
    private Map<String, Map<String, String>> GetInterfacesInformationFromNodes(
            Map<String, Map<String, ArrayList>> walkInformationFromNodes,
            Map<String, Map<String, String>> duplex_mode, Map<String, Map<String, ArrayList<String>>> interface_ipaddress,
            Map<String, Map<String, String[]>> getVlanPortUntagTag) {
        Map result = new HashMap<>();

        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIndex").entrySet()) {
            String node = entry.getKey();
//            System.out.println(node);
            ArrayList<String[]> val_list = entry.getValue();

            for (String[] val : val_list) {
                String id_iface = val[1];
                String[] res = GetIfaceName(node, id_iface, walkInformationFromNodes, false);
                String iface_name = res[1];
                id_iface=res[0];

                if (!iface_name.equals("")) {
                    Map tmp_map = new HashMap<>();
                    ArrayList<String[]> list1 = walkInformationFromNodes.get("ifMTU").get(node);
                    if(list1 != null) {
                        for (String[] mas1 : list1) {
                            if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                tmp_map.put("mtu", mas1[1]);
                                break;
                            }
                        }
                    }
                    
                    list1 = walkInformationFromNodes.get("ifSpeed").get(node);
                    if(list1 != null) {
                        for (String[] mas1 : list1) {
                            if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                if(mas1[1].equals("10000000")) tmp_map.put("speed", "10 Mbps");
                                else if(mas1[1].equals("100000000")) tmp_map.put("speed", "100 Mbps");
                                else if(mas1[1].equals("1000000000")) tmp_map.put("speed", "1 Gbps");
                                else if(mas1[1].equals("10000000000")) tmp_map.put("speed", "10 Gbps");
                                else if(mas1[1].equals("100000000000")) tmp_map.put("speed", "100 Gbps");
                                else tmp_map.put("speed", mas1[1]);                                
                                break;
                            }
                        }
                    }                    
                    
                    if (duplex_mode.get(node) != null && duplex_mode.get(node).get(id_iface) != null) tmp_map.put("duplex", duplex_mode.get(node).get(id_iface));
                    
                    
                    
                    list1 = walkInformationFromNodes.get("ifMAC").get(node);
                    if(list1 != null) {
                        for (String[] mas1 : list1) {
                            if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                tmp_map.put("mac", ReplaceDelimiter(TranslateMAC(mas1[1])));
                                break;
                            }
                        }
                    }                     

                    if (interface_ipaddress.get(node).get(id_iface) != null) tmp_map.put("ip", interface_ipaddress.get(node).get(id_iface));

                    
                    
                    
                    list1 = walkInformationFromNodes.get("ifAdminStatus").get(node);
                    if(list1 != null) {
                        for (String[] mas1 : list1) {
                            if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                String status = "";
                                if(mas1[1].equals("1")) status="up";
                                else status="down";
                                tmp_map.put("admin_status", status);
                                break;
                            }
                        }
                    }
                    
                    
                    list1 = walkInformationFromNodes.get("ifOperStatus").get(node);
                    if(list1 != null) {
                        for (String[] mas1 : list1) {
                            if (mas1[0].split("\\.").length > 0 && mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                String status = "";
                                if(mas1[1].equals("1")) status="up";
                                else status="down";                                
                                tmp_map.put("operation_status", status);
                                break;
                            }
                        }
                    }
                    
                    if(getVlanPortUntagTag.get(node) != null && getVlanPortUntagTag.get(node).get(id_iface) != null && getVlanPortUntagTag.get(node).get(id_iface).length == 2) {
                        String[] mas1 = getVlanPortUntagTag.get(node).get(id_iface)[0].split(":", -1);
                        if(mas1.length == 2) {
                            if(mas1[1].equals("access")) {
                                tmp_map.put("mode", "access");
                                tmp_map.put("access_vlan", mas1[0]);
                            } else if(mas1[1].equals("trunk")) {
                                tmp_map.put("mode", "trunk");
                                tmp_map.put("trunk_vlan", getVlanPortUntagTag.get(node).get(id_iface)[1].replace(":", ","));  
                                tmp_map.put("native_vlan", mas1[0]); 
                            }
                        }
                    }
                    result.put(iface_name, tmp_map);
                }

            }
        }

        return result;
    }
    
    //  output: node ---> route_information
    private Map<String, String> GetRoutesInformationFromNodes(
            Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("RouteDestination").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();

            String routeInform = "";
            for (String[] val : val_list) {
                String network = "";
                String destination = "";
                String mask = "";
                String routeType = "";
                String routeProto = "";
                String routeAge = "";

                String[] mas = val[0].split("\\.");
                if (mas.length > 0) {
                    network = mas[mas.length - 4] + "." + mas[mas.length - 3] + "." + mas[mas.length - 2] + "." + mas[mas.length - 1];
                }
                if (val[1].split("\\.").length > 0) {
                    destination = val[1];
                }

                ArrayList<String[]> list = walkInformationFromNodes.get("RouteMask").get(node);
                if(list != null) {
                    for (String[] val1 : list) {
                        String[] mas1 = val1[0].split("\\.");
                        if (mas1.length > 0) {
                            String network1 = mas1[mas1.length - 4] + "." + mas1[mas1.length - 3] + "." + mas1[mas1.length - 2] + "." + mas1[mas1.length - 1];
                            if (network.equals(network1)) {
                                mask = val1[1];
                                break;
                            }
                        }
                    }
                }
                list = walkInformationFromNodes.get("RouteType").get(node);
                if(list != null) {
                    for (String[] val1 : list) {
                        String[] mas1 = val1[0].split("\\.");
                        if (mas1.length > 0) {
                            String network1 = mas1[mas1.length - 4] + "." + mas1[mas1.length - 3] + "." + mas1[mas1.length - 2] + "." + mas1[mas1.length - 1];
                            if (network.equals(network1)) {
                                routeType = val1[1];
                                break;
                            }
                        }
                    }
                }
                list = walkInformationFromNodes.get("RouteProto").get(node);
                if(list != null) {
                    for (String[] val1 : list) {
                        String[] mas1 = val1[0].split("\\.");
                        if (mas1.length > 0) {
                            String network1 = mas1[mas1.length - 4] + "." + mas1[mas1.length - 3] + "." + mas1[mas1.length - 2] + "." + mas1[mas1.length - 1];
                            if (network.equals(network1)) {
                                routeProto = val1[1];
                                break;
                            }
                        }
                    }
                }
                list = walkInformationFromNodes.get("RouteAge").get(node);
                if(list != null) {
                    for (String[] val1 : list) {
                        String[] mas1 = val1[0].split("\\.");
                        if (mas1.length > 0) {
                            String network1 = mas1[mas1.length - 4] + "." + mas1[mas1.length - 3] + "." + mas1[mas1.length - 2] + "." + mas1[mas1.length - 1];
                            if (network.equals(network1)) {
                                routeAge = val1[1];
                                break;
                            }
                        }
                    }
                }
                if (!(network.equals("") || destination.equals("") || mask.equals("") || routeType.equals("") || routeProto.equals("") || routeAge.equals(""))) {
                    if (routeInform.equals("")) {
                        routeInform = network + "," + destination + "," + routeType + "," + routeProto + "," + routeAge + "," + mask;
                    } else {
                        routeInform = routeInform + "|" + network + "," + destination + "," + routeType + "," + routeProto + "," + routeAge + "," + mask;
                    }
                }
            }
            result.put(node, routeInform);
        }

        return result;
    }    
    
    //  output: node ---> vlans_information
    private Map<String, String> GetVlansInformationFromNodes(Map<String, Map<String, String>> getVlanInform) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : getVlanInform.entrySet()) {
            String vlan_info = "";
            String node = entry.getKey();
            for (Map.Entry<String, String> entry1 : entry.getValue().entrySet()) {
                String id_vlan = entry1.getKey();
                String name_vlan = entry1.getValue();
                if (vlan_info.equals("")) {
                    vlan_info = id_vlan + "," + name_vlan;
                } else {
                    vlan_info = vlan_info + "|" + id_vlan + "," + name_vlan;
                }
            }
            result.put(node, vlan_info);
        }
        return result;
    }    
    
    // Output format: ArrayList(node, id_iface, name_iface, remote_node, id_iface_remote, name_iface_remote)
    private Map<String, ArrayList<Map<String, String>>> GetDP(Map<String, Map<String, String>> commonInformationFromNodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, ArrayList> getLLDP = GetLLDP(commonInformationFromNodes, walkInformationFromNodes);
        Map<String, ArrayList> getCDP = GetCDP(walkInformationFromNodes);
        
        Map<String, ArrayList<Map<String, String>>> result = new HashMap<>();

        for (Map.Entry<String, ArrayList> entry : getCDP.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            for(String[] mas : val_list) {
                Map<String, String> map_tmp1 = new HashMap<>();
                try {
                    if(mas[2].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        map_tmp1.put("remote_ip", mas[2]);
                    } else {
                        String name = mas[2].split("\\s*\\(")[0];
                        if(!name.equals("")) {
                            InetAddress address = InetAddress.getByName(name);
                            String ip=address.getHostAddress();
                            map_tmp1.put("remote_ip", ip);
                        } else map_tmp1.put("remote_ip", mas[2]);
                    }
                } catch (UnknownHostException ex) {
                    map_tmp1.put("remote_ip", mas[2]);
                }
                map_tmp1.put("remote_id", mas[5]);
                map_tmp1.put("remote_port_id", mas[4]);
                map_tmp1.put("remote_version", mas[6]);
                map_tmp1.put("type", "cdp");
                
                if(result.get(mas[1]) != null) {
                    result.get(mas[1]).add(map_tmp1);
                } else {
                    ArrayList<Map<String, String>> list_tmp = new ArrayList();
                    list_tmp.add(map_tmp1);
                    result.put(mas[1], list_tmp);
                }
            }
        }
        
        for (Map.Entry<String, ArrayList> entry : getLLDP.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            for(String[] mas : val_list) {
                String local_port = mas[1];
                Map<String, String> map_tmp1 = new HashMap<>();
                try {
                    if(mas[2].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        map_tmp1.put("remote_ip", mas[2]);
                    } else { 
                        String name = mas[2].split("\\s*\\(")[0];
                        if(!name.equals("")) {
                            InetAddress address = InetAddress.getByName(name);
                            String ip=address.getHostAddress();
                            map_tmp1.put("remote_ip", ip);
                        } else map_tmp1.put("remote_ip", mas[2]);
                    }
                } catch (UnknownHostException ex) {
                    map_tmp1.put("remote_ip", mas[2]);
                }
                map_tmp1.put("remote_id", mas[5]);
                map_tmp1.put("remote_port_id", mas[4]);
                map_tmp1.put("remote_version", mas[6]);
                map_tmp1.put("type", "lldp");
                
                String local_port1 = null;
                boolean found = false;
                for (Map.Entry<String, ArrayList<Map<String, String>>> entry1 : result.entrySet()) {
                    local_port1 = entry1.getKey(); 
                    if(EqualsIfaceName(local_port, local_port1)) {
                        found = true;
                        break;
                    }
                }                
                if(found && result.get(local_port1) != null) {
                    result.get(local_port1).add(map_tmp1);
                } else {
                    ArrayList<Map<String, String>> list_tmp = new ArrayList();
                    list_tmp.add(map_tmp1);
                    result.put(local_port, list_tmp);
                }
            }
        }    
        
        return result;
    }    
    
    private boolean EqualsIfaceName(String str1, String str2) {
        String short_iface="";
        String full_iface="";
        if(str1 == null || str2 == null || str1.equals("") || str2.equals("")) return false;
        str1=str1.toLowerCase();
        str2=str2.toLowerCase();
        String[] mas = str1.split("\\s");
        str1=mas[mas.length-1];
        str1=str1.replaceAll("\\W", "");
        mas = str2.split("\\s");
        str2=mas[mas.length-1];
        str2=str2.replaceAll("\\W", "");
        if(str1.length() > str2.length()) { short_iface=str2; full_iface=str1;}
        else { short_iface=str1; full_iface=str2;}
        
        if(short_iface.equals(full_iface)) return true;
        
        Pattern p = Pattern.compile("^(.*?)(\\d+?)$");  
        Matcher m = p.matcher(short_iface);
        String[] short_iface_mas = new String[2];
        if(m.matches()) { 
            short_iface_mas[0]=m.group(1);
            short_iface_mas[1]=m.group(2);
        } else return false;
        
        Matcher m1 = p.matcher(full_iface);
        String[] full_iface_mas = new String[2];
        if(m1.matches()) { 
            full_iface_mas[0]=m1.group(1);
            full_iface_mas[1]=m1.group(2);
        } else return false;
        
        if( (!short_iface_mas[0].equals("") && !full_iface_mas[0].equals("")) &&
                full_iface_mas[0].startsWith(short_iface_mas[0]) && 
                short_iface_mas[1].equals(full_iface_mas[1])) return true;
        else return false;
    }
    
    // Output format: node ---> ArrayList(id_port, remote_node, id_port_remote, name_port_remote)
    private Map<String, ArrayList> GetLLDP(Map<String, Map<String, String>> commonInformationFromNodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, ArrayList> result = new HashMap<>();

        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
        
        Map<String, ArrayList> tmp_map = walkInformationFromNodes.get("lldpRemManAddrIfSubtype");
        if(tmp_map.size() > 0) {
            for (Map.Entry<String, ArrayList> entry : tmp_map.entrySet()) {
                String node = entry.getKey();
//                System.out.println("lldpRemManAddrIfSubtype node="+node);
//                if(result.get(node) == null) {
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();
                for(int i=0; i<val_list.size(); i++) {
                    String[] item = val_list.get(i);

                    String[] buf=item[0].split("\\.");
                    if(buf.length == 20) {
                    
                        String ip = buf[16]+"."+buf[17]+"."+buf[18]+"."+buf[19];
                        if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                            System.out.println(buf[12]+" --- "+ip);
                            String id_iface=buf[12];
                            String name_iface="";
                            String node_remote=ip;
                            String id_iface_remote=buf[13];
                            String name_iface_remote="";                         
                            String sysname_remote="";
                            String sysdescr_remote="";

                            ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemSysName").get(node);
                            if(tmp_list1 != null && tmp_list1.size() > i) {
//                                for(String[] it : tmp_list1) {
//                                    String last_last_key = it[0].split("\\.")[it[0].split("\\.").length - 2];
//                                    String last_key = it[0].split("\\.")[it[0].split("\\.").length - 1];
//                                    if(last_last_key.equals(id_iface) && last_key.equals(id_iface_remote)) {
//                                        sysname_remote=it[1];
//                                        break;
//                                    }
//                                }
                                sysname_remote=tmp_list1.get(i)[1];
                            }

                            tmp_list1 = walkInformationFromNodes.get("lldpRemSysDesc").get(node);
                            if(tmp_list1 != null && tmp_list1.size() > i) {
                                sysdescr_remote=tmp_list1.get(i)[1];
                            }

                            if(!node_remote.equals("")) {
                                tmp_list1 = walkInformationFromNodes.get("lldpRemPortId").get(node);
                                if(tmp_list1 != null && tmp_list1.size() > i) {
                                    String[] mas = GetRemotePort(node_remote, id_iface_remote, tmp_list1.get(i)[1], walkInformationFromNodes, hash_ip);
                                    id_iface_remote=mas[0];
                                    name_iface_remote=mas[1];                                    

                                }
                            }

                            if(!id_iface.equals("") && !node_remote.equals("") && !name_iface_remote.equals("")) {

                                tmp_list1 = walkInformationFromNodes.get("ldpLocPortId").get(node);
                                if(tmp_list1 != null && tmp_list1.size() > i) {
                                    for(String[] item1 : tmp_list1) {
                                        String last=item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                                        if(last.equals(id_iface)) {
                                            name_iface=item1[1];
                                            break;
                                        }
                                    }
                                }
                                if(!name_iface.equals("")) {
                                    String[] mas1 = GetIfaceName(node, id_iface, walkInformationFromNodes, true);
                                    if(!mas1[0].equals("") && !mas1[1].equals("")) {
                                        String[] mas = new String[7];
                                        mas[0]=ReplaceDelimiter(mas1[0]);
                                        mas[1]=ReplaceDelimiter(mas1[1]);
                                        mas[2]=node_remote;
                                        mas[3]=ReplaceDelimiter(id_iface_remote);
                                        mas[4]=ReplaceDelimiter(name_iface_remote);
                                        mas[5]=ReplaceDelimiter(sysname_remote);
                                        mas[6]=ReplaceDelimiter(sysdescr_remote);
                                        tmp_list.add(mas);  
//                                        System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                    } else {
                                        mas1 = SearchInterfaceName(name_iface, walkInformationFromNodes.get("ifDescr").get(node));
                                        if(mas1[0] != null && mas1[1] != null) {
                                            String[] mas = new String[7];
                                            mas[0]=ReplaceDelimiter(mas1[0]);
                                            mas[1]=ReplaceDelimiter(mas1[1]);
                                            mas[2]=node_remote;
                                            mas[3]=ReplaceDelimiter(id_iface_remote);
                                            mas[4]=ReplaceDelimiter(name_iface_remote);
                                            mas[5]=ReplaceDelimiter(sysname_remote);
                                            mas[6]=ReplaceDelimiter(sysdescr_remote);
                                            tmp_list.add(mas);
//                                            System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                        } 
                                    }
                                }
                            }
                        }
                    }
                }

                result.put(node, tmp_list);
            }

        }
        
        if(walkInformationFromNodes.get("lldpRemChassisId").size() > 0) {
            for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("lldpRemChassisId").entrySet()) {
                String node = entry.getKey();
//                if(result.get(node) == null) {
                ArrayList<String[]> val_list = entry.getValue();
                ArrayList<String[]> tmp_list = new ArrayList();
                
                for(int i=0; i<val_list.size(); i++) {
                    String[] item = val_list.get(i);
                    String id_iface=item[0].split("\\.")[item[0].split("\\.").length - 2];
                    String name_iface="";
                    String node_remote=TranslateMAC(item[1]);
                    String id_iface_remote=item[0].split("\\.")[item[0].split("\\.").length - 1];
                    String name_iface_remote="";                         
                    String sysname_remote="";
                    String sysdescr_remote="";

                    ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("lldpRemSysName").get(node);
                    if(tmp_list1 != null && tmp_list1.size() > i) {
                        node_remote=tmp_list1.get(i)[1]+"("+node_remote+")";
                        sysname_remote=tmp_list1.get(i)[1];
                    }

                    tmp_list1 = walkInformationFromNodes.get("lldpRemSysDesc").get(node);
                    if(tmp_list1 != null && tmp_list1.size() > i) {
                        sysdescr_remote=tmp_list1.get(i)[1];
                    }

//                    String last=item[0].split("\\.")[item[0].split("\\.").length - 1];
                    String mac_remote=TranslateMAC(item[1]);

                    if(mac_remote.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                        boolean find=false;
                        for(Map.Entry<String, Map<String, String>> entry1 : commonInformationFromNodes.entrySet()) {
                            String node1 = entry1.getKey();
                            Map<String, String> val_list1 = entry1.getValue();
                            if(!node.equals(node1) && TranslateMAC(val_list1.get("base_address")).equals(mac_remote)) {
                                node_remote=node1;
                                find=true;
                                break;                                        
                            }
                        }
                        if(!find) {
                            for(Map.Entry<String, ArrayList> entry1 : walkInformationFromNodes.get("ifMAC").entrySet()) {
                                String node1 = entry1.getKey();
                                ArrayList<String[]> val_list1 = entry1.getValue();
//                                    boolean find=false;
                                for (String[] item1 : val_list1) {
//                                    if(item1[1].matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) {
                                    if(TranslateMAC(item1[1]).equals(mac_remote)) {
                                        node_remote=node1;
                                        find=true;
                                        break;
                                    }
//                                    }
                                }
                                if(find) break;
                            }
                        }
                    } else {
                        node_remote="";
                    }

                    if(!node_remote.equals("")) {
                        tmp_list1 = walkInformationFromNodes.get("lldpRemPortId").get(node);
                        if(tmp_list1 != null && tmp_list1.size() > i) {
                            String[] mas = GetRemotePort(node_remote, id_iface_remote, sysdescr_remote=tmp_list1.get(i)[1], walkInformationFromNodes, hash_ip);
                            id_iface_remote=mas[0];
                            name_iface_remote=mas[1];                            
                        }
                    }

                    if(!id_iface.equals("") && !node_remote.equals("") && !name_iface_remote.equals("")) {

                        tmp_list1 = walkInformationFromNodes.get("ldpLocPortId").get(node);
                        if(tmp_list1 != null && tmp_list1.size() > i) {
                            for(String[] item1 : tmp_list1) {
                                String last=item1[0].split("\\.")[item1[0].split("\\.").length - 1];
                                if(last.equals(id_iface)) {
                                    name_iface=item1[1];
                                    break;
                                }
                            }
                        }
                        if(!name_iface.equals("")) {
                            String[] mas1 = GetIfaceName(node, id_iface, walkInformationFromNodes, true);
                            if(!mas1[0].equals("") && !mas1[1].equals("")) {
                                String[] mas = new String[7];
                                mas[0]=mas1[0];
                                mas[1]=mas1[1];
                                mas[2]=node_remote;
                                mas[3]=id_iface_remote;
                                mas[4]=name_iface_remote;
                                mas[5]=sysname_remote;
                                mas[6]=sysdescr_remote;
                                tmp_list.add(mas);  
//                                System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                            } else {
                                    mas1 = SearchInterfaceName(name_iface, walkInformationFromNodes.get("ifDescr").get(node));
                                    if(mas1[0] != null && mas1[1] != null) {
                                    String[] mas = new String[7];
                                    mas[0]=mas1[0];
                                    mas[1]=mas1[1];
                                    mas[2]=node_remote;
                                    mas[3]=id_iface_remote;
                                    mas[4]=name_iface_remote;
                                    mas[5]=sysname_remote;
                                    mas[6]=sysdescr_remote;
                                    tmp_list.add(mas);  
//                                    System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                                }
                            }
                        }
                    }
                }

                result.put(node, tmp_list);
//                }
            }

        }
        
        return result;
    } 
    
    // Output format: node ---> ArrayList(id_port, remote_node, id_port_remote, name_port_remote)
    private Map<String, ArrayList> GetCDP(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, ArrayList> result = new HashMap<>();

        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
        
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("cdpCacheAddress").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            ArrayList<String[]> tmp_list = new ArrayList();
            for (String[] item : val_list) {
                String[] buf=item[0].split("\\.");
                String ip = TranslateIP(item[1]);
                if(ip != null && ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && buf.length >= 16)
                {
                    String id_iface=buf[14];
                    String name_iface="";
                    String node_remote=ip;
                    String id_iface_remote=buf[15];
                    String name_iface_remote="";
                    String sysname_remote="";
                    String sysdescr_remote="";
                    
                    if(walkInformationFromNodes.get("cdpCacheDevicePort").get(node) != null) {
                        ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("cdpCacheDevicePort").get(node);
                        for (String[] item1 : tmp_list1) {
                            if(item1[0].equals(oids.get("cdpCacheDevicePort")+"."+id_iface+"."+id_iface_remote)) {
                                name_iface_remote=item1[1];
                                String[] res1 = GetIfaceName(node, buf[14], walkInformationFromNodes, false);
                                id_iface=res1[0];
                                name_iface=res1[1];
                                break;
                            }
                        }
                    }
                    
                    if(walkInformationFromNodes.get("cdpRemSysName").get(node) != null) {
                        ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("cdpRemSysName").get(node);
                        for (String[] item1 : tmp_list1) {
                            if(item1[0].equals(oids.get("cdpRemSysName")+"."+id_iface+"."+id_iface_remote)) {
                                sysname_remote=item1[1];
                                break;
                            }
                        }
                    }
                    
                    if(walkInformationFromNodes.get("cdpRemSysDesc").get(node) != null) {
                        ArrayList<String[]> tmp_list1 = walkInformationFromNodes.get("cdpRemSysDesc").get(node);
                        for (String[] item1 : tmp_list1) {
                            if(item1[0].equals(oids.get("cdpRemSysDesc")+"."+id_iface+"."+id_iface_remote)) {
                                sysdescr_remote=item1[1];
                                break;
                            }
                        }
                    }
                    
                    if(!id_iface.equals("") && !node_remote.equals("") && !name_iface_remote.equals("")) {
                        String[] res1 = GetIfaceName(node, id_iface, walkInformationFromNodes, false);
                        if(!res1[0].equals("") && !res1[1].equals("")) {
                            String[] mas = new String[7];
                            mas[0]=res1[0]; mas[1]=res1[1]; 
                            if(node_remote.equals("0.0.0.0")) {
                                mas[2]=sysname_remote+"("+sysdescr_remote+")";
                            } else {
                                mas[2]=node_remote;
                            }
                            String[] mas1 = GetRemotePort(node_remote, id_iface_remote, name_iface_remote, walkInformationFromNodes, hash_ip);
                            mas[3]=mas1[0]; 
                            mas[4]=mas1[1];
                            mas[5]=ReplaceDelimiter(sysname_remote); mas[6]=ReplaceDelimiter(sysdescr_remote);
                            tmp_list.add(mas);
//                            System.out.println("Link adding CDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                        } else {
                                res1 = SearchInterfaceName(name_iface, walkInformationFromNodes.get("ifDescr").get(node));
                                if(res1[0] != null && res1[1] != null) {
                                String[] mas = new String[7];
                                mas[0]=res1[0];
                                mas[1]=res1[1];
                                if(node_remote.equals("0.0.0.0")) {
                                    mas[2]=sysname_remote+"("+sysdescr_remote+")";
                                } else {
                                    mas[2]=node_remote;
                                }
                                mas[3]=id_iface_remote;
                                mas[4]=name_iface_remote;
                                mas[5]=sysname_remote;
                                mas[6]=sysdescr_remote;
                                tmp_list.add(mas);  
//                                    System.out.println("Link adding LLDP: "+node+" ---> "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]);
                            }
                        }
                    }
                    
                }

            }
            result.put(node, tmp_list);
        }
        return result;
    }

    // Output format: mac ---> ip
    private Map<String, String> GetARP(ArrayList<String[]> node_community_version) {
        String ArpTable = "1.3.6.1.2.1.3.1.1.2";
        String ArpTable1 = "1.3.6.1.2.1.4.22.1.2";
        
        Map<String, String> result = new HashMap<>(); 
        
        if(node_community_version.size() > 0) {
            Map<String, ArrayList> map = new HashMap<>();
            WalkPool walkPool = new WalkPool();
            Map<String, ArrayList> res = walkPool.Get(node_community_version, ArpTable);
            ArrayList<String[]> buff = new ArrayList();
            for(String[] item1 : node_community_version) {
                if(res.get(item1[0]) == null) {
                    String[] mas = new String[3];
                    mas[0]=item1[0];
                    mas[1]=item1[1];
                    mas[2]=item1[2];
                    buff.add(mas);
                }
            }
            map.putAll(res);
            if(buff.size() != 0) {
                res = walkPool.Get(buff, ArpTable1);
                map.putAll(res);
            }

            for (Map.Entry<String, ArrayList> entry : map.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                for(String[] item : val_list) {
                    String[] buf=item[0].split("\\.");
                    if(buf.length == 16) {
                        String ip = buf[12]+"."+buf[13]+"."+buf[14]+"."+buf[15];
                        if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac=item[1];
                            if(mac != null) result.put(mac, ip);
                        }
                    } else if(buf.length == 15) {
                        String ip = buf[11]+"."+buf[12]+"."+buf[13]+"."+buf[14];
                        if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac=item[1];
                            if(mac != null) result.put(mac, ip);
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    // Output format: node ---> id_iface ---> list(mac)
    private Map<String, ArrayList> GetMAC(ArrayList<String[]> node_community_version, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        int retries = 10;
        ArrayList<String> macTable = new ArrayList();
        macTable.add("1.3.6.1.2.1.17.4.3.1.2");
        macTable.add("1.3.6.1.2.1.17.7.1.2.2.1.2");
        
        Map<String, Map<String, ArrayList>> result = new HashMap<>();

        WalkPool walkPool = new WalkPool();
        
        ArrayList<String[]> list_node_community_version_oid = new ArrayList();
        for(String oid : macTable) {
            Map<String, ArrayList> res = walkPool.Get(node_community_version, oid);
            for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
                String node = entry.getKey();
                if(entry.getValue().size() > 0) {
                    for(String[] item1 : node_community_version) {
                        if(node.equals(item1[0])) {                    
                            String[] mas = new String[4];
                            mas[0]=item1[0]; mas[1]=item1[1]; mas[2]=item1[2];
                            mas[3]=oid;
                            list_node_community_version_oid.add(mas);
                            break;
                        }
                    }
                }
            }
        }
        
        ArrayList<String[]> tmp_node_community_version = new ArrayList();
        for(String[] item : node_community_version) {
            ArrayList<String> getVlanCommunity = GetVlanCommunity(walkInformationFromNodes, item[0]);
            if(getVlanCommunity.size() > 0) {
                for(String item1 : getVlanCommunity) {
                    for(String[] item2 : list_node_community_version_oid) {
                        if(item2[0].equals(item[0])) {
                            String[] mas = new String[4];
                            mas[0]=item[0]; mas[1]=item1; mas[2]=item[2];
                            mas[3]=item2[3];
                            tmp_node_community_version.add(mas);
                            break;
                        }
                    }
                }
            } else {
                for(String[] item2 : list_node_community_version_oid) {
                    if(item2[0].equals(item[0])) {
                        String[] mas = new String[4];
                        mas[0]=item[0];
                        mas[1]=item[1];
                        mas[2]=item[2];
                        mas[3]=item2[3];
                        tmp_node_community_version.add(mas);
                        break;
                    }
                }
            }
        }
            
        for(int i=0; i<retries; i++) {
            int num_mac_records=0;
            Map<String, ArrayList> res = walkPool.Get(tmp_node_community_version);
            for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                for(String[] item : val_list) {
                    String mac="";
                    String[] buf=item[0].split("\\.");
                    if(buf.length == 17) {
                        mac=DecToHex(Integer.valueOf(buf[11]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[12]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[13]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[14]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[15]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[16]));
                    }
                    if(buf.length == 20) {
                        mac=DecToHex(Integer.valueOf(buf[14]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[15]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[16]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[17]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[18]));
                        mac=mac+":"+DecToHex(Integer.valueOf(buf[19]));
                    }                    
                    String id_iface=item[1];

                    if(!(mac.equals("") || id_iface.equals("")) ) {
                        if(result.size() > 0 && result.get(node) != null && result.get(node).get(id_iface) != null) {
                            boolean find=false;
                            ArrayList<String> tmp_list = result.get(node).get(id_iface);
                            for(String item1 : tmp_list) {
                                if(item1.equals(mac)) { find=true; break; }
                            }
                            if(!find) { result.get(node).get(id_iface).add(mac); num_mac_records++; }
                        } else if(result.size() > 0 && result.get(node) != null && result.get(node).get(id_iface) == null) {
                            ArrayList tmp_list = new ArrayList();
                            tmp_list.add(mac);
                            result.get(node).put(id_iface, tmp_list);
                            num_mac_records++;
                        } else {
                             ArrayList tmp_list = new ArrayList();
                            tmp_list.add(mac);
                            Map<String, ArrayList> tmp_map = new HashMap<>();
                            tmp_map.put(id_iface, tmp_list);
                            result.put(node, tmp_map);
                            num_mac_records++;
                        }
                    }
                }
            }
            System.out.println("retries="+i+" - "+"num_mac_records="+num_mac_records);
        }
        
        // output: node ---> id ---> id_translate
        Map<String, Map<String,String>> tmp_map = new HashMap<>();
        for (Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("IfaceMaping").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String> tmp_map1 = new HashMap<>();
            for(String[] item : val_list) {
                tmp_map1.put(item[0].split("\\.")[item[0].split("\\.").length - 1], item[1]);
            }
            tmp_map.put(node, tmp_map1);
        }
        
        Map<String, ArrayList> map = new HashMap<>();
        for (Map.Entry<String, Map<String, ArrayList>> entry : result.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_list = entry.getValue();
            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                if(tmp_map.get(node) != null && tmp_map.get(node).get(entry1.getKey()) != null) {
                    String iface_id = tmp_map.get(node).get(entry1.getKey());
                    ArrayList<String> val_list1 = entry1.getValue();

                    if(walkInformationFromNodes.get("ifDescr").get(node) != null) {
                        for(String[] mas : (ArrayList<String[]>)walkInformationFromNodes.get("ifDescr").get(node)) {
                            String id_if = mas[0].split("\\.")[mas[0].split("\\.").length - 1];
                            if(id_if.equals(iface_id)) {
                                map.put(mas[1], val_list1);
                            }
                        }
                    }
                }
            }
        }
        
        return map;
    }
    
    public boolean CheckInterfaceName(String interface_name, String interface_name_sec) {
        try {
            if(interface_name != null) {
                if(interface_name.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
                     String[] fields = interface_name.split(":");

                    String out="";
                    for(String octet : fields) {
                        int dec = Integer.parseInt(octet, 16);
                        if(dec != 0) out=out+(char)dec;

                    }
                    interface_name=out;
                }

                if(interface_name_sec.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
                     String[] fields = interface_name.split(":");

                    String out="";
                    for(String octet : fields) {
                        int dec = Integer.parseInt(octet, 16);
                        if(dec != 0) out=out+(char)dec;

                    }
                    interface_name_sec=out;
                }

                String iface="";
                String iface_long="";

                if(!interface_name.equals(interface_name_sec)) {
                    if(interface_name.length() < interface_name_sec.length()) {
                        iface=interface_name;
                        iface_long=interface_name_sec;
                    } else {
                        iface=interface_name_sec;
                        iface_long=interface_name;            
                    }

                    iface=iface.toLowerCase();
                    iface_long=iface_long.toLowerCase();        
                    Pattern p = Pattern.compile("^([A-Za-z\\s]+)[\\s_-]*(\\d+[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*)$");
                    Matcher m = p.matcher(iface);
                    if(m.find()){  
                        String start=m.group(1);
                        String stop=m.group(2);
                        Pattern p1 = Pattern.compile("^"+start+"([\\w\\s]*)"+stop+"$");
                        Matcher m1 = p1.matcher(iface_long);
                        if(m1.find()) {
                            String diff = m1.group(1);
                            if(diff.length() > 3) return true;
                        }

                    }
                } else {
                    return true;
                }
            }

            return false;
        } catch (Exception e) { return false; }
    }    
    
    //  output: id_iface, iface_name
    private String[] SearchInterfaceName(String interface_name, ArrayList<String[]> list_interface) {
        String[] result = new String[2];

        if(interface_name != null) {
            interface_name = ReplaceDelimiter(TranslateHexString_to_SymbolString(interface_name));

            Pattern p = Pattern.compile("^([A-Za-z\\s]+)[\\s_-]*(\\d+[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*[/._-]*\\d*)$");
            Matcher m = p.matcher(interface_name.toLowerCase());
            if(m.find()){  
                String start=m.group(1);
                String stop=m.group(2);
                Pattern p1 = Pattern.compile("^"+start+"[\\w\\s]*"+stop+"$");
                if(list_interface != null) {
                    for(String[] iter : list_interface) {
                        Matcher m1 = p1.matcher(iter[1].toLowerCase());
                        if(m1.matches()) {
                            result[0]=iter[0].split("\\.")[iter[0].split("\\.").length - 1];
                            result[1]=iter[1];
                            break;
                        }
                    }
                }
            }
        }
        
        return result;
    }    
    
    public Map<String, ArrayList<String>> GetIpAddress(Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, ArrayList<String>> result = new HashMap();
        
        for(Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIpAddress").entrySet()) {
            String node = entry.getKey();
            ArrayList<String> tmp_list = GetIpAddressFromNode(walkInformationFromNodes, node);
            result.put(node, tmp_list);
        }
        
        return result;
    }   
    
    private ArrayList<String> GetIpAddressFromNode(Map<String, Map<String, ArrayList>> walkInformationFromNodes, String node) {
        String ifOperStatus = "1.3.6.1.2.1.2.2.1.8";
        ArrayList<String> result = new ArrayList();
        ArrayList<String[]> list = walkInformationFromNodes.get("ifIpAddress").get(node);
        if(list != null) {
            result.add(node);
            for(String[] item : list) {
                String[] tmp = item[0].split("\\.");
                String ip = tmp[tmp.length - 4] + "." + tmp[tmp.length - 3] + "." + tmp[tmp.length - 2] + "." + tmp[tmp.length - 1];
                String id_iface = item[1];
                ArrayList<String[]> list1 = walkInformationFromNodes.get("ifOperStatus").get(node);
                if(list1 != null) {
                    for(String[] item1 : list1) {
                        if(item1[0].equals(ifOperStatus+"."+id_iface)) {
                            if(item1[1].equals("1")) if(ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) result.add(ip);
                            else System.out.println("node="+node+" ip="+ip+" is not up.");
//                            break;
                        }
                    }
                }
            }
        } else result.add(node);
        
//        ru.kos.neb.neb_lib.Utils lib_utils = new ru.kos.neb.neb_lib.Utils();
        ArrayList<String> result_sort = new ArrayList();
//        for(String ip : result) {
//            boolean find=false;
//            for(String network : Neb.networks) {
//                if(lib_utils.InsideInterval(ip, network)) {
//                    result_sort.add(ip);
//                    find=true; 
//                    break;
//                }
//            }
//            if(find) break;
//        }
        if(result_sort.size() == 1) {
            for(String ip : result) {
                if(!ip.equals(result_sort.get(0))) result_sort.add(ip);
            }
        } else result_sort.addAll(result);
        
        return result_sort;
    }    
    
    private String[] GetRemotePort(String node_remote, String id_iface_remote, String name_iface_remote, Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList<String>> hash_ip) {
        String[] result = new String[2];
        result[0]=id_iface_remote;
        result[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(name_iface_remote));
        
//        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
        node_remote = GetRealIpAddress(hash_ip, node_remote);
        
        if(walkInformationFromNodes.get("ifDescr").get(node_remote) != null) {
            String[] mas1 = SearchInterfaceName(name_iface_remote, walkInformationFromNodes.get("ifDescr").get(node_remote));
            if(mas1[0] != null && mas1[1] != null) {
                result[0]=mas1[0];
                result[1]=mas1[1];
            } else {
                if(name_iface_remote.matches("\\d+")) {
                    String[] res = GetIfaceName(node_remote, name_iface_remote, walkInformationFromNodes, true);
                    if(!res[0].equals("")) {
                        result[0]=res[0];
                        result[1]=res[1];
                    } else {
                        res = GetIfaceName(node_remote, id_iface_remote, walkInformationFromNodes, true);
                        if(!res[0].equals("")) {
                            result[0]=res[0];
                            result[1]=res[1];
                        }                        
                    }
                } 
            }
        }
        
        return result;
    }

    private String GetRealIpAddress(Map<String, ArrayList<String>> hash_ip, String ip_search) {
        String result=ip_search;
        
        for (Map.Entry<String, ArrayList<String>> entry : hash_ip.entrySet()) {
            boolean find=false;
            for(String ip : entry.getValue()) {
                if(ip.equals(ip_search)) {
                    result=entry.getKey();
                    find=true;
                    break;
                }
            }
            if(find) break;
        }  
        return result;
    }    
    
//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////    
    private String convertHexToString(String hex){

              StringBuilder sb = new StringBuilder();
              StringBuilder temp = new StringBuilder();

              try {
                  for( int i=0; i<hex.length()-1; i+=3 ){

                      //grab the hex in pairs
                      String output = hex.substring(i, (i + 2));
                      //convert hex to decimal
                      int decimal = Integer.parseInt(output, 16);
                      if(decimal == 0) decimal=64;
                      //convert the decimal to character
                      sb.append((char)decimal);

                      temp.append(decimal);
                  }
    //              System.out.println("Decimal : " + temp.toString());

                  return sb.toString();
              }
              catch(Exception e) {
                  return hex;
              }

    }    
    
    private ArrayList SetUniqueList(ArrayList<String[]> list, int num_field) {
        for (int i = 0; i < list.size(); i++) {
            String node1 = list.get(i)[num_field];
            if (!node1.equals("")) {
                for (int j = i + 1; j < list.size(); j++) {
                    String node2 = list.get(j)[num_field];
                    if (!node2.equals("")) {
                        if (node1.equals(node2)) {
                            String[] mas = new String[3];
                            mas[0] = "";
                            mas[1] = "";
                            mas[2] = "";
                            list.set(j, mas);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[0].equals("")) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }    
    
    private String TranslateHexString_to_SymbolString(String str) {
        String result=str;
        
        boolean wrong=false;
        if(str.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
            String[] fields = str.split("[:-]");
            if(fields.length > 1) {
                String out="";
                for(String octet : fields) {
                    int dec = Integer.parseInt(octet, 16);
                    if(dec != 0 && (dec == 63 || dec < 32 || dec > 126)) wrong=true;
                    if(dec != 0) out=out+(char)dec;
                }
                if(!wrong) result=out;
            }
        }
        return result;
    }   
    
    private String ReplaceDelimiter(String str) {
        if(str != null) {
            str=str.replaceAll("\n", " ").replaceAll("\r", "");
            str=str.replaceAll("\\;", " ");
            str=str.replaceAll("\\|", " ");
            str=str.replaceAll("\\,", " ");
        }
   
        return str;
    } 
    
    private String HexMapToVlans(String hex) {
        String result = "";

        if (hex != null && !hex.equals("")) {
            String[] fields = hex.split(":");
            String hash = "";
            for (int p = 0; p < fields.length; p++) {
                if(fields[p].matches("^[0-9A-Fa-f]{1,2}$")) {
                    String octet = Integer.toBinaryString(Integer.valueOf(fields[p], 16));
                    for (int q = 0; q < 8 - octet.length(); q++) {
                        hash = hash + "0";
                    }
                    hash = hash + octet;
                } else {
                    for(int i=0; i<fields[p].length(); i++) {
                        String octet = Integer.toBinaryString(Integer.valueOf(fields[p].charAt(i)));
                        for (int q = 0; q < 8 - octet.length(); q++) {
                            hash = hash + "0";
                        }
                        hash = hash + octet;
                    }
                }
            }
            boolean first = true;
            for (int p = 0; p < hash.length(); p++) {
                if (String.valueOf(hash.charAt(p)).equals("1")) {
                    if (!first) {
                        result = result + ":" + String.valueOf(p);
                    } else {
                        result = result + String.valueOf(p);
                    }
                    first = false;
                }
            }
        }
        return result;
    }  
    
    private String TranslateMAC(String mac)
    {
        String out = "";

        if(mac != null) {
            if(mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) out=mac;
            else
            {
                if(mac.length() == 6)
                {
                    byte[] bb = mac.getBytes();
                    out=String.format("%02x", bb[0]);
                    for(int i=1; i<bb.length; i++) {
                        out = out+":"+String.format("%02x", bb[i]);
                    }
                }
            }
        }
        return out.toLowerCase().replace("-", ":").replace(" ", ":");
    }
    
    private String TranslateIP(String ip)
    {
        String out = null;

        if(ip != null) {
            if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) out=ip;
            else
            {
                if(ip.length() == 4)
                {
                    String s=String.valueOf((int)ip.charAt(0));
                    out=s;
                    s=String.valueOf((int)ip.charAt(1));
                    out=out+"."+s;
                    s=String.valueOf((int)ip.charAt(2));
                    out=out+"."+s;
                    s=String.valueOf((int)ip.charAt(3));
                    out=out+"."+s;
                }
                else
                {
                    String[] buf = ip.split(":");
                    if(buf.length == 4)
                    {
                        String s = String.valueOf(Integer.parseInt(buf[0],16));
                        out=s;
                        s = String.valueOf(Integer.parseInt(buf[1],16));
                        out=out+"."+s;
                        s = String.valueOf(Integer.parseInt(buf[2],16));
                        out=out+"."+s;
                        s = String.valueOf(Integer.parseInt(buf[3],16));
                        out=out+"."+s;
                    }
                    else
                    {
                        buf = ip.split(" ");
                        if(buf.length == 4)
                        {
                            String s = String.valueOf(Integer.parseInt(buf[0],16));
                            out=s;
                            s = String.valueOf(Integer.parseInt(buf[1],16));
                            out=out+"."+s;
                            s = String.valueOf(Integer.parseInt(buf[2],16));
                            out=out+"."+s;
                            s = String.valueOf(Integer.parseInt(buf[3],16));
                            out=out+"."+s;
                        }
    //                    else System.out.println("Error - "+ip);
                    }
                }
            }
        }

        return out;
    }
    
    private String DecToHex(int dec){
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};

        String hex=String.valueOf(hexDigits[dec/16])+String.valueOf(hexDigits[dec%16]);

        return hex;
    }    
    
    private String HexStringToUTF8(String hex) {
        if(hex.matches("([0-9A-Fa-f]{2}[:-])+([0-9A-Fa-f]{2})")) {
            hex = hex.replace(":", "").replace("-", "");
            ByteBuffer buff = ByteBuffer.allocate(hex.length()/2);
            for (int i = 0; i < hex.length(); i+=2) {
                buff.put((byte)Integer.parseInt(hex.substring(i, i+2), 16));
            }
            ((Buffer)buff).rewind();
//            buff.rewind();
            Charset cs = Charset.forName("UTF-8");
            CharBuffer cb = cs.decode(buff);
            return cb.toString();  
        } else return hex;
    }    
}

class Check_Parent extends Thread {
    private static int timeout=5;
    private static int retry=5;
    private static long countdown = 60*1000; // 30 sec.
    
    public void run() {
        long start = System.currentTimeMillis();
        while (true) {
            long current = System.currentTimeMillis();
            if(current-start > countdown) {
                if(!CheckPort("localhost", 8080, timeout, retry)) {
                    System.out.println("Parrent not respond: "+SNMP_information.cmd);
                    System.exit(0);
                }                
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) {  }
        }

    }
    
    private boolean CheckPort(String node, int port, int timeout, int retry) {
        while(retry > 0) {
            if(TCPportIsOpen("localhost", 8080, timeout)) return true;
            retry--;
        }
        return false;
    }
    
    private boolean TCPportIsOpen(String node, int port, int timeout) {
        try {
            InetAddress IP = InetAddress.getByName(node);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(IP, port), timeout*1000);
            socket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }  
    
}