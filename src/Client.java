import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client {

    public void run(String host, int port) {
        try {
            InetSocketAddress hostAddress = new InetSocketAddress(host, port);
            SocketChannel client = SocketChannel.open(hostAddress);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = userInput.readLine();
                client.write(ByteBuffer.wrap(line.getBytes()));
                ByteBuffer readBuffer = ByteBuffer.allocate(256);
                client.read(readBuffer);
                System.out.println(new String(readBuffer.array()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
