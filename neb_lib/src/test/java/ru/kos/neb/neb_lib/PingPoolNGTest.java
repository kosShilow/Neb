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
public class PingPoolNGTest {
    
    public PingPoolNGTest() {
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

    @Test
    public void testGet_3args() {
        System.out.println("Ping");
        ArrayList<String> list_ip = new ArrayList();
        list_ip.add("10.97.1.200");
        list_ip.add("10.98.120.200");
        int timeout = 3;
        int retries = 1;
        PingPool instance = new PingPool();
        Map expResult = null;
        Map result = instance.Get(list_ip, timeout, retries);
        assertEquals(result.get("10.97.1.200"), "ok");
        assertEquals(result.get("10.98.120.200"), "err");
    }

    @Test
    public void testGet_4args() {
        System.out.println("Ping network");
        String network = "10.96.120.192/28";
        Map<String, String> exclude_list_ip = new HashMap();
        int timeout = 3;
        int retries = 1;
        PingPool instance = new PingPool();
        Map expResult = null;
        Map result = instance.Get(network, exclude_list_ip, timeout, retries);
        assertEquals(result.size(), 14);
    }
    
}
