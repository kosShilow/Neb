/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.kos.neb.neb_lib;

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
public class GetSnmpNGTest {
    
    public GetSnmpNGTest() {
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
     * Тест метод Get, класса GetSnmp.
     */
    @Test
    public void testGet_7args() throws Exception {
        System.out.println("Get");
        String node = "10.97.1.200";
        String community = "20fyufhf80";
        String oid = "1.3.6.1.2.1.1.5.0";
        int version = 2;
        int port = 161;
        int timeout = 1;
        int retries = 1;
        GetSnmp instance = new GetSnmp();
        String[] t = instance.Get(node, community, oid, version, port, timeout, retries);
        assertEquals(t[1], "CS2960c-infocom-1-207-adm-N1.severstal.com");
        version = 1;
        t = instance.Get(node, community, oid, version, port, timeout, retries);
        assertEquals(t[1], "CS2960c-infocom-1-207-adm-N1.severstal.com");
        version = 2;
        node = "10.98.120.201";
        t = instance.Get(node, community, oid, version, port, timeout, retries);
        assertNotEquals(t[1], "CS2960c-infocom-1-207-adm-N1.severstal.com");
        node = "10.97.1.200";
        oid = "2.3.6.1.2.1.1.5.0";
        t = instance.Get(node, community, oid, version, port, timeout, retries);
        assertNotEquals(t[1], "CS2960c-infocom-1-207-adm_N1.severstal.com");        
//        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    /**
     * Тест метод onResponse, класса GetSnmp.
     */
//    @Test
//    public void testOnResponse() {
//        System.out.println("onResponse");
//        ResponseEvent event = null;
//        GetSnmp instance = new GetSnmp();
//        instance.onResponse(event);
//        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
//        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
//    }
    
}
