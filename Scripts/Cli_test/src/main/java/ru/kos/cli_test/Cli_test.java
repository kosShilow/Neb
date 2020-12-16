package ru.kos.cli_test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import json.JSON;

import ru.kos.terminal.Terminal;

public class Cli_test {
    private static int timeout=20;  // expect timeout sec.
    private static int timeout_ssh_telnet=timeout/2; // telnet ssh timeout sec.    
    private static List<Pattern> stop_symbols = new ArrayList();
    private static Cli_test cisco_information = new Cli_test();
    private static ArrayList conn = new ArrayList();
    private static Terminal terminal = null;
//    private static Terminal terminal;    
    public static String cmd = "";
    private static int retries=1;   
    private static boolean DEBUG = false;
    private static boolean CHECK_PARENT = true;

    public static void main(String[] args) {
        int port_ssh = 22;
        int port_telnet = 23;
        BufferedReader br_STDIN = null;
        FileWriter file_writer = null;

        if(CHECK_PARENT) {
            Check_Parent check_Parent = new Check_Parent();
            check_Parent.start();
        }  
        try {
//            final File temp = File.createTempFile("out-temp", ".tmp");
            br_STDIN = new BufferedReader(new InputStreamReader(System.in));
            while (true) {               
                try {
                    stop_symbols = new ArrayList();
                    stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+#"));
                    stop_symbols.add(Pattern.compile("[\r\n]*[^ \f\n\r\t#]+>"));                 

                    cmd = br_STDIN.readLine();
                    if(cmd != null && !cmd.equals("")) {
                        System.out.println(cmd);
//                        file_writer = new FileWriter(temp, true);
//                        terminal = new Terminal(file_writer);                         
                        //////////////////
//                        file_writer.write(cmd+"\n");
                        ///////////////////                        
                        if ("QUIT".equals(cmd)) {
                            System.out.println("Exit!");
                            System.exit(0);
                        }
                        String[] mas = cmd.split("\\s+");
                        if(mas.length == 3) {
                            String node = mas[0];
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
                                            timeout=timeout_ssh_telnet*6;
                                        }
                                        if(mas2.length >= 5) retries=Integer.parseInt(mas2[4]);

                                        Map<String, String> out = new HashMap();

                                        if (protocol.toLowerCase().equals("ssh")) {                            
                                            out = cisco_information.GetSSH(node, user, passwd, enable_passwd, port_ssh);                          
                                        } else if (protocol.toLowerCase().equals("telnet")) {
                                            out = cisco_information.GetTelnet(node, user, passwd, enable_passwd, port_telnet);
                                        }
                                        
                                        JSON jSON = new JSON();
                        //                String output = cisco_information.PrettyOut(jSON.mapToJSON(out));
                                        if (out.size() > 0) {
                                            output = jSON.mapToJSON(out);
                                        }
                       
                                        //////////////////////////////
//                                        file_writer.write(output+"\n");
                                        /////////////////////////////
                                                                      
                                        if(!output.equals("")) break;
                                    }

                                }
                            }
                            if(!output.equals("")) System.out.println("<result>"+output+"</result>\n");
                            else System.out.println("<result></result>\n");

                        } else System.out.println("<result></result>\n");
//                        if(file_writer != null) file_writer.close();
                    }
                }  catch (Exception e) {
                    e.printStackTrace();
                    if(DEBUG) System.out.println("main: "+e.getMessage());
//                    System.out.println(e.getMessage()+"\n");
                }
                try { Thread.sleep(1000); } catch (InterruptedException e) {  }
            }
        } catch (Exception ex1) { 
            System.out.println("<result></result>\n");
        } finally {           
            if (br_STDIN != null) {
                try {
                    br_STDIN.close();
                } catch (IOException e) {}
            }
        }
    }
    
    public Map<String, String> GetSSH(String node, String user, String passwd, String enable_passwd, int port_ssh) {
        Map<String, String> result = new HashMap<>();
        
        if(cisco_information.TCPportIsOpen(node, port_ssh, timeout_ssh_telnet)) {           
            terminal = new Terminal(timeout_ssh_telnet, timeout);
            conn = terminal.ConnectSSH(node, user, passwd, enable_passwd, stop_symbols);           
            if(conn.size() == 3) {
                stop_symbols=(List<Pattern>)conn.get(2);
                result.put("node", node);
                result.put("protocol", "ssh");
                result.put("user", user);
                result.put("passwd", passwd);
                result.put("enable_passwd", enable_passwd);
            }
            terminal.DisconnectSSH(conn);
        }

        return result;
    }
    
    public Map<String, String> GetTelnet(String node, String user, String passwd, String enable_passwd, int port_telnet) {
        Map result = new HashMap<>();

        if(cisco_information.TCPportIsOpen(node, port_telnet, timeout_ssh_telnet)) {
            terminal = new Terminal(timeout_ssh_telnet, timeout);
            conn = terminal.ConnectTelnet(node, user, passwd, enable_passwd, stop_symbols);
            if(conn.size() == 3) {
                stop_symbols=(List<Pattern>)conn.get(2);
                result.put("node", node);
                result.put("protocol", "telnet");
                result.put("user", user);
                result.put("passwd", passwd);
                result.put("enable_passwd", enable_passwd);
            }
            terminal.DisconnectTelnet(conn);
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
                    System.out.println("Parrent not respond: "+Cli_test.cmd);
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
