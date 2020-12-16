package ru.kos.neb.neb_lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.nio.channels.DatagramChannel;
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
public class PingPool {
    public static Map<String, String> result = new HashMap();
    public static String[] out = new String[2];
    
    private int LIMITS_INTERVAL = 128;
    private int MAXPOOLTHREADS = 128;
    private int timeout = 3;
    private int retries = 2;
    public static Logger logger;
    
    Utils utils = new Utils();
    
    public PingPool() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }     

    public Map<String, String> Get(ArrayList<String> list_ip)
    {
        return GetProc(list_ip, this.timeout, this.retries);
    }       
    public Map<String, String> Get(ArrayList<String> list_ip, int timeout)
    {
        return GetProc(list_ip, timeout, this.retries);
    }    
    public Map<String, String> Get(ArrayList<String> list_ip, int timeout, int retries)
    {
        return GetProc(list_ip, timeout, retries);
    }
    
    private Map<String, String> GetProc(ArrayList<String> list_ip, int timeout, int retries) {
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
//                System.out.println("node: "+node);
                WorkerPing worker = new WorkerPing(node, timeout, retries);
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
    
    public Map<String, String> Get(String network, Map<String, String> exclude_list_ip)
    {
        return GetProc(network, exclude_list_ip, this.timeout, this.retries);
    }       
    public Map<String, String> Get(String network, Map<String, String> exclude_list_ip, int timeout)
    {
        return GetProc(network, exclude_list_ip, timeout, this.retries);
    }    
    public Map<String, String> Get(String network, Map<String, String> exclude_list_ip, int timeout, int retries)
    {
        return GetProc(network, exclude_list_ip, timeout, retries);
    }    
    private Map<String, String> GetProc(String network, Map<String, String> exclude_list_ip, int timeout, int retries) {
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
                        WorkerPing worker = new WorkerPing(ip, timeout, retries);
                        service.submit(worker);
                    }
                }
            } else {
                long addr = interval[0];
                String ip=utils.NetworkToIPAddress(addr);
                if(exclude_list_ip.get(ip) == null) {
                    WorkerPing worker = new WorkerPing(ip, timeout, retries);
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
}

class WorkerPing implements Runnable {   
    private String node;
    private int timeout;
    private int retries;
    
    public WorkerPing(String node, int timeout, int retries) {
        this.node = node;
        this.timeout=timeout;
        this.retries=retries;
    }
    
    @Override
    public void run() {
        try {
    //        System.out.println(Thread.currentThread().getName()+" Start.");
            // Get OS
            String os = getOsName();
            if (os.equals("windows"))
            {
                try
                {
                    ProcessBuilder theProcess1= new ProcessBuilder("ping.exe", "-n", String.valueOf(retries), "-w", String.valueOf(timeout), node);
                    theProcess1=theProcess1.redirectErrorStream(true);
                    Process theProcess=theProcess1.start();
                    InputStream o=theProcess.getInputStream();

                    InputStreamReader ir=new InputStreamReader(o);
                    BufferedReader inStream2 = new BufferedReader(ir);
                    String line=null;
                    int ok_priznak=0;
                    boolean end=false;
                    while((line=inStream2.readLine())!=null)
                    {
    //                    System.out.println("in-"+line);
                         if (line.trim().length() > 0)
                         {
                             if(line.matches(".+(\\d+%\\s\\S+).+"))
                             {
                                 end=true;
                                 if(!line.matches(".+(100%\\s\\S+).+")) ok_priznak++;
                             }
                             if(line.matches(".+"+node+":.+TTL=\\d+$")) ok_priznak++;
    //                             if(line.indexOf(address+":") >= 0 && line.indexOf("TTL=") >= 0) ok_priznak++;
                         }

                    }
                    if(ok_priznak >= 2) {
                        synchronized (PingPool.result) { PingPool.result.put(node, "ok"); }
                    }
                    else synchronized (PingPool.result) { PingPool.result.put(node, "err"); }
                    theProcess.waitFor();
                }
                catch(Exception e)
                {
    //                e.printStackTrace();
                    synchronized (PingPool.result) { PingPool.result.put(node, "err"); }
                }
            }
            else
            {
                try
                {
                    InetAddress aHost = InetAddress.getByName(node);
                    for(int i=0; i<retries; i++)
                    {
                        if(aHost.isReachable(timeout)) {
                            synchronized (PingPool.result) { PingPool.result.put(node, "ok"); }
                        }
                        else synchronized (PingPool.result) { PingPool.result.put(node, "err"); }
                    }
                }
                catch (Exception ex)
                {
                    System.out.println(ex);
                    synchronized (PingPool.result) { PingPool.result.put(node, "err"); }
                }
            }
        } catch (Exception ex) {
            PingPool.logger.Println(ex.getMessage(), PingPool.logger.DEBUG);
        }
        finally {
        }
//        System.out.println(Thread.currentThread().getName()+" Stop!");
    }    
    
    public static String getOsName()
    {
        String os = "";
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1)
        {
            os = "windows";
        }
        else if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1)
        {
            os = "linux";
        }
        else if (System.getProperty("os.name").toLowerCase().indexOf("mac") > -1)
        {
            os = "mac";
        }

        return os;
    }
}

