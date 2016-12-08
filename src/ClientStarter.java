import java.io.IOException;

public class ClientStarter {

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.init();
        client.connect("localhost", Constants.SERVER_PORT);
        client.run();
    }
}
