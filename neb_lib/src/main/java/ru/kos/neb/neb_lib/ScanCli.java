/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;


import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

//import ru.kos.neb.neb_lib.Terminal;
//import ru.kos.neb.expect.Expect;

/**
 *
 * @author kos
 */
public class ScanCli {

//    private int LIMITS_INTERVAL = 1024;
//    private int MAXPOOLTHREADS = 1024;
    private int LIMITS_INTERVAL = 2;
    private int MAXPOOLTHREADS = 2;    
    private int timeout_ScanCliWorker = 180; // seconds
    
//    public static Snmp snmp = null;
//    private TransportMapping transport=null;
    public static ArrayList<String[]> result = new ArrayList();

    Utils utils = new Utils();

    public ArrayList<String[]> Scan(String network, Map<String, String> exclude_list_ip, ArrayList<ArrayList<String>> user_password_enablepassword) {
        int timeout = 10; // in sec
        int retries = 1;
        return Scan(network, exclude_list_ip, user_password_enablepassword, timeout, retries);
    }

    public ArrayList<String[]> Scan(String network, Map<String, String> exclude_list_ip, ArrayList<ArrayList<String>> user_password_enablepassword, int timeout, int retries) {
        result.clear();

        try {
            long[] interval = utils.IntervalNetworkAddress(network);
            ExecutorService service;
            if(interval[1] - interval[0] > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }

            PrintStream oldError = System.err;
            if(!Utils.DEBUG_SCRIPT) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip=utils.NetworkToIPAddress(addr);
                    if(exclude_list_ip == null || exclude_list_ip.get(ip) == null) {
                        ScanCliWorker scanCliWorker = new ScanCliWorker(ip, user_password_enablepassword, timeout, retries);
                        service.submit(scanCliWorker);  
//                        System.out.println("ip="+ip);
                    }
                }
                
            } else {
                long addr = interval[0];
                String ip=utils.NetworkToIPAddress(addr);
                if(exclude_list_ip == null || exclude_list_ip.get(ip) == null) {
                    ScanCliWorker scanCliWorker = new ScanCliWorker(ip, user_password_enablepassword, timeout, retries);
                    service.submit(scanCliWorker);
                    System.out.println("ip="+ip);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
            if(!Utils.DEBUG_SCRIPT) System.setErr(oldError);
        }
        catch (Exception ex) {
//            System.out.println(SnmpScan.class.getName());
        }  
        
        return result;
    }

    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<ArrayList<String>> user_password_enablepassword) {
        int timeout = 10; // in sec
        int retries = 1;
        return Scan(list_ip, exclude_list_ip, user_password_enablepassword, timeout, retries);
    }       
    
    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<ArrayList<String>> user_password_enablepassword, int timeout, int retries) {
        result.clear();

        try {     
            ExecutorService service;
            if(list_ip.size() > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }

            PrintStream oldError = System.err;
            System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (String ip : list_ip) {
                if(exclude_list_ip == null || exclude_list_ip.get(ip) == null) {
                    ScanCliWorker scanCliWorker = new ScanCliWorker(ip, user_password_enablepassword, timeout, retries);
                    service.submit(scanCliWorker);
                    System.out.println("ip="+ip);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
            System.setErr(oldError);
        }   
        catch (Exception ex) {
//            Logger.getLogger(SnmpScan.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
}

class ScanCliWorker implements Runnable {   
    
    private String node=null;
    private String oid=null;
    ArrayList<ArrayList<String>> user_password_enablepassword = new ArrayList();
    private int port_ssh=22;
    private int port_telnet=23;
    private int timeout;
    private int retries;
    List<Pattern> stop_symbols = new ArrayList();
    
    public ScanCliWorker(String node, ArrayList<ArrayList<String>> user_password_enablepassword, int timeout, int retries) {
        this.node = node;
        this.oid=oid;
        this.user_password_enablepassword=user_password_enablepassword;
        this.timeout=timeout;
        this.retries=retries;
        stop_symbols.add(Pattern.compile("[\n\r]\\S*\\w+\\S*#"));
        stop_symbols.add(Pattern.compile("[\n\r]\\S*\\w+\\S*>"));        
    }
    
    @Override
    public void run() {
//        System.out.println(Thread.currentThread().getName()+" Start.");
        String[] mas = TestCli(node, user_password_enablepassword);
        if(mas != null) synchronized(ScanCli.result) {
            ScanCli.result.add(mas); 
            if(Utils.DEBUG_SCRIPT) System.out.println("ScanCli node - "+" "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]+","+mas[4]);
        }
//        System.out.println(Thread.currentThread().getName()+" Stop!");

//        System.out.println(Thread.currentThread().getName()+" Stop!");
    }    
    
    private String[] TestCli(String node, ArrayList<ArrayList<String>> accounts) {
        String[] result=null;
        try {
            result=GetSSH(node, accounts);
            if(result == null) 
                result=GetTelnet(node, accounts);
        } catch (Exception ex) {
//            if(Utils.DEBUG_SCRIPT) System.out.println(node+":\t"+ex.toString());
        }
        return result;
    }
    
    private String[] GetSSH(String node, ArrayList<ArrayList<String>> accounts) {
        String[] result=null;

        Terminal terminal = new Terminal();
        if(TCPportIsOpen(node, port_ssh, timeout)) {
            for(ArrayList<String> account : accounts) {
                String user="";
                String passwd="";
                String enable_passwd="";
                if(account.size() == 3) {
                    user = account.get(0);
                    passwd = account.get(1);
                    enable_passwd = account.get(2);
                } else if(account.size() == 2) {
                    user = account.get(0);
                    passwd = account.get(1);                    
                }

                ArrayList conn = new ArrayList();
                try {
                    if(account.size() == 2 || account.size() == 3) {
                        conn = terminal.ConnectSSH(node, user, passwd, enable_passwd, timeout, stop_symbols);
                        if(conn.size() == 3) {
                            result = new String[5];
                            result[0]=node;
                            result[1]=user;
                            result[2]=passwd;
                            result[3]=enable_passwd;
                            result[4]="ssh";
                            if(conn.size()>0) terminal.DisconnectSSH(conn);
                            break;
                        }
                    }
                } catch (Exception e) {
                    if(conn.size()>0) terminal.DisconnectSSH(conn);
//                    System.out.println("GetSSH "+node+":\t"+e.toString());
                }
            }
        }
        
        return result;
    }
    
    private String[] GetTelnet(String node, ArrayList<ArrayList<String>> accounts) {
        String[] result=null;

        Terminal terminal = new Terminal();
        if(TCPportIsOpen(node, port_telnet, timeout)) {
            for(ArrayList<String> account : accounts) {
                String user="";
                String passwd="";
                String enable_passwd="";
                if(account.size() == 3) {
                    user = account.get(0);
                    passwd = account.get(1);
                    enable_passwd = account.get(2);
                } else if(account.size() == 2) {
                    user = account.get(0);
                    passwd = account.get(1);                    
                }
                
                ArrayList conn = new ArrayList();
                try {   
                    if(account.size() == 2 || account.size() == 3) {
                        conn = terminal.ConnectTelnet(node, user, passwd, enable_passwd, timeout, stop_symbols);
                        if(conn.size() == 3) {
                            result = new String[5];
                            result[0]=node;
                            result[1]=user;
                            result[2]=passwd;
                            result[3]=enable_passwd;
                            result[4]="telnet";
                            if(conn.size()>0) terminal.DisconnectTelnet(conn);
                            break;
                        }
                    }
                } catch (Exception e) {
                    if(conn.size()>0) terminal.DisconnectTelnet(conn);
//                    if(Utils.DEBUG_SCRIPT) System.out.println("GetTelnet "+node+":\t"+e.toString());
                }               
            }
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
