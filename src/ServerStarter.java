import java.io.IOException;

public class ServerStarter {

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.init("localhost", Constants.SERVER_PORT);
        server.listen();
    }
}
