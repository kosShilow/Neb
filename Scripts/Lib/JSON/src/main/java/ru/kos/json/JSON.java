package json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.*;
import org.json.simple.parser.*;

public class JSON {
    public Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> retMap = new HashMap<>();

        if(json != null) {
            retMap = toMap(json);
        }
        return retMap;
    }
    
    private Map<String, Object> toMap(JSONObject object) {
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
        
    private List<Object> toList(JSONArray array) {
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
    
//    public String mapToJSON(Map<String, Object> map) {
//        return JSONValue.toJSONString(map);
//    }
    
    public String mapToJSON(Map map) {
        return JSONValue.toJSONString(map);
    }    

    public String listToJSON(List<Object> list) {
        return JSONValue.toJSONString(list);
    }
}
