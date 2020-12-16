/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.kos.neb.neb_lib;

import java.io.File;
import java.util.ArrayList;
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
public class BulkSnmpNGTest {
    
    public BulkSnmpNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        File file = new File(Utils.LOG_FILE); 
        if(file.exists())
            file.delete();        
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
     * Тест метод Get, класса BulkSnmp.
     */
    @Test(enabled = true)
    public void testGet_7args() throws Exception {
        System.out.println("BulkSnmp.Get");
        String node = "10.97.1.200";
        String community = "20fyufhf80";
        String oid = "1.3.6.1.2.1.2.2.1.2";
        int version = 1;
        int port = 161;
        int timeout = 1;
        int retries = 3;
//        WalkSnmp instance = new WalkSnmp();
//        ArrayList expResult = null;
        ArrayList result = new BulkSnmp().Get(node, community, oid, "2", port, timeout, retries);
        assertTrue(result.size() > 0);
        result = new BulkSnmp().Get(node, community, oid, "2", port, timeout, retries);
        assertTrue(result.size() > 0);   
        node = "10.98.120.200";
        result = new BulkSnmp().Get(node, community, oid, "2", port, timeout, retries);
        assertTrue(result.isEmpty());        
        node = "10.97.1.200";
        community = "public1";
        result = new BulkSnmp().Get(node, community, oid, "2", port, timeout, retries);
        assertTrue(result.isEmpty());     
        community = "20fyufhf80";
        oid = "2.3.6.1.2.1.2.2.1.2";
        result = new BulkSnmp().Get(node, community, oid, "2", port, timeout, retries);
        assertTrue(result.isEmpty());                   
        // Просмотр списка задач TODO для сгенерированного кода теста и удаление вызова по умолчанию для случаев сбоя.
//        fail("\u042d\u0442\u043e\u0442 \u0442\u0435\u0441\u0442 \u044f\u0432\u043b\u044f\u0435\u0442\u0441\u044f \u043f\u0440\u043e\u0442\u043e\u0442\u0438\u043f\u043e\u043c.");
    }

    
}
