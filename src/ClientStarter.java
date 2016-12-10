import java.io.IOException;

public class ClientStarter {

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.connect("192.168.1.11", Constants.SERVER_PORT);
        client.run();
    }
}
