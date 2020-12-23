/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_builder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Math.abs;
//import java.nio.file.Paths;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
//import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.*;
import org.json.simple.parser.*;
import com.google.gson.*;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static ru.kos.neb.neb_builder.Neb.DEBUG;
//import static ru.kos.neb.neb_builder.Neb.INFORMATION;
import static ru.kos.neb.neb_builder.Neb.history_dir;
import static ru.kos.neb.neb_builder.Neb.logger;
import static ru.kos.neb.neb_builder.Neb.map_file;
import static ru.kos.neb.neb_builder.Neb.names_info;
import static ru.kos.neb.neb_builder.Neb.utils;
//import static ru.kos.neb.neb_builder.Neb.networks;
import ru.kos.neb.neb_lib.*;
//import org.mapdb.*;

/**
 *
 * @author kos
 */
public class Utils {
    
    private final Map<String, String> oids = new HashMap<>();
    private ArrayList<File> queue = new ArrayList<File>();

    public String GetHomePath() {
        String result = "";
        String path = "";
        try {
            path = Neb.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String[] mas = path.split("/");
            for (int j = 1; j < mas.length - 1; j++) {
                result = result + mas[j] + "/";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(Utils.class.getName() + " - " + ex);
            System.exit(1);
        }
        return result;
    }

//    public ArrayList GetReadXMLWithKey(org.w3c.dom.Document doc, String key, boolean replace_blank) throws NullPointerException {
//        ArrayList result = new ArrayList();
//
//        try {
//            NodeList list = doc.getElementsByTagName(key).item(0).getChildNodes();
//            if (list.getLength() > 1) {
//                for (int i = 0; i < list.getLength(); i++) {
//                    Node item = list.item(i);
//                    if (item.getNodeType() == 1) {
//                        if (replace_blank) {
//                            result.add(item.getTextContent().replace("\n", "").replace(" ", ""));
//                        } else {
//                            result.add(item.getTextContent().replace("\n", ""));
//                        }
//                    }
//                }
//            } else if (replace_blank) {
//                result.add(doc.getElementsByTagName(key).item(0).getTextContent().replace("\n", "").replace(" ", ""));
//            } else {
//                result.add(doc.getElementsByTagName(key).item(0).getTextContent().replace("\n", ""));
//            }
//        } catch (NullPointerException ex) {
//            if(DEBUG) System.out.println(ex);
////            System.out.println("Bad parameter " + key + " in configuration fule.");
////            System.exit(1);
//        }
//        return result;
//    }

    public Map ReadConfig(String filename) {
        Map result = new HashMap<>();
        JSONParser parser = new JSONParser();

        try {
            /* Get the file content into the JSONObject */
            JSONObject jsonObject = (JSONObject)parser.parse(new FileReader(filename));
            result = toMap(jsonObject);
            if(result.get("build_network_time") == null) {
                System.out.println("Not set build_network_time!");
                System.exit(1);
            }
            if(result.get("areas") == null) {
                System.out.println("Not set areas!");
                System.exit(1);
            }
            if(((Map)result.get("areas")).size() == 0  ) {
                System.out.println("Not set none area!");
                System.exit(1);
            }
            for(Map.Entry<String, Map> entry : ((Map<String, Map>)result.get("areas")).entrySet()) {
                String area_name = entry.getKey();
                Map map_area = entry.getValue();
                if(map_area.get("networks") == null || ((ArrayList)map_area.get("networks")).size() == 0) {
                    System.out.println("Not set networks in area "+area_name+"!");
                    ((Map)result.get("areas")).remove(area_name);
                    if(((Map)result.get("areas")).size() == 0  ) {
                        System.out.println("Not set none area!");
                        System.exit(1);
                    }                    
                } else {
                    ArrayList<String> list = new ArrayList();
                    for(String network : (ArrayList<String>)map_area.get("networks")) {
                        if(!(network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$") || 
                                network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+\\s*$") ||
                                network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))
                                ) {
                            System.out.println("In networks section "+network+" is not correct format!");
                        } else {
                            list.add(network);
                        }
                    }
                    if(list.size() > 0)
                        map_area.put("networks", list);
                    else {
                        System.out.println("Not set networks in area!");
                        System.exit(1);                         
                    }
                        
                }
                
                ArrayList<String> list1 = new ArrayList();
                for(String network : (ArrayList<String>)map_area.get("include")) {
                    if(!(network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$") || 
                            network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+\\s*$") ||
                            network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))
                            ) {
                        System.out.println("In include section "+network+" is not correct format!");
                    } else {
                        list1.add(network);
                    }
                }
                map_area.put("include", list1);
                
                Map map1 = new HashMap();
                for(String network : (ArrayList<String>)map_area.get("exclude")) {
                    if(!(network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$"))) {
                        System.out.println("In exclude section "+network+" is not correct format!");
                    } else {
                        map1.put(network, network);
                    }
                }
                map_area.put("exclude", map1);
                
                if(map_area.get("snmp_community") == null || ((ArrayList)map_area.get("snmp_community")).size() == 0) {
                    ArrayList list = new ArrayList();
                    list.add("public");
                    map_area.put("snmp_community", list);
                }
                
                if(map_area.get("discovery_networks") == null) {
                    map_area.put("discovery_networks", "yes");
                }    
                
                if(map_area.get("calculate_links_from_counters") == null) {
                    map_area.put("calculate_links_from_counters", "yes");
                }                  
            }
            if(result.get("map_file") == null) result.put("map_file", "neb.map");
            if(result.get("passwd_file") == null) result.put("passwd_file", "passwd");
            if(result.get("level_log") == null) result.put("level_log", "INFO");
            if(result.get("scan_mode") == null) result.put("scan_mode", "normal");
            if(result.get("host_time_live") == null) result.put("host_time_live", 30);
            if(result.get("history_map") == null) result.put("history_map", "history");
            if(result.get("history_num_days") == null) result.put("history_num_days", 365);
            if(result.get("log_num_days") == null) result.put("log_num_days", 41);
            if(result.get("canvas_color") == null) result.put("canvas_color", "255,255,255");
            if(result.get("term_font_size") == null) result.put("term_font_size", 22);
            if(result.get("term_font_style") == null) result.put("term_font_style", "bold");
            
            
        } catch (Exception ex) {
            ex.printStackTrace();
            if(DEBUG) System.out.println(ex);            
            System.out.println(Utils.class.getName() + " - " + ex);
            System.exit(1);
        }        
        
        return result;
    }
    
//    public Map Read_NebMapFile(String filename) {
//        Map result = new HashMap<>();
//        JSONParser parser = new JSONParser();
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(filename));
//            
//            char[] buff = new char[65535];
////            char[] buff = new char[1024];
//            int size;
//            StringBuilder out = new StringBuilder();
//            while ((size=reader.read(buff))>=0) {
//                out.append(String.valueOf(buff, 0, size));
//            }
//            reader.close();
//            
//            String[] mas = out.toString().split("\n*#+ .+ #+\n");
//            for(String str : mas) {
//                if(!str.equals("")) {
////                    System.out.println(str);
//                    JSONObject jsonObject = (JSONObject)parser.parse(str);
//                    Map map_tmp = toMap(jsonObject);
//                    result.putAll(map_tmp);
//                }
//            }
//        } catch (Exception ex) {
//            System.out.println(Utils.class.getName() + " - " + ex);
//        }        
//        
//        return result;
//    }    
    
    public Map ReadJSONFile(String filename) {
        Map result = new HashMap<>();
        File file = new File(filename);
        if(file.exists()) {
            JSONParser parser = new JSONParser();
            FileReader fr = null;
            try {
                /* Get the file content into the JSONObject */
                fr = new FileReader(filename);
                JSONObject jsonObject = (JSONObject)parser.parse(fr);
                result = toMap(jsonObject);
                fr.close();
            } catch (Exception ex) {
                if(fr != null) try {
                    fr.close();
                } catch (IOException ex1) {
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex1);
                }
                ex.printStackTrace();
                System.out.println(Utils.class.getName() + " - " + ex);
            }
        }        
        
        return result;
    }
    
    public Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keySet().iterator();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    } 
    
    private List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }    

    private ArrayList RescanSNMPVersion(ArrayList<String[]> node_community_version, int timeout, int retries) {
        ArrayList<String[]> result = new ArrayList();
        
        PrintStream oldError = System.err;
        System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
        
        ArrayList<String[]> node_community_version_oid_in = new ArrayList();
        if(node_community_version.size() > 0) {
            for(String[] it : node_community_version) {
                String[] mas = new String[4];
                mas[0]=it[0]; mas[1]=it[1]; mas[2]=it[2];
                if(mas[2].equals("1")) mas[3]="1.3.6.1.2.1.2.2.1.10";
                else if(mas[2].equals("2")) mas[3]="1.3.6.1.2.1.31.1.1.1.6";
                node_community_version_oid_in.add(mas);
            }
        }
        
        ArrayList<String[]> node_community_version_oid_out = new ArrayList();
        if(node_community_version.size() > 0) {
            for(String[] it : node_community_version) {
                String[] mas = new String[4];
                mas[0]=it[0]; mas[1]=it[1]; mas[2]=it[2];
                if(mas[2].equals("1")) mas[3]="1.3.6.1.2.1.2.2.1.16";
                else if(mas[2].equals("2")) mas[3]="1.3.6.1.2.1.31.1.1.1.10";
                node_community_version_oid_out.add(mas);
            }
        }        
        
        if(node_community_version.size() > 0) {
            WalkPool walkPool = new WalkPool();
            Map<String, ArrayList> res1 = walkPool.Get(node_community_version_oid_in, timeout, retries);
            Map<String, ArrayList> res2 = walkPool.Get(node_community_version_oid_out, timeout, retries);

            if(res1.size() > 0 && res2.size() > 0) {
                Map<String, String> tmp = new HashMap<>();
                for (Map.Entry<String, ArrayList> entry : res1.entrySet()) {
                    String node = entry.getKey();
                    if (res2.get(node) != null) {
                        tmp.put(node, "2");
                    }
                }

                for (String[] mas : node_community_version) {
                    if (mas[2].equals("2")) {
                        if (tmp.get(mas[0]) != null) {
                            String[] buff = new String[3];
                            buff[0] = mas[0];
                            buff[1] = mas[1];
                            buff[2] = "2";
        //                    System.out.println(buff[0]+","+buff[1]+","+buff[2]);
                           result.add(buff); 
                        } else {
                            String[] buff = new String[3];
                            buff[0] = mas[0];
                            buff[1] = mas[1];
                            buff[2] = "1";
            //                System.out.println(buff[0]+","+buff[1]+","+buff[2]);
                            result.add(buff);                            
                        }
                    } else {
                        String[] buff = new String[3];
                        buff[0] = mas[0];
                        buff[1] = mas[1];
                        buff[2] = "1";
        //                System.out.println(buff[0]+","+buff[1]+","+buff[2]);
                        result.add(buff); 
                    }
                }
            }
        }

        // get sysDescription information 
        GetPool getPool = new GetPool();
        ArrayList<String> oid_list = new ArrayList();
        String sysDescr=".1.3.6.1.2.1.1.1.0";
        oid_list.add(sysDescr);
        Map<String, ArrayList<String[]>> res = getPool.Get(result, oid_list, timeout, retries);
        
        // adding sysDescr to node_commenity_version
        ArrayList<String[]> result1 = new ArrayList();
        for(String[] it : result) {
            if(res.get(it[0]) != null && res.get(it[0]).size() == 1) {
                String[] mas = new String[4];
                mas[0]=it[0]; mas[1]=it[1];
                mas[2]=it[2]; mas[3]=res.get(it[0]).get(0)[1];
                result1.add(mas);
            }
        }
        System.setErr(oldError);
        return result1;
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

//        //  output: node ---> id_iface ---> name_iface,in,out
//    public Map<String, Map<String, String[]>> GetTestCountersFromNodes(String filename) {
//        Map<String, Map<String, String[]>> result = new HashMap<>();
//
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(filename));
//            try {
//                String s;
//                ArrayList<String[]> list = new ArrayList();
//                while ((s = in.readLine()) != null) {
//                    String[] buf = s.split(",");
//                    list.add(buf);
//                }
//                
//                for(int i=0; i<list.size(); i++) {
//                   String[] item = list.get(i);
//                   Map<String, String[]> tmp = new HashMap<>();
//                   String[] mas = new String[3];
//                   mas[0]=item[2]; mas[1]=item[3]; mas[2]=item[4];
//                   tmp.put(item[1], mas);
//                   
//                   for(int j=i+1; j<list.size(); j++) { 
//                       String[] item1 = list.get(j);
//                       if(item[0].equals(item1[0])) {
//                           String[] mas1 = new String[3];
//                           mas1[0]=item1[2]; mas1[1]=item1[3]; mas1[2]=item1[4];
//                           tmp.put(item1[1], mas1);
//                           list.remove(j);
//                           j--;
//                       }
//                       
//                   }
//                   result.put(item[0], tmp);
//                }
//                
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }
//        
//        
//        return result;
//    }
    
    //  output: node ---> iface_name ---> iface_id
    private Map<String, ArrayList<ArrayList<String>>> GetNode_Ifaceid_Ifacename(ArrayList<String[]> node_community_version) {
        Map<String, ArrayList<ArrayList<String>>> result = new HashMap<>();
        
        Map<String, Map<String, ArrayList>> getWalkCountersFromNodes = GetWalkInterfacesFromNodes(node_community_version);
        for (Map.Entry<String, ArrayList> entry : getWalkCountersFromNodes.get("ifIndex").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue(); 
            ArrayList<ArrayList<String>> list = new ArrayList();
            for (String[] val : val_list) {
                String id_iface = val[1];
                String[] res = GetIfaceName(node, id_iface, getWalkCountersFromNodes, false);
                if(!res[1].equals("")) {
                    ArrayList mas = new ArrayList();
                    mas.add(res[0]); mas.add(res[1]);
                    list.add(mas);
                }
            }
            result.put(node, list);
        }  
        return result;
    }
   
    //  output: node ---> id_iface ---> name_iface,in,out
    public Map<String, Map<String, String[]>> GetCountersFromNodes(ArrayList<String[]> node_community_version, ArrayList<String[]> dp_links) {
        // get node_version map
        Map<String, String> node_version = new HashMap();
        for(String[] item : node_community_version) {
            node_version.put(item[0], item[2]);
        }
        
        Map<String, ArrayList<String[]>> node_listiface_from_dplinks = new HashMap<>();
        for(String[] item : dp_links) {
            String node = item[0];
            String iface_id = item[1];
            String iface = item[2];
            if(node_listiface_from_dplinks.get(node) != null) {
                String[] mas = new String[2];
                mas[0]=iface_id; mas[1]=iface;
                node_listiface_from_dplinks.get(node).add(mas);
                node_listiface_from_dplinks.put(node, node_listiface_from_dplinks.get(node));
            } else {
                ArrayList<String[]> iface_list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iface_id; mas[1]=iface;
                iface_list.add(mas);
                node_listiface_from_dplinks.put(node, iface_list);
            }
            node = item[3];
            iface_id = item[4];
            iface = item[5];
            if(node_listiface_from_dplinks.get(node) != null) {
                String[] mas = new String[2];
                mas[0]=iface_id; mas[1]=iface;
                node_listiface_from_dplinks.get(node).add(mas);
                node_listiface_from_dplinks.put(node, node_listiface_from_dplinks.get(node));
            } else {
                ArrayList<String[]> iface_list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iface_id; mas[1]=iface;
                iface_list.add(mas);
                node_listiface_from_dplinks.put(node, iface_list);
            } 
        }
        
        Map<String, Map<String, String[]>> result = new HashMap<>();
        Map<String, Map<String, ArrayList>> getWalkCountersFromNodes = GetWalkCountersFromNodes(node_community_version);
        for (Map.Entry<String, ArrayList> entry : getWalkCountersFromNodes.get("ifIndex").entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val_list = entry.getValue();
            Map<String, String[]> tmp = new HashMap<>();
            if(val_list.size() >= 3) {
                for (String[] val : val_list) {
                    String id_iface = val[1];
                    ArrayList<String[]> list2 = getWalkCountersFromNodes.get("ifOperStatus").get(node);
                    if(list2 != null) {
                        for (String[] mas2 : list2) {
                            String id_iface2 = mas2[0].split("\\.")[mas2[0].split("\\.").length - 1];
                            String iface_status = mas2[1];
                            if (id_iface2.equals(id_iface) && iface_status.equals("1")) {
                                ArrayList<String[]> list = getWalkCountersFromNodes.get("ifType").get(node);
                                if(list != null) {
                                    for (String[] mas : list) {
                                        String id_iface1 = mas[0].split("\\.")[mas[0].split("\\.").length - 1];
                                        String iface_type = mas[1];
                                        if (id_iface1.equals(id_iface)) {
    //                                        if (!iface_type.equals("24") && Integer.parseInt(iface_type) <= 32) {
                                            if (iface_type.equals("6")) {
                                                String[] out = new String[4];
                                                String[] res = GetIfaceName(node, id_iface, getWalkCountersFromNodes, false);
                                                out[0]=res[1];
                                                id_iface=res[0];
                                                if(!out[0].equals("")) {
                                                    boolean find=false;
                                                    if(node_listiface_from_dplinks.get(node) != null) {
                                                        for(String[] ifaceid_ifacename : node_listiface_from_dplinks.get(node)) {
                                                            if(ifaceid_ifacename[0].equals(id_iface) || ifaceid_ifacename[1].equals(out[0])) 
                                                            { 
                                                                find=true; 
                                                                break; 
                                                            }
                                                        }
                                                    }
                                                    if(!find) {
                                                        if(node_version.get(node) != null && node_version.get(node).equals("2")) {
                                                            ArrayList<String[]> list1 = getWalkCountersFromNodes.get("ifHCInOctets").get(node);
                                                            if (list1 != null) {
                                                                for (String[] mas1 : list1) {
                                                                    if (mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                                                        out[1] = mas1[1];
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            ArrayList<String[]> list1 = getWalkCountersFromNodes.get("ifInOctets").get(node);
                                                            if (list1 != null) {
                                                                for (String[] mas1 : list1) {
                                                                    if (mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                                                        out[1] = mas1[1];
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if(node_version.get(node) != null && node_version.get(node).equals("2")) {
                                                            ArrayList<String[]> list1 = getWalkCountersFromNodes.get("ifHCOutOctets").get(node);
                                                            if (list1 != null) {
                                                                for (String[] mas1 : list1) {
                                                                    if (mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                                                        out[2] = mas1[1];
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            ArrayList<String[]> list1 = getWalkCountersFromNodes.get("ifOutOctets").get(node);
                                                            if (list1 != null) {
                                                                for (String[] mas1 : list1) {
                                                                    if (mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                                                        out[2] = mas1[1];
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        ArrayList<String[]> list1 = getWalkCountersFromNodes.get("ifSpeed").get(node);
                                                        if (list1 != null) {
                                                            for (String[] mas1 : list1) {
                                                                if (mas1[0].split("\\.")[mas1[0].split("\\.").length - 1].equals(id_iface)) {
                                                                    out[3] = mas1[1];
                                                                    break;
                                                                }
                                                            }
                                                        }

                                                        if (!(out[0] == null || out[1] == null || out[2] == null || out[3] == null || out[3].equals("0"))) {
                                                            tmp.put(id_iface, out);
        //                                                    System.out.println(node+","+id_iface+","+out[0]+","+out[1]+","+out[2]);
                                                        }
                                                    } /* else logger.Println("Exclude node="+node+" iface="+out[0]+" exist in dp_links.", logger.DEBUG); */
                                                }
                                            }
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if(tmp.size() > 0) result.put(node, tmp);
        }
        return result;
    }

    private double Delta(String node, double start, double stop, ArrayList<String[]>node_community_version) {
        double result = -1L;
        double MAX_VALUE32=Math.pow(2, 32);
        double MAX_VALUE64=Math.pow(2, 64);
        
        double delta = stop-start;
        if(delta >=0) result=delta;
        else {
            for(String[] item : node_community_version) {
                if(item[0].equals(node)) {
                    if(item[2].equals("1")) result=MAX_VALUE32+stop-start;
                    else result=MAX_VALUE64+stop-start;
                    break;
                }
            }
        }
        
        return result;
    }
    
    //  output: LinkedList(ArrayList(node, id_iface, name_iface, in, out))
    public Map<String, LinkedList<ArrayList>> DeltaCounters(Map<String, Map<String, String[]>> start_counters, Map<String, Map<String, String[]>> stop_counters, ArrayList<String[]>node_community_version) {
        LinkedList<ArrayList> result = new LinkedList();

        for (Map.Entry<String, Map<String, String[]>> entry : start_counters.entrySet()) {
            String node = entry.getKey();
            Map<String, String[]> val_list = entry.getValue();
            for (Map.Entry<String, String[]> entry1 : val_list.entrySet()) {
                ArrayList tmp = new ArrayList();
                tmp.add(node);
                String id_iface = entry1.getKey();
                tmp.add(id_iface);
                String[] list_start = entry1.getValue();
                tmp.add(list_start[0]);
                if(stop_counters.get(node) != null) {
                    String[] list_stop = stop_counters.get(node).get(id_iface);
                    if (list_stop != null) {
                        double delta = Delta(node, Double.parseDouble(list_start[1]), Double.parseDouble(list_stop[1]), node_community_version);
                        if(delta >= 0) tmp.add(delta);
                        else continue;
                        
                        delta = Delta(node, Double.parseDouble(list_start[2]), Double.parseDouble(list_stop[2]), node_community_version);
                        if(delta >= 0) tmp.add(delta);
                        else continue;
                        if (tmp.size() == 5 && (Double) tmp.get(3) != 0 && (Double) tmp.get(4) != 0) {
                            tmp.add((Double) tmp.get(3) + (Double) tmp.get(4));
                        } else {
                            continue;
                        }
                        int type = 0;
                        for(String[] mas : node_community_version) {
                            if(mas[0].equals(node)) {
                                if(mas[3].equals("cdp")) type=1;
                                else if(mas[3].equals("lldp")) type=2;
                                else if(mas[3].equals("cdp,lldp")) type=3;
                                break;
                            }
                        }
                        tmp.add(type);
                        tmp.add(list_start[3]);
                        
                        if(tmp.size() == 8) {
                            result.add(tmp);
                            logger.Println("delta: "+tmp.get(0)+","+tmp.get(1)+","+tmp.get(2)+","+tmp.get(3)+","+tmp.get(4)+","+tmp.get(5)+","+tmp.get(6)+","+tmp.get(7), logger.DEBUG);
//                            System.out.println("delta: "+(String)tmp.get(0)+","+(String)tmp.get(1)+","+(String)tmp.get(2)+","+Math.round((double)tmp.get(3))+","+Math.round((double)tmp.get(4))+","+(double)tmp.get(5));
                        }
                    }
                }
            }
        }
        
        // split to speed
        Map<String, LinkedList<ArrayList>> result_map = new HashMap();
        for(ArrayList item : result) {
            String speed = (String)item.get(7);
            if(result_map.get(speed) != null) result_map.get(speed).add(item);
            else {
                LinkedList<ArrayList> tmp_list = new LinkedList();
                tmp_list.add(item);
                result_map.put(speed, tmp_list);
            }
        }
        return result_map;
    }
    
    public Map<String, ArrayList<ArrayList<String>>> GetCalculateLinks(Map<String, Map> informationFromNodesAllAreas, 
            Map<String, ArrayList<String[]>> area_node_community_version_dp, 
            int pause, int pause_test, double precession_limit, int retries_testing, int limit_retries_testing) {
        // check needed calculate links
        Map<String, Boolean> next_counters_links_areas = new HashMap();
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            Map<String, Map> val = area.getValue();
            Map<String, Map> nodes_information = val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
            Map node_protocol_accounts = (Map) val.get("node_protocol_accounts");

            if (links != null) {
                for (Map.Entry<String, Map> entry : nodes_information.entrySet()) {
                    String node = entry.getKey();
                    boolean find = false;
                    if (node_protocol_accounts.get(node) != null) {
                        for (ArrayList<String> item : links) {
                            if (node.equals(item.get(0)) || node.equals(item.get(2))) {
                                find = true;
                                break;
                            }
                        }
                        if (!find) {
                            next_counters_links_areas.put(area_name, true);
                            break;
                        }
                    }
                }
            }
            if (next_counters_links_areas.get(area_name) == null) {
                next_counters_links_areas.put(area_name, false);
            }
        }        
        
        Map<String, Map<String, Map<String, String[]>>> start_counters = new HashMap();
        Map<String, Map<String, Map<String, String[]>>> stop_counters = new HashMap();
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            if (next_counters_links_areas.containsKey(area_name) && next_counters_links_areas.get(area_name)) {
                Map node_protocol_accounts = (Map) val.get("node_protocol_accounts");
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (node_community_version_area != null && node_community_version_area.size() > 1) {
                    logger.Println("Start get counters area " + area_name + " ...", logger.INFO);
                    ArrayList<ArrayList<String>> links_tmp = (ArrayList) val.get("links");
                    ArrayList<String[]> links = new ArrayList();
                    if (links_tmp != null) {
                        for (ArrayList<String> it1 : links_tmp) {
                            String[] mas = new String[6];
                            mas[0] = it1.get(0);
                            mas[1] = it1.get(1);
                            mas[2] = it1.get(2);
                            mas[3] = it1.get(3);
                            mas[4] = it1.get(4);
                            mas[5] = it1.get(5);
                            links.add(mas);
                        }
                    }
                    Map<String, Map<String, String[]>> start_counters_area = GetCountersFromNodes(node_community_version_area, links);
                    start_counters.put(area_name, start_counters_area);
                    logger.Println("Stop get counters area " + area_name + ".", logger.INFO);
                }
            }
        }

        Waiting(pause);

        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            if (next_counters_links_areas.containsKey(area_name) && next_counters_links_areas.get(area_name)) {
                Map node_protocol_accounts = (Map) val.get("node_protocol_accounts");
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (node_community_version_area != null && node_community_version_area.size() > 1) {
                    logger.Println("Start get counters area " + area_name + " ...", logger.INFO);
                    ArrayList<ArrayList<String>> links_tmp = (ArrayList) val.get("links");
                    ArrayList<String[]> links = new ArrayList();
                    if (links_tmp != null) {
                        for (ArrayList<String> it1 : links_tmp) {
                            String[] mas = new String[6];
                            mas[0] = it1.get(0);
                            mas[1] = it1.get(1);
                            mas[2] = it1.get(2);
                            mas[3] = it1.get(3);
                            mas[4] = it1.get(4);
                            mas[5] = it1.get(5);
                            links.add(mas);
                        }
                    }
                    Map<String, Map<String, String[]>> stop_counters_area = GetCountersFromNodes(node_community_version_area, links);
                    stop_counters.put(area_name, stop_counters_area);
                    logger.Println("Stop get counters area " + area_name + ".", logger.INFO);
                }
            }
        }

        Map<String, Map<String, LinkedList<ArrayList>>> deltaCounters = new HashMap();
        for (Map.Entry<String, Map<String, Map<String, String[]>>> area : ((Map<String, Map<String, Map<String, String[]>>>) start_counters).entrySet()) {
            String area_name = area.getKey();
            Map<String, Map<String, String[]>> val_start = area.getValue();
            ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
            if (stop_counters.get(area_name) != null && node_community_version_area != null && node_community_version_area.size() > 0) {
                Map<String, Map<String, String[]>> val_stop = ((Map<String, Map<String, String[]>>) stop_counters.get(area_name));
                Map<String, LinkedList<ArrayList>> speed_deltaCounters_area = DeltaCounters(val_start, val_stop, node_community_version_area);
                // write to file speed_deltaCounters_area
                Map<String, LinkedList<ArrayList>> speed_deltaCounters_area_sort = new HashMap();
                for (Map.Entry<String, LinkedList<ArrayList>> entry : speed_deltaCounters_area.entrySet()) {
                    String speed = entry.getKey();
                    LinkedList<ArrayList> deltaCounters_area = entry.getValue();
                    // sorting ...
                    Collections.sort(deltaCounters_area, new Comparator<ArrayList>() {
                        @Override
                        public int compare(ArrayList o1, ArrayList o2) {
                            if ((double) o1.get(5) < (double) o2.get(5)) {
                                return -1;
                            } else if ((double) o1.get(5) == (double) o2.get(5)) {
                                return 0;
                            } else {
                                return 1;
                            }
                        }
                    });
                    speed_deltaCounters_area_sort.put(speed, deltaCounters_area);
                    deltaCounters.put(area_name, speed_deltaCounters_area_sort);
                }
            }
        }
        // write to file deltaCounters
        Map<String, ArrayList<String[]>> links_calculate = new HashMap();
        for (Map.Entry<String, Map<String, LinkedList<ArrayList>>> area : ((Map<String, Map<String, LinkedList<ArrayList>>>) deltaCounters).entrySet()) {
            String area_name = area.getKey();
            Map<String, LinkedList<ArrayList>> speed_deltaCounters_area = area.getValue();
            logger.Println("Start CalculateLinks ...", logger.INFO);
            ArrayList<String[]> links_area = new ArrayList();
            for (Map.Entry<String, LinkedList<ArrayList>> entry : speed_deltaCounters_area.entrySet()) {
                String speed = entry.getKey();
                logger.Println("Calculate speed = " + speed, logger.DEBUG);
                LinkedList<ArrayList> deltaCounters_list = entry.getValue();
                ArrayList<String[]> links_area_tmp = CalculateLinks(deltaCounters_list, precession_limit);
                links_area.addAll(links_area_tmp);
            }
            links_calculate.put(area_name, links_area);
            logger.Println("Stop CalculateLinks.", logger.INFO);
        }

        // write to file links_calculate
        Map<String, ArrayList<String[]>>[] testingLinks = new HashMap[retries_testing];
        for (int n = 0; n < retries_testing; n++) {
            testingLinks[n] = new HashMap();
        }
        for (int ii = 0; ii < retries_testing; ii++) {
            Map<String, ArrayList<String[]>> testingLinks_start_map = new HashMap();
            for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                String area_name = area.getKey();
                Map val = area.getValue();
                ArrayList<String[]> links_calculate_area = (ArrayList<String[]>) links_calculate.get(area_name);
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (links_calculate_area != null && links_calculate_area.size() > 0 && node_community_version_area != null && node_community_version_area.size() > 0) {
                    ArrayList<String[]> links = new ArrayList();
                    for (String[] list : links_calculate_area) {
                        String[] mas = new String[6];
                        mas[0] = list[0];
                        mas[1] = list[1];
                        mas[2] = list[2];
                        mas[3] = list[3];
                        mas[4] = list[4];
                        mas[5] = list[5];
                        logger.Println("Prepare link for testing = " + mas[0] + ", " + mas[1] + ", " + mas[2] + " <--->" + mas[3] + ", " + mas[4] + ", " + mas[5], logger.DEBUG);
                        links.add(mas);
                    }

                    ArrayList<String[]>[] calculateTestingLinks = new ArrayList[retries_testing];
                    logger.Println("Start get counters for testing links area " + area_name + " ...", logger.INFO);
                    ArrayList<String[]> testingLinks_start = GetCountersTestingLinks(links, node_community_version_area);
                    testingLinks_start_map.put(area_name, testingLinks_start);
                    logger.Println("Stop get counters for testing links area " + area_name + ".", logger.INFO);
                }
            }

            Waiting(pause_test);

            Map<String, ArrayList<String[]>> testingLinks_stop_map = new HashMap();
            for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                String area_name = area.getKey();
                Map val = area.getValue();
                ArrayList<String[]> links_calculate_area = (ArrayList<String[]>) links_calculate.get(area_name);
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (links_calculate_area != null && links_calculate_area.size() > 0 && node_community_version_area != null && node_community_version_area.size() > 0) {
                    ArrayList<String[]> links = new ArrayList();
                    for (String[] list : links_calculate_area) {
                        String[] mas = new String[6];
                        mas[0] = list[0];
                        mas[1] = list[1];
                        mas[2] = list[2];
                        mas[3] = list[3];
                        mas[4] = list[4];
                        mas[5] = list[5];
                        links.add(mas);
                    }

                    ArrayList<String[]>[] calculateTestingLinks = new ArrayList[retries_testing];
                    logger.Println("Start get counters for testing links area " + area_name + " ...", logger.INFO);
                    ArrayList<String[]> testingLinks_stop = GetCountersTestingLinks(links, node_community_version_area);
                    testingLinks_stop_map.put(area_name, testingLinks_stop);
                    logger.Println("Stop get counters for testing links area " + area_name + ".", logger.INFO);
                }
            }

            Map<String, ArrayList<String[]>> calculateTestingLinks = new HashMap();
            for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                String area_name = area.getKey();
                Map val = area.getValue();
                ArrayList<String[]> testingLinks_start = testingLinks_start_map.get(area_name);
                ArrayList<String[]> testingLinks_stop = testingLinks_stop_map.get(area_name);
                ArrayList<String[]> node_community_version_area = area_node_community_version_dp.get(area_name);
                if (testingLinks_start_map != null && testingLinks_start_map.size() > 0
                        && testingLinks_stop_map != null && testingLinks_stop_map.size() > 0
                        && testingLinks_start != null && testingLinks_start.size() > 0
                        && testingLinks_stop != null && testingLinks_stop.size() > 0) {
                    ArrayList<String[]> calculateTestingLinks_area = CalculateTestingLinks(testingLinks_start, testingLinks_stop, precession_limit, node_community_version_area);
                    calculateTestingLinks.put(area_name, calculateTestingLinks_area);
                }

            }
            testingLinks[ii] = calculateTestingLinks;
        }

        Map<String, ArrayList<String[]>> links_map = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : testingLinks[0].entrySet()) {
            String area_name = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            ArrayList<String[]> links_list = new ArrayList();
            for (String[] link : val) {
                String[] mas = new String[9];
                mas[0] = link[0];
                mas[1] = link[1];
                mas[2] = link[2];
                mas[3] = link[3];
                mas[4] = link[4];
                mas[5] = link[5];
                mas[6] = link[6];
                mas[7] = link[7];
                mas[8] = "1";
                links_list.add(mas);
            }
            links_map.put(area_name, links_list);
        }
        for (int ii = 1; ii < retries_testing; ii++) {
            for (Map.Entry<String, ArrayList<String[]>> entry : testingLinks[ii].entrySet()) {
                String area_name = entry.getKey();
                ArrayList<String[]> val = entry.getValue();
                if (links_map.get(area_name) != null) {
                    for (String[] link : val) {
                        for (String[] link1 : links_map.get(area_name)) {
                            if (link[0].equals(link1[0]) && link[1].equals(link1[1])
                                    && link[2].equals(link1[2]) && link[3].equals(link1[3])
                                    && link[4].equals(link1[4]) && link[5].equals(link1[5])) {
                                link1[8] = String.valueOf(Integer.parseInt(link1[8]) + 1);
                                break;
                            }
                        }

                    }
                }
            }
        }

        Map<String, ArrayList<String[]>> links_from_counters = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : links_map.entrySet()) {
            String area_name = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            ArrayList<String[]> list_tmp = new ArrayList();
            for (String[] link : val) {
                if (link.length == 9) {
                    if (Integer.parseInt(link[8]) >= limit_retries_testing) {
                        String[] mas = new String[9];
                        mas[0] = link[0];
                        mas[1] = link[1];
                        mas[2] = link[2];
                        mas[3] = link[3];
                        mas[4] = link[4];
                        mas[5] = link[5];
                        mas[6] = link[6];
                        mas[7] = link[7];
                        mas[8] = link[8];
                        list_tmp.add(mas);
                    }
                }

            }
            links_from_counters.put(area_name, list_tmp);
        }

        Map<String, ArrayList<ArrayList<String>>> area_links_calculate = new HashMap();
        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
            String area_name = area.getKey();
//            Map val = area.getValue();
            ArrayList<String[]> links_calculate_area = links_from_counters.get(area_name);
            ArrayList<ArrayList<String>> links_calculate_list = new ArrayList();
            if (links_calculate_area != null) {
                for (String[] link : links_calculate_area) {
                    ArrayList list = new ArrayList();
                    for (String s : link) {
                        list.add(s);
                    }
                    links_calculate_list.add(list);
                }
                if (links_calculate_list.size() > 0) {
                    
                    area_links_calculate.put(area_name, links_calculate_list);
                }
            }
        }
        
        for (Map.Entry<String, ArrayList<ArrayList<String>>> area : area_links_calculate.entrySet()) {
            String area_name = area.getKey();
            ArrayList<ArrayList<String>> val = area.getValue();
            for(ArrayList<String> link : val) {
                String str = area_name;
                for(String item : link) {
                    str = str+", "+item;
                }
                logger.Println("area_links_calculate : "+str, logger.DEBUG);
            }
        }
        
        return area_links_calculate;
    }
    
    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2))     pause in seconds
    public ArrayList<String[]> CalculateLinks(LinkedList<ArrayList> deltaCounters, double precession_limit) {
        ArrayList<String[]> result = new ArrayList();

        for(int num=4; num>=0; num--) {
            double precession_limit_calc=precession_limit/Math.pow(2,num);
            logger.Println("precession_limit_calc = "+precession_limit_calc, logger.DEBUG);
            while(deltaCounters.size() >= 2) {
                int pos1=-1; int pos2=-1;
                double precession=0; 
                double min = Math.pow(2, 64);
                long start_time = System.currentTimeMillis();
                for (int i = 0; i < deltaCounters.size(); i++) {
                    ArrayList item1 = deltaCounters.get(i);
                    int type1 = (int)item1.get(6);
                    for (int j = i + 1; j < deltaCounters.size(); j++) {
                        ArrayList item2 = deltaCounters.get(j);
                        int type2 = (int)item2.get(6);
                        if(type1 == 3) {
                            if(type2 == 1) continue;
                            if(type2 == 2) continue;
                            if(type2 == 3) continue;
                        }
                        else if(type1 == 1) if(type2 == 1) continue;
                        else if(type1 == 2) if(type2 == 2) continue;
                        
                        double delta_sum = Math.abs((double)item1.get(5)-(double)item2.get(5));
                        double sum = (double)item1.get(5)+(double)item2.get(5);
                        if ( !item1.get(0).equals(item2.get(0)) ) {
                            if (2*delta_sum/sum < precession_limit_calc) {
                                double delta1=abs((double)item1.get(3)-(double)item2.get(4));
                                double sum1=(double)item1.get(3)+(double)item2.get(4);
                                double delta2=abs((double)item1.get(4)-(double)item2.get(3));
                                double sum2=(double)item1.get(4)+(double)item2.get(3);
                                double prec1 = 2*delta1/sum1;
                                double prec2 = 2*delta2/sum2;
                                double delta = (delta1+delta2)/2;
                                precession = (prec1+prec2)/2;
                                if(precession < min) { min=precession; pos1=i; pos2=j; }

                            } else {
//                                if(pos1 == -1 && pos2 == -1) {
//                                    deltaCounters.remove(i);
//                                    System.out.println("Remove delta: "+(String)deltaCounters.get(i).get(0)+","+(String)deltaCounters.get(i).get(1)+","+(String)deltaCounters.get(i).get(2));
//                                    i--;
//                                    j--;
//                                }
                                break;
                            }
                        } 

                    }
        //            System.out.println(i);
                }
                long stop_time = System.currentTimeMillis();
                logger.Println((stop_time-start_time)/1000+" sec.  records="+deltaCounters.size(), logger.DEBUG);

                if( !(pos1 == -1 || pos2 == -1 || deltaCounters.size() <= 1) ) {
                    String[] mas = new String[6];
                    mas[0]=(String)deltaCounters.get(pos1).get(0);
                    mas[1]=(String)deltaCounters.get(pos1).get(1);
                    mas[2]=(String)deltaCounters.get(pos1).get(2);
                    mas[3]=(String)deltaCounters.get(pos2).get(0);
                    mas[4]=(String)deltaCounters.get(pos2).get(1);
                    mas[5]=(String)deltaCounters.get(pos2).get(2);

                    double delta1=abs((double)deltaCounters.get(pos1).get(3)-(double)deltaCounters.get(pos2).get(4));
                    double sum1=(double)deltaCounters.get(pos1).get(3)+(double)deltaCounters.get(pos2).get(4);
                    double delta2=abs((double)deltaCounters.get(pos1).get(4)-(double)deltaCounters.get(pos2).get(3));
                    double sum2=(double)deltaCounters.get(pos1).get(4)+(double)deltaCounters.get(pos2).get(3);
                    double prec1 = 2*delta1/sum1;
                    double prec2 = 2*delta2/sum2;
                    double delta = (delta1+delta2)/2;
                    precession = (prec1+prec2)/2;

                    if(precession < 5*precession_limit && !mas[0].equals(mas[3])) {
                        logger.Println("Calculate link: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" --- "+precession, logger.DEBUG);
                        result.add(mas);
                        deltaCounters.remove(pos1);
                        deltaCounters.remove(pos2-1);                        
                    } else break;

                } else break;

            }
        }
        
        return result;
    }

//    public void TestCalculateLinks() {
//        
//        LinkedList<ArrayList> deltaCounters = new LinkedList();
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("delta"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    ArrayList tmp = new ArrayList();
//                    
//                    tmp.add(s.split(",")[0]);
//                    tmp.add(s.split(",")[1]);
//                    tmp.add(s.split(",")[2]);
//                    tmp.add(Double.parseDouble(s.split(",")[3]));
//                    tmp.add(Double.parseDouble(s.split(",")[4]));
//                    tmp.add(Double.parseDouble(s.split(",")[3])+Double.parseDouble(s.split(",")[4]));
//                    deltaCounters.add(tmp);
//
//                }
//            } finally {
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }    
//        
//        
//        ArrayList<String[]> result = new ArrayList();
//        double precession_limit = 0.02;
//
//        for(int num=4; num>=0; num--) {
//            double precession_limit_calc=precession_limit/Math.pow(2,num);
//            logger.Println("precession_limit_calc = "+precession_limit_calc, logger.DEBUG);
//            while(deltaCounters.size() >= 2) {
//                int pos1=-1; int pos2=-1;
//                double precession=0; 
//                double min = Math.pow(2, 64);
//                long start_time = System.currentTimeMillis();
//                for (int i = 0; i < deltaCounters.size(); i++) {
//                    ArrayList item1 = deltaCounters.get(i);
//                    for (int j = i + 1; j < deltaCounters.size(); j++) {
//                        ArrayList item2 = deltaCounters.get(j);
//                        double delta_sum = Math.abs((double)item1.get(5)-(double)item2.get(5));
//                        double sum = (double)item1.get(5)+(double)item2.get(5);
//                        if ( !item1.get(0).equals(item2.get(0)) ) {
//                            if (2*delta_sum/sum < precession_limit_calc) {
//                                double delta1=abs((double)item1.get(3)-(double)item2.get(4));
//                                double sum1=(double)item1.get(3)+(double)item2.get(4);
//                                double delta2=abs((double)item1.get(4)-(double)item2.get(3));
//                                double sum2=(double)item1.get(4)+(double)item2.get(3);
//                                double prec1 = 2*delta1/sum1;
//                                double prec2 = 2*delta2/sum2;
//                                double delta = (delta1+delta2)/2;
//                                precession = (prec1+prec2)/2;
//                                if(precession < min) { min=precession; pos1=i; pos2=j; }
//
//                            } else {
////                                if(pos1 == -1 && pos2 == -1) {
////                                    deltaCounters.remove(i);
////                                    System.out.println("Remove delta: "+(String)deltaCounters.get(i).get(0)+","+(String)deltaCounters.get(i).get(1)+","+(String)deltaCounters.get(i).get(2));
////                                    i--;
////                                    j--;
////                                }
//                                break;
//                            }
//                        } 
//
//                    }
//        //            System.out.println(i);
//                }
//                long stop_time = System.currentTimeMillis();
//                System.out.println( (stop_time-start_time)/1000+" sec.  records="+deltaCounters.size());
//
//                if( !(pos1 == -1 || pos2 == -1 || deltaCounters.size() <= 1) ) {
//                    String[] mas = new String[6];
//                    mas[0]=(String)deltaCounters.get(pos1).get(0);
//                    mas[1]=(String)deltaCounters.get(pos1).get(1);
//                    mas[2]=(String)deltaCounters.get(pos1).get(2);
//                    mas[3]=(String)deltaCounters.get(pos2).get(0);
//                    mas[4]=(String)deltaCounters.get(pos2).get(1);
//                    mas[5]=(String)deltaCounters.get(pos2).get(2);
//
//                    double delta1=abs((double)deltaCounters.get(pos1).get(3)-(double)deltaCounters.get(pos2).get(4));
//                    double sum1=(double)deltaCounters.get(pos1).get(3)+(double)deltaCounters.get(pos2).get(4);
//                    double delta2=abs((double)deltaCounters.get(pos1).get(4)-(double)deltaCounters.get(pos2).get(3));
//                    double sum2=(double)deltaCounters.get(pos1).get(4)+(double)deltaCounters.get(pos2).get(3);
//                    double prec1 = 2*delta1/sum1;
//                    double prec2 = 2*delta2/sum2;
//                    double delta = (delta1+delta2)/2;
//                    precession = (prec1+prec2)/2;
//
//                    if(precession < 5*precession_limit) {
////                        System.out.println("Calculate link: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" --- "+precession);
//                        result.add(mas);
//                        deltaCounters.remove(pos1);
//                        deltaCounters.remove(pos2-1);                        
//                    } else break;
//                } else break;
//
//            }
//        }
////        System.out.println("11111111111111");
//    }

    // output: oid_key ---> node ---> ArrayList(String,String) 
    private Map<String, Map<String, ArrayList>> GetWalkCountersFromNodes(ArrayList<String[]> node_community_version) {
        Map<String, String> oids = new HashMap<>();
        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
        oids.put("ifOperStatus", "1.3.6.1.2.1.2.2.1.8");
        oids.put("ifInOctets", "1.3.6.1.2.1.2.2.1.10");
        oids.put("ifOutOctets", "1.3.6.1.2.1.2.2.1.16");
        oids.put("ifSpeed", "1.3.6.1.2.1.2.2.1.5");

        Map<String, String> oidsV2 = new HashMap<>();
        oidsV2.put("ifHCInOctets", "1.3.6.1.2.1.31.1.1.1.6");
        oidsV2.put("ifHCOutOctets", "1.3.6.1.2.1.31.1.1.1.10");
        
        Map<String, Map<String, ArrayList>> result = new HashMap<>();

        PrintStream oldError = System.err;
        System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
        
        WalkPool walkPool = new WalkPool();
        for (Map.Entry<String, String> entry : oids.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            Map<String, ArrayList> res = walkPool.Get(node_community_version, val);
            Map tmp = new HashMap(res);
            result.put(key, tmp);
        }
        
        ArrayList<String[]> node_community_V2 = new ArrayList();
        for(String[] item : node_community_version) {
            if(item[2].equals("2")) node_community_V2.add(item);
        }
        for (Map.Entry<String, String> entry : oidsV2.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            Map<String, ArrayList> res = walkPool.Get(node_community_V2, val);
            Map tmp = new HashMap(res);
            result.put(key, tmp);
        }
        System.setErr(oldError);
        return result;
    }
    
    // output: oid_key ---> node ---> ArrayList(String,String) 
//    private Map<String, Map<String, ArrayList>> GetWalkInformationFromNodes(ArrayList<String[]> node_community_version) {
//        Map<String, String> oids = new HashMap<>();
//        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
//        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
//        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
//        oids.put("ifMTU", "1.3.6.1.2.1.2.2.1.4");
//        oids.put("ifSpeed", "1.3.6.1.2.1.2.2.1.5");
//        oids.put("ifMAC", "1.3.6.1.2.1.2.2.1.6");
//        oids.put("ifAdminStatus", "1.3.6.1.2.1.2.2.1.7");
//        oids.put("ifOperStatus", "1.3.6.1.2.1.2.2.1.8");
//        oids.put("ifIpAddress", "1.3.6.1.2.1.4.20.1.2");
//        oids.put("ifIpNetMask", "1.3.6.1.2.1.4.20.1.3");
//        oids.put("NetRoute", "1.3.6.1.2.1.4.21.1.1");
//        oids.put("RouteMetric", "1.3.6.1.2.1.4.21.1.3");
//        oids.put("RouteDestination", "1.3.6.1.2.1.4.21.1.7");
//        oids.put("RouteType", "1.3.6.1.2.1.4.21.1.8");
//        oids.put("RouteProto", "1.3.6.1.2.1.4.21.1.9");
//        oids.put("RouteAge", "1.3.6.1.2.1.4.21.1.10");
//        oids.put("RouteMask", "1.3.6.1.2.1.4.21.1.11");
//        oids.put("IdNameVlan", "1.3.6.1.2.1.17.7.1.4.3.1.1");
//        oids.put("IdVlanToNumberInterface", "1.3.6.1.2.1.16.22.1.1.1.1.4.1.3.6.1.2.1.16.22.1.4.1");
//        oids.put("TaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.4");
//        oids.put("UnTaggedVlan", "1.3.6.1.2.1.17.7.1.4.2.1.5");
//        oids.put("IdNameVlanCisco", "1.3.6.1.4.1.9.9.46.1.3.1.1.4.1");
//        oids.put("IdVlanToNumberInterfaceCisco", "1.3.6.1.4.1.9.9.128.1.1.1.1.3");
//        oids.put("VlanType", "1.3.6.1.4.1.9.9.46.1.6.1.1.3");
//        oids.put("VlanPortAccessModeCisco", "1.3.6.1.4.1.9.9.68.1.2.1.1.2");
//        oids.put("IndexInterfaceCisco", "1.3.6.1.4.1.9.5.1.4.1.1.11");
//        oids.put("IndexInterfaceCiscoSec", "1.3.6.1.4.1.9.9.82.1.5.1.1.2");
//        oids.put("PortTrunkNativeVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.5");
//        oids.put("PortTrunkVlanCisco", "1.3.6.1.4.1.9.9.46.1.6.1.1.11");
//        oids.put("vlanTrunkPortDynamicStatus","1.3.6.1.4.1.9.9.46.1.6.1.1.14");
//        oids.put("VlanNameHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.2");
//        oids.put("VlanIdHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.4.1.5");
//        oids.put("VlanPortStateHP", "1.3.6.1.4.1.11.2.14.11.5.1.3.1.1.8.1.1");
//        oids.put("VlanNameTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.2");
//        oids.put("VlanIdTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.3");
//        oids.put("VlanIdTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.3");
//        oids.put("TagPortsTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.4");
//        oids.put("UnTagPortsTelesyn", "1.3.6.1.4.1.207.8.15.4.1.1.5");
//        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
//        oids.put("Duplex_Allied", "1.3.6.1.4.1.207.8.10.3.1.1.5");
//        oids.put("Duplex_Asante", "1.3.6.1.4.1.298.1.5.1.2.6.1.7");
//        oids.put("Duplex_Dell", "1.3.6.1.4.1.89.43.1.1.4");
//        oids.put("Duplex_Foundry", "1.3.6.1.4.1.1991.1.1.3.3.1.1.4");
//        oids.put("Duplex_Cisco2900", "1.3.6.1.4.1.9.9.87.1.4.1.1.32");
//        oids.put("Duplex_HP", "1.3.6.1.2.1.26.2.1.1.3");
//        oids.put("Duplex_Cisco", "1.3.6.1.2.1.10.7.2.1.19");
//        oids.put("IpAddress", "1.3.6.1.2.1.4.20.1.1");
//        oids.put("VlanCommunity", "1.3.6.1.2.1.47.1.2.1.1.4");
//        oids.put("NumSwitchPorts", "1.3.6.1.2.1.17.1.2.0");
//        oids.put("lldpRemManAddrIfId", "1.0.8802.1.1.2.1.4.2.1.4");
//        oids.put("lldpRemPortId", "1.0.8802.1.1.2.1.4.1.1.7");
//        oids.put("lldpRemChassisId","1.0.8802.1.1.2.1.4.1.1.5");
//        oids.put("lldpRemManAddrIfSubtype","1.0.8802.1.1.2.1.4.2.1.3");
//        oids.put("lldpRemSysName","1.0.8802.1.1.2.1.4.1.1.9");
//        oids.put("lldpRemSysDesc", "1.0.8802.1.1.2.1.4.1.1.10");
//        oids.put("ldpLocPortId", "1.0.8802.1.1.2.1.3.7.1.3");
//        oids.put("cdpCacheAddress", "1.3.6.1.4.1.9.9.23.1.2.1.1.4");
//        oids.put("cdpCacheDevicePort", "1.3.6.1.4.1.9.9.23.1.2.1.1.7");
//        oids.put("cdpRemSysName", "1.3.6.1.4.1.9.9.23.1.2.1.1.6");
//        oids.put("cdpRemSysDesc", "1.3.6.1.4.1.9.9.23.1.2.1.1.8");
//        oids.put("vmVlanType", "1.3.6.1.4.1.9.9.68.1.2.2.1.1");
//        oids.put("vmVlan", "1.3.6.1.4.1.9.9.68.1.2.2.1.2");
//        oids.put("vmVlans", "1.3.6.1.4.1.9.9.68.1.2.2.1.4");
//        oids.put("portMembersPortChannel", "1.2.840.10006.300.43.1.2.1.1.12");
//        oids.put("IfaceMaping", "1.3.6.1.2.1.17.1.4.1.2");
//        oids.put("entPhysicalDescr", "1.3.6.1.2.1.47.1.1.1.1.2");
//        oids.put("entPhysicalSerialNumber", "1.3.6.1.2.1.47.1.1.1.1.11");
//
//        Map<String, Map<String, ArrayList>> result = new HashMap<>();
//
//        WalkPool walkPool = new WalkPool();
//        for (Map.Entry<String, String> entry : oids.entrySet()) {
//            String key = entry.getKey();
//            String val = entry.getValue();
//            Map<String, ArrayList> res = walkPool.Get(node_community_version, val);
//            Map tmp = new HashMap(res);
//            result.put(key, tmp);
//        }
//        return result;
//    }     

    // output: oid_key ---> node ---> ArrayList(String,String) 
    private Map<String, Map<String, ArrayList>> GetWalkInterfacesFromNodes(ArrayList<String[]> node_community_version) {
        Map<String, String> oids = new HashMap<>();
        oids.put("ifIndex", "1.3.6.1.2.1.2.2.1.1");
        oids.put("ifDescr", "1.3.6.1.2.1.2.2.1.2");
        oids.put("IfNameExtendedIfName", "1.3.6.1.2.1.31.1.1.1.1");
        oids.put("ifType", "1.3.6.1.2.1.2.2.1.3");
        String IfaceMaping = "1.3.6.1.2.1.17.1.4.1.2";
        String vlan_community = "1.3.6.1.2.1.47.1.2.1.1.4";

        Map<String, Map<String, ArrayList>> result = new HashMap<>();
        WalkPool walkPool = new WalkPool();

        ArrayList<String[]> list_node_community_version_oid = new ArrayList();
        for(String[] item : node_community_version) {
            String[] mas = new String[4];
            mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
            mas[3]=IfaceMaping;
            list_node_community_version_oid.add(mas);            
        }

        ArrayList<ArrayList> node_community_version_oid_list = new ArrayList();
        Map<String, ArrayList> res = walkPool.Get(node_community_version, vlan_community);
        if(res != null && res.size() > 0) {
            for(String[] item : list_node_community_version_oid) {
                boolean find=false;
                for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
                    String node = entry.getKey();
                    ArrayList<String[]> val = entry.getValue();
                    if(item[0].equals(node)) {
                        ArrayList node_community_version_oid = new ArrayList();
                        node_community_version_oid.add(item[0]);
                        ArrayList community_list = new ArrayList();
                        for(String[] item1 : val) {
//                            community_list.add(TranslateHexString_to_SymbolString(item1[1]).replaceAll("NUL", "@"));
                            String community = TranslateHexString_to_SymbolString(item1[1]).replaceAll("NUL", "@");
                            if(!community_list.contains(community))
                                if(community != null && !community.equals("") && community.matches(".+@\\d+"))
                                    community_list.add(community);                            
                        }
                        if(community_list.size() == 0) {
                            for(String[] item1 : list_node_community_version_oid) {
                                if(item1[0].equals(node)) {
                                    community_list.add(item1[1]);
                                    break;
                                }
                            }
                        }
                        node_community_version_oid.add(community_list);
                        node_community_version_oid.add(item[2]);
                        node_community_version_oid.add(item[3]);
                        node_community_version_oid_list.add(node_community_version_oid);
                        find=true;
                        break;
                    }
                }
                if(!find) {
                    ArrayList node_community_version_oid = new ArrayList();
                    node_community_version_oid.add(item[0]);
                    ArrayList community_list = new ArrayList();
//                    community_list.add(TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@"));
                    String community = TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                    if(!community_list.contains(community))
                        if(community != null && !community.equals(""))
                            community_list.add(community);                    
                    node_community_version_oid.add(community_list); 
                    node_community_version_oid.add(item[2]); 
                    node_community_version_oid.add(item[3]);
                    node_community_version_oid_list.add(node_community_version_oid);                  
                }
            }
        } else {
            for(String[] item : list_node_community_version_oid) {
                ArrayList node_community_version_oid = new ArrayList();
                node_community_version_oid.add(item[0]);
                ArrayList community_list = new ArrayList();
//                community_list.add(TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@"));
                String community = TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                if(!community_list.contains(community))
                    if(community != null && !community.equals(""))
                        community_list.add(community);                
                node_community_version_oid.add(community_list); 
                node_community_version_oid.add(item[2]); 
                node_community_version_oid.add(item[3]);
                node_community_version_oid_list.add(node_community_version_oid);
            }
        }

        for (Map.Entry<String, String> entry : oids.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            Map<String, ArrayList> res1 = walkPool.Get(node_community_version, val);
            Map tmp = new HashMap(res1);
            result.put(key, tmp);
        }
        
        Map<String, Map<String,String>> tmp_map = new HashMap<>();
        int snmp_port = 161;
        Map<String, ArrayList> res_iface_maping = walkPool.GetNodeMultiCommunityVersionOid(node_community_version_oid_list, snmp_port, Neb.timeout_mac, Neb.retries_mac);

        if(res_iface_maping != null && res_iface_maping.size() > 0) {
            result.put("IfaceMaping", res_iface_maping);
        }
        
        return result;
    }
    
//    private ArrayList<String> GetMACAddressFromNode(Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList> commonInformationFromNodes, String node) {
//        ArrayList<String> result = new ArrayList();
//        
//        ArrayList<String[]> list = walkInformationFromNodes.get("ifMAC").get(node);
//        if(list != null) {
//            if(commonInformationFromNodes.get(node) != null) result.add((String)commonInformationFromNodes.get(node).get(6));
//            ArrayList<String[]> list1 = walkInformationFromNodes.get("ifMAC").get(node);
//            if(list1 != null) {
//                for (String[] item : list1) {
//                    result.add(ReplaceDelimiter(TranslateMAC(item[1])));
//                }
//            } 
//        } 
//        
//        return result;
//    }
//    
//    private Map<String, ArrayList<String>> GetNativeMACAddress(Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList> commonInformationFromNodes) {
//        Map<String, ArrayList<String>> result = new HashMap();
//        
//        for(Map.Entry<String, ArrayList> entry : walkInformationFromNodes.get("ifIpAddress").entrySet()) {
//            String node = entry.getKey();
//            ArrayList<String> tmp_list = GetMACAddressFromNode(walkInformationFromNodes, commonInformationFromNodes, node);
//            result.put(node, tmp_list);
//        }
//        
//        return result;        
//    }
    
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
                            else logger.Println("node="+node+" ip="+ip+" is not up.", logger.DEBUG);
//                            break;
                        }
                    }
                }
            }
        } else result.add(node);
        
        ru.kos.neb.neb_lib.Utils lib_utils = new ru.kos.neb.neb_lib.Utils();
        ArrayList<String> result_sort = new ArrayList();
        for(String ip : result) {
            boolean find=false;
            for(String network : Neb.networks) {
                if(lib_utils.InsideInterval(ip, network)) {
                    result_sort.add(ip);
                    find=true; 
                    break;
                }
            }
            if(find) break;
        }
        if(result_sort.size() == 1) {
            for(String ip : result) {
                if(!ip.equals(result_sort.get(0))) result_sort.add(ip);
            }
        } else result_sort.addAll(result);
        
        return result_sort;
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
    
    public Map<String, ArrayList<String>> GetIpAddress(Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, String> info_nodes) {
        Map<String, ArrayList<String>> result = new HashMap();
        
        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
            String node = entry.getKey();
            ArrayList<String> tmp_list = GetIpAddressFromNode(walkInformationFromNodes, node);
            result.put(node, tmp_list);
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

    private String ReplaceDelimiter(String str) {
        if(str != null) {
            str=str.replaceAll("\n", " ").replaceAll("\r", "");
            str=str.replaceAll("\\;", " ");
            str=str.replaceAll("\\|", " ");
            str=str.replaceAll("\\,", " ");
        }
   
        return str;
    }
   
//    private String TranslateIP(String ip)
//    {
//        String out = null;
//
//        if(ip != null) {
//            if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) out=ip;
//            else
//            {
//                if(ip.length() == 4)
//                {
//                    String s=String.valueOf((int)ip.charAt(0));
//                    out=s;
//                    s=String.valueOf((int)ip.charAt(1));
//                    out=out+"."+s;
//                    s=String.valueOf((int)ip.charAt(2));
//                    out=out+"."+s;
//                    s=String.valueOf((int)ip.charAt(3));
//                    out=out+"."+s;
//                }
//                else
//                {
//                    String[] buf = ip.split(":");
//                    if(buf.length == 4)
//                    {
//                        String s = String.valueOf(Integer.parseInt(buf[0],16));
//                        out=s;
//                        s = String.valueOf(Integer.parseInt(buf[1],16));
//                        out=out+"."+s;
//                        s = String.valueOf(Integer.parseInt(buf[2],16));
//                        out=out+"."+s;
//                        s = String.valueOf(Integer.parseInt(buf[3],16));
//                        out=out+"."+s;
//                    }
//                    else
//                    {
//                        buf = ip.split(" ");
//                        if(buf.length == 4)
//                        {
//                            String s = String.valueOf(Integer.parseInt(buf[0],16));
//                            out=s;
//                            s = String.valueOf(Integer.parseInt(buf[1],16));
//                            out=out+"."+s;
//                            s = String.valueOf(Integer.parseInt(buf[2],16));
//                            out=out+"."+s;
//                            s = String.valueOf(Integer.parseInt(buf[3],16));
//                            out=out+"."+s;
//                        }
//    //                    else System.out.println("Error - "+ip);
//                    }
//                }
//            }
//        }
//
//        return out;
//    }
//    
//    private String TranslateMAC(String mac)
//    {
//        String out = "";
//
//        if(mac != null) {
//            if(mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) out=mac;
//            else
//            {
//                if(mac.length() == 6)
//                {
//                    byte[] bb = mac.getBytes();
//                    out=String.format("%02x", bb[0]);
//                    for(int i=1; i<bb.length; i++) {
//                        out = out+":"+String.format("%02x", bb[i]);
//                    }
//                }
//            }
//        }
//        return out.toLowerCase().replace("-", ":").replace(" ", ":");
//    }
    
    // Output format: mac ---> ip
    private Map<String, String> GetARP(ArrayList<String[]> node_community_version) {
        String ArpTable = "1.3.6.1.2.1.3.1.1.2";
        String ArpTable1 = "1.3.6.1.2.1.4.22.1.2";
        
        Map<String, String> result = new HashMap<>(); 
        
//        ru.kos.neb.neb_lib.Utils.DEBUG = true;

        ArrayList<String[]> mac_ip_node = new ArrayList();
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
                            String mac=MAC_Char_To_HEX(item[1]);
                            if(mac != null) {
                                String[] mas = new String[3];
                                mas[0] = mac; mas[1] = ip; mas[2] = node;
                                mac_ip_node.add(mas);
//                                logger.Println("ARP: "+mac+","+ip, logger.DEBUG);
                            }
                        }
                    } else if(buf.length == 15) {
                        String ip = buf[11]+"."+buf[12]+"."+buf[13]+"."+buf[14];
                        if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            String mac=MAC_Char_To_HEX(item[1]);
                            if(mac != null) { 
                                String[] mas = new String[3];
                                mas[0] = mac; mas[1] = ip; mas[2] = node;
                                mac_ip_node.add(mas);
//                                logger.Println("ARP: "+mac+","+ip, logger.DEBUG);
                            }
                        }
                    }
                }
            }
        }
        
        result = ProcessingARP(mac_ip_node);

//        ru.kos.neb.neb_lib.Utils.DEBUG = false;
        return result;
    }
    
    public Map<String, String> ProcessingARP(ArrayList<String[]> mac_ip_node) {
        Map<String, String> result = new HashMap();
        
        Map<String, ArrayList<String[]>> node_macip = new HashMap();
        for(int i=0; i<mac_ip_node.size(); i++) {
            String[] iter1 = mac_ip_node.get(i);
            String mac = iter1[0];
            String ip = iter1[1];
            String node = iter1[2];
            if(node_macip.get(node) == null) {
                ArrayList<String[]> tmp_list = new ArrayList();
                String[] mas = new String[2];
                mas[0] = mac; mas[1] = ip;
                tmp_list.add(mas);
                node_macip.put(node, tmp_list);
            } else {
                String[] mas = new String[2];
                mas[0] = mac; mas[1] = ip;                
                node_macip.get(node).add(mas);
            }
        }    
        
        Map<String, Map<String, String[]>> node_mac_ipscore = new HashMap();
        for (Map.Entry<String, ArrayList<String[]>> entry : node_macip.entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            Map<String, Integer> mac_score = new HashMap();
            for(String[] macip : val) {
                String mac = macip[0];
                if(mac_score.get(mac) == null) {
                    mac_score.put(mac, 1);
                } else {
                    int score = mac_score.get(mac)+1;
                    mac_score.put(mac, score);
                }
            }
            
            Map<String, String[]> mac_ipscore = new HashMap();
            for(String[] macip : val) {
                String mac = macip[0];
                String ip = macip[1];
                if(mac_score.get(mac) != null) {
                    String[] mas = new String[2];
                    mas[0]=ip; mas[1]=String.valueOf(mac_score.get(mac));
                    mac_ipscore.put(mac, mas);
                }
            }
            node_mac_ipscore.put(node, mac_ipscore);
        }
        
        Map<String, String[]> mac_ipscore = new HashMap();
        for(Map.Entry<String, Map<String, String[]>> entry : node_mac_ipscore.entrySet()) {
            String node = entry.getKey();
            Map<String, String[]> val = entry.getValue();
            for(Map.Entry<String, String[]> entry1 : val.entrySet()) {
                String mac = entry1.getKey();
                String[] val1 = entry1.getValue();
                String ip = val1[0];
                String score = val1[1];
                if(mac_ipscore.get(mac) == null)
                    mac_ipscore.put(mac, val1);
                else if(mac_ipscore.get(mac) != null && Integer.valueOf(score) < Integer.valueOf(mac_ipscore.get(mac)[1]))
                    mac_ipscore.put(mac, val1);
            }
        }
        
        for(Map.Entry<String, String[]> entry : mac_ipscore.entrySet()) {
            String mac = entry.getKey();
            String[] val = entry.getValue();
            result.put(mac, val[0]);
        }
        
        return result;
    }
    
    // Output format: ArrayList(String)
//    private ArrayList GetVlanCommunity(Map<String, Map<String, ArrayList>> walkInformationFromNodes, String node) {
//        ArrayList result = new ArrayList();
//        
//        ArrayList<String[]> res = walkInformationFromNodes.get("VlanCommunity").get(node);
//        if(res != null) {
//            res = SetUniqueList(res, 1);
//            for(String[] item : res) {
//                String[] mas = item[1].split(":");
//                String community = "";
//                if(mas.length > 1) {
//                    for(String item1 : mas) {
//                        String ch = convertHexToString(item1);
//                        community=community+ch;
//                    }
//                } else community=item[1];
//                if(!community.equals("") && !community.startsWith("@")) result.add(community);
//            }
//        }
//        return result;
//    }
    
    // Output format: node ---> id_iface ---> list(mac)
    public Map<String, Map<String, ArrayList>> GetMAC(ArrayList<String[]> node_community_version) {
        int retries = 1;
        ArrayList<String> macTable = new ArrayList();
        macTable.add("1.3.6.1.2.1.17.4.3.1.2");
        macTable.add("1.3.6.1.2.1.17.7.1.2.2.1.2");
        
        String vlan_community = "1.3.6.1.2.1.47.1.2.1.1.4";
        String vtpVlanName = "1.3.6.1.4.1.9.9.46.1.3.1.1.4";
        
        String IfaceMaping = "1.3.6.1.2.1.17.1.4.1.2";
        String ifDescr = "1.3.6.1.2.1.2.2.1.2";

        
//        ru.kos.neb.neb_lib.Utils.DEBUG = true;
        
        Map<String, Map<String, ArrayList>> result = new HashMap<>();

        WalkPool walkPool = new WalkPool();
        
        // select only number iface > 2
        Map<String, ArrayList> res1 = walkPool.Get(node_community_version, ifDescr);
        ArrayList<String[]> list_node_community_version_oid = new ArrayList();
        ArrayList<String[]> node_community_version_cur = new ArrayList();
        for(String[] item : node_community_version) {
            if(res1.get(item[0]) != null) { 
                ArrayList val = res1.get(item[0]);
                if(val.size() > 2) {
                    String[] mas = new String[3];
                    mas[0] = item[0]; mas[1] = item[1]; mas[2] = item[2];
                    node_community_version_cur.add(mas);
                    logger.Println("Select node_community_version: "+mas[0]+","+mas[1]+","+mas[2], logger.DEBUG);                    
                } else
                    logger.Println("Not select node_community_version: "+item[0]+","+item[1]+","+item[2], logger.DEBUG);
            }       
        }
        
        for(int i=0; i<Neb.retries_mac; i++) {
            logger.Println("retries_mac: "+i, logger.DEBUG);
            for(String oid : macTable) {
                Map<String, Boolean> res = walkPool.Test(node_community_version_cur, oid, 161, Neb.timeout_mac, Neb.retries_mac, 2);
    //            Map<String, ArrayList> res = walkPool.Get(node_community_version, oid, 161, Neb.timeout_mac, Neb.retries_mac);
                if(res != null && res.size() > 0) {
                    for (Map.Entry<String, Boolean> entry : res.entrySet()) {
                        String node = entry.getKey();
                        if(entry.getValue()) {
                            for(String[] item1 : node_community_version_cur) {
                                if(node.equals(item1[0])) {                    
                                    String[] mas = new String[4];
                                    mas[0]=item1[0]; mas[1]=item1[1]; mas[2]=item1[2];
                                    mas[3]=oid;
                                    list_node_community_version_oid.add(mas);
                                    logger.Println("list_node_community_version_oid: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3], logger.DEBUG);
                                    break;
                                }
                            }
                        }
                    }
                    ArrayList<String[]> node_community_version_cur_new = new ArrayList();
                    for(String[] item : node_community_version_cur) {
                        boolean find = false;
                        for(String[] item1 : list_node_community_version_oid) {
                            if(item[0].equals(item1[0])) {
                                find = true;
                                break;
                            }
                        }
                        if(!find) {
                            String[] mas = new String[3];
                            mas[0] = item[0]; mas[1] = item[1]; mas[2] = item[2];
                            node_community_version_cur_new.add(mas);
                            logger.Println("node_community_version_cur: "+mas[0]+","+mas[1]+","+mas[2], logger.DEBUG);
                        }
                    }
                    node_community_version_cur = node_community_version_cur_new;
                    if(node_community_version_cur.size() == 0) break;
                } else {
                    node_community_version_cur.clear();
                    break;
                }
            }
            if(node_community_version_cur.size() == 0) break;
        }
        
        ArrayList<ArrayList> node_community_version_oid_list = new ArrayList();
        Map<String, ArrayList> res = walkPool.Get(node_community_version, vlan_community);
        
        ArrayList<String[]> buff = new ArrayList();
        ArrayList<String[]> list_node_community_version_oid_new = new ArrayList();
        for(String[] item1 : list_node_community_version_oid) {
            if(res.get(item1[0]) == null) {
                String[] mas = new String[4];
                mas[0]=item1[0];
                mas[1]=item1[1];
                mas[2]=item1[2];
                mas[3]=item1[3];
                buff.add(mas);
            } else {
                list_node_community_version_oid_new.add(item1);
            }
        }
        
        if(res != null && res.size() > 0) {
            for(String[] item : list_node_community_version_oid_new) {
                boolean find=false;
                for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
                    String node = entry.getKey();
                    ArrayList<String[]> val = entry.getValue();
                    if(item[0].equals(node)) {
                        ArrayList node_community_version_oid = new ArrayList();
                        node_community_version_oid.add(item[0]);
                        ArrayList community_list = new ArrayList();
                        for(String[] item1 : val) {
                            String community = TranslateHexString_to_SymbolString(item1[1]).replaceAll("NUL", "@");
                            if(!community_list.contains(community))
                                if(community != null && !community.equals("") && community.matches(".+@\\d+"))
                                    community_list.add(community);
                        }
                        if(community_list.size() == 0) {
                            community_list.add(item[1]);
                        }
                        
                        node_community_version_oid.add(community_list);
                        node_community_version_oid.add(item[2]);
                        node_community_version_oid.add(item[3]);
                        node_community_version_oid_list.add(node_community_version_oid);
                        find=true;
                        break;
                    }
                }
                if(!find) {
                    ArrayList node_community_version_oid = new ArrayList();
                    node_community_version_oid.add(item[0]);
                    ArrayList community_list = new ArrayList();
                    String community = TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                    if(!community_list.contains(community))
                        if(community != null && !community.equals("")) 
                            community_list.add(community);
                    node_community_version_oid.add(community_list); 
                    node_community_version_oid.add(item[2]); 
                    node_community_version_oid.add(item[3]);
                    node_community_version_oid_list.add(node_community_version_oid);                  
                }
            }
        }
        ///////////////////////////////

        if(buff.size() != 0) {
            res = walkPool.Get(buff, vtpVlanName);
            if(res != null && res.size() > 0) {
                for(String[] item : buff) {
                    boolean find=false;
                    for (Map.Entry<String, ArrayList> entry : res.entrySet()) {
                        String node = entry.getKey();
                        ArrayList<String[]> val = entry.getValue();
                        if(item[0].equals(node)) {
                            ArrayList node_community_version_oid = new ArrayList();
                            node_community_version_oid.add(item[0]);
                            ArrayList community_list = new ArrayList();
                            for(String[] item1 : val) {
                                String[] tmp = item1[0].split("\\.");
                                String id_vlan = tmp[tmp.length - 1];

                                community_list.add(item[1]+"@"+id_vlan);
                            }
                            node_community_version_oid.add(community_list);
                            node_community_version_oid.add(item[2]);
                            node_community_version_oid.add(item[3]);
                            node_community_version_oid_list.add(node_community_version_oid);
                            find=true;
                            break;
                        }
                    }
                    if(!find) {
                        ArrayList node_community_version_oid = new ArrayList();
                        node_community_version_oid.add(item[0]);
                        ArrayList community_list = new ArrayList();
                        String community = TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                        if(!community_list.contains(community))
                            if(community != null && !community.equals("")) 
                                community_list.add(community);
 
                        node_community_version_oid.add(community_list); 
                        node_community_version_oid.add(item[2]); 
                        node_community_version_oid.add(item[3]);
                        node_community_version_oid_list.add(node_community_version_oid);                  
                    }
                }                
            }

        }
        /////////////////
        ArrayList<String[]> buff1 = new ArrayList();
        for(String[] item1 : buff) {
            if(res.get(item1[0]) == null) {
                String[] mas = new String[4];
                mas[0]=item1[0];
                mas[1]=item1[1];
                mas[2]=item1[2];
                mas[3]=item1[3];
                buff1.add(mas);
            }
        }        
        
        if(buff1.size() != 0) {
            for(String[] item : buff1) {
                ArrayList node_community_version_oid = new ArrayList();
                node_community_version_oid.add(item[0]);
                ArrayList community_list = new ArrayList();
//                community_list.add(TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@"));
                String community = TranslateHexString_to_SymbolString(item[1]).replaceAll("NUL", "@");
                if(!community_list.contains(community))
                    if(community != null && !community.equals("")) 
                        community_list.add(community);    

                node_community_version_oid.add(community_list); 
                node_community_version_oid.add(item[2]); 
                node_community_version_oid.add(item[3]);
                node_community_version_oid_list.add(node_community_version_oid);
            } 
        }
        
        ////////////////////////////////////////////////////
        // remove not unicalc for node records
        for(int i=0; i<node_community_version_oid_list.size(); i++) {
            ArrayList<String> item1 = node_community_version_oid_list.get(i);
            for(int j=i+1; j<node_community_version_oid_list.size(); j++) {
                ArrayList<String> item2 = node_community_version_oid_list.get(j);
                if(item1.get(0).equals(item2.get(0))) {
                    logger.Println("Remove duplicate mac samle: "+item2.get(0), logger.DEBUG);
                    node_community_version_oid_list.remove(j);
                    j=j-1;
                }
            }
        }       
        //////////////////////////////////////////////////////
        int snmp_port = 161;
        // fast mac address scaning. SNMP BULK
        int num_mac_records=0;
        res = walkPool.GetNodeMultiCommunityVersionOid(node_community_version_oid_list, snmp_port, Neb.timeout_mac, Neb.retries_mac);
    
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
                        if(!find) { 
                            result.get(node).get(id_iface).add(mac); 
                            logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                            num_mac_records++; 
                        }
                    } else if(result.size() > 0 && result.get(node) != null && result.get(node).get(id_iface) == null) {
                        ArrayList tmp_list = new ArrayList();
                        tmp_list.add(mac);
                        logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                        result.get(node).put(id_iface, tmp_list);
                        num_mac_records++;
                    } else {
                        ArrayList tmp_list = new ArrayList();
                        tmp_list.add(mac);
                        logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                        Map<String, ArrayList> tmp_map = new HashMap<>();
                        tmp_map.put(id_iface, tmp_list);
                        result.put(node, tmp_map);
                        num_mac_records++;
                    }
                }
            }
        }
        logger.Println("fast mac addres scanning. num_mac_records="+num_mac_records, logger.DEBUG);
//            System.out.println("retries="+i+" - "+"num_mac_records="+num_mac_records);

        // check nodes not return mac address
        ArrayList<ArrayList> node_community_version_oid_list1 = new ArrayList();
        for(String[] item : WalkPool.error_nodes) {
            ArrayList list_tmp = new ArrayList();
            list_tmp.add(item[0]); 
            ArrayList<String> list_tmp1 = new ArrayList();
            list_tmp1.add(item[1]);
            list_tmp.add(list_tmp1); 
            list_tmp.add(item[2]);
            list_tmp.add(item[3]);
            logger.Println("Error mac fast scanning from node: "+item[0]+", "+item[1]+", "+item[2]+", "+item[3], logger.DEBUG);
            node_community_version_oid_list1.add(list_tmp);
        }
        
        // check nodes not return mac address
        ArrayList<ArrayList> new_node_community_version_oid_list = new ArrayList();
        for(ArrayList node_community_version_oid : node_community_version_oid_list) {
            String node = (String)node_community_version_oid.get(0);
            if(result.get(node) == null) {
                ArrayList node_community_version_oid_new = new ArrayList();
                node_community_version_oid_new.add(node_community_version_oid.get(0));
                node_community_version_oid_new.add(node_community_version_oid.get(1)); 
                node_community_version_oid_new.add(node_community_version_oid.get(2)); 
                node_community_version_oid_new.add(node_community_version_oid.get(3));
                new_node_community_version_oid_list.add(node_community_version_oid_new);                    

                logger.Println("From nodes - "+node+" not receive mac address list from fast scanning!!!", logger.DEBUG);
            }
        }

        for(ArrayList node_community_version_oid : new_node_community_version_oid_list) {
            boolean found = false;
            for(ArrayList node_community_version_oid1 : node_community_version_oid_list1) {
                if(node_community_version_oid.get(0).equals(node_community_version_oid1.get(0))) {
                    found = true;
                    break;
                }
            }
            if(!found)
                node_community_version_oid_list1.add(node_community_version_oid);
        }        
       
        for(ArrayList node_community_version_oid : node_community_version_oid_list1) {
            logger.Println("Adding node for carefully scaning - "+node_community_version_oid.get(0), logger.DEBUG);
        }

        // carefully mac address scanning
        for(int i=0; i<Neb.retries_mac; i++) {
            if(node_community_version_oid_list1.size() > 0) {
                Waiting(Neb.pause_fast_and_carefully_mac_scanning);
                num_mac_records=0;
                res = walkPool.GetNodeMultiCommunityVersionOidNotBulk(node_community_version_oid_list1, snmp_port, Neb.timeout_mac, Neb.retries_mac);
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
                                if(!find) { 
                                    result.get(node).get(id_iface).add(mac); 
                                    logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                                    num_mac_records++; 
                                }
                            } else if(result.size() > 0 && result.get(node) != null && result.get(node).get(id_iface) == null) {
                                ArrayList tmp_list = new ArrayList();
                                tmp_list.add(mac);
                                logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                                result.get(node).put(id_iface, tmp_list);
                                num_mac_records++;
                            } else {
                                ArrayList tmp_list = new ArrayList();
                                tmp_list.add(mac);
                                logger.Println("MAC: "+node+","+id_iface+","+mac, logger.DEBUG);
                                Map<String, ArrayList> tmp_map = new HashMap<>();
                                tmp_map.put(id_iface, tmp_list);
                                result.put(node, tmp_map);
                                num_mac_records++;
                            }
                        }
                    }
                }
                logger.Println("retries="+i+" - "+"num_mac_records="+num_mac_records, logger.DEBUG);
//            System.out.println("retries="+i+" - "+"num_mac_records="+num_mac_records);


                // check nodes not return mac address
                new_node_community_version_oid_list = new ArrayList();
                for(ArrayList node_community_version_oid : node_community_version_oid_list1) {
                    String node = (String)node_community_version_oid.get(0);
                    if(result.get(node) == null) {
                        ArrayList node_community_version_oid_new = new ArrayList();
                        node_community_version_oid_new.add(node_community_version_oid.get(0));
                        node_community_version_oid_new.add(node_community_version_oid.get(1)); 
                        node_community_version_oid_new.add(node_community_version_oid.get(2)); 
                        node_community_version_oid_new.add(node_community_version_oid.get(3));
                        new_node_community_version_oid_list.add(node_community_version_oid_new);                    

                        logger.Println("From nodes - "+node+" not receive mac address list from carefully scanning!!!", logger.DEBUG);
                    }
                }
//                node_community_version_oid_list1 = new_node_community_version_oid_list;

                // check nodes not return mac address
                node_community_version_oid_list1 = new ArrayList();
                for(String[] item : WalkPool.error_nodes) {
                    ArrayList list_tmp = new ArrayList();
                    list_tmp.add(item[0]); 
                    ArrayList<String> list_tmp1 = new ArrayList();
                    list_tmp1.add(item[1]);
                    list_tmp.add(list_tmp1);                      
                    list_tmp.add(item[2]);
                    list_tmp.add(item[3]);
                    logger.Println("Error mac carefully scanning from node: "+item[0]+", "+item[1]+", "+item[2]+", "+item[3], logger.DEBUG);
                    node_community_version_oid_list1.add(list_tmp);
                } 
                
                for(ArrayList node_community_version_oid : new_node_community_version_oid_list) {
                    boolean found = false;
                    for(ArrayList node_community_version_oid1 : node_community_version_oid_list1) {
                        if(node_community_version_oid.get(0).equals(node_community_version_oid1.get(0))) {
                            found = true;
                            break;
                        }
                    }
                    if(!found)
                        node_community_version_oid_list1.add(node_community_version_oid);
                }
                
                for(ArrayList node_community_version_oid : node_community_version_oid_list1) {
                    logger.Println("Adding node for carefully scaning - "+node_community_version_oid.get(0), logger.DEBUG);
                }                
                
            }
        }
        /////////////////////////////////////////////////
        //////////////////////////////////////////////////
        
        // output: node ---> id ---> id_translate
        ArrayList<ArrayList> node_community_version_oid_new_list = new ArrayList();
        for(ArrayList node_community_version_oid : node_community_version_oid_list) {
           ArrayList node_community_version_oid_new = new ArrayList();
           node_community_version_oid_new.add(node_community_version_oid.get(0));
           node_community_version_oid_new.add(node_community_version_oid.get(1)); 
           node_community_version_oid_new.add(node_community_version_oid.get(2)); 
           node_community_version_oid_new.add(IfaceMaping);
           node_community_version_oid_new_list.add(node_community_version_oid_new);
        }
        
        Map<String, Map<String,String>> tmp_map = new HashMap<>();
        Map<String, ArrayList> res_iface_maping = walkPool.GetNodeMultiCommunityVersionOid(node_community_version_oid_new_list, snmp_port, Neb.timeout_mac, Neb.retries_mac);

        if(res_iface_maping != null && res_iface_maping.size() > 0) {
            for (Map.Entry<String, ArrayList> entry : res_iface_maping.entrySet()) {
                String node = entry.getKey();
                ArrayList<String[]> val_list = entry.getValue();
                Map<String, String> tmp_map1 = new HashMap<>();
                for(String[] item : val_list) {
                    String id=item[0].split("\\.")[item[0].split("\\.").length - 1];
                    if(tmp_map1.get(id) == null) tmp_map1.put(id, item[1]);
                }
                tmp_map.put(node, tmp_map1);
            }
        } else return result;
        
        // translate in result id_iface to id_iface_translate
        Map<String, Map<String, ArrayList>> out = new HashMap<>();
        for (Map.Entry<String, Map<String, ArrayList>> entry : result.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_list = entry.getValue();
            Map<String, ArrayList> map = new HashMap<>();
            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                String iface_id = entry1.getKey();
                ArrayList<String> val_list1 = entry1.getValue();
                if(tmp_map.get(node) != null && tmp_map.get(node).get(iface_id) != null) {
                    map.put(tmp_map.get(node).get(iface_id), val_list1);
                } else {
                    map.put(iface_id, val_list1);
                }
            }
            out.put(node, map);
        }
        
//        ru.kos.neb.neb_lib.Utils.DEBUG = false;
        
        return out;
    }
    
    private String DecToHex(int dec){
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};

        String hex=String.valueOf(hexDigits[dec/16])+String.valueOf(hexDigits[dec%16]);

        return hex;
    }

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
   
    // Output format: list(node, iface_id, iface_name, ip, mac 
    public ArrayList GetARPMAC(Map<String, Map<String, ArrayList>> MAC, Map<String, String> ARP, ArrayList<String[]> node_community_version) {
        ArrayList<String[]> result  = new ArrayList();

        Map<String, Map<String, ArrayList>> walkInterfacesFromNodes = GetWalkInterfacesFromNodes(node_community_version);
        
        // Output format: node ---> iface_id ---> list(iface_name, list(ip, mac))
        Map<String, Map<String, ArrayList>> node_iface_id_iface_name_ip_mac  = new HashMap<>();
        if(MAC.size() > 0 && ARP.size() > 0) {
            for (Map.Entry<String, Map<String, ArrayList>> entry : MAC.entrySet()) {
                String node = entry.getKey();
                Map<String, ArrayList> val_list = entry.getValue();
                Map<String, ArrayList> map = new HashMap<>();
                for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                    String iface_id = entry1.getKey();
                    ArrayList<String> val_list1 = entry1.getValue();
                    ArrayList<String[]> list1 = new ArrayList();
                    for(String mac : val_list1) {
//                        System.out.println(node+","+iface_id+","+mac);
                        String[] ip_mac = new String[2];
                        String ip = ARP.get(mac);
                        if(ip != null) {
                            ip_mac[0]=ip;
                            ip_mac[1]=mac;
//                            System.out.println(node+","+iface_id+","+ip+","+mac);
                        } else {
                            ip_mac[0]="unknown_ip";
                            ip_mac[1]=mac;                            
//                            System.out.println(node+","+iface_id+","+"unknown_ip"+","+mac);
                        }
                        list1.add(ip_mac);
                    }
                    
                    String[] res = GetIfaceName(node, iface_id, walkInterfacesFromNodes, false);
                    String iface_name = res[1];
                    iface_id=res[0];

                    String iface_type = "";
                    if(walkInterfacesFromNodes.get("ifType").get(node) != null) {
                        ArrayList<String[]> tmp_list = walkInterfacesFromNodes.get("ifType").get(node);
                        for(String[] item1 : tmp_list) {
                            if(item1[0].split("\\.")[item1[0].split("\\.").length - 1].equals(iface_id)) {
                                if (!item1[1].equals("24") && Integer.parseInt(item1[1]) <= 32) {
                                    iface_type=item1[1];
                                }
                                break;
                            }
                        }
                    }
                    
                    
                    if(!iface_name.equals("") && !iface_type.equals("")) {
                        ArrayList iface_name_list_ip_mac = new ArrayList();
                        iface_name_list_ip_mac.add(iface_name);
                        iface_name_list_ip_mac.add(list1);
                        map.put(iface_id, iface_name_list_ip_mac);
                    }
                }
                node_iface_id_iface_name_ip_mac.put(node, map);
            }
        }
        /////////////////////////////////////////////////////////////////////////

        // Output format: ip ---> list(node, iface_id, iface_name, mac)
        Map<String, String[]> ip_node_iface_id_iface_name_mac  = new HashMap<>();
        for (Map.Entry<String, Map<String, ArrayList>> entry : node_iface_id_iface_name_ip_mac.entrySet()) {
            String node = entry.getKey();
            Map<String, ArrayList> val_list = entry.getValue();
            for (Map.Entry<String, ArrayList> entry1 : val_list.entrySet()) {
                String iface_id = entry1.getKey();
                ArrayList val_list1 = entry1.getValue();
                String iface_name = (String)val_list1.get(0);
                ArrayList<String[]> ip_mac = (ArrayList)val_list1.get(1);
                
                for(String[] item : ip_mac) {
                    String ip=item[0];
                    String mac=item[1];
                    String[] mas = new String[5];
                    mas[0]=node; 
                    mas[1]=iface_id;
                    mas[2]=iface_name;
                    mas[3]=ip;
                    mas[4]=mac;
                    result.add(mas);
//                    outFile.write("ARP_MAC: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+"\n");
//                    System.out.println("ARP_MAC: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]);
                }
            }
            
        }
        
        return result;
    }
    
    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2, in1, ou1, in2, out2, time))
    public ArrayList<String[]> GetCountersTestingLinks(ArrayList<String[]> links, ArrayList<String[]> node_community_version) {
        String ifHCInOctets = "1.3.6.1.2.1.31.1.1.1.6";
        String ifHCOutOctets = "1.3.6.1.2.1.31.1.1.1.10"; 
        String ifInOctets = "1.3.6.1.2.1.2.2.1.10";
        String ifOutOctets = "1.3.6.1.2.1.2.2.1.16";
        ArrayList<String[]> result = new ArrayList();
        
        ArrayList<String[]> list_node_community_ver_oid = new ArrayList();
        for(String[] item : links) {
            for(String[] item1 : node_community_version) {
                if(item[0].equals(item1[0])) {
                    String[] mas = new String[4];
                    mas[0]=item[0];
                    mas[1]=item1[1];
                    mas[2]=item1[2];
                    if(item1[2].equals("2")) mas[3]=ifHCInOctets+"."+item[1]; else mas[3]=ifInOctets+"."+item[1];
                    list_node_community_ver_oid.add(mas);
                    String[] mas1 = new String[4];
                    mas1[0]=item[0];
                    mas1[1]=item1[1];
                    mas1[2]=item1[2];
                    if(item1[2].equals("2")) mas1[3]=ifHCOutOctets+"."+item[1]; else mas1[3]=ifOutOctets+"."+item[1];
                    list_node_community_ver_oid.add(mas1);
                    break;
                }
            }
            for(String[] item1 : node_community_version) {
                if(item[3].equals(item1[0])) {
                    String[] mas = new String[4];
                    mas[0]=item[3];
                    mas[1]=item1[1];
                    mas[2]=item1[2];
                    if(item1[2].equals("2")) mas[3]=ifHCInOctets+"."+item[4]; else mas[3]=ifInOctets+"."+item[4];
                    list_node_community_ver_oid.add(mas);
                    String[] mas1 = new String[4];
                    mas1[0]=item[3];
                    mas1[1]=item1[1];
                    mas1[2]=item1[2];                    
                    if(item1[2].equals("2")) mas1[3]=ifHCOutOctets+"."+item[4]; else mas1[3]=ifOutOctets+"."+item[4];
                    list_node_community_ver_oid.add(mas1);
                    break;
                }
            }
            
        }
    
        GetList getList = new GetList();
        long start_time = System.currentTimeMillis();
        Map<String, ArrayList<String[]>> res = getList.Get(list_node_community_ver_oid);
        long stop_time = System.currentTimeMillis();
        
        for(String[] item : links) {
            String in1 = null;
            String out1 = null;
            String in2 = null;
            String out2 = null;       
            
            ArrayList<String[]> list = res.get(item[0]);
            if(list != null) {
                for(String[] item1 : list) {
                    if(item1[0].equals(ifHCInOctets+"."+item[1])) {
                        in1=item1[1];
                        break;
                    }
                }
                if(in1 == null) {
                    for(String[] item1 : list) {
                        if(item1[0].equals(ifInOctets+"."+item[1])) {
                            in1=item1[1];
                            break;
                        }
                    }                    
                }
                for(String[] item1 : list) {
                    if(item1[0].equals(ifHCOutOctets+"."+item[1])) {
                        out1=item1[1];
                        break;
                    }
                }
                if(out1 == null) {
                    for(String[] item1 : list) {
                        if(item1[0].equals(ifOutOctets+"."+item[1])) {
                            out1=item1[1];
                            break;
                        }
                    }                    
                }
            }

            list = res.get(item[3]);
            if(list != null) {
                for(String[] item1 : list) {
                    if(item1[0].equals(ifHCInOctets+"."+item[4])) {
                        in2=item1[1];
                    }
                }
                if(in2 == null) {
                    for(String[] item1 : list) {
                        if(item1[0].equals(ifInOctets+"."+item[4])) {
                            in2=item1[1];
                        }
                    }                    
                }
                for(String[] item1 : list) {
                    if(item1[0].equals(ifHCOutOctets+"."+item[4])) {
                        out2=item1[1];
                    }
                }
                if(out2 == null) {
                    for(String[] item1 : list) {
                        if(item1[0].equals(ifOutOctets+"."+item[4])) {
                            out2=item1[1];
                        }
                    }                    
                }
            }
            
            if(in1 != null && in2 != null && out1 != null && out2 != null) {
                String[] mas = new String[11];
                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
                mas[6]=in1; mas[7]=out1; mas[8]=in2; mas[9]=out2;
                mas[10]=String.valueOf(stop_time-start_time);      
//                System.out.println("GetcounterTestLink: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]+","+mas[7]+","+mas[8]+","+mas[9]+","+mas[10]);
                result.add(mas);
            }
        }
        
        return result;
    }
    
    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2))
    public ArrayList<String[]> CalculateTestingLinks(ArrayList<String[]> testingLinks_start, ArrayList<String[]> testingLinks_stop, double precession_limit, ArrayList<String[]>node_community_version) {
        ArrayList<String[]> result = new ArrayList();
        
        for(String[] item : testingLinks_start) {
            for(String[] item1 : testingLinks_stop) {
                if(item[0].equals(item1[0]) && 
                        item[1].equals(item1[1]) &&
                        item[2].equals(item1[2]) &&
                        item[3].equals(item1[3]) &&
                        item[4].equals(item1[4]) &&
                        item[5].equals(item1[5]) &&
                        !item[6].equals("") && !item1[6].equals("") &&
                        !item[7].equals("") && !item1[7].equals("") &&
                        !item[8].equals("") && !item1[8].equals("") &&
                        !item[9].equals("") && !item1[9].equals("")
                        ) {
                    double delta_in1 = Delta(item[0], Double.parseDouble(item[6]), Double.parseDouble(item1[6]), node_community_version);
                    double delta_out1 = Delta(item[0], Double.parseDouble(item[7]), Double.parseDouble(item1[7]), node_community_version);
                    double delta_in2 = Delta(item[3], Double.parseDouble(item[8]), Double.parseDouble(item1[8]), node_community_version);
                    double delta_out2 = Delta(item[3], Double.parseDouble(item[9]), Double.parseDouble(item1[9]), node_community_version);

                    if(delta_in1 >= 0 && delta_out1 >= 0 && delta_in2 >= 0 && delta_out2 >= 0) {
                        double epsilon1 = 2*abs(delta_in1-delta_out2)/(delta_in1+delta_out2);
                        double epsilon2 = 2*abs(delta_in2-delta_out1)/(delta_in2+delta_out1);
                        double epsilon = (epsilon1+epsilon2)/2;
                        float delta_time=(float)(Long.valueOf(item1[10])+Long.valueOf(item[10]))/1000;
//                        float precession_limit=(float)((delta_time+time_lag)/wait_time);
                        if( epsilon < 2*precession_limit ) {
                            String[] mas = new String[8];
                            mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2]; 
                            mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
                            mas[6]=String.valueOf(epsilon);                            
                            mas[7]=String.valueOf(2*precession_limit);
                            logger.Println("CalculateTestingLink: "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]+","+mas[6]+","+mas[7], logger.DEBUG);
                            result.add(mas);
                        } else {
                            logger.Println("CalculateTestingLink exclude: "+item[0]+","+item[1]+","+item[2]+","+item[3]+","+item[4]+","+item[5]+","+String.valueOf(epsilon)+","+String.valueOf(precession_limit), logger.DEBUG);
                        }
                    }
                    break;
                }
            }
        }
        
        return result;
    }
    
    public void Waiting(long wait_timeout) {
        long start_time = System.currentTimeMillis();
        String backspace="";
        if(wait_timeout > 0) {
            while (true)
            {
                long stop_time = System.currentTimeMillis();
                System.out.print(backspace);
                String out = "Elapsed time : " + String.valueOf((stop_time - start_time) / 1000) + " sec.\t\tFrom : " + wait_timeout + " sec.";
                System.out.print(out);
                for(int i=0; i<out.length(); i++) backspace=backspace+"\b";
                if ((stop_time - start_time) / 1000 > wait_timeout)
                {
                    System.out.println("");
                    break;
                }
                try { Thread.sleep(10000); } catch (InterruptedException e) { }
            }
        }
        
    }
    
    //  output: ArrayList(String[](node1,id1,iface1,node2,id2,iface2))
    public ArrayList<String[]> SummaryLinks(ArrayList<String[]> links, ArrayList<String[]> dplinks, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        ArrayList<String[]> result = new ArrayList();

        for(String[] item : dplinks) {
            String[] mas = new String[7];
            mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
            mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
            mas[6]="dp_link";
            result.add(mas);
        }
        
        Map<String, ArrayList<String>> nodes_ip_address = GetIpAddress(walkInformationFromNodes);
        for(String[] item : links) {
            if(CheckDuplicateLink(item, result, nodes_ip_address)) {
                String[] mas = new String[7];
                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5]; 
                mas[6]="calc_link";
                result.add(mas);
//                System.out.println("Link adding from links: "+item[0]+","+item[1]+","+item[2]+" <---> "+item[3]+","+item[4]+","+item[5]);
            }
        }
        
        for(String[] item : result) {
            logger.Println("Start merging dplinks ... "+item[0]+","+item[1]+","+item[2]+" <---> "+item[3]+","+item[4]+","+item[5]+" --- "+item[6], logger.DEBUG);
        }
     
        return result;
    }
    
    private String TranslateHexString_to_SymbolString(String str) {
        String result=str;
        
        if(str.matches("^([0-9A-Fa-f]{1,2}[:-])+[0-9A-Fa-f]{1,2}$")) {
            String[] fields = str.split("[:-]");
            if(fields.length > 1) {
                String out="";
                for(String octet : fields) {
                    int dec = Integer.parseInt(octet, 16);
                    if(dec == 0) out=out+"NUL";
                    else if(dec == 63 || dec < 32 || dec > 126) {
                        out = "";
                        break;
                    }
                    else out=out+(char)dec;
                }
                result = out;
            }
        }
        return result;
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
    
    // Output format: ArrayList
    //    mac --> ip, node, if_iface, name_iface
    public ArrayList<String[]> CalculateARPMAC(ArrayList<String[]> ARP_MAC, ArrayList<ArrayList<String>> links, Map<String, Map> nodes_information, Map node_protocol_accounts) {
        ArrayList<String[]> result = new ArrayList();
//        ArrayList<String> nodes = new ArrayList();
        ArrayList<String> mac = new ArrayList();
        ArrayList<String> ip = new ArrayList();
        ArrayList<String[]> ARP_MAC_NEW = new ArrayList();
        ARP_MAC_NEW.addAll(ARP_MAC);
        
        ArrayList<String> nodes_unknown = new ArrayList();
        for(Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node = entry.getKey();
            if(node_protocol_accounts.get(node) == null) {
                nodes_unknown.add(node);
            }
            Map<String, Map> val = entry.getValue();
            ArrayList<String> mac_list = GetMACFromNode(val);
            mac.addAll(mac_list);
            ArrayList<String> ip_list = GetIpListFromNode(val);
            ip.addAll(ip_list);
        }

        // get links to unknown nodes
        ArrayList<ArrayList<String>> links_to_unknown_nodes = new ArrayList();
        ArrayList<ArrayList<String>> links_to_known_nodes = new ArrayList();
        for(ArrayList<String> item : links) {
            boolean find = false;
            for(String node : nodes_unknown) {
                if(item.get(3).equals(node)) {
                    find = true;
                    break;
                }
            }
            if(!find)
                links_to_known_nodes.add(item);
            else
                links_to_unknown_nodes.add(item);
        }
//        WriteArrayListToFile1("links_to_known_nodes", links_to_known_nodes);
//        WriteArrayListToFile1("links_to_unknown_nodes", links_to_unknown_nodes);
//////////////////////////////////////////////////////////////////////////////  
        // indexing node_iface
        Map<String, ArrayList<Integer>> node_iface_number = new HashMap();
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            String node_iface = item[0]+" "+item[2];
            if(node_iface_number.get(node_iface) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                node_iface_number.put(node_iface, number_list);
            }
            else {
                node_iface_number.get(node_iface).add(i);
            }
        }        
        
        Map<Integer, Integer> remove_numbers_links = new HashMap();
        for(ArrayList<String> item : links_to_known_nodes) {
            String node_iface = item.get(0)+" "+item.get(2);
            ArrayList<Integer> number_list = node_iface_number.get(node_iface);
            if(number_list != null) {
                for(Integer num : number_list)
                    remove_numbers_links.put(num, num);
            }
            node_iface = item.get(3)+" "+item.get(5);           
            number_list = node_iface_number.get(node_iface);
            if(number_list != null) {
                for(Integer num : number_list)
                    remove_numbers_links.put(num, num);
            }            
        }
        
        // removes ARP_mac records if is am link
        ArrayList<String[]> ARP_MAC_tmp = new ArrayList();
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if(remove_numbers_links.get(i) == null) ARP_MAC_tmp.add(item);
        }        
        
        ARP_MAC_NEW.clear();
        ARP_MAC_NEW.addAll(ARP_MAC_tmp);
        ARP_MAC_tmp.clear();
        
////////////////////////////////////////////////////////////////  
//         indexing node_iface
        node_iface_number = new HashMap();
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            String node_iface = item[0]+" "+item[2];
            if(node_iface_number.get(node_iface) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                node_iface_number.put(node_iface, number_list);
            }
            else {
                node_iface_number.get(node_iface).add(i);
            }
        }        
        
        Map<Integer, ArrayList<String>> update_numbers_links_to_unknown_nodes = new HashMap();
        for(ArrayList<String> item : links_to_unknown_nodes) {
            String node_iface = item.get(0)+" "+item.get(2);
            ArrayList<Integer> number_list = node_iface_number.get(node_iface);
            if(number_list != null) {
                for(Integer num : number_list)
                    update_numbers_links_to_unknown_nodes.put(num, item);
            }
        }
        
        // moves mac ip address to CDP and LLDP nodes.
        for(Map.Entry<Integer, ArrayList<String>> entry : update_numbers_links_to_unknown_nodes.entrySet()) {
            String[] item = ARP_MAC_NEW.get(entry.getKey());
            ArrayList<String> item1 = entry.getValue();
            String[] mas = new String[5];
            mas[0]=item1.get(3); mas[1]="unknown"; mas[2]="unknown";
            mas[3]=item[3]; mas[4]=item[4];
            ARP_MAC_NEW.set(entry.getKey(), mas);
        }
////////////////////////////////////////////////  

        // indexing mac
        Map<String, ArrayList<Integer>> MAC_number = new HashMap();
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if(MAC_number.get(item[4]) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                MAC_number.put(item[4], number_list);
            }
            else {
                MAC_number.get(item[4]).add(i);
            }
        }
        
        Map<Integer, Integer> remove_numbers = new HashMap();
        for(String item : mac) {
            ArrayList<Integer> number_list = MAC_number.get(item);
            if(number_list != null) {
                for(Integer num : number_list)
                    remove_numbers.put(num, num);
            }
        }
        
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if(remove_numbers.get(i) == null) ARP_MAC_tmp.add(item);
        }
        ARP_MAC_NEW.clear();
        ARP_MAC_NEW.addAll(ARP_MAC_tmp);
        ARP_MAC_tmp.clear();
//////////////////////////////////////////

        // indexing ip
        Map<String, ArrayList<Integer>> IP_number = new HashMap();
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if(IP_number.get(item[3]) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                IP_number.put(item[3], number_list);
            }
            else {
                IP_number.get(item[3]).add(i);
            }
        }
        
        remove_numbers = new HashMap();
        for(String item : ip) {
            ArrayList<Integer> number_list = IP_number.get(item);
            if(number_list != null) {
                for(Integer num : number_list)
                    remove_numbers.put(num, num);
            }
        }
        
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            if(remove_numbers.get(i) == null) ARP_MAC_tmp.add(item);
        }
        ARP_MAC_NEW.clear();
        ARP_MAC_NEW.addAll(ARP_MAC_tmp);
        ARP_MAC_tmp.clear();
           
////////////////////////////////////////// 

        // indexing node_iface
        node_iface_number = new HashMap();
        for(int i=0; i<ARP_MAC_NEW.size(); i++) {
            String[] item = ARP_MAC_NEW.get(i);
            String node_iface = item[0]+" "+item[2];
            if(node_iface_number.get(node_iface) == null) {
                ArrayList<Integer> number_list = new ArrayList();
                number_list.add(i);
                node_iface_number.put(node_iface, number_list);
            }
            else {
                node_iface_number.get(node_iface).add(i);
            }
        }        

        ArrayList<String[]> mac_ip_node_ifaceid_ifacename_nummacforport = new ArrayList();
        for(String[] item : ARP_MAC_NEW) {
            String node_iface = item[0]+" "+item[2];
            int num_mac=0;
            if(node_iface_number.get(node_iface) != null) num_mac=node_iface_number.get(node_iface).size();
            String[] mas = new String[6];
            mas[0]=item[4];
            mas[1]=item[3];
            mas[2]=item[0];
            mas[3]=item[1];
            mas[4]=item[2];
            mas[5]=String.valueOf(num_mac);
            mac_ip_node_ifaceid_ifacename_nummacforport.add(mas);
//            System.out.println("+++ "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]);
        }

        // indexing mac
        Map<String, ArrayList<String[]>> MAC_ip_node_ifaceid_ifacename_num = new HashMap();
        for(String[] item : mac_ip_node_ifaceid_ifacename_nummacforport) {
            if(MAC_ip_node_ifaceid_ifacename_num.get(item[0]) == null) {
                ArrayList<String[]> list = new ArrayList();
                list.add(item);
                MAC_ip_node_ifaceid_ifacename_num.put(item[0], list);
            }
            else {
                MAC_ip_node_ifaceid_ifacename_num.get(item[0]).add(item);
            }
        }        
        
        // if mac clients exist in node name - remove from result
        Pattern p1 = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
        Pattern p2 = Pattern.compile("(([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4}))"); 
        ArrayList<String> mackey_delete_list = new ArrayList();
        for (Map.Entry<String, ArrayList<String[]>> entry : MAC_ip_node_ifaceid_ifacename_num.entrySet()) {
            String mac_addr = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            for(String[] item : val) {
                Matcher m_mac1 = p1.matcher(item[2]);
                Matcher m_mac2 = p2.matcher(item[2]);
                String mac_address = null;
                if(m_mac1.find()) { 
                    mac_address=m_mac1.group(1).replaceAll("[:.-]", "").toLowerCase();
                } else if(m_mac2.find()) { 
                    mac_address=m_mac2.group(1).replaceAll("[:.-]", "").toLowerCase();
                }
                String mac_addr_short = mac_addr.replaceAll("[:.-]", "");
                if(mac_addr_short != null && mac_addr_short.equals(mac_address)) {
                    mackey_delete_list.add(mac_addr);
                }
            }
        }
        
        for(String item : mackey_delete_list) {
            MAC_ip_node_ifaceid_ifacename_num.remove(item);
        }
        
        for (Map.Entry<String, ArrayList<String[]>> entry : MAC_ip_node_ifaceid_ifacename_num.entrySet()) {
            String mac_addr = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            if(!mac.equals("unknown_mac")) {
                String[] min_item = new String[6];
                min_item[5]=String.valueOf(2137483647);
                for(String[] item : val) {
                    if(Integer.parseInt(item[5]) < Integer.parseInt(min_item[5])) {
                        min_item[0]=item[0]; min_item[1]=item[1];
                        min_item[2]=item[2]; min_item[3]=item[3];
                        min_item[4]=item[4]; min_item[5]=item[5];
                    }
                }
                if(min_item[0] != null) result.add(min_item);
            } else {
                for(String[] item : val) {
                    result.add(item);
                }
            }
        }        
        
        return result;
    }
    
//    // Output format: ArrayList
//    //    mac --> ip, node, if_iface, name_iface
//    public void CalculateARPMAC() {        
//        ArrayList<String[]> result = new ArrayList();
//        ArrayList<String> nodes = new ArrayList();
//        ArrayList<String> mac = new ArrayList();
//        ArrayList<String> ip = new ArrayList();  
//        
//      
//        ArrayList<String[]> ARP_MAC = new ArrayList();
//        ArrayList<String[]> added_from_exclude_nodes_links = new ArrayList();
//        ArrayList<String[]> links = new ArrayList();
//        Map<String, String> informationFromNodes = new HashMap<>();
//        
//
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("ARP_MAC"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    ARP_MAC.add(s.split(","));
////                    System.out.println(s);
//                }
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("added_from_exclude_nodes_links"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    added_from_exclude_nodes_links.add(s.split(","));
////                    System.out.println(s);
//                }
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("links"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    links.add(s.split(","));
//                }
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("info_nodes1"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    String[] mas=s.split(";", -1);
//                    String str = mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5]+";"+mas[6]+";"+mas[7]+";"+mas[8]+";"+mas[9]+";"+mas[10];
//                    informationFromNodes.put(mas[0], str);
////                    System.out.println(s);
//                }
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//        
//        
///////////////////////////////////////////////////////////////////
//
//        // adding ARP_MAC table from added_from_exclude_nodes_links
//        for(String[] item : added_from_exclude_nodes_links) {
//            boolean find=false;
//            for(String[] item1 : ARP_MAC) {
//                if(item1[3].equals(item[3])) {
//                    find=true;
//                    break;
//                }
//            }
//            if(!find) ARP_MAC.add(item);
//        }
////        ARP_MAC.addAll(added_from_exclude_nodes_links);
//
//        ArrayList<String> nodes_unknown = new ArrayList();
//        for (Map.Entry<String, String> entry : informationFromNodes.entrySet()) {
//            String node = entry.getKey();
//            String val = entry.getValue();
//            
////            System.out.println("CalculateARPMAC: "+node+";"+val);
//            String[] mas=val.split(";", -1);
//            nodes.add(node);
//            if(mas[0].split("\\|").length != 3) {
//                nodes_unknown.add(node);
//            }
//            
//            
//            if(mas.length == 10) {
//                if(!mas[7].equals("")) {
//                    String[] mas1=mas[7].split("\\|");
//                    for(String item : mas1) {
//                        String[] mas2=item.split(",", -1);
//                        if(mas2.length == 11) {
//                            if(!mas2[5].equals("")) {
//                                mac.add(mas2[5]);
//                            }
//                            if(!mas2[6].equals("")) {
//                                String ipaddr=mas2[6].split("/")[0];
//                                if(!ipaddr.equals("127.0.0.1")) ip.add(ipaddr);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // get links to unknown nodes
//        ArrayList<String[]> links_to_unknown_nodes = new ArrayList();
//        for(String[] item : links) {
//            for(String node : nodes_unknown) {
//                if(item[3].equals(node.split(" ")[0])) {
//                    links_to_unknown_nodes.add(item);
//                    break;
//                }
//            }
//        }
//         
//        // moves mac ip address to CDP and LLDP nodes.
//        for(int i=0; i<ARP_MAC.size(); i++) {
//            String[] item = ARP_MAC.get(i);
//            for(String[] item1 : links_to_unknown_nodes) {
//                if(item1[0].equals(item[0]) && item1[2].equals(item[2])) {
//                    String[] mas = new String[5];
//                    mas[0]=item1[3]; mas[1]="unknown"; mas[2]="unknown";
//                    mas[3]=item[3]; mas[4]=item[4];
//                    if(mas[0].equals(mas[3])) {
//                        ARP_MAC.remove(i);
//                    } else {
//                        ARP_MAC.set(i, mas);
//                    }
//                    break;
//                }
//            }
//        }
//        
//        ArrayList<String[]> ARP_MAC_tmp = new ArrayList();
//        for(String[] item : ARP_MAC) {
//            boolean found=false;
//            for(String[] item1 : links) {
//                if( (item[0].equals(item1[0]) && CheckInterfaceName(item[2], item1[2])) || (item[0].equals(item1[3]) && CheckInterfaceName(item[2], item1[5])) ) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_tmp.add(item);
//            }
//        }
//        
//        ArrayList<String[]> ARP_MAC_tmp1 = new ArrayList();
//        for(String[] item : ARP_MAC_tmp) {
//            boolean found=false;
//            for(String item1 : mac) {
//                if( item[4].equals(item1) ) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_tmp1.add(item);
//            }
//        }        
//        
//        ArrayList<String[]> ARP_MAC_tmp2 = new ArrayList();
//        for(String[] item : ARP_MAC_tmp1) {
//            boolean found=false;
//            for(String item1 : ip) {
//                if( item[3].equals(item1) ) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_tmp2.add(item);
//            }
//        } 
//        
//        ArrayList<String[]> ARP_MAC_userports = new ArrayList();
//        for(String[] item : ARP_MAC_tmp2) {
//            boolean found=false;
//            for(String node : nodes) {
//                if( item[3].equals(node)) {
//                    found=true;
//                    break;
//                }
//            }
//            if(!found) {
//                ARP_MAC_userports.add(item);
////                System.out.println("### "+item[0]+","+item[1]+","+item[2]+","+item[3]+","+item[4]);
//            }
//        }
//        
//        
//        
//        ArrayList<String[]> mac_ip_node_ifaceid_ifacename_nummacforport = new ArrayList();
//        for(String[] item : ARP_MAC_userports) {
//            int num_mac=0;
//            for(String[] item1 : ARP_MAC_userports) {
//                if(item[0].equals(item1[0]) && item[2].equals(item1[2]) ) {
//                    num_mac++;
//                }
//            }
//            String[] mas = new String[6];
//            mas[0]=item[4];
//            mas[1]=item[3];
//            mas[2]=item[0];
//            mas[3]=item[1];
//            mas[4]=item[2];
//            mas[5]=String.valueOf(num_mac);
//            mac_ip_node_ifaceid_ifacename_nummacforport.add(mas);
////            System.out.println("+++ "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]+","+mas[5]);
//        }
//
////        ArrayList<String[]> result = new ArrayList();
//        for(int pos=0; pos<mac_ip_node_ifaceid_ifacename_nummacforport.size(); pos++) {
//            String[] item=mac_ip_node_ifaceid_ifacename_nummacforport.get(pos);
//            if(!item[0].equals("unknown_mac")) {
//                String[] item_min = item.clone();
//                int num_min=Integer.parseInt(item[5]);
//                for(int j=pos+1; j<mac_ip_node_ifaceid_ifacename_nummacforport.size(); j++) {
//                    String[] item1=mac_ip_node_ifaceid_ifacename_nummacforport.get(j);
//                    if( item[0].equals(item1[0])) {
//                        if(Integer.parseInt(item1[5]) < num_min) {
//                            item_min = item1.clone();
//                            num_min=Integer.parseInt(item1[5]);
//                        }
//                        mac_ip_node_ifaceid_ifacename_nummacforport.remove(j);
//                        j--;
//                    }
//                }
//                if( !item_min[1].equals(item_min[2]) ) {
////                    System.out.println("--- "+item_min[0]+","+item_min[1]+","+item_min[2]+","+item_min[3]+","+item_min[4]+","+item_min[5]);
//                    result.add(item_min);
//                }
//            } else {
////                System.out.println("--- "+item[0]+","+item[1]+","+item[2]+","+item[3]+","+item[4]+","+item[5]);
//                result.add(item);                
//            }       
//        }
//
//    }
    
    private String[] GetIfaceName(String node, String id_iface, Map<String, Map<String, ArrayList>> walkInformationFromNodes, boolean with_interface_maping) {
        String[] result = new String[2];
        result[0]=""; result[1]="";

        if(with_interface_maping) {
            if(walkInformationFromNodes.get("IfaceMaping").size() > 0 && walkInformationFromNodes.get("IfaceMaping").get(node) != null) {
                for(String[] item0 : (ArrayList<String[]>)walkInformationFromNodes.get("IfaceMaping").get(node)) {
                    if(item0[0].split("\\.")[item0[0].split("\\.").length - 1].equals(id_iface)) {
                        id_iface=item0[1];
                        break;
                    }
                }
            }
        }
        
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
                if(walkInformationFromNodes.get("IfNameExtendedIfName") != null && walkInformationFromNodes.get("IfNameExtendedIfName").get(node) != null) {
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

        return result;
    }
    
//    public ArrayList<String[]> GetNodeCommunityVersion() {
//        ArrayList<String[]> result = new ArrayList();
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("nodes"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    String[] mas=s.split(",");
//                    result.add(mas);
////                    System.out.println(s);
//                }
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
//        
//        return result;
//    }
    
    public LinkedList<ArrayList> SortingMinPathDeltaCounters(LinkedList<ArrayList> deltaCounters) {
        LinkedList<ArrayList> result = new LinkedList();
        
        int pos=0;
        while(deltaCounters.size() > 1) {
            int first_pos=pos;
            ArrayList item = deltaCounters.get(first_pos);
            double min = Math.pow(2, 64);
            for(int i=1; i<deltaCounters.size(); i++) {
                ArrayList item1 = deltaCounters.get(i);
                if(!item.get(0).equals(item1.get(0))) {
                    double d1 = Math.abs(2*((double)item.get(3)-(double)item1.get(4))/((double)item.get(3)+(double)item1.get(4)));
                    double d2 = Math.abs(2*((double)item.get(4)-(double)item1.get(3))/((double)item.get(4)+(double)item1.get(3)));
//                    double path = Math.pow(d1, 2 ) + Math.pow(d2, 2 );
                    double path = (d1+d2)/2;
                    if(path < min) {
                        min=path;
                        pos=i;
                    }
                }
            }

            if(min < Math.pow(2, 64)) {
                ArrayList list_tmp = new ArrayList();
                list_tmp.add(item.get(0));
                list_tmp.add(item.get(1));
                list_tmp.add(item.get(2));
                list_tmp.add(item.get(3));
                list_tmp.add(item.get(4));
                list_tmp.add(min);
                result.add(list_tmp);
                deltaCounters.remove(first_pos);
                pos--;
            } else {
                deltaCounters.remove(first_pos);
                pos--;
            }
   
        }    
        
        
        return result;
    }
    
//    private String[] GetRemotePort(String node_remote, String id_iface_remote, String name_iface_remote, Map<String, Map<String, ArrayList>> walkInformationFromNodes, Map<String, ArrayList<String>> hash_ip) {
//        String[] result = new String[2];
//        result[0]=id_iface_remote;
//        result[1]=ReplaceDelimiter(TranslateHexString_to_SymbolString(name_iface_remote));
//        
////        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
//        node_remote = GetRealIpAddress(hash_ip, node_remote);
//        
//        if(walkInformationFromNodes.get("ifDescr").get(node_remote) != null) {
//            String[] mas1 = SearchInterfaceName(name_iface_remote, walkInformationFromNodes.get("ifDescr").get(node_remote));
//            if(mas1[0] != null && mas1[1] != null) {
//                result[0]=mas1[0];
//                result[1]=mas1[1];
//            } else {
//                if(name_iface_remote.matches("\\d+")) {
//                    String[] res = GetIfaceName(node_remote, name_iface_remote, walkInformationFromNodes, true);
//                    if(!res[0].equals("")) {
//                        result[0]=res[0];
//                        result[1]=res[1];
//                    } else {
//                        res = GetIfaceName(node_remote, id_iface_remote, walkInformationFromNodes, true);
//                        if(!res[0].equals("")) {
//                            result[0]=res[0];
//                            result[1]=res[1];
//                        }                        
//                    }
//                } 
//            }
//        }
//        
//        return result;
//    }
    
    // Output format: ArrayList
    //    ArrayList<String[]> links
    //    Map<String, String> informationFromNodes
    public ArrayList<String[]> FindHub(ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {
        logger.Println("Starting FindHub ...", logger.INFO);
        ArrayList<String[]> result = new ArrayList();
//        result.addAll(links);

        for(int i=0; i<links.size(); i++) {
            String[] item = links.get(i);
            ArrayList<Integer> find_positions_hub_tmp = FindLinksFromSomeInterface(i, links, nodes_ip_address);
            if(find_positions_hub_tmp.size() > 1) {
                ArrayList<String[]> links_hub_tmp = new ArrayList();
                ArrayList<Integer> find_positions_hub = new ArrayList();
                for(int pos : find_positions_hub_tmp) {
                    if(!find_positions_hub.contains(pos)) { 
                        find_positions_hub.add(pos);
                        links_hub_tmp.add(links.get(pos));
//                        System.out.println("links_hub: "+links_tmp.get(pos)[0]+","+links_tmp.get(pos)[2]+" <---> "+links_tmp.get(pos)[3]+","+links_tmp.get(pos)[5]);
                    }
                }
                for(int ii=find_positions_hub_tmp.size()-1; ii>=0; ii--) {
                    links.remove((int)find_positions_hub_tmp.get(ii));
                }
                
                if(links_hub_tmp.size() > 1) {
                    ArrayList<String[]> links_hub = new ArrayList();
//                    ArrayList<String> buff = new ArrayList();
                    String buff = "";
                    for(String[] it : links_hub_tmp) {
                        String[] mas = new String[6];
                        mas[0]=it[0]; mas[1]=it[1]; mas[2]=it[2];
                        mas[3]="hub"; mas[4]="unknown"; mas[5]="unknown";
                        boolean find=false;
                        for(String[] it1 : links_hub) {
                            if(it1[0].equals(it[0]) && it1[2].equals(it[2])) { find=true; break; }
                            if(it1[3].equals(it[0]) && it1[5].equals(it[2])) { find=true; break; }
                        }
                        if(!find) { links_hub.add(mas); buff=it[0]+"_"+it[2]; }
                        
                        String[] mas1 = new String[6];
                        mas1[0]=it[3]; mas1[1]=it[4]; mas1[2]=it[5];
                        mas1[3]="hub"; mas1[4]="unknown"; mas1[5]="unknown";
                        find=false;
                        for(String[] it1 : links_hub) {
                            if(it1[0].equals(it[3]) && it1[2].equals(it[5])) { find=true; break; }
                            if(it1[3].equals(it[3]) && it1[5].equals(it[5])) { find=true; break; }
                        }                        
                        if(!find) { links_hub.add(mas1); buff=it[0]+"_"+it[2]; }
                    }
                    
//                    Collections.sort(buff);
                    String hub="";
//                    for(String iter : buff) hub=hub+"_"+iter;

                    BitSet bs = BitSet.valueOf(new long[]{buff.hashCode()});
                    hub = "hub_"+bs.toByteArray().toString();
                    
                    for(String[] it : links_hub) {
                        if(it[3].equals("hub")) it[3]=hub;
                    }
                    
                    result.addAll(links_hub);
                }
                i--;
//                    for(String[] it1 : links) {
//                        boolean find=false;
//                        for(String[] it2 : links_hub) {
//                            if( (it1[0].equals(it2[0]) && it1[2].equals(it2[2])) ||
//                                (it1[3].equals(it2[0]) && it1[5].equals(it2[2])) ||  
//                                (it1[0].equals(it2[3]) && it1[2].equals(it2[5])) ||    
//                                (it1[3].equals(it2[3]) && it1[5].equals(it2[5])) ) {
//                                find=true;
//                                break;
//                            }
//                        }
//                        if(find) logger.Println("Deleted link (FindHub):"+it1[0]+" "+it1[2]+" <---> "+it1[3]+" "+it1[5], logger.DEBUG);
//                        else result.add(it1);
//                    }
//                result.addAll(links_hub);

            }
        }
        result.addAll(links);
          
        logger.Println("Stop FindHub.", logger.INFO);
        return result;
    }
    
    private boolean CheckUniqalNode(String node, Map<String, String> info_nodes) {
        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
            String node1 = entry.getKey();
            String value = entry.getValue();
            String[] mas=value.split(";", -1)[7].split("\\|", -1);
            boolean find=false;
            for(String item : mas) {
                String str=item.split(",", -1)[6];
                if(!str.equals("")) {
                    String ip = str.split("/",-1)[0];
                    if(node.equals(ip)) {
                        find=true;
                        break;
                    }
                }
            }
            if(!find) return true;
            else return false;
        } 
        return true;
    }
    
    public boolean CheckUniqalNode(String node, Map<String, String> info_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
            String node1 = entry.getKey();
            ArrayList<String> list_ip = GetIpAddressFromNode(walkInformationFromNodes, node1);
            if(list_ip.indexOf(node) >= 0) return false;
        }
        return true;
    }
    
    public String DuplicateNodeWithNode(String node, Map<String, String> info_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        String duplicate_with_node = "";
        for (Map.Entry<String, String> entry : info_nodes.entrySet()) {
            String node1 = entry.getKey();
            ArrayList<String> list_ip = GetIpAddressFromNode(walkInformationFromNodes, node1);
            int pos = list_ip.indexOf(node);
            if(pos >= 0) {
                duplicate_with_node=list_ip.get(pos)+";"+info_nodes.get(list_ip.get(pos));
                break;
            }
        }
        return duplicate_with_node;
    }    
    
    public ArrayList<String[]> CheckDuplicateLinkList(ArrayList<String[]> links_new, ArrayList<String[]> links, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        Map<String, ArrayList<String>> nodes_ip_address = GetIpAddress(walkInformationFromNodes);
        for(String[] item : links_new) {
            if(CheckDuplicateLink(item, links, nodes_ip_address)) {
                logger.Println("CheckDuplicateLinkList: adding link: "+item[0]+","+item[1]+","+item[2]+" <---> "+item[3]+","+item[4]+","+item[5], logger.DEBUG);
                links.add(item);
            }
        }   
        return links;
    }
    
    public ArrayList<String> DuplicateNodesList(Map<String, String[]> info_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        ArrayList<String> result = new ArrayList();
        Map<String, ArrayList<String>> nodes_ip_address = GetIpAddress(walkInformationFromNodes);
        ArrayList<String> nodes_list = new ArrayList();
        for (Map.Entry<String, String[]> entry : info_nodes.entrySet()) nodes_list.add(entry.getKey());
        
        for(int i=0; i<nodes_list.size(); i++) {
            String node1 = nodes_list.get(i);
            ArrayList<String> list_ip = nodes_ip_address.get(node1);
            if(list_ip == null) { list_ip = new ArrayList(); list_ip.add(node1); }
            for(int j=i+1; j<nodes_list.size(); j++) {
                String node2 = nodes_list.get(j);
                if(list_ip.contains(node2)) {
                    result.add(node2);
                    nodes_list.remove(j);
                    logger.Println("DuplicateNodesList: duplicate node: "+node1+" <---> "+node2, logger.DEBUG);
                    j--;
                }
            }
        }
 
        return result;
    }  
    
    public boolean CheckDuplicateLink(String[] link, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {
        // get unique links(remove mirroring links)
        if(CheckUniqalLink(link, links, nodes_ip_address) == -1) return true;
        else return false;

    }
    
    private int CheckUniqalLink(String[] link, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {

        // get unique links(remove mirroring links)
        if(links.size() == 0) {
            return -1;
        }
        else {
            boolean found=false;
            int pos=0;
            for(String[] item1 : links) {
                ArrayList<String> list_ip1 = nodes_ip_address.get(item1[0]);
                if(list_ip1 == null) { list_ip1 = new ArrayList(); list_ip1.add(item1[0]); }
                ArrayList<String> list_ip2 = nodes_ip_address.get(item1[3]);
                if(list_ip2 == null) { list_ip2 = new ArrayList(); list_ip2.add(item1[3]); }
                if(
                        (list_ip1.indexOf(link[0]) >= 0 && list_ip2.indexOf(link[3]) >= 0) && ( CheckInterfaceName(item1[2],link[2]) || CheckInterfaceName(item1[5],link[5]) ) ||
                        (list_ip2.indexOf(link[0]) >= 0 && list_ip1.indexOf(link[3]) >= 0) && ( CheckInterfaceName(item1[2],link[5]) || CheckInterfaceName(item1[5],link[2]) )
                   ) {
                    found=true;
                    logger.Println("Link duplicate: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5]+"\t\t\t"+item1[0]+","+item1[1]+","+item1[2]+" <---> "+item1[3]+","+item1[4]+","+item1[5], logger.DEBUG);
                    break;
                }
                pos++;
            }
            if(found) {
                return pos;
            }
        }

        return -1;
    }
    
    private ArrayList<Integer> FindLinksFromSomeInterface(int start_pos, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {
        ArrayList<Integer> result = new ArrayList();
        
        ArrayList<String[]> collection = new ArrayList();
        String[] mas = new String[2];
        mas[0]=links.get(start_pos)[0]; mas[1]=links.get(start_pos)[2];
        collection.add(mas);
        String[] mas1 = new String[2];
        mas1[0]=links.get(start_pos)[3]; mas1[1]=links.get(start_pos)[5];
        collection.add(mas1);        
        result.add(start_pos);

        for(int pos=start_pos+1; pos<links.size(); pos++) {
            String[] item = links.get(pos);
            ArrayList<String> list_ip1 = nodes_ip_address.get(item[0]);
            if(list_ip1 == null) { list_ip1 = new ArrayList(); list_ip1.add(item[0]); }
            ArrayList<String> list_ip2 = nodes_ip_address.get(item[3]);
            if(list_ip2 == null) { list_ip2 = new ArrayList(); list_ip2.add(item[3]); }
            ArrayList<String[]> collection_add = new ArrayList();
            for(int i=0; i<collection.size(); i++) {
                String[] mas2 = collection.get(i);
                if(
                        !(mas2[1].matches("^Trk\\d*") || mas2[1].matches("^trk\\d*") || mas2[1].matches("channel") || mas2[1].matches("Channel")) && 
                        (list_ip1.contains(mas2[0]) && CheckInterfaceName(item[2],mas2[1]) ||
                        list_ip2.contains(mas2[0]) && CheckInterfaceName(item[5],mas2[1]) )
                   ) {
                    if(!result.contains(pos)) result.add(pos);
                    String[] mas3 = new String[2];
                    mas3[0]=item[0]; mas3[1]=item[2];
                    collection_add.add(mas3);
                    String[] mas4 = new String[2];
                    mas4[0]=item[3]; mas4[1]=item[5];
                    collection_add.add(mas4);                    
                }
            }
            for(String[] item1 : collection_add) {
                boolean find=false;
                for(String[] item2 : collection) {
                    if(item1[0].equals(item2[0]) && item1[1].equals(item2[1])) {
                        find=true;
                        break;
                    }
                }
                if(!find) collection.add(item1);
            }
        }

        return result;
    }
    
//    public ArrayList<String[]> MergingLinks(ArrayList<String[]> links_new, ArrayList<String[]> links, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList<String[]> result = new ArrayList();
//
//        if(links_new.size() == 0 && links.size() > 0) {
//            result.addAll(links);
//        } else if(links_new.size() > 0 && links.size() == 0) {
//            result.addAll(links_new);
//        } else if(links_new.size() > 0 && links.size() > 0) {
//            result.addAll(links);
//            for(String[] link : links_new) {
//                boolean find=false;
//                for(int pos=0; pos<result.size(); pos++ ) {
//                    String[] link1 = result.get(pos);
//                    ArrayList<String> list_ip1 = GetIpAddressFromNode(walkInformationFromNodes, link1[0]);
//                    ArrayList<String> list_ip2 = GetIpAddressFromNode(walkInformationFromNodes, link1[3]);
//                    if( (list_ip1.indexOf(link[0]) >= 0 && list_ip2.indexOf(link[3]) >= 0) && ( CheckInterfaceName(link1[2],link[2]) || CheckInterfaceName(link1[5],link[5]) ) ) {
//                        logger.Println("Link mirorring: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5]+"\t\t\t"+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//                        find=true;
//                        break;
//                    } else if( (list_ip2.indexOf(link[0]) >= 0 && list_ip1.indexOf(link[3]) >= 0) && ( CheckInterfaceName(link1[2],link[5]) || CheckInterfaceName(link1[5],link[2]) ) ) {
//                        find=true;
//                        result.remove(pos);
//                        pos--;
//                        String[] mas = new String[link.length];
//                        for(int i=0; i<link.length; i++) mas[i]=link[i];
//                        mas[3]=link1[0]; mas[4]=link1[1]; mas[5]=link1[2];
//                        if(link.length == 7) mas[6]=link[6];
//                        result.add(mas);
//                        logger.Println("Rename id or name link: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" from: "+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//                        break;
//                    }
//                }
//                if(!find) {
//                    String[] mas = new String[7];
//                    mas[0]=link[0]; mas[1]=link[1]; mas[2]=link[2];
//                    mas[3]=link[3]; mas[4]=link[4]; mas[5]=link[5];
//                    if(link.length == 7) mas[6]=link[6];
//                    result.add(mas);
//                    if(link.length == 7) logger.Println("Link adding: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5]+" ---"+mas[6], logger.DEBUG);
//                    else logger.Println("Link adding: "+mas[0]+","+mas[1]+","+mas[2]+" <---> "+mas[3]+","+mas[4]+","+mas[5], logger.DEBUG);
//                }
//            }
//        }
//        return result;
//    }
//    
//    public ArrayList<String[]> MergingLinksWithoutDuplicateNode(ArrayList<String[]> links_new, ArrayList<String[]> links, ArrayList<String> duplicate_nodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        for (String duplicate_node : duplicate_nodes) {
//            boolean find1=false; boolean find2=false;
//            for(int i=0; i<links_new.size(); i++) {
//                String[] link = links_new.get(i);
//                if(link[0].equals(duplicate_node)) find1=true;
//                if(link[3].equals(duplicate_node)) find2=true;
//                if(find1 && find2) {
//                    links_new.remove(i);
//                    i--;
//                }
//            }
//        }
//        return MergingLinks(links_new, links, walkInformationFromNodes);
//    }

    public int CheckFullUniqalLink(String[] link, ArrayList<String[]> links, Map<String, ArrayList<String>> nodes_ip_address) {

        // get unique links(remove mirroring links)
        if(links.size() == 0) {
            return -1;
        }
        else {
            boolean found=false;
            int pos=0;
            for(String[] item1 : links) {
                ArrayList<String> list_ip1 = nodes_ip_address.get(item1[0]);
                if(list_ip1 == null) { list_ip1 = new ArrayList(); list_ip1.add(item1[0]); }
                ArrayList<String> list_ip2 = nodes_ip_address.get(item1[3]); 
                if(list_ip2 == null) { list_ip2 = new ArrayList(); list_ip2.add(item1[3]); }
                if(
                        (list_ip1.indexOf(link[0]) >= 0 && list_ip2.indexOf(link[3]) >= 0) && CheckInterfaceName(item1[2],link[2]) && CheckInterfaceName(item1[5],link[5])  ||
                        (list_ip2.indexOf(link[0]) >= 0 && list_ip1.indexOf(link[3]) >= 0) && CheckInterfaceName(item1[2],link[5]) && CheckInterfaceName(item1[5],link[2]) 
                   ) {
                    found=true;
//                    logger.Println("Link mirorring full: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5]+"\t\t\t"+item1[0]+","+item1[1]+","+item1[2]+" <---> "+item1[3]+","+item1[4]+","+item1[5], logger.DEBUG);
                    break;
                }
                pos++;
            }
            if(found) {
                return pos;
            }
        }

        return -1;
    }
    
//    public boolean WriteToMapFile(String filename, String buff, Map<String, String> info_nodes, ArrayList<String[]> links, ArrayList<String[]> ARP_MAC, Map<String, String> excluded_nodes, String host_time_live, String history_dir, int history_num_days) {
//        BufferedWriter outFile = null;
//        BufferedWriter outBuff = null;
//        File file = new File(filename);
//        try {
//            if(file.exists()) {
//                logger.Println("File: "+filename+" is exist.", logger.DEBUG);
//                ArrayList<String[]> nodes_in_mapfile = ReadFromMapFile(filename, "^:nodes:$");
//                ArrayList<String[]> links_in_mapfile = ReadFromMapFile(filename, "^:links:$");
//                ArrayList<String[]> hosts_in_mapfile = ReadFromMapFile(filename, "^:hosts:$");
//                ArrayList<String[]> custom_texts_in_mapfile = ReadFromMapFile(filename, "^:custom_texts:$");
////                ArrayList<String[]> extend_info_in_mapfile = ReadFromMapFile(filename, "^:extend_info:$");
//                ArrayList<String[]> text_in_mapfile = ReadFromMapFile(filename, "^:text:$");
//                
//                ArrayList<String[]> nodes_in_buff = ReadFromMapFile(buff, "^:nodes:$");
//                ArrayList<String[]> links_in_buff = ReadFromMapFile(buff, "^:links:$");
//                
//                //clear delete.buff file
//                for(int i=0; i<nodes_in_buff.size(); i++) {
//                    String[] item = nodes_in_buff.get(i);
//                    if(System.currentTimeMillis()-Long.valueOf(item[item.length-1]) > Long.valueOf(host_time_live)*86400*1000) {
//                        nodes_in_buff.remove(i);
//                        logger.Println("Node: "+item[0]+" remove from "+buff+" time exceed.", logger.DEBUG);
//                    }
//                }
//                for(int i=0; i<links_in_buff.size(); i++) {  
//                    String[] item = links_in_buff.get(i);
//                    if(System.currentTimeMillis()-Long.valueOf(item[item.length-1]) > Long.valueOf(host_time_live)*86400*1000) {
//                        links_in_buff.remove(i);
//                        logger.Println("link: "+item[0]+","+item[1]+","+item[2]+" <---> "+item[3]+","+item[4]+","+item[5]+" remove from "+buff+" time exceed.", logger.DEBUG);
//                    }
//                }
//                
//                outFile = new BufferedWriter(new FileWriter(filename+".temp"));
//                outBuff = new BufferedWriter(new FileWriter(buff));
//                
//                //nodes
//                for(String[] item : nodes_in_mapfile) {
////                    Map<String, String> info_nodes = (Map<String, String>) informationFromNodes.get(0);
//                    boolean find=false;
//                    for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                        if(entry.getKey().equals(item[0])) {
//                            find=true;
//                            break;                            
//                        }
//                    }
//                    if(!find) {
//                        if(nodes_in_buff.size() > 0) {
//                            for(int i=0; i<nodes_in_buff.size(); i++) {
//                                String[] item1 = nodes_in_buff.get(i);
//                                if(item[0].equals(item1[0])) {
//                                    nodes_in_buff.remove(i);
//                                    i--;
//                                }
//                            }
//                            if(!item[0].matches("^hub_\\d+")) {
//                                String[] mas = new String[4];
//                                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2]; mas[3]=String.valueOf(System.currentTimeMillis());
//                                nodes_in_buff.add(mas);
//                                logger.Println("Node: "+item[0]+" adding to nodes_in_buff", logger.DEBUG);
//                            }
//                        } else {
//                            if(!item[0].matches("^hub_\\d+")) {
//                                String[] mas = new String[4];
//                                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2]; mas[3]=String.valueOf(System.currentTimeMillis());
//                                nodes_in_buff.add(mas);
//                                logger.Println("Node: "+item[0]+" adding to nodes_in_buff", logger.DEBUG); 
//                            }
//                        }
//                    }
//                }    
//
//                outFile.write(":nodes:\n");
////                Map<String, String> info_nodes = (Map<String, String>) informationFromNodes.get(0);
//                for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                    
//                    boolean find=false;
//                    String coord = "";
//                    String node_image = "";
//                    for(String[] item : nodes_in_mapfile) {
//                        if(entry.getKey().equals(item[0])) {
//                            find=true;
//                            coord=item[1];
//                            node_image=item[2];
//                            break;
//                        }
//                    }
//                    if(find) {
//                        String line=entry.getKey()+";"+coord+";"+node_image+";"+entry.getValue();
//                        outFile.write(line+"\n");
//                        logger.Println("Node: "+line+" write to "+filename+".temp. Is exist node.", logger.DEBUG);
//                    } else {
//                        boolean find1=false;
//                        String coord1 = "";
//                        String node_image1 = "";                        
//                        for(String[] item : nodes_in_buff) {
//                            if(entry.getKey().equals(item[0])) {
//                                find1=true;
//                                coord1=item[1];
//                                node_image1=item[2]; 
//                                nodes_in_buff.remove(item);
//                                break;
//                            }
//                        }
//                        if(find1) {
//                            String line=entry.getKey()+";"+coord1+";"+node_image1+";"+entry.getValue();
//                            outFile.write(line+"\n");
//                            logger.Println("Node: "+line+" write to "+filename+".temp. Is new node. Exist in nodes_in_buff", logger.DEBUG);
//                        } else {
//                            String line=entry.getKey()+";;;"+entry.getValue();
//                            outFile.write(line+"\n");
//                            logger.Println("Node: "+line+" write to "+filename+".temp. Is new node.", logger.DEBUG);
//                        }
//                    }
//                    
//                }
//                outBuff.write(":nodes:\n");
//                for(String[] item : nodes_in_buff) {
////                    if(item[0].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
//                        String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3];
//                        outBuff.write(line+"\n");
//                        logger.Println("Node: "+line+" write to "+buff, logger.DEBUG);
////                    }
//                }                
//                
//                
//                //links
//                for(String[] item : links_in_mapfile) {
//                    if(item[0].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && item[3].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && 
//                            !(item[0].matches("^hub_\\d+") || item[3].matches("^hub_\\d+")) ) {
//                        boolean find=false;
//                        String[] link = new String[7];
//                        link[0]=item[0]; link[1]=item[1]; link[2]=item[2]; link[3]=item[3]; link[4]=item[4]; link[5]=item[5];
//                        link[6]=String.valueOf(System.currentTimeMillis());                        
//                        if(links.size() > 0) {
//                            for(String[] item1 : links) {
//                                if( (item[0].equals(item1[0]) && item[2].equals(item1[2]) && item[3].equals(item1[3]) && item[5].equals(item1[5])) ||
//                                    (item[0].equals(item1[3]) && item[2].equals(item1[5]) && item[3].equals(item1[0]) && item[5].equals(item1[2]))    
//                                        ) { 
//                                    find=true; break; 
//                                }
//                            }
//                            if(!find) {
//                                links_in_buff.add(link);
//                                logger.Println("Adding link from: "+filename+" - "+link[0]+";"+link[1]+";"+link[2]+";"+link[3]+";"+link[4]+";"+link[5]+";"+link[6]+" to links_in_buff", logger.DEBUG);
//                            } else logger.Println("Not adding link from: "+filename+" - "+link[0]+";"+link[1]+";"+link[2]+";"+link[3]+";"+link[4]+";"+link[5]+";"+link[6]+" to links_in_buff", logger.DEBUG);
//                        } else {
//                            links_in_buff.add(link);
//                            logger.Println("Adding link from: "+filename+" - "+link[0]+";"+link[1]+";"+link[2]+";"+link[3]+";"+link[4]+";"+link[5]+";"+link[6]+" to links_in_buff", logger.DEBUG);
//                        }
//                    }
//                }
//                
//                for(int i=0; i<links_in_buff.size(); i++) {
//                    String[] item = links_in_buff.get(i);
//                    if(item[0].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && item[3].matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && 
//                            !(item[0].matches("^hub_\\d+") || item[3].matches("^hub_\\d+")) ) {
//                        int pos=-1;
//                        if(links.size() > 0) {
//                            for(String[] item1 : links) {
//                                if( (item[0].equals(item1[0]) && item[2].equals(item1[2]) && item[3].equals(item1[3]) && item[5].equals(item1[5])) ||
//                                    (item[0].equals(item1[3]) && item[2].equals(item1[5]) && item[3].equals(item1[0]) && item[5].equals(item1[2]))    
//                                        ) { 
//                                    pos=i;
//                                    break; 
//                                }
//                            }
//                            if(pos >= 0) {
//                                logger.Println("Remove link from: "+buff+" - "+links_in_buff.get(pos)[0]+";"+links_in_buff.get(pos)[1]+";"+links_in_buff.get(pos)[2]+";"+links_in_buff.get(pos)[3]+";"+links_in_buff.get(pos)[4]+";"+links_in_buff.get(pos)[5]+";"+links_in_buff.get(pos)[6]+" to links_in_buff", logger.DEBUG);
//                                links_in_buff.remove(pos);
//                                i--;
//                            }
//                        }
//                    }
//                } 
//                
//                outBuff.write(":links:\n");
//                for(String[] item : links_in_buff) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+";"+item[6];
//                    outBuff.write(line+"\n");
//                    logger.Println("links: "+line+" write to "+buff, logger.DEBUG);
//                }
//                
//                outFile.write(":links:\n");
//                for(String[] item : links) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5];
//                    outFile.write(line+"\n");
//                    logger.Println("links: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//
//                //hosts
//                outFile.write(":hosts:\n");
//                for(String[] item : ARP_MAC) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+System.currentTimeMillis();
//                    outFile.write(line+"\n");
//                    logger.Println("host: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//
//                //custom_texts information
//                outFile.write(":custom_texts:\n");
//                for(String[] item : custom_texts_in_mapfile) {
//                    String line=item[0]+";"+item[1]+";"+item[2];
//                    outFile.write(line+"\n");
//                    logger.Println("custom_texts: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }     
//                
//                //text information
//                outFile.write(":text:\n");
//                for(String[] item : text_in_mapfile) {
//                    String line=item[0]+";"+item[1]+";"+item[2];
//                    outFile.write(line+"\n");
//                    logger.Println("text: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }     
//                
//                //extend_info information
//                outFile.write(":extend_info:\n");
//                for(Map.Entry<String, String> entry : excluded_nodes.entrySet()) {
//                    boolean find=false;
//
//                    for(Map.Entry<String, String> entry1 : info_nodes.entrySet()) {
//                        if(entry.getKey().equals(entry1.getKey())) { find=true; break; }
//                    }                    
//                    if(!find) {
//                        String line=entry.getKey()+";"+entry.getValue();
//                        outFile.write(line+"\n");
//                        logger.Println("extend_info: "+line+" write to "+filename+".temp", logger.DEBUG);
//                    }
//                }                
////                for(String[] item : extend_info_in_mapfile) {
////                    boolean find=false;
////                    for(Map.Entry<String, String> entry1 : info_nodes.entrySet()) {
////                        if(item[0].equals(entry1.getKey())) { find=true; break; }
////                    }
////                    if(!find) {
////                        String line=item[0];
////                        for(int i=1; i<item.length; i++) line=line+";"+item[i];
////                        outFile.write(line+"\n");
////                        logger.Println("extend_info: "+line+" write to "+filename+".temp", logger.DEBUG);
////                    }
////                }                 
//            } else {
//                logger.Println("File: "+filename+" not exist.", logger.DEBUG);
//                outFile = new BufferedWriter(new FileWriter(filename+".temp"));
//                outFile.write(":nodes:\n");
//                for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                    String line=entry.getKey()+";;;"+entry.getValue();
//                    outFile.write(line+"\n");
//                    logger.Println("Node: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//                outFile.write(":links:\n");
//                for(String[] item : links) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5];
//                    outFile.write(line+"\n");
//                    logger.Println("link: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//                outFile.write(":hosts:\n");
//                for(String[] item : ARP_MAC) {
//                    String line=item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+System.currentTimeMillis();
//                    outFile.write(line+"\n");
//                    logger.Println("host: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }
//                //extended information
//                outFile.write(":extend_info:\n");
//                for(Map.Entry<String, String> entry : excluded_nodes.entrySet()) {
//                    String line=entry.getKey()+";"+entry.getValue();
//                    outFile.write(line+"\n");
//                    logger.Println("extend_info: "+line+" write to "+filename+".temp", logger.DEBUG);
//                }                       
//            }
//
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            
//            // remove old map files
//            RemoveOldFiles(history_dir, history_num_days);
//            // replace map file to history
//            File file_tmp = new File(filename+".temp");
//            if(file_tmp.exists()) {
//                Date d = new Date(file.lastModified());
//                SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy-HH.mm");
//                String file_history = history_dir+"/Neb_"+format1.format(d)+".map"; 
//                File folder_history = new File(history_dir);
//                File history = new File(file_history);
//                if (!folder_history.exists()) {
//                    folder_history.mkdir();
//                }                  
//                if(!history.exists()) file.renameTo(history);
//                if(file_tmp.exists()) {
//                    file.delete();
//                    if(!file.exists()) file_tmp.renameTo(file);
//                }
//            }
//            
//        } catch (Exception ex) {
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        } finally {
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            if(outBuff != null) try {
//                outBuff.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            
//        }
//
//        return true;
//    }
//    
//    public ArrayList ReadFromMapFile(String filename, String match) {
//        ArrayList<String[]> result = new ArrayList();
//
//        BufferedReader inFile = null;
//        File file = new File(filename);
//        if(file.exists()) {
//            try {
//                inFile = new BufferedReader(new FileReader(filename));
//                while(true) {
//                    String line = inFile.readLine().toString();
//                    if(line.matches(match)) break;
//                }
//                while(true) {
//                    String line = inFile.readLine().toString();
//                    if(line.matches("^:\\S+:$")) break;
//                    if(!line.equals("")) result.add(line.split(";", -1));
////                    System.out.println(line);
//                }                
//            } catch (Exception ex) {} 
//            finally {
//                if(inFile != null) try {
//                    inFile.close();
//                } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            }
//        }
//        
//        return result;
//    }
//    
//    private ArrayList<String[]> ReadFromMapFile(File file, String match) {
//        ArrayList<String[]> result = new ArrayList();
//
//        BufferedReader inFile = null;
//        if (file.exists()) {
//            try {
//                inFile = new BufferedReader(new FileReader(file));
//                while (true) {
//                    String line = inFile.readLine().toString();
//                    if (line.matches(match)) {
//                        break;
//                    }
//                }
//                while (true) {
//                    String line = inFile.readLine().toString();
//                    if (line.matches("^:\\S+:$")) {
//                        break;
//                    }
//                    if (!line.equals("")) {
//                        result.add(line.split(";", -1));
//                    }
////                    System.out.println(line);
//                }
//            } catch (Exception ex) {
//            } finally {
//                if (inFile != null) {
//                    try {
//                        inFile.close();
//                    } catch (IOException ex) {
//                    }
//                }
//            }
//        }
//
//        return result;
//    }    
    
    public Map RescanInformationFromNodes(Map informationFromNodes, 
            ArrayList<String> community_list, ArrayList<ArrayList<String>> accounts_list, 
            ArrayList<String> include_list, ArrayList<String> include_sysDescr_cli_matching, 
            Map<String, String[]> snmp_accounts_priority,
            Map<String, String[]> cli_accounts_priority, ArrayList<String> net_list) {    
        Map result = new HashMap();
        
        if(informationFromNodes.size() == 3) {
            ru.kos.neb.neb_lib.Utils lib_utils = new ru.kos.neb.neb_lib.Utils();
            ArrayList<String> list_ip_test = new ArrayList();            
            Map<String, String> exclude_list = (Map<String, String>)informationFromNodes.get("exclude_list");
            for(Map.Entry<String, Map> entry : ((Map<String, Map>)informationFromNodes.get("nodes_information")).entrySet()) {
//                logger.Println("Get ip addresses from node - "+entry.getKey()+" to list_ip_test", logger.DEBUG);
                // adding ip address from arp
                Map<String, Map> advanced = (Map<String, Map>)entry.getValue().get("advanced");
                if(advanced != null && advanced.size() > 0) {
                    Map<String, String> arp_list = (Map<String, String>)advanced.get("arp");
                    if(arp_list != null) {
                        for(Map.Entry<String, String> ip_mac : arp_list.entrySet()) {
                            String ip = ip_mac.getValue();
                            if(!list_ip_test.contains(ip) && exclude_list.get(ip) == null) {
                                boolean find=false;
                                for(String include_network : include_list) {
                                    if(lib_utils.InsideInterval(ip, include_network)) {
                                        find=true;
                                        break;
                                    }
                                }
                                if(find) list_ip_test.add(ip);
                                else logger.Println("ip="+ip+" not included.", logger.DEBUG);
                            }                        

                        }
                    }
                }
                
                // adding ip address from cdp, lldp
                if(entry.getValue().get("advanced") != null) {
                    Map<String, ArrayList<Map<String, String>>> links_list = (Map<String, ArrayList<Map<String, String>>>)((Map<String, Map>)entry.getValue().get("advanced")).get("links");
                    if(links_list != null) {
                        for(Map.Entry<String, ArrayList<Map<String, String>>> entry1 : links_list.entrySet()) {
                            ArrayList<Map<String, String>> list = entry1.getValue();
                            for(Map<String, String> item : list) {
                                String remote_ip = item.get("remote_ip");
                                if(remote_ip != null && !remote_ip.matches("^0\\.0\\.0\\.\\d+$") && remote_ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                                    if(!list_ip_test.contains(remote_ip) && exclude_list.get(remote_ip) == null) {
                                        boolean find=false;
                                        for(String include_network : include_list) {
                                            if(lib_utils.InsideInterval(remote_ip, include_network)) {
                                                find=true;
                                                break;
                                            }
                                        }
                                        if(find) {
                                            list_ip_test.add(remote_ip);
                                            logger.Println("Adding from links remote_ip "+remote_ip, logger.DEBUG);
                                        }
                                        else logger.Println("ip="+remote_ip+" not included.", logger.DEBUG);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        
            for(String ip_test : list_ip_test)
            {
                logger.Println("list_ip_test="+ip_test, logger.DEBUG);
            }
            
            if(list_ip_test.size() > 0) {
                logger.Println("Scanning recursive list ...", logger.INFO);

                NodesScanPool nodesScanPool = new NodesScanPool();
                ArrayList<String> ip_cli_list = new ArrayList();
                ArrayList<Long> list_tmp = new ArrayList();
                if(Neb.cfg.get("excluded_port") != null) {
                    list_tmp=(ArrayList<Long>)Neb.cfg.get("excluded_port");
                }
                ArrayList<Integer> excluded_port = new ArrayList();
                for(long port : list_tmp) excluded_port.add((int)port);
                Map<String, String> res = nodesScanPool.Get(list_ip_test, excluded_port, Neb.timeout/2, Neb.retries);
                for(Map.Entry<String, String> entry : res.entrySet()) {
                    if(entry.getValue().equals("ok")) {
                        ip_cli_list.add(entry.getKey());
                        logger.Println("nodesScan: "+entry.getKey(), logger.DEBUG);
                    }
                }                
                
                logger.Println("Start scanning snmp ip list ...", logger.DEBUG);
                ArrayList<String[]> node_community_version = new ArrayList();
                SnmpScan snmpScan = new SnmpScan();
                ArrayList<String[]> res1 = snmpScan.Scan(ip_cli_list, exclude_list, community_list, "1.3.6.1.2.1.1.3.0", 161, Neb.timeout/2, Neb.retries, snmp_accounts_priority);
                for(String[] mas : res1) {
                    node_community_version.add(mas);
                    logger.Println("snmpScan: "+mas[0]+","+mas[1]+","+mas[2], logger.DEBUG);
                }
                logger.Println("Stop scanning snmp ip list.", logger.DEBUG);
        
                logger.Println("Start Rescanning ...", logger.DEBUG);
                node_community_version = RescanSNMPVersion(node_community_version, Neb.timeout, Neb.retries);
                logger.Println("Stop Rescanning.", logger.DEBUG);        
        
                ArrayList<String[]> cli_node_account = new ArrayList();
                if(accounts_list != null && accounts_list.size() > 0) {
                    if(node_community_version.size() > 0) {
                        logger.Println("Start scanning cli ip list ...", logger.DEBUG);
                        ArrayList<String> ip_list = new ArrayList();
                        for(String[] mas : node_community_version) {
                            boolean matchng=false;
                            for(String mstr : include_sysDescr_cli_matching) {
                                if(mas[3].replaceAll("[\n\r]", " ").matches(".*"+mstr+".*")) {
                                    matchng=true;
                                    break;
                                }
                            }
                            if(matchng) {
                                ip_list.add(mas[0]);
                                logger.Println("Matching node: "+mas[0], logger.DEBUG);
                            }
                            else logger.Println("Not matching node: "+mas[0], logger.DEBUG);
                        }

                        cli_node_account = RunScriptCliTest(ip_list, exclude_list, accounts_list, Neb.timeout, Neb.retries, cli_accounts_priority);

                        logger.Println("Stop scanning cli ip list.", logger.DEBUG);
                    }
                }

                //merging node_protocol_accounts and node_community_version
                Map<String, ArrayList<String[]>> node_protocol_accounts = MergingCliAndSnmpAccounts(cli_node_account, node_community_version);
                logger.Println("New scanning nodes = "+node_protocol_accounts.size(), logger.DEBUG);


                logger.Println("Start get information from nodes ...", logger.INFO);
                Map nodes_information = new HashMap();
                if(Neb.cfg.get("scripts") != null && ((Map)Neb.cfg.get("scripts")).get("get_info_node") != null) {
                    Map<String, ArrayList<String>> scripts = (Map<String, ArrayList<String>>)((Map)Neb.cfg.get("scripts")).get("get_info_node");
                    if(scripts.size() > 0 && node_protocol_accounts.size() > 0) {
                        RunScriptsPool runScriptsPool = new RunScriptsPool(Neb.timeout_process, Neb.timeout_output, Neb.MAX_RUNSCRIPT_CLI);
                        ArrayList<String> mas = runScriptsPool.Get(node_protocol_accounts, scripts, Neb.timeout, Neb.retries);
//                        String[] mas = out.split("\n");

                        JSONParser parser = new JSONParser();

                        for(String it : mas) {
                            try {
                                JSONObject jsonObject = (JSONObject)parser.parse(it);
                                Map<String, Object> map = toMap(jsonObject);
                                for(Map.Entry<String, Object> entry : map.entrySet()) {
                                    String node = entry.getKey();
                                    Map val = (Map)entry.getValue();
                                    nodes_information.put(node, val);
                                }
                            } catch (ParseException ex) {
                                if(DEBUG) System.out.println(ex);
                                ex.printStackTrace();
//                                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

        //                for(Map.Entry<String, ArrayList<String[]>> entry : node_protocol_accounts.entrySet()) {
        //                    String node = entry.getKey();
        //                    if(nodes_information.get(node) == null) System.out.println("Node "+node+"not exist!!!");
        //                }

//                        System.out.println(out);
                    }
                }        
                logger.Println("Stop get information from nodes.", logger.INFO);
                nodes_information = Uniqal_Nodes_Information(nodes_information);

                Gson gson = new Gson();
                for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet() ) {
                    String str = gson.toJson(entry.getValue());            
                    logger.Println("nodes_information="+entry.getKey()+" - "+str, logger.DEBUG);
        //            System.out.println("nodes_information="+entry.getKey()+" - "+str);
                }
                
                // merging nodes_information
                ((Map<String, Map>)informationFromNodes.get("nodes_information")).putAll(nodes_information);
                result.put("nodes_information", (Map<String, Map>)informationFromNodes.get("nodes_information"));

                Map<String, String> ip_from_nodes=GetIpFromNodes(nodes_information, net_list);
                exclude_list.putAll(ip_from_nodes);
                for(String ip : list_ip_test) {
                    exclude_list.put(ip, ip);
                }
                result.put("exclude_list", exclude_list);
                
                // translate node_protocol_accounts Array to ArrayList
                Map<String, ArrayList<ArrayList<String>>> node_protocol_accounts_new = TranslateNodeProtocolAccountsToList(node_protocol_accounts);
                
                // merging node_protocol_accounts
                ((Map<String, ArrayList<ArrayList<String>>>)informationFromNodes.get("node_protocol_accounts")).putAll(node_protocol_accounts_new);
                result.put("node_protocol_accounts", (Map<String, Map>)informationFromNodes.get("node_protocol_accounts"));                
 
                // checking nodes not get information !!!
                for(Map.Entry<String, ArrayList<String[]>> entry : ((Map<String, ArrayList<String[]>>)node_protocol_accounts).entrySet()) {
                    String node = entry.getKey();
                    String node_base_ip = ip_from_nodes.get(node);
                    if(node_base_ip == null) node_base_ip=node;
                    boolean find = false;
                    for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)nodes_information).entrySet() ) {
                        String node1 = entry1.getKey();
                        String node_base_ip1 = ip_from_nodes.get(node1);
                        if(node_base_ip1 == null) node_base_ip=node1;
                        if(node_base_ip.equals(node_base_ip1)) {
                            find = true;
                            break;
                        }
                    }
                    if(!find) logger.Println("Not access information from nodes - "+node_base_ip+" !!!", logger.DEBUG);
                }  

            }
        }
        
        return result;
    }
    
    public ArrayList Merge_nodes_information(ArrayList new_list, ArrayList old_list, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
        ArrayList result = new ArrayList();
        
        Map<String, String> information_nodes_res = new HashMap<>();
        ArrayList<String> exclude_list = new ArrayList();

        Map<String, String> information_nodes_new = (Map<String, String>)new_list.get(0);
        Map<String, String> information_nodes_old = (Map<String, String>)old_list.get(0);

        ArrayList<String[]> dplinks_new = (ArrayList<String[]>)new_list.get(1);
        ArrayList<String[]> dplinks_old = (ArrayList<String[]>)old_list.get(1);

        // merging node information
        for(Map.Entry<String, String> entry : information_nodes_new.entrySet()) {
            String node_new = entry.getKey();
            String node_info_new = entry.getValue();

            boolean find=false;
            for(Map.Entry<String, String> entry1 : information_nodes_old.entrySet()) {
                String node_old = entry1.getKey();
                String node_info_old = entry1.getValue();

                if(node_new.equals(node_old)) {
                    String[] mas = node_info_old.split(";", -1)[0].split("\\|");
                    if(mas.length != 3) {
                        String[] mas1 = node_info_new.split(";", -1)[0].split("\\|");
                        if(mas1.length != 3) {
                            if(node_new.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                               exclude_list.add(node_new);
                            }
                        }
                        information_nodes_res.put(node_new, node_info_new);
                    } else {
                        information_nodes_res.put(node_old, node_info_old);
                    }
                    find=true;
                    break;
                }
            }
            if(!find) {
//                String[] mas = node_info_new.split(";", -1)[0].split("\\|");
//                if(mas.length != 3) {
//                    if(node_new.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
//                       exclude_list.add(node_new);
//                    }                        
//                }
                information_nodes_res.put(node_new, node_info_new);
            }
        }

        for(Map.Entry<String, String> entry : information_nodes_old.entrySet()) {
            String node_old = entry.getKey();
            String node_info_old = entry.getValue();
            boolean find=false;
            for(Map.Entry<String, String> entry1 : information_nodes_new.entrySet()) {
                String node_new = entry1.getKey();
                if(node_old.equals(node_new)) {
                    find=true;
                    break;
                }
            }
            if(!find) {
                String[] mas = node_info_old.split(";", -1)[0].split("\\|");
                if(mas.length != 3) {
                    if(node_old.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                       exclude_list.add(node_old);
                    }                        
                }                    
                information_nodes_res.put(node_old, node_info_old);
            }
        }

        // merging links
        Map<String, ArrayList<String>> nodes_ip_address = GetIpAddress(walkInformationFromNodes);
        for(String[] item : dplinks_new) {
            if(CheckDuplicateLink(item, result, nodes_ip_address)) {
                dplinks_old.add(item);
            }
        }

        // merging walk info
        Map<String, Map<String, ArrayList>> walkInformation_new = (Map<String, Map<String, ArrayList>>)new_list.get(2);
        Map<String, Map<String, ArrayList>> walkInformation_old = (Map<String, Map<String, ArrayList>>)old_list.get(2);
        
        Map<String, Map<String, ArrayList>> walkInformation = MergingWalk(walkInformation_new, walkInformation_old);
        
        
//        for (Map.Entry<String, Map<String, ArrayList>> entry : walkInformation_new.entrySet()) {
//            String key = entry.getKey();
//            Map<String, ArrayList> val = entry.getValue();
//            Map<String, ArrayList> val_all = walkInformation.get(key);
//            val_all.putAll(val);
//            walkInformation.put(key, val_all);
//        }

        // merging exclude list
        ArrayList<String> exclude_old = (ArrayList<String>)old_list.get(3);
        for(String item : exclude_list) {
            if(exclude_old.indexOf(item) < 0) exclude_old.add(item);
        }

        // merging node_community_version
        ArrayList<String[]> node_community_version_old = (ArrayList<String[]>)old_list.get(4);
        for(String[] item : (ArrayList<String[]>)new_list.get(4)) {
            boolean find=false;
            for(String[] item_old : node_community_version_old) {
                if(item_old[0].equals(item[0])) {
                    find=true;
                    break;
                }
            }
            if(!find) node_community_version_old.add(item);
        }

        result.add(information_nodes_res);
        result.add(dplinks_old);
        result.add(walkInformation);
        result.add(exclude_old);
        result.add(node_community_version_old);

        return result;
    }
    
    public Map<String, Map<String, ArrayList>> MergingWalk(Map<String, Map<String, ArrayList>> walkInformation_new, Map<String, Map<String, ArrayList>> walkInformation_old) {
        Map<String, Map<String, ArrayList>> result = new HashMap();
        result.putAll(walkInformation_old);
        
        for (Map.Entry<String, Map<String, ArrayList>> entry : walkInformation_new.entrySet()) {
            String key = entry.getKey();
            Map<String, ArrayList> val = entry.getValue();
            Map<String, ArrayList> val_all = walkInformation_old.get(key);
            if(val_all == null) {
                val_all = new HashMap();
            }
            val_all.putAll(val);
            result.put(key, val_all);            
        } 
        return result;
    }
    
    public ArrayList<String[]> GetArpMacFromNodes(ArrayList<String[]> node_community_version) {
//        ru.kos.neb.neb_lib.Utils.DEBUG = true;
        
        ArrayList<String[]> getARPMAC = new ArrayList();
        logger.Println("Start GetARP...", logger.DEBUG);
        Map<String, String> getARP = GetARP(node_community_version);
        for (Map.Entry<String, String> entry : getARP.entrySet()) {
            String mac = entry.getKey();
            String ip = entry.getValue();
            logger.Println(mac+" - "+ip, logger.DEBUG);
        }
        logger.Println("Stop GetARP.", logger.DEBUG);
        logger.Println("Start GetMAC...", logger.DEBUG);
        Map<String, Map<String, ArrayList>> getMAC = GetMAC(node_community_version);
        logger.Println("Stop GetMAC...", logger.DEBUG);
        logger.Println("Start svodka ARP MAC...", logger.DEBUG);
        getARPMAC = GetARPMAC(getMAC, getARP, node_community_version);
        logger.Println("Stop svodka ARP MAC...", logger.DEBUG);
        
//        ru.kos.neb.neb_lib.Utils.DEBUG = false;
        
        return getARPMAC;
    }
    
    public Map ScanInformationFromNodes(ArrayList<String> network_list, 
            ArrayList<String> ip_list, ArrayList<String> community_list, 
            ArrayList<ArrayList<String>> accounts_list, 
            Map<String, String> exclude_list, 
            ArrayList<String> include_sysDescr_cli_matching, 
            Map<String, String[]> snmp_accounts_priority,
            Map<String, String[]> cli_accounts_priority, ArrayList<String> net_list) {
        Map result = new HashMap();
        
        logger.Println("Scanning networks ...", logger.INFO);
        SnmpScan snmpScan = new SnmpScan();
//        ScanCli scanCli = new ScanCli();
        NodesScanPool nodesScanPool = new NodesScanPool();
        PingPool pingPool = new PingPool();
        ArrayList<String[]> node_community_version = new ArrayList();
        ArrayList<String[]> cli_node_account = new ArrayList();
        ArrayList<String> list_ip_ping = new ArrayList();
        list_ip_ping.addAll(ip_list);
        for (String network : network_list) {
            logger.Println("Start scanning snmp networks:\t" + network + " ...", logger.DEBUG);
            
            ArrayList<String> list_ip = new ArrayList();
            Map<String, String> res = pingPool.Get(network, exclude_list, Neb.timeout/2, Neb.retries);
            for(Map.Entry<String, String> entry : res.entrySet()) {
                if(entry.getValue().equals("ok")) {
                    list_ip.add(entry.getKey());
                    logger.Println("pingPool: "+entry.getKey(), logger.DEBUG);
                }
            }
            
            ArrayList<Long> list_tmp = new ArrayList();
            if(Neb.cfg.get("excluded_port") != null) {
                list_tmp=(ArrayList<Long>)Neb.cfg.get("excluded_port");
            }
            ArrayList<Integer> excluded_port = new ArrayList();
            for(long port : list_tmp) excluded_port.add((int)port);
            
            ArrayList<String> ip_cli_list = new ArrayList();
            res = nodesScanPool.Get(list_ip, excluded_port, Neb.timeout/2, Neb.retries);
            for(Map.Entry<String, String> entry : res.entrySet()) {
                if(entry.getValue().equals("ok")) {
                    ip_cli_list.add(entry.getKey());
                    logger.Println("nodesScan: "+entry.getKey(), logger.DEBUG);
                }
            }  
            list_ip_ping.addAll(ip_cli_list);
//            System.out.println("list_ip_ping size = "+list_ip_ping.size());
            logger.Println("list_ip_ping size = "+list_ip_ping.size(), logger.DEBUG);
            // output
//            for(String node : list_ip) logger.Println("node pinger=" + node, logger.DEBUG);
            ArrayList<String[]> res1 = snmpScan.Scan(list_ip, exclude_list, community_list, "1.3.6.1.2.1.1.3.0", Neb.timeout/2, Neb.retries);
//            System.out.println("snmpScan.Scan size = "+res1.size());
            logger.Println("snmpScan.Scan size = "+res1.size(), logger.DEBUG);
            
            for(String[] mas : res1) {
                node_community_version.add(mas);
                logger.Println("snmpScan: "+mas[0]+","+mas[1]+","+mas[2], logger.DEBUG);
            }
            logger.Println("Stop scanning snmp networks:\t" + network, logger.DEBUG);
        }
        
        if(ip_list.size() > 0) {
            logger.Println("Start scanning snmp ip list ...", logger.DEBUG);
            ArrayList<String[]> res1 = snmpScan.Scan(ip_list, exclude_list, community_list, "1.3.6.1.2.1.1.3.0", 161, Neb.timeout/2, Neb.retries, snmp_accounts_priority);
            for(String[] mas : res1) {
                node_community_version.add(mas);
                logger.Println("snmpScan: "+mas[0]+","+mas[1]+","+mas[2], logger.DEBUG);
//                exclude_list.put(mas[0], mas[0]);
            }
            logger.Println("Stop scanning snmp ip list.", logger.DEBUG);
        }
        
        logger.Println("Start Rescanning ...", logger.DEBUG);
        node_community_version = RescanSNMPVersion(node_community_version, Neb.timeout, Neb.retries);
        logger.Println("Stop Rescanning.", logger.DEBUG);        
        
        if(accounts_list != null && accounts_list.size() > 0) {
            if(node_community_version.size() > 0) {
                logger.Println("Start scanning cli ip list ...", logger.DEBUG);
                ArrayList<String> ip_cli_list = new ArrayList();
                for(String[] mas : node_community_version) {
                    logger.Println("RescanSNMPVersion: "+mas[0]+","+mas[1]+","+mas[2], logger.DEBUG);
                    boolean matchng=false;
                    for(String mstr : include_sysDescr_cli_matching) {
                        if(mas[3].replaceAll("[\n\r]", " ").matches(".*"+mstr+".*")) {
                            matchng=true;
                            break;
                        }
                    }
                    if(matchng) {
                        ip_cli_list.add(mas[0]);
                        logger.Println("Matching node: "+mas[0], logger.DEBUG);
                    } else logger.Println("Not matching node: "+mas[0], logger.DEBUG);
                }

                cli_node_account = RunScriptCliTest(ip_cli_list, exclude_list, accounts_list, Neb.timeout, Neb.retries, cli_accounts_priority);
                logger.Println("Stop scanning cli ip list.", logger.DEBUG);
            } 
        }
        logger.Println("End scanning networks.", logger.INFO);

        //merging node_protocol_accounts and node_community_version
        Map<String, ArrayList<String[]>> node_protocol_accounts = MergingCliAndSnmpAccounts(cli_node_account, node_community_version);
        logger.Println("New scanning nodes = "+node_protocol_accounts.size(), logger.DEBUG);        
        
        logger.Println("Start get information from nodes ...", logger.INFO);
        Map nodes_information = new HashMap();
        if(Neb.cfg.get("scripts") != null && ((Map)Neb.cfg.get("scripts")).get("get_info_node") != null) {
            Map<String, ArrayList<String>> scripts = (Map<String, ArrayList<String>>)((Map)Neb.cfg.get("scripts")).get("get_info_node");
            if(scripts.size() > 0 && node_protocol_accounts.size() > 0) {
                RunScriptsPool runScriptsPool = new RunScriptsPool(Neb.timeout_process, Neb.timeout_output, Neb.MAX_RUNSCRIPT_CLI);
                ArrayList<String> mas = runScriptsPool.Get(node_protocol_accounts, scripts, Neb.timeout, Neb.retries);
                logger.Println("Start information from nodes to JSON.", logger.DEBUG);
                JSONParser parser = new JSONParser();
                for(String it : mas) {
                    try {
//                        System.out.println(it);
                        JSONObject jsonObject = (JSONObject)parser.parse(it);
                        Map<String, Object> map = toMap(jsonObject);
                        for(Map.Entry<String, Object> entry : map.entrySet()) {
                            String node = entry.getKey();
                            Map val = (Map)entry.getValue();
                            nodes_information.put(node, val);
                        }
                    } catch (ParseException ex) {
                        if(DEBUG) System.out.println(ex);
                        ex.printStackTrace();
//                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }        
        logger.Println("Stop get information from nodes.", logger.INFO);
        ArrayList<String> networks = new ArrayList();
        networks.addAll(network_list);
        networks.addAll(ip_list);
        nodes_information = Uniqal_Nodes_Information(nodes_information, networks);
        
        Gson gson = new Gson();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet() ) {
            String str = gson.toJson(entry.getValue());            
            logger.Println("nodes_information="+entry.getKey()+" - "+str, logger.DEBUG);
//            System.out.println("nodes_information="+entry.getKey()+" - "+str);
        }
        
        result.put("nodes_information", nodes_information);
        
        Map<String, String> ip_from_nodes=GetIpFromNodes(nodes_information, net_list);
        exclude_list.putAll(ip_from_nodes);
        for(String ip : list_ip_ping) 
            exclude_list.put(ip, ip);
        result.put("exclude_list", exclude_list);
        
        // checking nodes not get information !!!
        for(Map.Entry<String, ArrayList<String[]>> entry : ((Map<String, ArrayList<String[]>>)node_protocol_accounts).entrySet()) {
            String node = entry.getKey();
            String node_base_ip = ip_from_nodes.get(node);
            if(node_base_ip == null) node_base_ip=node;
            boolean find = false;
            for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)nodes_information).entrySet() ) {
                String node1 = entry1.getKey();
                String node_base_ip1 = ip_from_nodes.get(node1);
                if(node_base_ip1 == null) node_base_ip=node1;
                if(node_base_ip.equals(node_base_ip1)) {
                    find = true;
                    break;
                }
            }
            if(!find) 
                logger.Println("Not access information from nodes - "+node_base_ip+" !!!", logger.DEBUG);
        }        
        
        // translate node_protocol_accounts Array to ArrayList
        Map<String, ArrayList<ArrayList<String>>> node_protocol_accounts_new = TranslateNodeProtocolAccountsToList(node_protocol_accounts);
        
        result.put("node_protocol_accounts", node_protocol_accounts_new);
        
        return result;
    }
    
    private Map<String, ArrayList<String[]>> MergingCliAndSnmpAccounts(ArrayList<String[]> cli_node_account, ArrayList<String[]> node_community_version) {
        
        // normalize node_community_version
        ArrayList<String[]> node_community_version_norm = new ArrayList();
        for(String[] mas : node_community_version) {
            String[] mas1 = new String[4];
            mas1[0]=mas[0];
            mas1[1]="snmp";
            mas1[2]=mas[1];
            mas1[3]=mas[2];
            node_community_version_norm.add(mas1);
        }         
        
        Map<String, ArrayList<String[]>> node_protocol_accounts = new HashMap();
        for(String[] mas : cli_node_account) {
            String[] mas_tmp = new String[4];
            mas_tmp[0]=mas[1]; mas_tmp[1]=mas[2];
            mas_tmp[2]=mas[3]; mas_tmp[3]=mas[4];
            ArrayList<String[]> list = node_protocol_accounts.get(mas[0]);
            if(list != null && list.size() > 0) {
                boolean find_protocol=false;
                for(String[] it : list) {
                    if(it[0].equals(mas_tmp[0])) {
                        find_protocol=true;
                        break;
                    }
                }
                if(!find_protocol) node_protocol_accounts.get(mas[0]).add(mas_tmp);
            } else {
                ArrayList list_tmp = new ArrayList();
                list_tmp.add(mas_tmp);
                node_protocol_accounts.put(mas[0], list_tmp);
            }
        }
        for(String[] mas : node_community_version_norm) {
            String[] mas_tmp = new String[3];
            mas_tmp[0]=mas[1]; mas_tmp[1]=mas[2];
            mas_tmp[2]=mas[3]; 
            ArrayList<String[]> list = node_protocol_accounts.get(mas[0]);
            if(list != null && list.size() > 0) {
                boolean find_protocol=false;
                for(String[] it : list) {
                    if(it[0].equals(mas_tmp[0])) {
                        find_protocol=true;
                        break;
                    }
                }
                if(!find_protocol) node_protocol_accounts.get(mas[0]).add(mas_tmp);
            } else {
                ArrayList list_tmp = new ArrayList();
                list_tmp.add(mas_tmp);
                node_protocol_accounts.put(mas[0], list_tmp);
            }            
        } 
        return node_protocol_accounts;
    }
    
    public void DeleteUnlinkedNodes() {
        Map<String, String[]> nodes_info_tmp = new HashMap();
        nodes_info_tmp.putAll(Neb.nodes_info);
        ArrayList<String[]> links_info_tmp = new ArrayList();
        links_info_tmp.addAll(Neb.links_info);
        
        // delete unlinked nodes
        for(Map.Entry<String, String[]> entry : nodes_info_tmp.entrySet()) {
            String node = entry.getKey();
            boolean find=false;
            for(String[] item : links_info_tmp) {
                if(node.equals(item[0]) || node.equals(item[3])) {
                    find=true;
                    break;
                }
            }
            if(!find) DeletedNode(node, false);
        } 
    }
    
//    public ArrayList IntegrationCheckingOne(Map<String, String> info_nodes, ArrayList<String[]> links) {
//        ArrayList result = new ArrayList();
////        Map<String, String> info_nodes_out = new HashMap();
//        ArrayList<String[]> links_out = new ArrayList();
//        Map<String, String> excluded_nodes_out = new HashMap();
//        
//        
////        // delete unlinked nodes
////        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
////            String node = entry.getKey();
////            boolean find=false;
////            for(String[] item : links) {
////                if(node.equals(item[0]) || node.equals(item[3])) {
////                    find=true;
////                    break;
////                }
////            }
////            if(find) info_nodes_out.put(node, entry.getValue());
////            else { 
////                excluded_nodes_out.put(node, entry.getValue());
////                logger.Println("Excluded unlinked node: "+node+";"+entry.getValue(), logger.DEBUG);
////            }
////        }
//        
//        // delete unused links
//        for(String[] item : links) {
//            boolean find1=false;
//            for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                String node = entry.getKey();
//                if(node.equals(item[0])) {
//                    find1=true;
//                    break;
//                }
//            }
//            boolean find2=false;
//            for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//                String node = entry.getKey();
//                if(node.equals(item[3])) {
//                    find2=true;
//                    break;
//                }
//            }            
//            if(find1 && find2) links_out.add(item);
//        }
//        
//        // check duplicate nodes am MAC address in node name
//        ArrayList<String> info_list = new ArrayList();
//        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            info_list.add(entry.getKey()+";"+entry.getValue());
//        }
//        ArrayList<String[]> node_replace = new ArrayList();
//        for(String item : info_list) {
//            String node = item.split(";", -1)[0];
//            Pattern p = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
//            Matcher m = p.matcher(node);
//            if(m.find()) {
//                String find_mac = node.substring(m.start(), m.end());
//                for(String item1 : info_list) {
//                    ArrayList<String> mac_list = GetMacAddressFromInfoNode(item1);
//                    boolean find=false;
//                    for(String mac : mac_list) {
//                        if(find_mac.equals(mac)) {
//                            find=true;
//                            String[] mas = new String[2];
//                            mas[0]=node; mas[1]=item1.split(";", -1)[0];
//                            if(!mas[1].equals("unknown_ip")) {
//                                logger.Println("Node: "+mas[0]+" need replaced to: "+mas[1], logger.DEBUG);
//                                node_replace.add(mas);
//                            }
//                            break;
//                        }
//                    }
//                    if(find) {
//                        break;
//                    }
//                }
//            }            
//        }
//        for(String[] item : node_replace) {
//            info_nodes.remove(item[0]);            
//            logger.Println("Remove duplicate node: "+item[0], logger.DEBUG);
//            for(int i=0; i<links_out.size(); i++) {
//                String[] link = links_out.get(i);
//                if(item[0].equals(link[0])) {
//                    String[] mas = new String[6];
//                    mas[0]=item[1]; mas[1]=link[1]; mas[2]=link[2];
//                    mas[3]=link[3]; mas[4]=link[4]; mas[5]=link[5];
//                    links_out.remove(i);
//                    i--;
//                    links_out.add(mas);
//                    logger.Println("Replace link from: "+link[0]+";"+link[1]+";"+link[2]+" <---> "+link[3]+";"+link[4]+";"+link[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//                }
//                if(item[0].equals(link[3])) {
//                    String[] mas = new String[6];
//                    mas[0]=link[0]; mas[1]=link[1]; mas[2]=link[2];
//                    mas[3]=item[1]; mas[4]=link[4]; mas[5]=link[5];
//                    links_out.remove(i);
//                    i--;
//                    links_out.add(mas);
//                    logger.Println("Replace link from: "+link[0]+";"+link[1]+";"+link[2]+" <---> "+link[3]+";"+link[4]+";"+link[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//                }                
//            }
//            
//        }
//        
//        
//        result.add(info_nodes);
//        result.add(links_out);
//        result.add(excluded_nodes_out);
//        
//        return result;
//    }
    
//    public ArrayList IntegrationCheckingTwo(Map<String, String> info_nodes, ArrayList<String[]> links, ArrayList<String[]> arp_mac, Map<String, ArrayList<String>> nodes_ip_address) {
//        ArrayList result = new ArrayList();
//        
//        // replace info_nodes
//        Map<String, String> info_nodes_out = new HashMap();
//        Map<String, String> replace = new HashMap();
//        for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
//            String node = entry.getKey();
//            Pattern p = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
//            Matcher m = p.matcher(node);
//            if(m.find()) {
//                String find_mac = node.substring(m.start(), m.end());
//                String ip = "";
//                for(String[] item : arp_mac) {
//                    if(find_mac.equals(item[0]) && !item[1].equals("unknown_ip")) {
//                        ip=item[1];
//                        break;
//                    }
//                }
//                if(!ip.equals("")) {
//                    replace.put(node, ip);
//                    info_nodes_out.put(ip, entry.getValue());
//                    logger.Println("Replace node from: "+node+" to: "+ip, logger.DEBUG);                    
//                } else {
//                    info_nodes_out.put(node, entry.getValue());
//                }
//            } else info_nodes_out.put(node, entry.getValue());
//        }
//        
//        // replace links
//        ArrayList<String[]> links_out = new ArrayList();
//        for(String[] item : links) {
//            if(replace.get(item[0]) != null) {
//                String[] mas = new String[6];
//                mas[0]=replace.get(item[0]); mas[1]=item[1]; mas[2]=item[2];
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                links_out.add(mas);
//                logger.Println("Replace link from: "+item[0]+";"+item[1]+";"+item[2]+" <---> "+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//            if(replace.get(item[3]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
//                mas[3]=replace.get(item[3]); mas[4]=item[4]; mas[5]=item[5];
//                links_out.add(mas);
//                logger.Println("Replace link from: "+item[0]+";"+item[1]+";"+item[2]+" <---> "+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }   
//            if(replace.get(item[0]) == null && replace.get(item[3]) == null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2];
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                links_out.add(mas);                
//            }
//        }
//        
//        // replace hosts
//        ArrayList<String[]> arp_mac_tmp = new ArrayList();
//        for(String[] item : arp_mac) {
//            if(replace.get(item[1]) != null && replace.get(item[2]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=replace.get(item[1]); mas[2]=replace.get(item[2]);
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_tmp.add(mas);
//                logger.Println("Replace hosts record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//            else if(replace.get(item[1]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=replace.get(item[1]); mas[2]=item[2];
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_tmp.add(mas);
//                logger.Println("Replace hosts record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//            else if(replace.get(item[2]) != null) {
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=replace.get(item[2]);
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_tmp.add(mas);
//                logger.Println("Replace hosts record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            } else arp_mac_tmp.add(item);
//        }
//        
//        // delete taftology in hosts
//        ArrayList<String[]> arp_mac_out = new ArrayList();
//        for(String[] item : arp_mac_tmp) {
//            Pattern p = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
//            Pattern p1 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
//            
//            Matcher m = p.matcher(item[0]);
//            String mac="";
//            if(m.find()) mac=m.group(1);
//            
//            Matcher m1 = p.matcher(item[2]);
//            String mac1="";
//            if(m1.find()) mac1=m1.group(1);
//            
//            Matcher m2 = p1.matcher(item[1]);
//            String ip="";
//            if(m2.find()) ip=m2.group(1);    
//            
//            Matcher m3 = p1.matcher(item[2]);
//            String ip1="";
//            if(m3.find()) ip1=m3.group(1);              
//            
//            if( !(!mac.equals("") && !mac1.equals("") && mac.equals(mac1) &&
//               !ip.equals("") && !ip1.equals("") && ip.equals(ip1))    
//                    ) arp_mac_out.add(item);
//            else logger.Println("Delete taftology in hosts: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5], logger.DEBUG);
//        }
//        
//        //replace ip aliases in arp_mac_out
//        Map<String, String> replace1 = new HashMap();
//        for(Map.Entry<String, ArrayList<String>> entry : nodes_ip_address.entrySet()) {
//            for(String item : entry.getValue()) {
//                replace1.put(item, entry.getKey());
//                logger.Println("replace1: "+item+" ===> "+entry.getKey(), logger.DEBUG);
//            }
//        }
//        
//        for(int i=0; i<arp_mac_out.size(); i++) {
//            String[] item = arp_mac_out.get(i);
//            if(replace1.get(item[2]) != null && !replace1.get(item[2]).equals(item[2])) {
//                arp_mac_out.remove(i);
//                i--;
//                String[] mas = new String[6];
//                mas[0]=item[0]; mas[1]=item[1]; mas[2]=replace1.get(item[2]);
//                mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
//                arp_mac_out.add(mas);
//                logger.Println("Replace arp_mac_out record from: "+item[0]+";"+item[1]+";"+item[2]+";"+item[3]+";"+item[4]+";"+item[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
//            }
//        }
//        
//        result.add(info_nodes_out);
//        result.add(links_out);
//        result.add(arp_mac_out);
//        
//        return result;
//    }
    
//    private ArrayList<String> GetMacAddressFromInfoNode(String info_node) {
//        ArrayList<String> result = new ArrayList();
//        String[] mas = info_node.split(";", -1);
//        if(mas.length == 11) {
//            if(mas[7].matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) result.add(mas[7]);
//            String[] mas1 = mas[8].split("\\|", -1);
//            for(String item : mas1) {
//                String[] mas2 = item.split(",", -1);
//                if(mas2.length > 6) {
//                    String mac = mas2[5];
//                    if(mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) result.add(mac);
//                }
//            }
//        }
//        return result;
//    }
    
    public ArrayList Nodes_Links_Summary(ArrayList informationFromNodes, Map<String, Map<String, ArrayList>> walkInformationFromNodes, ArrayList<String[]> arp_mac) {
        ArrayList result = new ArrayList();
        
        Map<String, String> nodes_info = (Map<String, String>)informationFromNodes.get(0);
        ArrayList<String[]> links = (ArrayList<String[]>)informationFromNodes.get(1);
        Map<String, Map<String, ArrayList>> walks_info = (Map<String, Map<String, ArrayList>>)informationFromNodes.get(2);
        ArrayList<String> exclude_list = (ArrayList<String>)informationFromNodes.get(3);
//        ArrayList<String[]> node_community_version = (ArrayList<String[]>)informationFromNodes.get(4);
        
        // node ip address to map
        Map<String, String> ip_ip = new HashMap();
        for(Map.Entry<String, String> entry : nodes_info.entrySet()) {
            String node = entry.getKey();
            ArrayList<String> list_ip = GetIpAddressFromNode(walkInformationFromNodes, node);
            for(String item : list_ip) {
                if(item.equals("127.0.0.1")) ip_ip.put("127.0.0.1", "127.0.0.1");
                else ip_ip.put(item, list_ip.get(0));
            }
        }
        
        // delete duplicate nodes and adding to ip_ip
        Map<String, String> nodes_info_new = new HashMap();
        for(Map.Entry<String, String> entry : nodes_info.entrySet()) {
            String node = entry.getKey();
            String tmp = ip_ip.get(entry.getKey());
            if(tmp != null) node=tmp;
            else ip_ip.put(node, node);
            if(!node.equals("127.0.0.1")) {
                String str_node_info_new = nodes_info_new.get(node);
                if(str_node_info_new != null) {
                    if(str_node_info_new.split(";", -1)[0].split("\\|", -1).length == 1) {
                        if(entry.getValue().split(";", -1)[0].split("\\|", -1).length > 1) {
                            nodes_info_new.remove(node);
                            nodes_info_new.put(node, entry.getValue());
                            logger.Println("Rename duplicate node: "+node+";"+str_node_info_new+" -  rename to node: "+entry.getKey()+";"+entry.getValue(), logger.DEBUG);
                        } else logger.Println("Duplicate node: "+entry.getKey()+";"+entry.getValue()+" - exist node: "+node+";"+str_node_info_new, logger.DEBUG);
                    } else logger.Println("Duplicate node: "+entry.getKey()+";"+entry.getValue()+" - exist node: "+node+";"+str_node_info_new, logger.DEBUG);
                } else nodes_info_new.put(node, entry.getValue());
            }
        }

        // rename and delete unused links
        ArrayList<String[]> links_new = new ArrayList();
        ArrayList<String[]> excluded_links = new ArrayList();
        for(String[] item : links) {
            if(item.length == 8) {
                String aliase1=ip_ip.get(item[0]);
                String aliase2=ip_ip.get(item[3]);

                if(aliase1 != null && aliase2 != null) {
                        if(!item[0].equals(aliase1) || !item[3].equals(aliase2)) {
                            logger.Println("Rename link: "+item[0]+" "+item[2]+" <---> "+item[3]+" "+item[5]+" to: "+aliase1+" "+item[2]+" <---> "+aliase2+" "+item[5], logger.DEBUG);
                        }                        

                        String[] mas = new String[8];
                        mas[0]=aliase1; mas[1]=item[1]; mas[2]=item[2];
                        mas[3]=aliase2; mas[4]=item[4]; mas[5]=item[5];
                        mas[6]=item[6]; mas[7]=item[7];
                        links_new.add(mas);
                } else {
                    excluded_links.add(item);
                }
            } else {
                LoggingArray(item, "Delete link from Nodes_Links_Summary", logger.DEBUG);
            }
        }
        
        // rename and delete walks_info
        Map<String, Map<String, ArrayList>> walks_info_new = new HashMap();
        for (Map.Entry<String, Map<String, ArrayList>> entry : walks_info.entrySet()) {
            String key = entry.getKey();
            Map<String, ArrayList> val = entry.getValue();
            Map<String, ArrayList> map_tmp = new HashMap();
            for (Map.Entry<String, ArrayList> entry1 : val.entrySet()) {
                String node = ip_ip.get(entry1.getKey());
                ArrayList list = entry1.getValue();
                if(node != null) {
                    map_tmp.put(node, list);
                }
            }
            walks_info_new.put(key, map_tmp);
        }
        
        // make node_community_version from nodes_info_new
        ArrayList<String[]> node_community_version = new ArrayList();
        for(Map.Entry<String, String> entry : nodes_info_new.entrySet()) {
            String node = entry.getKey();
            String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
            if(mas.length == 3) {
                String[] mas1 = new String[3];
                mas1[0]=node; mas1[1]=mas[1]; mas1[2]=mas[2];
                node_community_version.add(mas1);
            }
        }
        
        // rename arp_mac
        ArrayList arp_mac_new = new ArrayList();
        for(String[] item : arp_mac) {
            String node = ip_ip.get(item[0]);
            if(node != null) {
                if(!item[0].equals(node)) {
                    logger.Println("Rename arp_mac: "+item[0]+" --- "+node, logger.DEBUG);
                }
                String[] mas = new String[5];
                mas[0]=node; mas[1]=item[1]; mas[2]=item[2];
                mas[3]=item[3]; mas[4]=item[4];
                arp_mac_new.add(mas);
            }
        }
        
        result.add(nodes_info_new);
        result.add(links_new);
        result.add(walks_info_new);
        result.add(exclude_list);
        result.add(node_community_version);
        result.add(arp_mac_new);
        result.add(excluded_links);
        
        return result;
    }
    
    public int Num_Community_node(ArrayList informationFromNodes) {
        int community_nodes=0;
        if(informationFromNodes != null && informationFromNodes.size() > 0) {
            Map<String, String> map = (Map<String, String>)informationFromNodes.get(0);
            for(Map.Entry<String, String> entry : map.entrySet()) {
                String node = entry.getKey();
                if(node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
                    if(mas.length == 3) {
                        community_nodes++;
                    }
                }
            } 
        }
            
       return community_nodes;
    }
    
    public int Num_Community_node(Map<String, String> info_nodes) {
        int community_nodes=0;
        if(info_nodes != null) {
            Map<String, String> map = info_nodes;
            for(Map.Entry<String, String> entry : map.entrySet()) {
                String node = entry.getKey();
                if(node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
                    if(mas.length == 3) {
                        community_nodes++;
                    }
                }
            }   
        }
            
       return community_nodes;
    }    
    
    public ArrayList<String[]> Get_Community_Nodes_Version(Map<String, String> info_nodes) {
        ArrayList<String[]> result = new ArrayList();
        if(info_nodes != null) {
            for(Map.Entry<String, String> entry : info_nodes.entrySet()) {
                String node = entry.getKey();
//                System.out.println("Get_Community_Nodes_Version: "+node+";"+entry.getValue());
                if(node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                    String[] mas = entry.getValue().split(";", -1)[0].split("\\|");
                    if(mas.length == 3) {
                        String[] mas1 = new String[3];
                        mas1[0]=node; mas1[1]=mas[1]; mas1[2]=mas[2];
                        result.add(mas1);
                    }
                }
            } 
        }
       return result;
    } 
    
//    public ArrayList<String[]> InspectLinks(ArrayList<String[]> links, Map<String, Map<String, ArrayList>> walkInformationFromNodes) {
//        ArrayList<String[]> result = new ArrayList();
//        
//        logger.Println("Checking consistent link.", logger.DEBUG);
//        // rename id_iface and name _iface
//        Map<String, ArrayList<String>> hash_ip = GetIpAddress(walkInformationFromNodes);
//        for(String[] link : links) {
//            String[] mas1 = GetRemotePort(link[3], link[4], link[5], walkInformationFromNodes, hash_ip);
//            if(!mas1[0].equals(link[4]) || !mas1[1].equals(link[5])) {
//                logger.Println("Rename id or name link: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+mas1[0]+","+mas1[1]+" from: "+link[0]+","+link[1]+","+link[2]+" <---> "+link[3]+","+link[4]+","+link[5], logger.DEBUG);
//            }
//            String[] mas = new String[6];
//            mas[0]=link[0]; mas[1]=link[1]; mas[2]=link[2];
//            mas[3]=link[3];
//            mas[4]=mas1[0];
//            mas[5]=mas1[1];
//            result.add(mas);
//        }
//        
//        //delete duplicate link
//        ArrayList<String[]> result1 = new ArrayList();
//        for(int i=0; i<result.size(); i++) {
//            String[] link1=result.get(i);
//            boolean find=false;
//            for(int j=i+1; j<result.size(); j++) {
//                String[] link2=result.get(j);
//                if(
//                    (link1[0].equals(link2[0]) && link1[3].equals(link2[3])) && ( CheckInterfaceName(link1[2],link2[2]) || CheckInterfaceName(link1[5],link2[5]) ) ||
//                    (link1[3].equals(link2[0]) && link1[0].equals(link2[3])) && ( CheckInterfaceName(link1[2],link2[5]) || CheckInterfaceName(link1[5],link2[2]) )    
//                  ) {
//                    find=true;
//                    break;
//                }
//            }
//            if(!find) {
//                result1.add(link1);
//                logger.Println("Link added: "+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//            }
//            else logger.Println("Link duplicate: "+link1[0]+","+link1[1]+","+link1[2]+" <---> "+link1[3]+","+link1[4]+","+link1[5], logger.DEBUG);
//        }
//        
//        return result1;
//    }
    
    public void LoggingArray(String[] mas, String prefix, int level) {
        logger.Print(new Date()+":\t"+prefix+": ", level);
        logger.Print(mas[0], level);
        for(int i=1; i<mas.length; i++) logger.Print(", "+mas[i], level);
        logger.Print("\n", level);
    }
    
//    public void GetInformationFromMapFile(String mapfile) {
//        ArrayList<String[]> nodes = ReadFromMapFile(mapfile, "^:nodes:$");
//        synchronized(Neb.links_info) { Neb.links_info.clear(); Neb.links_info = ReadFromMapFile(mapfile, "^:links:$"); }
//        ArrayList<String[]> hosts = ReadFromMapFile(mapfile, "^:hosts:$");
//        ArrayList<String[]> texts = ReadFromMapFile(mapfile, "^:texts:$");
//        ArrayList<String[]> custom_texts = ReadFromMapFile(mapfile, "^:custom_texts:$"); 
//        ArrayList<String[]> extend_info = ReadFromMapFile(mapfile, "^:extend_info:$");
//
//        synchronized(Neb.nodes_info) { Neb.nodes_info.clear(); for(String[] item : nodes) Neb.nodes_info.put(item[0], item); }
//        synchronized(Neb.mac_ArpMacTable) { Neb.mac_ArpMacTable.clear(); for(String[] item : hosts) Neb.mac_ArpMacTable.put(item[0], item); }
//        synchronized(Neb.ip_ArpMacTable) { Neb.ip_ArpMacTable.clear(); for(String[] item : hosts) Neb.ip_ArpMacTable.put(item[1], item); }
//        synchronized(Neb.text_info) { Neb.text_info.clear(); for(String[] item : texts) Neb.text_info.put(item[0], item); }
//        synchronized(Neb.text_custom_info) { Neb.text_custom_info.clear(); for(String[] item : custom_texts) Neb.text_custom_info.put(item[0], item); }
//        synchronized(Neb.extended_info) { Neb.extended_info.clear(); for(String[] item : extend_info) Neb.extended_info.put(item[0], item); }
//    }
    
//    public boolean WriteInformationToMapFile(String filename, Map<String, String[]> info_nodes, ArrayList<String[]> links, Map<String, String[]> ARP_MAC, Map<String, String[]> text, Map<String, String[]> text_custom, Map<String, String[]> extended_info) {
//        BufferedWriter outFile = null;
//        BufferedReader inFile = null;
//        File file = new File(filename);
//        try {
//            outFile = new BufferedWriter(new FileWriter(filename+".temp"));
//            outFile.write(":nodes:\n");
//            for(Map.Entry<String, String[]> entry : info_nodes.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }
//            outFile.write(":links:\n");
//            for(String[] item : links) {
//                outFile.write(item[0]);
//                for(int i=1; i<item.length; i++) outFile.write(";"+item[i]);
//                outFile.write("\n");
//            }
//            outFile.write(":hosts:\n");
//            for(Map.Entry<String, String[]> entry : ARP_MAC.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }            
//            //extended information
//            outFile.write(":extend_info:\n");
//            for(Map.Entry<String, String[]> entry : extended_info.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }              
//            outFile.write(":text:\n");
//            for(Map.Entry<String, String[]> entry : text.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }               
//            outFile.write(":custom_texts:\n");
//            for(Map.Entry<String, String[]> entry : text_custom.entrySet()) {
//                outFile.write(entry.getValue()[0]);
//                for(int i=1; i<entry.getValue().length; i++) outFile.write(";"+entry.getValue()[i]);
//                outFile.write("\n");
//            }               
//
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { 
//                if(DEBUG) System.out.println(ex);
//                Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); 
//            }
//            
//            File file_tmp = new File(filename+".temp");
//            if(file_tmp.exists()) {
//                file.delete();
//                file_tmp.renameTo(file);
//            }
//            
//        } catch (Exception ex) {
//            if(DEBUG) System.out.println(ex);
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//            return false;
//        } finally {
//            if(outFile != null) try {
//                outFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }
//            if(inFile != null) try {
//                inFile.close();
//            } catch (IOException ex) { Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex); }            
//        }
//
//        return true;
//    }    

    public void RunScripts(String cmd) {
        try {
            if (new File(cmd).exists()) {
                Process process = new ProcessBuilder(cmd,"").start();
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    logger.Println("Scripts: "+line, logger.DEBUG);
                }
            } else logger.Println("Scripts "+cmd+" not found!!!", logger.DEBUG);
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    public boolean WriteStrToFile(String filename, String str) {
        try {
            Writer outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
//            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            outFile.write(str);
            outFile.close();
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }             
    }
    
    public boolean AppendStrToFile(String filename, String str) {
        try {
            Writer outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), "UTF-8"));
//            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            outFile.write(str);
            outFile.close();
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }             
    }    
    
    public boolean WriteArrayListToFile(String filename, ArrayList<String[]> list) {
        try {
            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            for(String[] item : list) {
                outFile.write(item[0]);
                for(int ii=1; ii<item.length; ii++) outFile.write(","+item[ii]);
                outFile.write("\n");
            }
            outFile.close();
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }             
    }

    public boolean WriteArrayListToFile1(String filename, ArrayList<ArrayList<String>> list) {
        try {
            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            for(ArrayList<String> item : list) {
                outFile.write(item.get(0));
                for(int ii=1; ii<item.size(); ii++) outFile.write(","+item.get(ii));
                outFile.write("\n");
            }
            outFile.close();
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }             
    }
    
    public boolean WriteHashMapToFile(String filename, Map<String, String> map) {
        try {
            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String node = entry.getKey();
                String val = entry.getValue();            
                outFile.write(node+";"+val+"\n");
            }
            outFile.close();  
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }              
    }
    
    public Map NormalizeMap(Map<String, Map> info_map, Map<String, ArrayList<String[]>> area_arp_mac_table) {
        try {
            // area_arp_mac_table to area_mac_ip and area_ip_mac
            Map<String, Map<String, String>> area_mac_ip = new HashMap();
            Map<String, Map<String, String>> area_ip_mac = new HashMap();
            for (Map.Entry<String, ArrayList<String[]>> area : area_arp_mac_table.entrySet()) {
                String area_name = area.getKey();
                ArrayList<String[]> area_info = area.getValue();
                Map<String, String> mac_map = new HashMap();
                Map<String, String> ip_map = new HashMap();
                for(String[] item : area_info) {
                    mac_map.put(item[4].replaceAll("[:.-]", "").toLowerCase(), item[3]);
                    ip_map.put(item[3], item[4]);
                }
                area_mac_ip.put(area_name, mac_map);
                area_ip_mac.put(area_name, ip_map);
            }
            
            // replace node formats CS2660...(ab:cd:ef:12:34:56) to ip address
            for (Map.Entry<String, Map> area : ((Map<String, Map>) info_map).entrySet()) {
                String area_name = area.getKey();
//                if(area_name.equals("area_chermk"))
                System.out.println(area_name);                
                Map area_info = area.getValue();
                ArrayList<String[]> mac_ip_port = (ArrayList<String[]>)area_info.get("mac_ip_port");
                ArrayList<String[]> mac_ip_port_normalize = new ArrayList();
                if(mac_ip_port != null) {
                    for(String[] host_it : mac_ip_port) {
                        String[] tmp = new String[host_it.length];
                        String mac_frombase = host_it[0].replaceAll("[:.-]", "").toLowerCase();
                        tmp[0] = mac_frombase;
                        for(int i=1; i<host_it.length; i++) {
                            tmp[i] = host_it[i];
                        }
                        mac_ip_port_normalize.add(tmp);
                    }
                }
                Map<String, Map> nodes_info = (Map<String, Map>)area_info.get("nodes_information"); 
                if(nodes_info != null) {
                    ArrayList<String[]> replace_nodes = new ArrayList();

                    //remove node equiv "" and "unknown_ip"
                    Map<String, Map> nodes_info_new = new HashMap();
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        if(!node.equals("") && !node.equals("unknown_ip")) nodes_info_new.put(node, node_info);
                    }
                    nodes_info = nodes_info_new;
                    
                    //replace symbol "/" to "_" in node name
                    nodes_info_new = new HashMap();
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey().replace("/", "_");
                        Map<String, Map> node_info = node_it.getValue();
                        if(!node.equals("")) nodes_info_new.put(node, node_info);
                    }
                    nodes_info = nodes_info_new;                    

                    // correct node information
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        if(node_info.get("general") != null && node_info.get("general").get("base_address") != null) {
                            String base_address = (String)node_info.get("general").get("base_address");
//                            if(base_address == null || base_address.equals("0"))
//                                System.out.println("11111");
                            if(base_address == null || !(base_address.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") || base_address.matches("^([0-9A-Fa-f]{4}[\\.]){2}([0-9A-Fa-f]{4})$"))) {
                                if(node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                    if(area_ip_mac.get(area_name) != null && area_ip_mac.get(area_name).get(node) != null) {
                                        String base_address_old = base_address;
                                        base_address = area_ip_mac.get(area_name).get(node);
                                        node_info.get("general").put("base_address", base_address);
                                        logger.Println("Replace base_address for node: "+node+" - "+base_address_old+"/"+base_address, logger.DEBUG);
                                    }
                                        
                                }
                            }                         
                        }
                    }                   
                    
                    //remove node if node equals sysname other node
                    Map<String, ArrayList<String>> sysname_node = new HashMap();
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        if(node_info.get("general") != null && node_info.get("general").get("sysname") != null) {
                            String sysname = (String)(node_info.get("general").get("sysname"));
                            sysname = sysname.split("\\.")[0];
                            if(sysname_node.get(sysname) == null) {
                                ArrayList<String> tmp_list = new ArrayList();
                                tmp_list.add(node);
                                sysname_node.put(sysname, tmp_list);
                            }
                            else {
                                sysname_node.get(sysname).add(node);
                            }
                        }
                    } 

                    ArrayList<String[]> nodes_to_nodes_for_mac_ip_port = new ArrayList();
                    for (Map.Entry<String, ArrayList<String>> it : ((Map<String, ArrayList<String>>)sysname_node).entrySet()) {
                        String sysname = it.getKey();
                        ArrayList<String> val = it.getValue();
                        if(val.size() == 2) {
                            if(val.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && !val.get(1).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String[] tmp = new String[2];
                                tmp[0] = val.get(1);
                                replace_nodes.add(tmp);  
                                String[] tmp1 = new String[2];
                                tmp1[0] = val.get(1);
                                tmp1[1] = val.get(0);
                                nodes_to_nodes_for_mac_ip_port.add(tmp1);
                            }
                            if(!val.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+") && val.get(1).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String[] tmp = new String[2];
                                tmp[0] = val.get(0);
                                replace_nodes.add(tmp);
                                String[] tmp1 = new String[2];
                                tmp1[0] = val.get(0);
                                tmp1[1] = val.get(1);
                                nodes_to_nodes_for_mac_ip_port.add(tmp1);                            
                            }                        

                        }
                    }           

                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();

                        // resolve nodes format CS2960...(12:34:56:78:0a:bc) and remove duplicate nodes
                        Pattern p = Pattern.compile("^(.*)\\((.*)\\)$");
                        Matcher m = p.matcher(node);
                        if(m.find()) {
                            String sysname = m.group(1);
                            if(!sysname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                String mac = m.group(2).replaceAll("[:.-]", "").toLowerCase();
                                String ip = null;
                                if(mac_ip_port_normalize != null) {
                                    if(mac != null) {
                                        for(String[] host_it : mac_ip_port_normalize) {
                                            if(host_it[0].equals(mac)) {
                                                if(!host_it[1].equals("unknown_ip")) {
                                                    ip = host_it[1];
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    if(ip == null && sysname != null && !sysname.equals("")) {
                                        InetAddress address;
                                        try {
                                            sysname = sysname.split("\\.")[0];
                                            address = InetAddress.getByName(sysname);
                                            ip = address.getHostAddress();
                                        } catch (UnknownHostException ex) {}                               
                                    }
                                }
                                if(ip != null) {
                                    String[] tmp = new String[2];
                                    tmp[0] = node; tmp[1] = ip;
                                    replace_nodes.add(tmp);                                
                                    String[] tmp1 = new String[2];
                                    tmp1[0] = node;
                                    tmp1[1] = ip;
                                    nodes_to_nodes_for_mac_ip_port.add(tmp1);                            
                                }
                            }

                        }  

                    }
                    
                    // find in node and sysname  name ip address.
//                    Pattern p_ip = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
//                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
//                        String node = node_it.getKey();
//                        Map<String, Map> node_info = node_it.getValue();
//                        if(!node.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                            Matcher m_ip = p_ip.matcher(node);
//                            if(m_ip.find()) { 
//                                String ip=m_ip.group(1);
//                                String[] tmp = new String[2];
//                                tmp[0] = node; tmp[1] = ip;
//                                replace_nodes.add(tmp);                                
//                                String[] tmp1 = new String[2];
//                                tmp1[0] = node;
//                                tmp1[1] = ip;
//                                nodes_to_nodes_for_mac_ip_port.add(tmp1);                             
//                            } else {
//                                if(node_info.get("general") != null && node_info.get("general").get("sysname") != null) {
//                                    String sysname = (String)(node_info.get("general").get("sysname"));
//                                    Matcher m_ip1 = p_ip.matcher(sysname);
//                                    if(m_ip1.find()) { 
//                                        String ip=m_ip1.group(1);
//                                        String[] tmp = new String[2];
//                                        tmp[0] = node; tmp[1] = ip;
//                                        replace_nodes.add(tmp);                                
//                                        String[] tmp1 = new String[2];
//                                        tmp1[0] = node;
//                                        tmp1[1] = ip;
//                                        nodes_to_nodes_for_mac_ip_port.add(tmp1);                                
//                                    }
//                                }
//                            }
//                        }
//                    }
                    
                    // find in node and sysname name mac address.
                    Pattern p1 = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
                    Pattern p2 = Pattern.compile("(([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4}))");
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        Matcher m_mac1 = p1.matcher(node);
                        Matcher m_mac_sysname1 = null;
                        if(node_info.get("general") != null && node_info.get("general").get("sysname") != null) {
                            String sysname = (String)(node_info.get("general").get("sysname"));
                            m_mac_sysname1 = p1.matcher(sysname);
                        }
                        Matcher m_mac2 = p2.matcher(node);
                        Matcher m_mac_sysname2 = null;
                        if(node_info.get("general") != null && node_info.get("general").get("sysname") != null) {
                            String sysname = (String)(node_info.get("general").get("sysname"));
                            m_mac_sysname2 = p2.matcher(sysname);
                        }                        
                        
                        if(m_mac1.find()) { 
                            String mac=m_mac1.group(1).replaceAll("[:.-]", "").toLowerCase();
                            if(area_mac_ip.get(area_name) != null && area_mac_ip.get(area_name).get(mac) != null) {
                                String ip = area_mac_ip.get(area_name).get(mac);
                                if(!ip.equals("unknown_ip")) {
                                    String[] tmp = new String[2];
                                    tmp[0] = node; tmp[1] = ip;
                                    replace_nodes.add(tmp);                                
                                    String[] tmp1 = new String[2];
                                    tmp1[0] = node;
                                    tmp1[1] = ip;
                                    nodes_to_nodes_for_mac_ip_port.add(tmp1);
                                }
                            }
                        } else if(m_mac_sysname1 != null && m_mac_sysname1.find()) { 
                            String mac=m_mac_sysname1.group(1).replaceAll("[:.-]", "").toLowerCase();
                            if(area_mac_ip.get(area_name) != null && area_mac_ip.get(area_name).get(mac) != null) {
                                String ip = area_mac_ip.get(area_name).get(mac); 
                                if(!ip.equals("unknown_ip")) {
                                    String[] tmp = new String[2];
                                    tmp[0] = node; tmp[1] = ip;
                                    replace_nodes.add(tmp);                                
                                    String[] tmp1 = new String[2];
                                    tmp1[0] = node;
                                    tmp1[1] = ip;
                                    nodes_to_nodes_for_mac_ip_port.add(tmp1);
                                }
                            }
                        } else if(m_mac2.find()) { 
                            String mac=m_mac2.group(1).replaceAll("[:.-]", "").toLowerCase();
                            if(area_mac_ip.get(area_name) != null && area_mac_ip.get(area_name).get(mac) != null) {
                                String ip = area_mac_ip.get(area_name).get(mac);
                                if(!ip.equals("unknown_ip")) {
                                    String[] tmp = new String[2];
                                    tmp[0] = node; tmp[1] = ip;
                                    replace_nodes.add(tmp);                                
                                    String[] tmp1 = new String[2];
                                    tmp1[0] = node;
                                    tmp1[1] = ip;
                                    nodes_to_nodes_for_mac_ip_port.add(tmp1); 
                                }
                            }
                        } else if(m_mac_sysname2 != null && m_mac_sysname2.find()) { 
                            String mac=m_mac_sysname2.group(1).replaceAll("[:.-]", "").toLowerCase();
                            if(area_mac_ip.get(area_name) != null && area_mac_ip.get(area_name).get(mac) != null) {
                                String ip = area_mac_ip.get(area_name).get(mac);
                                if(!ip.equals("unknown_ip")) {
                                    String[] tmp = new String[2];
                                    tmp[0] = node; tmp[1] = ip;
                                    replace_nodes.add(tmp);                                
                                    String[] tmp1 = new String[2];
                                    tmp1[0] = node;
                                    tmp1[1] = ip;
                                    nodes_to_nodes_for_mac_ip_port.add(tmp1); 
                                }
                            }
                        }
                    }                   
                    
                    // replace and remove duplicate nodes
                    for(String[] replace_node : replace_nodes) {
                        Map<String, Map> node_info = nodes_info.get(replace_node[0]);
                        Map<String, Map> node_info1 = nodes_info.get(replace_node[1]);
                        if(replace_node[1] != null) {
//                            if(replace_node[1].equals("10.56.110.2"))
//                                System.out.println(replace_node[1]);
                            nodes_info.remove(replace_node[0]);
                            if(node_info1 != null)
                                nodes_info.put(replace_node[1], node_info1);
                            else if(node_info != null)
                                nodes_info.put(replace_node[1], node_info);
                            logger.Println("Replace node: "+replace_node[0]+" to: "+replace_node[1], logger.DEBUG);
                        } else {
                            nodes_info.remove(replace_node[0]);
                            logger.Println("Remove duplicate node: "+replace_node[0], logger.DEBUG);
                        }
                    }
        
                    // replace links from replace_nodes
                    ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>)area_info.get("links"); 
                    ArrayList<ArrayList<String>> links_new = new ArrayList();
                    for(ArrayList<String> link : links) {
                        String node1 = link.get(0);
                        String node2 = link.get(3);
                        String replace1 = null; String replace2 = null;
                        boolean remove_link = false;
                        for(String[] replace_node : replace_nodes) {
                            if(replace_node[0].equals(node1)) {
                                if(replace_node[1] != null) {
                                    replace1 = replace_node[1];
                                } else {
                                    remove_link = true;
                                }
                                break;
                            }

                        }
                        for(String[] replace_node : replace_nodes) {
                            if(replace_node[0].equals(node2)) {
                                if(replace_node[1] != null) {
                                    replace2 = replace_node[1];
                                } else {
                                    remove_link = true;                          
                                }
                                break;
                            }
                        }  

                        if(!remove_link) {
                            String node1_prev = link.get(0);
                            String node2_prev = link.get(3);
                            if(replace1 != null) link.set(0, replace1);
                            if(replace2 != null) link.set(3, replace2);
                            links_new.add(link);
                            if(replace1 != null || replace2 != null)
                                logger.Println("Replace link: "+node1_prev+" "+link.get(2)+" <---> "+node2_prev+" "+link.get(5)+"   to:   "+link.get(0)+" "+link.get(2)+" <---> "+link.get(3)+" "+link.get(5), logger.DEBUG);
                        } else {
                            logger.Println("Remove link: "+link.get(0)+" "+link.get(2)+" <---> "+link.get(3)+" "+link.get(5), logger.DEBUG);
                        }

                    }
                    links = links_new;

                    // remove duplicate links
                    for(int i=0; i<links.size(); i++) {
                        ArrayList<String> link=links.get(i);
                        String node1 = link.get(0);
                        String iface1 = link.get(2);
                        String node2 = link.get(3);
                        String iface2 = link.get(5); 
//                        boolean find = false;
                        for(int j = i+1; j<links.size(); j++) {
                            String node3 = links.get(j).get(0);
                            String iface3 = links.get(j).get(2);
                            String node4 = links.get(j).get(3);
                            String iface4 = links.get(j).get(5);                            
                            if(
                                    (node1.equals(node3) && EqualsIfaceName(iface1, iface3) && node2.equals(node4)) ||  
                                    (node1.equals(node4) && EqualsIfaceName(iface1, iface4) && node2.equals(node3)) ||
                                    (node1.equals(node3) && node2.equals(node4) && EqualsIfaceName(iface2, iface4)) ||  
                                    (node1.equals(node4) && node2.equals(node3) && EqualsIfaceName(iface2, iface3))
                              ) {
                                links.remove(j);
                                j--;
                                logger.Println("Normalize map Link: "+node1+" "+iface1+" <---> "+node2+" "+iface2+" duplicate to link:"+node3+" "+iface3+" <---> "+node4+" "+iface4, logger.DEBUG);
                                logger.Println("Remove Link: "+node3+" "+iface3+" <---> "+node4+" "+iface4, logger.DEBUG);
                            } else if(iface2.equals("unknown")) {
                                if(
                                    (node1.equals(node3) && EqualsIfaceName(iface1, iface3) && node2.equals(node4)) ||  
                                    (node1.equals(node4) && EqualsIfaceName(iface1, iface4) && node2.equals(node3))
                                ) {
                                    ArrayList<String> item = new ArrayList();
                                    for(String field : links.get(j))
                                        item.add(field);
                                    links.set(i, item);
                                    logger.Println("Normalize map Link: "+node1+" "+iface1+" <---> "+node2+" "+iface2+" duplicate to link:"+node3+" "+iface3+" <---> "+node4+" "+iface4, logger.DEBUG);
                                    logger.Println("Replace Link: "+node1+" "+iface1+" <---> "+node2+" "+iface2+" to link:"+node3+" "+iface3+" <---> "+node4+" "+iface4, logger.DEBUG);
                                }  
                            } else if(iface4.equals("unknown")) {
                                if(
                                    (node1.equals(node3) && EqualsIfaceName(iface1, iface3) && node2.equals(node4)) ||  
                                    (node1.equals(node4) && node2.equals(node3) && EqualsIfaceName(iface2, iface3))
                                ) {
                                    links.remove(j);
                                    j--;
                                    logger.Println("Normalize map Link: "+node1+" "+iface1+" <---> "+node2+" "+iface2+" duplicate to link:"+node3+" "+iface3+" <---> "+node4+" "+iface4, logger.DEBUG);
                                    logger.Println("Remove Link: "+node3+" "+iface3+" <---> "+node4+" "+iface4, logger.DEBUG);
                                }  
                            }
                        }
                    }
                    
                    // clear link. Remove link if node not exist.
                    ArrayList<ArrayList<String>> links_new1 = new ArrayList();
                    for(ArrayList<String> link : links) {
                        String node1 = link.get(0);
                        String node2 = link.get(3);
                        if(nodes_info.get(node1) != null && nodes_info.get(node2) != null)
                            links_new1.add(link);
                        else
                            logger.Println("Remove Link(not exist node): "+node1+" "+link.get(2)+" <---> "+node2+" "+link.get(5), logger.DEBUG); 
                    }
                    links = links_new1;

                    // replace mac_ip_port from replace_nodes
                    ArrayList<String[]> mac_ip_port_new = new ArrayList();
                    if(mac_ip_port != null) {
                        for(String[] it : mac_ip_port) {
                            String node = it[2];
                            String replace = null;
                            for(String[] replace_node : nodes_to_nodes_for_mac_ip_port) {
                                if(replace_node[0].equals(node)) {
                                    if(replace_node[1] != null) {
                                        replace = replace_node[1];
                                    }
                                    break;
                                }

                            }

                            String node_prev = it[2];
                            if(replace != null) it[2] = replace;
                            mac_ip_port_new.add(it);
                            if(replace != null)
                                logger.Println("Replace mac_ip_port: "+node_prev+"   to:   "+it[2], logger.DEBUG);

                        }
                    }
                    mac_ip_port = mac_ip_port_new;

                    // delete client if ip addres equals with node ip address
                    ArrayList<String[]> mac_ip_port_new1 = new ArrayList();
                    if(mac_ip_port != null) {
                        for(String[] it : mac_ip_port) {
                            if(!it[1].equals(it[2]))
                                mac_ip_port_new1.add(it);
                            else
                                logger.Println("Remove mac_ip_port: "+it[0]+","+it[1]+","+it[2]+","+it[3]+","+it[4], logger.DEBUG);
                        }
                    }
                    mac_ip_port = mac_ip_port_new1;  
                    
                    area_info.put("nodes_information", nodes_info);
                    area_info.put("links", links);
                    area_info.put("mac_ip_port", mac_ip_port);
                }             

            }   
            
            // check nodes without links and adding link from mac_ip_port
            for (Map.Entry<String, Map> area : ((Map<String, Map>) info_map).entrySet()) {
                String area_name = area.getKey();
                Map area_info = area.getValue();
                Map<String, Map> nodes_info = (Map<String, Map>)area_info.get("nodes_information");
                ArrayList<String[]> mac_ip_port = (ArrayList<String[]>)area_info.get("mac_ip_port");
                ArrayList<ArrayList<String>> links_info = (ArrayList<ArrayList<String>>)area_info.get("links");
                if(nodes_info != null && links_info != null) {
    //                Map<String, Map> nodes_info_new = new HashMap();
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        boolean found = false;
                        for(ArrayList<String> link : links_info) {
                            String node1 = link.get(0);
                            String node2 = link.get(3);
                            if(node.equals(node1) || node.equals(node2)) {
                                found = true;
                                break;
                            }
                        }
                        if(!found) {
                            logger.Println("Node "+node+" is not link.", logger.DEBUG);
    //                        boolean found_ip = false;
                            if(mac_ip_port != null) {
                                for(String[] mac_ip_node_id_iface : mac_ip_port) {
                                    if(node.equals(mac_ip_node_id_iface[1])) {
                                        ArrayList<String> link_tmp = new ArrayList();
                                        link_tmp.add(node); link_tmp.add(""); link_tmp.add("unknown");
                                        link_tmp.add(mac_ip_node_id_iface[2]); link_tmp.add(mac_ip_node_id_iface[3]); link_tmp.add(mac_ip_node_id_iface[4]);
                                        link_tmp.add("mac_ip_port");
                                        links_info.add(link_tmp);
                                        logger.Println("Adding new link from mac_ip_port: "+link_tmp.get(0)+", "+link_tmp.get(1)+", "+link_tmp.get(2)+" <---> "+link_tmp.get(3)+", "+link_tmp.get(4)+", "+link_tmp.get(5)+" --- "+link_tmp.get(6), logger.DEBUG);
                                        if(node_info != null && node_info.get("general") != null ) {
                                            String base_address = (String)node_info.get("general").get("base_address");
                                            if((mac_ip_node_id_iface[0].matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") || mac_ip_node_id_iface[0].matches("^([0-9A-Fa-f]{4}[\\.]){2}([0-9A-Fa-f]{4})$")) && 
                                                    (base_address == null || !(base_address.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") || base_address.matches("^([0-9A-Fa-f]{4}[\\.]){2}([0-9A-Fa-f]{4})$"))) ) {
                                                node_info.get("general").put("base_address", mac_ip_node_id_iface[0]);
                                            }
                                        }
                                    }
                                }
                            }
                        } 
                    }
                }                     
            }        

            for (Map.Entry<String, Map> area : ((Map<String, Map>) info_map).entrySet()) {
                String area_name = area.getKey();
    //            System.out.println(area_name);
                Map area_info = area.getValue();
                Map<String, Map> nodes_info = (Map<String, Map>)area_info.get("nodes_information");

                // remove clients from mac_ip_port if ip contains ip address node
                ArrayList<String> ip_list_nodes = new ArrayList();
                ArrayList<String> mac_list_nodes = new ArrayList();            
                if(nodes_info != null) {
                    for (Map.Entry<String, Map> node_it : ((Map<String, Map>) nodes_info).entrySet()) {
                        String node = node_it.getKey();
                        Map<String, Map> node_info = node_it.getValue();
                        if(node_info != null) {
                            ip_list_nodes.add(node);
                            ip_list_nodes.addAll(GetIpListFromNode(node_info));
                            mac_list_nodes.addAll(GetMACFromNode(node_info));
                        }
                    }                  
                }               

                ArrayList<String[]> mac_ip_port_new = new ArrayList();
                ArrayList<String[]> mac_ip_port = (ArrayList<String[]>)area_info.get("mac_ip_port");
                ArrayList<ArrayList<String>> links_info = (ArrayList<ArrayList<String>>)area_info.get("links");
                // remove clients if mac is node mac
                if(mac_ip_port != null) {
                    for(String[] item : mac_ip_port) {
                        String mac = item[0];
                        String ip = item[1];
                        if(!mac_list_nodes.contains(mac) && !ip_list_nodes.contains(ip))
                            mac_ip_port_new.add(item);
                        else
                            logger.Println("Remove client is mac node - "+item[0]+", "+item[1]+", "+item[2]+", "+item[3]+", "+item[4], logger.DEBUG);
                    }
                    mac_ip_port = mac_ip_port_new;

                    // remove clients am links
                    Map<String, String> mac_ip_node_iface_delete = new HashMap();
                    for(String[] item : mac_ip_port) {
                        String mac = item[0];
                        String ip = item[1];
                        String node = item[2];
                        String iface = item[4];
                        for(ArrayList<String> link : links_info) {
                            boolean found1 = false;
                            if(node.equals(link.get(0)) && CompareIfacename(iface, link.get(2)))
                                found1=true;
                            boolean found2 = false;
                            if(node.equals(link.get(3)) && CompareIfacename(iface, link.get(5)))
                                found2=true;
                            if(found1 || found2) {
                                String str = mac+":"+ip+":"+node+":"+iface;
                                mac_ip_node_iface_delete.put(str, str);
                            }
                        }
                    }

                    mac_ip_port_new = new ArrayList();
                    for(String[] item : mac_ip_port) {
                        String mac = item[0];
                        String ip = item[1];
                        String node = item[2];
                        String iface = item[4];                    
                        String str = mac+":"+ip+":"+node+":"+iface;
                        if(mac_ip_node_iface_delete.get(str) == null)
                            mac_ip_port_new.add(item);
                        else
                            logger.Println("Remove client am link: "+mac+", "+ip+" - "+node+", "+iface, logger.DEBUG);
                    }

                    mac_ip_port = mac_ip_port_new;
                    area_info.put("mac_ip_port", mac_ip_port);                
                }             
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return info_map;
    }

    private boolean CompareIfacename(String iface1, String iface2) {
        ArrayList<String> synonim1 = GetIface_Synonim(iface1);
        ArrayList<String> synonim2 = GetIface_Synonim(iface2);
        for(String it : synonim1) {
            if(synonim2.contains(it)) return true;
        }
        return false;
    }
    
    private ArrayList<String> GetIface_Synonim(String iface_name) {
        ArrayList<String> result = new ArrayList();
        iface_name = iface_name.toLowerCase();
        result.add(iface_name);
        Pattern p = Pattern.compile("^([a-z]+)(\\d+.*)$");
        Matcher m = p.matcher(iface_name);
        if(m.find()) {
            if(m.group(1).length() == 0) 
//                System.out.println("111111");
            if(m.group(1).length() > 0) {
                String alias1 = m.group(1).substring(0, 1)+m.group(2);
                result.add(alias1);
            }
            if(m.group(1).length() > 1) {
                String alias2 = m.group(1).substring(0, 2)+m.group(2);
                result.add(alias2);
            }
            if(m.group(1).length() > 2) {
                String alias3 = m.group(1).substring(0, 3)+m.group(2);
                result.add(alias3);
            }
        }
        return result;
    }
    
    public boolean WriteHashMapToFile1(String filename, Map<String, String[]> map) {
        try {
            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String node = entry.getKey();
                String[] val = entry.getValue();
                outFile.write(node);
                for(String item : val) { outFile.write(";"+item); }
                outFile.write("\n");
            }
            outFile.close();  
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }              
    }   
    
    public boolean WriteHashMapToFile2(String filename, Map<String, ArrayList<String>> map) {
        try {
            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                String node = entry.getKey();
                ArrayList<String> val = entry.getValue();
                outFile.write(node);
                for(String item : val) { outFile.write(";"+item); }
                outFile.write("\n");
            }
            outFile.close();  
            return true;
        } catch (IOException ex) {
            if(DEBUG) System.out.println(ex);
            ex.printStackTrace();
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }              
    }      
    
    public ArrayList<String[]> SelectOldLinks(ArrayList<String[]> links_in_buff, ArrayList<String[]> links, ArrayList<String[]> dp_links, Map<String, String> information_nodes) {
        ArrayList<String[]> result = new ArrayList();
        for(String[] item : links_in_buff) {
            boolean find=false;
            for(String[] item1 : links) {
                if( item[0].equals(item1[0]) && CheckInterfaceName(item[2], item1[2]) ) {
                    find=true;
                    logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[0]+" iface="+item1[2]+" is busy from links", logger.DEBUG);
                    break;
                } else if( item[0].equals(item1[3]) && CheckInterfaceName(item[2], item1[5]) ) {
                    find=true;
                    logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[3]+" iface="+item1[5]+" is busy from links", logger.DEBUG);
                    break;                                        
                } else if( item[3].equals(item1[0]) && CheckInterfaceName(item[5], item1[2]) ) {
                    find=true;
                    logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[0]+" iface="+item1[2]+" is busy from links", logger.DEBUG);
                    break;                                        
                } else if( item[3].equals(item1[3]) && CheckInterfaceName(item[5], item1[5]) ) {
                    find=true;
                    logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[3]+" iface="+item1[5]+" is busy from links", logger.DEBUG);
                    break;                                        
                }
            }                                
            if(!find) {
                find=false;
                for(String[] item1 : dp_links) {
                    if( item[0].equals(item1[0]) && CheckInterfaceName(item[2], item1[2]) ) {
                        find=true;
                        logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[0]+" iface="+item1[2]+" is busy from dp_links", logger.DEBUG);
                        break;
                    } else if( item[0].equals(item1[3]) && CheckInterfaceName(item[2], item1[5]) ) {
                        find=true;
                        logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[3]+" iface="+item1[5]+" is busy from dp_links", logger.DEBUG);
                        break;                                        
                    } else if( item[3].equals(item1[0]) && CheckInterfaceName(item[5], item1[2]) ) {
                        find=true;
                        logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[0]+" iface="+item1[2]+" is busy from dp_links", logger.DEBUG);
                        break;                                        
                    } else if( item[3].equals(item1[3]) && CheckInterfaceName(item[5], item1[5]) ) {
                        find=true;
                        logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node="+item1[3]+" iface="+item1[5]+" is busy from dp_links", logger.DEBUG);
                        break;                                        
                    }
                }       
                if(!find) {
                    boolean find1=false; boolean find2=false;
                    for(Map.Entry<String, String> entry : information_nodes.entrySet()) {
                        String node = entry.getKey();
                        if(!find1 && item[0].equals(node)) find1=true;
                        if(!find2 && item[3].equals(node)) find2=true;
                        if(find1 && find2) break;
                    }       
                    if(find1 && find2) {
                        String[] mas = new String[6];
                        mas[0]=item[0]; mas[1]=item[1]; mas[2]=item[2]; mas[3]=item[3]; mas[4]=item[4]; mas[5]=item[5];
                        logger.Println("Adding old links: "+mas[0]+";"+mas[1]+";"+mas[2]+" <---> "+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG);
                        result.add(mas);                        
                    } else {
                        if(!find1) logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node_link="+item[0]+" not exist in information_nodes.", logger.DEBUG);
                        if(!find2) logger.Println("Not added old links: "+item[0]+","+item[2]+" <---> "+item[3]+","+item[5]+" node_link="+item[3]+" not exist in information_nodes.", logger.DEBUG);
                    }
                }
            }
        }
        return result;
    }
    
    public Map<String, String> SelectNodesCDPLLDPGroup( Map<String, String> informationFromNodes, Map<String, Map<String, ArrayList>> walkInformation) {
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, String> entry : informationFromNodes.entrySet()) {
            String node = entry.getKey();
            
            if( (walkInformation.get("cdpCacheAddress") != null && walkInformation.get("cdpCacheAddress").get(node) != null) &&
                ( walkInformation.get("lldpRemManAddrIfSubtype") != null && walkInformation.get("lldpRemManAddrIfSubtype").get(node) != null) && 
                ( walkInformation.get("lldpRemChassisId") != null && walkInformation.get("lldpRemChassisId").get(node) != null) ) { 
                logger.Println("SelectNodesCDPLLDPGroup: node="+node+" - cdp_lldp", logger.DEBUG);
                result.put(node, "cdp_lldp");
            }
            else if(walkInformation.get("cdpCacheAddress") != null && walkInformation.get("cdpCacheAddress").get(node) != null) {
                logger.Println("SelectNodesCDPLLDPGroup: node="+node+" - cdp", logger.DEBUG);
                result.put(node, "cdp");
            }
            else if( ( walkInformation.get("lldpRemManAddrIfSubtype") != null && walkInformation.get("lldpRemManAddrIfSubtype").get(node) != null) || 
                ( walkInformation.get("lldpRemChassisId") != null && walkInformation.get("lldpRemChassisId").get(node) != null) ) {
                logger.Println("SelectNodesCDPLLDPGroup: node="+node+" - lldp", logger.DEBUG);
                result.put(node, "lldp");
            }
            else {
                logger.Println("SelectNodesCDPLLDPGroup: node="+node+" - none", logger.DEBUG);
                result.put(node, "none");
            }
        }      
        return result;
    }
    
    public boolean Check_CDP_LDP_NONE_Neighbors(String priznak1, String priznak2) {
        if(
               (
                   (priznak1.equals("none") && priznak2.equals("none") ||
                    priznak1.equals("none") && priznak2.equals("cdp") ||
                    priznak1.equals("none") && priznak2.equals("lldp") ||
                    priznak1.equals("none") && priznak2.equals("cdp_lldp"))
                   ||
                   (priznak1.equals("cdp") && priznak2.equals("none") ||
                    priznak1.equals("cdp") && priznak2.equals("lldp"))                
                   || 
                   (priznak1.equals("lldp") && priznak2.equals("none") ||
                    priznak1.equals("lldp") && priznak2.equals("cdp"))  
                   ||
                   priznak1.equals("cdp_lldp") && priznak2.equals("none") 
               ) 
               &&
               (
                   (priznak2.equals("none") && priznak1.equals("none") ||
                    priznak2.equals("none") && priznak1.equals("cdp") ||
                    priznak2.equals("none") && priznak1.equals("lldp") ||
                    priznak2.equals("none") && priznak1.equals("cdp_lldp"))
                   ||
                   (priznak2.equals("cdp") && priznak1.equals("none") ||
                    priznak2.equals("cdp") && priznak1.equals("lldp"))                
                   || 
                   (priznak2.equals("lldp") && priznak1.equals("none") ||
                    priznak2.equals("lldp") && priznak1.equals("cdp"))  
                   ||
                   priznak2.equals("cdp_lldp") && priznak1.equals("none") 
               )                
        ) return true;
        else return false;
    }
    
    private void MapToIndexSubProcessor(Map<String, Object> map, String key, Object value, IndexWriter w, String area, String node) {
        if (value instanceof String) {
//            System.out.println(value);
            String[] mas = ((String) value).split("\n");
            for(String item : mas) {
                if(((String) item).length() > 32765) {

                    int i = 0;
                    while(true) {
                        String val = null;
                        try {
                            val = ((String)item).substring(32765*i, 32765*(i+1));
                        } catch(Exception ex) {
                            val = ((String)item).substring(32765*i);
                        }
                        addDoc(w, val, area, node);
                        if(val.length() < 32765) break;
                        i = i + 1;
                    }
                } else
                    addDoc(w, (String)item, area, node);
            }
        } else if (value instanceof List) {
            List list = (List) value;
            for (Object object : list) {
                MapToIndexSubProcessor(map, key, object, w, area, node);
            }
        } else if (value instanceof Map) {
            try {
                //noinspection unchecked
                Map<String, Object> subMap = (Map<String, Object>) value;
                MapToIndex(subMap, w, area, node);
            } catch (ClassCastException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException(String.valueOf(value));
        }
    }
    
    private void MapToIndex(Map<String, Object> map, IndexWriter w, String area, String node) {
        ArrayList<String> result = new ArrayList();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            MapToIndexSubProcessor(map, key, value, w, area, node);
        }
    }    
    
    public void IndexFullText(Map<String, Map> info, String index_directory) {
        try{ 
            final Directory index = FSDirectory.open(Paths.get(index_directory));
            StandardAnalyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter w = new IndexWriter(index, config);
            Gson gson = new Gson();
            for (Map.Entry<String, Map> entry : info.entrySet()) {
                String area = entry.getKey();
                Map<String, Map> area_info = entry.getValue();
                Map<String, Map> nodes_info = area_info.get("nodes_information");
                if(nodes_info != null) {
                    for(Map.Entry<String, Map> entry1 : nodes_info.entrySet()) {
                        String node = entry1.getKey();
                        Map node_info = entry1.getValue();
                        MapToIndex(node_info, w, area, node);
                    }
                }
            }
            
            w.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void addDoc(IndexWriter w, String text, String area, String node) {
        Document doc = new Document();
        doc.add(new TextField("text", text, Field.Store.YES));
//        doc.add(new TextField("text", text.replaceAll("[\\.|/\\\\;:]", " "), Field.Store.YES));

        // use a string field for isbn because we don't want it tokenized
//        doc.add(new StringField("origin", text, Field.Store.YES));
        doc.add(new StringField("area", area, Field.Store.YES));
        doc.add(new StringField("node", node, Field.Store.YES));
        try {
            w.addDocument(doc);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }     
    
    public void Indexing(Map<String, ArrayList<ArrayList<String>>> index) {
        logger.Println("Indexing map file: "+map_file, logger.DEBUG);
        FilesToIndexing(map_file, index);
        
        File history = new File(history_dir);
        if(history.isDirectory()) {
            File[] list_dir = history.listFiles();
            for(File file : list_dir) {
                String filename = file.getAbsoluteFile().toString();
                String short_filename = file.getAbsoluteFile().getName();
                logger.Println("Indexing map file: "+short_filename, logger.DEBUG);
                FilesToIndexing(filename, index);      
            }
        }
        
        // write full text index
        logger.Println("Start Indexing Full text search map file - "+map_file+"...", logger.DEBUG);
        Map<String, Map> info=utils.ReadJSONFile(map_file);
        IndexFullText(info, Neb.index_dir);
        logger.Println("Stop Indexing Full text search map file - "+map_file+".", logger.DEBUG);
     
    }    
    
    public void FilesToIndexing(String file, Map<String, ArrayList<ArrayList<String>>> index) {
        Pattern p = Pattern.compile(".+Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        String date_str = "";
        Matcher m = p.matcher(file);
        if(m.find()) {
            date_str = m.group(1).replace("-", " ");
        } else {
            date_str = GetFileCreateTime(file);
        }
        
        Map<String, Map> info=utils.ReadJSONFile(file);
        for (Map.Entry<String, Map> entry : info.entrySet()) {
            String area = entry.getKey();
            Map<String, Map> area_info = entry.getValue();
            Map<String, Map> nodes_info = area_info.get("nodes_information");
            if(nodes_info != null) {
                for(Map.Entry<String, Map> entry1 : nodes_info.entrySet()) {
                    String node = entry1.getKey();
                    Map node_info = entry1.getValue();
                    
                    String sysname = null;
                    if(node_info.get("general") != null && ((Map)node_info.get("general")).get("sysname") != null) {
                        sysname = (String)((Map)node_info.get("general")).get("sysname");
                        AddToIndexStruct(file, date_str, area, sysname, sysname+"("+node+")", node, "", index);
                    }
                    String name = node;
                    if(sysname != null) name = name+"("+sysname+")";
                    AddToIndexStruct(file, date_str, area, node, name, node, "", index);
                    
                    Map<String, Map> interfaces = (Map<String, Map>)node_info.get("interfaces");
                    if(interfaces != null) {
                        for(Map.Entry<String, Map> entry2 : interfaces.entrySet()) {
                            String iface = entry2.getKey();
//                            if(node.equals("10.96.187.82") && iface.equals("Vlan1"))
//                                System.out.println("111");
                            
                            Map iface_info = entry2.getValue();
                            if(iface_info.get("ip") != null) {
                                ArrayList<String> ip_list = (ArrayList<String>)iface_info.get("ip");
                                for(String ip : ip_list) {
                                    ip = ip.split(" ")[0].split("/")[0];
                                    name = ip;
                                    if(sysname != null) name = name+"("+sysname+")";                                   
                                    AddToIndexStruct(file, date_str, area, ip, name, node, "", index);
                                }
                            }
                            if(iface_info.get("mac") != null) {
                                String mac = NormalizeMAC((String)iface_info.get("mac"));
                                name = mac;
                                if(sysname != null) name = name+"("+sysname+")";                                   
                                AddToIndexStruct(file, date_str, area, mac, name, node, "", index);
                            }                            
                        }
                    }
                }
            }
            
            ArrayList<ArrayList<String>> mac_ip_port = (ArrayList<ArrayList<String>>)area_info.get("mac_ip_port");
            if(mac_ip_port != null) {
                for(ArrayList<String> mip : mac_ip_port) {
                    String mac = NormalizeMAC(mip.get(0));
                    String ip = mip.get(1);
                    String node1 = mip.get(2);
                    String port = mip.get(4);
                    if(!mac.equals("unknown_mac") || !mac.equals("")) {
                        String name1 = mac;
                        if(!ip.equals("unknown_ip"))
                            name1 = ip+"("+mac+")";
                        AddToIndexStruct(file, date_str, area, mac, name1, node1, port, index);
                    }
                    if(!ip.equals("unknown_ip")) {
                        String name1 = ip;
                        if(!mac.equals("unknown_mac") || !mac.equals(""))
                            name1 = ip+"("+mac+")";
                        AddToIndexStruct(file, date_str, area, ip, name1, node1, port, index);
                    }                            
                }
            }            
        }

    }
    
    private String NormalizeMAC(String mac) {
        String out = "";
        mac = mac.toLowerCase().replace(":", "").replace("-", "").replace(".", "");
        if(mac.length() == 12 && !mac.equals("unknown_mac")) {
            String[] mas = mac.split("");
            out = mas[0]+mas[1]+":"+mas[2]+mas[3]+":"+mas[4]+mas[5]+":"+mas[6]+mas[7]+":"+mas[8]+mas[9]+":"+mas[10]+mas[11];
        }
        
        return out;
    }
    
    private void AddToIndexStruct(String file, String data, String area, String mac_ip_sysname, String name, String node, String port, Map<String, ArrayList<ArrayList<String>>> index) {
        if(!mac_ip_sysname.equals("")) {
            if(index.get(mac_ip_sysname) != null) {
                ArrayList<ArrayList<String>> list_tmp = index.get(mac_ip_sysname);

                boolean find = false;
                for(ArrayList<String> item : list_tmp) {
                    if(item.get(0).equals(data) || item.get(1).equals(file)) {
                        find = true;
                        break;
                    }
                }
                if(!find) {
                    ArrayList list_tmp1 = new ArrayList();
                    list_tmp1.add(data);
                    list_tmp1.add(file);
                    list_tmp1.add(area);
                    list_tmp1.add(name);
                    list_tmp1.add(node);
                    list_tmp1.add(port);                    
                    list_tmp.add(list_tmp1);
                }
            } else {
                ArrayList<ArrayList<String>> list_tmp = new ArrayList();
                ArrayList list_tmp1 = new ArrayList();
                list_tmp1.add(data);
                list_tmp1.add(file);
                list_tmp1.add(area);
                list_tmp1.add(name);
                list_tmp1.add(node);
                list_tmp1.add(port);
                list_tmp.add(list_tmp1);
                index.put(mac_ip_sysname, list_tmp);
            }
        }        
    }
    
    public boolean DeletedNode(String node, boolean force) {
        boolean is_changed=false;
        if(Neb.nodes_info.get(node) != null) {
            String[] link = new String[6];
            ArrayList<Integer> pos = new ArrayList();
            for(int i=0; i<Neb.links_info.size(); i++) {
                String[] item = Neb.links_info.get(i);
                if(item[0].equals(node)) {
                    link[0]=item[0]; link[1]=item[1]; link[2]=item[2];
                    link[3]=item[3]; link[4]=item[4]; link[5]=item[5];
                    pos.add(i);
                } else if(item[3].equals(node)) {
                    link[0]=item[3]; link[1]=item[4]; link[2]=item[5];
                    link[3]=item[0]; link[4]=item[1]; link[5]=item[2];
                    pos.add(i);
                }                        
            }
            if(pos.size() == 1 || force) {
                is_changed=true;
                Map<String, String[]> mac_ArpMacTable_tmp = new HashMap();
                synchronized(Neb.mac_ArpMacTable) { mac_ArpMacTable_tmp.putAll(Neb.mac_ArpMacTable); }
                for (Map.Entry<String, String[]> entry : mac_ArpMacTable_tmp.entrySet()) {
                    String[] value = entry.getValue();
                    String[] mas = new String[6];
                    if(value[2].equals(node) || value[1].equals(node)) {
                        mas[0]=value[0]; mas[1]=value[1]; mas[2]=link[3];
                        mas[3]=link[4]; mas[4]=link[5]; mas[5]=value[5];
                        synchronized(Neb.mac_ArpMacTable) { 
                            Neb.mac_ArpMacTable.replace(entry.getKey(), mas);
                            logger.Println("Replace mac_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
                        }
                    }
                }

                Map<String, String[]> ip_ArpMacTable_tmp = new HashMap();
                synchronized(Neb.ip_ArpMacTable) { ip_ArpMacTable_tmp.putAll(Neb.ip_ArpMacTable); }
                for (Map.Entry<String, String[]> entry : ip_ArpMacTable_tmp.entrySet()) {
                    String[] value = entry.getValue();
                    String[] mas = new String[6];
                    if(value[2].equals(node) || value[1].equals(node)) {
                        mas[0]=value[0]; mas[1]=value[1]; mas[2]=link[3];
                        mas[3]=link[4]; mas[4]=link[5]; mas[5]=value[5];                                
                        synchronized(Neb.ip_ArpMacTable) { 
                            Neb.ip_ArpMacTable.replace(entry.getKey(), mas); 
                            logger.Println("Replace ip_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5]+" to: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
                        }
                    }
                }
                // adding deleted nodes to ARP_MAC table
                String[] node_info = Neb.nodes_info.get(node);
                if(node_info != null && node_info.length > 10) {
                    String[] mas = new String[6];
                    Pattern p = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
                    String line=node_info[0];
                    for(int i=1; i<node_info.length; i++) line=line+";"+node_info[i];
                    Matcher m = p.matcher(line);
                    String mac="";
                    if(m.find()) mac=m.group(1);

                    if(!mac.equals("")) mas[0]=mac; else mas[0]=node_info[0];
                    mas[1]=node_info[0];
                    mas[2]=link[3]; mas[3]=link[4]; mas[4]=link[5];
                    mas[5]=String.valueOf(System.currentTimeMillis());

                    Matcher m2 = p.matcher(mas[0]);
                    String mac_find="";
                    if(m2.find()) mac_find=m2.group(1);

                    Pattern p1 = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)");
                    Matcher m1 = p1.matcher(mas[1]);
                    String ip_find="";
                    if(m1.find()) ip_find=m1.group(1);

                    if(Neb.mac_ArpMacTable.get(mac_find) == null && Neb.ip_ArpMacTable.get(ip_find) == null) {

                        logger.Println("Adding deleted node to arp_mac_table: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
                        synchronized(Neb.mac_ArpMacTable) { Neb.mac_ArpMacTable.put(mas[0], mas); } 
                        synchronized(Neb.ip_ArpMacTable) { Neb.ip_ArpMacTable.put(mas[1], mas); }
                    } else {
                        logger.Println("Not adding deleted node to arp_mac_table. Is exist: "+mas[0]+";"+mas[1]+";"+mas[2]+";"+mas[3]+";"+mas[4]+";"+mas[5], logger.DEBUG); 
                    }

                }  
                synchronized(Neb.extended_info) { 
                    Neb.extended_info.put(node, Neb.nodes_info.get(node)); 
                    logger.Println("Put deleted node: "+node+" to extended_info.", logger.DEBUG); 
                }
                synchronized(Neb.nodes_info) { 
                    Neb.nodes_info.remove(node); 
                    logger.Println("Remove deleted node: "+node+" from nodes_info.", logger.DEBUG); 
                }
                synchronized(Neb.links_info) {
                    for(int p : pos) Neb.links_info.remove(p);
                    logger.Println("Node: "+node+" remove from links_info."+Neb.links_info.get(pos.get(0))[0]+","+Neb.links_info.get(pos.get(0))[2]+" <===> "+Neb.links_info.get(pos.get(0))[3]+","+Neb.links_info.get(pos.get(0))[5], logger.DEBUG); 
                }
                synchronized(Neb.text_info) { 
                    Neb.text_info.remove(node); 
                    logger.Println("Remove deleted node: "+node+" from text_info.", logger.DEBUG); 
                }
            } else if(pos.size() == 0) {
                is_changed=true;
                
                Map<String, String[]> mac_ArpMacTable_tmp = new HashMap();
                synchronized(Neb.mac_ArpMacTable) { mac_ArpMacTable_tmp.putAll(Neb.mac_ArpMacTable); }
                for (Map.Entry<String, String[]> entry : mac_ArpMacTable_tmp.entrySet()) {
                    String[] value = entry.getValue();
                    String[] mas = new String[6];
                    if(value[2].equals(node)) {
                        synchronized(Neb.mac_ArpMacTable) { 
                            Neb.mac_ArpMacTable.remove(entry.getKey());
                            logger.Println("Delete mac_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5], logger.DEBUG); 
                        }
                    }
                }
                
                Map<String, String[]> ip_ArpMacTable_tmp = new HashMap();
                synchronized(Neb.ip_ArpMacTable) { ip_ArpMacTable_tmp.putAll(Neb.ip_ArpMacTable); }
                for (Map.Entry<String, String[]> entry : ip_ArpMacTable_tmp.entrySet()) {
                    String[] value = entry.getValue();
                    String[] mas = new String[6];
                    if(value[2].equals(node)) {
                        synchronized(Neb.ip_ArpMacTable) { 
                            Neb.ip_ArpMacTable.remove(entry.getKey());
                            logger.Println("Delete ip_ArpMacTable: "+entry.getKey()+" ===>"+value[0]+";"+value[1]+";"+value[2]+";"+value[3]+";"+value[4]+";"+value[5], logger.DEBUG); 
                        }
                    }
                }                
                
                synchronized(Neb.extended_info) { 
                    Neb.extended_info.put(node, Neb.nodes_info.get(node));
                    logger.Println("Put deleted node: "+node+" to extended_info.", logger.DEBUG); 
                }
                synchronized(Neb.nodes_info) { 
                    Neb.nodes_info.remove(node); 
                    logger.Println("Remove deleted node: "+node+" from nodes_info.", logger.DEBUG); 
                }
                synchronized(Neb.text_info) { 
                    Neb.text_info.remove(node);
                    logger.Println("Remove deleted node: "+node+" from text_info.", logger.DEBUG);
                }
            } else logger.Println("Not delete node: "+node+" num links = "+pos.size(), logger.DEBUG);
        }
        return is_changed;
    }
    
    public void RemoveOldFiles(String folder, int history_day) {
        File f_folder = new File(folder);
        if(f_folder.exists() && f_folder.isDirectory()) {
            File[] folderEntries = f_folder.listFiles();  
            for (File file : folderEntries)
            {
                if (file.isFile())
                {
                    if((long)(System.currentTimeMillis()-GetFileCreateTime_mSec(file.getPath())) > (long)history_day*24*60*60*1000) {
                        file.delete();
    //                    System.out.println("Delete file: "+file.getName());
                        logger.Println("Delete file: "+file.getName(), logger.DEBUG);
                    }
                }

            } 
        }
    }   
    
    public boolean MapToFile(Map map, String filename) {
        Gson gson = new Gson(); 
        String str = gson.toJson(map);        
        return WriteStrToFile(filename, PrettyJSONOut(str));
    }
    
    public boolean Map_To_NebMapFile(Map map, String filename) {
        Gson gson = new Gson(); 
        
        WriteStrToFile(filename, "");
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)map).entrySet()) {
            String key = entry.getKey();
            Map value = entry.getValue();
            Map<String, Map> map_tmp = new HashMap();
            map_tmp.put(key, value);
            String str = PrettyJSONOut(gson.toJson(map_tmp));
            str = "########################## "+key + " ####################################\n" + str+"\n";
            if(!AppendStrToFile(filename, str)) return false;
        }
       
        return true;
    }    

    private String PrettyJSONOut(String str) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(str);
        String result = gson.toJson(je);        
        
        return result;
    } 
    
    private Map Uniqal_Nodes_Information(Map<String, Map> nodes_information) {
        Map result = new HashMap();
        Map<String, String> ip_collection = new HashMap();
        
        for(Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node=entry.getKey();
            Map val=(Map)entry.getValue();
            if(ip_collection.get(node) == null) result.put(node, val);
            else logger.Println("Not uniqal node: "+node, logger.DEBUG);
            ip_collection.putAll(GetIpNode(val));
        }
        
        return result;
    }
    
    private Map Uniqal_Nodes_Information(Map<String, Map> nodes_information, ArrayList<String> networks) {
        Map<String, Map> result = new HashMap();
        Map<String, String> ip_node = new HashMap();
        
        Map<String, Integer> node_priority = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
            String node = entry.getKey();
            int pos = 0;
            boolean found = false;
            for(String network : networks) {
                if(Inside_Network(node, network)) {
                    found = true;
                    break;
                } 
                pos = pos + 1;                
            }
            if(found)
                node_priority.put(node, pos);
        }
        
        for(Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node=entry.getKey();
            Map val=(Map)entry.getValue();
            if(ip_node.get(node) == null) result.put(node, val);
            else {
                String node_exist = ip_node.get(node);
                if( (node_priority.get(node) != null && node_priority.get(node_exist) == null) ||
                    (node_priority.get(node) != null && node_priority.get(node_exist) != null && 
                        node_priority.get(node) < node_priority.get(node_exist)) ) {
                    result.remove(node_exist);
                    logger.Println("Remove not uniqal node: "+node_exist, logger.DEBUG);
                    result.put(node, val);
                    logger.Println("Adding uniqal node: "+node, logger.DEBUG);
                } else {
                    logger.Println("Not uniqal node: "+node, logger.DEBUG);
                }      

            }

            Map<String, String> ip_map = GetIpNode(val);
            for(Map.Entry<String, String> entry1 : ip_map.entrySet()) {
                String ip = entry1.getKey();
                ip_node.put(ip, node);
            }

        }
        
        return result;
    }
    
    private Map<String, String> GetIpNode(Map<String, Map> node_information) {
        // Get Ip => node
        Map<String, String> ip_node = new HashMap();
            Map<String, Map> interfaces=(Map)node_information.get("interfaces");
            if(interfaces != null && interfaces.size() > 0) {
                for(Map.Entry<String, Map> entry1 : interfaces.entrySet()) {
                    String iface_name=entry1.getKey();
                    ArrayList<String> ip_list = new ArrayList();
                    if(entry1.getValue().get("ip") != null) {
                        if(entry1.getValue().get("ip") instanceof String) {
                            ip_list.add((String)entry1.getValue().get("ip"));
                        } else if(entry1.getValue().get("ip") instanceof ArrayList) {
                            ip_list = (ArrayList<String>)entry1.getValue().get("ip");
                        }
                    }
                    if(ip_list.size() > 0) {
                        for(String ip : ip_list) {
                            ip=ip.split("[/\\s]")[0];
                            if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"))
                                ip_node.put(ip, ip);
                        }
                    }
                }
            }
        
        return ip_node;
    }   
    
//    private Map<String, String> GetIpFromNone(Map<String, Map> nodes_information) {
//        Map<String, String> result = new HashMap();
//
//        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
//            String node = entry.getKey();
//            result.put(node, node);
//            Map<String, Map> map_interf = ((Map<String, Map>)entry.getValue()).get("interfaces");
//            if(map_interf != null && map_interf.size() > 0) {
//                for(Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
//                    ArrayList<String> list = (ArrayList)entry1.getValue().get("ip");
//                    if(list != null && list.size() > 0) {
//                        for(String ip : list) {
//                            if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")) {
//                                ip=ip.split("/")[0];
//                                result.put(ip, ip);
//                            }
//                            else if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                                ip=ip.split(" ")[0];
//                                result.put(ip, ip);
//                            }
//                            else if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
//                                result.put(ip, ip);
//                            } else logger.Println("Bad format ip address - "+ip, logger.DEBUG);
//                        }
//                    }
//                }
//            }
//        }
//        
//        return result;
//    }
    
    public ArrayList<ArrayList<ArrayList<String>>> NormalizationLinks(Map<String, Map> nodes_information, ArrayList<String> networks) {
        ArrayList<ArrayList<ArrayList<String>>> result = new ArrayList();
        
        // Get mac => [node, iface]
        Map<String, String[]> mac_node = GetMacNode_Iface(nodes_information);
        // Get Ip => node
        Map<String, String> ip_node = GetIpFromNodes(nodes_information, networks);
        
        ArrayList<ArrayList<String>> links = new ArrayList();
        ArrayList<ArrayList<String>> links_extended = new ArrayList();
        for(Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node=entry.getKey();
            Map val=entry.getValue();
            if(val.get("advanced") != null) {
                Map<String, ArrayList<Map<String, String>>> links_map=(Map)((Map)val.get("advanced")).get("links");
                if(links_map != null && links_map.size() > 0) {
                    for(Map.Entry<String, ArrayList<Map<String, String>>> entry1 : links_map.entrySet()) {
                        String iface_local=entry1.getKey();
                        ArrayList<Map<String, String>> val1=entry1.getValue();
                        for(Map<String, String> link : val1) {
                            String node_remote="unknown";
                            String str_tmp=(String)link.get("remote_ip");
                            if(str_tmp != null && !str_tmp.equals("")) node_remote=MacSysnameToIp(str_tmp, mac_node);
                            if(node_remote.equals("unknown"))
                                if(link.get("remote_id") != null)
                                    node_remote=(String)link.get("remote_id");

                            String port_remote="unknown";
                            str_tmp=(String)link.get("remote_port");
                            if(str_tmp != null && !str_tmp.equals("")) {
                                if(!CheckMACFromName(str_tmp)) port_remote=str_tmp;
                                else {
                                    String mac = ExtractMACFromName(str_tmp);
                                    mac=mac.toLowerCase().replaceAll("[\\.:-]", "");
                                    if(mac_node.get(mac) != null) {
                                        logger.Println("Replace remote interface: "+port_remote+" to - "+mac_node.get(mac)[1], logger.DEBUG);
//                                        System.out.println("Replace remote interface: "+port_remote+" to - "+mac_node.get(mac)[1]);
                                        port_remote=mac_node.get(mac)[1];
                                    }
                                }
                            }
                            str_tmp=(String)link.get("remote_port_id");
                            if(str_tmp != null && !str_tmp.equals("")) {
                                if(!CheckMACFromName(str_tmp)) port_remote=str_tmp;
                                else {
                                    String mac = ExtractMACFromName(str_tmp);
                                    mac=mac.toLowerCase().replaceAll("[\\.:-]", "");                            
                                    if(mac_node.get(mac) != null) {
        //                                System.out.println("Replace remote interface: "+port_remote+" to - "+mac_node.get(mac)[1]);
                                        port_remote=mac_node.get(mac)[1];
                                    }
                                }                        
                            }

                            if(node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && !node.startsWith("127.")) {
                                if(!node.equals(ip_node.get(node)) && ip_node.get(node) != null) {
                                    logger.Println("Replace: "+node+" to: "+ip_node.get(node), logger.DEBUG);
    //                                System.out.println("Replace: "+node+" to: "+ip_node.get(node));
                                }
                                if(ip_node.get(node) != null) node=ip_node.get(node);
                            }
                            if(node_remote.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") && !node_remote.startsWith("127.")) {
                                if(!node_remote.equals(ip_node.get(node_remote)) && ip_node.get(node_remote) != null) {
                                    logger.Println("Replace: "+node_remote+" to: "+ip_node.get(node_remote), logger.DEBUG);
    //                                System.out.println("Replace: "+node_remote+" to: "+ip_node.get(node_remote));
                                }
                                if(ip_node.get(node_remote) != null) node_remote=ip_node.get(node_remote);
                            }                    
                            ArrayList<String> mas = new ArrayList();
                            mas.add(node); mas.add(iface_local);
                            mas.add(node_remote); mas.add(port_remote);
                            Gson gson = new Gson(); 
                            mas.add(gson.toJson(link));
//                            if(!mas.get(3).equals("unknown")) 
//                            {
                                if(!mas.get(0).equals(mas.get(2))) links.add(mas);
                                else {
                                    logger.Println("Remove link(source and dest equals):"+mas.get(0)+", "+mas.get(1)+" <---> "+mas.get(2)+", "+mas.get(3), logger.DEBUG);
    //                                System.out.println("Remove link(source and dest equals):"+mas.get(0)+", "+mas.get(1)+" <---> "+mas.get(2)+", "+mas.get(3));
                                }
//                            }
//                            else links_extended.add(mas);
                        }
                    }
                }
            }
        }
        
        // checking and replace short interface to full name interface
        for(ArrayList<String> mas : links) {
            String node1=mas.get(0);
            String iface1=mas.get(1);
            if(nodes_information.get(node1) != null && nodes_information.get(node1).get("interfaces") != null) {
                Map<String, Map> interfaces_map = (Map<String, Map>)nodes_information.get(node1).get("interfaces");
                boolean find=false;
                if(interfaces_map.get(iface1) == null) {
                    for (Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
                        String interfacename=entry.getKey();
                        if(EqualsIfaceName(iface1, interfacename)) {
                            mas.set(1, interfacename);
                            logger.Println("Replace: "+node1+" "+iface1+" to: "+node1+" "+mas.get(1), logger.DEBUG);
//                            System.out.println("Replace: "+node1+" "+iface1+" to: "+node1+" "+mas.get(1));
                            find=true;
                            break;
                        }
                    }
                } else find=true;
                if(!find){
                   logger.Println(node1+" "+mas.get(1)+" not finded interfece from node.", logger.DEBUG);
//                   System.out.println(node1+" "+mas.get(1)+" not finded interfece from node.");
                }
            }
            
            String node2=mas.get(2);
            String iface2=mas.get(3);
            if(nodes_information.get(node2) != null && nodes_information.get(node2).get("interfaces") != null) {
                Map<String, Map> interfaces_map = (Map<String, Map>)nodes_information.get(node2).get("interfaces");
                boolean find=false;
                if(interfaces_map.get(iface2) == null) {
                    for (Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
                        String interfacename=entry.getKey();
                        if(EqualsIfaceName(iface2, interfacename)) {
                            mas.set(3, interfacename);
                            logger.Println("Replace: "+node2+" "+iface2+" to: "+node2+" "+mas.get(3), logger.DEBUG);
//                            System.out.println("Replace: "+node2+" "+iface2+" to: "+node2+" "+mas.get(3));
                            find=true;
                            break;
                        }
                    }
                } else find=true;
                if(!find){
                   logger.Println(node2+" "+mas.get(3)+" not finded interfece from node.", logger.DEBUG);
//                   System.out.println(node2+" "+mas.get(3)+" not finded interfece from node.");
                }
            }
            
        }

        // delete duplicate link
        ArrayList<ArrayList<String>> links_new = new ArrayList();
        for(int i=0; i<links.size(); i++) {
            ArrayList<String> mas1=links.get(i);
            ArrayList<ArrayList<String>> links_duplicate = new ArrayList();
            links_duplicate.add(mas1);
            for(int j=i+1; j<links.size(); j++) {
                ArrayList<String> mas2=links.get(j);
                if(
                        (mas1.get(0).equals(mas2.get(0)) && EqualsIfaceName(mas1.get(1), mas2.get(1)) && mas1.get(2).equals(mas2.get(2))) && EqualsIfaceName(mas1.get(3), mas2.get(3)) ||
                        (mas1.get(2).equals(mas2.get(2)) && EqualsIfaceName(mas1.get(3), mas2.get(3)) && mas1.get(0).equals(mas2.get(0))) && EqualsIfaceName(mas1.get(1), mas2.get(1)) ||
                        (mas1.get(0).equals(mas2.get(2)) && EqualsIfaceName(mas1.get(1), mas2.get(3)) && mas1.get(2).equals(mas2.get(0))) && EqualsIfaceName(mas1.get(3), mas2.get(1)) ||
                        (mas1.get(2).equals(mas2.get(0)) && EqualsIfaceName(mas1.get(3), mas2.get(1))) && mas1.get(0).equals(mas2.get(2)) && EqualsIfaceName(mas1.get(1), mas2.get(3))) {
                    links_duplicate.add(mas2);
                    links.remove(j);
                    j--;
                    logger.Println("Link: "+mas1.get(0)+" "+mas1.get(1)+" <---> "+mas1.get(2)+" "+mas1.get(3)+" dublicate to link: "+mas2.get(0)+" "+mas2.get(1)+" <---> "+mas2.get(2)+" "+mas2.get(3), logger.DEBUG);
//                    System.out.println("Link: "+mas1.get(0)+" "+mas1.get(1)+" <---> "+mas1.get(2)+" "+mas1.get(3)+" dublicate to link: "+mas2.get(0)+" "+mas2.get(1)+" <---> "+mas2.get(2)+" "+mas2.get(3));                    
                }
            }
            
            if(links_duplicate.size() > 1) {
                ArrayList<String[]> links_score = new ArrayList();
                for(ArrayList<String> link : links_duplicate) {
                    String[] link_score = new String[6];
                    link_score[0] = link.get(0);
                    link_score[1] = link.get(1);
                    link_score[2] = link.get(2);
                    link_score[3] = link.get(3);
                    link_score[4] = link.get(4);
                    int score = 0;
                    if(link.get(0).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) score=score+100;
                    score=score+link.get(1).length();
                    if(link.get(2).matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) score=score+100;
                    score=score+link.get(3).length();
                    link_score[5] = Integer.toString(score);
                    links_score.add(link_score);
                }
                ArrayList<String> link_normal = new ArrayList();
                int max_score=0;
                for(String[] link : links_score) {
                    int score = Integer.valueOf(link[5]);
                    if(score > max_score) {
                        max_score=score;
                        link_normal = new ArrayList();
                        link_normal.add(link[0]);
                        link_normal.add(link[1]);
                        link_normal.add(link[2]);
                        link_normal.add(link[3]);
                        link_normal.add(link[4]);
                    }
                }
                links_new.add(link_normal);
                logger.Println("Adding normal link: "+link_normal.get(0)+" "+link_normal.get(1)+" <---> "+link_normal.get(2)+" "+link_normal.get(3), logger.DEBUG);
            } else {
                links_new.add(mas1);
            }

        }
        links = links_new;
        
        // delete link with node 127.0.0.1
        for(int i=0; i<links.size(); i++) {
            ArrayList<String> mas=links.get(i);
            if(mas.get(0).startsWith("127.") && mas.get(2).startsWith("127.")) {
                logger.Println("Remove link: "+mas.get(0)+" "+mas.get(1)+" <---> "+mas.get(2)+" "+mas.get(3), logger.DEBUG);
//                System.out.println("Remove link: "+mas.get(0)+" "+mas.get(1)+" <---> "+mas.get(2)+" "+mas.get(3));                    
                links.remove(i);
                i--;
            }
        }
        
        // replace \n and space
        ArrayList<ArrayList<String>> new_list = new ArrayList();
        for(ArrayList<String> mas : links) {
            ArrayList<String> mas1 = new ArrayList();
            for(int i = 0; i<4; i++) {
                String val = mas.get(i);
                val = val.replace("\n", "").replaceAll("\\s+", "_").replace("\"", "_").replace("\'", "_").replace("\\;", "_").replace("\\:", "_");
                mas1.add(val);
            }
            mas1.add(mas.get(4));
            new_list.add(mas1);
        }
        links = new_list;
        
        new_list = new ArrayList();
        for(ArrayList<String> mas : links_extended) {
            ArrayList<String> mas1 = new ArrayList();
            for(String val : mas) {
                val = val.replace("\n", "").replaceAll("\\s+", "_").replace("\"", "_").replace("\'", "_").replace("\\;", "_").replace("\\:", "_");
                mas1.add(val);
            }
            new_list.add(mas1);
        }
        links_extended = new_list;        
        
        
        result.add(links);
        result.add(links_extended);

        return result;
    }
    
    private Map<String, String[]> GetMacNode_Iface(Map<String, Map> nodes_information) {
        // Get mac => [node, iface]
        Map<String, String[]> mac_node_iface = new HashMap();
        for(Map.Entry<String, Map> entry : nodes_information.entrySet()) {
            String node=entry.getKey();
            Map val=entry.getValue();
            Map<String, Map> interfaces=(Map)val.get("interfaces");
            if(interfaces != null && interfaces.size() > 0) {
                for(Map.Entry<String, Map> entry1 : interfaces.entrySet()) {
                    String iface_name=entry1.getKey();
                    String mac = (String)entry1.getValue().get("mac");
                    if(mac != null) {
                        mac=mac.toLowerCase().replaceAll("[\\.:-]", "");
                        String[] node_iface=new String[2];
                        node_iface[0]=node; node_iface[1]=iface_name;
                        mac_node_iface.put(mac, node_iface);
                    }
                }
            }   
        }
        return mac_node_iface;
    }
    
    private Map<String, String> GetIpFromNodes(Map<String, Map> nodes_information) {
        Map<String, String> result = new HashMap();

        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
            String node = entry.getKey();
            result.put(node, node);
            ArrayList<String> ip_list = GetIpListFromNode((Map<String, Map>)entry.getValue());
            for(String ip : ip_list) result.put(ip, node);
        }
        
        return result;
    }
    
    private Map<String, String> GetIpFromNodes(Map<String, Map> nodes_information, ArrayList<String> networks) {
        Map<String, String> result = new HashMap();

        Map<String, Integer> node_priority = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
            String node = entry.getKey();
            int pos = 0;
            boolean found = false;
            for(String network : networks) {
                if(Inside_Network(node, network)) {
                    found = true;
                    break;
                } 
                pos = pos + 1;                
            }
            if(found)
                node_priority.put(node, pos);
        }
        
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
            String node = entry.getKey();
            result.put(node, node);
            ArrayList<String> ip_list = GetIpListFromNode((Map<String, Map>)entry.getValue());
            for(String ip : ip_list) {
                if(result.get(ip) == null) 
                    result.put(ip, node);
                else
                {
                    String node_exist = result.get(ip);
                    if( (node_priority.get(node) != null && node_priority.get(node_exist) == null) ||
                        (node_priority.get(node) != null && node_priority.get(node_exist) != null && 
                            node_priority.get(node) < node_priority.get(node_exist)) ) {
                        result.remove(ip);
                        result.put(ip, node);
                        logger.Println("Replace ip_node: "+ip+", "+node_exist+" to:"+ip+", "+node, logger.DEBUG);
                    }   
                }
            }
        }
        
        return result;
    }            
    
    public ArrayList<String> GetIpListFromNode(Map<String, Map> node_information) {
        ArrayList<String> result = new ArrayList();
        
        if(node_information != null && node_information.size() > 0) {
            Map<String, Map> map_interf = (node_information).get("interfaces");
            if(map_interf != null && map_interf.size() > 0) {
                for(Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
                    ArrayList<String> list = (ArrayList)entry1.getValue().get("ip");
                    String operation_status = (String)entry1.getValue().get("operation_status");
                    if(list != null && list.size() > 0 && operation_status != null && operation_status.equals("up")) {
                        for(String ip : list) {
                            if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+")) {
                                ip=ip.split("/")[0];
                                result.add(ip);
                            }
                            else if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                ip=ip.split(" ")[0];
                                result.add(ip);
                            }
                            else if(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                result.add(ip);
                            } else System.out.println("Bad format ip address - ");
                        }
                    }
                }
            } 
        }
        return result;
    }

    public ArrayList<String> GetMACFromNode(Map<String, Map> node_information) {
        ArrayList<String> result = new ArrayList();

        Map<String, Map> map_interf = (node_information).get("interfaces");
        if(map_interf != null && map_interf.size() > 0) {
            for(Map.Entry<String, Map> entry1 : map_interf.entrySet()) {
                String mac = (String)entry1.getValue().get("mac");
                String status_iface = (String)entry1.getValue().get("operation_status");
                if(mac != null && !mac.equals("00:00:00:00:00:00") && 
                    !mac.equals("0000.0000.0000") &&
                    (mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$") ||
                    mac.matches("^[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}$")) 
                        )
                    result.add(mac);
            }
        }
        
        return result;
    }

    
    private String MacSysnameToIp(String name, Map<String, String[]> mac_node) {
        String result=name;
        
        String mac_extract=ExtractMACFromName(name);
        
        if(!mac_extract.equals("")) {
            String mac_extract1=mac_extract.replaceAll("[\\.:-]", "");
            String[] mas=mac_node.get(mac_extract1);
            if(mas != null && mas.length == 2) {
                result=mas[0];
//                System.out.println("Replace - "+mac_extract+" to - "+result);
            }
        }
        return result;
    }
    
    private String ExtractMACFromName(String name) {
        String mac_extract=name;
        name=name.toLowerCase();
        Pattern p = Pattern.compile(".*(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}).*");
        Matcher m = p.matcher(name);
        Pattern p1 = Pattern.compile(".*([0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}).*");
        Matcher m1 = p1.matcher(name);        
        Pattern p2 = Pattern.compile(".*([0-9A-Fa-f]{6}[:-][0-9A-Fa-f]{6}).*");
        Matcher m2 = p2.matcher(name);

        if(m.find()) mac_extract=m.group(1);
        else if(m1.find()) mac_extract=m1.group(1); 
        else if(m2.find()) mac_extract=m2.group(1);        
        return mac_extract;
    }
    
    private boolean CheckMACFromName(String name) {
        name=name.toLowerCase();
        Pattern p = Pattern.compile(".*(([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}).*");
        Matcher m = p.matcher(name);
        Pattern p1 = Pattern.compile(".*([0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}\\.[0-9A-Fa-f]{4}).*");
        Matcher m1 = p1.matcher(name);        
        Pattern p2 = Pattern.compile(".*([0-9A-Fa-f]{6}[:-][0-9A-Fa-f]{6}).*");
        Matcher m2 = p2.matcher(name);

        if(m.find()) return true;
        else if(m1.find()) return true; 
        else if(m2.find()) return true;        
        return false;
    }
    
    public Map AddingNodesFromLinks(Map informationFromNodes) {
        Map nodes_information = (Map)informationFromNodes.get("nodes_information");
        ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>)informationFromNodes.get("links");
        ArrayList<ArrayList<String>> links_new = new ArrayList();
        Map<String, Map> map_res = new HashMap();
//        Map<String, Integer> node_hash = new HashMap();
        for(ArrayList<String> list : links) {
            try {
                String node1=list.get(0);
                String iface1=list.get(1);
                String node2=list.get(2);
                String iface2=list.get(3);
                String info=list.get(4);
//                int hash = info.hashCode();

                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject)parser.parse(info);
                Map<String, String> info_map = (Map)toMap(jsonObject);
                
                Pattern p1 = Pattern.compile("(([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2}))");
                Pattern p2 = Pattern.compile("(([0-9A-Fa-f]{4}[.]){2}([0-9A-Fa-f]{4}))");
                String mac = "";
                for(Map.Entry<String, String> entry : ((Map<String, String>)info_map).entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    Matcher m1 = p1.matcher(val);
                    if(m1.find()) { 
                        mac=m1.group(1);
                        break;
                    }
                    else {
                        Matcher m2 = p2.matcher(val);
                        if(m2.find()) {
                            mac=m2.group(1);
                            break;
                        }                        
                    }
                }
                
                
                Map<String, String> map1 = new HashMap();
                String sysname = (String)info_map.get("remote_id");
                if(sysname.equals("0")) sysname = null;
                if(sysname != null) map1.put("sysname", sysname);
                String model = (String)info_map.get("remote_version");
                if(model != null) {
                    if(model.equals("0")) model = null;
                    if(model != null) map1.put("model", model);
                }
                if(!mac.equals("")) map1.put("base_address", mac);
                Map<String, Map> map2 = new HashMap();
                map2.put("general", map1);

                Map<String, Map> map4 = new HashMap();
                map4.put(iface2, new HashMap());
                map2.put("interfaces", map4);
                
                if(map_res.get(node2) != null && nodes_information.get(node2) == null) {
                    String base_address = (String)((Map)map_res.get(node2).get("general")).get("base_address");
                    String sysname_prev = (String)((Map)map_res.get(node2).get("general")).get("sysname");
                    if(
                            base_address != null && !mac.equals("") && 
                            !(base_address.replace(":", "").replace(".", "").toLowerCase()).equals(mac.replace(":", "").replace(".", "").toLowerCase())
                            ) {
                        node2 = node2+"("+mac+")";
                    } else if(
                            (sysname != null && sysname_prev == null) ||
                            (sysname == null && sysname_prev != null) ||
                            (sysname != null && sysname_prev != null && !sysname.equals(sysname_prev))
                            ) {
                        node2 = node2+"("+sysname+")";
                    }                 } 
                ArrayList<String> link = new ArrayList();
                link.add(node1); link.add(iface1);
                link.add(node2); link.add(iface2);
                link.add(info);
                links_new.add(link);
                
                int score = 0;
                if(sysname != null) score = 4;
                if(model != null) score = score + 2;
                
                int score1 = 0;
                if(map_res.get(node2) != null) {
                    if(((Map)(map_res.get(node2))).get("general") != null) {
                        String sysname1 = (String)((Map)((Map)(map_res.get(node2))).get("general")).get("sysname");
                        if(sysname1 != null) score1 = 4;
                        String model1 = (String)((Map)((Map)(map_res.get(node2))).get("general")).get("model");
                        if(model1 != null) score1 = score1 + 2;
                    }
                }
                
                int score2 = 0;
                if(nodes_information.get(node2) != null) {
                    Map general = (Map)((Map)nodes_information.get(node2)).get("general");
                    if(general != null) {
                        String sysname1 = (String)((Map)((Map)(nodes_information.get(node2))).get("general")).get("sysname");
                        if(sysname1 != null) score2 = 4;
                        String model1 = (String)((Map)((Map)(nodes_information.get(node2))).get("general")).get("model");
                        if(model1 != null) score2 = score2 + 2;
                        String sysDescription = (String)((Map)((Map)(nodes_information.get(node2))).get("general")).get("sysDescription");
                        if(sysDescription != null) score2 = score2 + 3; 
                        score2 = score2 + general.size();
                    }
                }                

                if(
                        (nodes_information.get(node2) == null && map_res.get(node2) == null) ||
                        (nodes_information.get(node2) == null && map_res.get(node2) != null && score > score1) ||
                        (nodes_information.get(node2) != null && map_res.get(node2) == null && score > score2) ||
                        (nodes_information.get(node2) != null && map_res.get(node2) != null && score > score1 && score > score2)
                ) {
                    map_res.put(node2, map2);
//                    node_hash.put(node2, info.hashCode());
                    logger.Println("Adding node "+node2+" from links.", logger.DEBUG);
                }
            } catch (Exception ex) {
                if(DEBUG) System.out.println(ex);
                ex.printStackTrace();
                System.out.println(Utils.class.getName() + " - " + ex);
            }                   
        }
        nodes_information.putAll(map_res);
        informationFromNodes.put("links", links_new);
//            System.out.println("1111111");        
        
        return informationFromNodes;
    }
    
    public String[] FindIfaceFromNode(String iface, ArrayList<ArrayList<String>> ifaceid_ifacename_list) {
        String[] result = new String[2];
        
        if(ifaceid_ifacename_list != null) {
            for(ArrayList<String> ifaceid_ifacename : ifaceid_ifacename_list) {
                if(iface.matches("\\d+")) {
                    if(iface.equals(ifaceid_ifacename.get(0))) {
                        result[0]=ifaceid_ifacename.get(0);
                        result[1]=ifaceid_ifacename.get(1);
                        break;                        
                    }
                }
                else if(iface.equals(ifaceid_ifacename.get(1))) {
                    result[0]=ifaceid_ifacename.get(0);
                    result[1]=ifaceid_ifacename.get(1);
                    break;                        
                }                
                else if(EqualsIfaceName(ifaceid_ifacename.get(1), iface)) {
                    result[0]=ifaceid_ifacename.get(0);
                    result[1]=ifaceid_ifacename.get(1);
                    break;
                }
            }
//            if(result[0] == null)
//                result = FindIfaceFromNodeLevenstein(iface, ifaceid_ifacename_list);
        }
        return result;
    }
    
    private boolean EqualsIfaceName(String str1, String str2) {
        String short_iface="";
        String full_iface="";
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
    
//    private String[] FindIfaceFromNodeLevenstein(String iface, ArrayList<ArrayList<String>> ifaceid_ifacename_list) {
//        ArrayList<ArrayList> list = new ArrayList();
//        ArrayList id_iface_distance_list = new ArrayList();
//        for(ArrayList<String> ifaceid_ifacename : ifaceid_ifacename_list) {
//            ArrayList id_iface_distance = new ArrayList();
//            id_iface_distance.add(ifaceid_ifacename.get(0));
//            id_iface_distance.add(ifaceid_ifacename.get(1));
//            id_iface_distance.add(LevensteinEquals(iface, (String)id_iface_distance.get(1)));
//            id_iface_distance_list.add(id_iface_distance);
//        }
//        int max_distance=0;
//        String[] id_iface = new String[2];
//        for(ArrayList id_iface_distance : (ArrayList<ArrayList>)id_iface_distance_list) {
//            int distance = (int)id_iface_distance.get(2);
//            if(distance > max_distance) {
//                max_distance=distance;
//                id_iface[0]=(String)id_iface_distance.get(0);
//                id_iface[1]=(String)id_iface_distance.get(1);
//            }
//        }
//        System.out.println(iface+" - "+id_iface[1]);
//        int count=0;
//        for(ArrayList id_iface_distance : (ArrayList<ArrayList>)id_iface_distance_list) {
//            int distance = (int)id_iface_distance.get(2);
//            if(max_distance == distance) count=count+1;
//        }
//        if(count > 1) {
//            id_iface[0]=null;
//            id_iface[1]=null;
//        }
//        
//        return id_iface;
//    }
//    
//    private int LevensteinEquals(String S1, String S2) {
//            S1=S1.replace("/", "").replace("\\", "").replace("-", "").replace(".", "").replace("_", "").toLowerCase();
//            S2=S2.replace("/", "").replace("\\", "").replace("-", "").replace(".", "").replace("_", "").toLowerCase();
//            if(S1.length() > S2.length()) {
//                String str_tmp=S1;
//                S1=S2;
//                S2=str_tmp;
//            }
//            int m = S1.length(), n = S2.length();
//            int[] D1;
//            int[] D2 = new int[n + 1];
//
//            for(int i = 0; i <= n; i ++)
//                    D2[i] = i;
//
//            for(int i = 1; i <= m; i ++) {
//                    D1 = D2;
//                    D2 = new int[n + 1];
//                    for(int j = 0; j <= n; j ++) {
//                            if(j == 0) D2[j] = i;
//                            else {
//                                    int cost = (S1.charAt(i - 1) != S2.charAt(j - 1)) ? 1 : 0;
//                                    if(D2[j - 1] < D1[j] && D2[j - 1] < D1[j - 1] + cost)
//                                            D2[j] = D2[j - 1] + 1;
//                                    else if(D1[j] < D1[j - 1] + cost)
//                                            D2[j] = D1[j] + 1;
//                                    else
//                                            D2[j] = D1[j - 1] + cost;
//                            }
//                    }
//            }
//            int result=n-D2[n];
//            return result;
//    }      
    
    // remove duplicate nodes in all areas am mac addresses, ip list and sysname
    public Map<String, Map> RemoveDuplicateNodes(Map<String, Map> informationFromNodesAllAreas, Map<String, ArrayList<String>> area_networks) {
        Map<String, String> mac_excluded = new HashMap();
        for(Map.Entry<String, Map> area : ((Map<String, Map>)informationFromNodesAllAreas).entrySet()) {
            String area_name=area.getKey();
            Map val=area.getValue();
            Map<String, Map> nodes_information = (Map<String, Map>)val.get("nodes_information");
            if(nodes_information != null) {
                Map<String, Integer> node_priority = new HashMap();
                for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
                    String node = entry.getKey();
                    int pos = 0;
                    boolean found = false;
                    ArrayList<String> networks = area_networks.get(area_name);
                    for(String network : networks) {
                        if(Inside_Network(node, network)) {
                            found = true;
                            break;
                        } 
                        pos = pos + 1;                
                    }
                    if(found)
                        node_priority.put(node, pos);
                }
        
                Map<String, Map> nodes_information_new = new HashMap();
                for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
                    String node = entry.getKey();
//                    if(node.equals("10.32.47.254") || node.equals("10.32.71.254"))
//                        System.out.println("111111");
                    Map<String, Map> node_information = entry.getValue();
                    ArrayList<String> mac_list = GetMACFromNode(node_information);
                    String mac_finded="";
                    int mac_count = 0;
                    for(String mac : mac_list) {
                        mac = mac.replace(":", "").replace(".", "").toLowerCase();
                        if(mac_excluded.get(mac) != null) {
                            mac_count = mac_count + 1;
                            mac_finded=mac;
                        }
                    }
                    boolean mac_find=false;
                    if(mac_count != 0 && mac_count == mac_list.size())
                        mac_find=true;
                    if(!mac_find) {
                        nodes_information_new.put(node, node_information);
//                        ArrayList list = new ArrayList();
//                        list.add(node);
//                        list.add(area_name);
//                        list.add(node_information);
                        for(String mac : mac_list) {
                            mac_excluded.put(mac.replace(":", "").replace(".", "").toLowerCase(), node);
                        }
                    } else {
                        String node_exist = mac_excluded.get(mac_finded);
                        if( (node_priority.get(node) != null && node_priority.get(node_exist) == null) ||
                            (node_priority.get(node) != null && node_priority.get(node_exist) != null && 
                                node_priority.get(node) < node_priority.get(node_exist)) ) {
                            nodes_information_new.remove(node_exist);
                            nodes_information_new.put(node, node_information);
                            logger.Println("Replace nodes_information_new: "+node_exist+" to: "+node, logger.DEBUG);
                        }      
                    
//                        logger.Println("Dublicate node="+node+" to: "+node_exist, logger.DEBUG);
                    }
                }
                val.remove("nodes_information");
                val.put("nodes_information", nodes_information_new);
            }
            // remove links
            nodes_information = (Map<String, Map>)val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>)val.get("links");
            if(links != null && nodes_information != null) {
                ArrayList<ArrayList<String>> links_new = new ArrayList();
                for(ArrayList<String> link : links) {
                    String node1=link.get(0);
                    String iface1=link.get(1);
                    String node2=link.get(2);
                    String iface2=link.get(3); 
                    String extended=link.get(4);
                    boolean find=false;
                    for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_information).entrySet()) {
                        String node = entry.getKey();
                        if(node.equals(node1) || node.equals(node2)) {
                            links_new.add(link);
                            find=true;
                            break;
                        }
                    }
                    if(!find) 
                        logger.Println("Remove link: "+node1+", "+iface1+" <---> "+node2+","+iface2, logger.DEBUG);
                }
                val.remove("links");
                val.put("links", links_new);                
            }
        } 
        return informationFromNodesAllAreas;
    }
    
    public Map AddingIfaceIdToLinks(Map informationFromNodesAllAreas, Map<String, Map<String, ArrayList<ArrayList<String>>>> area_node_ifaceid_ifacename) {
        // adding ifacaid to links
        for(Map.Entry<String, Map> area : ((Map<String, Map>)informationFromNodesAllAreas).entrySet()) {
            String area_name=area.getKey();
            Map<String, Object> val=area.getValue();
            Map<String, ArrayList<ArrayList<String>>> node_ifacename_ifaceid = area_node_ifaceid_ifacename.get(area_name);
            if(node_ifacename_ifaceid != null) {
                ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>)val.get("links");
                ArrayList<ArrayList<String>> links_new = new ArrayList();
                if(links != null) {
                    for(ArrayList<String> link : links) {
                        ArrayList<ArrayList<String>> ifaceid_ifacename_list = node_ifacename_ifaceid.get(link.get(0));
                        ArrayList<String> link_tmp = new ArrayList();
                        if(ifaceid_ifacename_list != null && ifaceid_ifacename_list.size() > 0) {
                            String[] if_name_iface1 = FindIfaceFromNode(link.get(1), ifaceid_ifacename_list);
                            if(if_name_iface1[0] != null) {
                                link_tmp.add(link.get(0)); link_tmp.add(if_name_iface1[0]); link_tmp.add(if_name_iface1[1]);
                            } else
                            {
                                link_tmp.add(link.get(0)); link_tmp.add(""); link_tmp.add(link.get(1));
//                                System.out.println("ID iface not found: "+link.get(0)+", "+link.get(1));                                
                            }
                        } else {
                            link_tmp.add(link.get(0)); link_tmp.add(""); link_tmp.add(link.get(1));
                        }

                        ifaceid_ifacename_list = node_ifacename_ifaceid.get(link.get(2));
                        if(ifaceid_ifacename_list != null && ifaceid_ifacename_list.size() > 0) {
                            String[] if_name_iface2 = FindIfaceFromNode(link.get(3), ifaceid_ifacename_list);
                            if(if_name_iface2[0] != null) {
                                link_tmp.add(link.get(2)); link_tmp.add(if_name_iface2[0]); link_tmp.add(if_name_iface2[1]);
                            } else {
                                link_tmp.add(link.get(2)); link_tmp.add(""); link_tmp.add(link.get(3));
//                                System.out.println("ID iface not found: "+link.get(2)+", "+link.get(3));
                            }
                        } else {
                            link_tmp.add(link.get(2)); link_tmp.add(""); link_tmp.add(link.get(3));
                        }
                        link_tmp.add(link.get(4));
                        links_new.add(link_tmp);
                    }
                }
                val.put("links", links_new);
            }
        }
        return informationFromNodesAllAreas;
    }
    
    public Map<String, Map<String, ArrayList<ArrayList<String>>>> GetAreaNodeIdIface(Map informationFromNodesAllAreas, Map<String, ArrayList<String[]>> node_community_version) {
        Map<String, Map<String, ArrayList<ArrayList<String>>>> result = new HashMap();
//        ru.kos.neb.neb_lib.Utils.DEBUG = true;
        for(Map.Entry<String, Map> area : ((Map<String, Map>)informationFromNodesAllAreas).entrySet()) {
            String area_name=area.getKey();
            Map<String, Map> val=area.getValue();   
            ArrayList<String[]> node_community_version_area = node_community_version.get(area_name);
            if(node_community_version_area != null && node_community_version_area.size() > 1) {                            
                Map<String, ArrayList<ArrayList<String>>> node_ifacename_ifaceid = GetNode_Ifaceid_Ifacename(node_community_version_area);
                result.put(area_name, node_ifacename_ifaceid);
            }
        } 
//        ru.kos.neb.neb_lib.Utils.DEBUG = false;
        return result;
    }
    
    public Map<String, ArrayList<String[]>> GetAreaNodeCommunityVersion(Map informationFromNodesAllAreas) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        for(Map.Entry<String, Map> area : ((Map<String, Map>)informationFromNodesAllAreas).entrySet()) {
            String area_name=area.getKey();
            Map val=area.getValue();
            
            Map nodes_information=(Map)val.get("nodes_information");
            Map node_protocol_accounts=(Map)val.get("node_protocol_accounts");
            if(nodes_information != null && nodes_information.size() > 0 &&
               node_protocol_accounts != null && node_protocol_accounts.size() > 0 ) {
                ArrayList<String[]> node_community_version = new ArrayList();
                for(Map.Entry<String, Map> map_tmp : ((Map<String, Map>)nodes_information).entrySet()) {
                    String node=map_tmp.getKey();
                    if(node_protocol_accounts.get(node) != null) {
                        for(ArrayList<String> it : (ArrayList<ArrayList<String>>)node_protocol_accounts.get(node)) {
                            if(it.get(0).equals("snmp")) {
                                String[] mas = new String[3];
                                mas[0]=node; mas[1]=it.get(1); mas[2]=it.get(2);
                                node_community_version.add(mas);
                            }
                        }
                    }
                }
                result.put(area_name, node_community_version);
            } else {
                   logger.Println(area_name+" - is not node_protocol_accounts!!!", logger.DEBUG);
//                   System.out.println(area_name+" - is not node_protocol_accounts!!!");
            }
        } 
        return result;
    }
    
    
    public Map<String, ArrayList<String[]>> GetAreaNodeCommunityVersionDP(Map informationFromNodesAllAreas) {
        Map<String, ArrayList<String[]>> result = new HashMap();
        for(Map.Entry<String, Map> area : ((Map<String, Map>)informationFromNodesAllAreas).entrySet()) {
            String area_name=area.getKey();
            Map val=area.getValue();
            
            Map<String, ArrayList<String>> nodes_dp_tmp = new HashMap();
            ArrayList<ArrayList<String>> links=(ArrayList<ArrayList<String>>)val.get("links");
            if(links != null && links.size() > 0) {
                JSONParser parser = new JSONParser();
                for(ArrayList<String> list : links) {
//                    String ext = "";
                    try {
                        String node1=null;
                        String node2=null;
                        String ext=null;
                        if(list.size() == 5) {
                            node1=list.get(0);
                            node2=list.get(2);
                            ext = list.get(4);
                        } else if(list.size() == 7) {
                            node1=list.get(0);
                            node2=list.get(3);
                            ext = list.get(6);
                        }
                        if(node1 != null && node2 != null && ext != null) {
                            JSONObject jsonObject = (JSONObject)parser.parse(ext);                
                            Map map_tmp = toMap(jsonObject);
                            String type = (String)map_tmp.get("type");
                            if(type != null) {
                                if(type.equals("cdp")) {
                                    ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                    if(list1 != null) list1.add("cdp");
                                    else { list1 = new ArrayList(); list1.add("cdp"); }
                                    nodes_dp_tmp.put(node1, list1);
                                    list1 = nodes_dp_tmp.get(node2);
                                    if(list1 != null) list1.add("cdp");
                                    else { list1 = new ArrayList(); list1.add("cdp"); }                            
                                    nodes_dp_tmp.put(node2, list1);
                                } else if(type.equals("lldp")) {
                                    ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                    if(list1 != null) list1.add("lldp");
                                    else { list1 = new ArrayList(); list1.add("lldp"); }
                                    nodes_dp_tmp.put(node1, list1);
                                    list1 = nodes_dp_tmp.get(node2);
                                    if(list1 != null) list1.add("lldp");
                                    else { list1 = new ArrayList(); list1.add("lldp"); }                            
                                    nodes_dp_tmp.put(node2, list1);                           
                                } else {
                                    ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                    if(list1 != null) list1.add("");
                                    else { list1 = new ArrayList(); list1.add(""); }
                                    nodes_dp_tmp.put(node1, list1);
                                    list1 = nodes_dp_tmp.get(node2);
                                    if(list1 != null) list1.add("");
                                    else { list1 = new ArrayList(); list1.add(""); }                            
                                    nodes_dp_tmp.put(node2, list1);                            
                                }
                            } else {
                                ArrayList<String> list1 = nodes_dp_tmp.get(node1);
                                if(list1 != null) list1.add("");
                                else { list1 = new ArrayList(); list1.add(""); }
                                nodes_dp_tmp.put(node1, list1);
                                list1 = nodes_dp_tmp.get(node2);
                                if(list1 != null) list1.add("");
                                else { list1 = new ArrayList(); list1.add(""); }                            
                                nodes_dp_tmp.put(node2, list1); 
                            }
                        }
    //                    System.out.println("1111111111111");
                    } catch (ParseException ex) {
//                        logger.Println("Error translate to JSON string : "+ext+" ...", logger.DEBUG);
//                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
            Map<String, String> nodes_dp = new HashMap();
            for(Map.Entry<String, ArrayList<String>> entry : nodes_dp_tmp.entrySet()) {
                String node = entry.getKey();
                ArrayList<String> list = entry.getValue();
                boolean cdp=false; boolean lldp=false;
                for(String str : list) {
                    if(str.equals("cdp")) cdp=true;
                    else if(str.equals("lldp")) lldp=true;
                }
                String dp = "";
                if(cdp && lldp) dp="cdp,lldp";
                else if(cdp && !lldp) dp="cdp";
                else if(!cdp && lldp) dp="lldp";
                nodes_dp.put(node, dp);
            }
            
            Map nodes_information=(Map)val.get("nodes_information");
            Map node_protocol_accounts=(Map)val.get("node_protocol_accounts");
            if(nodes_information != null && nodes_information.size() > 0 &&
               node_protocol_accounts != null && node_protocol_accounts.size() > 0 ) {
                ArrayList<String[]> node_community_version_dp = new ArrayList();
                for(Map.Entry<String, Map> map_tmp : ((Map<String, Map>)nodes_information).entrySet()) {
                    String node=map_tmp.getKey();
                    if(node_protocol_accounts.get(node) != null) {
                        for(ArrayList<String> it : (ArrayList<ArrayList<String>>)node_protocol_accounts.get(node)) {
                            if(it.get(0).equals("snmp")) {
                                String[] mas = new String[4];
                                mas[0]=node; mas[1]=it.get(1); mas[2]=it.get(2);
                                if(nodes_dp.get(node) != null) mas[3]=nodes_dp.get(node);
                                else mas[3]="";
                                node_community_version_dp.add(mas);
                            }
                        }
                    }
                }
                result.put(area_name, node_community_version_dp);
            } else {
                   logger.Println(area_name+" - is not node_protocol_accounts!!!", logger.DEBUG);
//                   System.out.println(area_name+" - is not node_protocol_accounts!!!");
            }
        } 
        return result;
    }
    
    private Map<String, ArrayList<ArrayList<String>>> TranslateNodeProtocolAccountsToList(Map<String, ArrayList<String[]>> node_protocol_accounts) {
        // translate node_protocol_accounts Array to ArrayList
        Map<String, ArrayList<ArrayList<String>>> node_protocol_accounts_new = new HashMap();
        for(Map.Entry<String, ArrayList<String[]>> entry : ((Map<String, ArrayList<String[]>>)node_protocol_accounts).entrySet()) {
            String node = entry.getKey();
            ArrayList<String[]> val = entry.getValue();
            ArrayList<ArrayList<String>> val1 = new ArrayList();
            for(String[] mas : val) {
                ArrayList<String> s_list = new ArrayList();
                for(String s : mas) s_list.add(s);
                val1.add(s_list);
            }
            node_protocol_accounts_new.put(node, val1);
        }
        return node_protocol_accounts_new;
    }
    
//    public ArrayList<String[]> ReadARP_Mac_FromNodes(String filename) {
//        ArrayList<String[]> result = new ArrayList();
//        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(filename));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    String[] buf = s.split(",");
//                    if(buf.length == 5) {
//                        result.add(buf);
//                    }
//                }
//                
//
//            } finally {
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }
//        return result;
//    } 
    
//    public String RunRequest(String str) {
//        String result = "";
//        
//        Object result_map = new HashMap();
//        String[] mas = str.split("\\s+");
//        if(mas.length > 0) {
//            String command = mas[0];
//            ArrayList<String> params = new ArrayList();
//            for(int i=1; i<mas.length; i++) params.add(mas[i]);
//            if(command.equals("GET")) {
//                if(params.size() == 0) result_map=Neb.INFORMATION;
//                else {
//                    String param1=params.get(0);
//                    result_map=GetKey(param1);
//                }
//                System.out.println("11111111111111");
//            }
//        }
//        return result;
//    }
    
    public Object GetKey(String key_str, Map MAP_INFO) {
        Object result = null;
        
        String[] keys = key_str.split("/");
        if(keys.length == 0) result=MAP_INFO;
        else {
            if(keys[0].equals("")) {
                Map value_map = (Map)MAP_INFO;
                Object value = null;
                String key = "";
                for(int i=1; i< keys.length; i++) {
                    key = keys[i];
                    if(value_map.get(key) instanceof Map) 
                        value_map = (Map)value_map.get(key);
                    else if(value_map.get(key) == null)
                        return null;
                    else { value = value_map.get(key); break; }
                }
                if(key.equals(keys[keys.length-1])) {
                    if(value == null) {
                        result=value_map;
                    } else result=value;
                }
            }

        }
        return result;
    }
    
    public String GetKeyList(String key_str, Map MAP_INFO) {
        String out = "";
        String[] keys = key_str.split("/", -1);
        if(keys.length > 0) {
            if(keys[0].equals("")) {
                Map value_map = (Map)MAP_INFO;
                Object value = null;
                String key = "";
                for(int i=1; i< keys.length; i++) {
                    key = keys[i];
                    if(value_map.get(key) instanceof Map) 
                        value_map = (Map)value_map.get(key);
                    else 
                        break;
                }
                if(key.equals(keys[keys.length-1])) {
                    for(Map.Entry<String, Object> entry : ((Map<String, Object>)value_map).entrySet()) {
                        key = entry.getKey();
                        out=out+"\n"+key;
                    }                  
                }
            }

        }
        return out.trim();
    }    
    
    public boolean SetKey(String key_str, Object val, Map MAP_INFO) {
        boolean result = false;
        
        String[] parent_key_str = GetParentKeyAndKey(key_str, MAP_INFO);
        if(parent_key_str != null) {
            Object parent_key = GetKey(parent_key_str[0], MAP_INFO);
            if(parent_key instanceof Map) {
                if(parent_key != null) {
                    ((Map)parent_key).put(parent_key_str[1], val);
                    result = true;
                }
            }
        }

        return result;
    }
    
    public boolean DeleteKey(String key_str, Map MAP_INFO) {
        boolean result = false;
        
        String[] parent_key_str = GetParentKeyAndKey(key_str, MAP_INFO);
        if(parent_key_str != null) {
            Map parent_key = (Map)GetKey(parent_key_str[0], MAP_INFO);
            String key = parent_key_str[1];
            if(parent_key != null) {
                if(parent_key.get(key) != null) {
                    parent_key.remove(key);
                    result = true;
                } else
                    result = false;
            }
        }

        return result;
    }    
    
    private Object StrToObject(String val) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        Map map = new HashMap();
        List list = new ArrayList();
        
        try {
            Object json = parser.parse(val);
            if(json instanceof JSONObject) { 
                map = toMap((JSONObject)json); 
                return map; 
            }
            else if(json instanceof JSONArray) { 
                list = toList((JSONArray)json); 
                return list; 
            }
        }
        catch (Exception excep) {
            return val;
        }
        return null;
    }
    
    public boolean AddToList(String key_str, String val, Map MAP_INFO) {
        boolean result = false;
        
        Object obj = StrToObject(val);
        
        Object value = GetValueKey(key_str, MAP_INFO);
        if(value instanceof ArrayList || value instanceof List) {
            ((ArrayList) value).add(obj);
            result = true;
        } else {
            result = false;
        }

        return result;
    }      
    
    public boolean DelFromList(String key_str, String val, Map MAP_INFO) {
        boolean result = false;

        Object value = GetValueKey(key_str, MAP_INFO);
        if(value instanceof ArrayList || value instanceof List) {
            ArrayList<Integer> del_row = new ArrayList();
            for(int i=((ArrayList)value).size()-1; i >= 0; i--) {
                Object iter = ((ArrayList)value).get(i);
                if(iter instanceof ArrayList || iter instanceof List) {
                    String[] fields = val.split("\\;");
                    boolean found = true;
                    int num_field = 0;
                    for(String feild : fields) {
                        if(!feild.equals("") && !feild.equals(((ArrayList)iter).get(num_field))) {
                            found=false;
                            break;
                        }
                        num_field++;
                    }
                    if(found) 
                        del_row.add(i);
                } else if(iter instanceof String) {
                    if(((String)iter).equals(val)) 
                        del_row.add(i);
                }
                
            }
            
            if(del_row.size() > 0) {
                for(int row : del_row)
                    ((ArrayList)value).remove(row);
                result = true;
            } else
                result = false;
        } else {
            result = false;
        }

        return result;
    }
    
    private String[] GetParentKeyAndKey(String key_str, Map MAP_INFO) {
        String[] result = new String[2];
        
        String parent = "";
        String s = key_str.substring(key_str.length()-1);
        if(s.equals("/"))
            key_str = key_str.substring(0, key_str.length()-1);
        String[] mas = key_str.split("/");
        
        if(mas[0].equals("") && mas.length > 1) {
            for(int i = 1; i<mas.length-1; i++) {
                String iter = mas[i];
                parent = parent +"/"+iter;
            }


            if(parent.equals("")) parent = "/";

            String key = mas[mas.length-1];

            result[0] = parent;
            result[1] = key;
        } else 
            result = null;
        
        return result;
    }
    
    private Object GetValueKey(String key_str, Map MAP_INFO) {
        Object result = null;
        
        String[] keys = key_str.split("/", -1);
        if(keys.length > 1) {
            if(keys[1].equals("")) {
                result = (Map)MAP_INFO;
            } else if(keys.length > 0) {
                if(keys[0].equals("")) {
                    Object value = MAP_INFO;
                    String key = "";
    //                int depth = 0;
                    for(int i=1; i< keys.length; i++) {
                        key = keys[i];
                        if(((Map)value).get(key) != null) {
                            if(((Map)value).get(key) instanceof Map) {
                                value = ((Map)value).get(key);
                                if(i == keys.length-1) {
                                    result = value;
                                    break;                                
                                }
                            } else {
                                result = ((Map)value).get(key);
                                break;
                            }
                        }
                        else {
                            result = null;
                            break;
                        }
                    } 
                }
            }
        }
        return result;
    }    
    
    private ArrayList<String[]> RunScriptCliTest(ArrayList<String> ip_list, Map<String, String> exclude_list, ArrayList<ArrayList<String>> accounts_list, int timeout, int retries, Map<String, String[]> cli_accounts_priority) {
        ArrayList<String[]> cli_node_account = new ArrayList();
        
        Map<String, ArrayList<String[]>> cli_accounts_list_priority = new HashMap();
        if(cli_accounts_priority != null) {
            for(Map.Entry<String, String[]> entry : ((Map<String, String[]>)cli_accounts_priority).entrySet()) {
                String node = entry.getKey();
                if(ip_list.contains(node)) {
                    ArrayList tmp_list = new ArrayList();
                    tmp_list.add(entry.getValue());
                    cli_accounts_list_priority.put(node, tmp_list);
                }
            }
        }
        
        Map<String, ArrayList<String[]>> node_protocol_accounts = new HashMap();
        for (String ip : ip_list) {
            if(exclude_list == null || exclude_list.get(ip) == null) {
                ArrayList<String[]> list = new ArrayList();
                if(node_protocol_accounts.get(ip) != null) 
                    list = node_protocol_accounts.get(ip);

                for(ArrayList<String> acount : accounts_list) {
                    if(acount.size() == 2) {
                        String[] mas1 = new String[4];
                        mas1[0]="ssh"; mas1[1]=acount.get(0); mas1[2]=acount.get(1); mas1[3]="";
                        list.add(mas1);
                        mas1 = new String[4];
                        mas1[0]="telnet"; mas1[1]=acount.get(0); mas1[2]=acount.get(1); mas1[3]="";
                        list.add(mas1);
                    } else if(acount.size() == 3) {
                        String[] mas1 = new String[4];
                        mas1[0]="ssh"; mas1[1]=acount.get(0); mas1[2]=acount.get(1); mas1[3]=acount.get(2);
                        list.add(mas1);
                        mas1 = new String[4];
                        mas1[0]="telnet"; mas1[1]=acount.get(0); mas1[2]=acount.get(1); mas1[3]=acount.get(2);
                        list.add(mas1);
                    }
                }
                node_protocol_accounts.put(ip, list);
            }
        }

        Map<String, ArrayList<String>> scripts = new HashMap();
        ArrayList<String> list_tmp = new ArrayList();
        list_tmp.add("java -jar -Xmx4M Scripts/Cli_test.jar");
        scripts.put("ssh", list_tmp);
        list_tmp = new ArrayList();
        list_tmp.add("java -jar -Xmx4M Scripts/Cli_test.jar");
        scripts.put("telnet", list_tmp);        

        if(cli_accounts_priority != null) {
            RunScriptsPool runScriptsPool1 = new RunScriptsPool(Neb.timeout_process, Neb.timeout_output, Neb.MAX_RUNSCRIPT_CLI_TEST);
            ArrayList<String> out1 = runScriptsPool1.Get(cli_accounts_list_priority, scripts, timeout, retries);                    
            for(String str : out1) {
                try {
                    JSONParser parser = new JSONParser();
                    logger.Println(str, logger.DEBUG);
                    JSONObject jsonObject = (JSONObject)parser.parse(str);
                    Map<String, Object> map = toMap(jsonObject); 
                    if(map.containsKey("node") && map.containsKey("protocol") &&
                            map.containsKey("user") && map.containsKey("passwd") &&
                            map.containsKey("enable_passwd")) {
                        String[] mas1 = new String[5];
                        mas1[0]=(String)map.get("node");
                        mas1[1]=(String)map.get("protocol");
                        mas1[2]=(String)map.get("user");
                        mas1[3]=(String)map.get("passwd");
                        mas1[4]=(String)map.get("enable_passwd");
                        cli_node_account.add(mas1);
                    }
                } catch (ParseException ex) {
                    if(DEBUG) System.out.println(ex);
                    ex.printStackTrace();
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }        
            ////////////////////////////////////////////////////////
            Map<String, ArrayList<String[]>> node_protocol_accounts_new = new HashMap();
            for(Map.Entry<String, ArrayList<String[]>> entry : ((Map<String, ArrayList<String[]>>)node_protocol_accounts).entrySet()) {        
                String node = entry.getKey();
                ArrayList<String[]> val = entry.getValue();
                boolean find = false;
                for(String[] item : cli_node_account) {
                    if(item[0].equals(node)) {
                        find=true;
                        break;
                    }
                }
                if(!find) node_protocol_accounts_new.put(node, val);
            }
            node_protocol_accounts=node_protocol_accounts_new;
        }
        /////////////////////////////////////////////////////////
        if(node_protocol_accounts.size() > 0) {
            RunScriptsPool runScriptsPool = new RunScriptsPool(Neb.timeout_process, Neb.timeout_output, Neb.MAX_RUNSCRIPT_CLI_TEST);
            ArrayList<String> out = runScriptsPool.Get(node_protocol_accounts, scripts, timeout, retries);                    
            for(String str : out) {
                try {
                    JSONParser parser = new JSONParser();
                    logger.Println(str, logger.DEBUG);
                    JSONObject jsonObject = (JSONObject)parser.parse(str);
                    Map<String, Object> map = toMap(jsonObject); 
                    if(map.containsKey("node") && map.containsKey("protocol") &&
                            map.containsKey("user") && map.containsKey("passwd") &&
                            map.containsKey("enable_passwd")) {
                        String[] mas1 = new String[5];
                        mas1[0]=(String)map.get("node");
                        mas1[1]=(String)map.get("protocol");
                        mas1[2]=(String)map.get("user");
                        mas1[3]=(String)map.get("passwd");
                        mas1[4]=(String)map.get("enable_passwd");
                        cli_node_account.add(mas1);
                    }
                } catch (ParseException ex) {
                    if(DEBUG) System.out.println(ex);
                    ex.printStackTrace();
                    Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return cli_node_account;
    }
    
    public ArrayList<String> ReadFileToList(String filename) {
        ArrayList<String> result_list = new ArrayList();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            try {
                String s;
                while ((s = in.readLine()) != null) {
                    result_list.add(s);
//                    System.out.println(s);
                }
            } finally {
                in.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return result_list;
    }
    
    public String ReadFileToString(String filename) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            try {
                sb.append(in.readLine());
                String s;
                while ((s = in.readLine()) != null) {
                    sb.append("\n"+s);
                }
            } finally {
                in.close();
            }
        } catch(IOException e) {
            if(DEBUG) System.out.println(e);
            e.printStackTrace();
//            throw new RuntimeException(e);
        }
        return sb.toString();
    }    
    
    public static String[] extended_spliter(String str, String delimiter) {
        str = str.replace("\\"+delimiter, "<delimiter>");
        String[] mas = str.split(";");
        int i=0;
        for(String it : mas) {
            mas[i] = it.replace("<delimiter>", delimiter);
            i++;
        }
        return mas;
    } 
    
    public Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if(query != null) {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                }else{
                    result.put(entry[0], "");
                }
            }
        }
        return result;
    } 
    
    public Object[] StringToArray(String str) {
        str = str.replace("\\,", "<_QWERTY_>");
        Pattern p = Pattern.compile("^\\s*\\[(.*)\\]\\s*$");
        Matcher m = p.matcher(str);
        Object[] res = null;
        if(m.find()){  
            String array=m.group(1); 
            String[] mas = array.split(",");
            res = new Object[mas.length];
            int i = 0;
            for(String it : mas) {
                it = it.replace("<_QWERTY_>", ",");
                Pattern p1 = Pattern.compile("^\\s*(.*?)\\s*$");
                Matcher m1 = p1.matcher(it);
                String val_str = "";
                if(m1.find()) val_str=m1.group(1);
                Pattern p2 = Pattern.compile("^\\\"(.*)\\\"$");
                Matcher m2 = p2.matcher(val_str);
                if(m2.find()) {
                    res[i]=m2.group(1);
                } else {
                    if(val_str.matches("\\d+\\.\\d*")) res[i] = Float.valueOf(val_str);
                    else if(val_str.matches("\\d+")) res[i] = Long.valueOf(val_str);
                }
                i++;
            }
        }
        Object[] result = null;
        if(res != null) {
            boolean ok = true;
            for(Object it : res) {
                if(it == null) {
                    ok = false;
                    break;
                }
            }
            if(ok) {
                result = new Object[res.length];
                result = res;        
            }
        }
        return result;
    }
    
//    public Object StringToValue(String str) {
//        Object result = null;
//        Pattern p1 = Pattern.compile("^\\s*(.*?)\\s*$");
//        Matcher m1 = p1.matcher(str);
//        String val_str = "";
//        if(m1.find()) 
//            val_str=m1.group(1);
//        Pattern p2 = Pattern.compile("^\\\"(.*)\\\"$");
//        Matcher m2 = p2.matcher(val_str);
//        if(m2.find()) {
//            result=m2.group(1);
//        } else {
//            if(val_str.matches("\\d+\\.\\d*")) result = Float.valueOf(val_str);
//            else if(val_str.matches("\\d+")) result = Long.valueOf(val_str);
//        }
//
//        return result;
//    }

    public boolean SetValueToInfo(String key, String str, Map MAP_INFO) {
        boolean result = false;
        
        try {
            str = URLDecoder.decode( str, "utf-8" );
            Object obj = StrToObject(str);
            if(Neb.utils.SetKey(key, obj, MAP_INFO)) result=true;
        } catch(Exception ex) {

        }
        return result;
    }
    
    public Map GetNodesAttributes(Map INFO) {
        Map<String, Map> result = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)INFO).entrySet()) {
            String area = entry.getKey();
            Map val = (Map)entry.getValue().get("nodes_information");
            Map tmp_node_map = new HashMap();
            if(val != null) {
                for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val).entrySet()) {
                    String node = entry1.getKey();
                    String image = (String)((Map)entry1.getValue()).get("image");
                    ArrayList<String> xy = (ArrayList<String>)entry1.getValue().get("xy");
                    ArrayList<String> size = (ArrayList<String>)entry1.getValue().get("size");
                    Map tmp_map = new HashMap();
                    if(image != null) tmp_map.put("image", image);
                    if(xy != null && xy.size() == 2) tmp_map.put("xy", xy);
                    if(size != null && size.size() == 2) tmp_map.put("size", size);
                    tmp_node_map.put(node, tmp_map);
                }
            }
            result.put(area, tmp_node_map);
        }
        return result;
    }   
    
    public Map SetNodesAttributes(Map<String, Map> node_attribute, Map INFO) {
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)INFO).entrySet()) {
            String area = entry.getKey();
            Map val = (Map)entry.getValue().get("nodes_information");
            if(val != null) {
                try {
                    for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val).entrySet()) {
                        String node = entry1.getKey();
//                        System.out.println(node);
//                        if(node.equals("image"))
//                            System.out.println("1111");                    
                        Map val1 = entry1.getValue();

                        if(val1 != null) {
                            ArrayList<String> list_ip = GetIpListFromNode(val1);
                            list_ip.add(node);
                            if(list_ip.size() > 0) {
                                for(String ip : list_ip) {
                                    if(!ip.startsWith("127.")) {
                                        Map value = (Map)Neb.utils.GetKey("/"+area+"/"+ip, node_attribute);
                                        if(value != null) {
                                            String image = (String)value.get("image");
                                            ArrayList<String> xy = (ArrayList<String>)value.get("xy");
                                            ArrayList<String> size = (ArrayList<String>)value.get("size"); 
                                            if(val1.get("image") == null && image != null) val1.put("image", image);
                                            if(val1.get("xy") == null && xy != null && xy.size() == 2) val1.put("xy", xy);
                                            if(val1.get("size") == null && size != null && size.size() == 2) val1.put("size", size);
                                            break;
                                        } 
                                    }
                                }
                            } else {
                                Map value = (Map)Neb.utils.GetKey("/"+area+"/"+node, node_attribute);
                                if(value != null) {
                                    String image = (String)value.get("image");
                                    ArrayList<String> xy = (ArrayList<String>)value.get("xy");
                                    ArrayList<String> size = (ArrayList<String>)value.get("size"); 
                                    if(image != null) val1.put("image", image);
                                    if(xy != null && xy.size() == 2) val1.put("xy", xy);
                                    if(size != null && size.size() == 2) val1.put("size", size);
                                }                        
                            }
                        } else
                            logger.Println("val1 is null node="+node+" !!!", logger.DEBUG);
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return INFO;
    }
    
    public Map<String, Map> SaveNodesAttributesOld(Map<String, Map> node_attribute, Map INFO) {
        Map<String, Map> result = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)node_attribute).entrySet()) {
            String area = entry.getKey();
            Map val = (Map)entry.getValue();
            if(val != null) {
                for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val).entrySet()) {
                    String node = entry1.getKey();
                    Map val1 = entry1.getValue();
                    if(val1 != null) {
                        Map value = (Map)Neb.utils.GetKey("/"+area+"/nodes_information/"+node, INFO);
                        if(value == null) {
                            if(result.get(area) != null) {
                                result.get(area).put(node, val1);
                            } else {
                                Map<String, Map> map_tmp = new HashMap();
                                map_tmp.put(node, val1);
                                result.put(area, map_tmp);
                            }
                        }
                    }

                }
            }
            
        }
        
        return result;
    }
    
//    public static String HTTPRequestGET(String url_str) {
//        try {
//            URL url = new URL(url_str);
//            HttpURLConnection con = (HttpURLConnection) url.openConnection();
//            con.setRequestMethod("GET");
//            int status = con.getResponseCode();
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//            String inputLine;
//            StringBuffer content = new StringBuffer();
//            while ((inputLine = in.readLine()) != null) {
//                content.append(inputLine+"\n");
//            }
//            in.close();
//            con.disconnect();
//            return content.toString();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return "";
//    } 
    
    private String MAC_Char_To_HEX(String str) {
        String result = str;

        if(str.length() == 6 && !str.matches("^[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}:[0-9A-Fa-f]{2}$")) {
            result = toHexString(str.getBytes());
        }
        return result;
    }
    
    private String toHexString(byte[] ba) {
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < ba.length; i++)
            str.append(String.format("%x", ba[i])).append(":");
        String out = str.toString();
        out = out.substring(0, out.length() - 1);
        return out;
    }
    
    public Map<String, Map<String, String[]>> Get_SNMP_accounts(Map neb_map_info) {
        Map<String, Map<String, String[]>> result = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)neb_map_info).entrySet()) {
            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, ArrayList> node_protocol_accounts = val.get("node_protocol_accounts");
            Map<String, String[]> map_node_tmp = new HashMap();
            if(node_protocol_accounts != null) {
                for(Map.Entry<String, ArrayList> entry1 : ((Map<String, ArrayList>)node_protocol_accounts).entrySet()) {
                    String node = entry1.getKey();
                    ArrayList<ArrayList<String>> val1 = entry1.getValue();
                    for(ArrayList<String> item : val1) {
                        if(item.get(0).equals("snmp")) {
                            String[] mas = new String[2];
                            mas[0]=item.get(1);
                            mas[1]=item.get(2);
                            map_node_tmp.put(node, mas);
                            break;
                        }
                    }
                }
            }
            if(map_node_tmp.size() > 0)
                result.put(area, map_node_tmp);
        }
        return result;
    }
    
    public Map<String, Map<String, String[]>> Get_CLI_accounts(Map neb_map_info) {
        Map<String, Map<String, String[]>> result = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)neb_map_info).entrySet()) {
            String area = entry.getKey();
            Map<String, Map> val = entry.getValue();
            Map<String, ArrayList> node_protocol_accounts = val.get("node_protocol_accounts");
            Map<String, String[]> map_node_tmp = new HashMap();
            if(node_protocol_accounts != null) {
                for(Map.Entry<String, ArrayList> entry1 : ((Map<String, ArrayList>)node_protocol_accounts).entrySet()) {
                    String node = entry1.getKey();
                    ArrayList<ArrayList<String>> val1 = entry1.getValue();
                    for(ArrayList<String> item : val1) {
                        if(item.get(0).equals("ssh") || item.get(0).equals("telnet")) {
                            String[] mas = new String[4];
                            mas[0]=item.get(0);
                            mas[1]=item.get(1);
                            mas[2]=item.get(2);
                            mas[3]=item.get(3);
                            map_node_tmp.put(node, mas);
                            break;
                        }
                    }
                }
            }
            if(map_node_tmp.size() > 0)
                result.put(area, map_node_tmp);
        }
        return result;
    } 
    
    public String GetFileCreateTime(String file) {
        String result = null;
        Pattern p = Pattern.compile(".+Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        String date_str = "";
        Matcher m = p.matcher(file);
        if(m.find()) {
            result = m.group(1).replace("-", " ");
        } else {
            try {
                BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
                ZonedDateTime ct = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
                String day;
                if(ct.getDayOfMonth() <= 9)
                    day = "0"+String.valueOf(ct.getDayOfMonth());
                else
                    day = String.valueOf(ct.getDayOfMonth());
                String month;
                if(ct.getMonthValue() <= 9)
                    month = "0"+String.valueOf(ct.getMonthValue());
                else
                    month = String.valueOf(ct.getMonthValue()); 
                String year = String.valueOf(ct.getYear());
                String hour;
                if(ct.getHour() <= 9)
                    hour = "0"+String.valueOf(ct.getHour());
                else
                    hour = String.valueOf(ct.getHour());  
                String min;
                if(ct.getMinute() <= 9)
                    min = "0"+String.valueOf(ct.getMinute());
                else
                    min = String.valueOf(ct.getMinute());             
                result = day+"."+month+"."+year+" "+hour+"."+min;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }    
    
    public long GetFileCreateTime_mSec(String file) {
        long result = 0;
        Pattern p = Pattern.compile(".+Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        Matcher m = p.matcher(file);
        if(m.find()) {
            String date_str = m.group(1).replace("-", " ");
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm");
            try {
                result = sdf.parse(date_str).getTime();
            } catch (java.text.ParseException ex) {
                ex.printStackTrace();
            }
        } else {        
            try {
                BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
                ZonedDateTime ct = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
                result = ct.toEpochSecond()*1000;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
    
    public void SetFileCreationDateNow(String filePath) {
        BasicFileAttributeView attributes = Files.getFileAttributeView(Paths.get(filePath), BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis((new Date()).getTime());
        try {
            attributes.setTimes(time, time, time);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }     
    
    public ArrayList FindKey(String key) {
        ArrayList<ArrayList<String>> result = new ArrayList();
        
        if(Neb.INDEX.get(key) != null) {
            result = Neb.INDEX.get(key);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm");
            Collections.sort(result, new Comparator<ArrayList>(){
                public int compare(ArrayList s1, ArrayList s2){
                   long time1 = 0;
                   long time2 = 0;
                    try {
                        time1 = sdf.parse((String)s1.get(0)).getTime();
                        time2 = sdf.parse((String)s2.get(0)).getTime();
                    } catch (java.text.ParseException ex) {
                        ex.printStackTrace();
                    }
                    if(time1 < time2) return 1;
                    else if(time1 >= time2) return -1;
                    return 0;
                }});        
         
        }
            
        return result;
    }

    public Map SetInformations(Map scan_info, Map info) {
        Map result = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)scan_info).entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            if(info.get(area) != null) {
                Map val1 = (Map)val.get("nodes_information_extended");
                if(val1 != null) {
                    ((Map)info.get(area)).put("nodes_information_extended", val1);
                }
                ArrayList list1 = (ArrayList)val.get("links");
                if(list1 != null) {
                    ((Map)info.get(area)).put("links", list1);
                }     
                val1 = (Map)val.get("node_protocol_accounts");
                if(val1 != null) {
                    ((Map)info.get(area)).put("node_protocol_accounts", val1);
                }   
                val1 = (Map)val.get("nodes_information");
                if(val1 != null) {
                    ((Map)info.get(area)).put("nodes_information", val1);
                }     
                list1 = (ArrayList)val.get("mac_ip_port");
                if(list1 != null) {
                    ((Map)info.get(area)).put("mac_ip_port", list1);
                }
                val1 = (Map)val.get("texts");
                if(val1 != null) {
                    ((Map)info.get(area)).put("texts", val1);
                }                  
            } else {
                Map map_tmp = new HashMap();
                Map val1 = (Map)val.get("nodes_information_extended");
                if(val1 != null) {
                    map_tmp.put("nodes_information_extended", val1);
                }
                ArrayList list1 = (ArrayList)val.get("links");
                if(list1 != null) {
                    map_tmp.put("links", list1);
                }     
                val1 = (Map)val.get("node_protocol_accounts");
                if(val1 != null) {
                    map_tmp.put("node_protocol_accounts", val1);
                }   
                val1 = (Map)val.get("nodes_information");
                if(val1 != null) {
                    map_tmp.put("nodes_information", val1);
                }     
                list1 = (ArrayList)val.get("mac_ip_port");
                if(list1 != null) {
                    map_tmp.put("mac_ip_port", list1);
                }
                val1 = (Map)val.get("texts");
                if(val1 != null) {
                    map_tmp.put("texts", val1);
                }                 
                info.put(area, map_tmp);
            }
        }
        
        // remove areas not scanned
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)info).entrySet()) {
            String area = entry.getKey();
            Map val = entry.getValue();
            if(scan_info.get(area) != null)
                result.put(area, val);
        }
        return result;
    }
    
    public boolean Inside_Network(String ip, String network) {
        if(ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            if(network.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                String[] net_mask = network.split("\\s+");
                if(net_mask.length == 2) { 
                    SubnetInfo subnet = (new SubnetUtils(net_mask[0], net_mask[1])).getInfo();
                    if(subnet.isInRange(ip)) 
                        return true;
                    else
                        return false;
                } else
                    return false;
            } else if(network.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+/32$")) {
                if(network.split("/")[0].equals(ip))
                    return true;
                else
                    return false;              
            } else if(network.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+$")) {
                SubnetInfo subnet = (new SubnetUtils(network)).getInfo();
                if(subnet.isInRange(ip)) 
                    return true;
                else
                    return false;            
            } else if(network.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || network.matches("\\d+\\.\\d+\\.\\d+\\.\\d+/32")) {
                if(network.equals(ip))
                    return true;
                else
                    return false;              
            } else
               return false;
        } else 
            return false;
    }
    
    public Map<String, ArrayList> GetForkList(Map<String, Map> INFO) {
        Map<String, ArrayList> result = new HashMap();
        Map<String, ArrayList<String[]>> area_node_community_version_dp = utils.GetAreaNodeCommunityVersionDP(INFO);
        for (Map.Entry<String, Map> area : ((Map<String, Map>) INFO).entrySet()) {
            String area_name = area.getKey();
//            if(!area_name.equals("area_chermk")) continue;
            Map val = area.getValue();
//            Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
            ArrayList<ArrayList<String>> mac_ip_port = (ArrayList<ArrayList<String>>) val.get("mac_ip_port");
            
            if(area_node_community_version_dp.get(area_name) != null) {
                ArrayList<String[]> node_community_version_dp_list = area_node_community_version_dp.get(area_name);
                ArrayList fork_list = GetFork(links, node_community_version_dp_list);
                if(fork_list.size() > 0) result.put(area_name, fork_list);
            }
        }
        return result;
    }
    
    private ArrayList GetFork(ArrayList<ArrayList<String>> links_base,
            ArrayList<String[]> node_community_version_dp_list) {
        
        ArrayList<ArrayList<String>> links = new ArrayList();
        for(ArrayList<String> link : links_base) {
            links.add(link);
//            System.out.println(link);
        }
        
        ArrayList fork_list = new ArrayList();
        for(int i=0; i<links.size(); i++) {
            ArrayList<String> link1 = links.get(i);
//            boolean flag = false;
//            if(
//                (link1.get(0).equals("10.96.115.140") && link1.get(2).equals("GigabitEthernet1/0/14")) ||
//                (link1.get(3).equals("10.96.115.140") && link1.get(5).equals("GigabitEthernet1/0/14"))
//            ) {
//                System.out.println("11111");
//                flag = true;
//            }

            ArrayList<String[]> neighbors_node_iface = new ArrayList();
            for(int j=i+1; j<links.size(); j++) {
                ArrayList<String> link2 = links.get(j);
               
                if(link1.get(0).equals(link2.get(0)) && !link1.get(2).equals("unknown") && !link2.get(2).equals("unknown") && EqualsIfaceName(link1.get(2), link2.get(2))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(3);
                    mas[1] = link2.get(4);
                    mas[2] = link2.get(5);
                    mas[3] = "";
                    for(String[] node_community_version_dp : node_community_version_dp_list) {
                        if(mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }
                    neighbors_node_iface.add(mas);
                    links.remove(j);
                    j--;
                }
                
                if(link1.get(0).equals(link2.get(3)) && !link1.get(2).equals("unknown") && !link2.get(5).equals("unknown") && EqualsIfaceName(link1.get(2), link2.get(5))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(0);
                    mas[1] = link2.get(1);
                    mas[2] = link2.get(2);
                    mas[3] = "";
                    for(String[] node_community_version_dp : node_community_version_dp_list) {
                        if(mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }                        
                    neighbors_node_iface.add(mas);
                    links.remove(j);
                    j--;
                }
            }

            if(neighbors_node_iface.size() > 0) {
                String[] mas = new String[4];
                mas[0] = link1.get(3);
                mas[1] = link1.get(4);
                mas[2] = link1.get(5);
                mas[3] = "";
                for(String[] node_community_version_dp : node_community_version_dp_list) {
                    if(mas[0].equals(node_community_version_dp[0])) {
                        mas[3] = node_community_version_dp[3];
                        break;
                    }
                }                  
                neighbors_node_iface.add(mas);
                String[] mas1 = new String[3];
                mas1[0] = link1.get(0);
                mas1[1] = link1.get(1);
                mas1[2] = link1.get(2);

                ArrayList fork = new ArrayList();
                fork.add(mas1);
                fork.add(neighbors_node_iface);
                fork_list.add(fork);
            }

            ArrayList<String[]> neighbors_node_iface1 = new ArrayList();
            for(int j=i+1; j<links.size(); j++) {
                ArrayList<String> link2 = links.get(j);
                if(link1.get(3).equals(link2.get(0)) && !link1.get(5).equals("unknown") && !link2.get(2).equals("unknown") && EqualsIfaceName(link1.get(5), link2.get(2))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(3);
                    mas[1] = link2.get(4);
                    mas[2] = link2.get(5);
                    mas[3] = "";
                    for(String[] node_community_version_dp : node_community_version_dp_list) {
                        if(mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }                    
                    neighbors_node_iface1.add(mas);
                }
                
                if(link1.get(3).equals(link2.get(3)) && !link1.get(5).equals("unknown") && !link2.get(5).equals("unknown") && EqualsIfaceName(link1.get(5), link2.get(5))) {
                    String[] mas = new String[4];
                    mas[0] = link2.get(0);
                    mas[1] = link2.get(1);
                    mas[2] = link2.get(2);
                    mas[3] = "";
                    for(String[] node_community_version_dp : node_community_version_dp_list) {
                        if(mas[0].equals(node_community_version_dp[0])) {
                            mas[3] = node_community_version_dp[3];
                            break;
                        }
                    }                    
                    neighbors_node_iface1.add(mas);
                }                
            }

            if(neighbors_node_iface1.size() > 0) {
                String[] mas = new String[4];
                mas[0] = link1.get(0);
                mas[1] = link1.get(1);
                mas[2] = link1.get(2);
                mas[3] = "";
                for(String[] node_community_version_dp : node_community_version_dp_list) {
                    if(mas[0].equals(node_community_version_dp[0])) {
                        mas[3] = node_community_version_dp[3];
                        break;
                    }
                }                  
                neighbors_node_iface1.add(mas);  
                String[] mas1 = new String[3];
                mas1[0] = link1.get(3);
                mas1[1] = link1.get(4);
                mas1[2] = link1.get(5);                    

                ArrayList fork = new ArrayList();
                fork.add(mas1);
                fork.add(neighbors_node_iface1);
                fork_list.add(fork);
            }                
        }
        return fork_list;
    }
    
    public Map<String, ArrayList> GetForkLinks(Map<String, ArrayList> area_forks, Map<String, Map> INFO, 
            Map<String, ArrayList<String[]>> area_arp_mac_table) {
        
        Map<String, ArrayList> area_add_del_links = new HashMap();
        Map<String, ArrayList<String[]>> area_node_community_version = GetAreaNodeCommunityVersion(INFO);
        for (Map.Entry<String, Map> area : ((Map<String, Map>) INFO).entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();
            ArrayList<String[]> arp_mac_table = new ArrayList();
            arp_mac_table = area_arp_mac_table.get(area_name);
            
            ArrayList<ArrayList<String>> links = new ArrayList();
            if(val.get("links") != null)
                links = (ArrayList<ArrayList<String>>)val.get("links");
            ArrayList<ArrayList<String>> mac_ip_port = new ArrayList();
            if(val.get("mac_ip_port") != null)
                mac_ip_port = (ArrayList<ArrayList<String>>)val.get("mac_ip_port");  
            
            ArrayList<ArrayList<String>> add_links = new ArrayList();
            ArrayList<ArrayList<String>> del_links = new ArrayList();
            if(area_forks.get(area_name) != null) {
                for(ArrayList forks : (ArrayList<ArrayList>)area_forks.get(area_name)) {
                    String[] parent_node_iface = (String[])forks.get(0);
                    ArrayList<String[]> children_list = (ArrayList<String[]>)forks.get(1);
                    int count_lldp = 0;
                    String[] lldp_node = new String[3];
                    for(String[] child : children_list) {
                        if(child[3].equals("lldp")) {
                            count_lldp = count_lldp + 1;
                            lldp_node[0] = child[0];
                            lldp_node[1] = child[1];
                            lldp_node[2] = child[2];
                        }
                    }
 
                    if(count_lldp == 1) {
//                        ArrayList<String> link = new ArrayList();
//                        link.add(parent_node_iface[0]); link.add(parent_node_iface[1]); link.add(parent_node_iface[2]);
//                        link.add(lldp_node[0]); link.add(lldp_node[1]); link.add(lldp_node[2]);
//                        link.add("{\"type\":\"lldp\"}");
//                        System.out.println("Adding fork link to middle "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
//                        add_links.add(link);
                        
                        ArrayList<String[]> children_list_new = new ArrayList();
                        for(String[] child : children_list) {
                            if(!(child[0].equals(lldp_node[0]) && child[1].equals(lldp_node[1]) && child[2].equals(lldp_node[2])))
                                children_list_new.add(child);
                        }
                        children_list = children_list_new;
                        
                        for(String[] child : children_list) {
                            boolean find = false;
                            for(ArrayList<String> iter : links) {
                                if(lldp_node[0].equals(iter.get(0)) && child[0].equals(iter.get(3))) {
//                                    ArrayList<String> link = new ArrayList();
//                                    link.add(iter.get(0)); link.add(iter.get(1)); link.add(iter.get(2));
//                                    link.add(iter.get(3)); link.add(iter.get(4)); link.add(iter.get(5));
//                                    link.add(iter.get(6));
//                                    add_links.add(link);
                                    find = true;
//                                    System.out.println("Adding fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                    ArrayList<String> link = new ArrayList();
                                    link.add(parent_node_iface[0]); link.add(parent_node_iface[1]); link.add(parent_node_iface[2]);
                                    link.add(child[0]); link.add(child[1]); link.add(child[2]);
                                    del_links.add(link);
                                    logger.Println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
//                                    System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);                                    
                                    break;                                    
                                }
                                if(lldp_node[0].equals(iter.get(3)) && child[0].equals(iter.get(0))) {
//                                    link = new ArrayList();
//                                    link.add(iter.get(3)); link.add(iter.get(4)); link.add(iter.get(5));
//                                    link.add(iter.get(0)); link.add(iter.get(1)); link.add(iter.get(2));
//                                    link.add(iter.get(6));
//                                    add_links.add(link);
                                    find = true;
//                                    System.out.println("Adding fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                    ArrayList<String> link = new ArrayList();
                                    link.add(parent_node_iface[0]); link.add(parent_node_iface[1]); link.add(parent_node_iface[2]);
                                    link.add(child[0]); link.add(child[1]); link.add(child[2]);
                                    del_links.add(link);
                                    logger.Println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
//                                    System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);
                                    break;
                                }                                
                            }
                            if(!find) {
                                for(String[] child1 : children_list) {
                                    boolean find1 = false;
                                    for(String[] nodeport_ip_mac : arp_mac_table) {
                                        if(nodeport_ip_mac[0].equals(lldp_node[0]) && nodeport_ip_mac[3].equals(child1[0])) {
                                            ArrayList<String> link = new ArrayList();
                                            link.add(nodeport_ip_mac[0]); link.add(nodeport_ip_mac[1]); link.add(nodeport_ip_mac[2]);
                                            link.add(child1[0]); link.add(child1[1]); link.add(child1[2]);
                                            link.add("mac_ip_port");
                                            boolean found = false;
                                            for(ArrayList<String> iter : add_links) {
                                                if(iter.get(0).equals(link.get(0)) && iter.get(1).equals(link.get(1)) && iter.get(2).equals(link.get(2)) &&
                                                   iter.get(3).equals(link.get(3)) && iter.get(4).equals(link.get(4)) && iter.get(5).equals(link.get(5)) ) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if(!found) {
                                                add_links.add(link);
                                                logger.Println("Adding fork link from mac_ip_port "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
                                            } else {
                                                logger.Println("Not adding duplicate fork link from mac_ip_port "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
                                            }
                                            find1 = true;
                                            link = new ArrayList();
                                            link.add(parent_node_iface[0]); link.add(parent_node_iface[1]); link.add(parent_node_iface[2]);
                                            link.add(child[0]); link.add(child[1]); link.add(child[2]);
                                            del_links.add(link);
//                                            System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);                                            
                                            logger.Println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
                                            break;                                            
                                        }
                                    }
                                    if(!find1) {
                                        ArrayList<String> link = new ArrayList();
                                        link.add(lldp_node[0]); link.add(""); link.add("unknown");
                                        link.add(child1[0]); link.add(child1[1]); link.add(child1[2]);
                                        link.add("mac_ip_port");
                                        boolean found = false;
                                        for(ArrayList<String> iter : add_links) {
                                            if(iter.get(0).equals(link.get(0)) && iter.get(1).equals(link.get(1)) && iter.get(2).equals(link.get(2)) &&
                                               iter.get(3).equals(link.get(3)) && iter.get(4).equals(link.get(4)) && iter.get(5).equals(link.get(5)) ) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if(!found) {
                                            add_links.add(link);                                    
                                            logger.Println("Adding fork link from mac_ip_port "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
                                        } else {
                                            logger.Println("Not adding fork link from mac_ip_port "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
                                        }
                                        logger.Println("Not link node "+child[0]+" "+child[2], logger.DEBUG);
                                        find1 = true;
                                        link = new ArrayList();
                                        link.add(parent_node_iface[0]); link.add(parent_node_iface[1]); link.add(parent_node_iface[2]);
                                        link.add(child[0]); link.add(child[1]); link.add(child[2]);
                                        del_links.add(link);
//                                        System.out.println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link);                                            
                                        logger.Println("Remove fork link from links "+parent_node_iface[0]+" "+parent_node_iface[2]+": "+link, logger.DEBUG);
                                    }
                                }

                            }
                        }

                    }
                }                
            }
            if(add_links.size() > 0 || del_links.size() > 0) {
                ArrayList item = new ArrayList();
                item.add(add_links);
                item.add(del_links);                
                area_add_del_links.put(area_name, item);
            }
        }   

        return area_add_del_links;
        
    }
    
    public void ModifyLinks(Map<String, Map> INFO, Map<String, ArrayList> area_add_del_links) { 
        for (Map.Entry<String, Map> area : ((Map<String, Map>) INFO).entrySet()) {
            String area_name = area.getKey();
            Map val = area.getValue();

            ArrayList add_del_links = area_add_del_links.get(area_name);
            if(add_del_links != null) {
//                ArrayList<ArrayList<String>> links = new ArrayList();
//                if(val.get("links") != null)
//                    links = (ArrayList<ArrayList<String>>)val.get("links"); 
                
                ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>)val.get("links");
                if(links != null) {
                    ArrayList<ArrayList<String>> add_links = (ArrayList<ArrayList<String>>)add_del_links.get(0);
                    for(ArrayList<String> item : add_links) {
                        links.add(item);
//                        System.out.println("Adding link: "+item.get(0)+" "+item.get(2)+" <---> "+item.get(3)+" "+item.get(5)+" --- "+item.get(6));
                        logger.Println("Adding link: "+item.get(0)+" "+item.get(2)+" <---> "+item.get(3)+" "+item.get(5)+" --- "+item.get(6), logger.DEBUG);
                    }

                    ArrayList<ArrayList<String>> del_links = (ArrayList<ArrayList<String>>)add_del_links.get(1);
                    ArrayList<ArrayList<String>> links_new = new ArrayList();
                    for(ArrayList<String> link : links) {
                        boolean found = false;
                        for(ArrayList<String> del_link : del_links) {
                            if(
                                (del_link.get(0).equals(link.get(0)) && del_link.get(1).equals(link.get(1)) && del_link.get(2).equals(link.get(2)) &&
                                del_link.get(3).equals(link.get(3)) && del_link.get(4).equals(link.get(4)) && del_link.get(5).equals(link.get(5)) ) ||
                                (del_link.get(0).equals(link.get(3)) && del_link.get(1).equals(link.get(4)) && del_link.get(2).equals(link.get(5)) &&
                                del_link.get(3).equals(link.get(0)) && del_link.get(4).equals(link.get(1)) && del_link.get(5).equals(link.get(2)) )                                
                               ) {
                                found = true;
                                break;
                            }
                        }
                        if(found)
//                            System.out.println("Remove link: "+link.get(0)+" "+link.get(2)+" <---> "+link.get(3)+" "+link.get(5)+" --- "+link.get(6));
                            logger.Println("Remove link: "+link.get(0)+" "+link.get(2)+" <---> "+link.get(3)+" "+link.get(5)+" --- "+link.get(6), logger.DEBUG);
                        else
                            links_new.add(link);
                    }
                    links.clear();
                    links.addAll(links_new);
                }
            }
        }
    }
    
    public Map SetAttributesToNodes(Map INFORMATION, String last_day_file, String attribute_old_file) {
        // set nodes attributes
        Map<String, Map> information_last_day = utils.ReadJSONFile(last_day_file);
        Map<String, Map> node_attribute = new HashMap();
        Map<String, Map> node_attribute_old_from_file = utils.ReadJSONFile(attribute_old_file);
        node_attribute = node_attribute_old_from_file;

        Map<String, Map> node_attribute_last_day = utils.GetNodesAttributes(information_last_day);
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)node_attribute_last_day).entrySet()) {
            String area = entry.getKey();
            Map val = (Map)entry.getValue();
            if(val != null) {
                for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val).entrySet()) {
                    String node = entry1.getKey();
                    Map val1 = entry1.getValue();
                    if(val1 != null) {
                        Map value = (Map)Neb.utils.GetKey("/"+area+"/"+node, node_attribute);
                        if(value != null) {
                            Neb.utils.SetKey("/"+area+"/"+node, val1, node_attribute);
                        } else {
                            if(node_attribute.get(area) != null) {
                                node_attribute.get(area).put(node, val1);
                            } else {
                                Map<String, Map> map_tmp = new HashMap();
                                map_tmp.put(node, val1);
                                node_attribute.put(area, map_tmp);
                            }
                        }
                    } 
                }
            }
        }                         

        // save nodes attribute old
        Map<String, Map> nodes_attribute_old = utils.SaveNodesAttributesOld(node_attribute, INFORMATION);
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)nodes_attribute_old).entrySet()) {
            String area = entry.getKey();
            Map val = (Map)entry.getValue();
            if(val != null) {
                for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val).entrySet()) {
                    String node = entry1.getKey();
                    Map val1 = entry1.getValue();
                    if(val1 != null) {
                        Map value = (Map)Neb.utils.GetKey("/"+area+"/"+node, node_attribute_old_from_file);
                        if(value != null) {
                            Neb.utils.SetKey("/"+area+"/"+node, val1, node_attribute_old_from_file);
                        } else {
                            if(node_attribute_old_from_file.get(area) != null) {
                                node_attribute_old_from_file.get(area).put(node, val1);
                            } else {
                                Map<String, Map> map_tmp = new HashMap();
                                map_tmp.put(node, val1);
                                node_attribute_old_from_file.put(area, map_tmp);
                            }
                        }
                    } 
                }
            }
        } 
        // write to file nodes_information
        utils.MapToFile((Map) node_attribute_old_from_file, attribute_old_file);

        INFORMATION = utils.SetNodesAttributes(node_attribute, INFORMATION);
//        utils.MapToFile((Map) INFORMATION, map_file_pre); 

        return INFORMATION;
    }
    
    public Map GetName(Map INFORMATION) {
        Map<String, String> name_ip = new HashMap();
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)INFORMATION).entrySet()) {
            String area = entry.getKey();
            System.out.println(area);
            Map val = (Map)entry.getValue();
            if(val.get("nodes_information") != null) {
                for(Map.Entry<String, Map> entry1 : ((Map<String, Map>)val.get("nodes_information")).entrySet()) {
                    String node = entry1.getKey();
                    if(node.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        try {
                            InetAddress ia = InetAddress.getByName(node);
                            if(!ia.getCanonicalHostName().equals(node)) {
                                String name = ia.getCanonicalHostName().split("\\.")[0].toLowerCase();
                                name_ip.put(name, node);
//                                System.out.println(name);
                            }
                        } catch (Exception ex) {}
                    
                    }                    
                }
            }
            
            
            ArrayList<ArrayList<String>> mac_ip_port = (ArrayList)val.get("mac_ip_port");
            if(mac_ip_port != null) {
                for(ArrayList<String> item : mac_ip_port) {
                    String host = item.get(1);
                    if(host.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
                        try {
                            InetAddress ia = InetAddress.getByName(host);
                            if(!ia.getCanonicalHostName().equals(host)) {
                                String name = ia.getCanonicalHostName().split("\\.")[0].toLowerCase();
                                name_ip.put(name, host);
//                                System.out.println(host);
                            }
                        } catch (Exception ex) {}
                    
                    }                    
                }
            }

        }
        return name_ip;
    }
    
    public void SetName(String info_file, String names_file) {
        Map<String, Map> info = ReadJSONFile(info_file);
        Map<String, String> name_ip_fromfile = ReadJSONFile(names_file);
        Map name_ip = GetName(info);
        
        
        for(Map.Entry<String, String> entry : ((Map<String, String>)name_ip).entrySet()) {
            String name = entry.getKey();
            String ip = entry.getValue();
            name_ip_fromfile.put(name, ip);

        }

        MapToFile((Map) name_ip_fromfile, names_file);
    }
 
}
