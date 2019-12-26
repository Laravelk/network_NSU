package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Console extends Thread {
    private Tree controller;

    public Console(Tree controller) {
        this.controller = controller;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (!Thread.interrupted()) {
                if (br.ready()) {
                    String line = br.readLine();
                    controller.send(line);
                }
                Thread.sleep(100);
            }
        } catch (IOException e) {
            e.printStackTrace();
            interrupt();
        } catch (InterruptedException ignored) {

        }
    }
}
