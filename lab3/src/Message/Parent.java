package Message;

import src.Node;

import java.io.Serializable;
import java.util.UUID;

public class Parent implements Message, Serializable {
    public String text = "parent";
    public UUID uuid;
    public Node node;
    public Node parentNode;

    public Parent(Node parent) {
        this.parentNode = parent;
        uuid = UUID.randomUUID();
    }

    @Override
    public void setString(String text) {
        this.text = text;
    }

    @Override
    public void generateNewUuid() {
        uuid = UUID.randomUUID();
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getString() {
        return text;
    }

    @Override
    public Node getNode() {
        return parentNode;
    }

    @Override
    public Node getCreatorNode() {
        return parentNode;
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public String toString(){
        return  parentNode.getName() + " i'm your new parent " + " uid: " + uuid;
    }
}
