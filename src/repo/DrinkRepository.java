import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DrinkRepository {
    public static List<Map<String, Object>> withStock(int branchId) throws SQLException {
        Connection conn = Store.connection();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT d.id, d.name, d.brand, d.price, 
                   COALESCE(s.quantity, 0) as quantity,
                   (COALESCE(s.quantity, 0) <= COALESCE(s.threshold, 0)) as low
            FROM drinks d
            LEFT JOIN stock s ON d.id = s.drink_id AND s.branch_id = ?
            ORDER BY d.id
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", rs.getInt("id"));
                    map.put("name", rs.getString("name"));
                    map.put("brand", rs.getString("brand"));
                    map.put("price", rs.getDouble("price"));
                    map.put("quantity", rs.getInt("quantity"));
                    map.put("low", rs.getBoolean("low"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public static double getPrice(int drinkId) throws SQLException {
        Connection conn = Store.connection();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT price FROM drinks WHERE id = ?")) {
            stmt.setInt(1, drinkId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("price");
            }
        }
        return 0;
    }
}
