/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static ru.kos.neb.neb_builder.Neb.logger;
//import static ru.kos.neb.neb_builder.Neb.cfg;
//import static ru.kos.neb.neb_builder.Neb.home;
//import static ru.kos.neb.neb_builder.Neb.logger;
//import static ru.kos.neb.neb_builder.Neb.utils;

/**
 *
 * @author kos
 */
public class NebNGTest {
    
    public NebNGTest() {
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
     * Test of main method, of class Neb.
     */
//    @Test
//    public void testMain() {
//        System.out.println("Neb main test");
////        Neb.home = Neb.utils.GetHomePath();
////        System.out.println("home=" + Neb.home);
//        Neb.logger = new Logger("neb.log");
//        // read config file
//        Neb.cfg = Neb.utils.ReadConfig("neb.cfg");
//        ru.kos.neb.neb_lib.Utils.DEBUG=true;
//        logger.SetLevel(logger.DEBUG);
//        
//        int http_port = ((Long) Neb.cfg.get("http_port")).intValue();
//        Server_HTTP server_HTTP = new Server_HTTP(http_port);
//        server_HTTP.start();
//        
//        Utils utils = new Utils();
//        ArrayList<String> networks = new ArrayList();
//        ArrayList<String> ip_list = new ArrayList();
//        ip_list.add("10.96.113.30");
//        ip_list.add("10.96.113.12");
//        ip_list.add("10.96.120.39");
//        ip_list.add("10.120.63.10");
//        ip_list.add("10.96.248.36");
////        ip_list.add("10.150.64.3");
////        ip_list.add("10.150.64.4");        
//        ArrayList<String> community_list = new ArrayList();
//        community_list.add("20fyufhf80");
//        community_list.add("public");
//        ArrayList<ArrayList<String>> accounts_list = new ArrayList();
//        ArrayList<String> account = new ArrayList();
//        account.add("rancid"); account.add("dicnar");
//        accounts_list.add(account);
//        Map<String, String> exclude_list = new HashMap();
//        exclude_list.put("10.0.0.1", "10.0.0.1");
//        ArrayList<String> include_sysDescr_cli_matching = new ArrayList();
//        include_sysDescr_cli_matching.add("[Cc]isco");
//        include_sysDescr_cli_matching.add("NX-OS");
//        Map informationFromNodes = utils.ScanInformationFromNodes(networks, ip_list, community_list, accounts_list, exclude_list, include_sysDescr_cli_matching);        
//        assertEquals(((Map<String, Map>)(informationFromNodes.get("nodes_information"))).size(), 3);
//
//    }
    
}
