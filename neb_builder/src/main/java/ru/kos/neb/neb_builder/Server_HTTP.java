package ru.kos.neb.neb_builder;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import static ru.kos.neb.neb_builder.Neb.map_file;
import static ru.kos.neb.neb_builder.Neb.utils;
import ru.kos.neb.neb_lib.GetList;
import ru.kos.neb.neb_lib.PingPool;

import ru.kos.neb.neb_lib.Utils;
import ru.kos.neb.neb_lib.WalkPool;

public class Server_HTTP extends Thread {
    private int port = 8080;
//    private String cert = "cert.jks";
    public static Map INFO = new HashMap();
    public static Map INFO_NAMES = new HashMap();
    public static ArrayList<String[]> file_key_val_list = new ArrayList();
    
    private static Map<String, String> lastmodify = new HashMap();
    
    public Server_HTTP(int port) {
        this.port = port;
    }    
    
    @Override
    public void run() {
        try {
            lastmodify = CheckModify(lastmodify);
////////////////////////////////////////////////////////
//            WatchInformationFile watchInformationFile = new WatchInformationFile();
//            watchInformationFile.start();   
///////////////////////////////////////////////////////
            
            // setup the socket address
            InetSocketAddress address = new InetSocketAddress(this.port);

            // initialise the HTTPS server
            HttpServer httpServer = HttpServer.create(address, 0);
            
            // ssl settings
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//
//            // initialise the keystore
//            char[] password = "1qaz2wsx".toCharArray();
//            KeyStore ks = KeyStore.getInstance("JKS");
//            FileInputStream fis = new FileInputStream(cert);
//            ks.load(fis, password);
//
//            // setup the key manager factory
//            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//            kmf.init(ks, password);
//
//            // setup the trust manager factory
//            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
//            tmf.init(ks);
//
//            // setup the HTTPS context and parameters
//            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
//            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
//                public void configure(HttpsParameters params) {
//                    try {
//                        // initialise the SSL context
//                        SSLContext context = getSSLContext();
//                        SSLEngine engine = context.createSSLEngine();
//                        params.setNeedClientAuth(false);
//                        params.setCipherSuites(engine.getEnabledCipherSuites());
//                        params.setProtocols(engine.getEnabledProtocols());
//
//                        // Set the SSL parameters
//                        SSLParameters sslParameters = context.getSupportedSSLParameters();
//                        params.setSSLParameters(sslParameters);
//
//                    } catch (Exception ex) {
//                        System.out.println("Failed to create HTTPS port");
//                    }
//                }
//            });
////////////////////////////            
//            httpServer.createContext("/token", new GetToken());
            httpServer.createContext("/get", new GetInfo());
            httpServer.createContext("/set", new SetInfo());
            httpServer.createContext("/ipbyname", new IpByName());
            httpServer.createContext("/find", new FindInfo());
            httpServer.createContext("/find_full_text", new FindFullTextInfo());
            httpServer.createContext("/delete", new Delete());
            httpServer.createContext("/del_from_list", new Del_From_List());
            httpServer.createContext("/add_to_list", new Add_To_List());
            httpServer.createContext("/list", new GetListKey());
            httpServer.createContext("/getfiles_list", new GetFilesList());
            httpServer.createContext("/getfile", new GetFile());
            httpServer.createContext("/getfile_attributes", new GetFileAttributes());
            httpServer.createContext("/commit", new Commit());
            httpServer.createContext("/setchunk", new SetChunk());
            httpServer.createContext("/snmpget", new SnmpGet());
            httpServer.createContext("/snmpwalk", new SnmpWalk());
            httpServer.createContext("/ping", new Ping());
            httpServer.createContext("/status", new Status());
            
            httpServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            httpServer.start();

        } catch (Exception exception) {
            System.out.println("Failed to create HTTPS server on port " + this.port + " of localhost");
            exception.printStackTrace();

        }
    }
    
    public static void response(HttpExchange he, int cod_response, String msg) {
        try {
            String response = msg;
            he.sendResponseHeaders(cod_response, msg.getBytes().length);
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();    
        } catch(IOException ex) { 
            ex.printStackTrace(); 
            Neb.logger.Println(ex.toString(), Neb.logger.INFO);
        }
    }
    
