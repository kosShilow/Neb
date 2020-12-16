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
import java.util.Random;
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
public class WalkPool {
    public static ArrayList<String[]> res = new ArrayList();
    public static Snmp snmp;
    public static Logger logger;
    
    private final int LIMITS_INTERVAL = 128;
    private final int MAXPOOLTHREADS = 128; 
    private final int LIMITS_INTERVAL_MACTABLE = 128;
    private final int MAXPOOLTHREADS_MACTABLE = 128;      
    private int port = 161;
    private int timeout = 3;
    private int retries = 1;
    public int bulk_size = 10;

    private final Map<String, ArrayList> result = new HashMap<String, ArrayList>();
    
    public WalkPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }    

    public Map<String, ArrayList> Get(ArrayList<String[]> list_ip, String oid)
    {
        GetProc(list_ip, oid, port, timeout, retries, bulk_size, false);
        return result;
    }       
    public Map<String, ArrayList> Get(ArrayList<String[]> list_ip, String oid, int port)
    {
        GetProc(list_ip, oid, port, timeout, retries, bulk_size, false);
        return result;
    }        
    public Map<String, ArrayList> Get(ArrayList<String[]> list_ip, String oid, int port, int timeout)
    {
        GetProc(list_ip, oid, port, timeout, retries, bulk_size, false);
        return result;
    }    
    public Map<String, ArrayList> Get(ArrayList<String[]> list_ip, String oid, int port, int timeout, int retries)
    {
        GetProc(list_ip, oid, port, timeout, retries, bulk_size, false);
        return result;
    }
    public Map<String, ArrayList> Get(ArrayList<String[]> list_ip, String oid, int port, int timeout, int retries, int bulk_size)
    {
        GetProc(list_ip, oid, port, timeout, retries, bulk_size, false);
        return result;
    }    
    public Map<String, Boolean> Test(ArrayList<String[]> node_community_version_list, String oid, int port, int timeout, int retries, int bulk_size)
    {
        Map<String, Boolean> out = new HashMap();
        GetProc(node_community_version_list, oid, port, timeout, retries, bulk_size, true);
        if(result != null) {
            for (Map.Entry<String, ArrayList> entry : result.entrySet()) {
                String node = entry.getKey();
                ArrayList val = entry.getValue();
                if(val != null && val.size() > 1) out.put(node, Boolean.TRUE);
                else out.put(node, Boolean.FALSE);
            }
        }
        return out;
    }    
    
    private void GetProc(ArrayList<String[]> list_ip, String oid, int port, int timeout, int retries, int bulk_size, boolean test) {
        try {
            result.clear();
            res.clear();
            
            this.port=port;
            this.timeout=timeout;
            this.retries=retries;
            oid=oid.replaceAll("^\\.", "");

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
            for (String[] mas : list_ip) {
                if(!mas[0].equals("") && !mas[1].equals("") && !mas[2].equals("")) {
                    Worker worker = new Worker(mas, oid, port, timeout, retries, bulk_size, test);
                    service.submit(worker);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
//            System.out.println("Finished all threads");
            if(!Utils.DEBUG) System.setErr(oldError);

            // Calculate result
            for(String[] iter : res) {
                ArrayList list = result.get(iter[0]);
                if(list == null) list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iter[1]; mas[1]=iter[2];
                list.add(mas);
                result.put(iter[0], list);
            }
            if(snmp != null) snmp.close();
            if(transport != null) transport.close();
        }

        catch (Exception ex) {
            if(!Utils.DEBUG) ex.printStackTrace();
            logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "WalkPool: ", ex);
        }
        finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
//                    Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "finalize: ", ex);
                }
                snmp = null;
            }
        } // finalize
    }
