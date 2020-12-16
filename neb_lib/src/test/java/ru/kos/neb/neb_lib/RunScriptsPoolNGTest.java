/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

import java.util.ArrayList;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author kos
 */
public class RunScriptsPoolNGTest {
    
    public RunScriptsPoolNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of Get method, of class RunScriptsPool.
     */

    @Test
    public void testGet() {
        System.out.println("Get Cli_test");
//        ArrayList<String> list_ip = new ArrayList();
        String node1 = "10.96.113.30";
//        String node1 = "10.124.248.30";
        String node2 = "10.96.115.254";
//        String node2 = "10.96.113.30";
//        list_ip.add(node1);
//        list_ip.add(node2);

        Map<String, ArrayList<String>> scripts = new HashMap();
        ArrayList<String> list_tmp = new ArrayList();
        list_tmp.add("java -jar -Xmx16M c:/Temp/Scripts/Cli_test.jar");
        scripts.put("ssh", list_tmp);
        list_tmp = new ArrayList();
        list_tmp.add("java -jar -Xmx16M c:/Temp/Scripts/Cli_test.jar");
        scripts.put("telnet", list_tmp);        

        Map<String, ArrayList<String[]>> node_protocol_accounts = new HashMap();
        ArrayList list = new ArrayList();
        String[] mas1 = new String[4];
        mas1[0]="ssh"; mas1[1]="rancid"; mas1[2]="dicnar"; mas1[3]="";
        list.add(mas1);
        String[] mas2 = new String[4];
        mas2[0]="telnet"; mas2[1]="rancid"; mas2[2]="dicnar"; mas2[3]="";
        list.add(mas2);
        String[] mas3 = new String[4];
        mas3[0]="ssh"; mas3[1]="config"; mas3[2]="LfqRjyabuVyt"; mas3[3]="";
        list.add(mas3);     
        String[] mas4 = new String[4];
        mas4[0]="telnet"; mas4[1]="config"; mas4[2]="LfqRjyabuVyt"; mas4[3]="";
        list.add(mas4);         
        node_protocol_accounts.put(node1, list);
        
        list = new ArrayList();
        mas1 = new String[4];
        mas1[0]="ssh"; mas1[1]="rancid"; mas1[2]="dicnar"; mas1[3]="";
        list.add(mas1);
        mas2 = new String[4];
        mas2[0]="telnet"; mas2[1]="rancid"; mas2[2]="dicnar"; mas2[3]="";
        list.add(mas2);
        mas3 = new String[4];
        mas3[0]="ssh"; mas3[1]="config"; mas3[2]="LfqRjyabuVyt"; mas3[3]="";
        list.add(mas3);     
        mas4 = new String[4];
        mas4[0]="telnet"; mas4[1]="config"; mas4[2]="LfqRjyabuVyt"; mas4[3]="";
        list.add(mas4);         
        node_protocol_accounts.put(node2, list);
        
//        for(int i=0; i<100; i++ ) node_protocol_accounts.put(String.valueOf(i), list);
        
        RunScriptsPool instance = new RunScriptsPool(30*60*1000, 10*60*1000, 32);
        ArrayList<String> result = instance.Get(node_protocol_accounts, scripts);
        assertTrue(result.size() > 0);
        
        
////////////////////////////////////////////////        
        System.out.println("Get RunScriptsPool");
//        ArrayList<String> list_ip = new ArrayList();
        node1 = "10.96.115.254";
//        String node1 = "10.124.248.30";
//        node2 = "10.96.248.168";
        node2 = "10.96.113.30";
//        list_ip.add(node1);
//        list_ip.add(node2);

        scripts = new HashMap();
        list_tmp = new ArrayList();
        list_tmp.add("java -jar -Xmx64M c:/Temp/Scripts/Cisco_information.jar");
        scripts.put("ssh", list_tmp);
        list_tmp = new ArrayList();
        list_tmp.add("java -jar -Xmx64M c:/Temp/Scripts/Cisco_information.jar");
        scripts.put("telnet", list_tmp);        
        list_tmp = new ArrayList();
        list_tmp.add("java -jar c:/Temp/Scripts/SNMP_information.jar");
        scripts.put("snmp", list_tmp); 
        
        node_protocol_accounts = new HashMap();
        list = new ArrayList();
        mas1 = new String[4];
        mas1[0]="ssh"; mas1[1]="rancid"; mas1[2]="dicnar"; mas1[3]="";
        list.add(mas1);
        mas2 = new String[4];
        mas2[0]="telnet"; mas2[1]="rancid"; mas2[2]="dicnar"; mas2[3]="";
        list.add(mas2);
        mas3 = new String[3];
        mas3[0]="snmp"; mas3[1]="20fyufhf80"; mas3[2]="2";
        list.add(mas3);        
        node_protocol_accounts.put(node1, list);
        
        list = new ArrayList();
        mas1 = new String[4];
        mas1[0]="ssh"; mas1[1]="rancid"; mas1[2]="dicnar"; mas1[3]="";
        list.add(mas1);
        mas2 = new String[4];
        mas2[0]="telnet"; mas2[1]="rancid"; mas2[2]="dicnar"; mas2[3]="";
        list.add(mas2);
        mas3 = new String[3];
        mas3[0]="snmp"; mas3[1]="20fyufhf80"; mas3[2]="2";
        list.add(mas3);        
        node_protocol_accounts.put(node2, list);
        
        instance = new RunScriptsPool(30*60*1000, 10*60*1000, 16);
        result = instance.Get(node_protocol_accounts, scripts);
        assertTrue(result.size() > 0);
    }
    
}
