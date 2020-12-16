/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_builder;

//import com.sun.net.httpserver.HttpServer;
//import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
//import java.io.FileWriter;
import java.io.IOException;
import static java.lang.Math.toIntExact;
//import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.*;
import org.json.simple.parser.*;

/**
 *
 * @author kos
 */
public class Neb {
//    public static String home = "";

    public static final String map_file = "neb.map";
    public static final String neb_cfg = "neb.cfg";
    public static final String map_file_pre = "neb.map.pre";
    private static final String delete_file = "delete.buff";
    private static final String history_map = "history";
    private static final String log = "neb.log";
    public static final String node_attribute_old_file = "node_attribute_old";
    public static final String names_info = "names_info";
    private static final String dump = "dump.tmp";
//    public static Map<String, String> passwd_hash_map = new HashMap();
    private static final int pause = 60 * 60; // sec.
//    private static final int pause = 15*60; // sec.    
    private static final int pause_test = 5 * 60; // sec.
//    private static final int pause_test = 5*60; // sec.
    private static final int retries_testing = 5;
    private static final int limit_retries_testing = 5;
    private static final int time_lag = 20; // sec.
    private static double precession_limit = 0.02;
    private static boolean discovery_networks = true;
    private static boolean extended_discovery_link = true;
    public static int timeout = 10; // timeout network.
    public static int retries = 1; // network timeout 1 sec.  
    public static long timeout_process = 30 * 60 * 1000; // 10 min
    public static long timeout_output = 10 * 60 * 1000;
//    public static int retries_process = 3; // retries for scripts    
    public static int timeout_mac = 60; // mac timeout 3 sec.
    public static int retries_mac = 3; // mac timeout 1 sec. 
    public static int MAX_RUNSCRIPT_CLI_TEST = 64;
    public static int MAX_RUNSCRIPT_CLI = 16;

    public static Logger logger;
    public static ArrayList<String> node_scanning = new ArrayList();
    public static ArrayList<String> networks = new ArrayList();
//    public static String home = "";
    public static String history_dir = "";
    public static Map<String, String[]> nodes_info = new HashMap();
    public static ArrayList<String[]> links_info = new ArrayList();
    public static Map<String, String[]> mac_ArpMacTable = new HashMap();
    public static Map<String, String[]> ip_ArpMacTable = new HashMap();
    public static Map<String, String[]> extended_info = new HashMap();
    public static Map<String, String[]> text_info = new HashMap();
    public static Map<String, String[]> text_custom_info = new HashMap();
    public static Map<String, ArrayList<String[]>> area_arp_mac_table = new HashMap();

    public static Map cfg = new HashMap();
    public static Map<String, ArrayList<ArrayList<String>>> INDEX = new HashMap();
    public static String index_dir = "index";
    public static Utils utils = new Utils();

    private static String run_post_scripts = "PostScripts/run.cmd";

    public static boolean DEBUG = true;