////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList> GetNodeMultiCommunityVersion(ArrayList<ArrayList> node_multicommunity_version_list, String oid, int port, int timeout, int retries) {
        try {
            result.clear();
            res.clear();
            
            this.port=port;
            this.timeout=timeout;
            this.retries=retries;
            oid=oid.replaceAll("^\\.", "");

            ExecutorService service;
            if(node_multicommunity_version_list.size() > LIMITS_INTERVAL_MACTABLE) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS_MACTABLE);
            } else {
                service = Executors.newCachedThreadPool();
            }
            
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            
            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (ArrayList node_multicommunity_version : node_multicommunity_version_list) {
                String node = (String)node_multicommunity_version.get(0);
                ArrayList<String> community_list = (ArrayList<String>)node_multicommunity_version.get(1);
                String version = (String)node_multicommunity_version.get(2);
                if(!node.equals("") && community_list != null && community_list.size() > 0 && !version.equals("")) {
                    WorkerMulticommunity workerMulticommunity = new WorkerMulticommunity(node_multicommunity_version, oid, port, timeout, retries);
                    service.submit(workerMulticommunity);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
//            System.out.println("Finished all threads");
            if(!Utils.DEBUG) System.setErr(oldError);

            // Calculate result
            for(String[] iter : res) {
                ArrayList list = result.get(iter[0]);
                if(list == null) list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iter[1]; mas[1]=iter[2];
                list.add(mas);
                result.put(iter[0], list);
            }
            if(snmp != null) snmp.close();
            if(transport != null) transport.close();
        }

        catch (Exception ex) {
            if(!Utils.DEBUG) ex.printStackTrace();
            logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "WalkPool: ", ex);
        }
        finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
//                    Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "finalize: ", ex);
                }
                snmp = null;
            }
        } // finalize
        return result;
    }
    
    public Map<String, ArrayList> GetNodeMultiCommunityVersionOid(ArrayList<ArrayList> node_multicommunity_version_oid_list, int port, int timeout, int retries) {
        try {
            result.clear();
            res.clear();
            
            this.port=port;
            this.timeout=timeout;
            this.retries=retries;

            ExecutorService service;
            if(node_multicommunity_version_oid_list.size() > LIMITS_INTERVAL_MACTABLE) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS_MACTABLE);
            } else {
                service = Executors.newCachedThreadPool();
            }
            
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            
            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (ArrayList node_multicommunity_version_oid : node_multicommunity_version_oid_list) {
                String node = (String)node_multicommunity_version_oid.get(0);
                ArrayList<String> community_list = (ArrayList<String>)node_multicommunity_version_oid.get(1);
                String version = (String)node_multicommunity_version_oid.get(2);
                String oid = (String)node_multicommunity_version_oid.get(3);
                oid=oid.replaceAll("^\\.", "");
                
                ArrayList node_multicommunity_version = new ArrayList();
                node_multicommunity_version.add(node);
                node_multicommunity_version.add(community_list);
                node_multicommunity_version.add(version);
                if(!node.equals("") && community_list != null && community_list.size() > 0 && !version.equals("") && !oid.equals("")) {
                    WorkerMulticommunity workerMulticommunity = new WorkerMulticommunity(node_multicommunity_version, oid, port, timeout, retries);
                    service.submit(workerMulticommunity);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
//            System.out.println("Finished all threads");
            if(!Utils.DEBUG) System.setErr(oldError);

            // Calculate result
            for(String[] iter : res) {
                ArrayList list = result.get(iter[0]);
                if(list == null) list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iter[1]; mas[1]=iter[2];
                list.add(mas);
                result.put(iter[0], list);
            }
            if(snmp != null) snmp.close();
            if(transport != null) transport.close();
        }

        catch (Exception ex) {
            logger.Println(ex.getMessage(), logger.DEBUG);
            if(Utils.DEBUG) ex.printStackTrace();
        }
        finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
//                    Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "finalize: ", ex);
                }
                snmp = null;
            }
        } // finalize
        return result;
    }
    
    public Map<String, ArrayList> GetNodeMultiCommunityVersionOidNotBulk(ArrayList<ArrayList> node_multicommunity_version_oid_list, int port, int timeout, int retries) {
        try {
            result.clear();
            res.clear();
            
            this.port=port;
            this.timeout=timeout;
            this.retries=retries;

            ExecutorService service;
            if(node_multicommunity_version_oid_list.size() > LIMITS_INTERVAL_MACTABLE) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS_MACTABLE);
            } else {
                service = Executors.newCachedThreadPool();
            }
            
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            
            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (ArrayList node_multicommunity_version_oid : node_multicommunity_version_oid_list) {
                String node = (String)node_multicommunity_version_oid.get(0);
                ArrayList<String> community_list = (ArrayList<String>)node_multicommunity_version_oid.get(1);
                String version = (String)node_multicommunity_version_oid.get(2);
                String oid = (String)node_multicommunity_version_oid.get(3);
                oid=oid.replaceAll("^\\.", "");
                
                ArrayList node_multicommunity_version = new ArrayList();
                node_multicommunity_version.add(node);
                node_multicommunity_version.add(community_list);
                node_multicommunity_version.add(version);
                if(!node.equals("") && community_list != null && community_list.size() > 0 && !version.equals("") && !oid.equals("")) {
                    WorkerMulticommunityNotBulk workerMulticommunity = new WorkerMulticommunityNotBulk(node_multicommunity_version, oid, port, timeout, retries);
                    service.submit(workerMulticommunity);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
//            System.out.println("Finished all threads");
            if(!Utils.DEBUG) System.setErr(oldError);

            // Calculate result
            for(String[] iter : res) {
                ArrayList list = result.get(iter[0]);
                if(list == null) list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iter[1]; mas[1]=iter[2];
                list.add(mas);
                result.put(iter[0], list);
            }
            if(snmp != null) snmp.close();
            if(transport != null) transport.close();
        }

        catch (Exception ex) {
            logger.Println(ex.getMessage(), logger.DEBUG);
            if(Utils.DEBUG) ex.printStackTrace();
        }
        finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
//                    Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "finalize: ", ex);
                }
                snmp = null;
            }
        } // finalize
        return result;
    }
    
