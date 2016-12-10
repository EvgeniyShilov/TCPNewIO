import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Application {

    public static void main(String[] args) {
        BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
        loop:
        while (true) {
            System.out.println("Type 's' to start server");
            System.out.println("Type 'c' to start client");
            System.out.println("Type 'q' to quit");
            String line;
            try {
                line = user.readLine();
                if (line.equals("")) continue;
            } catch (IOException e) {
                continue;
            }
            switch (line.charAt(0)) {
                case 's':
                    Server server = new Server();
                    while (true) {
                        try {
                            server.init();
                            server.listen();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                case 'c':
                    Client client = new Client();
                    while (true) {
                        try {
                            System.out.println("Server IP?");
                            line = user.readLine();
                            client.connect(line);
                            client.run();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                case 'q':
                    break loop;
            }
        }
    }
}
