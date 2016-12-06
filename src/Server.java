import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Server {

    public void run(String host, int port) {
        try {
            Selector selector = Selector.open();
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(host, port));
            server.register(selector, server.validOps());
            ByteBuffer buffer = ByteBuffer.allocate(256);
            while (selector.select() > -1) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        try {
                            if (key.isAcceptable()) {
                                SocketChannel client = server.accept();
                                client.configureBlocking(false);
                                client.register(selector, SelectionKey.OP_READ, client.socket().getPort());
                            } else if (key.isReadable()) {
                                SocketChannel client = (SocketChannel) key.channel();
                                int bytesRead = client.read(buffer);
                                if (bytesRead < 0) {
                                    key.cancel();
                                    client.close();
                                } else {
                                    buffer.flip();
                                    client.write(buffer);
                                    buffer.clear();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            //TODO: close(key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
}
