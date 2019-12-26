package Message;

import java.util.UUID;
import src.Node;

public interface Message {
    UUID getUUID();
    String getString();
    void setString(String text);
    Node getNode();
    Node getCreatorNode();
    void setNode(Node node);
    void generateNewUuid();
}
