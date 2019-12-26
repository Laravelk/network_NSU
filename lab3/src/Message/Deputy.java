package Message;

import src.Node;
import java.io.Serializable;
import java.util.UUID;

public class Deputy implements Message, Serializable {
    public String text = "deputy";
    public UUID uuid;
    public Node node;
    public Node deputyNode;

    public Deputy(Node node, Node deputyNode) {
        this.deputyNode = deputyNode;
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
    public UUID getUUID() {
        return uuid;
    }//TODO:return uuid;

    @Override
    public String getString() {
        return text;
    }//TODO:return text;

    @Override
    public Node getNode() {
        return deputyNode;
    }

    @Override
    public Node getCreatorNode() {
        return node;
    }

    @Override
    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public String toString(){
        return  node.getName() + " said: my deputy is " + deputyNode.getName();
    }
}
