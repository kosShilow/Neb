package ru.kos.neb_viewer;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.util.ArrayList;
import java.net.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.io.*;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.URL;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.swing.tree.*;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.PLayer;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.nodes.PImage;
import org.piccolo2d.util.PBounds;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static ru.kos.neb_viewer.CheckerMapFile.time_start;
import org.json.simple.*;
import org.json.simple.parser.*;
import com.google.gson.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import static ru.kos.neb_viewer.GetNodePropertiesForm.ip;

@SuppressWarnings({"serial", "unchecked"})

/**
 *
 * @author kos
 */
public class Utils {

    public final static String jpeg = "jpeg";
    public final static String jpg = "jpg";
    public final static String gif = "gif";
    public final static String tiff = "tiff";
    public final static String tif = "tif";
    public final static String png = "png";

    /**
     * Creates a new instance of Utils
     */
    public Utils() {
    }

    public String GetHomePath() {
        String result = "";
        String path = "";
        try {
            path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            String[] mas = path.split("/");
            for (int j = 1; j < mas.length - 1; j++) {
                result = result + mas[j] + "/";
            }
        } catch (Exception ex) {
            System.out.println(Utils.class.getName() + " - " + ex);
            System.exit(1);
        }
        return result;
    }

    public ArrayList GetReadXMLWithKey(org.w3c.dom.Document doc, String key, boolean replace_blank) throws NullPointerException {
        ArrayList result = new ArrayList();

        try {
            NodeList list = doc.getElementsByTagName(key).item(0).getChildNodes();
            if (list.getLength() > 1) {
                for (int i = 0; i < list.getLength(); i++) {
                    Node item = list.item(i);
                    if (item.getNodeType() == 1) {
                        if (replace_blank) {
                            result.add(item.getTextContent().replace("\n", "").replace(" ", ""));
                        } else {
                            result.add(item.getTextContent().replace("\n", ""));
                        }
                    }
                }
            } else if (replace_blank) {
                result.add(doc.getElementsByTagName(key).item(0).getTextContent().replace("\n", "").replace(" ", ""));
            } else {
                result.add(doc.getElementsByTagName(key).item(0).getTextContent().replace("\n", ""));
            }
        } catch (NullPointerException ex) {
            System.out.println("Bad parameter " + key + " in configuration fule.");
            System.exit(1);
        }
        return result;
    }

    public Map ReadConfig(String filename) {
        Map result = new HashMap<>();
        JSONParser parser = new JSONParser();
        String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/getfile?file=" + filename;

        try {
            String res = HTTPRequestGET(url);
            /* Get the file content into the JSONObject */
            JSONObject jsonObject = (JSONObject) parser.parse(res);
            result = toMap(jsonObject);

        } catch (Exception ex) {
            System.out.println("Error request: " + url);
            System.exit(1);
        }

        return result;
    }

    private boolean CopyHTTPFile(String url, String save_file) {
        try {
            System.out.println("Get file: " + save_file);

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("user", Main.user);
            con.setRequestProperty("passwd", Main.passwd); 
            InputStream in = (InputStream)con.getInputStream(); 
//            InputStream in = new URL(url).openStream();
            File img_file = new File(save_file);
            String dir = img_file.getParent();
            File theDir = new File(dir);
            theDir.mkdirs();
            Files.copy(in, Paths.get(save_file), StandardCopyOption.REPLACE_EXISTING);
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(Utils.class.getName() + " - " +ex);
            return false;
        }
    }

