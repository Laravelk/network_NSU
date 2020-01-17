package socks5;


import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

public class DNS implements SocksHandler {
    public DNS(int port) throws IOException {
        this.channel = DatagramChannel.open();
        this.channel.bind(new InetSocketAddress(port));

        String dnsServer = ResolverConfig.getCurrentConfig().server();
        this.addressDnsServer = new InetSocketAddress(dnsServer, 53);
        this.channel.connect(addressDnsServer);

        this.b_read.clear();
        this.b_write.clear();
    }

    public void register(Selector selector) throws IOException {
        this.channel.configureBlocking(false);
        this.channel.register(selector, 0, this);
        this.key = channel.keyFor(selector);
    }

    public void sendToResolve(String domainName, Connections handler) {
        try {
            Message dnsRequest = Message.newQuery(Record.newRecord(new Name(domainName + '.'), Type.A, DClass.IN));
            deque.addLast(dnsRequest);
            attachments.put(dnsRequest.getHeader().getID(), handler);
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
        catch (TextParseException ex) {
            ex.printStackTrace();
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
            if (key.isReadable()) {
                if (null != channel.receive(b_read)) {
                    b_read.flip();
                    byte[] data = new byte[b_read.limit()];
                    b_read.get(data);
                    b_read.clear();
                    Message response = new Message(data);
                    Connections session = attachments.remove(response.getHeader().getID());
                    for (Record record : response.getSectionArray(Section.ANSWER)) {
                        if (record instanceof ARecord) {
                            ARecord it = (ARecord) record;
                            if (session.connectToServer(it.getAddress())) {
                                break;
                            }
                        }
                    }
                }
                if (attachments.isEmpty()) {
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
                }
            }
            else if (key.isWritable()) {
                Message dnsRequest = deque.pollFirst();
                while (null != dnsRequest) {
                    b_write.clear();
                    b_write.put(dnsRequest.toWire());
                    b_write.flip();
                    if (0 == channel.send(b_write, addressDnsServer)) {
                        deque.addFirst(dnsRequest);
                        break;
                    }
                    key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                    dnsRequest = deque.pollFirst();
                }
                key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
            try {
                this.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }

    private final DatagramChannel channel;
    private final InetSocketAddress addressDnsServer;
    private final ByteBuffer b_read = ByteBuffer.allocateDirect(Message.MAXLENGTH);
    private final ByteBuffer b_write = ByteBuffer.allocateDirect(Message.MAXLENGTH);
    private final Deque<Message> deque = new LinkedList<>();
    private SelectionKey key;
    private final HashMap<Integer, Connections> attachments = new HashMap<>();
}
