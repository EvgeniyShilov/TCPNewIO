import java.io.IOException;

public class ServerStarter {

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.init("192.168.1.11", Constants.SERVER_PORT);
        server.listen();
    }
}
