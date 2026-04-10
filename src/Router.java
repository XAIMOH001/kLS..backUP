import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import java.util.Scanner;
import java.io.InputStream;

public class Router {
    public static Object branches(HttpExchange ex) throws Exception {
        return BranchRepository.getAll();
    }

    public static Object drinks(HttpExchange ex) throws Exception {
        String query = ex.getRequestURI().getQuery();
        int branchId = 0;
        if (query != null && query.contains("branch_id=")) {
            String[] parts = query.split("branch_id=");
            if (parts.length > 1) {
                branchId = Integer.parseInt(parts[1].split("&")[0]);
            }
        }
        return DrinkRepository.withStock(branchId);
    }

    private static Map<String, Object> parseJsonBody(HttpExchange ex) throws Exception {
        InputStream is = ex.getRequestBody();
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        String body = scanner.hasNext() ? scanner.next() : "";
        return Json.parseBody(body);
    }

    public static Object placeOrder(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) throw new Exception("POST required");
        Map<String, Object> body = parseJsonBody(ex);
        String name = (String) body.get("customer_name");
        String phone = (String) body.get("customer_phone");
        Integer branchId = (Integer) body.get("branch_id");
        Integer drinkId = (Integer) body.get("drink_id");
        Integer qty = (Integer) body.get("quantity");
        if (name == null || branchId == null || drinkId == null || qty == null) {
            throw new Exception("Missing required fields");
        }
        return OrderRepository.place(name, phone, branchId, drinkId, qty);
    }

    public static Object restock(HttpExchange ex) throws Exception {
        if (!"POST".equals(ex.getRequestMethod())) throw new Exception("POST required");
        Map<String, Object> body = parseJsonBody(ex);
        Integer branchId = (Integer) body.get("branch_id");
        Integer drinkId = (Integer) body.get("drink_id");
        Integer qty = (Integer) body.get("quantity");
        if (branchId == null || drinkId == null || qty == null) {
            throw new Exception("Missing required fields");
        }
        return StockRepository.transferFromHQ(branchId, drinkId, qty);
    }

    public static Object stock(HttpExchange ex) throws Exception {
        return StockRepository.getAll();
    }

    public static Object reportCustomers(HttpExchange ex) throws Exception {
        return ReportRepository.customerOrders();
    }

    public static Object reportBranches(HttpExchange ex) throws Exception {
        return ReportRepository.branchSales();
    }

    public static Object reportTotal(HttpExchange ex) throws Exception {
        return ReportRepository.grandTotal();
    }

    public static Object reportLowStock(HttpExchange ex) throws Exception {
        return StockRepository.getLow();
    }
}
