package ru.kos.terminal;

import com.jcraft.jsch.*;
import java.io.FileWriter;
import org.apache.commons.net.telnet.TelnetClient;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.kos.expect.*;

public class Terminal {
    private static boolean DEBUG=false;
    private int timeout_ssh_telnet=10;
    private int timeout_expect=40;
    private static FileWriter file_writer = null;
    private static Log log;
    public static String node_node = null;
    
    public Terminal(int timeout_ssh_telnet, int timeout_expect) {
        this.timeout_ssh_telnet = timeout_ssh_telnet;
        this.timeout_expect = timeout_expect;
        
//        String pid = ManagementFactory.getRuntimeMXBean().getName();
//        log = new Log("out.out");
//        log.SetLevel(log.INFO);        
    }
    
//    public Terminal(FileWriter file_writer) {
//        this.file_writer = file_writer;
//    }
    
    public ArrayList ConnectTelnet(String node, String user, String passwd, String passwd_enable, List<Pattern> stop_symbols) {
        ArrayList result = new ArrayList();
        TelnetClient channel = null;
        Expect expect = null;
        List<Pattern> new_stop_symbols = new ArrayList();
        
        stop_symbols.add(Pattern.compile(".*[Ii]ncorrect.*"));
        stop_symbols.add(Pattern.compile(".*[Ff]ail.*"));
        stop_symbols.add(Pattern.compile(".*[Ii]nvalid.*"));
        stop_symbols.add(Pattern.compile("[Uu]sername\\s*:\\s*$"));
        stop_symbols.add(Pattern.compile("[Pp]assword\\s*:\\s*$"));
        stop_symbols.add(Pattern.compile("[Ll]ogin\\s*:\\s*$"));        
//        if(DEBUG) {
//            for(Pattern stop_symbol : stop_symbols) {
//                System.out.println(stop_symbol.toString());
//            }
//        }
        try {
            channel = new TelnetClient();
            channel.setDefaultTimeout(timeout_ssh_telnet * 1000);
            channel.connect(node);  
            expect = new Expect(channel.getInputStream(),
                                        channel.getOutputStream());
            List<Pattern> s_s1 = new ArrayList();
//            s_s1.add(Pattern.compile("[Uu]sername\\s*:\\s*$"));
//            s_s1.add(Pattern.compile("[Pp]assword\\s*:\\s*$"));
//            s_s1.add(Pattern.compile("[Ll]ogin\\s*:\\s*$"));
            expect.expect(timeout_expect, stop_symbols);
            //////////////////
//            To_File(file_writer, expect);
            ///////////////////            
            if(DEBUG) System.out.println(expect.match);
//            log.Println(node_node+" - "+expect.match, log.DEBUG);
            if(DEBUG) System.out.println("before="+expect.before);
            if(expect.match != null) {
                expect.send(user+"\n");
                if(DEBUG) System.out.println("send="+user);
//                log.Println(node_node+" - "+"send="+user, log.DEBUG);
                //////////////////
//                if(file_writer != null) file_writer.write("send="+user+"\n");
                ///////////////////                
                expect.expect(timeout_expect, stop_symbols);
                //////////////////
//                To_File(file_writer, expect);
                ///////////////////                
                if(DEBUG) System.out.println(expect.match);
//                log.Println(node_node+" - "+expect.match, log.DEBUG);
                if(DEBUG) System.out.println("before="+expect.before);
                if(expect.match != null) {
                    expect.send(passwd+"\n");
                    //////////////////
//                    if(file_writer != null) file_writer.write("send="+passwd+"\n");
                    ///////////////////                     
                    if(DEBUG) System.out.println("send="+passwd);
//                    log.Println(node_node+" - "+"send="+passwd, log.DEBUG);
                    expect.expect(timeout_expect, stop_symbols);
                    //////////////////
//                    To_File(file_writer, expect);
                    ///////////////////                    
                    if(DEBUG) System.out.println(expect.match);
//                    log.Println(node_node+" - "+expect.match, log.DEBUG);
                    if(DEBUG) System.out.println("before="+expect.before);
                    List<String> err_symbols = new ArrayList();
                    err_symbols.add(".*[Ii]ncorrect.*");
                    err_symbols.add(".*[Ii]nvalid.*");
                    err_symbols.add(".*[Ff]ail.*");            
                    boolean err=false;
                    if(expect.match != null) {
                        for(String shablon : err_symbols) {
                            if(expect.match.matches(shablon)) {
                                err=true;
                                break;
                            }
                        }
                    }

                    if(expect.match != null && !err) {
                        Pattern p = Pattern.compile("[\n\r]\\S+>"); 
                        Matcher m = p.matcher(expect.match);  
                        if(m.matches() && !passwd_enable.equals("")) {
                            expect.send("enable\n");
                            //////////////////
//                            if(file_writer != null) file_writer.write("send="+"enable"+"\n");                   
                            ///////////////////                             
                            if(DEBUG) System.out.println("send="+"enable");
                            expect.expect(timeout_expect, stop_symbols);
                            //////////////////
//                            To_File(file_writer, expect);
                            ///////////////////                             
                            if(DEBUG) System.out.println(expect.match);
                            if(DEBUG) System.out.println("before="+expect.before);
                            expect.send(passwd_enable+"\n");
                            //////////////////
//                            if(file_writer != null) file_writer.write("send="+passwd_enable+"\n");                            
                            ///////////////////                              
                            if(DEBUG) System.out.println("send="+passwd_enable);
                            expect.expect(timeout_expect, stop_symbols); 
                            //////////////////
//                            To_File(file_writer, expect);
                            ///////////////////                              
                            if(DEBUG) System.out.println(expect.match);
                            if(DEBUG) System.out.println("before="+expect.before);
                            new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("[\r\n]", "").replaceAll("([^a-zA-Z0-9_-])","\\\\$1")));
                        } else new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("[\r\n]", "").replaceAll("([^a-zA-Z0-9_-])","\\\\$1")));
                        if(channel != null && expect != null) {
                            result.add(channel);
                            result.add(expect);
                            result.add(new_stop_symbols);
                        }                 
                    }
                }
            }
        } catch (IOException e) {
            if(DEBUG) e.printStackTrace();
        }        
        
        return result;
    }
    
    
    public ArrayList ConnectSSH(String node, String user, String passwd, String passwd_enable, List<Pattern> stop_symbols) {
        ArrayList result = new ArrayList();
        Channel channel = null;
        Expect expect = null;
        List<Pattern> new_stop_symbols = new ArrayList();
        
        stop_symbols.add(Pattern.compile(".*[Ii]ncorrect.*"));
        stop_symbols.add(Pattern.compile(".*[Ff]ail.*"));
        stop_symbols.add(Pattern.compile(".*[Ii]nvalid.*"));
        stop_symbols.add(Pattern.compile("[Uu]sername\\s*:\\s*$"));
        stop_symbols.add(Pattern.compile("[Pp]assword\\s*:\\s*$"));
        stop_symbols.add(Pattern.compile("[Ll]ogin\\s*:\\s*$"));        
 
//        if(DEBUG) {
//            for(Pattern stop_symbol : stop_symbols) {
//                System.out.println(stop_symbol.toString());
//            }
//        }        
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, node);
            session.setPassword(passwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(timeout_ssh_telnet * 1000);
            channel = session.openChannel("shell");
            channel.connect();   
            expect = new Expect(channel.getInputStream(),
                                        channel.getOutputStream());
            expect.expect(timeout_expect, stop_symbols);
            //////////////////
//            To_File(file_writer, expect);
            ///////////////////             
            if(DEBUG) System.out.println(expect.match);
            if(DEBUG) System.out.println("before="+expect.before);
//            log.Println(node_node+" - "+expect.match, log.DEBUG);
            if(expect.match == null) {
                expect.send("\n");
                expect.expect(timeout_expect, stop_symbols);
                //////////////////
//                To_File(file_writer, expect);
                ///////////////////                  
                if(DEBUG) System.out.println(expect.match);
                if(DEBUG) System.out.println("before="+expect.before);
//                log.Println(node_node+" - "+expect.match, log.DEBUG);                
            }
            
            List<String> err_symbols = new ArrayList();
            err_symbols.add(".*[Ii]ncorrect.*");
            err_symbols.add(".*[Ii]nvalid.*");
            err_symbols.add(".*[Ff]ail.*");           
            boolean err=false;
            for(String shablon : err_symbols) {
                if(expect.match.matches(shablon)) {
                    err=true;
                    break;
                }
            }            

            if(expect.match != null && !err) {
                Pattern p = Pattern.compile("[\n\r]\\S+>"); 
                Matcher m = p.matcher(expect.match);                 
                if(m.matches() && !passwd_enable.equals("")) {
                    expect.send("enable\n");
                    expect.expect(timeout_expect, stop_symbols);
                    //////////////////
//                    To_File(file_writer, expect);
                    ///////////////////                      
                    if(DEBUG) System.out.println(expect.match);
                    if(DEBUG) System.out.println("before="+expect.before);
                    expect.send(passwd_enable+"\n");
                    expect.expect(timeout_expect, stop_symbols);
                    //////////////////
//                    To_File(file_writer, expect);
                    ///////////////////                      
                    if(DEBUG) System.out.println(expect.match);
                    if(DEBUG) System.out.println("before="+expect.before);
                    new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("([^a-zA-Z0-9_-])","\\\\$1")));
                } else new_stop_symbols.add(Pattern.compile(expect.match.replaceAll("([^a-zA-Z0-9_-])","\\\\$1")));
                if(channel != null && expect != null) {
                    result.add(channel);
                    result.add(expect);
                    result.add(new_stop_symbols);
                }                
                    
            }

        } catch (Exception e) {
            if(DEBUG) e.printStackTrace();
        }        
        
        return result;
    }

    public void Disconnect(ArrayList connection) {
        if(connection.size() == 3) {
            if(connection.get(0) instanceof Channel) DisconnectSSH(connection);
            else DisconnectTelnet(connection);
        }
    }
    
    public void DisconnectTelnet(ArrayList connection) {
        if(connection.size() == 3) {
            TelnetClient channel = (TelnetClient)connection.get(0);
            Expect expect = (Expect)connection.get(1);
            expect.send("exit\n");
            expect.send("quit\n");
            expect.close();
            try {
                channel.disconnect();
            } catch (Exception ex) {
                //////////////////
//                try { file_writer.write("DisconnectTelnet: "+ex.getMessage()+"\n"); } catch (IOException ex1) {}
                ///////////////////                 
                if(DEBUG) java.util.logging.Logger.getLogger(Terminal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void DisconnectSSH(ArrayList connection) {
        if(connection.size() == 3) {
            Channel channel = (Channel)connection.get(0);
            Expect expect = (Expect)connection.get(1);
            expect.send("exit\n");
            expect.send("quit\n");
            expect.close();
            channel.disconnect();
        }
    }
    
    public String RunCommand(Expect expect, String cmd, List<Pattern> stop_symbols) {
        String out="";
//        try {
            expect.send("terminal length 0\n");
//            log.Println(node_node+" - "+"terminal length 0", log.DEBUG);
            //////////////////
//            if(file_writer != null) file_writer.write("send="+"terminal length 0\n");                            
            ///////////////////                              
            expect.expect(timeout_expect, stop_symbols);
//            log.Println("stop_symbols - "+stop_symbols.toString(), log.DEBUG);
//            if(expect.match == null) {
//                log.Println("Not matching terminal length 0 !!!", log.DEBUG);
//                log.Println("expect.buffer="+expect.buffer, log.DEBUG);
//            }
            //////////////////
//            To_File(file_writer, expect);
            ///////////////////          
    //        System.out.println("before="+expect.before);
            expect.send(cmd+"\n");
//            log.Println(node_node+" - "+"cmd="+cmd, log.DEBUG);
            //////////////////
//            if(file_writer != null) file_writer.write("send="+cmd+"\n");                            
            ///////////////////                
            expect.expect(timeout_expect, stop_symbols);
//            log.Println("stop_symbols - "+stop_symbols.toString(), log.DEBUG);
//            if(expect.match == null) {
//                log.Println("Not matching cmd="+cmd+" !!!", log.DEBUG);            
//                log.Println("expect.buffer="+expect.buffer, log.DEBUG);
//            }
            //////////////////
//            To_File(file_writer, expect);
            ///////////////////          
            if(expect.before != null) {
                String[] mas = expect.before.split("\n");
                if(mas.length > 1) for(int i=1; i<mas.length; i++) out=out+"\n"+mas[i];
            }
//            log.Println(node_node+" - "+"out="+out, log.DEBUG);
//        } catch(IOException ex) {}
        return out;
    }
    
//    private void To_File(FileWriter file_writer, Expect expect) {
//        try {
//        if(file_writer != null) {
//            file_writer.write("================================\n");
//            file_writer.write(expect.match+"\n");
//            file_writer.write("---------------------------------\n");
//            file_writer.write("before="+expect.before+"\n");
//        }
//        } catch(IOException ex) {}
    //    }
}
