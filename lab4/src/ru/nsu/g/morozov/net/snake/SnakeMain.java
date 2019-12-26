package ru.nsu.g.morozov.net.snake;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;
import ru.nsu.g.morozov.net.snake.model.Model;

public class SnakeMain {
    public static void main(String[] args) {
       {
            try {
                InetAddress inetAddress = InetAddress.getByName(args[0]);
                int port = Integer.parseInt(args[1]);
                UUID uuid = UUID.randomUUID();
                Model model = new Model(10, 10);
                Node node = new Node(model, SnakesProto.NodeRole.NORMAL, args[2], inetAddress, port, uuid);
                node.start();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
