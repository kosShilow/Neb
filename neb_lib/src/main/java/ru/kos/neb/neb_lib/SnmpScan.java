/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 *
 * @author kos
 */
public class SnmpScan {

    private int LIMITS_INTERVAL = 128;
    private int MAXPOOLTHREADS = 128;
    
    public static Snmp snmp = null;
//    private TransportMapping transport=null;
    public static ArrayList<String[]> result = new ArrayList();

    Utils utils = new Utils();

    public static Logger logger;
    
    public SnmpScan() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    } 
    
    public ArrayList<String[]> Scan(String network, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int port) {
        int timeout = 3; // in sec
        int retries = 1;
        return Scan(network, exclude_list_ip, community, oid, port, timeout, retries);
    }

    public ArrayList<String[]> Scan(String network, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        return Scan(network, exclude_list_ip, community, oid, port, timeout, retries);
    }

    public ArrayList<String[]> Scan(String network, Map<String, String> exclude_list_ip, ArrayList<String> community_list, String oid, int port, int timeout, int retries) {
        result.clear();
        oid=oid.replaceAll("^\\.", "");

        try {
            long[] interval = utils.IntervalNetworkAddress(network);
            ExecutorService service;
            if(interval[1] - interval[0] > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();    

            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip=utils.NetworkToIPAddress(addr);
                    if(exclude_list_ip.get(ip) == null) {
                        SnmpScanWorker snmpScanWorker = new SnmpScanWorker(ip, oid, community_list, port, timeout, retries);
                        service.submit(snmpScanWorker);
                    }
                }
            } else {
                long addr = interval[0];
                String ip=utils.NetworkToIPAddress(addr);
                if(exclude_list_ip.get(ip) == null) {
                    SnmpScanWorker snmpScanWorker = new SnmpScanWorker(ip, oid, community_list, port, timeout, retries);
                    service.submit(snmpScanWorker);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) { e.printStackTrace(); } }
            if(!Utils.DEBUG) System.setErr(oldError);
        }
        catch (Exception ex) {
            logger.Println(ex.getMessage(), logger.DEBUG);
//            System.out.println(SnmpScan.class.getName());
        }  

        finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception ex) { }
                snmp = null;
//                try { transport.close(); } catch (Exception ex) { }
//                transport = null;
            }
        } // finalize
        
        return result;
    }

    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int port) {
        int timeout = 3; // in sec
        int retries = 2;
        return Scan(list_ip, exclude_list_ip, community, oid, port, timeout, retries);
    }

    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 1;
        return Scan(list_ip, exclude_list_ip, community, oid, port, timeout, retries);
    }    
    
    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community, String oid, int timeout, int retries) {
        int port = 161;
        return Scan(list_ip, exclude_list_ip, community, oid, port, timeout, retries);
    }      
    
    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, ArrayList<String> community_list, String oid, int port, int timeout, int retries) {
        result.clear();
        oid=oid.replaceAll("^\\.", "");

        try {     
            ExecutorService service;
            if(list_ip.size() > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();          

            PrintStream oldError = System.err;
            System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (String ip : list_ip) {
                if(exclude_list_ip.get(ip) == null) {
                    SnmpScanWorker snmpScanWorker = new SnmpScanWorker(ip, oid, community_list, port, timeout, retries);
                    service.submit(snmpScanWorker);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) { e.printStackTrace(); } }
            System.setErr(oldError);
        }   
         catch (Exception ex) {
             logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(SnmpScan.class.getName()).log(Level.SEVERE, null, ex);
        }  

        finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception ex) { }
                snmp = null;
            }
        } // finalize
        return result;
    }
    
    public ArrayList<String[]> Scan(ArrayList<String> list_ip, Map<String, String> exclude_list_ip, 
            ArrayList<String> community_list, String oid, int port, int timeout, int retries, 
            Map<String, String[]> snmp_accounts_priority) {
        result.clear();
        oid=oid.replaceAll("^\\.", "");

        try {     
            ExecutorService service;
            if(list_ip.size() > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();          

            PrintStream oldError = System.err;
            System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            if(snmp_accounts_priority != null && snmp_accounts_priority.size() > 0) {
                for (String ip : list_ip) {
                    if(exclude_list_ip.get(ip) == null) {
                        SnmpScanWorker snmpScanWorker;
                        snmpScanWorker = new SnmpScanWorker(ip, oid, community_list, port, timeout, retries, snmp_accounts_priority.get(ip));
                        service.submit(snmpScanWorker);
                    }
                }
            } else {
                for (String ip : list_ip) {
                    if(exclude_list_ip.get(ip) == null) {
                        SnmpScanWorker snmpScanWorker;
                        snmpScanWorker = new SnmpScanWorker(ip, oid, community_list, port, timeout, retries);
                        service.submit(snmpScanWorker);
                    }
                }                
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) { e.printStackTrace(); } }
            System.setErr(oldError);
        }   
         catch (Exception ex) {
             logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(SnmpScan.class.getName()).log(Level.SEVERE, null, ex);
        }  

        finally {
            if (snmp != null) {
                try { snmp.close(); } catch (Exception ex) { }
                snmp = null;
            }
        } // finalize
        return result;
    }    
}

