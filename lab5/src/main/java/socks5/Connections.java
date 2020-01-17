package socks5;

import socks5.message.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Connections implements SocksHandler {
    public Connections(SocketChannel client, DNS dns) {
        this.dns = dns;
        this.client = client;
        this.byte_read.clear();
    }

    public void register(Selector selector) throws IOException {
        this.client.configureBlocking(false);
        this.client.register(selector, SelectionKey.OP_READ, this);
    }

    private HelloSocks readHelloMessage() throws IOException {
        int read_bytes = client.read(byte_read);
        if (-1 == read_bytes) {
            this.close();
            return null;
        }
        if (HelloSocks.isCorrectSizeOfMessage(byte_read)) {
            byte_read.flip();
            return new HelloSocks(byte_read);
        }
        return null;
    }

    private RequestSocks readRequestMessage() throws IOException {
        int read_bytes = client.read(byte_read);
        if (-1 == read_bytes) {
            this.close();
            return null;
        }
        if (RequestSocks.isCorrectSizeOfMessage(byte_read)) {
            byte_read.flip();
            return new RequestSocks(byte_read);
        }
        return null;
    }

    public byte[] getResponse() {
        byte[] data = new byte[2];
        data[0] = Proxy.SOCKS_5;
        if (!hello.hasMethod()) {
            data[1] = Proxy.NO_ACCEPTABLE_METHODS;
        }
        else {
            data[1] = Proxy.NO_AUTHENTICATION;
        }
        return data;
    }

    private void clientPart(SelectionKey key) throws IOException {
        if (key.isReadable()) {
            switch (state) {
                case HELLO: {
                    System.out.println("Get hello message");
                    hello = this.readHelloMessage();
                    if (null == hello) return;
                    key.interestOps(SelectionKey.OP_WRITE);
                    byte_read.clear();
                    break;
                }
                case REQUEST: {
                    System.out.println("Get request message");
                    request = this.readRequestMessage();
                    if (null == request) return;
                    if (!this.connect()) {
                        server = null;
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    else {
                        server.register(key.selector(), SelectionKey.OP_CONNECT, this);
                        key.interestOps(0);
                    }
                    byte_read.clear();
                    break;
                }
                case MESSAGE: {
                    if (this.readFrom(client, byte_read)) {
                        server.register(key.selector(), SelectionKey.OP_WRITE, this);
                        key.interestOps(0);
                    }
                    break;
                }
            }
        }
        else if (key.isWritable()) {
            switch (state) {
                case HELLO: {
                    if (null == byte_write) {
                        byte_write = ByteBuffer.wrap(getResponse());
                    }
                    if (this.writeTo(client, byte_write)) {
                        byte_write = null;
                        if (hello.hasMethod()) {
                            key.interestOps(SelectionKey.OP_READ);
                            state = State.REQUEST;
                        }
                        else {
                            System.err.println("Not support");
                            this.close();
                        }
                        hello = null;
                    }
                    break;
                }
                case REQUEST: {
                    if (null == byte_write) {
                        byte_write = ByteBuffer.wrap(ResponseOnRequestSocks.create(request, server != null));
                    }
                    if (this.writeTo(client, byte_write)) {
                        byte_write = null;
                        if (!request.isCommand(RequestSocks.CONNECT_TCP) || server == null) {
                            this.close();
                            System.out.println("Not support");
                        }
                        else {
                            key.interestOps(SelectionKey.OP_READ);
                            server.register(key.selector(), SelectionKey.OP_READ, this);
                            state = State.MESSAGE;
                        }
                        request = null;
                    }
                    break;
                }
                case MESSAGE: {
                    if (this.writeTo(client, byte_read)) {
                        key.interestOps(SelectionKey.OP_READ);
                        server.register(key.selector(), SelectionKey.OP_READ, this);
                    }
                    break;
                }
            }
        }
    }

    private void serverPart(SelectionKey key) throws IOException {
        if (key.isWritable()) {
            if (this.writeTo(server, byte_read)) {
                key.interestOps(SelectionKey.OP_READ);
                client.register(key.selector(), SelectionKey.OP_READ, this);
            }
        }
        else if (key.isReadable()) {
            if (this.readFrom(server, byte_read)) {
                client.register(key.selector(), SelectionKey.OP_WRITE, this);
                key.interestOps(0);
            }
        }
        else if (key.isConnectable()) {
            if (!server.isConnectionPending()) return;
            if (!server.finishConnect()) return;
            key.interestOps(0);
            client.register(key.selector(), SelectionKey.OP_WRITE, this);
        }
    }

    @Override
    public void accept(SelectionKey key) {
        try {
            if (!key.isValid()) {
                this.close();
                key.cancel();
                return;
            }

            if (client == key.channel()) {
                clientPart(key);
            }
            else if (server == key.channel()) {
                serverPart(key);
            }
        }
        catch (IOException ex) {
            try {
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean connectToServer(InetAddress address) {
        try {
            server.connect(new InetSocketAddress(address, request.getDestPort()));
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean connect() throws IOException {
        server = SocketChannel.open();
        server.configureBlocking(false);
        switch (request.getAddressType()) {
            case RequestSocks.IPv4: {
                return this.connectToServer(InetAddress.getByAddress(request.getDestAddress()));
            }
            case RequestSocks.DOMAIN_NAME: {
                dns.sendToResolve(new String(request.getDestAddress()), this);
                break;
            }
            case RequestSocks.IPv6: {
                System.err.println("It's a IPv6");
                return false;
            }
        }
        return true;
    }

    private boolean readFrom(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.compact();
        int read_bytes = channel.read(buffer);
        if (-1 == read_bytes) {
            this.close();
            return false;
        }
        if (0 != read_bytes) {
            buffer.flip();
        }
        return 0 != read_bytes;
    }

    private boolean writeTo(SocketChannel channel, ByteBuffer buffer) throws IOException {
        channel.write(buffer);
        return !buffer.hasRemaining();
    }

    @Override
    public void close() throws IOException {
        if (client != null) this.client.close();
        if (server != null) this.server.close();
    }

    private SocketChannel server = null;
    private final DNS dns;
    private static final int BUFFER_SIZE = 4096;
    private State state = State.HELLO;
    private final SocketChannel client;
    private ByteBuffer byte_read = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private ByteBuffer byte_write = null;
    private HelloSocks hello = null;
    private RequestSocks request = null;
}