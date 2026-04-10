import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {
    public static Map<String, Object> place(String name, String phone, int branchId, int drinkId, int qty) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        int available = StockRepository.checkQty(branchId, drinkId);
        if (available < qty) {
            result.put("error", "Insufficient stock");
            return result;
        }

        double price = DrinkRepository.getPrice(drinkId);
        double total = price * qty;

        Connection conn = Store.connection();
        int orderId = 0;
        String sql = "INSERT INTO orders (customer_name, customer_phone, branch_id, drink_id, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setInt(3, branchId);
            stmt.setInt(4, drinkId);
            stmt.setInt(5, qty);
            stmt.setDouble(6, price);
            stmt.setDouble(7, total);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) orderId = rs.getInt(1);
            }
        }

        StockRepository.deduct(branchId, drinkId, qty);
        
        int remaining = StockRepository.checkQty(branchId, drinkId);
        int threshold = StockRepository.getThreshold(branchId, drinkId);

        result.put("success", true);
        result.put("order_id", orderId);
        result.put("total", total);
        result.put("remaining", remaining);
        result.put("low_stock", remaining <= threshold);

        return result;
    }

    public static List<Map<String, Object>> all() throws SQLException {
        Connection conn = Store.connection();
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT o.id, o.customer_name, o.customer_phone, b.name as branch_name, d.name as drink_name, d.brand, 
                   o.quantity, o.unit_price, o.total_price, o.order_time
            FROM orders o
            JOIN branches b ON o.branch_id = b.id
            JOIN drinks d ON o.drink_id = d.id
            ORDER BY o.order_time DESC
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("customer_name", rs.getString("customer_name"));
                map.put("customer_phone", rs.getString("customer_phone"));
                map.put("branch_name", rs.getString("branch_name"));
                map.put("drink_name", rs.getString("drink_name"));
                map.put("brand", rs.getString("brand"));
                map.put("quantity", rs.getInt("quantity"));
                map.put("unit_price", rs.getDouble("unit_price"));
                map.put("total_price", rs.getDouble("total_price"));
                map.put("order_time", rs.getString("order_time"));
                list.add(map);
            }
        }
        return list;
    }
}
