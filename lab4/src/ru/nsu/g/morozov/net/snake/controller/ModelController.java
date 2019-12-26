package ru.nsu.g.morozov.net.snake.controller;

import ru.nsu.g.morozov.net.snake.Node;
import ru.nsu.g.morozov.net.snake.model.Model;

import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;

public class ModelController {
    private Model model;
    private Node node;
    public ModelController(Model model, Node node){
        this.model = model;
        this.node = node;
    }
    public void newGame(){
        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig.newBuilder()
                .setWidth(40)
                .setHeight(30)
                .setFoodStatic(50)
                .setFoodPerPlayer((float)0.2)
                .setStateDelayMs(500)
                .setDeadFoodProb((float)0.8)
                .setPingDelayMs(100)
                .setNodeTimeoutMs(9000)
                .build();
        model.newGameAsMaster(gameConfig, node.getName(), node.uuid.hashCode(), node.getMyPort());
        node.changeRole(SnakesProto.NodeRole.MASTER);
        System.out.println("NEW GAME!!!");
    }
    public void newGame(int w, int h, int fs, float fp, int delay){
        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig.newBuilder()
                .setWidth(w)
                .setHeight(h)
                .setFoodStatic(fs)
                .setFoodPerPlayer(fp)
                .setStateDelayMs(delay)
                .setDeadFoodProb((float)0.8)
                .setPingDelayMs(100)
                .setNodeTimeoutMs(9000)
                .build();
        model.newGameAsMaster(gameConfig, node.getName(), node.uuid.hashCode(), node.getMyPort());
        node.changeRole(SnakesProto.NodeRole.MASTER);
        System.out.println("NEW GAME!!!");
    }
    public void changeDirection(SnakesProto.Direction direction){
        node.changeDirection(direction);
    }

    public void nModelSub(int x){
        model.NotifyAll(x);
    }

    public void join(SnakesProto.GamePlayer player, SnakesProto.GameConfig config){
       // if (player.getId() == node.uuid.hashCode()) node.
        node.sendJoinGame(player, config);
    }
    public void exit(){
        node.sendChangeRoleMsg(null, SnakesProto.NodeRole.VIEWER, SnakesProto.NodeRole.VIEWER);
    }
    public boolean checkAlive(){
        return model.isAlive(node.uuid.hashCode());
    }
}