////////////////////////////////////////////////////////////////////////////////    
    public Map<String, ArrayList> Get(ArrayList<String[]> list_node_community_version_oid)
    {
        GetProc(list_node_community_version_oid, port, timeout, retries, bulk_size, false);
        return result;
    }
    
    public Map<String, ArrayList> Get(ArrayList<String[]> list_node_community_version_oid, int port)
    {
        GetProc(list_node_community_version_oid, port, timeout, retries, bulk_size, false);
        return result;
    }
    
    public Map<String, ArrayList> Get(ArrayList<String[]> list_node_community_version_oid, int timeout, int retries)
    {
        GetProc(list_node_community_version_oid, port, timeout, retries, bulk_size, false);
        return result;
    }
    
    public Map<String, ArrayList> Get(ArrayList<String[]> list_node_community_version_oid, int port, int timeout, int retries)
    {
        GetProc(list_node_community_version_oid, port, timeout, retries, bulk_size, false);
        return result;
    }
    
    public Map<String, ArrayList> Get(ArrayList<String[]> list_node_community_version_oid, int port, int timeout, int retries, int bulk_size)
    {
        GetProc(list_node_community_version_oid, port, timeout, retries, bulk_size, false);
        return result;
    }    
    
    private void GetProc(ArrayList<String[]> list_node_community_version_oid, int port, int timeout, int retries, int bulk_size, boolean test) {
        try {
            result.clear();
            res.clear();
            
            this.port=port;
            this.timeout=timeout;
            this.retries=retries;            

            ExecutorService service;
            if(list_node_community_version_oid.size() > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }
            
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            
            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (String[] mas : list_node_community_version_oid) {
//                System.out.println("--- "+mas[0]+","+mas[1]+","+mas[2]+","+mas[3]);
                if(mas.length == 4 && !mas[0].equals("") && !mas[1].equals("") && !mas[2].equals("") && !mas[3].equals("")) {
                    String oid=mas[3].replaceAll("^\\.", "");
                    String[] mas1 = new String[3];
                    mas1[0]=mas[0]; mas1[1]=mas[1]; mas1[2]=mas[2];
                    Worker worker = new Worker(mas1, oid, port, timeout, retries, bulk_size, test);
                    service.submit(worker);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) {  } }
//            System.out.println("Finished all threads");
            if(!Utils.DEBUG) System.setErr(oldError);

            // Calculate result
            for(String[] iter : res) {
                ArrayList list = result.get(iter[0]);
                if(list == null) list = new ArrayList();
                String[] mas = new String[2];
                mas[0]=iter[1]; mas[1]=iter[2];
                list.add(mas);
                result.put(iter[0], list);
//                System.out.println("Walk pool - "+iter[0]+","+iter[1]+","+iter[2]);
            }
            if(snmp != null) snmp.close();
            if(transport != null) transport.close();
        }

        catch (Exception ex) {
            if(!Utils.DEBUG) ex.printStackTrace();
            logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "WalkPool: ", ex);
        }
        finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (Exception ex) {
//                    Logger.getLogger(WalkPool.class.getName()).log(Level.SEVERE, "finalize: ", ex);
                }
                snmp = null;
            }
        } // finalize
    }    
}
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
class Worker implements Runnable {   
    private String start_oid=null;
    
