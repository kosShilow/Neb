/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.kos.neb.neb_lib;

import java.util.ArrayList;
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
public class GetPoolNGTest {
    
    public GetPoolNGTest() {
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
     * Тест метод Get, класса GetPool.
     */
    @Test
    public void testGet_ArrayList_ArrayList() throws Exception {
        System.out.println("GetPool");
        
        GetPool instance = new GetPool();
        String node1 = "10.97.1.200";
        String node2 = "10.96.113.30";
        ArrayList<String[]> list_ip =  new ArrayList();
        String[] mas = new String[3];
        mas[0]=node1; mas[1]="20fyufhf80"; mas[2]="2";
        list_ip.add(mas);
        mas = new String[3];
        mas[0]=node2; mas[1]="20fyufhf80"; mas[2]="2";
        list_ip.add(mas);
        
        ArrayList<String> oid_list =  new ArrayList();
        oid_list.add("1.3.6.1.2.1.1.1.0");
        oid_list.add("1.3.6.1.2.1.1.3.0");
        Map<String, ArrayList<String[]>> result = instance.Get(list_ip, oid_list, 10, 2);
        assertTrue(result.size() == 2);
        
        instance = new GetPool();
        node1 = "10.97.1.200";
        node2 = "10.98.113.30";
        list_ip =  new ArrayList();
        mas = new String[3];
        mas[0]=node1; mas[1]="20fyufhf80"; mas[2]="1";
        list_ip.add(mas);
        mas = new String[3];
        mas[0]=node2; mas[1]="20fyufhf80"; mas[2]="2";
        list_ip.add(mas);
        
        oid_list =  new ArrayList();
        oid_list.add("1.3.6.1.2.1.1.1.0");
        oid_list.add("1.3.6.1.2.1.1.3.0");
        result = instance.Get(list_ip, oid_list);
        assertTrue(result.size() == 1);        
 
        instance = new GetPool();
        node1 = "10.97.1.200";
        node2 = "10.96.113.30";
        list_ip =  new ArrayList();
        mas = new String[3];
        mas[0]=node1; mas[1]="20fyufhf80"; mas[2]="1";
        list_ip.add(mas);
        mas = new String[3];
        mas[0]=node2; mas[1]="public1"; mas[2]="2";
        list_ip.add(mas);
        
        oid_list =  new ArrayList();
        oid_list.add("1.3.6.1.2.1.1.1.0");
        oid_list.add("1.3.6.1.2.1.1.3.0");
        result = instance.Get(list_ip, oid_list);
        assertTrue(result.size() == 1);         
    }

    /**
     * Тест метод Get, класса GetPool.
     */
    @Test(enabled = false)
    public void testGet_3args() {
        System.out.println("Get");
        ArrayList<String[]> list_ip = null;
        ArrayList<String> oid_list = null;
        int port = 0;
        GetPool instance = new GetPool();
        Map expResult = null;
        Map result = instance.Get(list_ip, oid_list, port);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    /**
     * Тест метод Get, класса GetPool.
     */
    @Test(enabled = false)
    public void testGet_4args() {
        System.out.println("Get");
        ArrayList<String[]> list_ip = null;
        ArrayList<String> oid_list = null;
        int port = 0;
        int timeout = 0;
        GetPool instance = new GetPool();
        Map expResult = null;
        Map result = instance.Get(list_ip, oid_list, port, timeout);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    /**
     * Тест метод Get, класса GetPool.
     */
    @Test(enabled = false)
    public void testGet_5args() {
        System.out.println("Get");
        ArrayList<String[]> list_ip = null;
        ArrayList<String> oid_list = null;
        int port = 0;
        int timeout = 0;
        int retries = 0;
        GetPool instance = new GetPool();
        Map expResult = null;
        Map result = instance.Get(list_ip, oid_list, port, timeout, retries);
        assertEquals(result, expResult);
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }
    


}
