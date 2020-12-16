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
//import java.util.logging.Level;
//import java.util.logging.Logger;
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
public class GetPool {
    private int LIMITS_INTERVAL = 128;
    private int MAXPOOLTHREADS = 128;
    public static Snmp snmp = null;

    private String community = null;
    private int version = 1;
    private int port = 161;
    private int timeout = 3;
    private int retries = 2;
    public static Logger logger;
    
    public GetPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }    

    public static ArrayList<String[]> res = new ArrayList();
//    private final ArrayList<ArrayList<String[]>> result = new ArrayList();
    private final Map<String, ArrayList<String[]>> result = new HashMap<>();

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_ip, ArrayList<String> oid_list) {
        return GetProc(list_ip, oid_list, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_ip, ArrayList<String> oid_list, int port) {
        return GetProc(list_ip, oid_list, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_ip, ArrayList<String> oid_list, int timeout, int retries) {
        return GetProc(list_ip, oid_list, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_ip, ArrayList<String> oid_list, int port, int timeout, int retries) {
        return GetProc(list_ip, oid_list, port, timeout, retries);
    }

    private Map<String, ArrayList<String[]>> GetProc(ArrayList<String[]> list_ip, ArrayList<String> oid_list, int port, int timeout, int retries) {
        result.clear();
        res.clear();
//        oid=oid.replaceAll("^\\.", "");
        
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
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (String[] node_community_ver : list_ip) {
                GetPoolWorker getPoolWorker = new GetPoolWorker(node_community_ver, oid_list, port, timeout, retries);
                service.submit(getPoolWorker);
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
            if(!Utils.DEBUG) System.setErr(oldError);
            
            // Calculate result
            for (String[] iter : res) {
                ArrayList list = result.get(iter[0]);
                if(list == null) list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iter[1]; mas[1]=iter[2];
                list.add(mas);
                result.put(iter[0], list);
            }
            
        }   
         catch (Exception ex) {
             logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(SnmpScan.class.getName()).log(Level.SEVERE, "result="+result+"   res="+res, ex);
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

class GetPoolWorker implements Runnable {   
//    private String start_oid=null;
    
    private String[] node_community_ver=null;
    private ArrayList<String> oid_list = new ArrayList();
    private int port;
    private int timeout;
    private int retries;
    
    public GetPoolWorker(String[] node_community_ver, ArrayList<String> oid_list, int port, int timeout, int retries) {
        this.node_community_ver = node_community_ver;
        this.oid_list=oid_list;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
    }
    
    @Override
    public void run() {
//        System.out.println(Thread.currentThread().getName()+" Start.");
        for(String oid : oid_list) {
            if(!Send(node_community_ver, oid)) break;
        }
//        System.out.println(Thread.currentThread().getName()+" Stop!");
    }    
    
    private boolean Send(String[] node_community_ver_oid, String oid) {
        String node="";
        try {
            oid=oid.replaceAll("^\\.", "");
            node=node_community_ver_oid[0];
//            System.out.println("Send: "+node);
            String community=node_community_ver_oid[1];
            String version=node_community_ver_oid[2];
            Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout * 1000);
            target.setVersion(SnmpConstants.version1);
            if (version.equals("2")) {
                target.setVersion(SnmpConstants.version2c);
            }

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);
            
            ResponseEvent event = GetPool.snmp.send(pdu, target);

            PDU response = event.getResponse();
            if (response != null) {
                if (event.getPeerAddress().toString() != null) {
                    String[] mas = new String[3];
                    mas[0] = event.getPeerAddress().toString().split("/")[0];
                    String oidstr = response.get(0).getOid().toString();
                    String val = response.get(0).getVariable().toString();
                    if (val.equals("Null")) {
                        mas[1] = oidstr;
                        mas[2] = "";
                    }
                    else if (val.equals("noSuchObject")) {
                        mas[1] = oidstr;
                        mas[2] = "";
                    }
                    else if (val.equals("noSuchInstance")) {
                        mas[1] = oidstr;
                        mas[2] = "";
                    } else {
                        mas[1] = oidstr;
                        mas[2] = val;                        
                    }              
    //                System.out.println(mas[0]+":"+mas[1]);
                    synchronized(GetPool.res) { GetPool.res.add(mas); }
    //                System.out.println(event.getPeerAddress().toString().split("/")[0]+": "+response.get(response.size() - 1).getOid().toString());
                }
                return true;
            } else return false;
        } catch (Exception ex) {
            GetPool.logger.Println(ex.getMessage(), GetPool.logger.DEBUG);
//            Logger.getLogger(GetPool.class.getName()).log(Level.SEVERE, node, ex);
            return false;
        }
 
    }
}