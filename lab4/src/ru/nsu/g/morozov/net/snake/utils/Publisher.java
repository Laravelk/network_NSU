package ru.nsu.g.morozov.net.snake.utils;

import java.util.Vector;

public abstract class Publisher {

    Vector<Subscriber> subs = new Vector<>();
    public void NotifyAll(int x){
        for (Subscriber sub : subs){
            sub.Notify(x);
        }
    }
    public void addSub(Subscriber sub){
        subs.add(sub);
    }
}