    private String[] node = new String[3];
    private String oid=null;
    private int port;
    private int timeout;
    private int retries;
    private int bulk_size;
    private boolean test=false;
    private Map<String, String> oids_map = new HashMap();
    
    
    public Worker(String[] node, String oid, int port, int timeout, int retries, int bulk_size, boolean test) {
        this.node = node;
        this.oid=oid;
        this.start_oid=oid;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
        this.bulk_size=bulk_size;
        this.test=test;
    }
    
    @Override
    public void run() {
//        if(Utils.DEBUG) System.out.println(node[0]+" "+oid+" Start.");
        WalkPool.logger.Println(node[0]+" "+oid+" Start.", WalkPool.logger.DEBUG);
        Send(node, oid, test); 
//        if(Utils.DEBUG) System.out.println(node[0]+" "+oid+" Stop!");
        WalkPool.logger.Println(node[0]+" "+oid+" Stop!", WalkPool.logger.DEBUG);
    }    
    
    private void Send(String[] node, String oid, boolean test) {
//        System.out.println(node[0]+","+node[1]+","+node[2]+","+oid);
        Send(node, oid, bulk_size, test);
    }
    
    private void Send(String[] node, String oid, int bulk_size, boolean test) {
        try {
            Address targetAddress = GenericAddress.parse("udp:" + node[0] + "/" + port);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(node[1]));
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout * 1000);
            target.setVersion(SnmpConstants.version1);
            if (node[2].equals("2")) {
                target.setVersion(SnmpConstants.version2c);
            }
            
            PDU pdu = new PDU();
//            OID startOid = new OID(oid);
            pdu.add(new VariableBinding(new OID(oid)));
            if (node[2].equals("2")) {
                pdu.setType(PDU.GETBULK);
                pdu.setMaxRepetitions(bulk_size);
            }
            else
                pdu.setType(PDU.GETNEXT);

            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
            PDU response = event.getResponse();
            if (event != null && response != null) {
                boolean next_send=false;
                String cur_OID="";
                String ip_port = event.getPeerAddress().toString();
                if (ip_port != null) { 
                    for( VariableBinding var: response.toArray()) {
                        cur_OID = var.getOid().toString();
                        if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                            String[] mas = new String[3];
                            mas[0]=ip_port.split("/")[0];
                            mas[1]=var.getOid().toString();
                            mas[2]=var.getVariable().toString();
//                            if(Utils.DEBUG) System.out.println(mas[0]+": "+mas[1]+", "+mas[2]);
                            WalkPool.logger.Println(mas[0]+": "+mas[1]+", "+mas[2], WalkPool.logger.DEBUG);
                            synchronized(WalkPool.res) { 
                                if(oids_map.get(mas[1]) == null) {
                                    oids_map.put(mas[1], mas[1]);
                                    WalkPool.res.add(mas);
                                    next_send=true;
                                } else {
                                    next_send=false;
                                    break;                                    
                                }
                            }
                        }
                        else {
                            next_send=false;
                            break;
                        }
                    }
                    if(next_send && !test) {
//                        System.out.println("Next send: "+node[0]+","+cur_OID);
                        Send(node, cur_OID, test);
                    }
                    
                }
            } 
//            else {
//                if(bulk_size == 1) {
////                    System.out.println("Timeout exceeded: "+node[0]+","+node[1]+","+node[2]+","+oid);
//                } else {
//                    bulk_size=1;
////                    System.out.println("Resend: "+node[0]+","+node[1]+","+node[2]+","+oid+"bulk_size=1");
//                    Send(node, oid, 1, test);
//                }
//            }
                
        } catch (Exception ex) {
            WalkPool.logger.Println(ex.getMessage(), WalkPool.logger.DEBUG);
            if(Utils.DEBUG) {
                System.out.println(node[0]+" "+oid+" - "+ex);
                ex.printStackTrace();
            }
//            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, node[0]+","+oid, ex);
        }
    }

}


class WorkerMulticommunity implements Runnable {   
    private String start_oid=null;
    
    private ArrayList node_multicommunity_version = new ArrayList();
    private String oid=null;
    private int port;
    private int timeout;
    private int retries;
//    private Map<String, Map<String, String>> node_oids_map = new HashMap();
    private Map<String, String> oids_map = new HashMap();
    
