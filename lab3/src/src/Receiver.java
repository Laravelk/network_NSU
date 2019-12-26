package src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;

import java.util.concurrent.ThreadLocalRandom;

public class Receiver extends Thread {
    private final DatagramSocket socket;
    private final int packetSize;
    private LinkedList<DatagramPacket> queue;
    private final int lossFactor;


    Receiver(DatagramSocket socket, int packetSize, int queueCapacity, int lossProbability) {
        this.packetSize = packetSize;
        this.socket = socket;
        this.lossFactor = lossProbability;
        queue = new LinkedList<DatagramPacket>();
    }

    public DatagramPacket receive() throws InterruptedException {
        if (queue.isEmpty()) {
            return null;
        }
        DatagramPacket packet = queue.getFirst();
        queue.removeFirst();
        return  packet;
    }

    private int rand() {
        return ThreadLocalRandom.current().nextInt(0, 99 + 1);
    }

    @Override
    public void run() {
        System.out.println("Receiver run");
        try {
            while (!Thread.interrupted()) {
                DatagramPacket packet = new DatagramPacket(new byte[packetSize], packetSize);
                socket.receive(packet);
                if (lossFactor <= rand()) {
                    queue.push(packet);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
