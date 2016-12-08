import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class Server {

    private Selector selector;
    private ServerSocketChannel server;

    public void init(String host, int port) throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress(host, port));
        server.register(selector, server.validOps());
    }

    public void listen() throws IOException {
        while (selector.selectNow() > -1) {
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isValid()) try {
                    if (key.isAcceptable()) accept();
                    else if (key.isReadable()) read(key);
                } catch (Exception e) {
                    close(key);
                }
            }
        }
    }

    private void accept() throws IOException {
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, client.socket().getPort());
        System.out.println(client.socket().getRemoteSocketAddress() + " connected");
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        int bytesRead = client.read(buffer);
        if (bytesRead < 0) close(key);
        else {
            buffer.flip();
            String command = new String(buffer.array()).trim();
            System.out.println(client.socket().getRemoteSocketAddress() + " >>> " + command);
            process(key, command);
        }
    }

    private void process(SelectionKey key, String command) throws IOException {
        int delimiterIndex = command.indexOf(" ");
        String act = delimiterIndex == -1 ? command.toLowerCase()
                : command.toLowerCase().substring(0, delimiterIndex).trim();
        String argument = delimiterIndex == -1 ? " " : command.substring(delimiterIndex).trim();
        switch (act) {
            case "echo":
                send(key, argument);
                break;
            case "time":
                time(key);
                break;
            case "close":
                close(key);
                break;
            case "download":
                download(key, argument);
                break;
            default:
                send(key, "Wtf is " + act + "?");
        }
    }

    private void time(SelectionKey key) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
        Date date = new Date();
        send(key, "Server time: " + dateFormat.format(date));
    }

    private void download(SelectionKey key, String argument) throws IOException {
        //TODO: remove placeholder
        send(key, argument);
    }

    private void send(SelectionKey key, String response) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        client.write(ByteBuffer.wrap(response.getBytes()));
        System.out.println(client.socket().getRemoteSocketAddress().toString() + " <<< " + response);
    }

    private void send(SelectionKey key, byte[] response) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        client.write(ByteBuffer.wrap(response));
        System.out.println(client.socket().getRemoteSocketAddress().toString() + " <<< " + response.length + " bytes");
    }

    private void close(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        key.cancel();
        client.close();
        System.out.println(client.socket().getRemoteSocketAddress().toString() + " disconnected");
    }
}
