package ru.nsu.g.morozov.net.snake.gui;

import ru.nsu.g.morozov.net.snake.Node;
import ru.nsu.g.morozov.net.snake.controller.ModelController;
import ru.nsu.g.morozov.net.snake.model.Model;
import ru.nsu.g.morozov.net.snake.utils.Message;
import ru.nsu.g.morozov.net.snake.utils.Subscriber;


import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class GUIController extends Subscriber {
    private Node node;
    private GameDisplay mainField;
    private ModelController controller;
    private int w = 1600;
    private int h = w/2;
    public GUIController(Model model, Node node){
        super(model);
        this.node = node;
        controller = new ModelController(model, node);
        mainField = new GameDisplay(w, h, model.getWidth(), model.getHeight(), controller, model, node.uuid.hashCode());

    }
    @Override
    public void Notify(int x){
        mainField.update();
    }
    public void updateAnn(ConcurrentHashMap<Message, Instant> ann){
        mainField.updateAnn(ann);
    }
}
