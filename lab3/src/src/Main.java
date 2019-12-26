package src;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static final int ARGS_MIN = 3;
    public static final int EXIT_FAILURE = -1;

    public static void main(String[] args) throws InterruptedException {
        if (args.length < ARGS_MIN) {
            System.err.println("Too few arguments, required " + ARGS_MIN);
            System.exit(EXIT_FAILURE);
        }

        String name = args[0];
        int loss = Integer.parseInt(args[1]);
        int port = Integer.parseInt(args[2]);
        String parentAddress = null;

        int parentPort = 0;
        if (args.length == 5) {
            parentAddress = args[3];
            parentPort = Integer.parseInt(args[4]);
        }

        try {
            final Tree tree = new Tree(name, loss, port, parentAddress, parentPort);
            final Console console = new Console(tree);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("CTRL+C");
            }));

            tree.start();
            console.start();

            console.join();
            tree.join();
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
            System.exit(EXIT_FAILURE);
        }
    }
}
