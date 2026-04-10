import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockRepository {
    public static int checkQty(int branchId, int drinkId) throws SQLException {
        Connection conn = Store.connection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT quantity FROM stock WHERE branch_id = ? AND drink_id = ?")) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, drinkId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("quantity");
            }
        }
        return 0;
    }

    public static int getThreshold(int branchId, int drinkId) throws SQLException {
        Connection conn = Store.connection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT threshold FROM stock WHERE branch_id = ? AND drink_id = ?")) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, drinkId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("threshold");
            }
        }
        return 0;
    }

    public static void deduct(int branchId, int drinkId, int qty) throws SQLException {
        Connection conn = Store.connection();
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE stock SET quantity = quantity - ? WHERE branch_id = ? AND drink_id = ?")) {
            stmt.setInt(1, qty);
            stmt.setInt(2, branchId);
            stmt.setInt(3, drinkId);
            stmt.executeUpdate();
        }
    }

    public static Map<String, Object> transferFromHQ(int branchId, int drinkId, int qty) throws SQLException {
        Connection conn = Store.connection();
        Map<String, Object> result = new LinkedHashMap<>();
        int hqQty = checkQty(1, drinkId);
        if (hqQty < qty) {
            result.put("error", "HQ has insufficient stock");
            return result;
        }
        
        try (PreparedStatement addStmt = conn.prepareStatement("UPDATE stock SET quantity = quantity + ? WHERE branch_id = ? AND drink_id = ?")) {
            addStmt.setInt(1, qty);
            addStmt.setInt(2, branchId);
            addStmt.setInt(3, drinkId);
            addStmt.executeUpdate();
        }
        deduct(1, drinkId, qty);
        
        result.put("success", true);
        return result;
    }

    public static List<Map<String, Object>> getLow() throws SQLException {
        Connection conn = Store.connection();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, s.quantity, s.threshold
            FROM stock s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            WHERE s.quantity <= s.threshold
            ORDER BY s.quantity ASC
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("branch_id", rs.getInt("branch_id"));
                map.put("branch_name", rs.getString("branch_name"));
                map.put("drink_id", rs.getInt("drink_id"));
                map.put("drink_name", rs.getString("drink_name"));
                map.put("quantity", rs.getInt("quantity"));
                map.put("threshold", rs.getInt("threshold"));
                list.add(map);
            }
        }
        return list;
    }

    public static List<Map<String, Object>> getAll() throws SQLException {
        Connection conn = Store.connection();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, s.quantity, s.threshold,
                   (s.quantity <= s.threshold) as low
            FROM stock s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            ORDER BY s.branch_id, s.drink_id
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("branch_id", rs.getInt("branch_id"));
                map.put("branch_name", rs.getString("branch_name"));
                map.put("drink_id", rs.getInt("drink_id"));
                map.put("drink_name", rs.getString("drink_name"));
                map.put("quantity", rs.getInt("quantity"));
                map.put("threshold", rs.getInt("threshold"));
                map.put("low", rs.getBoolean("low"));
                list.add(map);
            }
        }
        return list;
    }
}
