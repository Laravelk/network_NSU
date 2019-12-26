package ru.nsu.g.morozov.net.snake.gui;

import ru.nsu.g.morozov.net.snake.controller.ModelController;
import ru.nsu.g.morozov.net.snake.model.Model;
import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;
import ru.nsu.g.morozov.net.snake.utils.Message;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SideField extends JPanel {
    private JPanel box = new JPanel();
    private JPanel ann = new JPanel();
    private JPanel score = new JPanel();
    private ModelController controller;
    private Model model;
    private JButton buttonNewGame = new JButton("Новая игра");
    private JButton updateAnn = new JButton("Обновить");
    private JScrollPane scrollPane;
    public SideField(ModelController controller, Model model){
        this.model = model;
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        ann.setLayout(new BoxLayout(ann, BoxLayout.Y_AXIS));
        score.setLayout(new BoxLayout(score, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(750,750));
        setBorder(BorderFactory.createLineBorder(Color.black));

        this.controller = controller;
        buttonNewGame.addActionListener((e)-> new GameSettings(controller));
        updateAnn.addActionListener((e)->controller.nModelSub(3));
        add(box);
        box.add(buttonNewGame);
        box.add(updateAnn);
        box.add(score);
        box.add(ann);

    }
    public void printScore(){
        char d = ' ';
        score.removeAll();
        for (SnakesProto.GamePlayer player: model.getGameState().getPlayers().getPlayersList()){
            score.add(new JLabel("playerId : [" + player.getName() + "]          score : " + player.getScore() + " "+ player.getRole()));

        }
        validate();
    }


    public void printAnn(ConcurrentHashMap<Message, Instant> annons){
        ann.removeAll();
        for (Map.Entry<Message, Instant> entry : annons.entrySet()){
            JPanel panel = new JPanel(new BorderLayout());
            JButton button = new JButton("Войти");
            JButton button1 = new JButton("Выйти");
            button1.addActionListener((e)->controller.exit());
            button.addActionListener((e)->controller.join(entry.getKey().getFrom(),
                    entry.getKey().getGameMessage().getAnnouncement().getConfig()));
            JLabel label = new JLabel(Model.getMaster(entry.getKey().getGameMessage().getAnnouncement().getPlayers()).getName()
                    + "    [" +entry.getKey().getFrom().getIpAddress() +"]    "
                    +entry.getKey().getGameMessage().getAnnouncement().getPlayers().getPlayersCount()
                    +"     "+ entry.getKey().getGameMessage().getAnnouncement().getConfig().getWidth() +"x"+entry.getKey().getGameMessage().getAnnouncement().getConfig().getHeight()
                    +"     " +entry.getKey().getGameMessage().getAnnouncement().getConfig().getFoodStatic() + " + "
                    + entry.getKey().getGameMessage().getAnnouncement().getConfig().getFoodPerPlayer()+ "x");

            panel.add(label, BorderLayout.WEST);
            if (entry.getKey().getFrom().getId() == model.getMasterIp() && controller.checkAlive()) panel.add(button1, BorderLayout.EAST);
            else
                panel.add(button, BorderLayout.EAST);
            ann.add(panel);
            buttonNewGame.setSize(100, 50);
            validate();
        }
    }
}
