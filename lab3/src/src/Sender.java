package src;

import Message.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class Sender extends Thread {
    private LinkedList<MessageInformation> packets;
    private final int maxQueueSize;
    private final DatagramSocket socket;
    private LinkedList<Node> deathNodesList;
    private HashMap<DatagramPacket, Integer> countFailedPacketsSend;

    private final Object lockTheList = new Object();
    private final Object lockTheDeathNodeList = new Object();
    private final Object lockTheDeathCountMap = new Object();

    Sender(DatagramSocket socket, int maxQueueSize, long timeout) {
        this.socket = socket;
        this.maxQueueSize = maxQueueSize;
        packets = new LinkedList<>();
        deathNodesList = new LinkedList<>();
        countFailedPacketsSend = new HashMap<>();
    }

    Message recoveryMessage(byte [] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return (Message)is.readObject();
    }

    byte[] serializeMessage(Message message) throws IOException {
        ByteArrayOutputStream os =
                new ByteArrayOutputStream();
        ObjectOutputStream oos =
                new ObjectOutputStream(os);
        oos.writeObject(message);
        return os.toByteArray();
    }

    void retraceOldMessages(int death_port, int new_port, int this_port) throws IOException, ClassNotFoundException {
        ArrayList<MessageInformation> needToPost = new ArrayList<>();
        synchronized (lockTheList) {
            for (MessageInformation info : packets) {
                if (info.packet.getPort() == death_port) {
                    needToPost.add(info);
                }
            }
            lockTheList.notifyAll();
        }
        for (MessageInformation message : needToPost) {
            Message newMessage= recoveryMessage(message.packet.getData());
            newMessage.generateNewUuid();
            newMessage.getNode().setPort(this_port);
            byte[] data = serializeMessage(newMessage);
            DatagramPacket newPacket = new DatagramPacket(data, data.length,message.packet.getAddress(), new_port);
            this.send(newPacket, newMessage.getUUID());
        }
    }

    boolean deathNodeListIsEmtpy() {
        return deathNodesList.isEmpty();
    }

    Node getDeathNode() {
        Node deathNode = null;
        synchronized (lockTheDeathNodeList) {
            if (!deathNodesList.isEmpty()) {
                deathNode = deathNodesList.getFirst();
                deathNodesList.removeFirst();
            }
        }
        return deathNode;
    }

    void clearDeathNodeList() {
        synchronized (lockTheDeathNodeList) {
            deathNodesList.clear();
            lockTheDeathNodeList.notifyAll();
        }
    }

    synchronized private void sendPacket(DatagramPacket packet) throws IOException {
        socket.send(packet);
    }

    void sendOnce(DatagramPacket packet) throws IOException {
        sendPacket(packet);
    }

    void send(DatagramPacket packet, UUID uuid) {
        synchronized (lockTheList) {
            if (maxQueueSize == packets.size()) {
                packets.removeFirst();
            }
            packets.add(new MessageInformation(packet, uuid, System.currentTimeMillis()));
            countFailedPacketsSend.put(packet, 0);
            lockTheList.notifyAll();
        }
    }

    void removeByUuid(final UUID id) {
        synchronized (lockTheList) {
            packets.remove(new MessageInformation(null, id, 0));
            lockTheList.notifyAll();
        }
    }

    void replaceSender(Node replacedNode, Node replace) {
        for (MessageInformation info : packets) {
            if (info.packet.getPort() == replacedNode.getPort()) {
                synchronized (lockTheDeathCountMap) {
                    countFailedPacketsSend.remove(info.packet);
                    info.packet.setPort(replace.getPort());
                    countFailedPacketsSend.put(info.packet, 0);
                }
            }

        }
    }

    @Override
    public void run() {
        System.out.println("Sender run");
        try {
            while (!Thread.interrupted()) {
                long timeSend;
                MessageInformation message;
                synchronized (lockTheList) {
                    while (packets.isEmpty()) {
                        lockTheList.wait();
                    }
                    message = packets.getFirst();
                    packets.add(packets.removeFirst());
                    lockTheList.notifyAll();
                }
                sendPacket(message.packet);
//                System.out.println(message.packet.getPort());
                sleep(200);
                if (packets.contains(message)) {
                    Integer count = countFailedPacketsSend.get(message.packet);
                    synchronized (lockTheDeathCountMap) {
                        countFailedPacketsSend.remove(message.packet);
                        countFailedPacketsSend.put(message.packet, count + 1);
                    }
                    if (5 == countFailedPacketsSend.get(message.packet)) {
                        synchronized (lockTheDeathNodeList) {
                            Node node = new Node(message.packet.getAddress().getHostName(), message.packet.getPort());
                            deathNodesList.push(node); //имя не будем знать
                            lockTheDeathNodeList.notifyAll();
                        }
                    }
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private static class MessageInformation {
        private DatagramPacket packet;
        private UUID uuid;
        private long timeSend;

        private MessageInformation(DatagramPacket packet, UUID id, long timeSend) {
            this.packet = packet;
            this.uuid = id;
            this.timeSend = timeSend;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj == null) { return false; }

            MessageInformation that = (MessageInformation) obj;

            return (uuid.equals(that.uuid));
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }
    }
}
