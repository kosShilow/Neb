package ru.kos.neb.neb_builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import static ru.kos.neb.neb_builder.Neb.logger;


public class GraphLayout {
    private static final int loop_cicle = 100;
    private static final double width = 1000;
    private static final double hight = 1000;
    
    private static final String neb_server = "localhost";
//    private static final String neb_server = "10.120.63.3";
    private static final String neb_server_port = "8080";
    private static final String map_file = "neb.map.pre"; 
    
    public static String user = "noc";
    public static String passwd = "1qaz2wsx"; 
    
    private static boolean PROCESSING_ALL_NODES_LAYOUT = false;
    
    private static Map INFORMATION = new HashMap<>(); 

    private ArrayList GetCenterLinkedToNode(String node, ArrayList<String[]> nodes_info, ArrayList<ArrayList<String>> links_info) {
        ArrayList result = new ArrayList();
        
        for(ArrayList<String> link : links_info) {
            String neighbor_name="";
            if(link.get(0).equals(node)) neighbor_name=link.get(3);
            if(link.get(3).equals(node)) neighbor_name=link.get(0);
            if(!neighbor_name.equals("")) {
                boolean found=false;
                for(String[] item1 : nodes_info) {
                    if(item1[0].equals(neighbor_name)) {
                        double[] center = new double[2];
                        center[0]=Double.valueOf(item1[1])+Double.valueOf(width)/2;
                        center[1]=Double.valueOf(item1[2])-Double.valueOf(hight)/2;
                        result.add(center);
                        found=true;
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    private double[] GetCenter(double x, double y, double width, double hight) {
        double[] result = new double[2];
        result[0] = x+width/2;
        result[1] = y-hight/2;      
        return result;
    }
    
    private double[] GetAttraction(String[] node_info, ArrayList<String[]> nodes_info, ArrayList<ArrayList<String>> links_info, double koefficient)
    {
        double[] attraction = new double[2];
//        double koefficient = 0.001;
        double forceX=0.; double forceY=0.;
        
        double x=Double.valueOf(node_info[1]);
        double y=Double.valueOf(node_info[2]);

        double[] center=GetCenter(x, y, width, hight);
        ArrayList coord_linked_nodes = GetCenterLinkedToNode(node_info[0], nodes_info, links_info);
        for(int i=0; i<coord_linked_nodes.size(); i++) {
            double[] center_neighbor = (double[]) coord_linked_nodes.get(i);
            double distance = center_neighbor[0]-center[0];
                double force = koefficient * distance;
                forceX=forceX+force;
            distance = center_neighbor[1]-center[1];
                force = koefficient * distance;            
                forceY=forceY+force;
        }
        attraction[0]=forceX; attraction[1]=forceY;
        return attraction;
    }
    
    private double[] GetRepulsion(String[] node_info, ArrayList<String[]> nodes_info, double electrical_repulsion)
    {
        double[] repulsion = new double[2];
        double forceX=0.; double forceY=0.;
        
        double x=Double.valueOf(node_info[1]);
        double y=Double.valueOf(node_info[2]);

        double[] center=GetCenter(x, y, width, hight);
        for(String[] item : nodes_info)
        {
            if(item[0].equals(node_info[0])) continue;
            double x1=Double.valueOf(item[1]);
            double y1=Double.valueOf(item[2]);
            double[] center_sec=GetCenter(x1, y1, width, hight);            

            double deltaX = (center[0]-center_sec[0]);
            double deltaY = (center[1]-center_sec[1]);
            
            if(deltaX != 0) {
                double force = electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
                double xforce=force*deltaX/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                forceX=forceX+xforce;
            }
            if(deltaY != 0) {
                double force = electrical_repulsion/(deltaX*deltaX+deltaY*deltaY);
                double yforce=force*deltaY/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                forceY=forceY+yforce;
            }

        }
        repulsion[0]=forceX; repulsion[1]=forceY;

        return repulsion;
    }
    
    private double[] MagicWind(String[] node_info, ArrayList<String[]> center_nodes, double wind_force)
    {
        double[] wind = new double[2];
        double forceX=0.; double forceY=0.;
        
        double x=Double.valueOf(node_info[1]);
        double y=Double.valueOf(node_info[2]);

        double[] center=GetCenter(x, y, width, hight);
        for(String[] item : center_nodes)
        {
            if(item[0].equals(node_info[0])) continue;
            double x1=Double.valueOf(item[1]);
            double y1=Double.valueOf(item[2]);
            double[] center_sec=GetCenter(x1, y1, width, hight);            

            double deltaX = (center[0]-center_sec[0]);
            double deltaY = (center[1]-center_sec[1]);
            
            if(deltaX != 0) {
                double force = wind_force;
                double xforce=force*deltaX/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                forceX=forceX+xforce;
            }
            if(deltaY != 0) {
                double force = wind_force;
                double yforce=force*deltaY/Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                forceY=forceY+yforce;
            }

        }
        wind[0]=forceX; wind[1]=forceY;

        return wind;
    } 
    
    private ArrayList<String[]> GraphLayuot(ArrayList<String[]> processing_nodes_position, 
            ArrayList<String[]> processing_nodes, ArrayList<ArrayList<String>> processing_links,
            ArrayList<String[]> center_nodes_list) {
        double increment = 0.5;
        for(int loop=0; loop<loop_cicle; loop++) {
//            System.out.println("\tloop="+loop);
            for(String[] node_position : processing_nodes_position) {
                double[] attraction = new double[2];
                double koef = 0.1;
                for(int ii=0; ii<10; ii++) {
                    attraction = GetAttraction(node_position, processing_nodes, processing_links, koef);
                    if(Math.abs(attraction[0]) > 1000000. || Math.abs(attraction[1]) > 1000000.) koef=koef/10;
                    else break;
                }
                double[] repulsion = new double[2];
                double koef1 = 10000000000.;
                for(int ii=0; ii<10; ii++) {
                    repulsion = GetRepulsion(node_position, processing_nodes, koef1);
                    if(Math.abs(repulsion[0]) > 1000000. || Math.abs(repulsion[1]) > 1000000.) koef1=koef1/10;
                    else break;
                }
                double[] wind = new double[2];
                double koef2 = 1000.;
                for(int ii=0; ii<10; ii++) {
                    wind = MagicWind(node_position, center_nodes_list, koef2);
                    if(Math.abs(repulsion[0]) > 1000000. || Math.abs(repulsion[1]) > 1000000.) koef2=koef2/10;
                    else break;
                }                    

                double xattr=attraction[0]; double yattr=attraction[1];
                double xrepuls=repulsion[0]; double yrepuls=repulsion[1];
                double xwind=wind[0]; double ywind=wind[1];
                double xforce=xattr + xrepuls + xwind;
                double yforce=yattr + yrepuls + ywind;
                double x_delta=xforce*increment; double y_delta=yforce*increment;

                String x = String.valueOf(Double.valueOf(node_position[1])+x_delta);
                String y = String.valueOf(Double.valueOf(node_position[2])+y_delta);
                if(!(Math.abs(Double.valueOf(x)) > 1000000.|| Math.abs(Double.valueOf(y)) > 1000000.)) {
                    node_position[1] = x;
                    node_position[2] = y;
                }
            }
//                System.out.println("loop="+loop);
        }
        return processing_nodes_position;
    }
    
    private ArrayList<String[]> RunGraphLayout(Map area_info, ArrayList<String[]> center_nodes_list) {
        ArrayList<String[]> nodes_position = new ArrayList();
        Map<String, String[]> nodes_position_map = new HashMap();
        Map<String, String> static_nodes = new HashMap();
        
        if(area_info.get("nodes_information") != null && area_info.get("links") != null) {
            
            Map<String, Map> nodes_info = (Map)area_info.get("nodes_information");
            ArrayList<ArrayList<String>> links = (ArrayList)area_info.get("links");
            
            if(!PROCESSING_ALL_NODES_LAYOUT) {
                for(Map.Entry<String, Map> item : nodes_info.entrySet()) {
                    String node = item.getKey();
                    Map val = item.getValue();
                    if(val.get("xy") != null) {
                        static_nodes.put(node, node);
                    } else {
                        logger.Println("\tprocessing node layout: "+node, logger.DEBUG);
                    }                 
                    
                }
            }
            
            Map<String, ArrayList<String>> links_map = new HashMap();
            for(ArrayList<String> item : links) {
                if(links_map.get(item.get(0)) == null) {
                    ArrayList<String> list_tmp = new ArrayList();
                    list_tmp.add(item.get(3));
                    links_map.put(item.get(0), list_tmp);
                } else {
                    links_map.get(item.get(0)).add(item.get(3));
                }
                if(links_map.get(item.get(3)) == null) {
                    ArrayList<String> list_tmp = new ArrayList();
                    list_tmp.add(item.get(0));
                    links_map.put(item.get(3), list_tmp);
                } else {
                    links_map.get(item.get(3)).add(item.get(0));
                }                
            }

            int iter = 0;
            while(true) {
//                System.out.println("Iteration: "+iter);
                ArrayList<String[]> processing_nodes_position = new ArrayList();
                ArrayList<String[]> processing_nodes = new ArrayList();
                Map<String, String> processing_nodes_map = new HashMap();
                
                for(Map.Entry<String, String> item : static_nodes.entrySet()) {
                    String static_node = item.getKey();
                    Map val1 = nodes_info.get(static_node);
                    if(val1 != null) {
                        if(nodes_position_map.get(static_node) != null) {
                            String[] mas_tmp = new String[3];
                            mas_tmp[0] = static_node;
                            mas_tmp[1] = nodes_position_map.get(static_node)[1];
                            mas_tmp[2] = nodes_position_map.get(static_node)[2];
                            processing_nodes.add(mas_tmp);
                            processing_nodes_map.put(static_node, static_node);                            
                        } else{
                            if(val1.get("xy") == null) {
                                double x = (double) (5000. * new Random().nextDouble());
                                double y = (double) (5000. * new Random().nextDouble());
                                String[] mas_tmp = new String[3];
                                mas_tmp[0] = static_node;
                                mas_tmp[1] = String.valueOf(x);
                                mas_tmp[2] = String.valueOf(y);
                                processing_nodes.add(mas_tmp);
                                processing_nodes_map.put(static_node, static_node);
//                                nodes_position_map.put(mas_tmp[0], mas_tmp);
                            } else {
                                String x = (String)((ArrayList)val1.get("xy")).get(0);
                                String y = (String)((ArrayList)val1.get("xy")).get(1);
                                String[] mas_tmp = new String[3];
                                mas_tmp[0] = static_node;
                                mas_tmp[1] = x;
                                mas_tmp[2] = y;
                                processing_nodes.add(mas_tmp);
                                processing_nodes_map.put(static_node, static_node);
                            }
                        }
                    }
                }

                for(Map.Entry<String, String> item : static_nodes.entrySet()) {
                    String static_node = item.getKey();
                    String x_static = null;
                    String y_static = null;
                    if(nodes_position_map.get(static_node) != null) {
                        x_static = nodes_position_map.get(static_node)[1];
                        y_static = nodes_position_map.get(static_node)[2];
                    }
                    if(links_map.get(static_node) != null) {
                        ArrayList<String> neighbor_nodes = links_map.get(static_node);
                        for(String node : neighbor_nodes) {
//                            if(node.equals("10.96.115.65"))
//                                System.out.println("11111");
                            if(static_nodes.get(node) == null && nodes_info.get(node) != null) {
                                Map val = nodes_info.get(node);
                                if(val != null) {
                                    if(x_static != null && y_static != null) {
                                        double x = (double) (1000. * new Random().nextDouble()) + Double.parseDouble(x_static);
                                        double y = (double) (1000. * new Random().nextDouble()) + Double.parseDouble(y_static);
                                        String[] mas_tmp = new String[3];
                                        mas_tmp[0] = node;
                                        mas_tmp[1] = String.valueOf(x);
                                        mas_tmp[2] = String.valueOf(y);
                                        boolean find = false;
                                        for(String[] item1 : processing_nodes_position) {
                                            if(item1[0].equals(node)) {
                                                find = true;
                                                break;
                                            }
                                        }
                                        if(!find) {
                                            processing_nodes_position.add(mas_tmp);
                                            processing_nodes.add(mas_tmp);
                                            processing_nodes_map.put(node, node); 
                                        }
                                    } else {
                                        if(val.get("xy") == null) {
                                            double x = (double) (5000. * new Random().nextDouble());
                                            double y = (double) (5000. * new Random().nextDouble());
                                            String[] mas_tmp = new String[3];
                                            mas_tmp[0] = node;
                                            mas_tmp[1] = String.valueOf(x);
                                            mas_tmp[2] = String.valueOf(y);
                                            boolean find = false;
                                            for(String[] item1 : processing_nodes_position) {
                                                if(item1[0].equals(node)) {
                                                    find = true;
                                                    break;
                                                }
                                            }         
                                            if(!find) {
                                                processing_nodes_position.add(mas_tmp);
                                                processing_nodes.add(mas_tmp);
                                                processing_nodes_map.put(node, node);
                                            }
                                        } else {
                                            String x = (String)((ArrayList)val.get("xy")).get(0);
                                            String y = (String)((ArrayList)val.get("xy")).get(1);
                                            String[] mas_tmp = new String[3];
                                            mas_tmp[0] = node;
                                            mas_tmp[1] = x;
                                            mas_tmp[2] = y;
                                            boolean find = false;
                                            for(String[] item1 : processing_nodes_position) {
                                                if(item1[0].equals(node)) {
                                                    find = true;
                                                    break;
                                                }
                                            }   
                                            if(!find) {
                                                processing_nodes_position.add(mas_tmp);
                                                processing_nodes.add(mas_tmp);
                                                processing_nodes_map.put(node, node);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } 
                
                if(processing_nodes_position.size() == 0) break;

                ArrayList<ArrayList<String>> processing_links = new ArrayList();
                for(ArrayList<String> item : links) {
                    if(processing_nodes_map.get(item.get(0)) != null && processing_nodes_map.get(item.get(3)) != null) {
                        processing_links.add(item);
                    }
                }

                processing_nodes_position = GraphLayuot(processing_nodes_position, processing_nodes, processing_links, center_nodes_list);
                
                for(String[] item : processing_nodes) {
                    if(static_nodes.get(item[0]) == null)
                        static_nodes.put(item[0], item[0]);
                }
                for(String[] node_position : processing_nodes_position) {
                    nodes_position.add(node_position);
                    nodes_position_map.put(node_position[0], node_position);
                }

                iter = iter + 1;
            }
        }
        return nodes_position;
    }
    
    public Map StartGraphLayout(Map INFORMATION) {
        Map<String, String> static_nodes = new HashMap();
        
        Map<String, Map<String, String>> center_nodes = new HashMap();
        Map<String, String> center_nodes_area_chermk = new HashMap();
        center_nodes_area_chermk.put("10.96.115.200", "");
        center_nodes_area_chermk.put("10.96.115.201", "");
        center_nodes.put("area_chermk", center_nodes_area_chermk);
      
        Map<String, String> center_nodes_area_orel = new HashMap();
        center_nodes_area_orel.put("10.64.3.254", "");
        center_nodes.put("area_orel", center_nodes_area_orel);        
        
        Map<String, String> center_nodes_area_kostomuksha = new HashMap();
        center_nodes_area_kostomuksha.put("10.32.10.254", "");
        center_nodes.put("area_kostomuksha", center_nodes_area_kostomuksha); 

        Map<String, String> center_nodes_area_vorkuta = new HashMap();
        center_nodes_area_kostomuksha.put("10.36.2.254", "");
        center_nodes.put("area_vorkuta", center_nodes_area_kostomuksha);        
         
        for(Map.Entry<String, Map> entry : ((Map<String, Map>)INFORMATION).entrySet()) {
            String area = entry.getKey();
            logger.Println("Graph layout: "+area, logger.DEBUG);
            System.out.println("Graph layout: "+area);
            Map area_info = entry.getValue();
            ArrayList<String[]> center_nodes_list = new ArrayList();
            if(area_info.get("nodes_information") != null) {
                Map<String, Map> nodes_info = (Map)area_info.get("nodes_information");
                if(center_nodes.get(area) != null) {
                    for(Map.Entry<String, String> item : center_nodes.get(area).entrySet()) {
                        String center_node = item.getKey();
                        if(nodes_info.get(center_node) != null && nodes_info.get(center_node).get("xy") != null) {
                            String x = (String)((ArrayList)nodes_info.get(center_node).get("xy")).get(0);
                            String y = (String)((ArrayList)nodes_info.get(center_node).get("xy")).get(1);
                            String[] mas_tmp = new String[3];
                            mas_tmp[0] = center_node;
                            mas_tmp[1] = x;
                            mas_tmp[2] = y;
                            center_nodes_list.add(mas_tmp);                                    
                        }
                    }
                }
            }
            ArrayList<String[]> nodes_position = RunGraphLayout(area_info, center_nodes_list);
            
            for(String[] item : nodes_position) {
                if(area_info.get("nodes_information") != null && ((Map)area_info.get("nodes_information")).get(item[0]) != null) {
                    String[] coord = new String[2];
                    coord[0] = item[1];
                    coord[1] = item[2];
                    ((Map)((Map)area_info.get("nodes_information")).get(item[0])).put("xy", coord);
                }
            }
        }
        return INFORMATION;
    }    
}

