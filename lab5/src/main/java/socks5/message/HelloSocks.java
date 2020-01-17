package socks5.message;

import socks5.Proxy;

import java.nio.ByteBuffer;

public class HelloSocks {
    public HelloSocks(ByteBuffer buffer) {
        this.data = new byte[buffer.limit()];
        buffer.get(data);
        if (data[1] + 2 != data.length) {
            throw new IllegalArgumentException();
        }
    }

    public boolean hasMethod() {
        for (int i = 0; i < data[1]; ++i) {
            if (Proxy.NO_AUTHENTICATION == data[i + 2]) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCorrectSizeOfMessage(ByteBuffer data) {
        return data.position() > 1 && data.position() >= 2 + data.get(1);
    }
    private final byte[] data;
}
