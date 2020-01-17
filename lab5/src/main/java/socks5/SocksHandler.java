package socks5;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface SocksHandler extends AutoCloseable {
    @Override
    void close() throws IOException;
    void accept(SelectionKey key);
}