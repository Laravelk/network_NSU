package ru.nsu.g.morozov.net.snake.gui;

import ru.nsu.g.morozov.net.snake.controller.ModelController;
import ru.nsu.g.morozov.net.snake.model.Model;
import ru.nsu.g.morozov.net.snake.utils.Message;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class GameDisplay extends JFrame {
    private GameField gameField;
    private SideField sideField;
    private int c_w, c_h;
    GameDisplay(int w, int h, int c_w, int c_h, ModelController controller, Model model, int id){
        setSize(new Dimension(w+10, h+30));
        this.c_h = c_h;
        this.c_w = c_w;
        setLayout(new BorderLayout());
        setTitle("Игра змейка");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        gameField = new GameField(w/2, h, model, controller, id);
        sideField = new SideField(controller, model);
        add(gameField, BorderLayout.WEST);
        add(sideField, BorderLayout.EAST);

        setVisible(true);
    }

    public int getC_h() {
        return c_h;
    }

    public int getC_w() {
        return c_w;
    }

    public void update(){
        gameField.setFocusable(true);
        gameField.requestFocusInWindow();
        sideField.printScore();
        gameField.repaint();
    }

    public void updateAnn(ConcurrentHashMap<Message, Instant> ann) {
        sideField.printAnn(ann);
    }
}
