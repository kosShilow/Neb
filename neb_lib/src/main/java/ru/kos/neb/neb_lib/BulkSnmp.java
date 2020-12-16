/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

import java.util.ArrayList;
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
public class BulkSnmp {

    private final static int BULK_SIZE = 10;
    private ArrayList<String[]> result = new ArrayList();
    private Snmp snmp = null;
    private String start_oid = null;
    public static Logger logger;
    
    public BulkSnmp() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }    

    public ArrayList<String[]> Get(String node, String community, String oid, String version, int port, int timeout) {
        int retries = 2;
        return Get(node, community, oid, version, port, timeout, retries);
    }

    public ArrayList<String[]> Get(String node, String community, String oid, String version, int port) {
        int timeout = 3; // in sec
        int retries = 2;
        return Get(node, community, oid, version, port, timeout, retries);
    }

    public ArrayList<String[]> Get(String node, String community, String oid, String version) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        return Get(node, community, oid, version, port, timeout, retries);
    }

    public ArrayList<String[]> Get(String node, String community, String oid, String version, int port, int timeout, int retries) {
        return GetList(node, community, oid, version, port, timeout, retries);
    }
    
    private ArrayList<String[]> GetList(String node, String community, String oid, String version, int port, int timeout, int retries) {
        result.clear();
        start_oid=oid;
        try {        
            ExecutorService service = Executors.newCachedThreadPool();
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();          

            Send(node, community, oid, version, port, timeout, retries);
            
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

    private void Send(String node, String community, String oid, String version, int port, int timeout, int retries) {
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
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GETBULK);
            pdu.setMaxRepetitions(BULK_SIZE);
            
            ResponseEvent event = snmp.send(pdu, target);

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
                            result.add(mas);
                            next_send=true;
                        }
                        else {
                            next_send=false;
                            break;
                        }
                    }
                    if(next_send) {
//                        System.out.println("Next send: "+node[0]+","+cur_OID);
                        Send(node, community, cur_OID, version, port, timeout, retries);
                    }
                    
                }
            }           
  
        } catch (Exception ex) {
            logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, node+","+oid, ex);
        }
    }    
}
