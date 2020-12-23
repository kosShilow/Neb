/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.kos.neb.neb_lib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
public class WalkPoolNGTest {
    
    public WalkPoolNGTest() {
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
     * Тест метод Get, класса WalkPool.
     */
    @Test
    public void testGet_ArrayList_String() throws Exception {
        
        WalkPool instance = new WalkPool();
        
//        ArrayList<String> soft_scan =  new ArrayList();
        System.out.println("WallkPool");
        String node1 = "10.96.248.171";
        String node2 = "10.96.115.254";
        ArrayList<String[]> list_ip =  new ArrayList();
        String[] mas = new String[3];
        mas[0]=node1; mas[1]="20fyufhf80"; mas[2]="2";
        list_ip.add(mas);
        mas = new String[3];
        mas[0]=node2; mas[1]="20fyufhf80"; mas[2]="2";
        list_ip.add(mas);
        String oid = "1.3.6.1.2.1.17.1.4.1.2";
        Map<String, ArrayList> result = instance.Get(list_ip, oid, 161, 5, 3);
        assertTrue(result.size() == 2);           
        
        oid = "1.3.6.1.2.1.17.4.3.1.2";
        ArrayList<ArrayList> node_multicommunity_version_oid_list = new ArrayList();
        ArrayList node_multicommunity_version_oid = new ArrayList();
        node_multicommunity_version_oid.add("10.96.115.254");
//        node_multicommunity_version_oid.add("10.96.250.113");
        ArrayList multicommunity_list = new ArrayList(); 
        multicommunity_list.add("20fyufhf80");
//        multicommunity_list.add("20fyufhf80@1");
//        multicommunity_list.add("20fyufhf80@10");
        node_multicommunity_version_oid.add(multicommunity_list);
        node_multicommunity_version_oid.add("2");
        node_multicommunity_version_oid.add(oid);
        node_multicommunity_version_oid_list.add(node_multicommunity_version_oid);
        result = instance.GetNodeMultiCommunityVersionOid(node_multicommunity_version_oid_list, 161, 10, 3);
        assertTrue(result.size() == 1);  
    
        oid = "1.3.6.1.2.1.17.4.3.1.2";
        node_multicommunity_version_oid_list = new ArrayList();
        node_multicommunity_version_oid = new ArrayList();
        node_multicommunity_version_oid.add("10.96.115.254");
//        node_multicommunity_version_oid.add("10.96.251.216");
//        node_multicommunity_version_oid.add("10.124.4.41");
        multicommunity_list = new ArrayList(); 
        multicommunity_list.add("20fyufhf80@1");
        multicommunity_list.add("20fyufhf80@10");
        node_multicommunity_version_oid.add(multicommunity_list);
        node_multicommunity_version_oid.add("2");
        node_multicommunity_version_oid.add(oid);
        node_multicommunity_version_oid_list.add(node_multicommunity_version_oid);
        result = instance.GetNodeMultiCommunityVersionOid(node_multicommunity_version_oid_list, 161, 10, 3);
        assertTrue(result.size() == 1);         

        
        
        
        
        oid = "1.3.6.1.2.1.17.4.3.1.2";
        ArrayList<ArrayList> node_multicommunity_version_list = new ArrayList();
        ArrayList node_multicommunity_version = new ArrayList();
        node_multicommunity_version.add("10.96.115.254");
        multicommunity_list = new ArrayList(); 
        multicommunity_list.add("20fyufhf80@1");
        multicommunity_list.add("20fyufhf80@10");
        node_multicommunity_version.add(multicommunity_list);
        node_multicommunity_version.add("2");
        node_multicommunity_version_list.add(node_multicommunity_version);
        result = instance.GetNodeMultiCommunityVersion(node_multicommunity_version_list, oid, 161, 10, 3);
        assertTrue(result.size() == 1);

        //////////////////////////////////////////
//        list_ip =  new ArrayList();
//        mas = new String[3];
//        mas[0]="10.32.114.71"; mas[1]="public"; mas[2]="1";
////        mas[0]="10.96.248.171"; mas[1]="20fyufhf80"; mas[2]="1";
//        list_ip.add(mas);
//
////        oid = "1.3.6.1.2.1.3.1.1.2";
//        oid = ".1.3.6.1.2.1.2.2.1.2";
//        instance = new WalkPool();
////        ArrayList expResult = null;
//        result = instance.Get(list_ip, oid);
//        assertTrue(result.size() == 1);        
        
        /////////////////////////////////////////
        
        list_ip =  new ArrayList();
        mas = new String[3];
        mas[0]=node1; mas[1]="20fyufhf80"; mas[2]="1";
        list_ip.add(mas);
        mas = new String[3];
        mas[0]=node2; mas[1]="20fyufhf80"; mas[2]="1";
        list_ip.add(mas);
        oid = ".1.3.6.1.2.1.2.2.1.2";
        instance = new WalkPool();
//        ArrayList expResult = null;
        result = instance.Get(list_ip, oid);
        assertTrue(result.size() == 2);        
        
        list_ip =  new ArrayList();
        mas = new String[3];
        mas[0]=node1; mas[1]="public1"; mas[2]="2";
        list_ip.add(mas);
        mas = new String[3];
        mas[0]=node2; mas[1]="20fyufhf80"; mas[2]="2";
        list_ip.add(mas);
        oid = "1.3.6.1.2.1.2.2.1.2";
        instance = new WalkPool();
//        ArrayList expResult = null;
        result = instance.Get(list_ip, oid);
        assertTrue(result.size() == 1);
        
//        node1 = "10.98.120.200";
//        list_ip =  new ArrayList();
//        mas = new String[3];
//        mas[0]=node1; mas[1]="20fyufhf80"; mas[2]="2";
//        list_ip.add(mas);
//        mas = new String[3];
//        mas[0]=node2; mas[1]="20fyufhf80"; mas[2]="2";
//        list_ip.add(mas);
//        oid = "1.3.6.1.2.1.2.2.1.2";
//        instance = new WalkPool();
////        ArrayList expResult = null;
//        result = instance.Get(list_ip, oid);
//        assertTrue(result.size() == 1);
        
        list_ip =  new ArrayList();
        mas = new String[3];
        mas[0]="10.96.248.62"; mas[1]="20fyufhf80"; mas[2]="2";
        
        list_ip.add(mas);
        String[] mas1 = new String[3];
        mas1[0]="10.96.115.254"; mas1[1]="20fyufhf80"; mas1[2]="2";
        list_ip.add(mas1);        
//        oid = "1.3.6.1.2.1.2.2.1.2";
        oid = "1.3.6.1.2.1.17.4.3.1.2";
//        oid = "1.3.6.1.2.1.17.7.1.2.2.1.2";
        instance = new WalkPool();
       
        Map<String, Boolean> res = instance.Test(list_ip, oid, 161, 3, 1, 10);
//        assertTrue(res.size() > 0);
        Map<String, Boolean> res1 = instance.Test(list_ip, oid, 161, 10, 5, 2);
        assertTrue(res.size() > 0);
        
        ArrayList list_node_community_version_oid =  new ArrayList();
        mas = new String[4];
        mas[0]="10.96.115.254"; mas[1]="20fyufhf80"; mas[2]="2"; mas[3]="1.3.6.1.2.1.17.4.3.1.2";
        list_node_community_version_oid.add(mas);
        mas = new String[4];
        mas[0]="10.96.113.30"; mas[1]="20fyufhf80"; mas[2]="2"; mas[3]="1.3.6.1.2.1.17.4.3.1.2";
        list_node_community_version_oid.add(mas);
        
//        try {
//            BufferedReader in = new BufferedReader(new FileReader("c:/Temp/run/tmp_node_community_version"));
//            try {
//                String s;
//                while ((s = in.readLine()) != null) {
//                    list_node_community_version_oid.add(s.split(","));
////                    System.out.println(s);
//                }
//            } finally {
//                //Также не забываем закрыть файл
//                in.close();
//            }
//        } catch(IOException e) {
//            throw new RuntimeException(e);
//        }        
        
        instance = new WalkPool();
//        ArrayList expResult = null;
        result = instance.Get(list_node_community_version_oid);
        assertTrue(result.size() == 2);          
        
    }


}
