package passwd;

import org.json.simple.*;
import org.json.simple.parser.*;
import com.google.gson.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.security.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Passwd {
    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("java -jar Passwd.jar user passwd");
            return;
        }

        String passwd_file = "passwd";
        String user = args[0];
        String passwd = args[1];
       
        System.out.println("Set user: "+user+" password: "+passwd);
        
        Map<String, String> accounts = new HashMap();
        if(new File(passwd_file).exists()) {
            accounts = ReadJSONFile(passwd_file);
        }
        
        String out_hash_md5 = MD5(user+":"+passwd);
        accounts.put(out_hash_md5, user);
        MapToFile(accounts, passwd_file);

    }
    
    private static String MD5(String str) {
        String out = null;
        try {
            byte[] bytesOfMessage = str.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            out = bytesToHex(thedigest);
        } catch(UnsupportedEncodingException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } 
        return out;
    }
    
    private static String bytesToHex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static Map ReadJSONFile(String filename) {
        Map result = new HashMap<>();
        JSONParser parser = new JSONParser();
        FileReader fr = null;
        try {
            /* Get the file content into the JSONObject */
            fr = new FileReader(filename);
            JSONObject jsonObject = (JSONObject)parser.parse(fr);
            result = toMap(jsonObject);
            fr.close();
        } catch (Exception ex) {
            if(fr != null) try {
                fr.close();
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
            ex.printStackTrace();
        }        
        
        return result;
    } 
    
    public static Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keySet().iterator();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    } 
    
    private static List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();
        for(int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }    
    
    private static boolean MapToFile(Map map, String filename) {
        Gson gson = new Gson(); 
        String str = gson.toJson(map);        
        return WriteStrToFile(filename, PrettyJSONOut(str));
    }

    private static String PrettyJSONOut(String str) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(str);
        String result = gson.toJson(je);        
        
        return result;
    }  
    
    private static boolean WriteStrToFile(String filename, String str) {
        try {
            Writer outFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
//            BufferedWriter outFile = new BufferedWriter(new FileWriter(filename));
            outFile.write(str);
            outFile.close();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }             
    }    
    
}
