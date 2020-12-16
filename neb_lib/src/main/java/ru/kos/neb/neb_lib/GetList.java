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
public class GetList {
    private int LIMITS_INTERVAL = 128;
    private int MAXPOOLTHREADS = 128;    
    public static  Snmp snmp = null;
    private int port = 161;
    private int timeout = 3;
    private int retries = 2;

    public static ArrayList<String[]> res = new ArrayList();
    public static Map<String, ArrayList<String[]>> result = new HashMap<>();
    public static Logger logger;
    
    public GetList() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }    
    

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_node_community_ver_oid) {
        return GetProc(list_node_community_ver_oid, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_node_community_ver_oid, int port) {
        return GetProc(list_node_community_ver_oid, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_node_community_ver_oid, int port, int timeout) {
        return GetProc(list_node_community_ver_oid, port, timeout, retries);
    }

    public Map<String, ArrayList<String[]>> Get(ArrayList<String[]> list_node_community_ver_oid, int port, int timeout, int retries) {
        return GetProc(list_node_community_ver_oid, port, timeout, retries);
    }

    private Map<String, ArrayList<String[]>> GetProc(ArrayList<String[]> list_node_community_ver_oid, int port, int timeout, int retries) {
        result.clear();
        res.clear();
//        oid=oid.replaceAll("^\\.", "");
        
        try {        
            ExecutorService service;
            if(list_node_community_ver_oid.size() > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            } 
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();          

            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (String[] node_community_ver_oid : list_node_community_ver_oid) {
                GetListWorker getListWorker = new GetListWorker(node_community_ver_oid, port, timeout, retries);
                service.submit(getListWorker);
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

class GetListWorker implements Runnable {   
//    private String start_oid=null;
    
    private String[] node_community_ver_oid=null;
    private int port;
    private int timeout;
    private int retries;
    
    public GetListWorker(String[] node_community_ver_oid, int port, int timeout, int retries) {
        this.node_community_ver_oid = node_community_ver_oid;
        node_community_ver_oid[3]=node_community_ver_oid[3].replaceAll("^\\.", "");
//        this.start_oid=oid;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
    }
    
    @Override
    public void run() {
//        System.out.println(Thread.currentThread().getName()+" Start.");
//        start_oid=node_community_ver_oid[3];
        Send(node_community_ver_oid);
//        System.out.println(Thread.currentThread().getName()+" Stop!");
    }    
    
    private void Send(String[] node_community_ver_oid) {
        try {
            String node=node_community_ver_oid[0];
            String community=node_community_ver_oid[1];
            String version=node_community_ver_oid[2];
            String oid=node_community_ver_oid[3];
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
            
            ResponseEvent event = GetList.snmp.send(pdu, target);

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
                    synchronized(GetList.res) {GetList.res.add(mas); }
    //                System.out.println(event.getPeerAddress().toString().split("/")[0]+": "+response.get(response.size() - 1).getOid().toString());
                }
            }
            
        } catch (Exception ex) {
            GetList.logger.Println(ex.getMessage(), GetList.logger.DEBUG);
//            Logger.getLogger(GetPool.class.getName()).log(Level.SEVERE, null, ex);
        }
 
    }
}