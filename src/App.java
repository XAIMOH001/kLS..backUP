public class App {
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Store.init();
        System.out.println("DB initialised");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new HttpGateway(port).start();
    }
}
