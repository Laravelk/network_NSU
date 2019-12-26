package src;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Node implements Serializable {
    private String name;
    private InetAddress address;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public void setPort(int port) {
        this.port = port;
    }

    private int port;
    private final String defaultName = "default_name";

    Node(String name, String address, int port) throws UnknownHostException {
        this.name = name;
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    Node(String address, int port) throws UnknownHostException {
        this.name = defaultName;
        this.address = InetAddress.getByName(address);
        this.port = port;
    }

    int getPort() {
        return port;
    }

    InetAddress getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        Node o = (Node) obj;
        if (this == obj) return true;
        return this.port == o.getPort();
//        return this.address.getHostAddress().equals(o.getAddress().getHostAddress());
    }
}