class SnmpScanWorker implements Runnable {   
    
    private String node=null;
    private String oid=null;
    ArrayList<String> community_list = new ArrayList();
    private int port;
    private int timeout;
    private int retries;
    private String[] snmp_accounts_priority;
    
    public SnmpScanWorker(String node, String oid, ArrayList<String> community_list, int port, int timeout, int retries) {
        this.node = node;
        this.oid=oid;
        this.community_list=community_list;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
    }
    
    public SnmpScanWorker(String node, String oid, ArrayList<String> community_list, int port, int timeout, int retries, String[] snmp_accounts_priority) {
        this.node = node;
        this.oid=oid;
        this.community_list=community_list;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
        this.snmp_accounts_priority=snmp_accounts_priority;
    }    
    
    
    @Override
    public void run() {
//        System.out.println(Thread.currentThread().getName()+" Start.");
        try {
            if(snmp_accounts_priority != null && snmp_accounts_priority.length == 2) {
                String community = snmp_accounts_priority[0];
                String version = snmp_accounts_priority[1];
                if(Send(node, oid, community, version)) {
                    String[] mas = new String[3];
                    mas[0]=node; mas[1]=community; mas[2]="2";
                    synchronized(SnmpScan.result) { SnmpScan.result.add(mas); }
                    return;
                }                
            }
            for(String community : community_list) {
                if(Send(node, oid, community, "2")) {
                    String[] mas = new String[3];
                    mas[0]=node; mas[1]=community; mas[2]="2";
                    synchronized(SnmpScan.result) { SnmpScan.result.add(mas); }
                    break;
                } else if(Send(node, oid, community, "1")) {
                    String[] mas = new String[3];
                    mas[0]=node; mas[1]=community; mas[2]="1";
                    synchronized(SnmpScan.result) { SnmpScan.result.add(mas); }
                    break;

                }
            }
        } catch (Exception ex) {
            SnmpScan.logger.Println(ex.getMessage(), SnmpScan.logger.DEBUG);
//            System.out.println(SnmpScan.class.getName());
        }
        finally {
//            if(Utils.DEBUG) System.out.println("Stop SnmpScan - "+this.node+"   ok\nQueue: "+SnmpScan.queue_size);
        }        
//        System.out.println(Thread.currentThread().getName()+" Stop!");
    }    
    
    private boolean Send(String node, String oid, String community, String version) {
        boolean result=false;
        try {
            Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout * 1000);
            if(version.equals("2")) target.setVersion(SnmpConstants.version2c);
            else target.setVersion(SnmpConstants.version1);

            
            PDU pdu = new PDU();
//            OID startOid = new OID(oid);
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent event = SnmpScan.snmp.send(pdu, target);
            PDU response = event.getResponse();
            if (event != null && response != null) {
                result=true;
            }
  
        } catch (Exception ex) {
            SnmpScan.logger.Println(ex.getMessage(), SnmpScan.logger.DEBUG);
//            System.out.println(SnmpScan.class.getName());
        }
        return result;
    }
}