    private boolean CopyHTTPSFile(String url_str, String save_file) {
        try {
            System.out.println("Get file: " + save_file);
            
            DisableSslVerification();
            URL url = new URL(url_str);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("user", Main.user);
            con.setRequestProperty("passwd", Main.passwd);
            con.setRequestMethod("GET");
//            int status = con.getResponseCode();
            InputStream in = con.getInputStream();
            
//            InputStream in = new URL(url).openStream();           
            File img_file = new File(save_file);
            String dir = img_file.getParent();
            File theDir = new File(dir);
            theDir.mkdirs();
            Files.copy(in, Paths.get(save_file), StandardCopyOption.REPLACE_EXISTING);
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex);
            return false;
        }
    }

    public boolean GetImagesFiles(String directory, String dst_path) {
        try {
            String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/getfiles_list?directory=" + directory;
            String res = HTTPRequestGET(url);
            if (!res.equals("")) {
                for (String file : res.split("\n")) {
                    file = file.replace("\\", "/");
                    //                System.out.println(file);

                    String url2 = "http://"+Main.neb_server+":"+Main.neb_server_port+"/getfile?file="+file;
                    File img_file = new File(dst_path+"/"+file);
                    if (!img_file.exists()) {
                        CopyHTTPFile(url2, img_file.getPath());
                    } else {
                        String url1 = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/getfile_attributes?file=" + file;
                        String res1 = HTTPRequestGET(url1);
                        JSONParser parser = new JSONParser();
                        JSONObject jsonObject = (JSONObject) parser.parse(res1);
                        Map file_attributes_map = toMap(jsonObject);
                        long size_server = 0;
                        String size = (String) file_attributes_map.get("size");
                        if (size != null) {
                            size_server = Long.valueOf(size);
                        }
                        long size_local = img_file.length();
                        if (size_server != size_local) {
                            CopyHTTPFile(url, img_file.getPath());
                        }
                    }
                }
            } else {
                return false;
            }

        } catch (Exception ex) {
            System.out.println(Utils.class.getName() + " - " + ex);
            return false;
        }

        return true;
    }

    public String GetAccess() {
        String out = "";
        try {
            String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/status";
            out = HTTPRequestGET(url);
        } catch (Exception ex) {
            System.out.println(Utils.class.getName() + " - " + ex);
        }

        return out;
    }

    private Map<String, String[]> CreateHash(String information) {
        Map<String, String[]> result = new HashMap<>();
        if (!information.equals("") && !information.equals("null")) {
            String[] ports = information.toString().split("\\|");
            for (String port : ports) {
                String[] fields = port.split(",", -1);
                result.put(fields[1], fields);

            }
        }
        return (result);
    }

    static public ArrayList CreateMatrixVlans(String vlansInformation) {
        ArrayList out = new ArrayList();
        String[] vlans = vlansInformation.toString().split("\\|");
        String[][] matrix = new String[vlans.length][16];
        for (int i = 0; i < vlans.length; i++) {
            String[] fields = vlans[i].split(",");
            if (fields.length <= 16) {
                for (int j = 0; j < fields.length; j++) {
                    matrix[i][j] = fields[j];
                    //                if(j>=1) break;
                }
            }
        }
        out.add(matrix);
        return (out);
    }

    static private int FindMasiv(String str, String[] masiv) {
        int i = 0;
        boolean find = false;
        for (i = 0; i < masiv.length; i++) {
            if (masiv[i].equals(str)) {
                find = true;
                break;
            }
        }
//        String[] out;
        if (find) {
            return i;
        } else {
            return -1;
        }
    }

    static public String[] AddMasiv(String str, String[] masiv) {
        int i = 0;
        int res = FindMasiv(str, masiv);

        String[] out;
        if (res >= 0) {
            out = new String[masiv.length];
        } else {
            out = new String[masiv.length + 1];
        }

        for (i = 0; i < masiv.length; i++) {
            out[i] = masiv[i];
        }
        if (res < 0) {
            out[i] = str;
        }

        return out;
    }

    static private ArrayList FindMasiv(ArrayList array, String row_finding, String column_finding) {
        ArrayList out = new ArrayList();
        String[] row_name;
        String[] column_name;
        String[][] matrix;

        row_name = (String[]) array.get(0);
        column_name = (String[]) array.get(1);
        matrix = (String[][]) array.get(2);

        int row = 0;
        boolean find1 = false;
        for (row = 0; row < row_name.length; row++) {
            if (row_name[row].equals(row_finding)) {
                find1 = true;
                break;
            }
        }
        int col = 0;
        boolean find2 = false;
        for (col = 0; col < column_name.length; row++) {
            if (column_name[col].equals(column_finding)) {
                find2 = true;
                break;
            }
        }
        if (find1 && find2) {
            out.add(row);
            out.add(col);
            out.add(matrix[row][col]);
            return out;
        } else {
            return null;
        }
    }

    static public ArrayList AssociationMatrix(ArrayList array1, ArrayList array2, String interf1, String interf2) {
        ArrayList out = new ArrayList();
        String[] row_name1;
        String[] column_name1;
        String[] column_name_name1;
        String[][] matrix1;
        String[] row_name2;
        String[] column_name2;
        String[] column_name_name2;
        String[][] matrix2;

        row_name1 = (String[]) array1.get(0);
        column_name1 = (String[]) array1.get(1);
        column_name_name1 = (String[]) array1.get(2);
        matrix1 = (String[][]) array1.get(3);
        row_name2 = (String[]) array2.get(0);
        column_name2 = (String[]) array2.get(1);
        column_name_name2 = (String[]) array2.get(2);
        matrix2 = (String[][]) array2.get(3);
        // count number row and column for new row
        int col = 0;
        for (int i = 0; i < column_name1.length; i++) {
            if (FindMasiv(column_name1[i], column_name2) >= 0) {
                col++;
            }
        }
        // fill 
        String[] row_result = new String[col];
        String[] matrix_result = new String[col];
        int num = 0;
        for (int i = 0; i < column_name1.length; i++) {
            if (FindMasiv(column_name1[i], column_name2) >= 0) {
                row_result[num] = column_name1[i];
                num++;
            }
        }
        // find row from matrix1
        int num1 = FindMasiv(interf1, row_name1);
        // find row from matrix2
        int num2 = FindMasiv(interf2, row_name2);
        // fill matrix_result
        if (num1 >= 0 && num2 >= 0) {
            ArrayList result_list = new ArrayList();
            for (int i = 0; i < row_result.length; i++) {
                int number1 = FindMasiv(row_result[i], column_name1);
                int number2 = FindMasiv(row_result[i], column_name2);
                if (number1 >= 0 && number2 >= 0) {
                    String[] str = new String[2];
                    str[0] = "";
                    str[1] = "";
                    str[0] = matrix1[num1][number1];
                    str[1] = matrix2[num2][number2];
                    if (str[0].equals("trunk")) {
                        str[0] = "tagged";
                    }
                    if (str[1].equals("trunk")) {
                        str[1] = "tagged";
                    }
                    if (str[0].equals("native-trunk")) {
                        str[0] = "untagged";
                    }
                    if (str[0].equals("native")) {
                        str[0] = "untagged";
                    }
                    if (str[0].equals("access")) {
                        str[0] = "untagged";
                    }
                    if (str[1].equals("native-trunk")) {
                        str[1] = "untagged";
                    }
                    if (str[1].equals("native")) {
                        str[1] = "untagged";
                    }
                    if (str[1].equals("access")) {
                        str[1] = "untagged";
                    }
                    result_list.add(str);
                }
            }
            // is symmetric untagged
            boolean symmetric_untagged = false;
            for (int i = 0; i < result_list.size(); i++) {
                String[] str = (String[]) result_list.get(i);
                if (str[0].equals("untagged")) {
                    for (int j = 0; j < result_list.size(); j++) {
                        String[] str1 = (String[]) result_list.get(j);
                        if (str1[1].equals("untagged")) {
                            symmetric_untagged = true;
                            break;
                        }
                    }
                }
                if (symmetric_untagged) {
                    break;
                }
            }

            for (int i = 0; i < result_list.size(); i++) {
                String[] str = (String[]) result_list.get(i);
                if (str[0].equals("tagged") && str[1].equals("tagged")) {
                    matrix_result[i] = "tagged";
                } else if ((str[0].equals("untagged") || str[1].equals("untagged")) && symmetric_untagged) {
                    matrix_result[i] = "untagged";
                } else {
                    matrix_result[i] = "";
                }
            }
            out.add(row_result);
            out.add(matrix_result);
        } else {
            out = null;
        }
        return out;
    }

    static public double[][] GraphLayer(double with_canvas, double hight_canvas) {
        double timestep = .001;
        double damping = 1.;
        double mass = .000001;
        int numnodes = Main.nodeLayer.getChildrenCount();
        double[][] positions = new double[numnodes][3];
        String[] node_name = new String[numnodes];
        Random rnd = new Random();
        // set random positions
        for (int i = 0; i < numnodes; i++) {
            PNode node = Main.nodeLayer.getChild(i);
            node_name[i] = (String) node.getAttribute("tooltip");
            positions[i][0] = (float) (with_canvas * rnd.nextDouble());
            positions[i][1] = (float) (hight_canvas * rnd.nextDouble());
        }
        // set velocities
        double[][] velocities = new double[numnodes][2];

        int num = 0;
        while (true) {
            double total_kinetic_energy = 0;
            for (int i = 0; i < numnodes; i++) {
                PNode node = Main.nodeLayer.getChild(i);
                double[] force = new double[2];
                double[] force1 = new double[2];
                for (int j = 0; j < numnodes; j++) {
                    if (i == j) {
                        continue;
                    }
                    double[] repulsion = new double[2];
                    repulsion = Repulsion(i, j, positions);
                    force[0] = force[0] - repulsion[0];
                    force[1] = force[1] - repulsion[1];
                }
                if (i == 0) {
                    System.out.println("repulsion force[0] = " + force[0] + "\tforce[1] = " + force[1]);
                }
                String tooltipString = (String) node.getAttribute("tooltip");
                ArrayList edges = (ArrayList) node.getAttribute("edges");
                for (int j = 0; j < edges.size(); j++) {
                    PPath edge = (PPath) edges.get(j);
                    ArrayList nodes = (ArrayList) edge.getAttribute("nodes");
                    PNode node1 = (PNode) nodes.get(0);
                    PNode node2 = (PNode) nodes.get(1);
                    String tooltipString1 = (String) node1.getAttribute("tooltip");
                    String tooltipString2 = (String) node2.getAttribute("tooltip");
                    String node_connection_tooltip = null;
                    if (tooltipString.equals(tooltipString1)) {
                        node_connection_tooltip = tooltipString2;
                    }
                    if (tooltipString.equals(tooltipString2)) {
                        node_connection_tooltip = tooltipString1;
                    }
                    boolean find = false;
                    int jj = 0;
                    for (jj = 0; jj < numnodes; jj++) {
                        if (node_connection_tooltip.equals(node_name[jj])) {
                            find = true;
                            break;
                        }
                    }
                    if (find) {
                        double[] attraction = new double[2];
                        attraction = Attraction(i, jj, positions);
                        force1[0] = force1[0] + attraction[0];
                        force1[1] = force1[1] + attraction[1];
                    }
                }
                if (i == 0) {
                    System.out.println("force1[0] = " + force1[0] + "\tforce1[1] = " + force1[1]);
                }
                force[0] = force[0] + force1[0];
                force[1] = force[1] + force1[1];
                if (i == 0) {
                    System.out.println("force[0] = " + force[0] + "\tforce[1] = " + force[1]);
                }
                velocities[i][0] = (velocities[i][0] + timestep * force[0]) * damping;
                velocities[i][1] = (velocities[i][1] + timestep * force[1]) * damping;
                positions[i][0] = positions[i][0] + timestep * velocities[i][0];
                positions[i][1] = positions[i][1] + timestep * velocities[i][1];
                if (positions[i][0] < 0) {
                    positions[i][0] = 0.;
                }
                if (positions[i][1] < 0) {
                    positions[i][0] = 0.;
                }
                if (positions[i][0] > with_canvas) {
                    positions[i][0] = with_canvas;
                }
                if (positions[i][1] > hight_canvas) {
                    positions[i][1] = hight_canvas;
                }
                total_kinetic_energy = total_kinetic_energy + mass * (velocities[i][0] * velocities[i][0] + velocities[i][1] * velocities[i][1]);
//                if(i == 0) System.out.println("total_kinetic_energy[0] = "+total_kinetic_energy);
            }
            System.out.println("total_kinetic_energy = " + total_kinetic_energy);
            if (num > 1000) {
                break;
            }
            num++;
        }
        return positions;
    }

    static private double[] Repulsion(int this_node_num, int other_node_num, double[][] positions) {
        double zarad = 10.;
        double x = Math.abs(positions[other_node_num][0] - positions[this_node_num][0]);
        double y = Math.abs(positions[other_node_num][1] - positions[this_node_num][1]);
        double[] f = new double[2];
        double l = Math.sqrt(x * x + y * y);
        f[0] = zarad * x / Math.pow(l, 2.);
        f[1] = zarad * y / Math.pow(l, 2.);
//        f[0] = zarad*x;
//        f[1] = zarad*y;

        return f;
    }

    static private double[] Attraction(int this_node_num, int connection_node_num, double[][] positions) {
        double k = 10.;
        double x = Math.abs(positions[connection_node_num][0] - positions[this_node_num][0]);
        double y = Math.abs(positions[connection_node_num][1] - positions[this_node_num][1]);
        double[] f = new double[2];
//        double l = Math.sqrt(x*x + y*y);
        f[0] = k * x;
        f[1] = k * y;
        return f;
    }

    static public void RepaintEdge(PNode node) {
        ArrayList edges = (ArrayList) node.getAttribute("edges");

        int i;
        for (i = 0; i < edges.size(); i++) {
            PPath edge = (PPath) edges.get(i);
            UpdateInterfaceName((PNode) edge);
            ArrayList nodes = (ArrayList) edge.getAttribute("nodes");
            PNode node1 = (PNode) nodes.get(0);
            PNode node2 = (PNode) nodes.get(1);

            edge.reset();
            // Note that the node's "FullBounds" must be used (instead of just the "Bound")
            // because the nodes have non-identity transforms which must be included when
            // determining their position.
            Point2D.Double bound1 = (Point2D.Double) node1.getFullBounds().getCenter2D();
            Point2D.Double bound2 = (Point2D.Double) node2.getFullBounds().getCenter2D();

            edge.moveTo((float) bound1.getX(), (float) bound1.getY());
            edge.lineTo((float) bound2.getX(), (float) bound2.getY());

            ArrayList tmp = new ArrayList();
            tmp.add(bound1);
            tmp.add(bound2);
            edge.addAttribute("coordinate", tmp);
        }
    }

    static public boolean CheckMapFile(String mapfile) {
        try //read map file for get nodes
        {
            BufferedReader inFile = new BufferedReader(new FileReader(mapfile));
            boolean end_scan_layer = false;
            while (true) {
                try {
                    if (inFile.readLine().toString().matches("^:nodes:$")) {
                        break;
                    }
                } catch (java.lang.NullPointerException e) {
                    inFile.close();
                    end_scan_layer = true;
                    break;
                }
            }

            while (!end_scan_layer) {
                try {
                    String line = inFile.readLine().toString();
                    if (line.matches("^:\\S+:$")) {
                        inFile.close();
                        break;
                    }
                    String[] fields = line.split(";");
                    if (fields.length == 12 || fields.length == 13) {
                        inFile.close();
                        return true;
                    }
                } catch (java.lang.NullPointerException e) {
                    break;
                }
            }
            inFile.close();
        } catch (java.io.FileNotFoundException e) {
//            System.out.println("File " + mapfile + " not found !");
        } catch (java.io.IOException e) {
            System.out.println("Error read from file : " + mapfile);
        }

        return false;
    }

    static public void GraphLayout(PLayer nodeLayer) {
        double increment = 0.5;
        double xforce;
        double yforce;
        double xattr;
        double yattr;
        double xrepuls;
        double yrepuls;
        for (int i = 0; i < nodeLayer.getChildrenCount(); i++) {
            PNode node = nodeLayer.getChild(i);
            String old_or_new = (String) node.getAttribute("old_or_new");
            if (old_or_new == null || old_or_new.equals("old")) {
                continue;
            }
            ArrayList attraction = GetAttraction(node);
            ArrayList repulsion = GetRepulsion(node, nodeLayer, i);
            xattr = (Double) attraction.get(0);
            yattr = (Double) attraction.get(1);
            xrepuls = (Double) repulsion.get(0);
            yrepuls = (Double) repulsion.get(1);
            xforce = xattr + xrepuls;
            yforce = yattr + yrepuls;
            double x_delta = xforce * increment;
            double y_delta = yforce * increment;
            Rectangle2D bound = node.getBounds().getBounds2D();
            node.setBounds(bound.getX() + x_delta, bound.getY() + y_delta, bound.getWidth(), bound.getHeight());
            ArrayList texts = (ArrayList) node.getAttribute("text");
            PText text = (PText) texts.get(0);
            bound = text.getBounds().getBounds2D();
            text.setBounds(bound.getX() + x_delta, bound.getY() + y_delta, bound.getWidth(), bound.getHeight());
            Utils.RepaintEdge(node);

        }
//        System.out.println("all_offset="+all_offset);
    }

    static private ArrayList GetLinkedNodes(PNode node) {
        ArrayList linked_nodes = new ArrayList();
        Point2D.Double center = (Point2D.Double) node.getBounds().getCenter2D();
        ArrayList edges = (ArrayList) node.getAttribute("edges");
        for (int i = 0; i < edges.size(); i++) {
            PPath edge = (PPath) edges.get(i);
            ArrayList nodes = (ArrayList) edge.getAttribute("nodes");
            PNode node1 = (PNode) nodes.get(0);
            PNode node2 = (PNode) nodes.get(1);
            Point2D.Double center1 = (Point2D.Double) node1.getBounds().getCenter2D();
            Point2D.Double center2 = (Point2D.Double) node2.getBounds().getCenter2D();
            if (!(center.getX() == center1.getX() && center.getY() == center1.getY())) {
                linked_nodes.add(node1);
            }
            if (!(center.getX() == center2.getX() && center.getY() == center2.getY())) {
                linked_nodes.add(node2);
            }

        }
        return linked_nodes;
    }

    static private ArrayList GetAttraction(PNode node) {
        ArrayList attraction = new ArrayList();
        double koefficient = 0.1;
        double forceX = 0.;
        double forceY = 0.;
        Point2D.Double center = (Point2D.Double) node.getBounds().getCenter2D();
        ArrayList linked_nodes = GetLinkedNodes(node);
        for (int i = 0; i < linked_nodes.size(); i++) {
            PNode node_sec = (PNode) linked_nodes.get(i);
            Point2D.Double center_sec = (Point2D.Double) node_sec.getBounds().getCenter2D();
            double distance = center_sec.getX() - center.getX();
            double force = koefficient * distance;
//            System.out.println("delta="+(center_sec.getX()-center.getX())+" distance="+distance+" force="+force);
            forceX = forceX + force;
            distance = center_sec.getY() - center.getY();
            force = koefficient * distance;
            forceY = forceY + force;
        }
        attraction.add(forceX);
        attraction.add(forceY);
        return attraction;
    }

    static private ArrayList GetRepulsion(PNode node, PLayer nodeLayer, int number) {
        ArrayList repulsion = new ArrayList();
        double electrical_repulsion = 1000;
        double forceX = 0.;
        double forceY = 0.;
        Point2D.Double center = (Point2D.Double) node.getBounds().getCenter2D();
        for (int i = 0; i < nodeLayer.getChildrenCount(); i++) {
            if (i == number) {
                continue;
            }
            PNode node_sec = nodeLayer.getChild(i);
            Point2D.Double center_sec = (Point2D.Double) node_sec.getBounds().getCenter2D();
            double deltaX = (center.getX() - center_sec.getX());
            double deltaY = (center.getY() - center_sec.getY());
            double force = (electrical_repulsion * electrical_repulsion) / (deltaX * deltaX + deltaY * deltaY);
            double xforce = force * deltaX / Math.sqrt(deltaX * deltaX + deltaY * deltaY);
//            System.out.println("deltaX="+deltaX+" xforce="+xforce);
            forceX = forceX + xforce;
            double yforce = force * deltaY / Math.sqrt(deltaX * deltaX + deltaY * deltaY);
//            System.out.println("deltaY="+deltaY+" yforce="+yforce);
            forceY = forceY + yforce;
        }
        repulsion.add(forceX);
        repulsion.add(forceY);
        return repulsion;
    }

    private ArrayList ReadFromMapFile(String filename, String match) {
        ArrayList<String[]> result = new ArrayList();

        BufferedReader inFile = null;
        File file = new File(filename);
        if (file.exists()) {
            try {
                inFile = new BufferedReader(new FileReader(filename));
                while (true) {
                    String line = inFile.readLine().toString();
                    if (line.matches(match)) {
                        break;
                    }
                }
                while (true) {
                    String line = inFile.readLine().toString();
                    if (line.matches("^:\\S+:$")) {
                        break;
                    }
                    if (!line.equals("")) {
                        result.add(line.split(";", -1));
                    }
//                    System.out.println(line);
                }
            } catch (Exception ex) {
            } finally {
                if (inFile != null) {
                    try {
                        inFile.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        return result;
    }

    private String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }

    public static String HTTPRequestGET(String url_str) {
        try {
            URL url = new URL(url_str);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("user", Main.user);
            con.setRequestProperty("passwd", Main.passwd);
            int status = con.getResponseCode();

            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer content = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine + "\n");
                }
                in.close();
                con.disconnect();
                return content.toString();
            }
            con.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    public static String HTTPRequestPOST(String url_str, String data) {
        String result = "";
        try {
            URL url = new URL(url_str);
            byte[] postData = data.getBytes(StandardCharsets.UTF_8);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con = (HttpURLConnection) url.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("user", Main.user);
            con.setRequestProperty("passwd", Main.passwd);

            try ( DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postData);
            }

            StringBuilder content;

            try ( BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            int code = con.getResponseCode();
            if (code == 200) {
                result = content.toString();
            }
            con.disconnect();
//            System.out.println("response code: "+con.getResponseCode());
//            result = content.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static String HTTPSRequestGET(String url_str) {
        String out = "";

        try {
            DisableSslVerification();
            URL url = new URL(url_str);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("user", Main.user);
            con.setRequestProperty("passwd", Main.passwd);
            con.setRequestMethod("GET");
            int status = con.getResponseCode();

            if (status == 200) {
                try ( BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine + "\n");
                    }
                    out = content.toString();
                }
            }
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }

    public static String HTTPSRequestPOST(String url_str, String data) {
        String result = "";
        try {
            DisableSslVerification();
            URL url = new URL(url_str);
            byte[] postData = data.getBytes(StandardCharsets.UTF_8);

            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con = (HttpsURLConnection) url.openConnection();

            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "Java client");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("user", Main.user);
            con.setRequestProperty("passwd", Main.passwd);

            try ( DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postData);
            }

            StringBuilder content;

            try ( BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

            int code = con.getResponseCode();
            if (code == 200) {
                result = content.toString();
            }
            con.disconnect();
//            System.out.println("response code: "+con.getResponseCode());
//            result = content.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public static void DisableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public Object GetKey(String key_str, Map MAP_INFO) {
        Object result = null;

        String[] keys = key_str.split("/");
        if (keys.length == 0) {
            result = MAP_INFO;
        } else {
            if (keys[0].equals("")) {
                Map value_map = (Map) MAP_INFO;
                Object value = null;
                String key = "";
                boolean found = true;
                for (int i = 1; i < keys.length; i++) {
                    key = keys[i];
                    if (value_map.get(key) instanceof Map) {
                        value_map = (Map) value_map.get(key);
                    } else {
                        value = value_map.get(key);
                        if (value == null) {
                            found = false;
                        }
                        break;
                    }
                }
                if (key.equals(keys[keys.length - 1])) {
                    if (found) {
                        if (value == null) {
                            result = value_map;
                        } else {
                            result = value;
                        }
                    }
                }
            }

        }
        return result;
    }

    public boolean SetKey(String key_str, Object val, Map MAP_INFO) {
        boolean result = false;

        String[] keys = key_str.split("/", -1);
        if (keys[keys.length - 1].equals("")) {
            String[] keys_new = new String[keys.length - 1];
            for (int i = 0; i < keys.length - 1; i++) {
                keys_new[i] = keys[i];
            }
            keys = keys_new;
        }
        if (keys.length > 0) {
            if (keys[0].equals("")) {
                Map value_map = (Map) MAP_INFO;
                String key = "";
                int depth = 0;
                for (int i = 1; i < keys.length; i++) {
                    key = keys[i];
                    if (value_map.get(key) != null && value_map.get(key) instanceof Map && i < keys.length - 1) {
                        value_map = (Map) value_map.get(key);
                        depth = depth + 1;
                    } else {
                        break;
                    }
                }
                if (depth < keys.length - 2) {
                    Map map_append = new HashMap();
                    map_append.put(keys[keys.length - 1], val);
                    for (int i = keys.length - 2; i > depth; i--) {
                        Map map_tmp = new HashMap();
                        map_tmp.put(keys[i], map_append);
                        map_append = map_tmp;
                    }
                    value_map.putAll(map_append);
                    result = true;
                } else if (key.equals("")) {
                    value_map.clear();
                    value_map.putAll((Map) val);
                    result = true;
                } else {
                    value_map.put(key, val);
                    result = true;
                }
            }

        }
        return result;
    }

    public void LoadNewMap(String mapfile, String area_name) {
        Map<String, String> parameters = new HashMap<>();
        String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/get?file=" + mapfile + "&key=/" + area_name;
        
        WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
        try {
//            System.out.println("Start load.");
            ClearNebViewer();

            Main.isBusy = true;
            waitCircleApplicationIcon.end = false;
            waitCircleApplicationIcon.start(); 
            Main.control_panel.SetDisable();
            Main.time_machine.SetDisable();

            String result = HTTPRequestGET(url);
    //        System.out.println(result);

            JSONParser parser = new JSONParser();
            if (!result.equals("")) {
                try {
                    JSONObject jsonObject = (JSONObject) parser.parse(result);
                    Main.INFORMATION = toMap(jsonObject);
                } catch (Exception ex) {
                    Frame frame = Main.getFrames()[0];
                    JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            } else {
                Frame frame = Main.getFrames()[0];
                JOptionPane.showMessageDialog(frame, "Error request to Neb server !!!", "Error request.", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            if (Main.INFORMATION.size() > 0) {
                // Initialize, and create a layer for the edges (always underneath the nodes)
//                Main.textCustomLayer = new PLayer();
//                Main.canvas.getCamera().addLayer(0, Main.textCustomLayer);                
                Main.textLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.textLayer);
                Main.flashLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.flashLayer);
                Main.nodeLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.nodeLayer);
                Main.edgeLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.edgeLayer);
                Main.interfnameLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.interfnameLayer);
                Main.textCustomLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.textCustomLayer);
                Main.selectLayer = new PLayer();
                Main.canvas.getCamera().addLayer(0, Main.selectLayer);
                Random rnd = new Random();

                // create flash links
                PPath krug1 = PPath.createEllipse(50, 50, Main.origRadiusFlashLink, Main.origRadiusFlashLink);
                krug1.setPaint(new Color(76, 255, 0));
                krug1.setStroke(new BasicStroke(2));
                krug1.setStrokePaint(new Color(38, 127, 0));
                krug1.setVisible(false);
                Main.flashLayer.addChild(krug1);
                PPath krug2 = PPath.createEllipse(50, 50, Main.origRadiusFlashLink, Main.origRadiusFlashLink);
                krug2.setPaint(new Color(76, 255, 0));
                krug2.setStroke(new BasicStroke(2));
                krug2.setStrokePaint(new Color(38, 127, 0));
                krug2.setVisible(false);
                Main.flashLayer.addChild(krug2);

                // load nodes
                Map<String, Map> nodes_information = (Map<String, Map>) Main.INFORMATION.get("nodes_information");
                ArrayList<ArrayList<String>> mac_ip_port = (ArrayList<ArrayList<String>>) Main.INFORMATION.get("mac_ip_port");
                Map<String, Map<String, ArrayList<String[]>>> node_port_ip_mac = new HashMap();
                for (ArrayList<String> item : mac_ip_port) {
                    String mac = item.get(0);
                    String ip = item.get(1);
                    String node = item.get(2);
                    String iface = item.get(4);
                    if (node_port_ip_mac.get(node) == null) {
                        Map<String, ArrayList<String[]>> port_ip_mac_map = new HashMap();
                        ArrayList<String[]> ip_mac_list = new ArrayList();
                        String[] ip_mac = new String[2];
                        ip_mac[0] = ip;
                        ip_mac[1] = mac;
                        ip_mac_list.add(ip_mac);
                        port_ip_mac_map.put(iface, ip_mac_list);
                        node_port_ip_mac.put(node, port_ip_mac_map);
                    } else {
                        Map<String, ArrayList<String[]>> port_ip_mac_map = node_port_ip_mac.get(node);
                        ArrayList<String[]> ip_mac_list = new ArrayList();
                        if (port_ip_mac_map.get(iface) == null) {
                            String[] ip_mac = new String[2];
                            ip_mac[0] = ip;
                            ip_mac[1] = mac;
                            ip_mac_list.add(ip_mac);
                            port_ip_mac_map.put(iface, ip_mac_list);
                        } else {
                            ip_mac_list = port_ip_mac_map.get(iface);
                            String[] ip_mac = new String[2];
                            ip_mac[0] = ip;
                            ip_mac[1] = mac;
                            ip_mac_list.add(ip_mac);
                        }
                        port_ip_mac_map.put(iface, ip_mac_list);
                    }
                }

                if (nodes_information != null) {
                    Map<String, PImage> ip_pimage = new HashMap();
                    for (Map.Entry<String, Map> iter1 : nodes_information.entrySet()) {
                        String node = iter1.getKey();
                        if (!node.equals("")) {
//                            if(node.equals("10.96.201.186"))
//                                System.out.println("11111");
                            
                            Map val1 = iter1.getValue();
                            PImage image = new PImage();
                            String path_image = (String) val1.get("image");
                            if (path_image == null) {
                                path_image = Main.default_image;
                            }
                            path_image = Main.path+"/"+path_image;
                            path_image = path_image.replace("\\", "/");
                            image.setImage(path_image);

                            ArrayList<String> xy = (ArrayList) val1.get("xy");
                            if (xy != null && xy.size() == 2) {
                                double x = Double.parseDouble(xy.get(0));
                                double y = Double.parseDouble(xy.get(1));
                                double width = 0;
                                double height = 0;
                                image.setX(x);
                                image.setY(y);
                                image.addAttribute("old_or_new", "old");
                            } else {
                                float x = (float) (5000. * rnd.nextDouble());
                                float y = (float) (5000. * rnd.nextDouble());
//                                image.setX(500000. + x);
//                                image.setY(50000. + y);
                                image.setX(x);
                                image.setY(y);                                
                                image.addAttribute("old_or_new", "new");
                            }
                            image.addAttribute("edges", new ArrayList());
                            if (image != null) {
                                ip_pimage.put(node, image);
                                image.addAttribute("pimage", image);
                            }
                            image.addAttribute("tooltip", node);

                            String sysname = (String) GetKey("/general/sysname", val1);
                            if (sysname != null) {
                                image.addAttribute("tooltip_full", node + " - " + sysname);
                            } else {
                                image.addAttribute("tooltip_full", node);
                            }

                            if (path_image != null) {
                                image.addAttribute("path_image", path_image);
                            }
                            image.addAttribute("ip", node);
                            if (sysname != null) {
                                image.addAttribute("sysDescription", sysname);
                            }
                            Map<String, ArrayList<String>> account = (Map<String, ArrayList<String>>) Main.INFORMATION.get("node_protocol_accounts");
                            if (account != null) {
                                ArrayList<String> acc = account.get(node);
                                if (acc != null) {
                                    image.addAttribute("snmp_account", acc);
                                }
                            }

                            String uptime = (String) GetKey("/general/uptime", val1);
                            if (uptime != null) {
                                image.addAttribute("uptime", uptime);
                            }
                            if (sysname != null) {
                                image.addAttribute("sysName", sysname);
                            }

                            Map iface = (Map) val1.get("interfaces");
                            if (iface != null && iface.size() > 0) {
                                image.addAttribute("interfInformation", iface);
                            }
                            ArrayList routes = (ArrayList) val1.get("routes");
                            if (routes != null && routes.size() > 0) {
                                image.addAttribute("routeInformation", routes);
                            }
                            Map vlans = (Map) val1.get("vlans");
                            if (vlans != null && vlans.size() > 0) {
                                image.addAttribute("vlanInformation", vlans);
                            }

                            if (node_port_ip_mac.get(node) != null) {
                                image.addAttribute("clientsInformation", node_port_ip_mac.get(node));
                            }
                            Main.nodeLayer.addChild(image);
                        }
                    }

                    // Adding links
                    ArrayList<ArrayList<String>> links_information = (ArrayList<ArrayList<String>>) Main.INFORMATION.get("links");
                    for (ArrayList<String> iter1 : links_information) {
                        if (iter1.size() == 6 || iter1.size() == 7) {
                            String node1_str = iter1.get(0);
                            PImage node1 = ip_pimage.get(node1_str);
                            String iface1_id = iter1.get(1);
                            String iface1_name = iter1.get(2);
                            String node2_str = iter1.get(3);
                            PImage node2 = ip_pimage.get(node2_str);
                            String iface2_id = iter1.get(4);
                            String iface2_name = iter1.get(5);
                            String type = "";
                            if(iter1.size() == 7) type = iter1.get(6);
                            String id_link = node1_str + ";" + iface1_name + ";" + node2_str + ";" + iface2_name;
 
//                            if((node1_str.equals("10.96.113.30") && iface1_name.equals("GigabitEthernet1/0/49")) ||
//                                    (node2_str.equals("10.96.113.30") && iface2_name.equals("GigabitEthernet1/0/49")) )
//                                System.out.println("1111");
                            
                            
                            
                            if (node1 != null && node2 != null && !iface1_name.equals("") && !iface2_name.equals("")) {
                                Point2D.Double bound1 = (Point2D.Double) node1.getBounds().getCenter2D();
                                Point2D.Double bound2 = (Point2D.Double) node2.getBounds().getCenter2D();
                                Map<String, Map<String, String>> interfInformation1 = (Map<String, Map<String, String>>) node1.getAttribute("interfInformation");
                                Map<String, Map<String, String>> interfInformation2 = (Map<String, Map<String, String>>) node2.getAttribute("interfInformation");

                                String speed1 = "";
                                if (interfInformation1 != null && interfInformation1.get(iface1_name) != null && interfInformation1.get(iface1_name).get("speed") != null) {
                                    speed1 = interfInformation1.get(iface1_name).get("speed");
                                }
                                String speed2 = "";
                                if (interfInformation2 != null && interfInformation2.get(iface2_name) != null && interfInformation2.get(iface2_name).get("speed") != null) {
                                    speed2 = interfInformation2.get(iface2_name).get("speed");
                                }

                                PPath edge = new PPath.Double();
                                edge.moveTo((float) bound1.getX(), (float) bound1.getY());
                                edge.lineTo((float) bound2.getX(), (float) bound2.getY());

                                ArrayList tmp = (ArrayList) node1.getAttribute("edges");
                                tmp.add(edge);
                                node1.addAttribute("edges", tmp);
                                tmp = (ArrayList) node2.getAttribute("edges");
                                tmp.add(edge);
                                node2.addAttribute("edges", tmp);

                                ArrayList<PImage> tmp1 = new ArrayList();
                                tmp1.add(node1);
                                tmp1.add(node2);
                                edge.addAttribute("nodes", tmp1);
                                ArrayList<Point2D.Double> tmp2 = new ArrayList();
                                tmp2.add(bound1);
                                tmp2.add(bound2);
                                edge.addAttribute("coordinate", tmp2);

                                ArrayList<String> link = new ArrayList();
                                link.add(node1_str);
                                link.add(iface1_id);
                                link.add(iface1_name);
                                link.add(node2_str);
                                link.add(iface2_id);
                                link.add(iface2_name);
                                edge.addAttribute("link", link);

                                String tooltip = node1_str + " " + iface1_name + " - " + node2_str + " " + iface2_name;
                                edge.addAttribute("tooltip", tooltip);
                                edge.addAttribute("index_iface", iface1_id + ":" + iface2_id);

                                edge.addAttribute("ppath", edge);

                                // set with edge
                                int width = 1;
                                //            double color=0.0;
                                if (!speed1.equals("") || !speed2.equals("")) {
                                    double sp1 = 0;
                                    if (speed1.matches(".*10 Mbps.*") || speed1.matches(".*10Mb/s.*")) {
                                        sp1 = 10000000;
                                    } else if (speed1.matches(".*100 Mbps.*") || speed1.matches(".*100Mb/s.*")) {
                                        sp1 = 100000000;
                                    } else if (speed1.matches(".*1 Gbps.*") || speed1.matches(".*1000 Mbps.*") ||
                                            speed1.matches(".*1Gb/s.*") || speed1.matches(".*1000Mb/s.*")) {
                                        sp1 = 1000000000;
                                    } else if (speed1.matches(".*10 Gbps.*") || speed1.matches(".*10Gb/s.*")) {
                                        sp1 = 10000000000.;
                                    } else if (speed1.matches(".*100 Gbps.*") || speed1.matches(".*100Gb/s.*")) {
                                        sp1 = 100000000000.;
                                    }
                                    double sp2 = 0;
                                    if (speed2.matches(".*10 Mbps.*") || speed2.matches(".*10Mb/s.*")) {
                                        sp2 = 10000000;
                                    } else if (speed2.matches(".*100 Mbps.*") || speed2.matches(".*100Mb/s.*")) {
                                        sp2 = 100000000;
                                    } else if (speed2.matches(".*1 Gbps.*") || speed2.matches(".*1000 Mbps.*") ||
                                            speed2.matches(".*1Gb/s.*") || speed2.matches(".*1000Mb/s.*")) {
                                        sp2 = 1000000000;
                                    } else if (speed2.matches(".*10 Gbps.*") || speed2.matches(".*10Gb/s.*")) {
                                        sp2 = 10000000000.;
                                    } else if (speed2.matches(".*100 Gbps.*") || speed2.matches(".*100Gb/s.*")) {
                                        sp2 = 100000000000.;
                                    }
                                    double speed = 0.;
                                    if (sp1 > sp2) {
                                        speed = sp1;
                                    } else {
                                        speed = sp2;
                                    }

                                    if (speed <= 10000000) {
                                        edge.setStroke(new BasicStroke(1));
                                        width = 1;
                                    } else if (speed > 10000000 && speed <= 100000000) {
                                        edge.setStroke(new BasicStroke(2));
                                        width = 2;
                                        edge.setStrokePaint(new Color(0, 0, 255));
                                    } else if (speed > 100000000 && speed <= 1000000000) {
                                        edge.setStroke(new BasicStroke(4));
                                        width = 4;
                                        edge.setStrokePaint(new Color(255, 0, 0));
                                    } else if (speed > 1000000000. && speed <= 10000000000.) {
                                        edge.setStroke(new BasicStroke(6));
                                        width = 6;
                                        edge.setStrokePaint(new Color(0, 64, 0));
                                    } else if (speed > 10000000000.) {
                                        edge.setStroke(new BasicStroke(8));
                                        width = 8;
                                        edge.setStrokePaint(new Color(0, 150, 0));
                                    }
                                }
                                edge.addAttribute("width", width);
                                Main.edgeLayer.addChild(edge);

                            }

                        }
                    }

                    for (int i = 0; i < Main.edgeLayer.getChildrenCount(); i++) {
                        PText interf_name = new PText();
                        PNode n = Main.edgeLayer.getChild(i);
                        ArrayList tmp = new ArrayList();
                        tmp.add(interf_name);
                        n.addAttribute("interf_name", tmp);

                        UpdateInterfaceName(n);
                        Main.interfnameLayer.addChild(interf_name);
                    }
                    // split overlaped links
                    SplitAllOverlapedLinks();

                    // Adding text
                    for (int i = 0; i < Main.nodeLayer.getChildrenCount(); i++) {
                        PNode node1 = Main.nodeLayer.getChild(i);

                        String[] bound = new String[4];
                        PText text = new PText();
                        String tooltipString = (String) node1.getAttribute("tooltip");
                        text.setText(tooltipString);

                        Font font = new Font("Arial", Font.PLAIN, 50);
                        text.setFont(font);
                        Point2D.Double pbound = (Point2D.Double) node1.getBounds().getCenter2D();
                        double w = node1.getWidth();
                        double h = node1.getHeight();
                        double wt = text.getWidth();
                        double ht = text.getHeight();
                        double x = pbound.getX();
                        double y = pbound.getY();
                        text.setX(x - wt / 2.);

                        text.setY(y - h / 2. - ht / 2. - 10);

                        node1.addAttribute("text", text);
                        text.addAttribute("text", text);
                        text.addAttribute("node1", node1);

                        Main.textLayer.addChild(text);
                    }

//////////////////////////////////////
                    // Adding custom text
                    if(Main.INFORMATION.get("texts") != null) {
                        Map<String, Map> text_information = (Map<String, Map>) Main.INFORMATION.get("texts");
                        for (Map.Entry<String, Map> iter1 : text_information.entrySet()) {
                            String text = iter1.getKey();
                            Map val = iter1.getValue();
                            double x = 0.0; double y = 0.0;
                            int size = 32;
                            ArrayList xy = (ArrayList)val.get("xy");
                            if(xy != null && xy.size() == 2) {
                                x = Double.valueOf((String)xy.get(0));
                                y = Double.valueOf((String)xy.get(1));
                            }
                            String size_str = (String)val.get("size");
                            if(size_str != null)
                                size = Integer.parseInt(size_str);
//                            String font = (String)val.get("font");
//                            String attribute = (String)val.get("attribute");
                            
                            PText text_custom = new PText();
                            text_custom.setText(text);
                            text_custom.setX(x);
                            text_custom.setY(y);
                            text_custom.addAttribute("text", text);
                            Font fnt = text_custom.getFont();
                            text_custom.setFont(new Font(fnt.getFontName(), Font.PLAIN, Integer.valueOf(size)));
                            Main.textCustomLayer.addChild(text_custom);
                        }
                    }

                }

            }

            Main.camera = Main.canvas.getCamera();

            SetAllToScreen();

            //create tooltip areas
            float[] mas_x = new float[7];
            float[] mas_y = new float[7];
            float x = 0f;
            float y = 0f;
            float width = 90f;
            float height = 50f;
            float otstup = 3f;

            PText tooltipTextTop = new PText();
            tooltipTextTop.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create top tooltip area
            mas_x[1] = x - 2f * width / 3f;
            mas_y[1] = y - height;
            mas_x[2] = x - 2f * width / 3f;
            mas_y[2] = y - 2f * height;
            mas_x[3] = x + width / 3f;
            mas_y[3] = y - 2f * height;
            mas_x[4] = x + width / 3f;
            mas_y[4] = y - height;
            mas_x[5] = x - width / 3f;
            mas_y[5] = y - height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectTop = PPath.createPolyline(mas_x, mas_y);
            tooltipRectTop.setPaint(Color.yellow);
            tooltipRectTop.setPickable(false);
            tooltipRectTop.setVisible(false);
            tooltipRectTop.addChild(tooltipTextTop);
            Main.camera.addChild(tooltipRectTop);

            PText tooltipTextBottom = new PText();
            tooltipTextBottom.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create bottom tooltip area
            mas_x[1] = x - width / 3f;
            mas_y[1] = y + height;
            mas_x[2] = x + width / 3f;
            mas_y[2] = y + height;
            mas_x[3] = x + width / 3f;
            mas_y[3] = y + 2f * height;
            mas_x[4] = x - 2f * width / 3f;
            mas_y[4] = y + 2f * height;
            mas_x[5] = x - 2f * width / 3f;
            mas_y[5] = y + height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectBottom = PPath.createPolyline(mas_x, mas_y);
            tooltipRectBottom.setPaint(Color.yellow);
            tooltipRectBottom.setPickable(false);
            tooltipRectBottom.setVisible(false);
            tooltipRectBottom.addChild(tooltipTextBottom);
            Main.camera.addChild(tooltipRectBottom);

            PText tooltipTextLeft = new PText();
            tooltipTextLeft.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create left tooltip area
            mas_x[1] = x + width / 3f;
            mas_y[1] = y - height;
            mas_x[2] = x + width / 3f;
            mas_y[2] = y - 2f * height;
            mas_x[3] = x + 4f * width / 3f;
            mas_y[3] = y - 2f * height;
            mas_x[4] = x + 4f * width / 3f;
            mas_y[4] = y - height;
            mas_x[5] = x + 2f * width / 3f;
            mas_y[5] = y - height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectLeft = PPath.createPolyline(mas_x, mas_y);
            tooltipRectLeft.setPaint(Color.yellow);
            tooltipRectLeft.setPickable(false);
            tooltipRectLeft.setVisible(false);
            tooltipRectLeft.addChild(tooltipTextLeft);
            Main.camera.addChild(tooltipRectLeft);

            PText tooltipTextRight = new PText();
            tooltipTextRight.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create right tooltip area
            mas_x[1] = x - 2f * width / 3f;
            mas_y[1] = y - height;
            mas_x[2] = x - 4f * width / 3f;
            mas_y[2] = y - height;
            mas_x[3] = x - 4f * width / 3f;
            mas_y[3] = y - 2f * height;
            mas_x[4] = x - width / 3f;
            mas_y[4] = y - 2f * height;
            mas_x[5] = x - width / 3f;
            mas_y[5] = y - height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectRight = PPath.createPolyline(mas_x, mas_y);
            tooltipRectRight.setPaint(Color.yellow);
            tooltipRectRight.setPickable(false);
            tooltipRectRight.setVisible(false);
            tooltipRectRight.addChild(tooltipTextRight);
            Main.camera.addChild(tooltipRectRight);

            PText tooltipTextTopSecond = new PText();
            tooltipTextTopSecond.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create second top tooltip area
            mas_x[1] = x - 2f * width / 3f;
            mas_y[1] = y - height;
            mas_x[2] = x - 2f * width / 3f;
            mas_y[2] = y - 2f * height;
            mas_x[3] = x + width / 3f;
            mas_y[3] = y - 2f * height;
            mas_x[4] = x + width / 3f;
            mas_y[4] = y - height;
            mas_x[5] = x - width / 3f;
            mas_y[5] = y - height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectTopSecond = PPath.createPolyline(mas_x, mas_y);
            tooltipRectTopSecond.setPaint(Color.yellow);
            tooltipRectTopSecond.setPickable(false);
            tooltipRectTopSecond.setVisible(false);
            tooltipRectTop.addChild(tooltipTextTopSecond);
            Main.camera.addChild(tooltipRectTopSecond);

            PText tooltipTextBottomSecond = new PText();
            tooltipTextBottomSecond.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create second bottom tooltip area
            mas_x[1] = x - width / 3f;
            mas_y[1] = y + height;
            mas_x[2] = x + width / 3f;
            mas_y[2] = y + height;
            mas_x[3] = x + width / 3f;
            mas_y[3] = y + 2f * height;
            mas_x[4] = x - 2f * width / 3f;
            mas_y[4] = y + 2f * height;
            mas_x[5] = x - 2f * width / 3f;
            mas_y[5] = y + height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectBottomSecond = PPath.createPolyline(mas_x, mas_y);
            tooltipRectBottomSecond.setPaint(Color.yellow);
            tooltipRectBottomSecond.setPickable(false);
            tooltipRectBottomSecond.setVisible(false);
            tooltipRectBottomSecond.addChild(tooltipTextBottomSecond);
            Main.camera.addChild(tooltipRectBottomSecond);

            PText tooltipTextLeftSecond = new PText();
            tooltipTextLeftSecond.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create second left tooltip area
            mas_x[1] = x + width / 3f;
            mas_y[1] = y - height;
            mas_x[2] = x + width / 3f;
            mas_y[2] = y - 2f * height;
            mas_x[3] = x + 4f * width / 3f;
            mas_y[3] = y - 2f * height;
            mas_x[4] = x + 4f * width / 3f;
            mas_y[4] = y - height;
            mas_x[5] = x + 2f * width / 3f;
            mas_y[5] = y - height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectLeftSecond = PPath.createPolyline(mas_x, mas_y);
            tooltipRectLeftSecond.setPaint(Color.yellow);
            tooltipRectLeftSecond.setPickable(false);
            tooltipRectLeftSecond.setVisible(false);
            tooltipRectLeftSecond.addChild(tooltipTextLeftSecond);
            Main.camera.addChild(tooltipRectLeftSecond);

            PText tooltipTextRightSecond = new PText();
            tooltipTextRightSecond.setPickable(false);
            mas_x[0] = x;
            mas_y[0] = y;     // create second right tooltip area
            mas_x[1] = x - 2f * width / 3f;
            mas_y[1] = y - height;
            mas_x[2] = x - 4f * width / 3f;
            mas_y[2] = y - height;
            mas_x[3] = x - 4f * width / 3f;
            mas_y[3] = y - 2f * height;
            mas_x[4] = x - width / 3f;
            mas_y[4] = y - 2f * height;
            mas_x[5] = x - width / 3f;
            mas_y[5] = y - height;
            mas_x[6] = x;
            mas_y[6] = y;
            PPath tooltipRectRightSecond = PPath.createPolyline(mas_x, mas_y);
            tooltipRectRightSecond.setPaint(Color.yellow);
            tooltipRectRightSecond.setPickable(false);
            tooltipRectRightSecond.setVisible(false);
            tooltipRectRightSecond.addChild(tooltipTextRightSecond);
            Main.camera.addChild(tooltipRectRightSecond);

            // create selected contur
            Main.leftLineSelectNode = new PPath.Double();
            Main.leftLineSelectNode.setVisible(false);
            Main.leftLineSelectNode.setStrokePaint(Color.green);
            Main.leftLineSelectNode.setStroke(new BasicStroke(1));
            Main.selectLayer.addChild(0, Main.leftLineSelectNode);
            Main.rightLineSelectNode = new PPath.Double();
            Main.rightLineSelectNode.setVisible(false);
            Main.rightLineSelectNode.setStrokePaint(Color.green);
            Main.selectLayer.addChild(1, Main.rightLineSelectNode);
            Main.topLineSelectNode = new PPath.Double();
            Main.topLineSelectNode.setVisible(false);
            Main.topLineSelectNode.setStrokePaint(Color.green);
            Main.selectLayer.addChild(2, Main.topLineSelectNode);
            Main.bottomLineSelectNode = new PPath.Double();
            Main.bottomLineSelectNode.setVisible(false);
            Main.bottomLineSelectNode.setStrokePaint(Color.green);
            Main.selectLayer.addChild(3, Main.bottomLineSelectNode);

            Main.view_scale = Main.camera.getViewScale();

            addCanvasContextMenu();
            addTextCustomContextMenu();

            Main.control_panel.CreateEvent(false);     // create event for View mode
//            System.out.println("End load.");
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            waitCircleApplicationIcon.end = true;
            Main.control_panel.SetEnable();
            Main.time_machine.SetEnable();
            Main.isBusy = false;
        }
    }

    public void ClearNebViewer() {
        if (Main.selectLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.selectLayer);
        }
        if (Main.textCustomLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.textCustomLayer);
        }
        if (Main.edgeLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.edgeLayer);
        }
        if (Main.nodeLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.nodeLayer);
        }
        if (Main.flashLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.flashLayer);
        }
        if (Main.interfnameLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.interfnameLayer);
        }
        if (Main.textLayer != null) {
            Main.canvas.getCamera().removeLayer(Main.textLayer);
        }
        Runtime r = Runtime.getRuntime();
        r.gc();
    }

    public void LoadMap(String mapfile, String area) {
//        Main.isBusy = true;
//        ClearNebViewer();
        //     System.out.println("Loading map file "+mapfile);
        
//        System.out.println("Run Thread load.");
        Thread t = new Thread(new Runnable() {
            public void run() {
                LoadNewMap(mapfile, area);
            }
        });
        t.start(); 
        
//        LoadNewMap(mapfile, area);
        ControlPanel.jToggleButton1.setSelected(false);
//        Main.isBusy = false;
    }

    public ArrayList<String[]> GetListMapFiles() {
        ArrayList<String[]> date_time_list = new ArrayList();
        String res = HTTPRequestGET("http://" + Main.neb_server + ":" + Main.neb_server_port + "/getfiles_list?directory=" + Main.history_path_short);
        Pattern pattern = Pattern.compile(Main.history_path_short + "/*(.+)");
        Pattern p = Pattern.compile("^.*Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");
        for (String file : res.split("\n")) {
            file = file.replace("\\", "/");
            Matcher matcher = pattern.matcher(file);
            if (matcher.find()) {
                String short_filename = matcher.group(1);
                Matcher m = p.matcher(short_filename);
                if (m.find()) {
                    String data_time = m.group(1).replace("-", " ");
                    String[] mas = new String[2];
                    mas[0] = data_time;
                    mas[1] = file;
                    date_time_list.add(mas);
                }
            }
        }

        try {
            String file = Main.map_file;
            String res1 = HTTPRequestGET("http://" + Main.neb_server + ":" + Main.neb_server_port + "/getfile_attributes?file=" + file);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(res1);
            Map file_attributes_map = toMap(jsonObject);
            String create_time = (String) file_attributes_map.get("create_time");
            if (create_time != null) {
                String[] mas = new String[2];
                mas[0] = create_time;
                mas[1] = file;
                date_time_list.add(mas);
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        // sorting
        if (date_time_list.size() > 1) {
            Collections.sort(date_time_list, new Comparator<String[]>() {
                @Override
                public int compare(String[] o1, String[] o2) {
                    try {
                        SimpleDateFormat ft = new SimpleDateFormat("dd.MM.yyyy HH.mm");
                        long sec1 = ft.parse(o1[0]).getTime();
                        long sec2 = ft.parse(o2[0]).getTime();
                        if (sec1 < sec2) {
                            return -1;
                        } else if (sec1 == sec2) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } catch (ParseException ex) {
                        java.util.logging.Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return 0;
                }
            });

        }

        return date_time_list;
    }

    public static void SetTimeMachine(ArrayList<String[]> history_list, int selector, String area) {    
        String timePattern = "dd.MM.yyyy HH.mm";
        DateFormat dateFormatter = new SimpleDateFormat(timePattern);

//        Main.isBusy = true;
//        System.out.println("selector="+selector);
        TimeMachineForm.selector = selector;
        Main.map_filename = history_list.get(selector)[1];

        Utils m_Utils = new Utils();
        if (selector > history_list.size()) {
            selector = history_list.size();
        }
        if (selector < 0) {
            selector = 0;
        }

//        if (Main.isChanged) {
//            Frame frame = Main.getFrames()[0];
//            int n = JOptionPane.showConfirmDialog(frame, "Would you like save changed ???", "Save changed ???", JOptionPane.YES_NO_OPTION);
//            if (n == 0) {
//                SaveChanged(Main.map_filename, area);
//            }
//        }
        ControlPanel.jButton1.setEnabled(false);
        URL url = Main.class.getResource("/images/edit_off.png");
        ControlPanel.jToggleButton1.setIcon(new ImageIcon(url));
        ControlPanel.jToggleButton1.setToolTipText("To edit mode");
        Main.isChanged = false;

//        WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
//        waitCircleApplicationIcon.end = false;
//        waitCircleApplicationIcon.start();

        if (history_list.get(selector) != null) {
            TimeMachineForm.jButton1.setEnabled(false);
            TimeMachineForm.jButton2.setEnabled(false);
            TimeMachineForm.jButton4.setEnabled(false);
            TimeMachineForm.jButton3.setEnabled(false);
            Main.jPopupMenuPrev.removeAll();
            Main.jPopupMenuNext.removeAll();
            TimeMachineForm.jTextField1.setText(history_list.get(selector)[0]);
            if (history_list.size() > 1) {
                if (selector == history_list.size() - 1) {
                    TimeMachineForm.last_map_selector = true;
                    for (int i = history_list.size() - 2; i >= 0; i--) {
                        Main.jPopupMenuPrev.add(new AbstractAction(history_list.get(i)[0]) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String select_item = e.getActionCommand();
                                int selector = -1;
                                for (int ii = 0; ii < history_list.size(); ii++) {
                                    if (select_item.equals(history_list.get(ii)[0])) {
                                        selector = ii;
                                        break;
                                    }
                                }
                                if (selector != -1) {
                                    SetTimeMachine(Main.history_list, selector, area);
                                }
                            }
                        });
                    }
                    TimeMachineForm.jButton1.setEnabled(true);
                    TimeMachineForm.jButton2.setEnabled(true);

                } else if (selector == 0) {
                    TimeMachineForm.last_map_selector = false;
                    for (int i = 1; i < history_list.size(); i++) {
                        Main.jPopupMenuNext.add(new AbstractAction(history_list.get(i)[0]) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String select_item = e.getActionCommand();
                                int selector = -1;
                                for (int ii = 0; ii < history_list.size(); ii++) {
                                    if (select_item.equals(history_list.get(ii)[0])) {
                                        selector = ii;
                                        break;
                                    }
                                }
                                if (selector != -1) {
                                    SetTimeMachine(Main.history_list, selector, area);
                                }
                            }
                        });
                    }
                    TimeMachineForm.jButton3.setEnabled(true);
                    TimeMachineForm.jButton4.setEnabled(true);

                } else if (selector > 0 && selector < history_list.size()) {
                    TimeMachineForm.last_map_selector = false;
                    for (int i = selector - 1; i >= 0; i--) {
                        Main.jPopupMenuPrev.add(new AbstractAction(history_list.get(i)[0]) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String select_item = e.getActionCommand();
                                int selector = -1;
                                for (int ii = 0; ii < history_list.size(); ii++) {
                                    if (select_item.equals(history_list.get(ii)[0])) {
                                        selector = ii;
                                        break;
                                    }
                                }
                                if (selector != -1) {
                                    SetTimeMachine(Main.history_list, selector, area);
                                }
                            }
                        });
                    }
                    for (int i = selector + 1; i < history_list.size(); i++) {
                        Main.jPopupMenuNext.add(new AbstractAction(history_list.get(i)[0]) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                String select_item = e.getActionCommand();
                                int selector = -1;
                                for (int ii = 0; ii < history_list.size(); ii++) {
                                    if (select_item.equals(history_list.get(ii)[0])) {
                                        selector = ii;
                                        break;
                                    }
                                }
                                if (selector != -1) {
                                    SetTimeMachine(Main.history_list, selector, area);
                                }
                            }
                        });
                    }
                    TimeMachineForm.jButton1.setEnabled(true);
                    TimeMachineForm.jButton2.setEnabled(true);
                    TimeMachineForm.jButton3.setEnabled(true);
                    TimeMachineForm.jButton4.setEnabled(true);
                }
            }

