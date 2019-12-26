package ru.nsu.g.morozov.net.snake.utils;

import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;
import ru.nsu.g.morozov.net.snake.Node;

public class MulticastReceiver extends Thread {
    private MulticastSocket socket;
    private Node node;
    private byte[] temp;
    public MulticastReceiver(MulticastSocket socket, Node node){
        this.socket = socket;
        this.node = node;
    }

    @Override
    public void run() {
        byte[] data = new byte[10000];
        SnakesProto.GameMessage gameMessage;
        DatagramPacket packet = new DatagramPacket(data, data.length);
        while (true) {
            try {
                socket.receive(packet);

                temp = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, temp, 0, packet.getLength());
                gameMessage = SnakesProto.GameMessage.parseFrom(temp);
                node.receiveMulticast(gameMessage, packet.getAddress(), packet.getPort());
            } catch (SocketTimeoutException e) {
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
