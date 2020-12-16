/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.kos.neb.neb_lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.snmp4j.event.ResponseEvent;
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
public class SnmpScanNGTest {
    
    public SnmpScanNGTest() {
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

    private boolean CheckEqualsLists(ArrayList<String[]> list1, ArrayList<String[]> list2)
    {
        int num=0;
        for(String[] r : list1)
        {
            boolean find=false;
            for(String[] t : list2)
            {
                if(r[0].equals(t[0]) && r[1].equals(t[1]) && r[2].equals(t[2])) { find=true; break; }
            }
            if(find) num++;
        }
        return num == list1.size();
    }
    /**
     * Тест метод Scan, класса SnmpScan.
     */
    @Test
    public void testScan_6args() throws Exception {
        ArrayList<String> community_list = new ArrayList();
        community_list.add("20fyufhf80");
        community_list.add("public1");
        String oid = "1.3.6.1.2.1.1.3.0";
        int port = 161;
        int timeout = 1;
        int retries = 3;        
        
        System.out.println("Scan1");
        String network = "10.97.1.192/28";
        Map<String, String> exclude_list_ip = new HashMap();
        exclude_list_ip.put("10.0.0.1", "10.0.0.1");
        SnmpScan instance = new SnmpScan();
        ArrayList expResult = new ArrayList();
        String[] mas = new String[3];
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
//        mas = new String[3];
//        mas[0]="10.96.113.30"; mas[1]="20fyufhf80"; mas[2]="2";
//        expResult.add(mas); 
        ArrayList result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));
        
        System.out.println("Scan2");
        expResult = new ArrayList();
        network = "10.97.1.192 255.255.255.240";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
//        mas = new String[3];
//        mas[0]="10.96.113.30"; mas[1]="20fyufhf80"; mas[2]="2";
//        expResult.add(mas); 
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));
        
        System.out.println("Scan3");
        expResult = new ArrayList();
        network = "10.97.1.192/28";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));   
        
        System.out.println("Scan4");
        expResult = new ArrayList();
        network = "10.97.1.200/31";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));    
        
        System.out.println("Scan5");
        expResult = new ArrayList();
        network = "10.97.1.200 255.255.255.254";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));           
        
        System.out.println("Scan6");
        expResult = new ArrayList();
        network = "10.97.1.200/32";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));                   

        System.out.println("Scan7");
        expResult = new ArrayList();
        network = "10.97.1.200 255.255.255.255";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));   
        
        System.out.println("Scan8");
        expResult = new ArrayList();
        network = "10.97.1.200";
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));         
        
        System.out.println("Scan9");
        network = "10.97.1.201/32";
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(result.isEmpty());    
        
        System.out.println("Scan10");
        network = "10.97.1.201 255.255.255.255";
        result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(result.isEmpty());          
     
        System.out.println("Scan11");
        ArrayList<String> list_ip = new ArrayList();
        list_ip.add("10.97.1.200");
        list_ip.add("10.97.1.203");
        expResult = new ArrayList();
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        mas = new String[3];
        mas[0]="10.96.113.30"; mas[1]="20fyufhf80"; mas[2]="2";        
        result = instance.Scan(list_ip, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertTrue(CheckEqualsLists(expResult, result));
        
        System.out.println("Scan12");
        list_ip = new ArrayList();
        list_ip.add("10.97.1.200");
        list_ip.add("10.96.113.30");
        expResult = new ArrayList();
        mas[0]="10.97.1.200"; mas[1]="20fyufhf80"; mas[2]="2";
        expResult.add(mas);
        mas = new String[3];
        mas[0]="10.96.113.30"; mas[1]="20fyufhf80"; mas[2]="2"; 
        
        Map<String, String[]> snmp_accounts_priority = new HashMap();
        String[] mas1 = new String[2];
        mas1[0]="20fyufhf80"; mas1[1]="2";
        snmp_accounts_priority.put("10.97.1.200", mas1);
        mas1 = new String[2];
        mas1[0]="20fyufhf80"; mas1[1]="2";
        snmp_accounts_priority.put("10.96.113.30", mas1);        
        
        result = instance.Scan(list_ip, exclude_list_ip, community_list, oid, port, timeout, retries, snmp_accounts_priority);
        assertTrue(CheckEqualsLists(expResult, result));         
    }

    /**
     * Тест метод Scan, класса SnmpScan.
     */
    @Test(enabled = false)
    public void testScan_5args() {
        System.out.println("Scan");
        String network = "";
        Map<String, String> exclude_list_ip = new HashMap();
        exclude_list_ip.put("10.0.0.1", "10.0.0.1");        
        ArrayList<String> community = null;
        String oid = "";
        int port = 0;
        SnmpScan instance = new SnmpScan();
        ArrayList expResult = null;
        ArrayList result = instance.Scan(network, exclude_list_ip, community, oid, port);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    /**
     * Тест метод Scan, класса SnmpScan.
     */
    @Test(enabled = false)
    public void testScan_4args_1() {
        System.out.println("Scan");
        String network = "";
        Map<String, String> exclude_list_ip = new HashMap();
        exclude_list_ip.put("10.0.0.1", "10.0.0.1");        
        ArrayList<String> community = null;
        String oid = "";
        SnmpScan instance = new SnmpScan();
        ArrayList expResult = null;
        ArrayList result = instance.Scan(network, exclude_list_ip, community, oid);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    /**
     * Тест метод Scan, класса SnmpScan.
     */
    @Test(enabled = false)
    public void testScan_4args_2() {
        System.out.println("Scan");
        ArrayList<String> list_ip = null;
        Map<String, String> exclude_list_ip = new HashMap();
        exclude_list_ip.put("10.0.0.1", "10.0.0.1");        
        ArrayList<String> community = null;
        String oid = "";
        SnmpScan instance = new SnmpScan();
        ArrayList expResult = null;
        ArrayList result = instance.Scan(list_ip, exclude_list_ip, community, oid);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    /**
     * Тест метод Scan, класса SnmpScan.
     */
    @Test(enabled = false)
    public void testScan_7args() {
        System.out.println("Scan");
        String network = "";
        Map<String, String> exclude_list_ip = new HashMap();
        exclude_list_ip.put("10.0.0.1", "10.0.0.1");        
        ArrayList<String> community_list = null;
        String oid = "";
        int port = 0;
        int timeout = 0;
        int retries = 0;
        SnmpScan instance = new SnmpScan();
        ArrayList expResult = null;
        ArrayList result = instance.Scan(network, exclude_list_ip, community_list, oid, port, timeout, retries);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }


    
}