    public static boolean check_client_read(HttpExchange he) {
        lastmodify = CheckModify(lastmodify);
        Headers headers=he.getRequestHeaders();
        String user = headers.getFirst("user");
        String passwd = headers.getFirst("passwd");        
        
        if(user != null && passwd != null) {
            if(Neb.cfg.get("users") != null && ((Map)Neb.cfg.get("users")).get(user) != null) {
                Map<String, String> user_info = (Map)((Map)Neb.cfg.get("users")).get(user);
                if(user_info.get("passwd").equals(passwd) && user_info.get("access").equals("read"))
                    return true;
                else
                    return false;
            } else
                return false;
        } else
            return false;
    }
    
    public static boolean check_client_write(HttpExchange he) {
        lastmodify = CheckModify(lastmodify);
        Headers headers=he.getRequestHeaders();
        String user = headers.getFirst("user");
        String passwd = headers.getFirst("passwd");        
        
        if(user != null && passwd != null) {
            if(Neb.cfg.get("users") != null && ((Map)Neb.cfg.get("users")).get(user) != null) {
                Map<String, String> user_info = (Map)((Map)Neb.cfg.get("users")).get(user);
                if(user_info.get("passwd").equals(passwd) && user_info.get("access").equals("write"))
                    return true;
                else
                    return false;
            } else
                return false;
        } else
            return false;
    } 
    
    private static Map CheckModify(Map<String, String> lastmodify) {
        if(lastmodify.get(Neb.map_file) != null) {
            long lastmodify_time_prev = Long.valueOf(lastmodify.get(Neb.map_file));
            File f_map = new File(Neb.map_file);
            if(f_map.exists()) {
                if(lastmodify_time_prev < f_map.lastModified()) {
                    System.out.println("Reload "+Neb.map_file+" file.");
                    Server_HTTP.INFO = Neb.utils.ReadJSONFile(Neb.map_file);
                    System.out.println("Stop reload "+Neb.map_file+" file.");
                    lastmodify.put(Neb.map_file, String.valueOf(f_map.lastModified()));
                }
            }
        } else {
            File f_map = new File(Neb.map_file);
            if(f_map.exists()) {
                System.out.println("Reload "+Neb.map_file+" file.");
                Server_HTTP.INFO = Neb.utils.ReadJSONFile(Neb.map_file);
                System.out.println("Stop reload "+Neb.map_file+" file.");
                lastmodify.put(Neb.map_file, String.valueOf(f_map.lastModified()));
            }            
        }
        
        if(lastmodify.get(Neb.neb_cfg) != null) {
            long lastmodify_time_prev = Long.valueOf(lastmodify.get(Neb.neb_cfg));
            File f_cfg = new File(Neb.neb_cfg);
            if(f_cfg.exists()) {
                if(lastmodify_time_prev < f_cfg.lastModified()) {
                    System.out.println("Reload "+Neb.neb_cfg+" file.");
                    Neb.cfg = Neb.utils.ReadJSONFile(Neb.neb_cfg);
                    System.out.println("Stop reload "+Neb.neb_cfg+" file.");
                    lastmodify.put(Neb.neb_cfg, String.valueOf(f_cfg.lastModified()));
                }
            }
        } else {
            File f_cfg = new File(Neb.neb_cfg);
            if(f_cfg.exists()) {
                System.out.println("Reload "+Neb.neb_cfg+" file.");
                Neb.cfg = Neb.utils.ReadJSONFile(Neb.neb_cfg);
                System.out.println("Stop reload "+Neb.neb_cfg+" file.");
                lastmodify.put(Neb.neb_cfg, String.valueOf(f_cfg.lastModified()));
            }            
        }
        
        if(lastmodify.get(Neb.names_info) != null) {
            long lastmodify_time_prev = Long.valueOf(lastmodify.get(Neb.names_info));
            File f_names_info = new File(Neb.names_info);
            if(f_names_info.exists()) {
                if(lastmodify_time_prev < f_names_info.lastModified()) {
                    System.out.println("Reload "+Neb.names_info+" file.");
                    Server_HTTP.INFO_NAMES = Neb.utils.ReadJSONFile(Neb.names_info);
                    System.out.println("Stop reload "+Neb.names_info+" file.");
                    lastmodify.put(Neb.names_info, String.valueOf(f_names_info.lastModified()));
                }
            }
        } else {
            File f_names_info = new File(Neb.names_info);
            if(f_names_info.exists()) {
                System.out.println("Reload "+Neb.names_info+" file.");
                Server_HTTP.INFO_NAMES = Neb.utils.ReadJSONFile(Neb.names_info);
                System.out.println("Stop reload "+Neb.names_info+" file.");
                lastmodify.put(Neb.names_info, String.valueOf(f_names_info.lastModified()));
            }            
        }        
        return lastmodify;
    }
}

