import java.util.*;

public class Json {
    public static String toJson(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof String) {
            String s = (String) value;
            s = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
            return "\"" + s + "\"";
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append(toJson(entry.getKey())).append(":").append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + value.toString() + "\"";
    }

    public static Map<String, Object> parseBody(String body) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (body == null || body.trim().isEmpty()) return map;
        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1);
        }
        
        // Simple manual split parsing: splits on comma outside quotes
        boolean inQuotes = false;
        StringBuilder currentPair = new StringBuilder();
        List<String> pairs = new ArrayList<>();
        
        for (char c : body.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            if (c == ',' && !inQuotes) {
                pairs.add(currentPair.toString());
                currentPair.setLength(0);
            } else {
                currentPair.append(c);
            }
        }
        if (currentPair.length() > 0) pairs.add(currentPair.toString());
        
        for (String pair : pairs) {
            int colonIndex = pair.indexOf(':');
            if (colonIndex == -1) continue;
            
            String key = pair.substring(0, colonIndex).trim();
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            
            String val = pair.substring(colonIndex + 1).trim();
            
            if (val.startsWith("\"") && val.endsWith("\"")) {
                map.put(key, val.substring(1, val.length() - 1));
            } else if (val.equals("true") || val.equals("false")) {
                map.put(key, Boolean.parseBoolean(val));
            } else if (val.equals("null")) {
                map.put(key, null);
            } else {
                try {
                    if (val.contains(".")) {
                        map.put(key, Double.parseDouble(val));
                    } else {
                        map.put(key, Integer.parseInt(val));
                    }
                } catch (NumberFormatException e) {
                    map.put(key, val);
                }
            }
        }
        return map;
    }
}
