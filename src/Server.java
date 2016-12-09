import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private Selector selector;
    private ServerSocketChannel server;
    private List<FileForDownloading> files;

    public void init(String host, int port) throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress(host, port));
        server.register(selector, server.validOps());
        files = new CopyOnWriteArrayList<>();
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
                    e.printStackTrace();
                    close(key);
                }
            }
            for (FileForDownloading file : files) sendNextPart(file);
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

    private void download(SelectionKey key, String fileParams) throws IOException {
        int delimiterIndex = fileParams.indexOf(" ");
        String filename = fileParams.toLowerCase().substring(0, delimiterIndex).trim();
        File file = new File(filename);
        if (!file.exists()) {
            send(key, "No file");
            return;
        }
        send(key, String.valueOf(file.length()));
        long downloadedBytes = Long.parseLong(fileParams.substring(delimiterIndex).trim());
        files.add(new FileForDownloading(file, key, downloadedBytes));
    }

    private void sendNextPart(FileForDownloading file) throws IOException {
        RandomAccessFile fileReader = file.getReader();
        long downloadedBytes = file.getDownloadedBytes();
        fileReader.seek(downloadedBytes);
        byte[] bytes = new byte[Constants.BUFFER_SIZE];
        int countBytes = fileReader.read(bytes);
        if (countBytes <= 0) {
            System.out.println("File was downloaded");
            file.closeReader();
            files.remove(file);
            return;
        }
        send(file.getKey(), bytes);
        file.setDownloadedBytes(downloadedBytes + countBytes);
        System.out.println(file.getClient().socket().getRemoteSocketAddress() + " <<< " + countBytes + " bytes");
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

    private static class FileForDownloading {

        private SelectionKey key;
        private SocketChannel client;
        private long downloadedBytes;
        private RandomAccessFile reader;

        public FileForDownloading(File file, SelectionKey key, long downloadedBytes) throws FileNotFoundException {
            this.key = key;
            this.client = (SocketChannel) key.channel();
            this.downloadedBytes = downloadedBytes;
            this.reader = new RandomAccessFile(file, "r");
        }

        public SelectionKey getKey() {
            return key;
        }

        public SocketChannel getClient() {
            return client;
        }

        public RandomAccessFile getReader() {
            return reader;
        }

        public long getDownloadedBytes() {
            return downloadedBytes;
        }

        public void setDownloadedBytes(long downloadedBytes) {
            this.downloadedBytes = downloadedBytes;
        }

        public void closeReader() throws IOException {
            reader.close();
        }
    }
}
