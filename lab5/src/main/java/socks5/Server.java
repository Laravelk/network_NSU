package socks5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

public class Server implements SocksHandler {
    public Server(int port) throws IOException {
        this.dns = new DNS(0);
        this.server.bind(new InetSocketAddress(port));
    }

    public void register(Selector selector) throws IOException {
        this.server.configureBlocking(false);
        this.server.register(selector, SelectionKey.OP_ACCEPT, this);
        this.dns.register(selector);
    }

    @Override
    public void accept(SelectionKey key) {
        try {
            if (!key.isValid()) {
                this.close();
                return;
            }
            new Connections(server.accept(), dns)
                .register(key.selector());
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        this.server.close();
    }

    private final ServerSocketChannel server = ServerSocketChannel.open();
    private final DNS dns;
}