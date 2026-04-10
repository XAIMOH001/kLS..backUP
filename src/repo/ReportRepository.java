import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportRepository {
    public static List<Map<String, Object>> customerOrders() throws SQLException {
        return OrderRepository.all();
    }

    public static List<Map<String, Object>> branchSales() throws SQLException {
        Connection conn = Store.connection();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT b.id, b.name, b.city,
                   COALESCE(SUM(o.total_price), 0) as total_revenue,
                   COUNT(o.id) as total_orders
            FROM branches b
            LEFT JOIN orders o ON b.id = o.branch_id
            GROUP BY b.id
            ORDER BY b.id
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("name", rs.getString("name"));
                map.put("city", rs.getString("city"));
                map.put("total_revenue", rs.getDouble("total_revenue"));
                map.put("total_orders", rs.getInt("total_orders"));
                list.add(map);
            }
        }
        return list;
    }

    public static Map<String, Object> grandTotal() throws SQLException {
        Connection conn = Store.connection();
        Map<String, Object> map = new LinkedHashMap<>();
        String sql = "SELECT COALESCE(SUM(total_price), 0) as total_revenue, COUNT(id) as total_orders FROM orders";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                map.put("total_revenue", rs.getDouble("total_revenue"));
                map.put("total_orders", rs.getInt("total_orders"));
            }
        }
        return map;
    }
}
