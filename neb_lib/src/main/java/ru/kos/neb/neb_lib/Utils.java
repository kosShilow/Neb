/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.kos.neb.neb_lib;

import java.net.InetAddress;

/**
 *
 * @author kos
 */
public class Utils {
    public static boolean DEBUG = true;
    public static boolean DEBUG_SCRIPT = false;
    
    public static String LOG_FILE = "neb_lib.log";

    public long[] IntervalNetworkAddress(String network) {
        String[] network_mask = new String[2];
        int net;
        int mask;
        long[] interval = new long[2];

        try {
            if (network.indexOf('/') != -1) {
                network_mask = network.split("/");
                net = InetAddress.getByName(network_mask[0].trim()).hashCode();
                int num_mask = Integer.parseInt(network_mask[1].trim());
                mask = 0xffffffff;
                mask = mask << (32 - num_mask);
                interval[0] = Long.valueOf(Integer.toBinaryString((net & mask)), 2);
                interval[1] = Long.valueOf(Integer.toBinaryString((net | ~mask)), 2);                
            } else if (network.indexOf(' ') != -1) {
                network_mask = network.split(" ");
                net = InetAddress.getByName(network_mask[0].trim()).hashCode();
                mask = InetAddress.getByName(network_mask[1].trim()).hashCode();
                interval[0] = Long.valueOf(Integer.toBinaryString((net & mask)), 2);
                interval[1] = Long.valueOf(Integer.toBinaryString((net | ~mask)), 2);                   
            } else {
                interval[0] = InetAddress.getByName(network.trim()).hashCode();
                interval[1] = InetAddress.getByName(network.trim()).hashCode();
            }
            return interval;
        } catch (java.net.UnknownHostException | java.lang.NumberFormatException e) {
            interval[0] = 0;
            interval[1] = 0;
            return interval;
        }
    }

    public String NetworkToIPAddress(long addr) {
        long[] octets = new long[4];
        long tmp;
        String ip;

        octets[0] = addr / (256 * 256 * 256);
        tmp = addr % (256 * 256 * 256);
        octets[1] = tmp / (256 * 256);
        tmp = tmp % (256 * 256);
        octets[2] = tmp / 256;
        octets[3] = tmp % 256;
        ip = Long.toString(octets[0]) + "."
                + Long.toString(octets[1]) + "."
                + Long.toString(octets[2]) + "."
                + Long.toString(octets[3]);
        return ip;
    }

    public boolean InsideInterval(String ip, String network)
    {
        try
        {
            int addr = java.net.InetAddress.getByName(ip).hashCode();
            
            if(network.indexOf('/') != -1)
            {
                String[] network_mask = network.split("/");
                int net = InetAddress.getByName(network_mask[0]).hashCode();
                int num_mask = Integer.parseInt(network_mask[1]);
                int mask = 0xffffffff;
                mask = mask << (32-num_mask);
                int net_start = net & mask;
                int net_stop = net | ~mask; 
                if(addr >= net_start && addr <= net_stop) return true;
            }
            else if(network.indexOf(' ') != -1) {
                String[] network_mask = network.split(" ");
                int net = InetAddress.getByName(network_mask[0]).hashCode();
                int mask = InetAddress.getByName(network_mask[1]).hashCode();  
                int net_start = net & mask;
                int net_stop = net | ~mask; 
                if(addr >= net_start && addr <= net_stop) return true;
            }
            else
            {
                if(network.equals(ip)) return true;
            }
 
            return false;
        }
        catch(java.net.UnknownHostException e)
        { return false; }
    }
    
}