    public WorkerMulticommunity(ArrayList node_multicommunity_version, String oid, int port, int timeout, int retries) {
        this.node_multicommunity_version = node_multicommunity_version;
        this.oid=oid;
        this.start_oid=oid;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
    }
    
    @Override
    public void run() {
//        if(Utils.DEBUG) System.out.println(node_multicommunity_version.get(0)+" "+oid+" Start.");
        WalkPool.logger.Println(node_multicommunity_version.get(0)+" "+oid+" Start.", WalkPool.logger.DEBUG);
        Send(node_multicommunity_version, oid);
//        if(Utils.DEBUG) System.out.println(node_multicommunity_version.get(0)+" "+oid+" Stop!");
        WalkPool.logger.Println(node_multicommunity_version.get(0)+" "+oid+" Stop!", WalkPool.logger.DEBUG);
    }    
    
//    private void Send(ArrayList node_multicommunity_version, String oid) {
////        System.out.println(node[0]+","+node[1]+","+node[2]+","+oid);
//        Send(node_multicommunity_version, oid);
//    }
    
    private void Send(ArrayList node_multicommunity_version, String oid) {
        try {
            String node = (String)node_multicommunity_version.get(0);
            ArrayList<String> community_list = (ArrayList<String>)node_multicommunity_version.get(1);
            String version = (String)node_multicommunity_version.get(2);
            for(String community : community_list) {
                oids_map = new HashMap();
                if(community != null && !community.equals("")) {
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

                    if (version.equals("2")) {
                        pdu.setType(PDU.GETBULK);
                        pdu.setMaxRepetitions(20);
                    } else
                        pdu.setType(PDU.GETNEXT);

                    ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                    PDU response = event.getResponse();
                    if (event != null && response != null) {
                        boolean next_send=false;
                        String cur_OID="";
                        String ip_port = event.getPeerAddress().toString();
                        if (ip_port != null) { 
                            for( VariableBinding var: response.toArray()) {
                                cur_OID = var.getOid().toString();
                                if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                    String[] mas = new String[3];
                                    mas[0]=ip_port.split("/")[0];
                                    mas[1]=var.getOid().toString();
                                    mas[2]=var.getVariable().toString();
//                                    if(mas[2].equals("5")) System.err.println(mas[0]+" : "+", "+community+", "+version+", "+mas[1]+", "+mas[2]);
//                                    System.err.println(mas[0]+" : "+", "+community+", "+version+", "+mas[1]+", "+mas[2]);
                                    WalkPool.logger.Println(mas[0]+" : "+", "+community+", "+version+", "+mas[1]+", "+mas[2], WalkPool.logger.DEBUG);
//                                    synchronized(WalkPool.res) { WalkPool.res.add(mas); }
                                    synchronized(WalkPool.res) { 
                                        if(oids_map.get(mas[1]) == null) {
                                            oids_map.put(mas[1], mas[1]);
                                            WalkPool.res.add(mas);
                                            next_send=true;                                            
                                        }
                                        else {
                                            next_send=false;
                                            break;                                    
                                        }
                                    }                                    
                                }
                                else {
                                    next_send=false;
                                    break;
                                }
                            }
                            if(next_send) {
        //                        System.out.println("Next send: "+node[0]+","+cur_OID);
                                String[] node_comm_ver = new String[3];
                                node_comm_ver[0]=node; node_comm_ver[1]=community;
                                node_comm_ver[2]=version;
                                if(!SendRetry(node_comm_ver, cur_OID)) 
                                    break;
                            }

                        }
                    } else
                        break;
                } 
            }
                
        } catch (Exception ex) {
            WalkPool.logger.Println(ex.getMessage(), WalkPool.logger.DEBUG);
            if(Utils.DEBUG) {
                System.out.println(node_multicommunity_version.get(0)+" "+oid+" - "+ex);
                ex.printStackTrace();
            }
//            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, node[0]+","+oid, ex);
        }
    }

    private boolean SendRetry(String[] node, String oid) {
        try {
            Address targetAddress = GenericAddress.parse("udp:" + node[0] + "/" + port);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(node[1]));
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout * 1000);
            target.setVersion(SnmpConstants.version1);
            if (node[2].equals("2")) {
                target.setVersion(SnmpConstants.version2c);
            }
            
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            
            if (node[2].equals("2")) {
                pdu.setType(PDU.GETBULK);
                pdu.setMaxRepetitions(20);
            } else
                pdu.setType(PDU.GETNEXT);                          

            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
            PDU response = event.getResponse();
            if (event != null && response != null) {
                boolean next_send=false;
                String cur_OID="";
                String ip_port = event.getPeerAddress().toString();
                if (ip_port != null) { 
                    for( VariableBinding var: response.toArray()) {
                        cur_OID = var.getOid().toString();
                        if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                            String[] mas = new String[3];
                            mas[0]=ip_port.split("/")[0];
                            mas[1]=var.getOid().toString();
                            mas[2]=var.getVariable().toString();
//                            if(mas[2].equals("5")) System.err.println(mas[0]+" : "+", "+node[1]+", "+node[2]+", "+mas[1]+", "+mas[2]);
//                            System.err.println(mas[0]+" : "+", "+node[1]+", "+node[2]+", "+mas[1]+", "+mas[2]);
                            WalkPool.logger.Println(mas[0]+" : "+", "+node[1]+", "+node[2]+", "+mas[1]+", "+mas[2], WalkPool.logger.DEBUG);
                            synchronized(WalkPool.res) { 
                                if(oids_map.get(mas[1]) == null) {
                                    oids_map.put(mas[1], mas[1]);
                                    WalkPool.res.add(mas);
                                    next_send=true;                                            
                                }
                                else {
//                                    System.out.println(oids_map.get(mas[1])+" - "+mas[1]);
                                    next_send=false;
                                    break;                                    
                                }
                            }
                        }
                        else {
                            next_send=false;
                            break;
                        }
                    }
                    if(next_send) {
//                        System.out.println("Next send: "+node[0]+","+cur_OID);
                        SendRetry(node, cur_OID);
                    }
                    
                }
                return true;
            } return false;
                
        } catch (Exception ex) {
            WalkPool.logger.Println(ex.getMessage(), WalkPool.logger.DEBUG);
            if(Utils.DEBUG) {
                System.out.println(node_multicommunity_version.get(0)+" "+oid+" - "+ex);
                ex.printStackTrace();
            }
//            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, node[0]+","+oid, ex);
            return false;
        }
    }
    
}

