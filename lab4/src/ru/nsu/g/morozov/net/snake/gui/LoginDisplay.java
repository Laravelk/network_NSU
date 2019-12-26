package ru.nsu.g.morozov.net.snake.gui;

import ru.nsu.g.morozov.net.snake.Node;
import ru.nsu.g.morozov.net.snake.model.Model;
import ru.nsu.g.morozov.net.snake.protocol.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.regex.Pattern;

public class LoginDisplay extends JFrame {
    private JTextField get_name, get_ip, get_port;

    public LoginDisplay(){
        init();
    }

    private void init(){
        GridBagLayout grid = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(grid);
        setTitle("Login");

        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0.1;
        gbc.weighty = 0.1;
        gbc.gridx = 0;

        gbc.gridy = 0;
        JLabel name_prompt = new JLabel("Enter your name:");
        add(name_prompt, gbc);

        gbc.gridy = 1;
        get_name = new JTextField("KAVO?");
        get_name.setColumns(20);
        get_name.setHorizontalAlignment(JTextField.CENTER);
        add(get_name, gbc);

        gbc.gridy = 2;
        JLabel ip_prompt = new JLabel("Enter IP:");
        add(ip_prompt, gbc);

        gbc.gridy = 3;
        get_ip = new JTextField("127.0.0.1");
        get_ip.setColumns(8);
        get_ip.setHorizontalAlignment(JTextField.CENTER);
        add(get_ip, gbc);

        gbc.gridy = 4;
        JLabel port_prompt = new JLabel("Enter port:");
        add(port_prompt, gbc);

        gbc.gridy = 5;
        get_port = new JTextField("12345");
        get_port.setColumns(5);
        get_port.setHorizontalAlignment(JTextField.CENTER);
        add(get_port, gbc);

        gbc.gridy = 6;
        JLabel status = new JLabel("Waiting for username");
        add(status, gbc);

        gbc.gridy = 7;
        JButton login = new JButton("Login");
        login.addActionListener(e -> {
            int port;
            if (!Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
                    .matcher(get_ip.getText())
                    .matches()) {
                status.setText("Enter a valid IP address.");
            } else {
                try {
                    port = Integer.parseInt(get_port.getText());
                } catch (Exception e1) {
                    status.setText("Enter a valid port.");
                    return;
                }

                try {
                    if(port < 1 || port > 65535){
                        status.setText("Enter a valid port.");
                        return;
                    }
                    InetAddress inetAddress = InetAddress.getByName(get_ip.getText());
                    UUID uuid = UUID.randomUUID();
                    Model model = new Model(10,10);
                    Node node = new Node(model, SnakesProto.NodeRole.NORMAL, get_name.getText(), inetAddress, port, uuid);
                    setVisible(false);
                    node.start();
                } catch (ExceptionInInitializerError e1) {
                    status.setText("Can`t connect to the server!");
                } catch (UnknownHostException e1) {
                    status.setText("Can`t connect to the server!");
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        add(login, gbc);

        setLocationRelativeTo(null);
        setSize(300, 200);
        setPreferredSize(getSize());
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }
}
