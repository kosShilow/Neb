
package ru.kos.neb.neb_builder;

import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import static ru.kos.neb.neb_builder.Neb.map_file;
import static ru.kos.neb.neb_builder.Neb.map_file_pre;
import static ru.kos.neb.neb_builder.Neb.utils;

public class QueueWorker extends Thread {
    public static Map<String, ArrayList<String[]>> queue = new HashMap();
    public static boolean busy = false;
    public static long sleep_timeout = 10*1000;
 
    public QueueWorker() {

    } 
    
    @Override
    public void run() {
        while(true) {
//            System.out.println("Queue size = "+queue.size());
            Map<String, ArrayList<String[]>> queue_tmp = new HashMap();
            if(queue.size() > 0) {
                try {
                    Neb.logger.Println("QueueWorker = True", Neb.logger.DEBUG);
                    busy = true;
//                    System.out.println("Start queue worker ...");
                    for(Map.Entry<String, ArrayList<String[]>> entry : queue.entrySet()) {
                        String file = entry.getKey();
                        ArrayList<String[]> list_tmp = new ArrayList();
                        for(String[] key_val : entry.getValue()) {
                            String[] mas = new String[3];
                            mas[0] = key_val[0];
                            mas[1] = key_val[1];
                            mas[2] = key_val[2];
//                            System.out.println("command="+mas[0]+" - "+mas[1]+" - "+mas[2]);
                            list_tmp.add(mas);
                        }
                        queue_tmp.put(file, list_tmp);
                    }

                    synchronized (queue) { queue = new HashMap(); }
//                    System.out.println("Start set key ...");
                    for(Map.Entry<String, ArrayList<String[]>> entry : queue_tmp.entrySet()) {
                        String file = entry.getKey();
                        Map INFO = Neb.utils.ReadJSONFile(file);
                        Map<String, Map> node_attribute = utils.ReadJSONFile(Neb.node_attribute_old_file);
                        for(String[] key_val : entry.getValue()) {
                            String command = key_val[0];
                            String key = key_val[1];
                            String val = key_val[2];
                            if(command.equals("SET")) {
                                Neb.logger.Println("Set key: "+key+" val: "+val, Neb.logger.DEBUG);
                                if(!Neb.utils.SetValueToInfo(key, val, INFO)) {
//                                    System.out.println("Error set key: "+key+" val: "+val);
                                    Neb.logger.Println("Error set key: "+key+" val: "+val, Neb.logger.DEBUG);
                                }
                            } else if(command.equals("DELETE")) {
                                Neb.logger.Println("Delete key: "+key, Neb.logger.DEBUG);
                                if(file.equals(Neb.map_file) || file.equals(Neb.map_file_pre)) {
                                    String end_symbol = key.substring(key.length()-1);
                                    if(end_symbol.equals("/")) {
                                        key = key.substring(0, key.length()-1);
                                    }

                                    String[] mas = key.split("/");
                                    if(mas.length == 4 && mas[2].equals("nodes_information")) {
                                        String area = mas[1];
                                        String node = mas[3];
                                        if(INFO.get(area) != null && 
                                                ((Map)INFO.get(area)).get("nodes_information") != null && 
                                                ((Map)((Map)INFO.get(area)).get("nodes_information")).get(node) != null &&
                                                ((Map)((Map)((Map)INFO.get(area)).get("nodes_information")).get(node)).get("xy") != null
                                                ) {
                                            String image = (String)((Map)((Map)((Map)INFO.get(area)).get("nodes_information")).get(node)).get("image");
                                            ArrayList<String> xy = (ArrayList<String>)((Map)((Map)((Map)INFO.get(area)).get("nodes_information")).get(node)).get("xy");
                                            if(node_attribute.get(area) != null) {
                                                Map tmp_map = new HashMap();
                                                if(image != null) tmp_map.put("image", image);
                                                if(xy != null) tmp_map.put("xy", xy);
                                                node_attribute.get(area).put(node, tmp_map);
                                            } else {
                                                Map tmp_node_map = new HashMap();
                                                Map tmp_map = new HashMap();
                                                if(image != null) tmp_map.put("image", image);
                                                if(xy != null) tmp_map.put("xy", xy);
                                                tmp_node_map.put(node, tmp_map);
                                                node_attribute.put(area, tmp_node_map);
                                            }
                                        }
                                    }
                                }                                
                                if(!Neb.utils.DeleteKey(key, INFO)) {
//                                    System.out.println("Error delete key: "+key);
                                    Neb.logger.Println("Error delete key: "+key, Neb.logger.DEBUG);
                                }
                            } else if(command.equals("ADD_TO_LIST")) {
                                Neb.logger.Println("Adding to list: "+key+" value:"+val, Neb.logger.DEBUG);
                                if(!Neb.utils.AddToList(key, val, INFO)) {
//                                    System.out.println("Error adding to list: "+key+" value:"+val);
                                    Neb.logger.Println("Error adding to list: "+key+" value:"+val, Neb.logger.DEBUG);
                                }
                            } else if(command.equals("DEL_FROM_LIST")) {
                                Neb.logger.Println("Delete from list: "+key+" value:"+val, Neb.logger.DEBUG);
                                if(!Neb.utils.DelFromList(key, val, INFO)) {
//                                    System.out.println("Error delete from list: "+key+" value:"+val);
                                    Neb.logger.Println("Error delete from list: "+key+" value:"+val, Neb.logger.DEBUG);
                                }
                            }                             
                        }
                        Neb.logger.Println("Start write info to file: "+file+" ...", Neb.logger.DEBUG);
                        Neb.utils.MapToFile((Map)INFO, file);
                        utils.MapToFile((Map) node_attribute, Neb.node_attribute_old_file);
                      
                        Neb.logger.Println("Stop write info to file: "+file, Neb.logger.DEBUG);
                    }
//                    System.out.println("Stop queue worker.");
                } catch(ConcurrentModificationException ex) { System.out.println("ConcurrentModificationException"); }
                finally {
                    busy = false;
                    Neb.logger.Println("QueueWorker = Fasle", Neb.logger.DEBUG);
                }
            
            }
            try { Thread.sleep(sleep_timeout); } catch (InterruptedException e) { }
        }
    }
}
