package ru.kos.cisco_information;

import com.sonalake.utah.Parser;
import com.sonalake.utah.config.Config;
import com.sonalake.utah.config.ConfigLoader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.io.StringReader;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import json.JSON;

import ru.kos.terminal.Terminal;
import ru.kos.expect.Expect;

public class Cisco_information {
    private static int timeout=120;  // expect timeout sec.
    private static int timeout_ssh_telnet=timeout/6; // telnet ssh timeout sec.    
    private static List<Pattern> stop_symbols = new ArrayList();
    private static Cisco_information cisco_information = new Cisco_information();
    private static ArrayList conn = new ArrayList();
    private static Terminal terminal = null;
    private static int retries=1;
    public static String cmd = "";
    private static Log log;

    private static boolean DEBUG = false;
    private static boolean CHECK_PARENT = true;
    
    Cisco_information() {
//        stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+#"));
//        stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+>"));        
    }   
    
    public static void main(String[] args) {
        int port_ssh = 22;
        int port_telnet = 23;
        BufferedReader br_STDIN = null;
        FileWriter file_writer = null;
        
//        String pid = ManagementFactory.getRuntimeMXBean().getName();
        
        log = new Log("out.out");
        log.SetLevel(log.DEBUG);
        
        
        if(CHECK_PARENT) {
            Check_Parent check_Parent = new Check_Parent();
            check_Parent.start();
        }        
        try {   
            br_STDIN = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
//                    final File temp = File.createTempFile("out-temp", ".tmp");
                    if(DEBUG) System.out.print("Type command: ");
                    cmd = br_STDIN.readLine();
                    if(DEBUG) System.out.println("command: "+cmd);
                    if(cmd != null && !cmd.equals("")) {
                        log.Println("command: "+cmd, log.DEBUG);                        
//                        file_writer = new FileWriter(temp, true);
//                        terminal = new Terminal(file_writer);                         
                        
                        if ("QUIT".equals(cmd)) {
                            log.Println("Exit!", log.DEBUG);
                            System.out.println("Exit!");
                            System.exit(0);
                        }
                        String[] mas = cmd.split("\\s+");
                        if(mas.length == 3) {
                            String node = mas[0];
                            Terminal.node_node = node;
                            String protocol = mas[1];
                            String command = mas[2];
                            String output = "";
                            if(protocol.toLowerCase().equals("ssh") || protocol.toLowerCase().equals("telnet")) {
                                command = command.replace("\\;", "<REPLACE_SEMICOLON>").replace("\\:", "<REPLACE_COLON>");
                                String[] mas1 = command.split(";");
                                for(String account : mas1) {
                                    String[] mas2 = account.split(":");
                                    if(mas2.length >= 2 && mas2.length <= 5) {
                                        String user = mas2[0].replace("<REPLACE_SEMICOLON>", ";").replace("<REPLACE_COLON>", ":");
                                        String passwd = mas2[1].replace("<REPLACE_SEMICOLON>", ";").replace("<REPLACE_COLON>", ":");
                                        String enable_passwd = "";
                                        if(mas2.length >= 3) enable_passwd=mas2[2].replace("<REPLACE_SEMICOLON>", ";").replace("<REPLACE_COLON>", ":");
                                        if(mas2.length >= 4) { 
                                            timeout_ssh_telnet=Integer.parseInt(mas2[3]);
                                            timeout=timeout_ssh_telnet*10;                                           
                                        }
                                        if(mas2.length >= 5) retries=Integer.parseInt(mas2[4]);

                                        Map<String, Map> out = new HashMap();
                                        if(protocol.toLowerCase().equals("ssh")) {
                                            out = cisco_information.GetSSH(node, user, passwd, enable_passwd, port_ssh);
                                        } else if(protocol.toLowerCase().equals("telnet")) {
                                            out = cisco_information.GetTelnet(node, user, passwd, enable_passwd, port_telnet);
                                        }            

                                        // check consistent data
                                        if(out.get(node) != null &&
                                                out.get(node).get("general") != null &&
                                                out.get(node).get("interfaces") != null &&
                                                ((Map)out.get(node).get("general")).size() > 0 && ((Map)out.get(node).get("interfaces")).size() > 0 ) {
                                            JSON jSON = new JSON();
                            //                String output = cisco_information.PrettyOut(jSON.mapToJSON(out));
                                            output = jSON.mapToJSON(out);

                                            if(!output.equals("")) break;
                                        }
                                    }
                                }
                            }
                            if(!output.equals("")) System.out.println("<result>"+output+"</result>\n");
                            else System.out.println("<result></result>\n");                            

                        } else System.out.println("<result></result>\n");
//                        if(file_writer != null) file_writer.close();  
                        log.Println("--- End command: "+cmd, log.DEBUG);
                    }
                } catch (Exception ex) {
                    for(StackTraceElement it : ex.getStackTrace()) {
                        log.Println("ex="+it.toString(), log.DEBUG);
                    }
                    log.Println("ex="+ex.toString(), log.DEBUG);
                    System.out.println("<result></result>\n");
                    if(DEBUG) ex.printStackTrace();
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) {  }
            }
        } finally {
            if (br_STDIN != null) {
                try {
                    br_STDIN.close();
                } catch (IOException e) {}
            }
        }         
    }
    
    public Map<String, Map> GetSSH(String node, String user, String passwd, String enable_passwd, int port_ssh) {
        Map result = new HashMap<>();
        stop_symbols.clear();
        stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+#"));
        stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+>")); 
        
        if(cisco_information.TCPportIsOpen(node, port_ssh, timeout_ssh_telnet)) {
            terminal = new Terminal(timeout_ssh_telnet, timeout);
            conn = terminal.ConnectSSH(node, user, passwd, enable_passwd, stop_symbols);
            if(conn.size() == 3) {
                String[] mas = ((List<Pattern>)conn.get(2)).get(0).toString().split("\\n");
                stop_symbols.clear();
                stop_symbols.add(Pattern.compile(mas[mas.length-1]));
                result=cisco_information.GetInformation();
            }
            terminal.DisconnectSSH(conn);
        }
        Map<String, Map> out = new HashMap<>();
        if(result.size() > 0) out.put(node, result);        
        
        return out;
    }
    
    public Map<String, Map> GetTelnet(String node, String user, String passwd, String enable_passwd, int port_telnet) {
        Map result = new HashMap<>();
        stop_symbols.clear();
        stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+#"));
        stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+>")); 
        
        if(cisco_information.TCPportIsOpen(node, port_telnet, timeout_ssh_telnet)) {
            terminal = new Terminal(timeout_ssh_telnet, timeout);
            conn = terminal.ConnectTelnet(node, user, passwd, enable_passwd, stop_symbols);
            if(conn.size() == 3) {
                String[] mas = ((List<Pattern>)conn.get(2)).get(0).toString().split("\\n");
                stop_symbols.clear();
                stop_symbols.add(Pattern.compile(mas[mas.length-1]));
                result=cisco_information.GetInformation();
            }
            terminal.DisconnectTelnet(conn);
        }
        
        Map<String, Map> out = new HashMap<>();
        if(result.size() > 0) out.put(node, result);
        
        return out;
    }
    
    private Map GetInformation() {
//        ArrayList result = new ArrayList();
        Map result = new HashMap<>();
//        log.Println("stop_symbols - "+stop_symbols.toString(), log.DEBUG);
        String out=terminal.RunCommand((Expect)conn.get(1), "sh ver", stop_symbols);
        
        ////////// Cisco IOS //////////
        Pattern p = Pattern.compile("[\r\n\\s]IOS[\r\n\\s]");  
        Matcher m = p.matcher(out);  
        if(m.find()) {
            ///////////// general section ///////////
            if(DEBUG) System.out.println("Start get general informations ...");
//            log.Println("Start get general informations ...", log.DEBUG);
            Map general = Get_General_Cisco_Info();
            if(general.size() > 0) result.putAll(general);
            if(DEBUG) System.out.println("Stop get general informations.");
//            log.Println("Stop get general informations.", log.DEBUG);
            ///////////// 802_1X section /////
            if(DEBUG) System.out.println("Start get 802_1X informations ...");
//            log.Println("Start get 802_1X informations ...", log.DEBUG);
//            Map iface_ip_mac = Get_802_1X_Cisco_info();
//            if(iface_ip_mac.size() > 0) result.putAll(iface_ip_mac);
            if(DEBUG) System.out.println("Stop get 802_1X informations.");
//            log.Println("Stop get 802_1X informations.", log.DEBUG);
            ///////////// cdp lldp section /////
            if(DEBUG) System.out.println("Start get cdp lldp informations ...");
//            log.Println("Start get cdp lldp informations ...", log.DEBUG);
            Map dp = Get_DP_Cisco_info();
            if(dp.size() > 0) {
                if(result.get("advanced") != null) {
                    ((Map)result.get("advanced")).putAll(dp);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("advanced", dp);
                    result.putAll(tmp);
                }
            }            
            if(DEBUG) System.out.println("Stop get cdp lldp informations.");
//            log.Println("Stop get cdp lldp informations.", log.DEBUG);
            ///////////// interfaces section ///////////
            if(DEBUG) System.out.println("Start get interfaces informations ...");
//            log.Println("Start get interfaces informations ...", log.DEBUG);
            Map interfaces = Get_Interfaces_Cisco_info();
            if(interfaces.size() > 0) result.putAll(interfaces);
            if(DEBUG) System.out.println("Stop get interfaces informations.");
//            log.Println("Stop get interfaces informations.", log.DEBUG);
            ///////////// vlans section ///////////
            if(DEBUG) System.out.println("Start get vlans informations ...");
//            log.Println("Start get vlans informations ...", log.DEBUG);
            Map vlans = Get_Vlans_Cisco_info();
            if(vlans.size() > 0) result.putAll(vlans);
            if(DEBUG) System.out.println("Stop get vlans informations.");
//            log.Println("Stop get vlans informations.", log.DEBUG);
            ///////////// acl section ///////////
            if(DEBUG) System.out.println("Start get acl informations ...");
//            log.Println("Start get acl informations ...", log.DEBUG);
            Map acl = Get_ACL_Cisco_info();
            if(acl.size() > 0) result.putAll(acl);
            if(DEBUG) System.out.println("Stop get acl informations.");
//            log.Println("Stop get acl informations.", log.DEBUG);
            ///////////// routes section //////////
            if(DEBUG) System.out.println("Start get routes informations ...");
//            log.Println("Start get routes informations ...", log.DEBUG);
            Map routes = Get_Route_Cisco_info();
            if(routes.size() > 0) result.putAll(routes);
            if(DEBUG) System.out.println("Stop get routes informations.");
//            log.Println("Stop get routes informations.", log.DEBUG);
            ///////////// ip mac table section /////
//            System.out.println("Start get mac table informations ...");
//            Map mac_table = Get_Mac_Cisco_info();
//            if(mac_table.size() > 0) result.putAll(mac_table);
//            System.out.println("Stop get mac table informations.");
            ///////////// ip arp table section /////
            if(DEBUG) System.out.println("Start get arp table informations ...");
//            log.Println("Start get arp table informations ...", log.DEBUG);
            Map arp_table = Get_Arp_Cisco_info();
            if(arp_table.size() > 0) {
                if(result.get("advanced") != null) {
                    ((Map)result.get("advanced")).putAll(arp_table);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("advanced", arp_table);
                    result.putAll(tmp);
                }
            }
            if(DEBUG) System.out.println("Stop get arp table informations.");
//            log.Println("Stop get arp table informations.", log.DEBUG);
            ///////////// run config section ///////////
            if(DEBUG) System.out.println("Start get config informations ...");
//            log.Println("Start get config informations ...", log.DEBUG);
            Map config = Get_Run_Cisco();
            if(config.size() > 0) result.putAll(config);
            if(DEBUG) System.out.println("Stop get config informations.");
//            log.Println("Stop get config informations.", log.DEBUG);
        }
        
        ////////// Cisco NX-OS //////////
        Pattern p1 = Pattern.compile("[\r\n\\s]\\(NX-OS\\)[\r\n\\s]");  
        Matcher m1 = p1.matcher(out);  
        if(m1.find()) {
            ///////////// general section ///////////
            if(DEBUG) System.out.println("Start get general informations ...");
            Map general = Get_General_Nexus_Info();
            if(general.size() > 0) result.putAll(general);
            if(DEBUG) System.out.println("Stop get general informations.");
            ///////////// cdp lldp section /////
            if(DEBUG) System.out.println("Start get cdp lldp informations ...");
            Map dp = Get_DP_Nexus_info();
            if(dp.size() > 0) {
                if(result.get("advanced") != null) {
                    ((Map)result.get("advanced")).putAll(dp);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("advanced", dp);
                    result.putAll(tmp);
                }
            }   
            if(DEBUG) System.out.println("Stop get cdp lldp informations.");
            ///////////// interfaces section ///////////
            if(DEBUG) System.out.println("Start get interfaces informations ...");
            Map interfaces = Get_Interfaces_Nexus_info();
            if(interfaces.size() > 0) result.putAll(interfaces);
            if(DEBUG) System.out.println("Stop get interfaces informations.");
            ///////////// vlans section ///////////
            if(DEBUG) System.out.println("Start get vlans informations ...");
            Map vlans = Get_Vlans_Cisco_info();
            if(vlans.size() > 0) result.putAll(vlans);
            if(DEBUG) System.out.println("Stop get vlans informations.");
            ///////////// acl section ///////////
            if(DEBUG) System.out.println("Start get acl informations ...");
            Map acl = Get_ACL_Cisco_info();
            if(acl.size() > 0) result.putAll(acl); 
            if(DEBUG) System.out.println("Stop get acl informations.");
            ///////////// routes section //////////
            if(DEBUG) System.out.println("Start get routes informations ...");
            Map routes = Get_Route_Nexus_info();
            if(routes.size() > 0) result.putAll(routes);
            if(DEBUG) System.out.println("Stop get routes informations.");
            ///////////// ip mac table section /////
//            System.out.println("Start get mac table informations ...");
//            Map mac_table = Get_Mac_Cisco_info();
//            if(mac_table.size() > 0) result.putAll(mac_table);
//            System.out.println("Stop get mac table informations.");
            ///////////// ip arp table section /////
            if(DEBUG) System.out.println("Start get arp table informations ...");
            Map arp_table = Get_Arp_Cisco_info();
            if(arp_table.size() > 0) {
                if(result.get("advanced") != null) {
                    ((Map)result.get("advanced")).putAll(arp_table);
                } else {
                    Map tmp = new HashMap();
                    tmp.put("advanced", arp_table);
                    result.putAll(tmp);
                }
            }
            if(DEBUG) System.out.println("Stop get arp table informations.");
            ///////////// run config section ///////////
            if(DEBUG) System.out.println("Start get config informations ...");
            Map config = Get_Run_Cisco();
            if(config.size() > 0) result.putAll(config);
            if(DEBUG) System.out.println("Stop get config informations.");
        }        
        
        return result;
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
   
    private List<Map<String, String>> parse_out(String in, String config_file) {
        List<Map<String, String>> observedValues = new ArrayList<Map<String, String>>();
        try {
            // load a config, using a URL or a Reader
            URL configURL = Thread.currentThread().getContextClassLoader().getResource(config_file);
            Config config = new ConfigLoader().loadConfig(configURL);
            
            // load a file and iterate through the records
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
            
            return observedValues;
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
    
    private Map Get_General_Cisco_Info() {
        String out=terminal.RunCommand((Expect)conn.get(1), "sh ver", stop_symbols);
        Map general = new HashMap<>();
        List<Map<String, String>> out_list = parse_out(out, "Templates/cisco_version_template.xml");
        general.putAll(out_list.get(0));
        // sh idprom all
        out=terminal.RunCommand((Expect)conn.get(1), "sh idprom all", stop_symbols);
        List<Map<String, Map<String, String>>> idprom = Parse_Idprom(out);
        if(idprom.size() > 0) general.put("idprom", idprom);
        
        String sysname = stop_symbols.toArray()[0].toString().replace("\\", "").replaceAll("[\n\r]", "").replace("#", "").replace(" ", "");
        general.put("sysname", sysname);
        
        Map<String, Map<String, String[]>> result = new HashMap<>();
        if(general.size() > 0) result.put("general", general);          
        return result;
    }
    
    private Map<String, Map<String, Map<String, String>>> Get_Interfaces_Cisco_info() {
        String out_interfaces=terminal.RunCommand((Expect)conn.get(1), "sh interfaces", stop_symbols);
        List<Map<String, String>> interfaces = Parse_Cisco_Interfaces(out_interfaces);  
        String out_interfaces_switchport=terminal.RunCommand((Expect)conn.get(1), "sh interfaces switchport", stop_symbols);
        List<Map<String, String>> interfaces_switchport = Parse_Cisco_Interfaces_Switchport(out_interfaces_switchport);
        String out_interfaces_etherchannel=terminal.RunCommand((Expect)conn.get(1), "sh interfaces etherchannel", stop_symbols);
        List<Map> interfaces_etherchannel = Parse_Cisco_Interfaces_Etherchannel(out_interfaces_etherchannel);
        String out_ip_interface=terminal.RunCommand((Expect)conn.get(1), "sh ip interface", stop_symbols);
        List<Map<String, String>> interfaces_ip = Parse_Cisco_Interfaces_Ip(out_ip_interface);
        String out_hsrp=terminal.RunCommand((Expect)conn.get(1), "sh standby", stop_symbols);
        Map<String, String> hsrp_ip = Parse_Cisco_HSRP(out_hsrp);        

        Map<String, Map> interfaces_map = new HashMap();
        for(Map<String, String> it : interfaces) {
            String interface_name="";
            Map map_tmp = new HashMap();
            for(Map.Entry<String, String> entry : it.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if(key.equals("interface")) interface_name=val;
                else map_tmp.put(key, val);
            }
            if(!interface_name.equals("")) {
                String iface_short = Interface_Full_To_Short_name(interface_name);
                map_tmp.put("iface_short", iface_short);
                interfaces_map.put(interface_name, map_tmp);
            }
        }
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            if(val.get("iface_short") != null) {
                String iface_short=(String)val.get("iface_short");
                for(Map<String, String> it : interfaces_switchport) {
                    if(it.get("interface") != null && it.get("interface").equals(iface_short)) {
                        val.putAll(it);
                        break;
                    }
                }
            }
        }
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            for(Map<String, String> it : interfaces_etherchannel) {
                if(it.get("interface") != null && it.get("interface").equals(iface)) {
                    val.putAll(it);
                    break;
                }
            }  
        }
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            for(Map<String, String> it : interfaces_ip) {
                if(it.get("interface") != null && it.get("interface").equals(iface)) {
                    val.putAll(it);
                    break;
                }
            }  
        }        
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            if(hsrp_ip.get(iface) != null) {
                if(val.get("ip") != null) {
                    String ip_exist = (String)((ArrayList)val.get("ip")).get(0);
                    String[] mas = ip_exist.split("/");
                    if(mas.length != 2) {
                        mas = ip_exist.split(" ");
                    }
                    if(mas.length == 2) {
                        String mask = mas[1];
                        ((ArrayList)val.get("ip")).add(hsrp_ip.get(iface)+"/"+mask);
                    }
                }
                
            }
        }
        
        Map out = new HashMap<>();
        if(interfaces_map.size() > 0) out.put("interfaces", interfaces_map);
        
        return out;
    }
    
    private Map Get_Vlans_Cisco_info() {
        String out_vlans=terminal.RunCommand((Expect)conn.get(1), "sh vlan", stop_symbols);
        Map<String, String> vlans = new HashMap();
        for(String line : out_vlans.split("\n")) {
            Pattern p = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+active.*$");  
            Matcher m = p.matcher(line);
            if(m.find()) 
                vlans.put(m.group(1), m.group(2));
        }         
        Map result = new HashMap<>();
        if(vlans.size() > 0) result.put("vlans", vlans);
        
        return result;        
    }
    
    private Map<String, String> Get_Run_Cisco() {
        String out_run=terminal.RunCommand((Expect)conn.get(1), "sh run", stop_symbols);
        Map<String, String> result = new HashMap<>();
        if(!out_run.equals("")) result.put("config", out_run);
        
        return result;        
    }
    
    private Map<String, Map<String, ArrayList<String>>> Get_ACL_Cisco_info() {
        String out_acl=terminal.RunCommand((Expect)conn.get(1), "sh ip access-lists", stop_symbols);
        out_acl=out_acl.replaceAll("[\r\n](\\S+)", "<delimiter>$1");
        String[] mas = out_acl.split("<delimiter>");
    
        Map<String, ArrayList<String>> result = new HashMap();
        if(mas.length > 0) {
            for(int i=1; i<mas.length; i++) {
                String[] mas1 = mas[i].split("\n");
                String key=mas1[0];
                ArrayList<String> list_rules = new ArrayList();
                for(int ii=1; ii<mas1.length; ii++) {
                    Pattern p = Pattern.compile("^\\s+\\d+\\s+(.+)(|\\s+\\(.+\\))");  
                    Matcher m = p.matcher(mas1[ii]);
                    if(m.find()) 
                        list_rules.add(m.group(1));
                }
                result.put(key, list_rules);
            }
        }
        
        Map<String, Map<String, ArrayList<String>>> out = new HashMap<>();
        if(result.size() > 0) out.put("ACL", result);
        
        return out;
    }
    
    private Map Get_Route_Cisco_info() {
        String out_route=terminal.RunCommand((Expect)conn.get(1), "sh ip route", stop_symbols);
        Pattern p_err = Pattern.compile("Invalid input detected");
        Matcher m_err = p_err.matcher(out_route);
        if(m_err.find()) out_route="";
        ArrayList route = new ArrayList();
        String[] mas = out_route.split("\n");
        for(String line : mas) {
            Pattern p = Pattern.compile("^(\\S\\S*\\s+.+)$");
            Matcher m = p.matcher(line);
            if(m.find()) route.add(m.group(1).replace("[", "(").replace("]", ")"));
        }         

        Map result = new HashMap<>();
        if(route.size() > 0) {
            result.put("routes", route);
        }

        return result;        
    }    
    
    private Map<String, Map<String, ArrayList<String>>> Get_Mac_Cisco_info() {
        int retries = 1;
        
        Map<String, String> mac_table = new HashMap();
        for(int i=0; i<retries; i++) {
            int num_mac_records=0;
            String out_mac=terminal.RunCommand((Expect)conn.get(1), "sh mac address-table", stop_symbols);
            if(!out_mac.equals("")) {
                String[] mas = out_mac.split("\n");
                for(String line : mas) {
                    String[] mas1=line.split("\\s+");
                    String mac="";
                    String port="";
                    Pattern p1 = Pattern.compile("[0-9a-f]{4}\\.[0-9a-f]{4}\\.[0-9a-f]{4}");
                    for(String buff : mas1) {
                        Matcher m1 = p1.matcher(buff);
                        if(m1.find()) {
                            mac=buff;
                            port=mas1[mas1.length-1];
                            break;
    //                        System.out.println(mac+"\t"+port);
                        }
                    }
                    if(!mac.equals("") && !port.equals("") && !port.contains(",")) {
                        String[] mac_port = new String[2];
                        mac_port[0]=mac; mac_port[1]=port;
                        if(mac_table.get(mac_port[0]) == null) {
                            mac_table.put(mac_port[0], mac_port[1]);
                            num_mac_records++;
                        }
                    }
                }
            }

            System.out.println("retries="+i+" - "+"num_mac_records="+num_mac_records);
        }            
        
        // translate in result id_iface to id_iface_translate
        Map<String, ArrayList<String>> map = new HashMap<>();
        for (Map.Entry<String, String> entry : mac_table.entrySet()) {
            String mac = entry.getKey();
            String iface = entry.getValue();
//        for (String[] it : mac_table) {
            if(map.get(iface) != null) {
                ArrayList<String> mac_list=map.get(iface);
                mac_list.add(mac);
            } else {
                ArrayList<String> mac_list = new ArrayList();
                mac_list.add(mac);
                map.put(iface, mac_list);
            }
        }
        
        // translate short iface name to long iface name
        Map<String, ArrayList<String>> map_translate = new HashMap<>();
        for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
            String short_iface_name = entry.getKey();
            ArrayList<String> mac_list = entry.getValue();
            String out_iface=terminal.RunCommand((Expect)conn.get(1), "sh interface "+short_iface_name, stop_symbols);
            Pattern p = Pattern.compile("^\\n*(\\S+)\\s+is\\s+.+");  
            Matcher m = p.matcher(out_iface);
            String long_iface_name="";
            if(m.find()) long_iface_name=m.group(1);
            if(!long_iface_name.equals("")) map_translate.put(long_iface_name, mac_list);
        }
        
        Map<String, Map<String, ArrayList<String>>> result = new HashMap<>();
        if(mac_table.size() > 0) result.put("mac", map_translate);
        
        return result;        
    }     

    private Map<String, Map<String, String>> Get_Arp_Cisco_info() {
        String out_arp=terminal.RunCommand((Expect)conn.get(1), "sh arp", stop_symbols);
       
        Map<String, String> arp_table = new HashMap();
        if(!out_arp.equals("")) {
            String[] mas = out_arp.split("\n");
            Pattern p = Pattern.compile("\\S+\\s+([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}).+([0-9a-f]{4}\\.[0-9a-f]{4}\\.[0-9a-f]{4})");            
            for(String line : mas) {
                Matcher m = p.matcher(line);
                String ip="";
                String mac="";
                if(m.find()) {
                    ip=m.group(1);
                    mac=m.group(2);
//                    System.out.println(ip+"\t"+mac);
                }
                if(!ip.equals("") && !mac.equals("")) {
                    arp_table.put(mac, ip);
                }
            }
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        if(arp_table.size() > 0) result.put("arp", arp_table);
        
        return result; 
    }     
    
    private Map Get_DP_Cisco_info() {
        Map<String, ArrayList<Map<String, String>>> cdp = Get_CDP_Cisco_info();
        Map<String, ArrayList<Map<String, String>>> lldp = Get_LLDP_Cisco_info();
        
        Map<String, ArrayList<Map<String, String>>> result = new HashMap<>();
        for (Map.Entry<String, ArrayList<Map<String, String>>> entry : cdp.entrySet()) {
            String local_port = entry.getKey();
            ArrayList<Map<String, String>> list_tmp = entry.getValue();
            for(Map<String, String> map_tmp : list_tmp)
                map_tmp.put("type", "cdp");
            result.put(local_port, list_tmp);
        }
        for (Map.Entry<String, ArrayList<Map<String, String>>> entry : lldp.entrySet()) {
            String local_port = entry.getKey();
            ArrayList<Map<String, String>> list_tmp = entry.getValue();
            for(Map<String, String> map_tmp : list_tmp)
                map_tmp.put("type", "lldp");
            
            String local_port1 = null;
            boolean found = false;
            for (Map.Entry<String, ArrayList<Map<String, String>>> entry1 : result.entrySet()) {
                local_port1 = entry1.getKey(); 
                if(EqualsIfaceName(local_port, local_port1)) {
                    found = true;
                    break;
                }
            }
            if(found)
                result.get(local_port1).addAll(list_tmp);
            else
                result.put(local_port, list_tmp);
        }        
        
        Map<String, Map<String, ArrayList<Map<String, String>>>> out = new HashMap<>();
        if(result.size() > 0) out.put("links", result);
        
        return out; 
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
    
    
    private Map<String, ArrayList<Map<String, String>>> Get_CDP_Cisco_info() {
        String out_cdp=terminal.RunCommand((Expect)conn.get(1), "sh cdp neighbors detail", stop_symbols);
        List<Map<String, String>> cdp = Parse_CDP_cisco(out_cdp); 
        
        Map<String, ArrayList<Map<String, String>>> result = new HashMap();
        for(Map<String, String> map : cdp) {
            if(map.get("local_port") != null) {
                String local_port = map.get("local_port");
                if(result.get(local_port) != null) {
                    result.get(local_port).add(map);
                } else {
                    ArrayList<Map<String, String>> list_tmp = new ArrayList();
                    list_tmp.add(map);
                    result.put(local_port, list_tmp);
                }
            }
        }
        
        return result;         
    }
    
    private Map<String, ArrayList<Map<String, String>>> Get_LLDP_Cisco_info() {
        Map<String, ArrayList<Map<String, String>>> result = new HashMap();
        
        String out_lldp=terminal.RunCommand((Expect)conn.get(1), "sh lldp neighbors", stop_symbols);

        String[] mas = out_lldp.split("\n");
        for(int i=0; i<mas.length; i++) mas[i]=mas[i].replaceAll("[\r\n]", "");
        int pos=-1;
        for(int i=0; i<mas.length; i++) {
            pos=i;
            if(mas[i].matches("Device ID\\s+Local Intf\\s+Hold-time\\s+Capability\\s+Port ID\\s*")) break;
        }
        ArrayList<String> lldp_interfaces = new ArrayList();
        for(int i=pos+1; i<mas.length; i++) {
            if(mas[i].equals("")) break;
            lldp_interfaces.add(mas[i].substring(20).split(" ")[0]);
//            System.out.println(mas[i].substring(20).split(" ")[0]);
        }
        
        String out_lldp_det=terminal.RunCommand((Expect)conn.get(1), "sh lldp neighbors detail", stop_symbols);
        List<Map<String, String>> lldp_det = Parse_Cisco_LLDP(out_lldp_det);     
        
        if(lldp_interfaces.size() == lldp_det.size()) {
            for(int i=0; i<lldp_interfaces.size(); i++) {
                String local_port = lldp_interfaces.get(i);
                String remote_ip = lldp_det.get(i).get("remote_ip");
                if(remote_ip == null) remote_ip="";
                String remote_port_description = lldp_det.get(i).get("remote_port_description");
                if(remote_port_description == null) remote_port_description="";
                String remote_id = lldp_det.get(i).get("remote_id");
                if(remote_id == null) remote_id="";
                String remote_version = lldp_det.get(i).get("remote_system_description");
                if(remote_version == null) remote_version="";
                String remote_port_id = lldp_det.get(i).get("remote_id");
                if(remote_port_id == null) remote_port_id="";
                String remote_chassis_id = lldp_det.get(i).get("remote_chassis_id");
                if(remote_chassis_id == null) remote_chassis_id="";
                if(remote_ip.equals(remote_port_id)) remote_port_id=remote_port_description;
                    
                if(remote_ip == null) remote_ip=remote_chassis_id;
                
                if(remote_ip != null && remote_id != null) {
                    Map<String, String> map_tmp = new HashMap();
                    map_tmp.put("remote_ip", remote_ip);
                    if(remote_port_description != null) map_tmp.put("remote_port_description", remote_port_description);
                    map_tmp.put("remote_id", remote_id);
                    if(remote_version != null) map_tmp.put("remote_version", remote_version.replace("\r", "").replace("\n", " "));
                    if(remote_port_id != null) map_tmp.put("remote_port_id", remote_port_id);
                    if(remote_chassis_id != null) map_tmp.put("remote_chassis_id", remote_chassis_id);
                    if(map_tmp.size() > 0) {
                        if(result.get(local_port) != null) {
                            result.get(local_port).add(map_tmp);
                        } else {
                            ArrayList<Map<String, String>> list_tmp = new ArrayList();
                            list_tmp.add(map_tmp);
                            result.put(local_port, list_tmp);
                        }        
                    }
                }
            }
        }
        
        return result;  
    } 
    
    private Map<String, Map<String, String[]>> Get_802_1X_Cisco_info() {
        String out_authentication_sessions=terminal.RunCommand((Expect)conn.get(1), "sh authentication sessions", stop_symbols);
       
        Map<String, String[]> interf_ip_mac = new HashMap();
        if(!out_authentication_sessions.equals("")) {
            String[] mas = out_authentication_sessions.split("\n");
            Pattern p = Pattern.compile("\\s*(\\S+)\\s+([0-9a-f]{4}\\.[0-9a-f]{4}\\.[0-9a-f]{4})");
            Pattern p_interf = Pattern.compile("\\s*Interface:\\s*(\\S+)");
            Pattern p_ip = Pattern.compile("\\s*[Ii][Pp] [Aa]ddress:\\s*(\\S+)");
            Pattern p_mac = Pattern.compile("\\s*MAC [Aa]ddress:\\s*(\\S+)");
            for(String line : mas) {
                Matcher m1 = p.matcher(line);
                if(m1.find()) {
                    String interf=m1.group(1);
                    String out_authentication_sessions_interf=terminal.RunCommand((Expect)conn.get(1), "sh authentication sessions interface "+interf, stop_symbols);
                    if(!out_authentication_sessions_interf.equals("")) {
                        String ip="";
                        String mac="";
                        String[] mas1 = out_authentication_sessions_interf.split("\n");
                        for(String line1 : mas1) {
                            Matcher m_interf = p_interf.matcher(line1);
                            if(m_interf.find()) interf=m_interf.group(1);
                            Matcher m_ip = p_ip.matcher(line1);
                            if(m_ip.find()) ip=m_ip.group(1);  
                            Matcher m_mac = p_mac.matcher(line1);
                            if(m_mac.find()) mac=m_mac.group(1);                             
                        }
                        if(!interf.equals("") && !ip.equals("") && !mac.equals("")) {
                            String[] ip_mac = new String[2];
                            ip_mac[0]=ip; ip_mac[1]=mac;
                            interf_ip_mac.put(interf, ip_mac);
                        }                        
//                        System.out.println(interf+"\t"+ip+"\t"+mac);
                    }
                }
            }
        }
        Map<String, Map<String, String[]>> out = new HashMap<>();
        if(interf_ip_mac.size() > 0) out.put("interf_ip_mac", interf_ip_mac);        

        return out; 
    }      
    
    private Map Get_General_Nexus_Info() {
        String out=terminal.RunCommand((Expect)conn.get(1), "sh ver", stop_symbols);        
        Map general = new HashMap<>();
        List<Map<String, String>> out_list = parse_out(out, "Templates/nexus_version_template.xml");
        general.putAll(out_list.get(0));
        // sh idprom all
//        out=terminal.RunCommand((Expect)conn.get(1), "sh idprom all", timeout, stop_symbols);
//        List<Map<String, Map<String, String>>> idprom = Parse_Idprom(out);
//        if(idprom.size() > 0) general.put("idprom", idprom);
        
        String sysname = stop_symbols.toArray()[0].toString().replace("\\", "").replaceAll("[\n\r]", "").replace("#", "").replace(" ", "");
        general.put("sysname", sysname);

        Map<String, Map<String, String[]>> result = new HashMap<>();
        if(general.size() > 0) result.put("general", general);          
        return result;
    }
    
    private Map Get_Interfaces_Nexus_info() {
        String out_interfaces=terminal.RunCommand((Expect)conn.get(1), "sh interface", stop_symbols);
        List<Map<String, String>> interfaces = Parse_Nexus_Interfaces(out_interfaces);  
        String out_interfaces_switchport=terminal.RunCommand((Expect)conn.get(1), "sh interface switchport", stop_symbols);
        List<Map<String, String>> interfaces_switchport = Parse_Nexus_Interfaces_Switchport(out_interfaces_switchport);
        String out_interfaces_etherchannel=terminal.RunCommand((Expect)conn.get(1), "sh port-channel summary", stop_symbols);
        String out_ip_interface=terminal.RunCommand((Expect)conn.get(1), "sh ip interface vrf all", stop_symbols);
        List<Map<String, String>> interfaces_ip = Parse_Nexus_Interfaces_Ip(out_ip_interface);        
        
        ArrayList<ArrayList> interfaces_etherchannel = new ArrayList();
        for(String line : out_interfaces_etherchannel.split("\n")) {
            Pattern p = Pattern.compile("^\\d+\\s+(\\S+)\\s+\\S+\\s+(\\S+)\\s+");  
            Matcher m = p.matcher(line);
            if(m.find()) {
                String portchannel = m.group(1).split("\\(")[0];
                String etherchannel_mode = m.group(2);
                String[] mas = line.split("\\s+");
                ArrayList<String> etherchannel_ports = new ArrayList();
                if(mas.length > 4) for(int i=4; i<mas.length; i++) etherchannel_ports.add(mas[i].split("\\(")[0]);
                ArrayList tmp_list = new ArrayList();
                tmp_list.add(portchannel);
                tmp_list.add(etherchannel_mode);
                tmp_list.add(etherchannel_ports);
                interfaces_etherchannel.add(tmp_list);
            }
        }

        Map<String, Map> interfaces_map = new HashMap();
        for(Map<String, String> it : interfaces) {
            String interface_name="";
            Map map_tmp = new HashMap();
            for(Map.Entry<String, String> entry : it.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if(key.equals("interface")) interface_name=val;
                else map_tmp.put(key, val);
            }
            if(!interface_name.equals("")) {
                String iface_short = Interface_Full_To_Short_name(interface_name);
                map_tmp.put("iface_short", iface_short);
                interfaces_map.put(interface_name, map_tmp);
            }
        }        
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            for(Map<String, String> it : interfaces_switchport) {
                if(it.get("interface") != null && it.get("interface").equals(iface)) {
                    val.putAll(it);
                    break;
                }
            }  
        }        
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            String iface_short=(String)val.get("iface_short");
            for(Map<String, String> it : interfaces_ip) {
                if(it.get("interface") != null && it.get("interface").equals(iface)) {
                    if(val.get("ip") != null) {
                        ArrayList<String> ip_list = new ArrayList(); 
                        ip_list.add((String)val.get("ip"));
                        val.put("ip", ip_list);
                    }                        
                    break;
                }
            }
        }
        
        for(Map.Entry<String, Map> entry : interfaces_map.entrySet()) {
            String iface = entry.getKey();
            Map val = entry.getValue();
            if(val.get("iface_short") != null) {
                String iface_short=(String)val.get("iface_short");
                iface_short=iface_short.toLowerCase();
                for(ArrayList it : interfaces_etherchannel) {
                    String portchannel=(String)it.get(0);
                    portchannel=portchannel.toLowerCase();
                    String etherchannel_mode=(String)it.get(1);
                    ArrayList<String> etherchannel_ports = (ArrayList)it.get(2);
                    if(iface_short.equals(portchannel)) {
                        val.put("etherchannel_mode", etherchannel_mode);
                        val.put("etherchannel_ports", etherchannel_ports);
                        break;
                    }
                }
            }
        }          

        Map<String, Map> out = new HashMap<>();
        if(interfaces_map.size() > 0) out.put("interfaces", interfaces_map);
        
        return out;        
    }
    
    private List<Map<String, String>> Parse_Nexus_Interfaces(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();

        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_interfaces_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("^[\n\r](\\S.+)", "<folder>$1</folder>");
            in=in.replaceAll("[\n\r](\\S.+)", "\n===\n<folder>$1</folder>");
            in=in.replaceAll("[\n\r] +(.+)", "\n<item>$1</item>");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
    
    private List<Map<String, String>> Parse_Nexus_Switchport(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();

        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_interfaces_switchport_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
    
    private Map Get_Route_Nexus_info() {
        String out_route=terminal.RunCommand((Expect)conn.get(1), "sh ip route vrf all", stop_symbols);
        Pattern p_err = Pattern.compile("Invalid input detected");
        Matcher m_err = p_err.matcher(out_route);
        if(m_err.find()) out_route="";         
        ArrayList route = new ArrayList();
        String[] mas = out_route.split("\n");
        for(String line : mas) {
            route.add(line.replace("[", "(").replace("]", ")"));
        }         

        Map result = new HashMap<>();
        if(route.size() > 0) {
            result.put("routes", route);
        }

        return result;        
    }    
    
    private Map Get_DP_Nexus_info() {
        Map<String, ArrayList<Map<String, String>>> cdp = Get_CDP_Nexus_info();
        Map<String, ArrayList<Map<String, String>>> lldp = Get_LLDP_Nexus_info();
        
        Map<String, ArrayList<Map<String, String>>> result = new HashMap<>();
        for (Map.Entry<String, ArrayList<Map<String, String>>> entry : cdp.entrySet()) {
            String local_port = entry.getKey();
            ArrayList<Map<String, String>> list_tmp = entry.getValue();
            for(Map<String, String> map_tmp : list_tmp)
                map_tmp.put("type", "cdp");
            result.put(local_port, list_tmp);
        }
        for (Map.Entry<String, ArrayList<Map<String, String>>> entry : lldp.entrySet()) {
            String local_port = entry.getKey();
            ArrayList<Map<String, String>> list_tmp = entry.getValue();
            for(Map<String, String> map_tmp : list_tmp)
                map_tmp.put("type", "lldp");
            String local_port1 = null;
            for (Map.Entry<String, ArrayList<Map<String, String>>> entry1 : cdp.entrySet()) {
                local_port1 = entry1.getKey(); 
                if(EqualsIfaceName(local_port, local_port1)) {
                    break;
                }
            }
            if(local_port1 != null)
                result.get(local_port1).addAll(list_tmp);
            else
                result.put(local_port, list_tmp);
        }        
        
        Map<String, Map<String, ArrayList<Map<String, String>>>> out = new HashMap<>();
        if(result.size() > 0) out.put("links", result);        
        return out; 
    }   
    
    private Map<String, ArrayList<Map<String, String>>> Get_CDP_Nexus_info() {
        String out_cdp=terminal.RunCommand((Expect)conn.get(1), "sh cdp neighbors detail", stop_symbols);
        List<Map<String, String>> cdp = Parse_CDP_nexus(out_cdp); 
        
        Map<String, ArrayList<Map<String, String>>> result = new HashMap();
        for(Map<String, String> map : cdp) {
            if(map.get("local_port") != null) {
                String local_port = map.get("local_port");
                if(result.get(local_port) != null) {
                    result.get(local_port).add(map);
                } else {
                    ArrayList<Map<String, String>> list_tmp = new ArrayList();
                    list_tmp.add(map);
                    result.put(local_port, list_tmp);
                }
            }
        }        
        
        return result;         
    }
    
    private Map<String, ArrayList<Map<String, String>>> Get_LLDP_Nexus_info() {
        Map<String, ArrayList<Map<String, String>>> result = new HashMap();
        
        String out_lldp_det=terminal.RunCommand((Expect)conn.get(1), "sh lldp neighbors detail", stop_symbols);
        List<Map<String, String>> lldp_det = Parse_Nexus_LLDP(out_lldp_det);     
        
        for(Map lldp : lldp_det) {
            if(lldp.size() > 0) {
                String local_port = (String)lldp.get("local_port");
                String remote_ip = (String)lldp.get("remote_ip");
                String remote_port_description = (String)lldp.get("remote_port_description");
                String remote_id = (String)lldp.get("remote_id");
                String remote_version = (String)lldp.get("remote_system_description");
                String remote_port_id = (String)lldp.get("remote_id");
                String remote_chassis_id = (String)lldp.get("remote_chassis_id");
                if(remote_ip == null) remote_ip=remote_chassis_id;
                if(remote_id != null && remote_port_id != null)
                    if(remote_id.equals(remote_port_id)) remote_port_id=remote_port_description;

                Map<String, String> map_tmp = new HashMap();
                if(local_port != null && remote_ip != null && remote_id != null) {
                    map_tmp.put("remote_ip", remote_ip);
                    if(remote_port_description != null) map_tmp.put("remote_port_description", remote_port_description);
                    map_tmp.put("remote_id", remote_id);
                    if(remote_version != null) map_tmp.put("remote_version", remote_version.replace("\r", "").replace("\n", " "));
                    if(remote_port_id != null) map_tmp.put("remote_port_id", remote_port_id);
                    if(remote_chassis_id != null) map_tmp.put("remote_chassis_id", remote_chassis_id);
                    if(map_tmp.size() > 0) {
                        if(result.get(local_port) != null) {
                            result.get(local_port).add(map_tmp);
                        } else {
                            ArrayList<Map<String, String>> list_tmp = new ArrayList();
                            list_tmp.add(map_tmp);
                            result.put(local_port, list_tmp);
                        }        
                    }
                }
            }
        }

        return result;         
    }    
    
    private List<Map<String, Map<String, String>>> Parse_Idprom(String in) {
        List<Map<String, Map<String, String>>> out_list_map = new ArrayList<>();
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_idprom_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("[\n\r](\\S.+)", "\n<folder>$1</folder>");
            in=in.replaceAll("[\n\r] +(.+)", "\n<item>$1</item>");
            in=in.replaceAll("\r\n\r\n", "\n===\n");
            
            List<Map<String, String>> observedValues = new ArrayList<>();
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
            
            for(Map<String, String> item : observedValues) {
                if(item.size() == 3) {
                    Map<String, String> it = new HashMap<>();
                    it.put("product_number", item.get("product_number"));
                    it.put("serial_number", item.get("serial_number"));
                    Map<String, Map<String, String>> folder = new HashMap<>();
                    folder.put(item.get("module"), it);
                    out_list_map.add(folder);
                }
            }
            
            
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out_list_map;
    }
    
    private List<Map<String, String>> Parse_Cisco_LLDP(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_LLDP_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("^[\n\r]-+", "");
//            in=in.replaceAll("[\n\r](\\S.+)", "\n===\n<folder>$1</folder>");
//            in=in.replaceAll("[\n\r] +(.+)", "\n<item>$1</item>");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
    
    private List<Map<String, String>> Parse_Nexus_LLDP(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_lldp_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("\n\r*\n", "\n===\n");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        List<Map<String, String>> result = new ArrayList<>();
        if(observedValues.size() > 1) {
            for(int i=1; i<observedValues.size(); i++) result.add(observedValues.get(i));
        }

        return result;
    }
    
    private List<Map<String, String>> Parse_Cisco_Interfaces(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_interfaces_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("^[\n\r](\\S.+)", "<folder>$1</folder>");
            in=in.replaceAll("[\n\r](\\S.+)", "\n===\n<folder>$1</folder>");
            in=in.replaceAll("[\n\r] +(.+)", "\n<item>$1</item>");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
 
    private List<Map<String, String>> Parse_Cisco_Interfaces_Switchport(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_interfaces_switchport_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("(Name:.+)[\n\r]", "<folder>$1</folder>");
            in=in.replaceAll("^[\n\r]", "");
            in=in.replaceAll("[\n\r]<folder>", "===\n<folder>");
            in=in.replaceAll("[\n\r][\n\r]", "\n");
            in=in.replaceAll("[\n\r](\\w.+)", "\n<item>$1</item>");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
    
    private List<Map<String, String>> Parse_Nexus_Interfaces_Switchport(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_interfaces_switchport_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("(Name:.+)[\n\r]", "<folder>$1</folder>");
            in=in.replaceAll("^[\n\r]", "");
            in=in.replaceAll("[\n\r]<folder>", "===\n<folder>");
//            in=in.replaceAll("[\n\r][\n\r]", "\n");
            in=in.replaceAll("[\n\r]\\s+(\\w.+)", "\n<item>$1</item>");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        return observedValues;
    }
    
    private List<Map> Parse_Cisco_Interfaces_Etherchannel(String in) {
        List<Map> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_interfaces_etherchannel_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("^[\n\r]-+", "");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(Map it : observedValues) {
            if(it.get("data") != null) {
//                String[] mas = it.get("data").split("\n");
                ArrayList etherchannel_ports = new ArrayList();
                for(String line : ((String)(it.get("data"))).split("\n")) {
                    Pattern p = Pattern.compile("^\\s+\\d\\s+\\d+\\s+(\\S+)\\s+\\S+\\s+\\d+.*?$");  
                    Matcher m = p.matcher(line);
                    if(m.find()) etherchannel_ports.add(m.group(1));
                }
                if(etherchannel_ports.size() > 0) 
                    it.put("etherchannel_ports", etherchannel_ports);  
                it.remove("data");
            }
        }
        
        return observedValues;
    }            
    
    private List<Map<String, String>> Parse_Cisco_Interfaces_Ip(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_interfaces_ip_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("^[\n\r](\\S.+)", "<folder>$1</folder>");
            in=in.replaceAll("[\n\r](\\S.+)", "\n===\n<folder>$1</folder>");
//            in=in.replaceAll("[\n\r] +(.+)", "\n<item>$1</item>");
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        for(Map it : observedValues) {
            if(it.get("data") != null) {
                ArrayList ip_address = new ArrayList();
                for(String line : ((String)(it.get("data"))).split("\n")) {
                    Pattern p = Pattern.compile("^\\s+Internet address is\\s+(\\S+)$");  
                    Matcher m = p.matcher(line);
                    if(m.find()) ip_address.add(m.group(1));                    
                }
                for(String line : ((String)(it.get("data"))).split("\n")) {
                    Pattern p1 = Pattern.compile("^\\s+Secondary address\\s+(\\S+)$");  
                    Matcher m1 = p1.matcher(line);
                    if(m1.find()) ip_address.add(m1.group(1));
                }
                
                if(ip_address.size() > 0) 
                    it.put("ip", ip_address);  
                it.remove("data");
            }
        }        
        return observedValues;
    }
    
    private Map<String, String> Parse_Cisco_HSRP(String in) {
        in=in.replaceAll("^[\n\r](\\S.+)", "<folder>$1</folder>");
        in=in.replaceAll("[\n\r](\\S.+)", "\n===\n<folder>$1</folder>");
        
        Map<String, String> out = new HashMap();
        String[] mas = in.split("===");
        Pattern p = Pattern.compile("^[\r\n]*<folder>(.+)\\s+-\\s+Group\\s+\\d+</folder>[\\s\\S]+Virtual\\s+IP\\s+address\\s+is\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)[\\s\\S]+$");
        for(String item : mas) {
            System.out.println(item);
            Matcher m = p.matcher(item);
            if(m.find()){  
                String iface=m.group(1);
                String ip=m.group(2); 
                out.put(iface, ip);
            }
        }
        return out;

    }    
    
    private List<Map<String, String>> Parse_Nexus_Interfaces_Ip(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_interfaces_ip_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            in=in.replaceAll("\n\r*\n", "\n===\n");

            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        return observedValues;
    }
    
    private List<Map<String, String>> Parse_CDP_cisco(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/cisco_cdp_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        List<Map<String, String>> result = new ArrayList<>();
        for(Map map : observedValues) if(map.size() > 0) result.add(map);
        
        return result;
    }
    
    private List<Map<String, String>> Parse_CDP_nexus(String in) {
        List<Map<String, String>> observedValues = new ArrayList<>();
 
        try {
            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_cdp_template.xml");
            Config config = new ConfigLoader().loadConfig(configURL);
            
            Parser parser = Parser.parse(config, new StringReader(in));
            while (true) {
                Map<String, String> record = parser.next();
                if (null == record) {
                    break;
                } else {
                    observedValues.add(record);
                }
            }
        } catch (JAXBException ex) {
            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        List<Map<String, String>> result = new ArrayList<>();
        for(Map map : observedValues) if(map.size() > 0) result.add(map);
        
        return result;
    }
  
//    private List<Map<String, String>> Parse_LLDP_nexus(String in) {
//        List<Map<String, String>> observedValues = new ArrayList<>();
//        
//        in=in.replaceAll("\r*\n\r*\n(.+)", "\n===\n$1");
// 
//        try {
//            URL configURL = Thread.currentThread().getContextClassLoader().getResource("Templates/nexus_lldp_template.xml");
//            Config config = new ConfigLoader().loadConfig(configURL);
//            
//            Parser parser = Parser.parse(config, new StringReader(in));
//            while (true) {
//                Map<String, String> record = parser.next();
//                if (null == record) {
//                    break;
//                } else {
//                    observedValues.add(record);
//                }
//            }
//        } catch (JAXBException ex) {
//            Logger.getLogger(Cisco_information.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        List<Map<String, String>> result = new ArrayList<>();
//        for(Map map : observedValues) if(map.size() > 0) result.add(map);
//        
//        return result;
//    }
    
    private String PrettyOut(String str) {
        String result="";
        int otstup=0;
        String blank="  "; 
        char prev='@';
        char prev_prev='@';
        for(char ch : str.toCharArray()) {
//            System.out.println("prev_prev="+prev_prev+"\tprev="+prev+"\tch="+ch);
            if(ch == '{' || ch == '[') { 
                if(prev == '{' || prev == '[') otstup=otstup+1;
                result=result+"\n";
                for(int i=0; i<otstup; i++) result=result+blank;
                result=result+ch;
            }
            else if(ch == '}' || ch == ']') { 
                if(prev != ',') otstup=otstup-1;
                result=result+"\n";
                for(int i=0; i<otstup; i++) result=result+blank; 
                result=result+ch; 
            }  
            else if(ch == ',') { 
                result=result+ch; 
            }                  
            else { 
                if(prev == '{' || prev == '[') {
                    result=result+"\n";
                    otstup=otstup+1;
                    for(int i=0; i<otstup; i++) result=result+blank;
                } 
                else if(prev == ',') {
                    if(prev_prev == '\"' || prev_prev == '}' || prev_prev == ']') {
                        result=result+"\n";
                        for(int i=0; i<otstup; i++) result=result+blank; 
                    }
                }
                result=result+ch;
            }
            prev_prev=prev;
            prev=ch;
        } 
        return result;
    }
    
    public ArrayList GetAccounts(String accounting) {
        ArrayList accounts = new ArrayList();
        
        accounting=accounting.replace("\\|", "{delimiter1}").replace("\\,", "{delimiter2}");
        String[] buff=accounting.split("\\|", -1);
        for(String item : buff) {
            String[] account=item.split("\\,", -1);
            for(int i=0; i<account.length; i++) {
                account[i]=account[i].replace("{delimiter1}", "\\|").replace("{delimiter2}", "\\,");
            }
            accounts.add(account);
        }
        return accounts;
    }
    
    private ArrayList GetList(String in) {
        ArrayList<String> out = new ArrayList();
        
        in=in.replace("\\;", "{delimiter}");
        String[] buff=in.split("\\;", -1);
        for(String item : buff) {
            item=item.replace("{delimiter}", ";");
            out.add(item);
        }
        return out;
    }   
    
    private String SetList(ArrayList<String> in ){
        String out = "";
        for(String item : in) out=out+";"+item.replace(";", "\\;");
        if(!out.equals("")) out=out.substring(1);
        return out;
    }  
    
    private String Interface_Full_To_Short_name(String iface_name) {
        String result=iface_name;
        Pattern p = Pattern.compile("^([Ee]th)\\S*?(\\d+.*)$");  
        Matcher m = p.matcher(iface_name); 
        if(m.find()) result=m.group(1)+m.group(2);
        else {
            Pattern p1 = Pattern.compile("^(\\w\\w)\\S*?(\\d+.*)$");  
            Matcher m1 = p1.matcher(iface_name); 
            if(m1.find()) result=m1.group(1)+m1.group(2); 
        }
        
        return result;
    }    
    
}

class Check_Parent extends Thread {
    private static int timeout=5;
    private static int retry=5;
    private static long countdown = 60*1000; // 60 sec.
    
    public void run() {
        long start = System.currentTimeMillis();
        while (true) {
            long current = System.currentTimeMillis();
            if(current-start > countdown) {
                if(!CheckPort("localhost", 8080, timeout, retry)) {
                    System.out.println("Parrent not respond: "+Cisco_information.cmd);
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