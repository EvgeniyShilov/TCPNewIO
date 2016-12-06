public class Application {

    public static void main(String[] args) {
        new Server().run("localhost", 1337);
        //new Client().run("localhost", 1337);
    }
}
