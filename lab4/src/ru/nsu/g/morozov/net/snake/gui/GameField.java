package ru.nsu.g.morozov.net.snake.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;

import ru.nsu.g.morozov.net.snake.controller.ModelController;
import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;
import ru.nsu.g.morozov.net.snake.model.Model;

public class GameField extends JPanel {
    private int WIDTH = 30;
    private int HEIGHT = 40;
    private int SCALE = 20;
    private Graphics graphics;
    private Model model;
    private int countPlyer;
    private ModelController controller;
    private HashMap<Integer, Color> snakesColor = new HashMap<>();
    private int id;
    private KeyEventDispatcher ked = (new KeyEventDispatcher(){
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if(e.getID()==KeyEvent.KEY_PRESSED){
                if((e.getKeyCode()==KeyEvent.VK_UP)||(e.getKeyCode()==KeyEvent.VK_W)){
                    controller.changeDirection(SnakesProto.Direction.UP);
                }
                if((e.getKeyCode()==KeyEvent.VK_DOWN)||(e.getKeyCode()==KeyEvent.VK_S)){
                    controller.changeDirection(SnakesProto.Direction.DOWN);
                }
                if((e.getKeyCode()==KeyEvent.VK_LEFT)||(e.getKeyCode()==KeyEvent.VK_A)){
                    controller.changeDirection(SnakesProto.Direction.LEFT);
                }
                if((e.getKeyCode()==KeyEvent.VK_RIGHT)||(e.getKeyCode()==KeyEvent.VK_D)){
                    controller.changeDirection((SnakesProto.Direction.RIGHT));
                }
            }
            return false;
        }
    });

    GameField(int weight, int height, Model model, ModelController controller, int id){
        this.id = id;
        this.model= model;
        this.controller = controller;
        WIDTH = model.getGameState().getConfig().getWidth();
        HEIGHT =  model.getGameState().getConfig().getHeight();
        SCALE = (int)Math.floor((float)weight/ WIDTH);

        setPreferredSize(setFieldSize());
        setBorder(BorderFactory.createLineBorder(Color.red));
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ked);
    }
    private Dimension setFieldSize(){
        return new Dimension(WIDTH *SCALE, HEIGHT*SCALE);
    }
    public void paint(Graphics g){
        int width = WIDTH;
        WIDTH =  model.getGameState().getConfig().getWidth();
        HEIGHT =  model.getGameState().getConfig().getHeight();
        SCALE = (int)Math.floor((float)width*SCALE/ WIDTH);
        paintField(g);

    }
    private void paintField(Graphics g){
        g.setColor(Color.BLACK);
        g.fillRect(0,0, WIDTH *SCALE,HEIGHT*SCALE);
        paintSnakes(g);
        paintApples(g);
        g.setColor(Color.BLACK);
        for (int x = 0; x < WIDTH *SCALE; x += SCALE){
            g.drawLine(x, 0,x,HEIGHT*SCALE);
        }
        for (int y = 0; y < HEIGHT*SCALE; y += SCALE){
            g.drawLine(0, y, WIDTH *SCALE,y);
        }
    }
    private void paintSnakes(Graphics g){
        for (SnakesProto.GameState.Snake snake : model.getGameState().getSnakesList()){
            snakesColor.put(snake.getPlayerId(), Color.RED);
            paintSnake(snake, g);
        }

    }
    private void paintSnake(SnakesProto.GameState.Snake snake, Graphics g){
        List<SnakesProto.GameState.Coord> list = model.snakeToList(snake);
        if (snake.getPlayerId() == id) g.setColor(Color.YELLOW);
        else g.setColor(Color.RED);
        for (SnakesProto.GameState.Coord coord : list){
            g.fillRect(coord.getX()*SCALE+1,coord.getY()*SCALE+1, SCALE,SCALE);
        }
    }
    private  void paintApples(Graphics g){
        for (SnakesProto.GameState.Coord coord :  model.getGameState().getFoodsList()){
            g.setColor(Color.GREEN);
            g.fillRect(coord.getX()*SCALE+1,coord.getY()*SCALE+1, SCALE-1,SCALE -1);
        }
    }
}
