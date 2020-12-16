package ru.kos.neb.neb_lib;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 *
 * @author kos
 */
public class NodesScanPool {
    public static Map<String, String> result = new HashMap();
    public static String[] out = new String[2];
    
    private int LIMITS_INTERVAL = 128;
    private int MAXPOOLTHREADS = 128;
    ArrayList<Integer> excluded_port = new ArrayList();
    private int timeout = 3;
    private int retries = 2;
    public static Logger logger;
    
    Utils utils = new Utils();
    
    
    
    public NodesScanPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }      

    public Map<String, String> Get(ArrayList<String> list_ip, ArrayList<Integer> excluded_port)
    {
        return GetProc(list_ip, excluded_port, this.timeout, this.retries);
    }       
    public Map<String, String> Get(ArrayList<String> list_ip, ArrayList<Integer> excluded_port, int timeout)
    {
        return GetProc(list_ip, excluded_port, timeout, this.retries);
    }    
    public Map<String, String> Get(ArrayList<String> list_ip, ArrayList<Integer> excluded_port, int timeout, int retries)
    {
        return GetProc(list_ip, excluded_port, timeout, retries);
    }
    
    private Map<String, String> GetProc(ArrayList<String> list_ip, ArrayList<Integer> excluded_port, int timeout, int retries) {
        try {
            result.clear();
            this.timeout=timeout;
            this.retries=retries;

            ExecutorService service;
            if(list_ip.size() > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }

            PrintStream oldError = System.err;
            System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            for (String node : list_ip) {
    //            System.out.println("node: "+node);
//                WorkerPing worker = new WorkerPing(node, timeout, retries);
                WorkerTCPScan worker = new WorkerTCPScan(node, excluded_port, timeout, retries);
                service.submit(worker);
    //                        System.out.println("execute - "+walkPool.res.size());
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) { e.printStackTrace(); } }
            System.setErr(oldError);
        }
        catch (Exception ex) { 
            logger.Println(ex.getMessage(), logger.DEBUG);
        }
//        System.out.println("Finished all threads");
        return result;
    }
    
    public Map<String, String> Get(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> excluded_port)
    {
        return GetProc(network, exclude_list_ip, excluded_port, this.timeout, this.retries);
    }       
    public Map<String, String> Get(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> excluded_port, int timeout)
    {
        return GetProc(network, exclude_list_ip, excluded_port, timeout, this.retries);
    }    
    public Map<String, String> Get(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> excluded_port, int timeout, int retries)
    {
        return GetProc(network, exclude_list_ip, excluded_port, timeout, retries);
    }    
    private Map<String, String> GetProc(String network, Map<String, String> exclude_list_ip, ArrayList<Integer> excluded_port, int timeout, int retries) {
        try {
            result.clear();
            this.timeout=timeout;
            this.retries=retries;

            ExecutorService service;
            long[] interval = utils.IntervalNetworkAddress(network);
            if(interval[1] - interval[0] > LIMITS_INTERVAL) {
                service = Executors.newFixedThreadPool(MAXPOOLTHREADS);
            } else {
                service = Executors.newCachedThreadPool();
            }       

            PrintStream oldError = System.err;
            if(!Utils.DEBUG) System.setErr(new PrintStream(new OutputStream() { public void write(int b) {} }));
            if (interval[1] - interval[0] > 3) {
                for (long addr = interval[0] + 1; addr < interval[1]; addr++) {
                    String ip=utils.NetworkToIPAddress(addr);
                    if(exclude_list_ip.get(ip) == null) {
                        WorkerTCPScan worker = new WorkerTCPScan(ip, excluded_port, timeout, retries);
                        service.submit(worker);
                    }
                }
            } else {
                long addr = interval[0];
                String ip=utils.NetworkToIPAddress(addr);
                if(exclude_list_ip.get(ip) == null) {
                    WorkerTCPScan worker = new WorkerTCPScan(ip, excluded_port, timeout, retries);
                    service.submit(worker);
                }
            }
            service.shutdown();
            while (!service.isTerminated()) { try { Thread.sleep(100L); } catch (InterruptedException e) { e.printStackTrace(); } }
            if(!Utils.DEBUG) System.setErr(oldError);
        } 
        catch (Exception ex) {    
            logger.Println(ex.getMessage(), logger.DEBUG);
        }
        finally {
        }
//        System.out.println("Finished all threads");
        return result;
    }

    private boolean TCPportIsOpen(String node, int port, int timeout){
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

class WorkerTCPScan implements Runnable {   
    private String node;
    private int timeout;
    private int retries;
    private ArrayList<Integer> excluded_port;
    
    public WorkerTCPScan(String node, ArrayList<Integer> excluded_port, int timeout, int retries) {
        this.node = node;
        this.timeout=timeout;
        this.retries=retries;
        this.excluded_port = excluded_port;
    }
    
    @Override
    public void run() {
        NodesScanPool.logger.Println("Start TCPportIsOpen - "+this.node, NodesScanPool.logger.DEBUG);
//        if(Utils.DEBUG) System.out.println("Start TCPportIsOpen - "+this.node);
//        if(TCPportIsOpen(this.node, 22, this.timeout) || TCPportIsOpen(this.node, 23, this.timeout)) { 
            boolean exclude = false;
            for(int port : this.excluded_port) {
                if(TCPportIsOpen(this.node, port, this.timeout)) {
                    exclude=true;
                    break;
                }
            }            
            if(!exclude) {
                synchronized (NodesScanPool.result) { NodesScanPool.result.put(this.node, "ok"); }
            }
            else {
                synchronized (NodesScanPool.result) { NodesScanPool.result.put(this.node, "err"); }
            }
//        }
//        else {
//            synchronized (NodesScanPool.result) { NodesScanPool.result.put(this.node, "err"); }
//        }
//        if(Utils.DEBUG) System.out.println("Stop TCPportIsOpen - "+this.node+"   ok");
        NodesScanPool.logger.Println("Stop TCPportIsOpen - "+this.node+"   ok", NodesScanPool.logger.DEBUG);
//        System.out.println(Thread.currentThread().getName()+" Stop!");
    }    
    
    private boolean TCPportIsOpen(String node, int port, int timeout){
        try {
            InetAddress IP = InetAddress.getByName(node);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(IP, port), timeout*1000);
            socket.close();
//            if(Utils.DEBUG) System.out.println("PortIsOpen - "+node+" "+port+" "+timeout+" : OK");
            return true;
        } catch (Exception ex) {
//            if(Utils.DEBUG) System.out.println("PortIsOpen - "+node+" "+port+" "+timeout+" : ERR");
            return false;
        }
    }
}