class WorkerMulticommunityNotBulk implements Runnable {   
    private String start_oid=null;
    
    private ArrayList node_multicommunity_version = new ArrayList();
    private String oid=null;
    private int port;
    private int timeout;
    private int retries;
//    private Map<String, Map<String, String>> node_oids_map = new HashMap();
    private Map<String, String> oids_map = new HashMap();
    
    public WorkerMulticommunityNotBulk(ArrayList node_multicommunity_version, String oid, int port, int timeout, int retries) {
        this.node_multicommunity_version = node_multicommunity_version;
        this.oid=oid;
        this.start_oid=oid;
        this.port=port;
        this.timeout=timeout;
        this.retries=retries;
    }
    
    @Override
    public void run() {
//        if(Utils.DEBUG) System.out.println(node_multicommunity_version.get(0)+" "+oid+" Start.");
        WalkPool.logger.Println(node_multicommunity_version.get(0)+" "+oid+" Start.", WalkPool.logger.DEBUG);
        Send(node_multicommunity_version, oid);
//        if(Utils.DEBUG) System.out.println(node_multicommunity_version.get(0)+" "+oid+" Stop!");
        WalkPool.logger.Println(node_multicommunity_version.get(0)+" "+oid+" Stop!", WalkPool.logger.DEBUG);
    }    
    
//    private void Send(ArrayList node_multicommunity_version, String oid) {
////        System.out.println(node[0]+","+node[1]+","+node[2]+","+oid);
//        Send(node_multicommunity_version, oid);
//    }
    
