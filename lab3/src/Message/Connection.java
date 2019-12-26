package Message;

import src.Node;
import java.io.Serializable;
import java.util.UUID;

public class Connection implements  Message, Serializable {
    public String text = "Connection";
    public Node creatorNode;
    public Node node;
    public UUID uuid;

    public Connection(Node node) {
        this.creatorNode = node;
        this.node = node;
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
    public Node getCreatorNode() {
        return creatorNode;
    }

    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public String getString() {
        return text;
    }

    @Override
    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
       return uuid + " " + creatorNode.getName() + " :" + text;
    }
}