//            m_Utils.LoadMap(Main.history_list.get(selector)[1], area);
            m_Utils.LoadMap(Main.map_filename, area);
            System.out.println("Main.map_filename = " + Main.map_filename);
        }

//        Main.isBusy = false;
//        waitCircleApplicationIcon.end = true;
//        System.out.println(loadfile.getAbsoluteFile()+"\tselector="+TimeMachineForm.selector);

    }

    public static void UpdateInterfaceName(PNode n) {
        ArrayList coordinate = (ArrayList) n.getAttribute("coordinate");
        if (coordinate != null) {
            ArrayList nodes = (ArrayList) n.getAttribute("interf_name");
            PText interf_name = (PText) nodes.get(0);
            String tooltipString = (String) n.getAttribute("tooltip");

            String[] fields = tooltipString.split(" ");
            String interface1 = fields[1].trim();
            String interface2 = fields[4].trim();
            nodes = (ArrayList) n.getAttribute("nodes");
            PNode node1 = (PNode) nodes.get(0);
            PNode node2 = (PNode) nodes.get(1);
            Point2D.Double bound1 = (Point2D.Double) node1.getFullBounds().getCenter2D();
            Point2D.Double bound2 = (Point2D.Double) node2.getFullBounds().getCenter2D();
            String tooltipStringNode1 = (String) node1.getAttribute("tooltip");
            String tooltipStringNode2 = (String) node2.getAttribute("tooltip");
            if (bound1.getX() < bound2.getX()) {
                if (tooltipStringNode1.equals(fields[0])) {
                    tooltipString = interface1 + " - " + interface2;
                }
                if (tooltipStringNode1.equals(fields[3])) {
                    tooltipString = interface2 + " - " + interface1;
                }
            } else {
                if (tooltipStringNode2.equals(fields[0])) {
                    tooltipString = interface1 + " - " + interface2;
                }
                if (tooltipStringNode2.equals(fields[3])) {
                    tooltipString = interface2 + " - " + interface1;
                }
            }
            interf_name.setText(tooltipString);
            Font font = new Font("Arial", Font.PLAIN, 50);
            interf_name.setFont(font);
            Point2D point1 = (Point2D) coordinate.get(0);
            Point2D point2 = (Point2D) coordinate.get(1);
            double x1 = point1.getX();
            double y1 = point1.getY();
            double x2 = point2.getX();
            double y2 = point2.getY();
            double delta_y = y2 - y1;
            double delta_x = x2 - x1;
            double alfa = Math.atan(delta_y / delta_x);
            double w = interf_name.getWidth();
            double x = x1 + (x2 - x1) / 2. - w / 2 * Math.cos(alfa);
            double y = y1 + (y2 - y1) / 2. - w / 2 * Math.sin(alfa);

            interf_name.setOffset(x, y);
            interf_name.setRotation(alfa);

        }
    }

    private static void SplitAllOverlapedLinks() {
        for (int i = 0; i < Main.edgeLayer.getChildrenCount(); i++) {
            PNode node = Main.edgeLayer.getChild(i);
            ArrayList nodes = (ArrayList) node.getAttribute("nodes");
            PNode node1 = (PNode) nodes.get(0);
            PNode node2 = (PNode) nodes.get(1);
            String tooltipString11 = (String) node1.getAttribute("tooltip");
            String tooltipString12 = (String) node2.getAttribute("tooltip");
            ArrayList coordinate1 = (ArrayList) node.getAttribute("coordinate");
            Point2D point1 = (Point2D) coordinate1.get(0);
            Point2D point2 = (Point2D) coordinate1.get(1);
            Double x11 = point1.getX();
            Double y11 = point1.getY();
            Double x12 = point2.getX();
            Double y12 = point2.getY();
            int num_overlaps = 0;
            for (int j = i + 1; j < Main.edgeLayer.getChildrenCount(); j++) {
                node = Main.edgeLayer.getChild(j);
                nodes = (ArrayList) node.getAttribute("nodes");
                node1 = (PNode) nodes.get(0);
                node2 = (PNode) nodes.get(1);
                String tooltipString21 = (String) node1.getAttribute("tooltip");
                String tooltipString22 = (String) node2.getAttribute("tooltip");

                ArrayList coordinate2 = (ArrayList) node.getAttribute("coordinate");
                point1 = (Point2D) coordinate2.get(0);
                point2 = (Point2D) coordinate2.get(1);
                Double x21 = point1.getX();
                Double y21 = point1.getY();
                Double x22 = point2.getX();
                Double y22 = point2.getY();

                if ((tooltipString11.equals(tooltipString21) && tooltipString12.equals(tooltipString22))
                        || (tooltipString12.equals(tooltipString21) && tooltipString11.equals(tooltipString22))) {
                    double sdvig = 0.;
                    double sdvig_interf_name = 0.;
                    int width = (Integer) node.getAttribute("width");
                    if (num_overlaps % 2 != 0) {
                        sdvig = num_overlaps / 2 + width * 4.;
                    } else {
                        sdvig = (-1) * num_overlaps / 2 + width * 4.;
                    }
                    nodes = (ArrayList) node.getAttribute("interf_name");
                    PText interf_name = (PText) nodes.get(0);
                    int size_text = interf_name.getFont().getSize();
                    if (num_overlaps % 2 != 0) {
                        sdvig_interf_name = num_overlaps / 2 + size_text;
                    } else {
                        sdvig_interf_name = (-1) * num_overlaps / 2 + size_text;
                    }

                    if (Math.abs(x12 - x11) >= Math.abs(y12 - y11)) {
                        node.translate(0., sdvig);
                        interf_name.translate(0., sdvig_interf_name);
                    } else {
                        node.translate(sdvig, 0.);
                        interf_name.translate(sdvig_interf_name, 0.);
                    }
                    num_overlaps++;
                }
            }
        }

    }

    public static void SetAllToScreen() {
        double min_x = 1000000.;
        double min_y = 1000000.;
        double max_x = -1000000.;
        double max_y = -1000000.;
        for (int i = 0; i < Main.nodeLayer.getChildrenCount(); i++) {
            PNode node = Main.nodeLayer.getChild(i);
            if (node.getFullBounds().getMinX() < min_x) {
                min_x = node.getFullBounds().getMinX();
            }
            if (node.getFullBounds().getMinY() < min_y) {
                min_y = node.getFullBounds().getMinY();
            }
            if (node.getFullBounds().getMaxX() > max_x) {
                max_x = node.getFullBounds().getMaxX();
            }
            if (node.getFullBounds().getMaxY() > max_y) {
                max_y = node.getFullBounds().getMaxY();
            }
        }

        double w_start = Main.canvas.getCamera().getFullBounds().getWidth();
        double h_start = Main.canvas.getCamera().getFullBounds().getHeight();
        double w_stop = max_x - min_x;
        double h_stop = max_y - min_y;
        if (w_start / w_stop < h_start / h_stop) {
            Main.camera.setViewScale(w_start / w_stop);
            Main.view_scale = w_start / w_stop;
        } else {
            Main.camera.setViewScale(h_start / h_stop);
            Main.view_scale = h_start / h_stop;
        }

        PBounds bound = new PBounds();
        bound.setRect(min_x - 20, min_y - 20, max_x - min_x + 40, max_y - min_y + 40);
        Main.canvas.getCamera().animateViewToCenterBounds(bound, true, 500);
        Main.canvas.getCamera().repaint();
    }

    protected void addNodeContextMenu() {
        Main.contextNodeMenu = new JPopupMenu();

        Main.contextNodeMenu.add(new AbstractAction("Information ...") {
            public void actionPerformed(ActionEvent e) {
                getPropertiesNode();
            }
        });
        Main.contextNodeMenu.add(new AbstractAction("Telnet") {
            public void actionPerformed(ActionEvent e) {
                if (Main.node_sel != null) {
                    String ip = (String) Main.node_sel.getAttribute("tooltip");
                    TelnetClient m_TelnetClient = new TelnetClient();
                    m_TelnetClient.RunTelnet(ip);
                }
            }
        });
        Main.contextNodeMenu.add(new AbstractAction("Properties ...") {
            public void actionPerformed(ActionEvent e) {
                propertiesNode();
            }
        });

    }

    protected void addNodeContextMenuView() {
        Main.contextNodeMenuView = new JPopupMenu();
        Main.contextNodeMenuView.add(new AbstractAction("Information ...") {
            public void actionPerformed(ActionEvent e) {
                getPropertiesNode();
            }
        });
        if (Main.node_sel.getAttribute("coord") != null) {
            Main.contextNodeMenuView.add(new AbstractAction("Show on map") {
                public void actionPerformed(ActionEvent e) {
                    if (Main.node_sel != null && Main.node_sel.getAttribute("coord") != null) {
                        try {
                            String ref = (String) Main.node_sel.getAttribute("coord");
                            Desktop.getDesktop().browse(new URI(ref));
                        } catch (Exception ee) {
                        }
                    }
                }
            });
        }
        Main.contextNodeMenuView.add(new AbstractAction("Telnet") {
            public void actionPerformed(ActionEvent e) {
                if (Main.node_sel != null) {
                    String ip = (String) Main.node_sel.getAttribute("tooltip");
                    TelnetClient m_TelnetClient = new TelnetClient();
                    m_TelnetClient.RunTelnet(ip);
                }
            }
        });
    }

    protected void addCanvasContextMenu() {
        Main.contextCanvasMenu = new JPopupMenu();
        Main.contextCanvasMenu.add(new AbstractAction("Add Text") {
            public void actionPerformed(ActionEvent e) {
                Point2D pt;
                pt = Main.canvas.getRoot().getDefaultInputManager().getCurrentCanvasPosition();
                pt = Main.canvas.getCamera().localToView(pt);
                addText(pt.getX(), pt.getY());
            }
        });

    }

    protected void addTextCustomContextMenu() {
        Main.contextTextCustomMenu = new JPopupMenu();
        Main.contextTextCustomMenu.add(new AbstractAction("Remove") {
            public void actionPerformed(ActionEvent e) {
                removeText();
            }
        });
        Main.contextTextCustomMenu.add(new AbstractAction("Properties ...") {
            public void actionPerformed(ActionEvent e) {
                propertiesText();
            }
        });
    }

    protected void getPropertiesNode() {
        if (Main.node_sel != null) {
            GetNodePropertiesForm.createGetNodePropertiesForm();
        }
    }

    protected void addLink() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (Main.node_sel != null) {
                    ArrayList texts = (ArrayList) Main.node_sel.getAttribute("text");
                    if (texts != null) {
                        PText text_sel = (PText) texts.get(0);
                        String name_node = text_sel.getText();
                        AddLinkForm.createAddLinkForm(name_node);
                    } else {
                        AddLinkForm.createAddLinkForm(null);
                    }
                } else {
                    AddLinkForm.createAddLinkForm(null);
                }
            }
        });
    }

    protected void removeNode() {
        Frame frame = Main.getFrames()[0];
        int n = JOptionPane.showConfirmDialog(frame, "Would you remove this node ?", "Remove node ...", JOptionPane.YES_NO_OPTION);
        if (n == 0) {
            Main.node_sel.removeFromParent();
            ArrayList texts = (ArrayList) Main.node_sel.getAttribute("text");
            PText text_sel = (PText) texts.get(0);
            text_sel.removeFromParent();
            ArrayList edges = (ArrayList) Main.node_sel.getAttribute("edges");
            for (int i = 0; i < edges.size(); i++) {
                PPath edge = (PPath) edges.get(i);
                ArrayList nodes = (ArrayList) edge.getAttribute("interf_name");
                PText interf_name = (PText) nodes.get(0);
                interf_name.removeFromParent();
                edge.removeFromParent();
            }
            Main.leftLineSelectNode.setVisible(false);
            Main.rightLineSelectNode.setVisible(false);
            Main.topLineSelectNode.setVisible(false);
            Main.bottomLineSelectNode.setVisible(false);
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    protected void propertiesNode() {
        if (Main.node_sel != null) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    SetNodePropertiesForm m_SetNodePropertiesForm = new SetNodePropertiesForm();
                    m_SetNodePropertiesForm.createSetNodePropertiesForm();
                }
            });

        }
    }

    protected void propertiesLink() {
        SetLinkPropertiesForm.createSetLinkPropertiesForm();
    }

    protected void removeLink() {
        Frame frame = Main.getFrames()[0];
        int n = JOptionPane.showConfirmDialog(frame, "Would you remove this link ?", "Remove link ...", JOptionPane.YES_NO_OPTION);
        if (n == 0) {
            ArrayList nodes = (ArrayList) Main.node_sel.getAttribute("interf_name");
            PText interf_name = (PText) nodes.get(0);
            interf_name.removeFromParent();
            Main.node_sel.removeFromParent();
            Main.leftLineSelectNode.setVisible(false);
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    protected void addNode(double x, double y) {
        PImage image = new PImage(Main.nodeLayer.toImage(100, 100, null));
        String path_image = Main.default_image;
        image.setImage(path_image);
        image.setX(x);
        image.setY(y);
        ArrayList tmp = new ArrayList();
        image.addAttribute("edges", tmp);
        tmp = new ArrayList();
        tmp.add(image);
        image.addAttribute("pimage", tmp);
        image.addAttribute("tooltip", "Unknown node");
        image.addAttribute("path_image", path_image);
        image.addAttribute("custom", "custom");

        Main.nodeLayer.addChild(image);

        PText text = new PText();
        text.setText("Unknown node");
        Point2D.Double pbound = (Point2D.Double) image.getBounds().getCenter2D();
        double w = image.getWidth();
        double h = image.getHeight();
        double wt = text.getWidth();
        double ht = text.getHeight();
        double xx = pbound.getX();
        double yy = pbound.getY();
        text.setX(xx - wt / 2.);
        text.setY(yy + ht / 2. + 10);

        tmp = new ArrayList();
        tmp.add(text);
        image.addAttribute("text", tmp);
        tmp = new ArrayList();
        tmp.add(text);
        text.addAttribute("text", tmp);

        Main.textLayer.addChild(text);
        Main.canvas.repaint();
        image.repaint();
        text.repaint();
        Main.isChanged = true;
        ControlPanel.jButton1.setEnabled(true);
    }

    protected void addText(double x, double y) {
        Frame frame = Main.getFrames()[0];
        String s = JOptionPane.showInputDialog(frame, "Please enter text: ", "Add text", JOptionPane.QUESTION_MESSAGE);
        if (s != null) {
            PText text = new PText();
            text.setX(x);
            text.setY(y);
            text.setText(s);
            ArrayList tmp = new ArrayList();
            tmp.add(text);
            text.addAttribute("text", tmp);
            Main.textCustomLayer.addChild(text);
            Main.textLayer.repaint();
            text.repaint();
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    protected void propertiesText() {
        if (Main.node_sel != null) {
            ArrayList texts = (ArrayList) Main.node_sel.getAttribute("text");
            PText text = (PText) texts.get(0);

            Frame frame = Main.getFrames()[0];
            String s = JOptionPane.showInputDialog(frame, "Please enter text size: ", "Set text properties", JOptionPane.QUESTION_MESSAGE);
            if (s != null) {
                Font font = new Font("Arial", Font.PLAIN, Integer.parseInt(s));
                text.setFont(font);
            }
            text.repaint();
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    protected void removeText() {
        Frame frame = Main.getFrames()[0];
        int n = JOptionPane.showConfirmDialog(frame, "Would you remove this text ?", "Remove text ...", JOptionPane.YES_NO_OPTION);
        if (n == 0) {
            Main.node_sel.removeFromParent();
            Main.leftLineSelectNode.setVisible(false);
            Main.isChanged = true;
            ControlPanel.jButton1.setEnabled(true);
        }
    }

    private static boolean WriteToServer(String url, Object val) {
        boolean result = true;
        Gson gson = new Gson();
        String str_out = gson.toJson(val);
        if (HTTPRequestPOST(url, str_out).equals("")) {
            System.out.println("Error post request: url - " + url + " val - " + str_out);
            result = false;
        }
        return result;
    }

    private static boolean WriteChunkToServer(String url, String str, int limit_size) {
        boolean result = true;
        String out = "";
        int length = 0;
        for (String line : str.split("\n")) {
            out = out + line + "\n";
            length = length + line.length();
            if (length > limit_size) {
                if (HTTPRequestPOST(url, out).equals("")) {
                    System.out.println("Error post CHUNK request: url - " + url + " val - " + out);
                    result = false;
                }
//                System.out.println(out);
//                System.out.println("------------------------------------------");
                out = "";
                length = 0;
            }
        }
        if (!out.equals("")) {
            if (HTTPRequestPOST(url, out).equals("")) {
                System.out.println("Error post request: url - " + url + " val - " + out);
                result = false;
            }
//            System.out.println("Ostatok: "+out);
        }
        return result;
    }

    protected static void SaveChanged(String map_filename, String area) {
        Main.isBusy = true;
        WaitCircleApplicationIcon waitCircleApplicationIcon = new WaitCircleApplicationIcon();
        waitCircleApplicationIcon.end = false;
        waitCircleApplicationIcon.start(); 
        Main.control_panel.SetDisable();
        Main.time_machine.SetDisable();        
        
        // saved nodes
//        String url = "http://"+Main.neb_server+":"+Main.neb_server_port+"/set?file="+map_filename+"&key=/"+area;
        String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/setchunk?file=" + map_filename;
        Gson gson = new Gson();
        String key_prefix = "/" + area + "/nodes_information/";
        String out = "";
        for (int i = 0; i < Main.nodeLayer.getChildrenCount(); i++) {
            PNode node = Main.nodeLayer.getChild(i);
            if (node != null) {
                String ip = ((String) node.getAttribute("tooltip"));
                String path_image = (String) node.getAttribute("path_image");
                ArrayList<String> xy = new ArrayList();
                String x = String.valueOf(node.getFullBounds().getX());
                String y = String.valueOf(node.getFullBounds().getY());
                xy.add(x);
                xy.add(y);

                ArrayList<String> size = new ArrayList();
                String width = String.valueOf(node.getFullBounds().getWidth());
                String hight = String.valueOf(node.getFullBounds().getHeight());
                size.add(width);
                size.add(hight);

                ArrayList<String> xy_old = (ArrayList<String>) Main.utils.GetKey("/nodes_information/" + ip + "/xy", Main.INFORMATION);
                if (xy_old != null && xy_old.size() == 2) {
                    if (!xy_old.get(0).equals(x) || !xy_old.get(1).equals(y)) {
                        String key = key_prefix + ip + "/xy;" + gson.toJson(xy) + "\n";
                        out = out + key;
                        System.out.println("Update - " + "/nodes_information/" + ip + "/xy");
                    }
                } else {
                    String key = key_prefix + ip + "/xy;" + gson.toJson(xy) + "\n";
                    out = out + key;
                    System.out.println("Update - " + "/nodes_information/" + ip + "/xy");
                }
            }
        }
        
        key_prefix = "/" + area + "/texts/";
        for (int i = 0; i < Main.textCustomLayer.getChildrenCount(); i++) {
            PNode node_text = Main.textCustomLayer.getChild(i);
            if (node_text != null) {
                ArrayList<String> xy = new ArrayList();
                String x = String.valueOf(node_text.getFullBounds().getX());
                String y = String.valueOf(node_text.getFullBounds().getY());
                xy.add(x);
                xy.add(y);
                String text = ((String) node_text.getAttribute("text"));
                
                if(text != null) {
                    ArrayList<String> xy_old = (ArrayList<String>) Main.utils.GetKey("/texts/" + text + "/xy", Main.INFORMATION);
                    if (xy_old != null && xy_old.size() == 2) {
                        if (!xy_old.get(0).equals(x) || !xy_old.get(1).equals(y)) {
                            String key = key_prefix + text + "/xy;" + gson.toJson(xy) + "\n";
                            out = out + key;
                            System.out.println("Update - " + "/texts/" + text + "/xy");
                        }
                    } else {
                        String key = key_prefix + text + "/xy;" + gson.toJson(xy) + "\n";
                        out = out + key;
                        System.out.println("Update - " + "/texts/" + text + "/xy");
                    }
                }
            }
          
        }
        
//        System.out.println(out);
        WriteChunkToServer(url, out, 1000);

        String res = HTTPRequestGET("http://" + Main.neb_server + ":" + Main.neb_server_port + "/commit");

        Main.isChanged = false;
        ControlPanel.jButton1.setEnabled(false);

        WriteStrToFile(Main.dump, String.valueOf(System.currentTimeMillis()));
        CheckerMapFile.time_start = System.currentTimeMillis();
//        System.out.println("time_start="+time_start);

        waitCircleApplicationIcon.end = true;
        Main.control_panel.SetEnable();
        Main.time_machine.SetEnable();
        Main.isBusy = false;
        
        WatchMapFile.my_changed = Main.map_filename;
    }

    public static void updateToolTip(double x, double y, String text, String mode) {
        int otstup = 3;
        String like = null;

        PText tooltipText = new PText();
        tooltipText.setText(text);
        double width = tooltipText.getWidth() + 2. * otstup;
        double height = tooltipText.getHeight() + 2. * otstup;

        double x1 = 0.;
        double y1 = 0.;
        double x2 = Main.canvas.getWidth();
        double y2 = Main.canvas.getHeight();

        if (mode.equals("top")) {
            if (x - 2. * width / 3. >= x1 && y - 2. * height >= y1 && x + width / 3. <= x2 && y <= y2) {
                like = "top";  // check top
            } else if (x - 2. * width / 3. >= x1 && y >= y1 && x + width / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottom";  // check bottom
            } else if (x >= x1 && y - 2. * height >= y1 && x + width <= x2 && y <= y2) {
                like = "left"; // check left
            } else if (x - width >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "right"; // check right
            } else {
                like = "top";
            }
        }
        if (mode.equals("bottom")) {
            if (x - 2. * width / 3. >= x1 && y >= y1 && x + width / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottom";  // check bottom
            } else if (x - 2. * width / 3. >= x1 && y - 2. * height >= y1 && x + width / 3. <= x2 && y <= y2) {
                like = "top";  // check top
            } else if (x >= x1 && y - 2. * height >= y1 && x + width <= x2 && y <= y2) {
                like = "left"; // check left
            } else if (x - width >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "right"; // check right
            } else {
                like = "bottom";
            }
        }
        if (mode.equals("left")) {
            double widthleft = 4. * width / 3.;
            if (x >= x1 && y - 2. * height >= y1 && x + widthleft <= x2 && y <= y2) {
                like = "left"; // check left
            } else if (x - widthleft >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "right"; // check right
            } else if (x - 2. * widthleft / 3. >= x1 && y >= y1 && x + widthleft / 3. <= x2 && y + height <= y2) {
                like = "bottom";  // check bottom
            } else if (x - 2. * widthleft / 3. >= x1 && y - 2. * height >= y1 && x + widthleft / 3. <= x2 && y <= y2) {
                like = "top";  // check top
            } else {
                like = "left";
            }
        }
        if (mode.equals("right")) {
            double widthright = 4. * width / 3.;
            if (x - widthright >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "right"; // check right
            } else if (x >= x1 && y - 2. * height >= y1 && x + widthright <= x2 && y <= y2) {
                like = "left"; // check left
            } else if (x - 2. * widthright / 3. >= x1 && y >= y1 && x + widthright / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottom";  // check bottom
            } else if (x - 2. * widthright / 3. >= x1 && y - 2. * height >= y1 && x + widthright / 3. <= x2 && y <= y2) {
                like = "top";  // check top
            } else {
                like = "right";
            }
        }
        if (mode.equals("topsec")) {
            if (x - 2. * width / 3. >= x1 && y - 2. * height >= y1 && x + width / 3. <= x2 && y <= y2) {
                like = "topsec";  // check top
            } else if (x - 2. * width / 3. >= x1 && y >= y1 && x + width / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottomsec";  // check bottom
            } else if (x >= x1 && y - 2. * height >= y1 && x + width <= x2 && y <= y2) {
                like = "leftsec"; // check left
            } else if (x - width >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "rightsec"; // check right
            } else {
                like = "topsec";
            }
        }
        if (mode.equals("bottomsec")) {
            if (x - 2. * width / 3. >= x1 && y >= y1 && x + width / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottomsec";  // check bottom
            } else if (x - 2. * width / 3. >= x1 && y - 2. * height >= y1 && x + width / 3. <= x2 && y <= y2) {
                like = "topsec";  // check top
            } else if (x >= x1 && y - 2. * height >= y1 && x + width <= x2 && y <= y2) {
                like = "leftsec"; // check left
            } else if (x - width >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "rightsec"; // check right
            } else {
                like = "bottomsec";
            }
        }
        if (mode.equals("leftsec")) {
            double widthleft = 4. * width / 3.;
            if (x >= x1 && y - 2. * height >= y1 && x + widthleft <= x2 && y <= y2) {
                like = "leftsec"; // check left
            } else if (x - widthleft >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "rightsec"; // check right
            } else if (x - 2. * widthleft / 3. >= x1 && y >= y1 && x + widthleft / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottomsec";  // check bottom
            } else if (x - 2. * widthleft / 3. >= x1 && y - 2. * height >= y1 && x + widthleft / 3. <= x2 && y <= y2) {
                like = "topsec";  // check top
            } else {
                like = "leftsec";
            }
        }
        if (mode.equals("rightsec")) {
            double widthright = 4. * width / 3.;
            if (x - widthright >= x1 && y - 2. * height >= y1 && x <= x2 && y <= y2) {
                like = "rightsec"; // check right
            } else if (x >= x1 && y - 2. * height >= y1 && x + widthright <= x2 && y <= y2) {
                like = "leftsec"; // check left
            } else if (x - 2. * widthright / 3. >= x1 && y >= y1 && x + widthright / 3. <= x2 && y + 2. * height <= y2) {
                like = "bottomsec";  // check bottom
            } else if (x - 2. * widthright / 3. >= x1 && y - 2. * height >= y1 && x + widthright / 3. <= x2 && y <= y2) {
                like = "topsec";  // check top
            } else {
                like = "rightsec";
            }
        }

        if (like.equals("top")) {
            PPath topArea = new PPath.Double();
            topArea = (PPath) Main.camera.getChild(0);
            PText tooltipTopText = (PText) topArea.getChild(0);
            tooltipTopText.setText(text);
            tooltipTopText.setX(x - 2. * width / 3. + otstup);
            tooltipTopText.setY(y - 2. * height + otstup);
            topArea.setWidth(width);
            topArea.setHeight(2. * height);
            topArea.setX(x - 2. * width / 3.);
            topArea.setY(y - 2. * height);
            topArea.setVisible(true);
        }
        if (like.equals("bottom")) {
            PPath bottomArea = new PPath.Double();
            bottomArea = (PPath) Main.camera.getChild(1);
            PText tooltipBottomText = (PText) bottomArea.getChild(0);
            tooltipBottomText.setText(text);
            tooltipBottomText.setX(x - 2. * width / 3. + otstup);
            tooltipBottomText.setY(y + height + otstup);
            bottomArea.setWidth(width);
            bottomArea.setHeight(2. * height);
            bottomArea.setX(x - 2. * width / 3.);
            bottomArea.setY(y);
            bottomArea.setVisible(true);
        }
        if (like.equals("left")) {
            PPath leftArea = new PPath.Double();
            leftArea = (PPath) Main.camera.getChild(2);
            PText tooltipLeftText = (PText) leftArea.getChild(0);
            tooltipLeftText.setText(text);
            tooltipLeftText.setX(x + width / 3. + otstup);
            tooltipLeftText.setY(y - 2. * height + otstup);
            leftArea.setWidth(4. * width / 3.);
            leftArea.setHeight(2. * height);
            leftArea.setX(x);
            leftArea.setY(y - 2. * height);
            leftArea.setVisible(true);
        }
        if (like.equals("right")) {
            PPath rightArea = new PPath.Double();
            rightArea = (PPath) Main.camera.getChild(3);
            PText tooltipRightText = (PText) rightArea.getChild(0);
            tooltipRightText.setText(text);
            tooltipRightText.setX(x - 4. * width / 3. + otstup);
            tooltipRightText.setY(y - 2. * height + otstup);
            rightArea.setWidth(4. * width / 3);
            rightArea.setHeight(2. * height);
            rightArea.setX(x - 4. * width / 3.);
            rightArea.setY(y - 2. * height);
            rightArea.setVisible(true);
        }
        if (like.equals("topsec")) {
            PPath topAreaSec = new PPath.Double();
            topAreaSec = (PPath) Main.camera.getChild(4);
            PText tooltipTopTextSec = (PText) topAreaSec.getChild(0);
            tooltipTopTextSec.setText(text);
            tooltipTopTextSec.setX(x - 2. * width / 3. + otstup);
            tooltipTopTextSec.setY(y - 2. * height + otstup);
            topAreaSec.setWidth(width);
            topAreaSec.setHeight(2. * height);
            topAreaSec.setX(x - 2. * width / 3.);
            topAreaSec.setY(y - 2. * height);
            topAreaSec.setVisible(true);
        }
        if (like.equals("bottomsec")) {
            PPath bottomAreaSec = new PPath.Double();
            bottomAreaSec = (PPath) Main.camera.getChild(5);
            PText tooltipBottomTextSec = (PText) bottomAreaSec.getChild(0);
            tooltipBottomTextSec.setText(text);
            tooltipBottomTextSec.setX(x - 2. * width / 3. + otstup);
            tooltipBottomTextSec.setY(y + height + otstup);
            bottomAreaSec.setWidth(width);
            bottomAreaSec.setHeight(2. * height);
            bottomAreaSec.setX(x - 2. * width / 3.);
            bottomAreaSec.setY(y);
            bottomAreaSec.setVisible(true);
        }
        if (like.equals("leftsec")) {
            PPath leftAreaSec = new PPath.Double();
            leftAreaSec = (PPath) Main.camera.getChild(6);
            PText tooltipLeftTextSec = (PText) leftAreaSec.getChild(0);
            tooltipLeftTextSec.setText(text);
            tooltipLeftTextSec.setX(x + width / 3. + otstup);
            tooltipLeftTextSec.setY(y - 2. * height + otstup);
            leftAreaSec.setWidth(4. * width / 3.);
            leftAreaSec.setHeight(2. * height);
            leftAreaSec.setX(x);
            leftAreaSec.setY(y - 2. * height);
            leftAreaSec.setVisible(true);
        }
        if (like.equals("rightsec")) {
            PPath rightAreaSec = new PPath.Double();
            rightAreaSec = (PPath) Main.camera.getChild(7);
            PText tooltipRightTextSec = (PText) rightAreaSec.getChild(0);
            tooltipRightTextSec.setText(text);
            tooltipRightTextSec.setX(x - 4. * width / 3. + otstup);
            tooltipRightTextSec.setY(y - 2. * height + otstup);
            rightAreaSec.setWidth(4. * width / 3);
            rightAreaSec.setHeight(2. * height);
            rightAreaSec.setX(x - 4. * width / 3.);
            rightAreaSec.setY(y - 2. * height);
            rightAreaSec.setVisible(true);
        }

    }

    public static void hideFlashLinks() {
//        System.out.println("Running function hideFlasLinks.");
        PPath flash1 = (PPath) Main.flashLayer.getChild(0);
        flash1.setVisible(false);
        flash1.repaint();
        PPath flash2 = (PPath) Main.flashLayer.getChild(1);
        flash2.setVisible(false);
        flash2.repaint();
        Main.camera.repaint();
        Main.canvas.repaint();
    }

    public static void showFlashLinks() {
        PPath flash1 = (PPath) Main.flashLayer.getChild(0);
        flash1.setVisible(true);
        flash1.repaint();
        PPath flash2 = (PPath) Main.flashLayer.getChild(1);
        flash2.setVisible(true);
        flash2.repaint();
        Main.camera.repaint();
    }

    public static void hideToolTip() {
        PPath topArea = new PPath.Double();
        topArea = (PPath) Main.camera.getChild(0);
        topArea.setVisible(false);
        PPath bottomArea = new PPath.Double();
        bottomArea = (PPath) Main.camera.getChild(1);
        bottomArea.setVisible(false);
        PPath leftArea = new PPath.Double();
        leftArea = (PPath) Main.camera.getChild(2);
        leftArea.setVisible(false);
        PPath rightArea = new PPath.Double();
        rightArea = (PPath) Main.camera.getChild(3);
        rightArea.setVisible(false);
        PPath topAreaSecond = new PPath.Double();
        topAreaSecond = (PPath) Main.camera.getChild(4);
        topAreaSecond.setVisible(false);
        PPath bottomAreaSecond = new PPath.Double();
        bottomAreaSecond = (PPath) Main.camera.getChild(5);
        bottomAreaSecond.setVisible(false);
        PPath leftAreaSecond = new PPath.Double();
        leftAreaSecond = (PPath) Main.camera.getChild(6);
        leftAreaSecond.setVisible(false);
        PPath rightAreaSecond = new PPath.Double();
        rightAreaSecond = (PPath) Main.camera.getChild(7);
        rightAreaSecond.setVisible(false);
    }

    public static void repaintText() {
        Main.view_scale = Main.camera.getViewScale();
        for (int i = 0; i < Main.textLayer.getChildrenCount(); i++) {
            PNode text = Main.textLayer.getChild(i);
            if (text.getHeight() * Main.view_scale > 6) {
                text.setVisible(true);
                text.setPickable(true);
            } else {
                text.setVisible(false);
                text.setPickable(false);
            }
        }
        for (int i = 0; i < Main.textCustomLayer.getChildrenCount(); i++) {
            PNode text = Main.textCustomLayer.getChild(i);
            if (text.getHeight() * Main.view_scale > 6) {
                text.setVisible(true);
                text.setPickable(true);
            } else {
                text.setVisible(false);
                text.setPickable(false);
            }
        }
    }

    public static void repaintInterfaceName() {
        Main.view_scale = Main.camera.getViewScale();
        for (int i = 0; i < Main.interfnameLayer.getChildrenCount(); i++) {
            PNode text = Main.interfnameLayer.getChild(i);
            if (text.getHeight() * Main.view_scale > 6) {
                text.setVisible(true);
                text.setPickable(true);
            } else {
                text.setVisible(false);
                text.setPickable(false);
            }

        }

    }

    public static void updateConturNode(PInputEvent e, boolean visible) {
        if (visible) {
            double scale = Main.camera.getViewScale();
            PNode node = e.getPickedNode();

            ArrayList tmp = new ArrayList();
            tmp.add(node);
            Main.leftLineSelectNode.addAttribute("node", tmp);

            PBounds contur = node.getFullBounds();
            double x1 = contur.getMinX();
            double y1 = contur.getMinY();
            double x2 = contur.getMaxX();
            double y2 = contur.getMaxY();

            Main.leftLineSelectNode.reset();
            Main.leftLineSelectNode.setStroke(new BasicStroke(1));
            Main.leftLineSelectNode.moveTo((float) x1 - 3, (float) y1 - 3);
            Main.leftLineSelectNode.lineTo((float) x1 - 3, (float) y2 + 3);
            Main.leftLineSelectNode.setVisible(true);
            Main.leftLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.leftLineSelectNode.repaint();
            Main.leftLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
            Main.rightLineSelectNode.reset();
            Main.rightLineSelectNode.moveTo((float) x2 + 3, (float) y1 - 3);
            Main.rightLineSelectNode.lineTo((float) x2 + 3, (float) y2 + 3);
            Main.rightLineSelectNode.setVisible(true);
            Main.rightLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.rightLineSelectNode.repaint();
            Main.rightLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
            Main.topLineSelectNode.reset();
            Main.topLineSelectNode.moveTo((float) x1 - 3, (float) y1 - 3);
            Main.topLineSelectNode.lineTo((float) x2 + 3, (float) y1 - 3);
            Main.topLineSelectNode.setVisible(true);
            Main.topLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.topLineSelectNode.repaint();
            Main.topLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
            Main.bottomLineSelectNode.reset();
            Main.bottomLineSelectNode.moveTo((float) x1 - 3, (float) y2 + 3);
            Main.bottomLineSelectNode.lineTo((float) x2 + 3, (float) y2 + 3);
            Main.bottomLineSelectNode.setVisible(true);
            Main.bottomLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.bottomLineSelectNode.repaint();
            Main.bottomLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
        } else {
            Main.leftLineSelectNode.setVisible(false);
            Main.rightLineSelectNode.setVisible(false);
            Main.topLineSelectNode.setVisible(false);
            Main.bottomLineSelectNode.setVisible(false);
        }
    }

    public static void paintToolTipNode(PInputEvent e) {
        PNode n = e.getInputManager().getMouseOver().getPickedNode();
        String tooltipString = (String) n.getAttribute("tooltip_full");
        if (tooltipString != null) {
            Point2D p = e.getCanvasPosition();
            e.getPath().canvasToLocal(p, Main.camera);
            updateToolTip(p.getX(), p.getY(), tooltipString, "top");
        }
    }

    public static void GetLinkAttribute(PInputEvent e) {
        PNode n = e.getInputManager().getMouseOver().getPickedNode();
        if (n != null) {
            ArrayList coordinate = (ArrayList) n.getAttribute("coordinate");
            if (coordinate != null) {
                Main.link_point1 = (Point2D) coordinate.get(0);
                Main.link_point2 = (Point2D) coordinate.get(1);
            }
            ArrayList nodes = (ArrayList) n.getAttribute("nodes");
            if (nodes != null) {
                Main.link_node1 = (PNode) nodes.get(0);
                Main.link_node2 = (PNode) nodes.get(1);
            }
        }
    }

    public static double[] GetFlashPoints(PInputEvent e, double scale) {
        double MIN_VIEW_RAZMER_FLASH = 7;
        double MAX_VIEW_RAZMER_FLASH = 12;
        double[] out = new double[5];
        out[0] = 999999999;
        out[1] = 999999999;
        out[2] = 999999999;
        out[3] = 999999999;
        out[4] = 999999999;

        if (e != null) {
            GetLinkAttribute(e);
        }

        boolean flashing_link = false;
        if (Main.link_node1 != null && Main.link_node2 != null
                && Main.link_point1 != null && Main.link_point2 != null) {
            flashing_link = true;
        } else {
            flashing_link = false;
        }

        if (!(e == null && !flashing_link)) {
            double s1 = (Main.link_node1.getWidth() + Main.link_node1.getHeight()) / 2;
            double s2 = (Main.link_node2.getWidth() + Main.link_node2.getHeight()) / 2;
            double radius = 0;
            if (s1 < s2) {
                radius = s1;
            } else {
                radius = s2;
            }
            if (radius * scale < MIN_VIEW_RAZMER_FLASH) {
                radius = MIN_VIEW_RAZMER_FLASH / scale;
            }
            if (radius * scale > MAX_VIEW_RAZMER_FLASH) {
                radius = MAX_VIEW_RAZMER_FLASH / scale;
            }
            //        double radius = Main.origRadiusFlashLink/scale;
            //        System.out.println("Scale - "+scale);
            out[4] = radius;

            if (Main.link_point1 != null && Main.link_point2 != null) {
                Point2D point1 = Main.link_point1;
                Point2D point2 = Main.link_point2;
                PNode node1 = Main.link_node1;
                PNode node2 = Main.link_node2;
                double node1x = node1.getFullBounds().getX();
                double node1y = node1.getFullBounds().getY();
                double node1w = node1.getFullBounds().getWidth();
                double node1h = node1.getFullBounds().getHeight();
                double node2x = node2.getFullBounds().getX();
                double node2y = node2.getFullBounds().getY();
                double node2w = node2.getFullBounds().getWidth();
                double node2h = node2.getFullBounds().getHeight();
                double[] coord = transformPoint(point2.getX(),
                        point1.getX(),
                        point2.getY(),
                        point1.getY(),
                        node1w, node1h);
                double[] coord1 = transformPointAmLine(point2.getX(),
                        point1.getX(),
                        point2.getY(),
                        point1.getY(),
                        coord[0], coord[1], radius * 0.707);
                out[0] = coord1[0] - radius / 2;
                out[1] = coord1[1] - radius / 2;
                coord = transformPoint(point1.getX(),
                        point2.getX(),
                        point1.getY(),
                        point2.getY(),
                        node2w, node2h);
                coord1 = transformPointAmLine(point1.getX(),
                        point2.getX(),
                        point1.getY(),
                        point2.getY(),
                        coord[0], coord[1], radius * 0.707);
                out[2] = coord1[0] - radius / 2;
                out[3] = coord1[1] - radius / 2;
            }
        }

        return out;
    }

    public static void UpdateFlash(PPath flash, double x, double y, double radius, double scale) {
        int width_stroke = (int) (2 / scale);
//        if(width_stroke < 2) width_stroke=2;
        flash.setStroke(new BasicStroke((float) width_stroke));
        flash.setWidth(radius);
        flash.setHeight(radius);
        flash.setX(x);
        flash.setY(y);
        flash.repaint();
        Main.flashLayer.repaint();
    }

    public static void RepaintFlash(PInputEvent e) {
        if (!Main.paint_flash_running) {
            double scale = Main.camera.getViewScale();
            double coord[] = Utils.GetFlashPoints(e, scale);
            double radius = coord[4];
            PPath flash1 = (PPath) Main.flashLayer.getChild(0);
            UpdateFlash(flash1, coord[0], coord[1], radius, scale);
            PPath flash2 = (PPath) Main.flashLayer.getChild(1);
            UpdateFlash(flash2, coord[2], coord[3], radius, scale);
        } else {
            Utils.StopAllFlashLinkProcesses();
            Utils.hideFlashLinks();
        }
    }

    public static void paintFlashLinks(PInputEvent e) {
        String ifOperStatus = "1.3.6.1.2.1.2.2.1.8";

        Main.paint_flash_running = true;
        Color up = new Color(76, 255, 0);
        Color down = new Color(38, 127, 0);

        double scale = Main.camera.getViewScale();
//        System.out.println("scale="+scale);
        PNode n = e.getInputManager().getMouseOver().getPickedNode();
        String tooltipString = (String) n.getAttribute("tooltip");
        if (tooltipString != null) {
            String[] str = tooltipString.split(" - ");
            if (str.length == 2) {
                str[0] = str[0].trim();
                str[1] = str[1].trim();
                String[] str1 = str[0].split(" ");
                String host1 = str1[0];
                String iface1 = str1[1];
                str1 = str[1].split(" ");
                String host2 = str1[0];
                String iface2 = str1[1];
                long period_oprosa = 10000;

                String index_iface = (String) n.getAttribute("index_iface");
                if (index_iface.contains(":")) {
                    String[] index = index_iface.split(":");
                    double coord[] = GetFlashPoints(e, scale);
                    double radius = coord[4];

                    ArrayList nodes = (ArrayList) n.getAttribute("nodes");
                    PNode node1 = (PNode) nodes.get(0);
                    PNode node2 = (PNode) nodes.get(1);

                    PPath flash1 = (PPath) Main.flashLayer.getChild(0);

                    String host = host1;
                    if (index.length >= 1) {
                        String iface_index = index[0];
                        ArrayList<ArrayList<String>> snmp_account = (ArrayList) node1.getAttribute("snmp_account");
                        String version = null;
                        String community = null;
                        for (ArrayList<String> it : snmp_account) {
                            if (it.get(0).equals("snmp")) {
                                version = it.get(2);
                                community = it.get(1);
                                break;
                            }
                        }
                        flash1.addAttribute("host", host);
                        flash1.addAttribute("iface_index", iface_index);
                        flash1.addAttribute("version", version);
                        flash1.addAttribute("community", community);
                        flash1.setVisible(false);

                        if (iface_index.matches("\\d+") && version != null && community != null) {
                            String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/snmpget";
                            String request_status = host + ";" + community + ";" + version + ";" + ifOperStatus + "." + iface_index;

                            String result = Utils.HTTPRequestPOST(url, request_status);
                            if (!result.equals("")) {
                                JSONParser parser = new JSONParser();
                                JSONObject jsonObject;
                                try {
                                    jsonObject = (JSONObject) parser.parse(result);

                                    Map response_map = Main.utils.toMap(jsonObject);
                                    if (response_map.get(host) != null && ((ArrayList) response_map.get(host)).get(0) != null) {
                                        String status = ((ArrayList<String>) ((ArrayList) response_map.get(host)).get(0)).get(1);
                                        if (status.equals("1")) {
                                            flash1.setPaint(up);
                                            flash1.addAttribute("mode", 1);
                                        } else {
                                            flash1.setPaint(down);
                                            flash1.addAttribute("mode", 0);
                                        }
                                        UpdateFlash(flash1, coord[0], coord[1], radius, scale);
                                        flash1.setVisible(true);
                                        flash1.repaint();
                                        Main.flashLayer.repaint();
                                    }
                                } catch (org.json.simple.parser.ParseException ex) {
                                    Logger.getLogger(GetNodePropertiesForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                System.out.println("Error post request: url - " + url + " val - " + request_status);
                            }
                        }
                    }

                    PPath flash2 = (PPath) Main.flashLayer.getChild(1);

                    host = host2;
                    if (index.length == 2) {
                        String iface_index = index[1];
                        ArrayList<ArrayList<String>> snmp_account = (ArrayList) node2.getAttribute("snmp_account");
                        String version = null;
                        String community = null;
                        for (ArrayList<String> it : snmp_account) {
                            if (it.get(0).equals("snmp")) {
                                version = it.get(2);
                                community = it.get(1);
                                break;
                            }
                        }
                        flash2.addAttribute("host", host);
                        flash2.addAttribute("iface_index", iface_index);
                        flash2.addAttribute("version", version);
                        flash2.addAttribute("community", community);
                        flash2.setVisible(false);

                        if (iface_index.matches("\\d+") && version != null && community != null) {
                            String url = "http://" + Main.neb_server + ":" + Main.neb_server_port + "/snmpget";
                            String request_status = host + ";" + community + ";" + version + ";" + ifOperStatus + "." + iface_index;

                            String result = Utils.HTTPRequestPOST(url, request_status);
                            if (!result.equals("")) {
                                JSONParser parser = new JSONParser();
                                JSONObject jsonObject;
                                try {
                                    jsonObject = (JSONObject) parser.parse(result);

                                    Map response_map = Main.utils.toMap(jsonObject);
                                    if (response_map.get(host) != null && ((ArrayList) response_map.get(host)).get(0) != null) {
                                        String status = ((ArrayList<String>) ((ArrayList) response_map.get(host)).get(0)).get(1);
                                        if (status.equals("1")) {
                                            flash2.setPaint(up);
                                            flash2.addAttribute("mode", 1);
                                        } else {
                                            flash2.setPaint(down);
                                            flash2.addAttribute("mode", 0);
                                        }
                                        UpdateFlash(flash2, coord[2], coord[3], radius, scale);
                                        flash2.setVisible(true);
                                        flash2.repaint();
                                        Main.flashLayer.repaint();
                                    }
                                } catch (org.json.simple.parser.ParseException ex) {
                                    Logger.getLogger(GetNodePropertiesForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } else {
                                System.out.println("Error post request: url - " + url + " val - " + request_status);
                            }
                        }
                    }
                    // show flas link
                    Main.paint_flash_running = false;

                    // paint flashing...
                    long in1 = -1;
                    long out1 = -1;
                    long in2 = -1;
                    long out2 = -1;
                    int mode1 = (Integer) (flash1.getAttribute("mode"));
                    if (mode1 == 1) {
                        Main.m_FlashColor1 = new FlashColor(flash1, new Color(76, 255, 0), new Color(38, 127, 0));
                        Main.m_FlashColor1.start();
                        long[] out = GetBytesInOut(flash1);
                        in1 = out[0];
                        out1 = out[1];
                    }
                    int mode2 = (Integer) (flash2.getAttribute("mode"));
                    if (mode2 == 1) {
                        Main.m_FlashColor2 = new FlashColor(flash2, new Color(76, 255, 0), new Color(38, 127, 0));
                        Main.m_FlashColor2.start();
                        long[] out = GetBytesInOut(flash2);
                        in2 = out[0];
                        out2 = out[1];
                    }
                    try {
                        Thread.sleep(period_oprosa);
                    } catch (InterruptedException ex) {
                    }

                    while (true) {
                        long in1_cur = -1;
                        long out1_cur = -1;
                        long in2_cur = -1;
                        long out2_cur = -1;

                        if (mode1 == 1) {
                            long[] out = GetBytesInOut(flash1);
                            in1_cur = out[0];
                            out1_cur = out[1];
                        }
                        if (mode2 == 1) {
                            long[] out = GetBytesInOut(flash2);
                            in2_cur = out[0];
                            out2_cur = out[1];
                        }
                        if (in1 != -1 && out1 != -1 && in1_cur != -1 && out1_cur != -1) {
                            long delta_in = in1_cur - in1;
                            long delta_out = out1_cur - out1;
                            double bytes_in_sec = 0;
                            if (delta_in > delta_out) {
                                bytes_in_sec = delta_in / period_oprosa;
                            } else {
                                bytes_in_sec = delta_out / period_oprosa;
                            }
                            Main.m_FlashColor1.time1 = 100;
                            Main.m_FlashColor1.time2 = 1000 - 100 * (long) (bytes_in_sec / 1024);
                            if (Main.m_FlashColor1.time2 <= 20) {
                                Main.m_FlashColor1.time2 = 20;
                            }
                            //                        System.out.println("flash1: bytes_in_sec="+bytes_in_sec+"\tactivnost_proc="+activnost_proc+" time1/time2="+Main.m_FlashColor1.time1+","+Main.m_FlashColor1.time2);
                        }
                        if (in2 != -1 && out2 != -1 && in2_cur != -1 && out2_cur != -1) {
                            long delta_in = in2_cur - in2;
                            long delta_out = out2_cur - out2;
                            double bytes_in_sec = 0;
                            if (delta_in > delta_out) {
                                bytes_in_sec = delta_in / period_oprosa;
                            } else {
                                bytes_in_sec = delta_out / period_oprosa;
                            }
                            Main.m_FlashColor2.time1 = 100;
                            Main.m_FlashColor2.time2 = 1000 - 100 * (long) (bytes_in_sec / 1024);
                            if (Main.m_FlashColor2.time2 <= 20) {
                                Main.m_FlashColor2.time2 = 20;
                            }
                            //                        System.out.println("flash2: bytes_in_sec="+bytes_in_sec+"\tactivnost_proc="+activnost_proc+" time1/time2="+Main.m_FlashColor2.time1+","+Main.m_FlashColor2.time2);
                        }
                        in1 = in1_cur;
                        out1 = out1_cur;
                        in2 = in2_cur;
                        out2 = out2_cur;
                        try {
                            Thread.sleep(period_oprosa);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }

    public static void transformFlashLinks(double offsetX, double offsetY, double scale) {
        PPath flash1 = (PPath) Main.camera.getChild(8);
        flash1.setX((flash1.getX() + offsetX) * scale);
        flash1.setY((flash1.getY() + offsetY) * scale);
        PPath flash2 = (PPath) Main.camera.getChild(9);
        flash2.setX((flash2.getX() + offsetX) * scale);
        flash2.setY((flash2.getY() + offsetY) * scale);
    }

    public static void paintToolTipEdge(PInputEvent e) {
        String[] node = new String[2];
        double scale = Main.camera.getViewScale();
        PNode n = e.getInputManager().getMouseOver().getPickedNode();
        String tooltipString = (String) n.getAttribute("tooltip");
        Point2D p = e.getCanvasPosition();
        e.getPath().canvasToLocal(p, Main.edgeLayer);
        Point2D p_canvas = e.getCanvasPosition();
        e.getPath().canvasToLocal(p_canvas, Main.camera);
        double deltax = p_canvas.getX() / scale - p.getX();
        double deltay = p_canvas.getY() / scale - p.getY();

        ArrayList coordinate = (ArrayList) n.getAttribute("coordinate");
        if (coordinate != null) {
            Point2D point1 = (Point2D) coordinate.get(0);
            Point2D point2 = (Point2D) coordinate.get(1);
            ArrayList nodes = (ArrayList) n.getAttribute("nodes");
            PNode node1 = (PNode) nodes.get(0);
            PNode node2 = (PNode) nodes.get(1);
            double node1x = node1.getFullBounds().getX();
            double node1y = node1.getFullBounds().getY();
            double node1w = node1.getFullBounds().getWidth();
            double node1h = node1.getFullBounds().getHeight();
            double node2x = node2.getFullBounds().getX();
            double node2y = node2.getFullBounds().getY();
            double node2w = node2.getFullBounds().getWidth();
            double node2h = node2.getFullBounds().getHeight();
            if (tooltipString != null && tooltipString.contains(" - ")) {
                node = tooltipString.split(" - ");
                node[0] = node[0].trim();
                node[1] = node[1].trim();
                PText text1 = (PText) node1.getAttribute("text");
                String nodename1 = text1.getText().split(" - ")[0];
                PText text2 = (PText) node2.getAttribute("text");
                String nodename2 = text2.getText().split(" - ")[0];
                PPath topArea = new PPath.Double();
                topArea = (PPath) Main.camera.getChild(0);
                PPath bottomArea = new PPath.Double();
                bottomArea = (PPath) Main.camera.getChild(1);
                PPath leftArea = new PPath.Double();
                leftArea = (PPath) Main.camera.getChild(2);
                PPath rightArea = new PPath.Double();
                rightArea = (PPath) Main.camera.getChild(3);
                PPath topAreaSecond = new PPath.Double();
                topAreaSecond = (PPath) Main.camera.getChild(4);
                PPath bottomAreaSecond = new PPath.Double();
                bottomAreaSecond = (PPath) Main.camera.getChild(5);
                PPath leftAreaSecond = new PPath.Double();
                leftAreaSecond = (PPath) Main.camera.getChild(6);
                PPath rightAreaSecond = new PPath.Double();
                rightAreaSecond = (PPath) Main.camera.getChild(7);

                if (point1.getX() >= node1x && point1.getX() <= node1x + node1w
                        && point1.getY() >= node1y && point1.getY() <= node1y + node1h
                        && point2.getX() >= node2x && point2.getX() <= node2x + node2w
                        && point2.getY() >= node2y && point2.getY() <= node2y + node2h) {
                    if (node[0].contains(nodename1)) {
                        double[] coord = transformPoint((point2.getX() + deltax) * scale,
                                (point1.getX() + deltax) * scale,
                                (point2.getY() + deltay) * scale,
                                (point1.getY() + deltay) * scale,
                                node1w * scale, node1h * scale);
                        if (node1x <= node2x) {
                            updateToolTip(coord[0], coord[1], node[0], "left");
                        } else {
                            updateToolTip(coord[0], coord[1], node[0], "right");
                        }

                    }
                    if (node[1].contains(nodename1)) {
                        double[] coord = transformPoint((point1.getX() + deltax) * scale,
                                (point2.getX() + deltax) * scale,
                                (point1.getY() + deltay) * scale,
                                (point2.getY() + deltay) * scale,
                                node2w * scale, node2h * scale);
                        if (node2x <= node1x) {
                            updateToolTip(coord[0], coord[1], node[1], "leftsec");
                        } else {
                            updateToolTip(coord[0], coord[1], node[1], "rightsec");
                        }
                    }
                    if (node[0].contains(nodename2)) {
                        double[] coord = transformPoint((point2.getX() + deltax) * scale,
                                (point1.getX() + deltax) * scale,
                                (point2.getY() + deltay) * scale,
                                (point1.getY() + deltay) * scale,
                                node1w * scale, node1h * scale);
                        if (node1x <= node2x) {
                            updateToolTip(coord[0], coord[1], node[0], "left");
                        } else {
                            updateToolTip(coord[0], coord[1], node[0], "right");
                        }
                    }
                    if (node[1].contains(nodename2)) {
                        double[] coord = transformPoint((point1.getX() + deltax) * scale,
                                (point2.getX() + deltax) * scale,
                                (point1.getY() + deltay) * scale,
                                (point2.getY() + deltay) * scale,
                                node2w * scale, node2h * scale);
                        if (node2x <= node1x) {
                            updateToolTip(coord[0], coord[1], node[1], "leftsec");
                        } else {
                            updateToolTip(coord[0], coord[1], node[1], "rightsec");
                        }
                    }
                }
                if (point1.getX() >= node2x && point1.getX() <= node2x + node2w
                        && point1.getY() >= node2y && point1.getY() <= node2y + node2h
                        && point2.getX() >= node1x && point2.getX() <= node1x + node1w
                        && point2.getY() >= node1y && point2.getY() <= node1y + node1h) {
                    if (node[0].contains(nodename1)) {
                        double[] coord = transformPoint((point2.getX() + deltax) * scale,
                                (point1.getX() + deltax) * scale,
                                (point2.getY() + deltay) * scale,
                                (point1.getY() + deltay) * scale,
                                node1w * scale, node1h * scale);
                        if (node1x <= node2x) {
                            updateToolTip(coord[0], coord[1], node[0], "left");
                        } else {
                            updateToolTip(coord[0], coord[1], node[0], "right");
                        }
                    }
                    if (node[1].contains(nodename1)) {
                        double[] coord = transformPoint((point1.getX() + deltax) * scale,
                                (point2.getX() + deltax) * scale,
                                (point1.getY() + deltay) * scale,
                                (point2.getY() + deltay) * scale,
                                node2w * scale, node2h * scale);
                        if (node2x <= node1x) {
                            updateToolTip(coord[0], coord[1], node[1], "leftsec");
                        } else {
                            updateToolTip(coord[0], coord[1], node[1], "rightsec");
                        }
                    }
                    if (node[0].contains(nodename2)) {
                        double[] coord = transformPoint((point2.getX() + deltax) * scale,
                                (point1.getX() + deltax) * scale,
                                (point2.getY() + deltay) * scale,
                                (point1.getY() + deltay) * scale,
                                node1w * scale, node1h * scale);
                        if (node1x <= node2x) {
                            updateToolTip(coord[0], coord[1], node[0], "left");
                        } else {
                            updateToolTip(coord[0], coord[1], node[0], "right");
                        }
                    }
                    if (node[1].contains(nodename2)) {
                        double[] coord = transformPoint((point1.getX() + deltax) * scale,
                                (point2.getX() + deltax) * scale,
                                (point1.getY() + deltay) * scale,
                                (point2.getY() + deltay) * scale,
                                node2w * scale, node2h * scale);
                        if (node2x <= node1x) {
                            updateToolTip(coord[0], coord[1], node[1], "leftsec");
                        } else {
                            updateToolTip(coord[0], coord[1], node[1], "rightsec");
                        }
                    }
                }
                topArea.repaint();
                bottomArea.repaint();
                leftArea.repaint();
                rightArea.repaint();
                topAreaSecond.repaint();
                bottomAreaSecond.repaint();
                leftAreaSecond.repaint();
                rightAreaSecond.repaint();

            }
        }

    }

    public static double[] transformPoint(double x1, double x2, double y1, double y2, double width, double height) {
        double[] coord = new double[2];
        double xx = 0.;
        double yy = 0.;

        double ugol = Math.atan(height / width);

        if (x2 >= x1) {
            double x_delta = x2 - x1;
            double y_delta = y2 - y1;
            double alfa = Math.atan(y_delta / x_delta);
            if (alfa >= (-1.) * ugol && alfa <= ugol) {
                coord[0] = x2 - width / 2.;
                coord[1] = y2 - width * Math.tan(alfa) / 2.;
            }
            if (alfa > ugol && alfa <= Math.PI / 2.) {
                coord[0] = x2 - height / (2 * Math.tan(alfa));
                coord[1] = y2 - height / 2;
            }
            if (alfa >= (-1.) * Math.PI / 2. && alfa < (-1.) * ugol) {
                coord[0] = x2 + height / (2 * Math.tan(alfa));
                coord[1] = y2 + height / 2;
            }
        } else {
            double x_delta = x1 - x2;
            double y_delta = y1 - y2;
            double alfa = Math.atan(y_delta / x_delta);
            if (alfa >= (-1.) * ugol && alfa <= ugol) {
                coord[0] = x2 + width / 2.;
                coord[1] = y2 + width * Math.tan(alfa) / 2.;
            }
            if (alfa > ugol && alfa <= Math.PI / 2.) {
                coord[0] = x2 + height / (2 * Math.tan(alfa));
                coord[1] = y2 + height / 2;
            }
            if (alfa >= (-1.) * Math.PI / 2. && alfa < (-1.) * ugol) {
                coord[0] = x2 - height / (2 * Math.tan(alfa));
                coord[1] = y2 - height / 2;
            }
        }

        return coord;
    }

    public static double[] transformPointAmLine(double x1, double x2, double y1, double y2, double coord1, double coord2, double sdvig) {
        double[] coord = new double[2];

        double x_delta = Math.abs(x2 - x1);
        double y_delta = Math.abs(y2 - y1);
        double alfa = Math.atan(y_delta / x_delta);
        double dx = sdvig * Math.cos(alfa);
        double dy = sdvig * Math.sin(alfa);
        if (x2 > x1) {
            dx = (-1) * dx;
        }
        if (y2 > y1) {
            dy = (-1) * dy;
        }
        coord[0] = coord1 + dx;
        coord[1] = coord2 + dy;

        return coord;
    }

    public static void updateConturEdge(PInputEvent e, boolean visible) {
        if (visible) {
            double scale = Main.camera.getViewScale();
            PNode node = e.getPickedNode();

            ArrayList tmp = new ArrayList();
            tmp.add(node);
            Main.leftLineSelectNode.addAttribute("node", tmp);

            PBounds contur = node.getFullBounds();
            double x1 = contur.getMinX();
            double y1 = contur.getMinY();
            double x2 = contur.getMaxX();
            double y2 = contur.getMaxY();

            ArrayList nodes = (ArrayList) node.getAttribute("nodes");
            PNode node1 = (PNode) nodes.get(0);
            PNode node2 = (PNode) nodes.get(1);
            double node1x = node1.getFullBounds().getX();
            double node1y = node1.getFullBounds().getY();
            double node2x = node2.getFullBounds().getX();
            double node2y = node2.getFullBounds().getY();

            boolean isLeftTop = false;
            if ((node1x < node2x && node1y < node2y) || (node1x > node2x && node1y > node2y)) {
                isLeftTop = true;
            }

            Main.leftLineSelectNode.reset();
            int width = (Integer) node.getAttribute("width");
            Main.leftLineSelectNode.setStroke(new BasicStroke(width + 4));
            if (isLeftTop) {
                Main.leftLineSelectNode.moveTo((float) x1, (float) y1);
                Main.leftLineSelectNode.lineTo((float) x2, (float) y2);
            } else {
                Main.leftLineSelectNode.moveTo((float) x1, (float) y2);
                Main.leftLineSelectNode.lineTo((float) x2, (float) y1);
            }
            Main.leftLineSelectNode.setVisible(true);
            Main.leftLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.leftLineSelectNode.repaint();
            Main.leftLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
        } else {
            Main.leftLineSelectNode.setVisible(false);
        }
    }

    public static void updateConturText(PInputEvent e, boolean visible) {
        if (visible) {
            double scale = Main.camera.getViewScale();
            PNode node = e.getPickedNode();

            ArrayList tmp = new ArrayList();
            tmp.add(node);
            Main.leftLineSelectNode.addAttribute("node", tmp);

            PBounds contur = node.getFullBounds();
            double x1 = contur.getMinX();
            double y1 = contur.getMinY();
            double x2 = contur.getMaxX();
            double y2 = contur.getMaxY();

            Main.leftLineSelectNode.reset();
            Main.leftLineSelectNode.setStroke(new BasicStroke(1));
            Main.leftLineSelectNode.moveTo((float) x1 - 3, (float) y1 - 3);
            Main.leftLineSelectNode.lineTo((float) x1 - 3, (float) y2 + 3);
            Main.leftLineSelectNode.setVisible(true);
            Main.leftLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.leftLineSelectNode.repaint();
            Main.leftLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
            Main.rightLineSelectNode.reset();
            Main.rightLineSelectNode.moveTo((float) x2 + 3, (float) y1 - 3);
            Main.rightLineSelectNode.lineTo((float) x2 + 3, (float) y2 + 3);
            Main.rightLineSelectNode.setVisible(true);
            Main.rightLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.rightLineSelectNode.repaint();
            Main.rightLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
            Main.topLineSelectNode.reset();
            Main.topLineSelectNode.moveTo((float) x1 - 3, (float) y1 - 3);
            Main.topLineSelectNode.lineTo((float) x2 + 3, (float) y1 - 3);
            Main.topLineSelectNode.setVisible(true);
            Main.topLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.topLineSelectNode.repaint();
            Main.topLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
            Main.bottomLineSelectNode.reset();
            Main.bottomLineSelectNode.moveTo((float) x1 - 3, (float) y2 + 3);
            Main.bottomLineSelectNode.lineTo((float) x2 + 3, (float) y2 + 3);
            Main.bottomLineSelectNode.setVisible(true);
            Main.bottomLineSelectNode.translate(e.getDelta().width, e.getDelta().height);
            Main.bottomLineSelectNode.repaint();
            Main.bottomLineSelectNode.translate((-1.) * e.getDelta().width, (-1.) * e.getDelta().height);
        } else {
            Main.leftLineSelectNode.setVisible(false);
            Main.rightLineSelectNode.setVisible(false);
            Main.topLineSelectNode.setVisible(false);
            Main.bottomLineSelectNode.setVisible(false);
        }
    }

    /*
     * Get the extension of a file.
     */
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Returns an ImageIcon, or null if the path was invalid.
     */
    protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = Utils.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    public static String FindTrialPeriod(String map_filename) {
        File map_file = new File(map_filename);
        if (map_file.exists()) {
            try //find from map_file start date
            {
                BufferedReader inFile = new BufferedReader(new FileReader(map_filename));
                String line = inFile.readLine().toString();
                if (line.matches("^:hash#\\S+:$")) {
                    inFile.close();
                    return line;
                }
                if (inFile != null) {
                    inFile.close();
                }
            } catch (java.io.IOException e) {
                System.out.println("Error read from file : " + map_filename);
            }
        }
        return null;
    }

    public static boolean CheckSerialNumber(String serial) {
        int sum = 0;
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(0)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(5)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(9)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(10)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(15)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(18)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(23)));
        sum = sum + Integer.valueOf(Character.toString(serial.charAt(26)));
        if (sum % 10 > 0) {
            return false;
        } else {
            return true;
        }
    }

    public static String getOsName() {
        String os = "";
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            os = "windows";
        } else if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
            os = "linux";
        } else if (System.getProperty("os.name").toLowerCase().indexOf("mac") > -1) {
            os = "mac";
        }

        return os;
    }

    public static long[] GetBytesInOut(PPath flash) {
        long[] out = new long[2];
        out[0] = -1;
        out[1] = -1;

        String host = (String) flash.getAttribute("host");
        String iface_index = (String) flash.getAttribute("iface_index");
        String version = (String) flash.getAttribute("version");
        String community = (String) flash.getAttribute("community");

        return out;
    }

    public static void StopAllFlashLinkProcesses() {
        if (Main.m_ProcessPaintFlashLinks != null) {
            Main.m_ProcessPaintFlashLinks.stop();
            Main.m_ProcessPaintFlashLinks = null;
        }

        if (Main.m_FlashColor1 != null) {
            Main.m_FlashColor1.stop();
            Main.m_FlashColor1 = null;
        }
        if (Main.m_FlashColor2 != null) {
            Main.m_FlashColor2.stop();
            Main.m_FlashColor2 = null;
        }
    }

    private static ArrayList GetkeyFromMonitorCfgFile(String str, String key) {
        ArrayList out = new ArrayList();

        String[] buf1 = str.split("<" + key + ">");
        String[] buf2 = buf1[1].split("</" + key + ">");
        String name = buf2[0];
        String[] line = name.split("\n");
        for (int i = 0; i < line.length; i++) {
            if (!line[i].equals("")) {
                out.add(line[i]);
            }
        }

        return out;
    }

    public static String[][] GetInformationForCharts(String ip) {
        ArrayList information_charts = new ArrayList();

        File dir = new File(Main.home + "data/" + ip);
        String[] list_files = dir.list();
        if (list_files != null) {
            for (int i = 0; i < list_files.length; i++) {
                String filename = Main.home + "data/" + ip + "/" + list_files[i];
                try //read data file
                {
                    BufferedReader dataFile = new BufferedReader(new FileReader(filename));
                    try {
                        dataFile.readLine().toString();
                        dataFile.readLine().toString();
                        dataFile.readLine().toString();
                        String path_str = dataFile.readLine().toString();

                        boolean found = false;
                        int k;
                        for (k = 0; k < information_charts.size(); k++) {
                            String[] path_from_list = (String[]) information_charts.get(k);
                            if (path_from_list[0].equals(path_str)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            String day = null;
                            String week = null;
                            String month = null;
                            String year = null;
                            if (filename.matches(".+_day")) {
                                day = filename;
                            } else if (filename.matches(".+_week")) {
                                week = filename;
                            } else if (filename.matches(".+_month")) {
                                month = filename;
                            } else if (filename.matches(".+_year")) {
                                year = filename;
                            }

                            String[] str = new String[5];
                            str[0] = path_str;
                            str[1] = day;
                            str[2] = week;
                            str[3] = month;
                            str[4] = year;
                            information_charts.add(str);
                        } else {
                            String[] str1 = (String[]) information_charts.get(k);
                            String day = str1[1];
                            String week = str1[2];
                            String month = str1[3];
                            String year = str1[4];
                            if (filename.matches(".+_day")) {
                                day = filename;
                            } else if (filename.matches(".+_week")) {
                                week = filename;
                            } else if (filename.matches(".+_month")) {
                                month = filename;
                            } else if (filename.matches(".+_year")) {
                                year = filename;
                            }
                            String[] str = new String[5];
                            str[0] = path_str;
                            str[1] = day;
                            str[2] = week;
                            str[3] = month;
                            str[4] = year;
                            information_charts.set(k, str);
                        }

                    } catch (java.lang.NullPointerException e) {
                        dataFile.close();
                        break;
                    }
                } catch (java.io.FileNotFoundException e) {
                    System.out.println("File " + filename + " not found !\n");
                } catch (java.io.IOException e) {
                    System.out.println("Error read from file : " + filename + "\n");
                }
            }

            String[][] out = new String[information_charts.size()][5];
            for (int i = 0; i < information_charts.size(); i++) {
                String[] str = (String[]) information_charts.get(i);
                out[i][0] = str[0];
                out[i][1] = str[1];
                out[i][2] = str[2];
                out[i][3] = str[3];
                out[i][4] = str[4];
            }

            return out;
        } else {
            return null;
        }
    }

    public static String[][] AddPathToTree(DefaultMutableTreeNode root, String[] shablon) {
        ArrayList out = new ArrayList();
        String default_icon = Main.home + "image/charts.png";

        String[] buf = shablon[0].split(",");
        String[][] node_icon = new String[buf.length][2];
        for (int i = 0; i < buf.length; i++) {
            String[] buf1 = buf[i].split(":");
            if (buf1.length > 1) {
                node_icon[i][0] = buf1[0].trim();
                node_icon[i][1] = buf1[1].trim();
            } else {
                node_icon[i][0] = buf1[0].trim();
                node_icon[i][1] = null;
            }
        }

        DefaultMutableTreeNode parent = root;
        boolean is_leaf = false;
        for (int i = 0; i < node_icon.length; i++) {
            if (i == node_icon.length - 1) {
                is_leaf = true;
            }
            boolean find = false;
            DefaultMutableTreeNode node = null;
            if (parent.getChildCount() > 0) {
                node = parent.getNextNode();
                if (node.toString().equals(node_icon[i][0])) {
                    find = true;
                } else {
                    for (int j = 0; j < node.getSiblingCount() - 1; j++) {
                        node = node.getNextSibling();
                        if (node.toString().equals(node_icon[i][0])) {
                            find = true;
                            break;
                        }
                    }
                }
            }
            if (find) {
                parent = node;
            } else {
                parent.add(new DefaultMutableTreeNode(node_icon[i][0]));
                String[] str = new String[8];
                str[0] = node_icon[i][0];;
                str[1] = String.valueOf(i);
                str[2] = node_icon[i][1];
                if (is_leaf) {
                    str[3] = shablon[1];
                    str[4] = shablon[2];
                    str[5] = shablon[3];
                    str[6] = shablon[4];
                    str[7] = shablon[0];
                }
                out.add(str);
                node = parent.getNextNode();
                if (node.toString().equals(node_icon[i][0])) {
                    parent = node;
                } else {
                    for (int j = 0; j < node.getSiblingCount() - 1; j++) {
                        node = node.getNextSibling();
                        if (node.toString().equals(node_icon[i][0])) {
                            parent = node;
                            break;
                        }
                    }
                }
            }
        }

        String[][] out_mas = new String[out.size()][8];
        for (int i = 0; i < out.size(); i++) {
            String[] str = (String[]) out.get(i);
            out_mas[i][0] = str[0];
            out_mas[i][1] = str[1];
            if (str[2] != null) {
                out_mas[i][2] = Main.home + "image/" + str[2];
            } else {
                out_mas[i][2] = default_icon;
            }
            out_mas[i][3] = str[3];
            out_mas[i][4] = str[4];
            out_mas[i][5] = str[5];
            out_mas[i][6] = str[6];
            out_mas[i][7] = str[7];
        }

        return out_mas;
    }

    public static String[] FindSettings(ArrayList settings_list, TreeNode[] path) {
        String[] out = new String[5];
        for (int i = 0; i < settings_list.size(); i++) {
            String[] str = (String[]) settings_list.get(i);

            if (str[7] != null) {
                String[] buf = str[7].split(",");
                String[] path_str = new String[buf.length];
                for (int j = 0; j < buf.length; j++) {
                    String[] buf1 = buf[j].trim().split(":");
                    path_str[j] = buf1[0].trim();
                }

                if (path_str.length == path.length - 1) {
                    boolean find = true;
                    for (int j = 1; j < path.length; j++) {
                        if (!path[j].toString().equals(path_str[j - 1])) {
                            find = false;
                            break;
                        }
                    }
                    if (find) {
                        out[0] = str[2];
                        out[1] = str[3];
                        out[2] = str[4];
                        out[3] = str[5];
                        out[4] = str[6];
                        return out;
                    }
                }
            }
        }
        return null;
    }

    public static String[] FindSettings_value_level(ArrayList settings_list, String node, int level) {
        String[] out = new String[5];
        boolean find = false;
        for (int i = 0; i < settings_list.size(); i++) {
            String[] str = (String[]) settings_list.get(i);
            if (str[0].equals(node) && str[1].equals(String.valueOf(level - 1))) {
                out[0] = str[2];
                out[1] = str[3];
                out[2] = str[4];
                out[3] = str[5];
                out[4] = str[6];
                find = true;
            }
        }
        if (find) {
            return out;
        } else {
            return null;
        }
    }

    public static String ReadFile(String file) {
        String str = null;

        File file_descr = new File(file);
        if (file_descr.exists()) {
            BufferedReader inFile = null;
            try //read monitor cfg file
            {
                str = "";
                inFile = new BufferedReader(new FileReader(file));
                while (true) {
                    try {
                        String line = inFile.readLine().toString();
                        if (line == null) {
                            break;
                        }
//                        if(line.equals("") || line.matches("//.*") || line.matches("#.*")) continue;
                        str = str + line + "\n";
                    } catch (java.lang.NullPointerException e) {
                        inFile.close();
                        break;
                    }
                }
                inFile.close();
            } catch (java.io.FileNotFoundException e) {
                System.out.println("File " + file + " not found !");
            } catch (java.io.IOException e) {
                System.out.println("Error read from file : " + file);
            } finally {
                try {
                    inFile.close();
                } catch (IOException ex) {
                }
            }
        }
        return str;
    }

    public static void WriteFile(String filename, String str, boolean append) {
        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(filename, append));
            outFile.write(str);
            outFile.close();

        } catch (java.io.IOException e) {
            System.out.println("Error write to file : " + filename);
        } finally {
            try {
                outFile.close();
            } catch (IOException ex) {
            }
        }
    }

    public static String GetTag(String str, String key) {
        String out = null;

        String[] buf1 = str.split("<" + key.trim() + ">");
        if (buf1.length > 1) {
            String[] buf2 = buf1[1].split("</" + key.trim() + ">");
            out = buf2[0];
        }
        return out;
    }

    public static void CreateFlagFile(String filename) {
        String path = filename.replace("\\", "/");
        String[] buf = path.split("/");
        if (buf.length > 1) {
            String dir = buf[0];
            for (int i = 1; i < buf.length - 1; i++) {
                dir = dir + "/" + buf[i];
            }
            File dir_descr = new File(dir);
            if (!dir_descr.exists()) {
                dir_descr.mkdirs();
                WriteFile(path, "Neb Viewer is running.", false);
            } else {
                WriteFile(path, "Neb Viewer is running.", false);
            }
        } else {
            WriteFile(filename, "Neb Viewer is running.", false);
        }
    }

    public static void DeleteFlagFile(String filename) {
        File filename_descr = new File(filename);
        if (filename_descr.exists()) {
            filename_descr.delete();
        }
    }

    public static void SetPositionToNode(ArrayList<String> name_node_port) {
        ControlPanel.message.setText("");
        String name = name_node_port.get(0);
        String node_name = name_node_port.get(1);
        String port = name_node_port.get(2);
        PNode node = null;
        for (int i = 0; i < Main.nodeLayer.getChildrenCount(); i++) {
            ArrayList<String[]> ip_list = new ArrayList();
            node = Main.nodeLayer.getChild(i);
            String ip_node = (String) node.getAttribute("ip");

            if (ip_node.equals(node_name)) {
                break;
            }
        }

        if (node != null) {
            double w_start = Main.canvas.getCamera().getFullBounds().getWidth();
            double w_stop = node.getWidth() * 10;
            if (w_stop == 0) {
                w_stop = 500 * 10;
            }
            Main.canvas.getCamera().setViewScale(w_start / w_stop);
            double width = node.getWidth() * 10.;
            if (width == 0) {
                width = 500 * 10;
            }
            double height = node.getHeight() * 10.;
            if (height == 0) {
                height = 500 * 10;
            }

            Point2D.Double center = (Point2D.Double) node.getBounds().getCenter2D();
            PBounds bound = new PBounds();
            bound.setRect(center.getX() - width / 2 - 20, center.getY() - height / 2 - 20, width + 40, height + 40);
            Main.canvas.getCamera().animateViewToCenterBounds(bound, true, 500);
            Main.canvas.getCamera().repaint();
            if (port != null && !port.equals("")) {
                ControlPanel.message.setText(node_name + "     port: " + port);
            } else {
                ControlPanel.message.setText("Node " + name + " is found.");
            }
        } else {
            ControlPanel.message.setText("Ip address: " + name + " not found !!!");
        }

    }

    public static void RunJava(String run_str) {
        try {
            System.out.println(run_str);
            Process p = Runtime.getRuntime().exec(run_str);
            try {
                p.waitFor();
            } catch (java.lang.InterruptedException ex) {
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            if (in != null) {
                in.close();
            }
            in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            if (in != null) {
                in.close();
            }
        } catch (java.io.IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    public static String GetKeyFromNebCFG(String str, String key) {
        String[] line = str.split("\\n");
        for (int i = 0; i < line.length; i++) {
            String[] buf = line[i].split("=");
            if (buf[0].trim().equalsIgnoreCase(key)) {
                return buf[1].trim();
            }
        }
        return null;
    }

    public static String SetKeyToNebCFG(String str, String key, String value) {
        String[] line = str.split("\\n");
        String out = "";
        boolean find = false;
        for (int i = 0; i < line.length; i++) {
            String[] buf = line[i].split("=");
            if (buf[0].trim().equalsIgnoreCase(key)) {
                out = out + buf[0].trim() + " = " + value + "\n";
                find = true;
            } else {
                out = out + line[i] + "\n";
            }
        }
        if (!find) {
            out = out + key + " = " + value + "\n";
        }
        return out;
    }

    public static long StringDateToSystemDate(String str) {
        String[] buf = str.split("\\s+");
        String[] buf1 = buf[0].split(":");
        int hour = Integer.valueOf(buf1[0]);
        int min = Integer.valueOf(buf1[1]);
        int sec = Integer.valueOf(buf1[2]);
        buf1 = buf[1].split("/");
        int day = Integer.valueOf(buf1[0]);
        int month = Integer.valueOf(buf1[1]);
        int year = Integer.valueOf(buf1[2]);
        Date d = new Date(year - 1900, month - 1, day, hour, min, sec);
        long time = d.getTime();
        return time;
    }

    public static Color DoubleToColor(double color) {
        int r = (int) Math.floor(color / (255 * 255));
        color = color - r * 255 * 255;
        int g = (int) Math.floor(color / 255);
        int b = (int) (color - g * 255);
        Color c = new Color(r, g, b);
        return c;
    }

    public static double ColorToDouble(Color c) {
        double color = c.getRed() * 255 * 255 + c.getGreen() * 255 + c.getBlue();
        return color;
    }

    public ArrayList LoadInfoFromMapFile(String map_file) {
        ArrayList result = new ArrayList();
        if (map_file != null) {
            ArrayList<String[]> nodes_in_mapfile = ReadFromMapFile(map_file, "^:nodes:$");
            ArrayList<String[]> extend_info_in_mapfile = ReadFromMapFile(map_file, "^:extend_info:$");
            ArrayList<String[]> hosts_in_mapfile = ReadFromMapFile(map_file, "^:hosts:$");

            Map<String, String[]> nodes_info = new HashMap<>();
            for (String[] item : nodes_in_mapfile) {
                nodes_info.put(item[0], item);
            }

            Map<String, String[]> extends_info = new HashMap<>();
            for (String[] item : extend_info_in_mapfile) {
                extends_info.put(item[0], item);
            }

            result.add(nodes_info);
            result.add(hosts_in_mapfile);
            result.add(extends_info);
        }
        return result;
    }

    private String GetDataTimeFromMapFile(String filename) {
        String result = "";
        String timePattern = "dd.MM.yyyy HH.mm";
        Pattern p = Pattern.compile("^.*Neb_(\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d-\\d\\d\\.\\d\\d)\\.map$");

        if (filename.equals(Main.map_filename)) {
            result = GetFileCreateTime(filename);

        } else {
            Matcher m = p.matcher(filename);
            if (m.find()) {
                result = m.group(1).replace("-", " ");
            }
        }

        return result;
    }

    public boolean VerifyIpAdress(String ip) {
        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            String[] tokens = ip.split("\\.");
            if (tokens.length == 4) {
                int token = Integer.parseInt(tokens[0]);
                if (!(token >= 0 && token <= 255)) {
                    return false;
                }
                token = Integer.parseInt(tokens[1]);
                if (!(token >= 0 && token <= 255)) {
                    return false;
                }
                token = Integer.parseInt(tokens[2]);
                if (!(token >= 0 && token <= 255)) {
                    return false;
                }
                token = Integer.parseInt(tokens[3]);
                if (!(token >= 0 && token <= 255)) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    public static void ZoomIn() {
        PBounds b0 = Main.camera.getViewBounds();
        Main.camera.scaleView(0.9);
        PBounds b1 = Main.camera.getViewBounds();
        PBounds b2 = new PBounds();
        b2.setRect(b0.getX() + (b0.getWidth() - b1.getWidth()) / 2, b0.getY() + (b0.getHeight() - b1.getHeight()) / 2, b1.getWidth(), b1.getHeight());
        Main.camera.setViewBounds(b2);
        Main.camera.repaint();
        Main.view_scale = Main.camera.getViewScale();
        repaintText();
        repaintInterfaceName();
        RepaintFlash(null);
    }

    public static void ZoomOut() {
        PBounds b0 = Main.camera.getViewBounds();
        Main.camera.scaleView(1.1);
        PBounds b1 = Main.camera.getViewBounds();
        PBounds b2 = new PBounds();
        b2.setRect(b0.getX() + (b0.getWidth() - b1.getWidth()) / 2, b0.getY() + (b0.getHeight() - b1.getHeight()) / 2, b1.getWidth(), b1.getHeight());
        Main.camera.setViewBounds(b2);
        Main.camera.repaint();
        Main.view_scale = Main.camera.getViewScale();
        repaintText();
        repaintInterfaceName();
        RepaintFlash(null);
    }

    public void LoadAndPositionMapFromTree(TreePath selPath) {
        String node_leaf = selPath.getPath()[2].toString();
        String data_str = selPath.getParentPath().getPath()[1].toString();

//        String ip=node_leaf.split("\\(")[0];
        String select_map_filename = null;
        String area = null;
        ArrayList<String> name_node_port = new ArrayList();
        for (ArrayList<String> item : Main.find_result_list) {
            if (item.get(0).equals(data_str)) {
                select_map_filename = item.get(1);
                area = item.get(2);
                name_node_port.add(item.get(3));
                name_node_port.add(item.get(4));
                name_node_port.add(item.get(5));
                break;
            }
        }

        if (select_map_filename != null && area != null && name_node_port.size() == 3) {
            if(new File(Main.map_filename).getName().equals(new File(select_map_filename)) && ControlPanel.area_select.equals(area)) {
                SetPositionToNode(name_node_port);
            } else {
                for (int i = 0; i < Main.history_list.size(); i++) {
                    String[] item = Main.history_list.get(i);
                    if (data_str.equals(item[0])) {
                        TimeMachineForm.selector = i;

                        Map<String, String> area_description = new HashMap();
                        for (Map.Entry<String, Map> entry : ((Map<String, Map>) Main.cfg_server.get("areas")).entrySet()) {
                            String area_name = entry.getKey();
                            String description = (String) entry.getValue().get("description");
                            if (description != null) {
                                area_description.put(area_name, description);
                            } else {
                                area_description.put(area_name, area_name);
                            }
                        }
                
                        if(!(new File(Main.map_filename).getName()).equals(new File(select_map_filename).getName()))
                            Main.utils.SetTimeMachine(Main.history_list, TimeMachineForm.selector, ControlPanel.area_select);
                        
                        if(area != null && !ControlPanel.area_select.equals(area)) {
                            try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                            while(true) {
                                if(!Main.isBusy) {
                                    try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                                    ControlPanel.jComboBox1.setSelectedItem(area_description.get(area));
                                    break;
                                } else
    //                                System.out.println("Is busy.");
                                    try { Thread.sleep(100); } catch(java.lang.InterruptedException e) {}
                            }
                            ControlPanel.area_select = area;
                        }
                        
                        try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                        while(true) {
                            if(!Main.isBusy) {
//                                System.out.println("Set position.");
                                try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                                SetPositionToNode(name_node_port);
                                break;
                            } else
//                                System.out.println("Is busy.");
                                try { Thread.sleep(100); } catch(java.lang.InterruptedException e) {}
                        }
       
//                        SetPositionToNode(name_node_port);
//                        try { Thread.sleep(5000); } catch(java.lang.InterruptedException e) {}
                                
                        break;
                    }
                }
            }
        }
    }

    public void LoadAndPositionMap(String area, String node) {
        if (area != null && node != null) {
            ArrayList<String> name_node_port = new ArrayList();
            name_node_port.add(node);
            name_node_port.add(node);
            name_node_port.add("");            
            
            if(new File(Main.map_filename).getName().equals(new File(Main.map_file)) && ControlPanel.area_select.equals(area)) {
                SetPositionToNode(name_node_port);
            } else {
                TimeMachineForm.selector = Main.history_list.size()-1;

                Map<String, String> area_description = new HashMap();
                for (Map.Entry<String, Map> entry : ((Map<String, Map>) Main.cfg_server.get("areas")).entrySet()) {
                    String area_name = entry.getKey();
                    String description = (String) entry.getValue().get("description");
                    if (description != null) {
                        area_description.put(area_name, description);
                    } else {
                        area_description.put(area_name, area_name);
                    }
                }

                if(!(new File(Main.map_filename).getName()).equals(new File(Main.map_file).getName()))
                    Main.utils.SetTimeMachine(Main.history_list, TimeMachineForm.selector, ControlPanel.area_select);

                if(area != null && !ControlPanel.area_select.equals(area)) {
                    try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                    while(true) {
                        if(!Main.isBusy) {
                            try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                            ControlPanel.jComboBox1.setSelectedItem(area_description.get(area));
                            break;
                        } else
//                                System.out.println("Is busy.");
                            try { Thread.sleep(100); } catch(java.lang.InterruptedException e) {}
                    }
                    ControlPanel.area_select = area;
                }

                try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                while(true) {
                    if(!Main.isBusy) {
//                                System.out.println("Set position.");
                        try { Thread.sleep(500); } catch(java.lang.InterruptedException e) {}
                        SetPositionToNode(name_node_port);
                        break;
                    } else
//                                System.out.println("Is busy.");
                        try { Thread.sleep(100); } catch(java.lang.InterruptedException e) {}
                }

            }
        }
    }
    
    private static boolean WriteStrToFile(String filename, String str) {
        try {
            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            outFile.write(str);
            outFile.close();
            return true;
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keySet().iterator();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    private java.util.List<Object> toList(JSONArray array) {
        java.util.List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    public String GetFileCreateTime(String file) {
        String result = null;
        try {
            BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
            ZonedDateTime ct = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
            String day;
            if (ct.getDayOfMonth() <= 9) {
                day = "0" + String.valueOf(ct.getDayOfMonth());
            } else {
                day = String.valueOf(ct.getDayOfMonth());
            }
            String month;
            if (ct.getMonthValue() <= 9) {
                month = "0" + String.valueOf(ct.getMonthValue());
            } else {
                month = String.valueOf(ct.getMonthValue());
            }
            String year = String.valueOf(ct.getYear());
            String hour;
            if (ct.getHour() <= 9) {
                hour = "0" + String.valueOf(ct.getHour());
            } else {
                hour = String.valueOf(ct.getHour());
            }
            String min;
            if (ct.getMinute() <= 9) {
                min = "0" + String.valueOf(ct.getMinute());
            } else {
                min = String.valueOf(ct.getMinute());
            }
            result = day + "." + month + "." + year + " " + hour + "." + min;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public long GetFileCreateTime_mSec(String file) {
        long result = 0;
        try {
            BasicFileAttributes attr = Files.readAttributes(new File(file).toPath(), BasicFileAttributes.class);
            ZonedDateTime ct = attr.creationTime().toInstant().atZone(ZoneId.systemDefault());
            result = ct.toEpochSecond() * 1000;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public Object StrToObject(String val) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        Map map = new HashMap();
        java.util.List list = new ArrayList();

        try {
            Object json = parser.parse(val);
            if (json instanceof JSONObject) {
                map = toMap((JSONObject) json);
                return map;
            } else if (json instanceof JSONArray) {
                list = toList((JSONArray) json);
                return list;
            } else {
                return val;
            }
        } catch (Exception excep) {
        }
        return null;
    }

}

class RunCommands extends Thread {

    public String run_str;

    @Override
    public void run() {
        Utils.RunJava(run_str);
    }
}