    private void Send(ArrayList node_multicommunity_version, String oid) {
        try {
            String node = (String)node_multicommunity_version.get(0);
            ArrayList<String> community_list = (ArrayList<String>)node_multicommunity_version.get(1);
            String version = (String)node_multicommunity_version.get(2);
            for(String community : community_list) {
                oids_map = new HashMap();                
                if(community != null && !community.equals("")) {
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

                    pdu.setType(PDU.GETNEXT);

                    ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
                    PDU response = event.getResponse();
                    if (event != null && response != null) {
                        boolean next_send=false;
                        String cur_OID="";
                        String ip_port = event.getPeerAddress().toString();
                        if (ip_port != null) { 
                            for( VariableBinding var: response.toArray()) {
                                cur_OID = var.getOid().toString();
                                if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                                    String[] mas = new String[3];
                                    mas[0]=ip_port.split("/")[0];
                                    mas[1]=var.getOid().toString();
                                    mas[2]=var.getVariable().toString();
//                                    if(Utils.DEBUG) System.err.println(mas[0]+" : "+", "+community+", "+version+", "+mas[1]+", "+mas[2]);
                                    WalkPool.logger.Println(mas[0]+" : "+", "+community+", "+version+", "+mas[1]+", "+mas[2], WalkPool.logger.DEBUG);
                                    synchronized(WalkPool.res) { 
                                        if(oids_map.get(mas[1]) == null) {
                                            oids_map.put(mas[1], mas[1]);
                                            WalkPool.res.add(mas);
                                            next_send=true;                                            
                                        }
                                        else {
                                            next_send=false;
                                            break;                                    
                                        }
                                    }    
                                }
                                else {
                                    next_send=false;
                                    break;
                                }
                            }
                            if(next_send) {
        //                        System.out.println("Next send: "+node[0]+","+cur_OID);
                                String[] node_comm_ver = new String[3];
                                node_comm_ver[0]=node; node_comm_ver[1]=community;
                                node_comm_ver[2]=version;
                                if(!SendRetry(node_comm_ver, cur_OID)) break;
                            }

                        }
                    } else
                        break;
                } 
//                else break;
            }
                
        } catch (Exception ex) {
            WalkPool.logger.Println(ex.getMessage(), WalkPool.logger.DEBUG);
            if(Utils.DEBUG) {
                System.out.println(node_multicommunity_version.get(0)+" "+oid+" - "+ex);
                ex.printStackTrace();
            }
//            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, node[0]+","+oid, ex);
        }
    }

    private boolean SendRetry(String[] node, String oid) {
        try {
            Address targetAddress = GenericAddress.parse("udp:" + node[0] + "/" + port);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(node[1]));
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout * 1000);
            target.setVersion(SnmpConstants.version1);
            if (node[2].equals("2")) {
                target.setVersion(SnmpConstants.version2c);
            }
            
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            
            pdu.setType(PDU.GETNEXT);                          

            ResponseEvent event = WalkPool.snmp.send(pdu, target, null);
            PDU response = event.getResponse();
            if (event != null && response != null) {
                boolean next_send=false;
                String cur_OID="";
                String ip_port = event.getPeerAddress().toString();
                if (ip_port != null) { 
                    for( VariableBinding var: response.toArray()) {
                        cur_OID = var.getOid().toString();
                        if(cur_OID.startsWith(start_oid) && !cur_OID.equals(start_oid)) {
                            String[] mas = new String[3];
                            mas[0]=ip_port.split("/")[0];
                            mas[1]=var.getOid().toString();
                            mas[2]=var.getVariable().toString();
//                            if(Utils.DEBUG) System.err.println(mas[0]+" : "+", "+node[1]+", "+node[2]+", "+mas[1]+", "+mas[2]);
                            WalkPool.logger.Println(mas[0]+" : "+", "+node[1]+", "+node[2]+", "+mas[1]+", "+mas[2], WalkPool.logger.DEBUG);
                            synchronized(WalkPool.res) { 
                                if(oids_map.get(mas[1]) == null) {
                                    oids_map.put(mas[1], mas[1]);
                                    WalkPool.res.add(mas);
                                    next_send=true;                                            
                                }
                                else {
                                    next_send=false;
                                    break;                                    
                                }
                            }
                        }
                        else {
                            next_send=false;
                            break;
                        }
                    }
                    if(next_send) {
//                        System.out.println("Next send: "+node[0]+","+cur_OID);
                        SendRetry(node, cur_OID);
                    }
                    
                }
                return true;
            } return false;
                
        } catch (Exception ex) {
            WalkPool.logger.Println(ex.getMessage(), WalkPool.logger.DEBUG);
            if(Utils.DEBUG) {
                System.out.println(node_multicommunity_version.get(0)+" "+oid+" - "+ex);
                ex.printStackTrace();
            }
//            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, node[0]+","+oid, ex);
            return false;
        }
    }
    
}
