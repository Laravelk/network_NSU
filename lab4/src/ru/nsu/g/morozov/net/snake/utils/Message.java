package ru.nsu.g.morozov.net.snake.utils;

import ru.nsu.g.morozov.net.snake.protocol.SnakesProto.*;

public class Message {
    public GameMessage gameMessage;
    public GamePlayer from;
    public GamePlayer to;
    public Message(GameMessage m, GamePlayer from, GamePlayer to){
        gameMessage = m;
        this.from = from;
        this.to = to;
    }

    public GamePlayer getFrom() {
        return from;
    }

    public GamePlayer getTo() {
        return to;
    }

    public GameMessage getGameMessage() {
        return gameMessage;
    }


    @Override
    public boolean equals(Object obj){
        Message m = (Message)obj;
        return from.getPort() == m.from.getPort();
    }
}
