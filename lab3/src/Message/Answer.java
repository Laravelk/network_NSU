package Message;

import src.Node;
import java.io.Serializable;
import java.util.UUID;

public class Answer implements Message, Serializable {
    public String text = "answer";
    public UUID uuid;
    public Node node;

    public Answer(UUID uuid, Node node) {
        this.uuid = uuid;
        this.node = node;
    }

    @Override
    public void generateNewUuid() {
        uuid = UUID.randomUUID();
    }

    @Override
    public void setString(String text) {
        this.text = text;
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public Node getCreatorNode() {
        return node;
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
        return null;
    }

    @Override
    public String toString() {
        return "Answer to node, which send message with this [" + uuid + "] uuid";
    }
}
