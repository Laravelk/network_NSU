package Message;

import src.Node;
import java.io.Serializable;
import java.util.UUID;


public class Text implements Message, Serializable {
    public String text;
    public Node node;
    public Node creatorNode;
    public UUID uuid;

    public Text(String string, Node node) {
        this.text = string;
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
    public String getString() {
        return  text;
    }

    @Override
    public Node getCreatorNode() {
        return creatorNode;
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    public UUID getUUID() {
        return uuid;
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