    public static void main(String[] args) {

        // get kernel processor
        int num_kernel = Runtime.getRuntime().availableProcessors();
        MAX_RUNSCRIPT_CLI = num_kernel * 4;

        if(DEBUG) ru.kos.neb.neb_lib.Utils.DEBUG = true;
        else ru.kos.neb.neb_lib.Utils.DEBUG = false;
        File file_log_lib = new File(ru.kos.neb.neb_lib.Utils.LOG_FILE); 
        if(file_log_lib.exists())
            file_log_lib.delete();
        
        // read config file
        cfg = utils.ReadConfig(neb_cfg);

        String history_map = (String) cfg.get("history_map");
        history_dir = history_map;
        int history_num_days = ((Long) cfg.get("history_num_days")).intValue();
        int log_num_days = ((Long) cfg.get("log_num_days")).intValue();
        String passwd_file = (String) cfg.get("passwd_file");
//        ArrayList<String> passwd_hash_list = utils.ReadFileToList(passwd_file);
//        for(String hash : passwd_hash_list) passwd_hash_map.put(hash, hash);
        int http_port = ((Long) cfg.get("http_port")).intValue();
        run_post_scripts = (String) cfg.get("postscript");

        // start logging
        logger = new Logger("neb.log");
        String level_log = (String) cfg.get("level_log");
        if (level_log.equals("INFO")) {
//            ru.kos.neb.neb_lib.Utils.DEBUG=false;
            logger.SetLevel(logger.INFO);
        } else if (level_log.equals("DEBUG")) {
//            ru.kos.neb.neb_lib.Utils.DEBUG=true;
            logger.SetLevel(logger.DEBUG);
        }

///////////////////////////////////////////////////////
//        try {
//            StandardAnalyzer analyzer = new StandardAnalyzer();
//
//            final Directory index = FSDirectory.open(Paths.get("index"));       
//
//            // 2. query
////            String querystr = args.length > 0 ? args[0] : "ANT-46";
//            Query q = new QueryParser("text", analyzer).parse("Помещение*");
//
//            // 3. search
//            int hitsPerPage = 10;
//            IndexReader reader = DirectoryReader.open(index);
//            IndexSearcher searcher = new IndexSearcher(reader);
//            TopDocs docs = searcher.search(q, hitsPerPage);
//            ScoreDoc[] hits = docs.scoreDocs;
//
//            // 4. display results
//            System.out.println("Found " + hits.length + " hits.");
//            for(int i=0;i<hits.length;++i) {
//                int docId = hits[i].doc;
//                Document d = searcher.doc(docId);
//                System.out.println((i + 1) + ". " + d.get("text") + "\t" + d.get("area") + "\t" + d.get("node"));
//            }
//            reader.close(); 
//        } catch(Exception ex) {
//            ex.printStackTrace();
//        }
//        
//        System.out.println("11111");
        
        
        
//                        // Graph layout
//                        Map INFORMATION1 = utils.ReadJSONFile("neb.map.pre");
//                        GraphLayout graphLayout1 = new GraphLayout();
//                        INFORMATION1 = graphLayout1.StartGraphLayout(INFORMATION1);
//                        utils.MapToFile((Map) INFORMATION1, "info_map.out");
//    System.out.println("11111");
    
//    Map INFORMATION1 = utils.ReadJSONFile("neb.map.pre");
//    INFORMATION1 = utils.SetAttributesToNodes(INFORMATION1, map_file, node_attribute_old_file); 
//    utils.MapToFile((Map) INFORMATION1, map_file_pre);    
//    System.out.println("111111111");
//    ArrayList<String> mac_ip_node_list = utils.ReadFileToList("mac_ip_node");
//    ArrayList<String[]> mac_ip_node = new ArrayList();
//    for(String item: mac_ip_node_list) {
//        String[] mas = item.split(",");
//        mac_ip_node.add(mas);
//    }
//    
//    Map<String, String> result1 = utils.ProcessingARP(mac_ip_node);
//    utils.MapToFile((Map) result1, "result1");
//    System.out.println("11");

//    Map INFO1 = utils.ReadJSONFile("neb.map");
////    utils.SetValueToInfo("/area_chermk/nodes_information/10.97.115.254/key/", "test", INFO1);
//    utils.DeleteKey("/area_chermk/nodes_information/10.97.115.254", INFO1);
////    utils.SetValueToInfo("/", "test", INFO1);
//    System.out.println("11111");
//    Map INFO1 = utils.ReadJSONFile("info3");
//    Map<String, ArrayList<ArrayList>> area_node_community_version1 = utils.ReadJSONFile("area_node_community_version_dp");
//    Map<String, ArrayList<String[]>> area_node_community_version2 = new HashMap();
//    for (Map.Entry<String, ArrayList<ArrayList>> area : ((Map<String, ArrayList<ArrayList>>) area_node_community_version1).entrySet()) {
//        String area_name = area.getKey();
//        ArrayList<ArrayList> area_info = area.getValue();
//        ArrayList<String[]> list = new ArrayList();
//        for(ArrayList item : area_info) {
//            String[] mas = new String[4];
//            mas[0]=(String)item.get(0);
//            mas[1]=(String)item.get(1);
//            mas[2]=(String)item.get(2);
//            mas[3]=(String)item.get(3);
//            list.add(mas);
//        }
//        area_node_community_version2.put(area_name, list);
//    }
//    
//    Map<String, Map<String, ArrayList<ArrayList<String>>>> area_node_ifaceid_ifacename1 = utils.GetAreaNodeIdIface(INFO1, area_node_community_version2);
//    System.out.println("1111111111111");
//    System.exit(0);

//    Map INFO = utils.ReadJSONFile("info7");
//    ArrayList<ArrayList<String>> links1 = (ArrayList)((Map)INFO.get("area_chermk")).get("links");
//    Map<String, Map> nodes_information1 = (Map)((Map)INFO.get("area_chermk")).get("nodes_information");
//    Map node_protocol_accounts1 = (Map)((Map)INFO.get("area_chermk")).get("node_protocol_accounts");
//
//    ArrayList<String> list = utils.ReadFileToList("arp_mac_table_area_chermk");
//    ArrayList<String[]> arp_mac_table1 = new ArrayList();
//    for(String it : list) {
//        String mas[] = it.split(",");
//        arp_mac_table1.add(mas);
//    }    
//    if (arp_mac_table1 != null && arp_mac_table1.size() > 0) {
//        ArrayList<String[]> mac_ip_node_port = utils.CalculateARPMAC(arp_mac_table1, links1, nodes_information1, node_protocol_accounts1);
//        utils.WriteArrayListToFile("mac_ip_node_port", mac_ip_node_port);
//    }
//    System.out.println("11111");
    
    
    
    
//        Map INFO = utils.ReadJSONFile("neb.map");
//        Map<String, ArrayList> area_forks1 = utils.GetForkList(INFO);
//        ArrayList<String> list = utils.ReadFileToList("arp_mac_table_area_chermk");
//        ArrayList<String[]> node_ip_mac = new ArrayList();
//        for(String it : list) {
//            String mas[] = it.split(",");
//            node_ip_mac.add(mas);
//        }
//        area_arp_mac_table.put("area_chermk", node_ip_mac);
//        Map<String, ArrayList> area_add_del_links1 = utils.GetForkLinks(area_forks1, INFO, area_arp_mac_table);
//        utils.ModifyLinks(INFO, area_add_del_links1);

//        Map INFO = utils.ReadJSONFile(map_file);
//        Map<String, Map> node_attribute1 = utils.GetNodesAttributes(INFO);
//        Map informationFromNodesAllAreas1 = utils.ReadJSONFile("informationFromNodesAllAreas1");
//        informationFromNodesAllAreas1 = utils.SetNodesAttributes(node_attribute1, informationFromNodesAllAreas1);

//        Map info_map = utils.ReadJSONFile("informationFromNodesAllAreas2");

//        for (Map.Entry<String, Map> area : ((Map<String, Map>) info_map).entrySet()) {
//            String area_name = area.getKey();
//            Map area_info = area.getValue();
//            ArrayList<ArrayList<String>> mac_ip_port = (ArrayList<ArrayList<String>>)area_info.get("mac_ip_port");
//            if(mac_ip_port != null) {
//                ArrayList<String[]> mac_ip_port_new = new ArrayList();
//                for(ArrayList<String> mip : mac_ip_port) {
//                    String[] mas = new String[mip.size()];
//                    int i = 0;
//                    for(String it : mip) {
//                        mas[i]=it;
//                        i = i + 1;
//                    }
//                    mac_ip_port_new.add(mas);
//                }
//                area_info.put("mac_ip_port", mac_ip_port_new);
//            }
//        }
//        // Normalization information map
//        info_map = utils.NormalizeMap(info_map, area_arp_mac_table);        
//        // write to file nodes_information
//        utils.MapToFile((Map) info_map, "info_map.out");        
/////////////////////////////////////////////////////
        QueueWorker queueWorker = new QueueWorker();
        queueWorker.start();
        
        Server_HTTP server_HTTP = new Server_HTTP(http_port);
        server_HTTP.start();        
        ///////////////////////////////////////////////////////

        // indexing
        logger.Println("Start indexing ...", logger.INFO);
        utils.Indexing(INDEX);
        logger.Println("Stop indexing.", logger.INFO);        

        File f = new File("neb.cfg");
        long time_cfg_file = f.lastModified();
        long prev_time_cfg_file = 0;
        File f_map = new File("neb.map");
        long time_cfg_file_map = f_map.lastModified();
        long prev_time_cfg_file_map = 0;
        while (true) {
            time_cfg_file = f.lastModified();
            if (time_cfg_file != prev_time_cfg_file) {
                // read config file
                cfg = utils.ReadConfig(neb_cfg);
                prev_time_cfg_file = time_cfg_file;
                System.out.println("Reload neb.cfg file.");
                history_map = (String) cfg.get("history_map");
                history_dir = history_map;
                history_num_days = ((Long) cfg.get("history_num_days")).intValue();
                log_num_days = ((Long) cfg.get("log_num_days")).intValue();
                run_post_scripts = (String) cfg.get("postscript");
                //                passwd_file = (String)cfg.get("passwd_file");
                //                passwd_hash_list = utils.ReadFileToList(passwd_file);
                //                for(String hash : passwd_hash_list) passwd_hash_map.put(hash, hash);
            }

            //            time_cfg_file_map = f_map.lastModified();
            //            if (time_cfg_file_map != prev_time_cfg_file_map) {
            //                // get information from neb.map file
            ////                utils.GetInformationFromMapFile(home + (String)cfg.get("map_file"));                
            //                prev_time_cfg_file_map=time_cfg_file_map;
            //                System.out.println("Reload "+home + (String)cfg.get("map_file")+" file.");
            //            }      
            // get current time
            Date date = new Date(System.currentTimeMillis());
            String d = date.toString();
            String[] arr = d.split(" ");
            String[] time1 = arr[3].split(":");
            int hour = Integer.valueOf(time1[0]);
            int minute = Integer.valueOf(time1[1]);

            ArrayList<String> build_network_time = (ArrayList<String>) cfg.get("build_network_time");
            ArrayList<String> build_times = new ArrayList();
            if (build_network_time != null) {
                for (String item : build_network_time) {
                    String[] mas = item.split("\\s*,\\s*");
                    for (String str : mas) {
                        if (str.matches("^\\d{1,2}:\\d{1,2}$")) {
                            build_times.add(str);
                        }
                    }
                }
            }

            for (String build_time : build_times) {
                String[] time_start = build_time.split(":");
                //                System.out.println("Start time: "+time_start[0]+":"+time_start[1]+"\tCurrent: "+hour+":"+minute);
                //                if(true) {
                if (Integer.valueOf(time_start[0]) == hour && Integer.valueOf(time_start[1]) == minute) {
                    // delete old log files
                    utils.RemoveOldFiles("Log", (int) log_num_days);
                    // copy neb.log file to Log directory
                    File file_log = new File("neb.log");
                    if (file_log.exists()) {
                        Date dd = new Date(file_log.lastModified());
                        SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy-HH.mm");
                        String file_log_history = "Log/Neb_" + format1.format(dd) + ".log";
                        File folder_log = new File("Log");
                        File history_log = new File(file_log_history);
                        if (!folder_log.exists()) {
                            folder_log.mkdir();
                        }
                        try {
                            if (!history_log.exists()) {
                                Files.copy(file_log.toPath(), history_log.toPath());
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            java.util.logging.Logger.getLogger(Neb.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    // start new logging
                    logger.Clear("neb.log");
                    File file_out_out = new File("out.out");
                    file_out_out.delete();

                    logger.Println("Runnung Neb program ...", logger.INFO);

                    // read config file
                    cfg = utils.ReadConfig(neb_cfg);
                    ArrayList<String> include_sysDescr_cli_matching = new ArrayList();
                    if (cfg.get("include_sysDescr_cli_matching") != null) {
                        include_sysDescr_cli_matching = (ArrayList<String>) cfg.get("include_sysDescr_cli_matching");
                    }
                    prev_time_cfg_file = time_cfg_file;
                    System.out.println("Load neb.cfg file.");

                    // read neb.map file
                    Map INFORMATION = new HashMap();
                    File file_map = new File(map_file);
                    if (file_map.exists()) {
                        INFORMATION = utils.ReadJSONFile(map_file);
                    }
                    
                    Map<String, Map<String, String[]>> snmp_accounts = utils.Get_SNMP_accounts(INFORMATION);
                    Map<String, Map<String, String[]>> cli_accounts = utils.Get_CLI_accounts(INFORMATION);

                    Map<String, Map> areas = (Map<String, Map>) cfg.get("areas");
                    if (areas != null && areas.size() > 0) {
                        Map informationFromNodesAllAreas = new HashMap();
//                        ArrayList arp_mac_area[] = new ArrayList[areas.size()];
//                        for (int i = 0; i < areas.size(); i++) {
//                            arp_mac_area[i] = new ArrayList();
//                        }
                        Map<String, Map<String, Map<String, String[]>>> start_counters = new HashMap();
                        //                        for(int i=0; i<areas.size(); i++) start_counters[i] = new HashMap<>();
                        Map<String, Map<String, Map<String, String[]>>> stop_counters = new HashMap();
                        //                        for(int i=0; i<areas.size(); i++) stop_counters[i] = new HashMap<>();
                        //                        int i=0;
                        node_scanning.clear();
                        //////////////////////////////////////////////////////////////////////////////////////
                        
                        Map<String, ArrayList<String>> area_networks = new HashMap();
                        for (Map.Entry<String, Map> area : areas.entrySet()) {
                            int timeout = Neb.timeout;
                            int retries = Neb.retries;
                            long timeout_process = Neb.timeout_process;
//                                int retries_process = Neb.retries_process;

                            String area_name = area.getKey();
                            logger.Println("Area: " + area_name, logger.INFO);
                            //                            System.out.println("Area: "+area_name);
                            Map informationFromNodes = new HashMap();
                            ArrayList<String> community_list = (ArrayList<String>) area.getValue().get("snmp_community");
                            ArrayList<ArrayList<String>> accounts_list = (ArrayList<ArrayList<String>>) area.getValue().get("cli_accounts");
                            ArrayList<String> include_list = (ArrayList<String>) area.getValue().get("include");
                            Map<String, String> exclude_list = (Map<String, String>) area.getValue().get("exclude");
                            ArrayList<String> network_list = (ArrayList<String>) area.getValue().get("networks");
                            //                            ArrayList<String> include_sysDescr_cli_matching = (ArrayList<String>)area.getValue().get("include_sysDescr_cli_matching");

                            ArrayList<String> ip_list = new ArrayList();
                            ArrayList<String> networks = new ArrayList();
                            for (String network : network_list) {
                                if (network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s+\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$") || network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+\\s*$")) {
                                    networks.add(network);
                                } else if (network.matches("^\\s*\\d+\\.\\d+\\.\\d+\\.\\d+\\s*$")) {
                                    ip_list.add(network);
                                } else {
                                    logger.Println(network + " is not correct format!", logger.INFO);
                                }
                            }

                            //                            int timeout=Neb.timeout;
                            //                            int retries=Neb.retries;
                            String discovery_networks = ((String) area.getValue().get("discovery_networks")).toLowerCase();
                            if (cfg.get("areas") != null && ((Map) cfg.get("areas")).get(area_name) != null) {
                                Long timeout_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("timeout");
                                Long retries_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("retries");
                                if (timeout_options != null) {
                                    timeout = toIntExact(timeout_options);
                                }
                                if (retries_options != null) {
                                    retries = toIntExact(retries_options);
                                }
                                Long timeout_process_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("timeout_process");
                                Long retries_process_options = (Long) ((Map) ((Map) cfg.get("areas")).get(area_name)).get("retries_process");
                                if (timeout_process_options != null) {
                                    timeout_process = toIntExact(timeout_process_options);
                                }
//                                    if(retries_process_options != null) retries_process=toIntExact(retries_process_options);                                
                            }

                            ArrayList<String> net_list = new ArrayList();
                            net_list.addAll(networks);
                            net_list.addAll(ip_list);
                            
                            logger.Println("Start scanning information from nodes ...", logger.DEBUG);
                            informationFromNodes = utils.ScanInformationFromNodes(networks, ip_list, community_list, accounts_list, exclude_list, include_sysDescr_cli_matching, snmp_accounts.get(area_name), cli_accounts.get(area_name), net_list);
                            logger.Println("Stop scanning information from nodes.", logger.DEBUG);
                            // recursive explorer networks
                            Map prev_informationFromNodes = informationFromNodes;
                            int iteration = 1;
                            logger.Println("Start rescanning information from nodes ...", logger.DEBUG);
                            while (discovery_networks.equals("yes")) {
                                informationFromNodes = utils.RescanInformationFromNodes(informationFromNodes, community_list, accounts_list, include_list, include_sysDescr_cli_matching, snmp_accounts.get(area_name), cli_accounts.get(area_name), net_list);
                                if (informationFromNodes.size() == 0) {
                                    break;
                                }
                                logger.Println("Discoverer iterations = " + iteration, logger.DEBUG);
                                iteration++;
                                prev_informationFromNodes = informationFromNodes;
                            }
                            logger.Println("Stop rescanning information from nodes.", logger.DEBUG);
                            informationFromNodes = prev_informationFromNodes;

                            Map<String, Map> nodes_information = (Map<String, Map>) informationFromNodes.get("nodes_information");
                            area_networks.put(area_name, net_list);
                            ArrayList<ArrayList<ArrayList<String>>> result = utils.NormalizationLinks(nodes_information, net_list);
                            ArrayList<ArrayList<String>> links = result.get(0);
                            ArrayList<ArrayList<String>> links_extended = result.get(1);
                            informationFromNodes.put("links", links);
                            informationFromNodes.put("links_extended", links_extended);

                            // adding nodes from links
                            informationFromNodes = utils.AddingNodesFromLinks(informationFromNodes);

                            // remove exclude_list
                            if (informationFromNodes.get("exclude_list") != null) {
                                informationFromNodes.remove("exclude_list");
                            }

                            informationFromNodesAllAreas.put(area_name, informationFromNodes);

                            // write to file nodes_information
//                            utils.MapToFile((Map)informationFromNodes, "informationFromNodes"+area_name);
                            logger.Println("=====================================", logger.INFO);
                        }

                        utils.MapToFile((Map) informationFromNodesAllAreas, "info1");
                        
                        // remove duplicate nodes in all areas am mac addresses, ip list and sysname
                        logger.Println("Start RemoveDuplicateNodes...", logger.DEBUG);
                        informationFromNodesAllAreas = utils.RemoveDuplicateNodes(informationFromNodesAllAreas, area_networks);
                        logger.Println("Stop RemoveDuplicateNodes.", logger.DEBUG); 
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

                        utils.MapToFile((Map) informationFromNodesAllAreas, "info2");
                        logger.Println("Start GetAreaNodeCommunityVersionDP...", logger.DEBUG);
                        Map<String, ArrayList<String[]>> area_node_community_version_dp = utils.GetAreaNodeCommunityVersionDP(informationFromNodesAllAreas);
                        logger.Println("Stop GetAreaNodeCommunityVersionDP.", logger.DEBUG);
                        utils.MapToFile((Map)area_node_community_version_dp, "area_node_community_version_dp");

                        utils.MapToFile((Map) informationFromNodesAllAreas, "info3");
                        // get nodes ifaceid ifacename 
                        logger.Println("Start GetAreaNodeIdIface...", logger.DEBUG);
                        Map<String, Map<String, ArrayList<ArrayList<String>>>> area_node_ifaceid_ifacename = utils.GetAreaNodeIdIface(informationFromNodesAllAreas, area_node_community_version_dp);
                        logger.Println("Stop GetAreaNodeIdIface.", logger.DEBUG);

                        // adding ifacaid to links
                        utils.MapToFile((Map) informationFromNodesAllAreas, "info4");
                        logger.Println("Start AddingIfaceIdToLinks...", logger.DEBUG);
                        informationFromNodesAllAreas = utils.AddingIfaceIdToLinks(informationFromNodesAllAreas, area_node_ifaceid_ifacename);
                        logger.Println("Stop AddingIfaceIdToLinks.", logger.DEBUG);

                        utils.MapToFile((Map) informationFromNodesAllAreas, "info5");
                        Map<String, ArrayList<ArrayList<String>>> area_links_calculate = new HashMap();
                        if (cfg.containsKey("calculate_links_from_counters") && cfg.get("calculate_links_from_counters").equals("yes")) {
                            area_links_calculate = utils.GetCalculateLinks(informationFromNodesAllAreas, area_node_community_version_dp, pause, pause_test, precession_limit, retries_testing, limit_retries_testing);
                        }

                        utils.MapToFile((Map) informationFromNodesAllAreas, "info6");
                        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                            String area_name = area.getKey();
                            Map val = area.getValue();
                            ArrayList<ArrayList<String>> links_result = new ArrayList();
                            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                            ArrayList<ArrayList<String>> links_calculate = (ArrayList<ArrayList<String>>) area_links_calculate.get(area_name);
                            if (links != null && links.size() > 0) {
                                for (ArrayList<String> link : links) {
                                    if (link.size() > 5) {
                                        ArrayList<String> mas = new ArrayList();
                                        mas.add(link.get(0));
                                        mas.add(link.get(1));
                                        mas.add(link.get(2));
                                        mas.add(link.get(3));
                                        mas.add(link.get(4));
                                        mas.add(link.get(5));
                                        mas.add(link.get(6));
                                        links_result.add(mas);
                                    } else {
                                        logger.Println("links size <= 5. Size =" + link.size(), logger.DEBUG);
                                        for (String it : link) {
                                            logger.Println(it, logger.DEBUG);
                                        }
                                        logger.Println("----------------------", logger.DEBUG);
                                        ArrayList<String> mas = new ArrayList();
                                        mas.add(link.get(0));
                                        mas.add("");
                                        mas.add(link.get(1));
                                        mas.add(link.get(2));
                                        mas.add("");
                                        mas.add(link.get(3));
                                        mas.add(link.get(4));
                                        links_result.add(mas);
                                    }
                                }
                                val.remove("links");
                            }
                            if (links_calculate != null && links_calculate.size() > 0) {
                                for (ArrayList<String> link : links_calculate) {
                                    if (link.size() > 5) {
                                        ArrayList<String> mas = new ArrayList();
                                        mas.add(link.get(0));
                                        mas.add(link.get(1));
                                        mas.add(link.get(2));
                                        mas.add(link.get(3));
                                        mas.add(link.get(4));
                                        mas.add(link.get(5));
                                        mas.add("calc");
                                        links_result.add(mas);
                                    } else {
                                        logger.Println("links size <= 5. Size =" + link.size(), logger.DEBUG);
                                        for (String it : link) {
                                            logger.Println(it, logger.DEBUG);
                                        }
                                        logger.Println("----------------------", logger.DEBUG);
                                        ArrayList<String> mas = new ArrayList();
                                        mas.add(link.get(0));
                                        mas.add("");
                                        mas.add(link.get(1));
                                        mas.add(link.get(2));
                                        mas.add("");
                                        mas.add(link.get(3));
                                        mas.add("calc");
                                        links_result.add(mas);
                                    }
                                }
//                                val.remove("links_calculate");
                            }
                            val.remove("links_extended");
                            val.put("links", links_result);
                        }

                        // write to file nodes_information
                        utils.MapToFile((Map) informationFromNodesAllAreas, "info7");

                        logger.Println("Start GetAreaNodeCommunityVersion...", logger.DEBUG);
                        Map<String, ArrayList<String[]>> area_node_community_version = utils.GetAreaNodeCommunityVersion(informationFromNodesAllAreas);
                        logger.Println("Stop GetAreaNodeCommunityVersion.", logger.DEBUG);
                    
//                        Map<String, ArrayList<String[]>> area_arp_mac_table = new HashMap();
                        for (Map.Entry<String, ArrayList<String[]>> area : area_node_community_version.entrySet()) {
                            String area_name = area.getKey();
                            logger.Println("Start ARP MAC area " + area_name + " ...", logger.DEBUG);
                            ArrayList<String[]> node_community_version = area.getValue();
                            ArrayList<String[]> arp_mac_table = utils.GetArpMacFromNodes(node_community_version);
                            area_arp_mac_table.put(area_name, arp_mac_table);
                            utils.WriteArrayListToFile("arp_mac_table_" + area_name, arp_mac_table);
                            logger.Println("Stop ARP MAC area " + area_name + ".", logger.DEBUG);
                        }

//                        informationFromNodesAllAreas=utils.ReadJSONFile("informationFromNodesAllAreas");
                        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                            String area_name = area.getKey();
                            Map val = area.getValue();
                            Map<String, Map> nodes_information = (Map<String, Map>) val.get("nodes_information");
                            ArrayList<ArrayList<String>> links = (ArrayList<ArrayList<String>>) val.get("links");
                            Map node_protocol_accounts = (Map) val.get("node_protocol_accounts");

                            logger.Println("Calculate mac_ip_node_port for area: " + area_name, logger.DEBUG);
//                            ArrayList<String[]> arp_mac_table = utils.ReadARP_Mac_FromNodes("arp_mac_table_"+area_name);
                            ArrayList<String[]> arp_mac_table = area_arp_mac_table.get(area_name);
                            if (arp_mac_table != null && arp_mac_table.size() > 0) {
                                ArrayList<String[]> mac_ip_node_port = utils.CalculateARPMAC(arp_mac_table, links, nodes_information, node_protocol_accounts);
                                val.put("mac_ip_port", mac_ip_node_port);
                            }
                        }
                        
                        // remove old map files
                        logger.Println("Remove old map and log files", logger.DEBUG);
                        utils.RemoveOldFiles(history_dir, history_num_days);

                        // remove advanced information 
                        logger.Println("Remove advanced information", logger.DEBUG);
                        for (Map.Entry<String, Map> area : ((Map<String, Map>) informationFromNodesAllAreas).entrySet()) {
                            String area_name = area.getKey();
                            Map<String, Map> val = area.getValue();
                            // remove advanced information  from nodes
                            Map<String, Map> info_nodes_tmp1 = (Map<String, Map>) val.get("nodes_information");
                            for (Map.Entry<String, Map> entry : info_nodes_tmp1.entrySet()) {
                                String node = entry.getKey();
                                Map val1 = entry.getValue();
                                if (val1.get("advanced") != null) {
                                    val1.remove("advanced");
                                }
                            }
                        }

                        // write to file nodes_information
                        utils.MapToFile((Map) informationFromNodesAllAreas, "info8");

                        // Normalization information map
                        logger.Println("Normalization information map", logger.DEBUG);
                        informationFromNodesAllAreas = utils.NormalizeMap(informationFromNodesAllAreas, area_arp_mac_table);
                                
                        // write to file nodes_information
                        utils.MapToFile((Map) informationFromNodesAllAreas, "info9");

                        ////////////// Calculate forks links /////////////////////////////////////////
                        Map<String, ArrayList> area_forks = utils.GetForkList(informationFromNodesAllAreas);
                        Map<String, ArrayList> area_add_del_links = utils.GetForkLinks(area_forks, informationFromNodesAllAreas, area_arp_mac_table);
                        utils.ModifyLinks(informationFromNodesAllAreas, area_add_del_links);
                        /////////////////////////////////////////////////////                        

                        // write to file nodes_information
                        utils.MapToFile((Map) informationFromNodesAllAreas, "info10");
                      
                        
                        // replace map file to history
                        logger.Println("Replace map file to history", logger.DEBUG);
                        File file = new File(map_file);
                        if (file.exists()) {
//                            Date dd = new Date(file.lastModified());
//                            SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy-HH.mm");
                            String file_history = history_dir + "/Neb_" + utils.GetFileCreateTime(map_file).replace(" ", "-") + ".map";
                            File folder_history = new File(history_dir);
                            File history = new File(file_history);
                            if (!folder_history.exists()) {
                                folder_history.mkdir();
                            }
                            if (!history.exists()) {
                                try {
                                    Files.copy(file.toPath(), history.toPath());
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                    java.util.logging.Logger.getLogger(Neb.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            }
                        }
                        
                        // write to file nodes_information
                        logger.Println("Write to "+map_file_pre+" nodes_information", logger.DEBUG);
                        INFORMATION = utils.SetInformations(informationFromNodesAllAreas, INFORMATION);
                        utils.MapToFile((Map) INFORMATION, map_file_pre);
                        
                        // set nodes attributes
                        INFORMATION = utils.SetAttributesToNodes(INFORMATION, map_file, node_attribute_old_file); 
                        utils.MapToFile((Map) INFORMATION, map_file_pre);
                        
//////////////////////////////////////////////////////////////////////////
                        // running Scripts
                        logger.Println("Starting running scripts ...", logger.INFO);
                        logger.Println("Run script: " + run_post_scripts, logger.DEBUG);
                        utils.RunScripts(run_post_scripts);
                        try {
                            Thread.sleep(120000);
                        } catch (java.lang.InterruptedException e) { }
                        logger.Println("Stop running scripts.", logger.INFO);
//////////////////////////////////////////////////////////////////////////  

                        utils.SetName(map_file_pre, names_info);
                        
                        // set nodes attributes
                        INFORMATION = utils.ReadJSONFile("neb.map.pre");
                        INFORMATION = utils.SetAttributesToNodes(INFORMATION, map_file, node_attribute_old_file);
                        
                        // Graph layout
                        GraphLayout graphLayout = new GraphLayout();
                        INFORMATION = graphLayout.StartGraphLayout(INFORMATION);
                        
                        utils.MapToFile((Map) INFORMATION, map_file);
              
                        // delete file map_file_pre
                        File f_pre = new File(map_file_pre);
                        if (f_pre.exists())
                            f_pre.delete();
                        
                        utils.SetFileCreationDateNow(map_file);
                        
                        informationFromNodesAllAreas.clear();

                        // indexing
                        logger.Println("Start indexing ...", logger.INFO);
                        INDEX = new HashMap();
                        utils.Indexing(INDEX);
                        logger.Println("Stop indexing.", logger.INFO);
                    }

                    logger.Println("Stop running Neb program ...", logger.INFO);
                }
            }
            try {
                Thread.sleep(10000);
            } catch (java.lang.InterruptedException e) {
                if (DEBUG) {
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        }
    }

}
