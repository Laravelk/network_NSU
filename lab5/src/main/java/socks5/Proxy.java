package socks5;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Set;

public class Proxy implements Runnable, AutoCloseable {
    public Proxy(int port) throws IOException {
        Server server = new Server(port);
        server.register(this.selector);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                int count = selector.select(timeout);
                if (0 == count) continue;
                Set<SelectionKey> modified = selector.selectedKeys();
                for (SelectionKey selected : modified) {
                    SocksHandler key  = (SocksHandler) selected.attachment();
                    key.accept(selected);
                }
                modified.clear();
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        selector.close();
    }

    public static final byte SOCKS_5 = 0x05;
    public static final byte NO_AUTHENTICATION = 0x00;
    static final byte NO_ACCEPTABLE_METHODS = (byte) 0xFF;

    private final static int timeout = 10000;
    private final Selector selector = Selector.open();
}