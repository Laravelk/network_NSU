package src;

import Message.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Tree extends Thread {
    private final int messageQueueSize = 20;
    private final int packetSize = 2048;
    private final long timeout = 100;
    private final DatagramSocket socket;
    private final Node node;
    private  Node parent = null;
    private final Sender sender;
    private final Receiver receiver;
    private DatagramPacket packet;
    private List<Node> children = new LinkedList<>();
    private HashMap<Node, Node> deputies = new HashMap<>();

    Tree(String name, int lossFactor, int port, String parentAddress, int parentPort) throws UnknownHostException, SocketException {
        InetAddress address = InetAddress.getLocalHost();
        this.node = new Node(name, address.getHostAddress(), port);
        if (null != parentAddress) {
            parent = new Node(parentAddress, parentPort);
        }
        this.socket = new DatagramSocket(port);
        System.out.println("Client start: " + name + " "+ node.getAddress() + " " + node.getPort() + " loss factor: "+ lossFactor);
        this.sender = new Sender(socket, messageQueueSize, timeout);
        this.receiver = new Receiver(socket, packetSize, messageQueueSize, lossFactor);
    }

    @Override
    public synchronized void start() {
        receiver.start();
        sender.start();
        super.start();
    }

    private void checkAndRepairTree() throws IOException, ClassNotFoundException {
        if(!sender.deathNodeListIsEmtpy()) {
            HashMap<Node, Node> entryRemove = new HashMap<>();
            Node death = sender.getDeathNode();
            while (death != null) {
                if (death == parent) {
                    parent = null;
                    continue;
                }
                for (HashMap.Entry<Node, Node> entry : deputies.entrySet()) {
                    if (death.equals(entry.getKey())) {
                        if (!children.contains(entry.getValue())) {
                            children.add(entry.getValue());
                        }
                        Parent parentMessage = new Parent(node);
                        sendOnce(parentMessage, entry.getValue());
                        entryRemove.put(entry.getKey(), entry.getValue());
                        sender.retraceOldMessages(death.getPort(), entry.getValue().getPort(), node.getPort());
                    }
                }
                clear(death);
                children.remove(death);
                death = sender.getDeathNode();
            }
            for (HashMap.Entry<Node,Node> entry : entryRemove.entrySet()) {
                for (Node child : children) {
                    if (child != entry.getValue()) {
                        sendOnce(new Deputy(node, entry.getValue()), child);
                        sendOnce(new Deputy(node, child), entry.getValue());
                    }
                }
            }

            for (HashMap.Entry<Node,Node> entry : entryRemove.entrySet()) {
                deputies.remove(entry.getKey(), entry.getValue());
            }
            sender.clearDeathNodeList();
        }
    }

    private void sendAnotherNode(Message message, Node from) throws IOException {
        message.setNode(node);
        if (parent != null && !parent.equals(from)) {
            message.generateNewUuid();
//            System.out.println("to parent " + parent.getPort() + " " + from.getPort());
            send(message, parent);
        }
        for (Node child : children) {
            if (!child.equals(from)) {
//                System.out.println("to child " + child.getPort());
                message.generateNewUuid();
                send(message, child);
            }
        }
    }

    private void send(Message message, Node to) throws IOException {
        message.setNode(node);
        send(message, to.getAddress(), to.getPort());
    }

    private void send(Message message, InetAddress inetAddress, int port) throws IOException {
        if (port != node.getPort() && message.getCreatorNode().getPort() != port) {
//            clear(message.getNode());
//            System.out.println("SEND TO " + message.getUUID() + " to " + port);
            byte[] data = sender.serializeMessage(message);
            DatagramPacket packet = new DatagramPacket(data, data.length, inetAddress, port);
            sender.send(packet, message.getUUID());
        }
    }

    private void sendOnce(Message message, Node to) throws IOException {
        sendOnce(message, to.getAddress(), to.getPort());
    }

    private void sendMessageToAll(Message message) throws IOException {
        if (parent != null) {
            send(message, parent);
        }
        for (Node child : children) {
            message.generateNewUuid();
            send(message, child);
        }
    }

    void send(String text) {
        if (text.equals("dep")) {
            showDeputies();
            return;
        }

        if (text.equals("chi")) {
            showChild();
            return;
        }
        Text message = new Text(text, node);
        try {
            sendMessageToAll(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clear(Node node) {
        ArrayList<Node> removeInDeputies = new ArrayList<>();
        for (HashMap.Entry<Node, Node> entry : deputies.entrySet()) {
//            System.out.println(entry.getKey().getPort() + " " + entry.getValue().getPort() + " " + node.getPort());
            if (entry.getValue().getPort() == node.getPort() /*&& (parent != node) && (!children.contains(node))*/) {
                removeInDeputies.add(entry.getKey());
            }
        }
//        System.out.println("DELETE " + removeInDeputies.size());
        for (Node node1 : removeInDeputies) {
            deputies.remove(node1);
        }
    }

    private void sendOnce(Message message, InetAddress address, int port) throws IOException {
        byte [] data = sender.serializeMessage(message);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        sender.sendOnce(packet);
    }

    private void print(Message message) {
        if ((message instanceof Text))
            System.out.println(message.toString());
        if (parent != null) {
//            System.out.println("PA " + parent.getPort());
        }
    }

    @Override
    public void run() {
        try {
            if (parent != null) {
                send(new Connection(node), parent);
            }

            while (!Thread.interrupted()) {
                checkAndRepairTree();
                packet = receiver.receive();
                if (packet == null) {
                    sleep(10);
                } else {
                    Message message = sender.recoveryMessage(packet.getData());
                    print(message);
                    if (message instanceof Connection) {
                        if (parent != null) {
                            sendOnce(new Deputy(node, message.getCreatorNode()), parent);
                        }
                        children.add((message).getNode());
                        sendOnce(new Answer(message.getUUID(), node), packet.getAddress(), packet.getPort());
                        for (Node child : children) {
                            if (child != ((Connection) message).creatorNode) {
                                send(new Deputy(node, child), ((Connection) message).creatorNode);
                                send(new Deputy(node, ((Connection) message).creatorNode), child);
                            }
                        }
                        if (parent != null) {
                            send(new Deputy(node, parent), ((Connection) message).creatorNode);
                            send(new Deputy(node, ((Connection) message).creatorNode), parent);
                        }
                    } else if (message instanceof Answer) {
                        sender.removeByUuid(message.getUUID());
                    } else if (message instanceof Deputy) {
                        boolean exist = false;
                        for (HashMap.Entry<Node, Node> entry : deputies.entrySet()) {
                            if (entry.getKey().getPort() == message.getCreatorNode().getPort() &&
                                    entry.getValue().getPort() == message.getNode().getPort()) {
                                exist = true;
                            }
                        }
                        if (!exist) {
                            deputies.put(message.getCreatorNode(), message.getNode());
                        }
                        sendOnce(new Answer(message.getUUID(), node), packet.getAddress(), packet.getPort());
                    } else if (message instanceof Parent) {
                        clear(message.getNode());
//                        System.out.println(message.getNode().getPort() + " FDSFDSFDS");
                        if (parent != null) {
                            parent = message.getNode();
                        } else {
                            parent = new Node(message.getNode().getName(), "localhost",
                                    message.getNode().getPort());
                        }
//                        System.out.println(parent.getPort() + " FDSFDSFDS");
                        sendOnce(new Answer(message.getUUID(), node), packet.getAddress(), packet.getPort());
                        for (Node child : children) {
                            sendOnce(new Deputy(node, child), parent);
                            sendOnce(new Deputy(node, parent), child);
                        }
                    } else if (message instanceof Text) {
                        sendOnce(new Answer(message.getUUID(), node), packet.getAddress(), packet.getPort());
                        sendAnotherNode(message, message.getNode());
                    }
                }
            }

        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // test zone

    private void showDeputies() {
        for (HashMap.Entry<Node, Node> entry : deputies.entrySet()) {
            System.out.println(entry.getKey() + "(" + entry.getKey().getPort() + ") and his dep is "
                    + entry.getValue() + "(" + entry.getValue().getPort() + ")\n");
        }
    }

    private void showChild() {
        for (Node child : children) {
            System.out.println(child  + "(" + child.getPort() + ")\n");
        }
    }
}
