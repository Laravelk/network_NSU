package ru.nsu.g.morozov.net.snake.utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import ru.nsu.g.morozov.net.snake.Node;
import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;

public class UnicastReceiver extends Thread{
    private DatagramSocket socket;
    private Node node;
    public UnicastReceiver(DatagramSocket socket, Node node){
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
                byte[] temp = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, temp, 0, packet.getLength());
                gameMessage = SnakesProto.GameMessage.parseFrom(temp);
                synchronized (node.messages) {
                    node.receiveUnicast(gameMessage, packet.getAddress(), packet.getPort());
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
