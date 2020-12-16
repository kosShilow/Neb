/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
public class SnmpScanCliNGTest {
    
    public SnmpScanCliNGTest() {
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
     * Test of Scan method, of class ScanCli.
     */
    @Test
    public void testScan_3args_1() {
        System.out.println("ScanCli Scan1");
        String network = "10.97.1.192/28";
        Map<String, String> exclude_list_ip = new HashMap();
        exclude_list_ip.put("10.0.0.1", "10.0.0.1");
        ArrayList<ArrayList<String>> user_password_enablepassword = new ArrayList();
        ArrayList<String> list = new ArrayList();
        list.add("rancid"); list.add("dicnar"); list.add("");
        user_password_enablepassword.add(list);
        ScanCli instance = new ScanCli();
        ArrayList expResult = null;
        ArrayList result = instance.Scan(network, exclude_list_ip, user_password_enablepassword);
        assertNotEquals(result.size(), 0);
        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }


    /**
     * Test of Scan method, of class ScanCli.
     */
//    @Test
//    public void testScan_3args_2() {
//        System.out.println("ScanCli Scan2");
//        ArrayList<String> list_ip = new ArrayList();
//        list_ip.add("10.96.248.103");
////        list_ip.add("10.96.120.203");
//        Map<String, String> exclude_list_ip = new HashMap();
//        exclude_list_ip.put("10.0.0.1", "10.0.0.1");
//        ArrayList<ArrayList<String>> user_password_enablepassword = new ArrayList();
//        ArrayList<String> list = new ArrayList();
//        list.add("rancid"); list.add("dicnar"); list.add("");
//        user_password_enablepassword.add(list);
//        ScanCli instance = new ScanCli();
//        ArrayList expResult = null;
//        ArrayList result = instance.Scan(list_ip, exclude_list_ip, user_password_enablepassword);
//        assertNotEquals(result.size(), 0);
//        // TODO review the generated test code and remove the default call to fail.
////        fail("The test case is a prototype.");
//    }

    
}