//class GetToken implements HttpHandler {
//    @Override
//    public void handle(HttpExchange he) {
//        if(he.getRequestMethod().equalsIgnoreCase("GET")) {
//            HttpExchange httpExchange = (HttpExchange) he;
//            he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
//            Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
//            String user = params.get("user");
//            String passwd = params.get("passwd");
//            if(user != null && passwd != null) {
//                Utils neb_lib_utils = new Utils();
//                String hash_str = neb_lib_utils.Hashing(user+passwd);
//                if(hash_str != null) Server_HTTP.response(he, 200, hash_str);
//                else Server_HTTP.response(he, 200, "\n");
//            } else {
//                Server_HTTP.response(he, 400, "Error query: not set user or passwd");
//            }
//        }
//    }
//}

class GetInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            if(file.equals(Neb.map_file)) {
                                Object value = Neb.utils.GetKey(key, Server_HTTP.INFO);
                                if(value != null) {
                                    Gson gson = new Gson(); 
                                    String info_str = gson.toJson(value);                             
                                    Server_HTTP.response(he, 200, info_str);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");
                            } else {
                                Map info_map = Neb.utils.ReadJSONFile(file);
                                Object key_value = Neb.utils.GetKey(key, info_map);
                                if(key_value != null) {
                                    Gson gson = new Gson(); 
                                    String info_str = gson.toJson(key_value);                             
                                    Server_HTTP.response(he, 200, info_str);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");                        
                            }
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}

class Status implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                if(Server_HTTP.check_client_write(he)) {
                    Server_HTTP.response(he, 200, "write");
                } else if(Server_HTTP.check_client_read(he)) {
                    Server_HTTP.response(he, 200, "read");
                } else {
                    Server_HTTP.response(he, 200, "not access");
                }

            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class IpByName implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String name = params.get("name");
                    if(name != null) {
                        String ip = "";
                        try {
                            if(Server_HTTP.INFO_NAMES.get(name) != null)
                                ip = (String)Server_HTTP.INFO_NAMES.get(name);                            
                            ip = InetAddress.getByName(name).getHostAddress();
                        } catch (UnknownHostException ex) {
                            if(Server_HTTP.INFO_NAMES.get(name) != null)
                                ip = (String)Server_HTTP.INFO_NAMES.get(name);
                        }    
                        Server_HTTP.response(he, 200, ip);
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set name.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");  
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class FindInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String key = params.get("key");
                    if(key != null) {
                        ArrayList find_list = Neb.utils.FindKey(key);
                        if(find_list.size() > 0) {
                            Gson gson = new Gson(); 
                            String info_str = gson.toJson(find_list);                             
                            Server_HTTP.response(he, 200, info_str);                    
                        } else Server_HTTP.response(he, 400, "Error query: Key not found.");

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");  
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class FindFullTextInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String key = params.get("key");
                    if(key != null) {
                        try {
                            key = key.replaceAll("\\++", " ");
                            String[] mas = key.split(" ");
//                            for(int i = 0; i < mas.length; i++) {
//                                char[] mas1 = mas[i].toCharArray();
//                                for(char s : mas1) {
//                                    if((int)s > 128) {
//                                        if(!mas[i].substring(mas[i].length()-1).equals("*"))
//                                            mas[i] = mas[i]+"*";
//                                        break;
//                                    }
//                                }                                     
//                            }
                            String query_str = mas[0];
                            for(int i = 1; i < mas.length; i++) {
                                query_str = query_str +" AND "+ mas[i];
                            }                            
                            
                            StandardAnalyzer analyzer = new StandardAnalyzer();
                            final Directory index = FSDirectory.open(Paths.get(Neb.index_dir));

                            Query q = new QueryParser("text", analyzer).parse(query_str);

                            // 3. search
                            int hitsPerPage = 10000;
                            IndexReader reader = DirectoryReader.open(index);
                            IndexSearcher searcher = new IndexSearcher(reader);
                            TopDocs docs = searcher.search(q, hitsPerPage);
                            ScoreDoc[] hits = docs.scoreDocs;     

                            // 4. display results
//                            System.out.println("Found " + hits.length + " hits.");
                            Map<String, String> area_node_attribute = new HashMap();
                            for(int i=0;i<hits.length;++i) {
                                int docId = hits[i].doc;
                                Document d = searcher.doc(docId);
                                    
                                String text = d.get("text").replace("\n", " ");
                                boolean find = false;
                                for(int ii = 0; ii < mas.length; ii++) {
                                    String pattern = mas[ii].replace("*", ".+").replace("?", ".").toLowerCase();
                                    Pattern p = Pattern.compile(pattern);
                                    Matcher m = p.matcher(text.toLowerCase());
                                    if(m.find()) {
                                        find = true;
                                        break;
                                    }
                                }
                                if(find) {
                                    String area = d.get("area");
                                    String node = d.get("node");
                                    String sysname = "";
                                    if(Server_HTTP.INFO.get(area) != null && ((Map)Server_HTTP.INFO.get(area)).get("nodes_information") != null && 
                                          ((Map)((Map)Server_HTTP.INFO.get(area)).get("nodes_information")).get(node) != null &&
                                          ((Map)((Map)((Map)Server_HTTP.INFO.get(area)).get("nodes_information")).get(node)).get("general") != null &&
                                          ((Map)((Map)((Map)((Map)Server_HTTP.INFO.get(area)).get("nodes_information")).get(node)).get("general")).get("sysname") != null )
                                        sysname = (String)((Map)((Map)((Map)((Map)Server_HTTP.INFO.get(area)).get("nodes_information")).get(node)).get("general")).get("sysname");

                                    if(area_node_attribute.get(area+"-"+node) == null) {
                                        String str = text + ";" + area + ";" + node + ";" + sysname + "\n";
                                        area_node_attribute.put(area+"-"+node, str);
                                        
                                    } else {
                                        text = area_node_attribute.get(area+"-"+node).split(";")[0]+", "+text;
                                        String str = text + ";" + area + ";" + node + ";" + sysname + "\n";
                                        area_node_attribute.put(area+"-"+node, str);                                        
                                    }
                                    
    //                                System.out.println((i + 1) + ". " + d.get("text") + "\t" + d.get("area") + "\t" + d.get("node"));
                                }
                            }
                            
                            String out = "";
                            for(Map.Entry<String, String> entry : area_node_attribute.entrySet()) {
                                String str = entry.getValue();
                                out = out + str;
                            }
                            
                            Server_HTTP.response(he, 200, out);                    
                        } catch(IOException | ParseException ex) {
                            Server_HTTP.response(he, 400, "Error query: Exception!");
                            ex.printStackTrace();
                        }

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");  
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}


class GetListKey implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            if(file.equals(Neb.map_file)) {
                                String key_list = Neb.utils.GetKeyList(key, Server_HTTP.INFO);
                                if(!key_list.equals("")) {
                                    Server_HTTP.response(he, 200, key_list);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");
                            } else {
                                Map info_map = Neb.utils.ReadJSONFile(file);
                                String key_list = Neb.utils.GetKeyList(key, info_map);
                                if(!key_list.equals("")) {
                                    Server_HTTP.response(he, 200, key_list);
                                } else Server_HTTP.response(he, 400, "Error query: Key not found.");                        
                            } 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server."); 
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class GetFilesList implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String directory = params.get("directory");

                    if(directory != null) {
                        if((new File(directory)).exists()) {
                            Stream<Path> walk = Files.walk(Paths.get(directory));
                            List<String> files_list = walk.filter(Files::isRegularFile)
                                            .map(x -> x.toString()).collect(Collectors.toList());                      

                            String out = "";
                            if(files_list != null) {
                                for(String f : files_list) {
    //                                Pattern pattern = Pattern.compile(directory+"/*(.+)");
    //                                Matcher matcher = pattern.matcher(f.replace("\\", "/"));
    //                                if(matcher.find()) {
    //                                    out=out+"\n"+matcher.group(1);
    //                                }
                                    out=out+"\n"+f;
                                } 
                            }
                            if(!out.equals("")) Server_HTTP.response(he, 200, out.trim()); 
                            else Server_HTTP.response(he, 200, "\n");
                        } else Server_HTTP.response(he, 400, "Error query: directory "+directory+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set directory.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch (IOException e) {
            e.printStackTrace();
            Neb.logger.Println(e.toString(), Neb.logger.INFO);
        }
    }
}

class GetFile implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                Headers h = he.getResponseHeaders();
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    if(file != null) {
                        File newFile = new File(file);
//                        System.out.println("Get file: " + newFile.getName());
                        if(newFile.exists()) {
                            byte[] allBytes = Files.readAllBytes(Paths.get(file));
                            h.add("Content-Type", "application/json");
                            he.sendResponseHeaders(200, allBytes.length);
                            OutputStream os = he.getResponseBody();
                            os.write(allBytes);
                            os.close(); 
                        } else {
                            Server_HTTP.response(he, 400, "Error query: File "+file+" not found.");  
                        }
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch (IOException e) {
            e.printStackTrace();
            Neb.logger.Println(e.toString(), Neb.logger.INFO);
        }
    }
}

class GetFileAttributes implements HttpHandler {
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                Headers h = he.getResponseHeaders();
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    if(file != null) {
                        File newFile = new File(file);
    //                    System.out.println("Get file attributes: " + newFile.getName());
                        if(newFile.exists()) {
                            long size = newFile.length();
                            long modify_time = newFile.lastModified();
                            String out = "{ \"size\": \""+String.valueOf(size)+"\", \"create_time\": \""+utils.GetFileCreateTime(file)+"\", \"modify_time\": \""+String.valueOf(modify_time)+"\" }";
                            Server_HTTP.response(he, 200, out);                         
                        } else {
                            Server_HTTP.response(he, 400, "Error query: File "+file+" not found.");  
                        }                    

                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class SetInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            try {
                                Headers requestHeaders = he.getRequestHeaders();
                                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                                InputStream is = he.getRequestBody();
                                byte[] data = new byte[contentLength];
                                int length = is.read(data);
                                String str = new String(data);

                                String[] mas1 = new String[4];
                                mas1[0] = file;
                                mas1[1] = "SET";
                                mas1[2] = key;
                                mas1[3] = str;
                                Server_HTTP.file_key_val_list.add(mas1);

        //                        synchronized(QueueWorker.queue) {
        //                            if(QueueWorker.queue.get(file) != null) {
        //                                ArrayList old_list = QueueWorker.queue.get(file);
        //                                old_list.addAll(tmp_list);
        //                            } else QueueWorker.queue.put(file, tmp_list);
        //                        }

                                Server_HTTP.response(he, 200, "OK");                                              
                            } catch (IOException ex) {
                                Server_HTTP.response(he, 400, "ERR");
                                ex.printStackTrace();
                                Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                            }
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class SetChunk implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    if(file != null) {
                        if((new File(file)).exists()) {
                            try {
                                Headers requestHeaders = he.getRequestHeaders();
                                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                                InputStream is = he.getRequestBody();
                                byte[] data = new byte[contentLength];
                                int length = is.read(data);
                                String str = new String(data);

        //                        System.out.println(str);

                                for(String line : str.split("\n")) {
                                    String[] mas = line.split(";");
                                    if(mas.length == 2) {
                                        String key = mas[0];
                                        String val = mas[1];
                                        String[] mas1 = new String[4];
                                        mas1[0] = file;
                                        mas1[1] = "SET";
                                        mas1[2] = key;
                                        mas1[3] = val;
                                        Server_HTTP.file_key_val_list.add(mas1);
                                    }
                                }
        //                        synchronized(QueueWorker.queue) {
        //                            if(QueueWorker.queue.get(file) != null) {
        //                                ArrayList old_list = QueueWorker.queue.get(file);
        //                                old_list.addAll(tmp_list);
        //                            } else QueueWorker.queue.put(file, tmp_list);
        //                        }

                                Server_HTTP.response(he, 200, "OK");
                            } catch (IOException ex) {
                                Server_HTTP.response(he, 400, "ERR");
                                ex.printStackTrace();
                                Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                            }
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class Delete implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            String[] mas1 = new String[4];
                            mas1[0] = file;
                            mas1[1] = "DELETE";
                            mas1[2] = key;
                            mas1[3] = null;
                            Server_HTTP.file_key_val_list.add(mas1);

                            Server_HTTP.response(he, 200, "OK"); 
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");  
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class Del_From_List implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he)) {        
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            try {
                                Headers requestHeaders = he.getRequestHeaders();
                                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                                InputStream is = he.getRequestBody();
                                byte[] data = new byte[contentLength];
                                int length = is.read(data);
                                String str = new String(data);

                                String[] mas1 = new String[4];
                                mas1[0] = file;
                                mas1[1] = "DEL_FROM_LIST";
                                mas1[2] = key;
                                mas1[3] = str;
                                Server_HTTP.file_key_val_list.add(mas1);

                                Server_HTTP.response(he, 200, "OK");                                              
                            } catch (IOException ex) {
                                Server_HTTP.response(he, 400, "ERR");
                                ex.printStackTrace();
                                Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                            }
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class Add_To_List implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he)) {          
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    Map<String, String> params = Neb.utils.queryToMap(he.getRequestURI().getQuery());
                    String file = params.get("file");
                    String key = params.get("key");
                    if(file != null && key != null) {
                        if((new File(file)).exists()) {
                            try {
                                Headers requestHeaders = he.getRequestHeaders();
                                int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
            //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                                InputStream is = he.getRequestBody();
                                byte[] data = new byte[contentLength];
                                int length = is.read(data);
                                String str = new String(data);

                                String[] mas1 = new String[4];
                                mas1[0] = file;
                                mas1[1] = "ADD_TO_LIST";
                                mas1[2] = key;
                                mas1[3] = str;
                                Server_HTTP.file_key_val_list.add(mas1);

                                Server_HTTP.response(he, 200, "OK");                                              
                            } catch (IOException ex) {
                                Server_HTTP.response(he, 400, "ERR");
                                ex.printStackTrace();
                                Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                            }
                        } else Server_HTTP.response(he, 400, "Error query: file "+file+" not exist!");
                    } else {
                        Server_HTTP.response(he, 400, "Error query: not set file or key.");  
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class Commit implements HttpHandler {
    private static long timeout = 60*1000; // msec
    @Override
    public void handle(HttpExchange he) {
        try {
            if(Server_HTTP.check_client_write(he)) {  
                if(he.getRequestMethod().equalsIgnoreCase("GET")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    synchronized(QueueWorker.queue) {
    //                    System.out.println("Start commit ...");
                        for(String[] key_val : Server_HTTP.file_key_val_list) {
                            String file = key_val[0];
                            String command = key_val[1];
                            String key = key_val[2];
                            String val = key_val[3];
                            if(QueueWorker.queue.get(file) != null) {
                                ArrayList old_list = QueueWorker.queue.get(file);
                                String[] mas = new String[3];
                                mas[0] = command;
                                mas[1] = key;
                                mas[2] = val;
                                old_list.add(mas);
                            } else {
                                ArrayList<String[]> list = new ArrayList();
                                String[] mas = new String[3];
                                mas[0] = command;
                                mas[1] = key;
                                mas[2] = val;
                                list.add(mas);
                                QueueWorker.queue.put(file, list);
                            }                                
                        }
                        Server_HTTP.file_key_val_list = new ArrayList();
    //                    System.out.println("Stop commit.");
                    }

                    try { Thread.sleep(QueueWorker.sleep_timeout+1000); } catch (InterruptedException e) { }
                    long start_time = System.currentTimeMillis();
                    while(true) {
                        long time_cur = System.currentTimeMillis();
                        if(!QueueWorker.busy || time_cur - start_time > timeout) {
                            break;
                        }
                        try { Thread.sleep(100); } catch (InterruptedException e) { }
                    }
                    Server_HTTP.response(he, 200, "OK");
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not write acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class SnmpGet implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {  
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    try {
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
    //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        int length = is.read(data);
                        String str = new String(data);

                        ArrayList<String[]> list_node_community_ver_oid = new ArrayList();
                        String[] lines = str.split("\n");
                        for(String line : lines) {
                            String[] mas = line.split(";");
                            if(mas.length == 4) list_node_community_ver_oid.add(mas);
                        }

                        GetList getList = new GetList();
                        Map<String, ArrayList<String[]>> res = getList.Get(list_node_community_ver_oid);                    
                        Gson gson = new Gson();
                        String out = gson.toJson(res);

                        Server_HTTP.response(he, 200, out);                                              
                    } catch (Exception ex) {
                        Server_HTTP.response(he, 400, "ERR");
                        ex.printStackTrace();
                        Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class SnmpWalk implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    try {
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
    //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        int length = is.read(data);
                        String str = new String(data);

                        ArrayList<String[]> node_community_version_oid = new ArrayList();
                        String[] lines = str.split("\n");
                        for(String line : lines) {
                            String[] mas = line.split(";");
                            if(mas.length == 4) node_community_version_oid.add(mas);
                        }

                        WalkPool walkPool = new WalkPool();
                        Map<String, ArrayList> res = walkPool.Get(node_community_version_oid);

                        Gson gson = new Gson();
                        String out = gson.toJson(res);

                        Server_HTTP.response(he, 200, out);                                              
                    } catch (Exception ex) {
                        Server_HTTP.response(he, 400, "ERR");
                        ex.printStackTrace();
                        Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

class Ping implements HttpHandler {
    @Override
    public void handle(HttpExchange he) { 
        try {
            if(Server_HTTP.check_client_write(he) || Server_HTTP.check_client_read(he)) {
                if(he.getRequestMethod().equalsIgnoreCase("POST")) {
                    HttpExchange httpExchange = (HttpExchange) he;
                    he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");            
                    try {
                        Headers requestHeaders = he.getRequestHeaders();
                        int contentLength = Integer.parseInt(requestHeaders.getFirst("Content-length"));
    //                        System.out.println(""+requestHeaders.getFirst("Content-length"));

                        InputStream is = he.getRequestBody();
                        byte[] data = new byte[contentLength];
                        int length = is.read(data);
                        String str = new String(data);

                        ArrayList<String> node_list = new ArrayList();
                        String[] lines = str.split("\n");
                        for(String line : lines) {
                            if(line.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) node_list.add(line);
                        }

                        PingPool pingPool = new PingPool();
                        Map<String, String> res = pingPool.Get(node_list);

                        Gson gson = new Gson();
                        String out = gson.toJson(res);

                        Server_HTTP.response(he, 200, out);                                              
                    } catch (Exception ex) {
                        Server_HTTP.response(he, 400, "ERR");
                        ex.printStackTrace();
                        Neb.logger.Println(ex.toString(), Neb.logger.INFO);
                    }
                }
            } else Server_HTTP.response(he, 300, "Error query: This client not read acces to server.");
        } catch(Exception ex) {
            ex.printStackTrace();
        }        
    }
}

//class WatchInformationFile extends Thread {
////    private String filename;
//    
//    public WatchInformationFile() {
////        this.filename = filename;
//    }    
//    public void run() {
//        File f_map = new File(Neb.map_file);
//        long prev_time_file_map=0;
//        File f_cfg = new File(Neb.neb_cfg);
//        long prev_time_file_cfg=0;        
//        while(true) {
//            long time_file_map = f_map.lastModified();
//            if (time_file_map != prev_time_file_map) {             
//                prev_time_file_map=time_file_map;
//                File file = new File(Neb.map_file);
//                if(file.exists()) {                
//                    System.out.println("Reload "+Neb.map_file+" file.");
//                    Server_HTTP.INFO = Neb.utils.ReadJSONFile(Neb.map_file);
//                    System.out.println("Stop reload "+Neb.map_file+" file.");
//                }
//            }
//            
//            long time_file_cfg = f_cfg.lastModified();
//            if (time_file_cfg != prev_time_file_cfg) {             
//                prev_time_file_cfg=time_file_cfg;
//                File file = new File(Neb.neb_cfg);
//                if(file.exists()) { 
//                    Neb.cfg = utils.ReadConfig(Neb.neb_cfg);
//                    System.out.println("Reload "+Neb.neb_cfg+" file.");
//                }
//            }            
//
//            try { Thread.sleep(10000); } catch(java.lang.InterruptedException e) {}
//        }
//    }
//}