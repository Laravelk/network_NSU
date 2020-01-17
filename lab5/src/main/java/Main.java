import socks5.Proxy;

import java.io.IOException;
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect number of input parameters");
            System.out.println("You should write port");
            return;
        }
        System.out.println("fff");
        try {
            int port = Integer.parseInt(args[0]);
            new Proxy(port).run();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
