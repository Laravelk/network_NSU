package ru.nsu.g.morozov.net.snake.utils;

abstract public class Subscriber {
    protected Publisher publisher;
    public abstract void Notify(int x);
    public Subscriber(Publisher publisher){
        this.publisher = publisher;
        publisher.addSub(this);
    }
}
