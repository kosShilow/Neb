/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

/**
 *
 * @author kos
 */
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

public class GetSnmp {

    private Snmp snmp = null;
    public static Logger logger;
    
    public GetSnmp() {
        // start logging
        logger = new Logger(Utils.LOG_FILE);
        if(Utils.DEBUG)
            logger.SetLevel(logger.DEBUG);
        else
            logger.SetLevel(logger.INFO);
    }    

    public String[] Get(String node, String community, String oid, int version, int port, int timeout) {
        int retries = 3;
        return Get(node, community, oid, version, port, timeout, retries);
    }

    public String[] Get(String node, String community, String oid, int version, int port) {
        int timeout = 3; // in sec
        int retries = 2;
        return Get(node, community, oid, version, port, timeout, retries);
    }

    public String[] Get(String node, String community, String oid, int version) {
        int port = 161;
        int timeout = 3; // in sec
        int retries = 2;
        return Get(node, community, oid, version, port, timeout, retries);
    }

    public String[] Get(String node, String community, String oid, int version, int port, int timeout, int retries) {
        return GetValue(node, community, oid, version, port, timeout, retries);
    }

    private String[] GetValue(String node, String community, String oid, int version, int port, int timeout, int retries) {
        String[] result = new String[2];
        try {
            result[0] = oid;
            result[1] = null;
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
            Address targetAddress = GenericAddress.parse("udp:" + node + "/" + port);
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(community));
            target.setAddress(targetAddress);
            target.setRetries(retries);
            target.setTimeout(timeout * 1000);
            target.setVersion(SnmpConstants.version1);
            if (version == 2) {
                target.setVersion(SnmpConstants.version2c);
            }

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);
            
            ResponseEvent event = snmp.send(pdu, target);
            PDU response = event.getResponse();
            if (event != null && response != null) {
                String oidstr = response.get(0).getOid().toString();
                String val = response.get(0).getVariable().toString();
                if (val.equals("Null")) {
                    result[0] = oidstr;
                    result[1] = "";
                }
                else if (val.equals("noSuchObject")) {
                    result[0] = oidstr;
                    result[1] = "";
                }
                else if (val.equals("noSuchInstance")) {
                    result[0] = oidstr;
                    result[1] = "";
                } else {
                    result[0] = oidstr;
                    result[1] = val;                    
                }                         
            }

            // disconnect
            if(snmp != null) snmp.close();
            if(transport != null) transport.close();
        } catch (Exception ex) {
            logger.Println(ex.getMessage(), logger.DEBUG);
//            Logger.getLogger(GetSnmp.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            try { 
                if(snmp != null) snmp.close();
            } catch (Exception ex) {  }           
        }
        return result;
    }

}
