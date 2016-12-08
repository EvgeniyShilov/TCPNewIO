import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {

    SocketChannel server;
    BufferedReader user;

    public void init() {
        user = new BufferedReader(new InputStreamReader(System.in));
    }

    public void connect(String host, int port) throws IOException {
        InetSocketAddress hostAddress = new InetSocketAddress(host, port);
        server = SocketChannel.open(hostAddress);
        System.out.println(server.socket().getRemoteSocketAddress() + " connected");
    }

    public void run() {
        try {
            while (true) {
                String command = user.readLine();
                if (command.length() == 0) continue;
                if (process(command)) break;
                String response = receiveLine();
                System.out.println(server.socket().getRemoteSocketAddress() + " >>> " + response);
            }
        } catch (IOException ignored) {
        } finally {
            System.out.println(server.socket().getRemoteSocketAddress() + " disconnected");
        }
    }

    private boolean process(String command) throws IOException {
        int delimiterIndex = command.indexOf(" ");
        String act = delimiterIndex == -1 ? command.toLowerCase()
                : command.toLowerCase().substring(0, delimiterIndex).trim();
        switch (act) {
            case "download":
                download(command);
                break;
            case "close":
                send(command);
                return true;
            default:
                send(command);
        }
        return false;
    }

    private void download(String command) throws IOException {
        //TODO: delete placeholder
        send(command);
    }

    private void send(String request) throws IOException {
        server.write(ByteBuffer.wrap(request.getBytes()));
        System.out.println(server.socket().getRemoteSocketAddress().toString() + " <<< " + request);
    }

    private String receiveLine() throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        server.read(readBuffer);
        //readBuffer.flip();
        return new String(readBuffer.array());
    }
}
