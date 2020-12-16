/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author kos
 */
public class Logger {
    public int CRITICAL = 1;
    public int ERROR = 2;
    public int WARNING = 3;
    public int INFO = 5;
    public int DEBUG = 10;
    private int level = 5;
    private BufferedWriter descLog = null;
    
    public Logger(String logfile) {
//        Clear(logfile);
        try {
            descLog = new BufferedWriter(new FileWriter(logfile, true));
        } catch (Exception ex) {
            System.out.println(Logger.class.getName()+"\t"+ex);
        }
    }
    
    public void Clear(String logfile) {
        if(this.descLog != null) try {
            this.descLog.close();
        } catch (Exception ex) {  }        

        try {
            descLog = new BufferedWriter(new FileWriter(logfile));
            descLog.write("");
        } catch (IOException ex) {
            System.out.println(Logger.class.getName()+"\t"+ex);
        }
    }
    
    public void SetLevel(int level) {
        this.level=level;
    }
    
    synchronized public int Println(String msg, int level) {
        int result = 0;
        if(level <= this.level) {
            if(level == this.INFO) System.out.println(msg);
            try {
                this.descLog.write(new Date()+":\t"+msg+"\r\n");
                this.descLog.flush();
                result=msg.length();
            } catch (IOException ex) {
                System.out.println(Logger.class.getName()+"\t"+ex);
            }
        }
        return result;
    }
    
    synchronized public int Print(String msg, int level) {
        int result = 0;
        if(level <= this.level) {
            if(level == this.INFO) System.out.print(msg);
            try {
                this.descLog.write(msg);
                this.descLog.flush();
                result=msg.length();
            } catch (IOException ex) {
                System.out.println(Logger.class.getName()+"\t"+ex);
            }
        }
        return result;
    }    
    
    @Override
    protected void finalize ( ) {
        if(this.descLog != null) try {
            this.descLog.close();
        } catch (Exception ex) {  }           
    }
}
