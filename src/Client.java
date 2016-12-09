import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Client {

    private SocketChannel server;
    private List<Pair<File, String>> underDownloadedFiles = new ArrayList<>();

    public void connect(String host, int port) throws IOException {
        InetSocketAddress hostAddress = new InetSocketAddress(host, port);
        server = SocketChannel.open(hostAddress);
        System.out.println(server.socket().getRemoteSocketAddress() + " connected");
    }

    public void run() {
        try {
            BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String command = user.readLine().trim();
                if (command.length() == 0) continue;
                if (process(command)) break;
            }
        } catch (IOException ignored) {
            ignored.printStackTrace();
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
                prepareDownload(command);
                break;
            case "close":
                send(command);
                return true;
            default:
                send(command);
                String response = receiveLine();
                System.out.println(server.socket().getRemoteSocketAddress() + " >>> " + response);
        }
        return false;
    }

    private void prepareDownload(String command) throws IOException {
        int delimiterIndex = command.indexOf(" ");
        String filename = delimiterIndex == -1 ? "" : command.substring(delimiterIndex).trim();
        File file = new File("client " + filename);
        if (file.exists()) {
            Pair<File, String> desiredFile = findUnderDownloadedFileBy(file.getName());
            if (desiredFile != null) {
                if (desiredFile.getValue().equals(server.socket().getRemoteSocketAddress().toString()))
                    download(command, file, file.length());
                else {
                    file.delete();
                    underDownloadedFiles.remove(desiredFile);
                    download(command, file, 0);
                }
            } else System.out.println("File already exists");
        } else download(command, file, 0);
    }

    private Long checkFileOnRemote(String command, long offset) throws IOException {
        send(command + " " +String.valueOf(offset));
        String response = receiveLine();
        if (response.trim().equals("No file")) {
            System.out.println(server.socket().getRemoteSocketAddress() + " >>> " + response);
            return null;
        } else {
            System.out.println(server.socket().getRemoteSocketAddress() + " >>> File size: " + response);
            return Long.parseLong(response.trim());
        }
    }

    private void download(String command, File file, long offset) throws IOException {
        Long fileSize = checkFileOnRemote(command, 0);
        if (fileSize == null) return;
        if (!file.exists()) {
            file.createNewFile();
            underDownloadedFiles.add(new Pair<>(file, server.socket().getRemoteSocketAddress().toString()));
        }
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(
                Paths.get(file.getPath()), StandardOpenOption.WRITE);
        while (true) {
            if (offset == fileSize) {
                Pair<File, String> desiredFile = findUnderDownloadedFileBy(file.getName());
                if (desiredFile != null)
                    if (desiredFile.getValue().equals(server.socket().getRemoteSocketAddress().toString())) {
                        underDownloadedFiles.remove(desiredFile);
                        System.out.println("File was downloaded");
                        fileChannel.close();
                        break;
                    }
            }
            byte[] response = receiveByteArray();
            int bytesRead = offset + response.length >= fileSize ? (int) (fileSize - offset) : response.length;
            fileChannel.write(ByteBuffer.wrap(Arrays.copyOfRange(response, 0, bytesRead)), offset);
            offset += bytesRead;
            System.out.println(server.socket().getRemoteSocketAddress() + " >>> " + bytesRead + " bytes");
        }
    }

    private Pair<File, String> findUnderDownloadedFileBy(String name) {
        for (Pair<File, String> underDownloadedFile : underDownloadedFiles)
            if (name.toUpperCase().equals(underDownloadedFile.getKey().getName().toUpperCase()))
                return underDownloadedFile;
        return null;
    }

    private void send(String request) throws IOException {
        server.write(ByteBuffer.wrap(request.getBytes()));
        System.out.println(server.socket().getRemoteSocketAddress().toString() + " <<< " + request);
    }

    private String receiveLine() throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        server.read(readBuffer);
        return new String(readBuffer.array());
    }

    private byte[] receiveByteArray() throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
        server.read(readBuffer);
        readBuffer.flip();
        return readBuffer.array();
    }
}
