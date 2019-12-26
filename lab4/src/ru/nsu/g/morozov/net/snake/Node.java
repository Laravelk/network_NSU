package ru.nsu.g.morozov.net.snake;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ru.nsu.g.morozov.net.snake.gui.GUIController;
import ru.nsu.g.morozov.net.snake.protocol.SnakesProto.*;
import ru.nsu.g.morozov.net.snake.utils.MulticastReceiver;
import ru.nsu.g.morozov.net.snake.model.Model;
import ru.nsu.g.morozov.net.snake.utils.Message;
import ru.nsu.g.morozov.net.snake.utils.UnicastReceiver;
import ru.nsu.g.morozov.net.snake.utils.Subscriber;

public class Node extends Subscriber {
    private static int stateNumber = 0;

    private DatagramPacket datagramPacket;

    private  NodeRole nodeRole;
    private String name;
    private static final int mcastPort = 9192;
    static  final String mcastIP = "239.192.0.4";
    private InetAddress senderIp;
    private int senderPort;
    private static final int timeout = 3000;

    private GamePlayer master;
    private GamePlayer deputy = null;

    private  InetAddress myAdress;
    private int myPort;

    private GUIController guiController;
    private MulticastSocket multiSocket;
    private DatagramSocket socket;
    private Model curModel;
    private boolean isActive = false;
    private long lastTimeForAnn, lastTimeForState, getLastTimeForSend;
    public UUID uuid;

    private ConcurrentHashMap<Message, Instant> announcements = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Message, Instant> messages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Message, Instant> toResponse = new ConcurrentHashMap<>();

    public String getName() {
        return name;
    }

    public int getMyPort() {
        return myPort;
    }

    public Node(Model model, NodeRole nodeRole, String name, InetAddress address, int portN, UUID uuid)  throws  Exception{
        super(model);

        this.uuid = uuid;
        myAdress = address;
        myPort = portN;

        curModel = model;
        guiController = new GUIController(curModel, this);
        this.nodeRole = nodeRole;
        this.name = name;

        multiSocket = new MulticastSocket(mcastPort);
        multiSocket.joinGroup(InetAddress.getByName(mcastIP));
        multiSocket.setSoTimeout(timeout);

        socket = new DatagramSocket(portN, address);
        socket.setSoTimeout(timeout);
    }

    public void start(){
        UnicastReceiver unicastReceiver = new UnicastReceiver(socket, this);
        MulticastReceiver multicastReceiver = new MulticastReceiver(multiSocket, this);

        lastTimeForAnn = Instant.now().toEpochMilli();
        lastTimeForState = Instant.now().toEpochMilli();
        getLastTimeForSend = Instant.now().toEpochMilli();
        unicastReceiver.start();
        multicastReceiver.start();

        while(true){
            sendMessages();
        }
    }

    public static int incState()
    {
        //System.out.println("stateNumber = "+ stateNumber);
        return stateNumber++;
    }
    private boolean isTimeTo(long time, int period){
        return Instant.now().toEpochMilli() - time > period;
    }

    private void sendMessages(){
        synchronized (messages){
            sendMail();
        }
        sendResponses();
        if (isTimeTo(lastTimeForAnn, 1000)) {
            toResponse.entrySet().removeIf(e -> Instant.now().toEpochMilli() - e.getValue().toEpochMilli()  > 4000);
            announcements.entrySet().removeIf(e -> Instant.now().toEpochMilli() - e.getValue().toEpochMilli()  > 4000);
            if(nodeRole == NodeRole.MASTER){
                sendAnnouncements();
                lastTimeForAnn = Instant.now().toEpochMilli();
            }
        }
    }
    private void sendResponses(){
        if (isTimeTo(getLastTimeForSend, curModel.getGameState().getConfig().getPingDelayMs())){
            checkPlayerActivity();
            getLastTimeForSend = Instant.now().toEpochMilli();
            System.gc();
        }
        else return;
        for (Map.Entry<Message, Instant> entry : messages.entrySet()) {
            try {
                Message m = entry.getKey();
                if (m.to == null) {
                    if (master != null)
                        m.to = master;
                    else {
                        messages.remove(m);
                        continue;
                    }
                }

                m.gameMessage = m.gameMessage.toBuilder().
                        setState(GameMessage.StateMsg.newBuilder().setState(curModel.getGameState())).build();

                datagramPacket =
                        new DatagramPacket(m.gameMessage.toByteArray(),
                                m.gameMessage.toByteArray().length,
                                InetAddress.getByName(m.to.getIpAddress()),
                                m.to.getPort());
                //System.out.println(m.to.getIpAddress() + " "+ m.to.getPort());
                socket.send(datagramPacket);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    private void sendMail(){
        boolean isTime = false;
        int time = curModel.getGameState().getConfig().getStateDelayMs();
        if (isTimeTo(lastTimeForState, time) && nodeRole == NodeRole.MASTER){
           // System.out.println("I master");
            curModel.computeNextStep();
            deputy = Model.getDeputy(curModel.getGameState().getPlayers());

            for (GamePlayer player : curModel.getGameState().getPlayers().getPlayersList()){
                if (uuid.hashCode() != player.getId()){
                    messages.put(new Message(null, master, player), Instant.now());
                }
            }
            isTime = true;
            lastTimeForState = Instant.now().toEpochMilli();
        }


        for (Map.Entry<Message, Instant> entry : messages.entrySet()) {

            try {
                Message m = entry.getKey();
                if (m.to == null) {
                    if (master != null)
                        m.to = master;
                    else {
                        messages.remove(m);
                        continue;
                    }
                }

                if (m.gameMessage == null){
                    m.gameMessage=GameMessage.newBuilder()
                            .setMsgSeq(incState())
                            .setSenderId(uuid.hashCode())
                            .setState(GameMessage.StateMsg.newBuilder().setState(curModel.getGameState()).build())
                            .build();
                }

                datagramPacket =
                        new DatagramPacket(m.gameMessage.toByteArray(),
                                m.gameMessage.toByteArray().length,
                                InetAddress.getByName(m.to.getIpAddress()),
                                m.to.getPort());

                if (m.gameMessage.getTypeCase() == GameMessage.TypeCase.STATE){

                    if (isTime){
                        socket.send(datagramPacket);
                    }
                }
                else {
                    socket.send(datagramPacket);
                    if (m.gameMessage.getTypeCase() == GameMessage.TypeCase.ACK) messages.remove(m);
                    else {
                        toResponse.put(m, Instant.now());
                        messages.remove(m);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isTime) {
            System.gc();
        }

    }

    private void checkPlayerActivity(){
        for (Map.Entry<Integer, Instant> entry : curModel.playerActivity.entrySet()){
            if (Instant.now().toEpochMilli() - entry.getValue().toEpochMilli() >
                    curModel.getGameState().getConfig().getNodeTimeoutMs()){
                if (curModel.getPlayer(entry.getKey()) == null) continue;
                if (curModel.getPlayer(entry.getKey()).getRole() == NodeRole.MASTER) {
                    if (nodeRole == NodeRole.DEPUTY){
                        curModel.setIp(master.getId(), master.getIpAddress());
                        curModel.changeRole(master.getId(), NodeRole.VIEWER, GameState.Snake.SnakeState.ZOMBIE);
                        nodeRole = NodeRole.MASTER;
                        master = Model.makePlayer(uuid.hashCode(),name,0,myAdress.getHostAddress(),NodeRole.MASTER);
                        curModel.repair(uuid.hashCode());
                    }
                    if (nodeRole == NodeRole.NORMAL){
                        master = Model.getDeputy(curModel.getGameState().getPlayers());
                    }
                }
                if (curModel.getPlayer(entry.getKey()).getRole() == NodeRole.DEPUTY) {//
                    if (nodeRole == NodeRole.MASTER){
                        curModel.changeRole(entry.getKey(), NodeRole.VIEWER, GameState.Snake.SnakeState.ZOMBIE);
                        GamePlayer n_d = curModel.setDeputy();
                        if (n_d != null) deputy = n_d;////
                    }
                }
                if (curModel.getPlayer(entry.getKey()).getRole() == NodeRole.NORMAL) {
                    curModel.changeRole(entry.getKey(), NodeRole.VIEWER, GameState.Snake.SnakeState.ZOMBIE);
                }
                sendPingMsg(curModel.getPlayer(entry.getKey()));
            }
        }
    }

    private void sendAnnouncements(){
        byte[] data;

        GameMessage.AnnouncementMsg announcementMsg = GameMessage.AnnouncementMsg.newBuilder()
                .setCanJoin(true)
                .setPlayers(curModel.getGameState().getPlayers())
                .setConfig(curModel.getConfig())
                .build();
        GameMessage mess = GameMessage.newBuilder()
                .setMsgSeq(0)
                .setSenderId(0)
                .setReceiverId(0)
                .setAnnouncement(announcementMsg)
                .build();

        try {
            data = mess.toByteArray();
            datagramPacket = new DatagramPacket(data, data.length,InetAddress.getByName(mcastIP), mcastPort);
            multiSocket.send(datagramPacket);

        }
        catch (Exception e){e.printStackTrace();}

    }

    public  void receiveUnicast(GameMessage message, InetAddress senderIp, int senderPort){
        this.senderIp = senderIp;
        this.senderPort = senderPort;
        if (message != null) {
            switch (message.getTypeCase()) {
                case ANNOUNCEMENT:
                    System.out.println("Wait, that's illegal");
                    break;
                case ACK:
                    handleAck(message);
                    break;
                case JOIN:
                    handleJoin(message);
                    break;
                case STEER:
                    handleSteer(message);
                    break;
                case STATE:
                    handleState(message);
                    break;
                case ROLE_CHANGE:
                    handleRoleChange(message);
                    break;
                default: createAndPutAck(message);
            }
            curModel.updatePlayer(message.getSenderId());
        }
    }

    public  void receiveMulticast(GameMessage message, InetAddress senderIp, int senderPort){
        this.senderIp = senderIp;
        this.senderPort = senderPort;
        if (message != null) {
            switch (message.getTypeCase()) {
                case ANNOUNCEMENT:
                    handleAnnouncement(message);
                    break;
                default:
                    System.out.println(message.getTypeCase());
            }
           // curModel.updatePlayer(message.getSenderId());
        }
    }

    private void handleState(GameMessage message){
        //System.out.println(message.getState().getState().getPlayers());
        if (master == null) return;
        GamePlayer m = Model.getMaster(message.getState().getState().getPlayers());
       if ( m != null) {
           //System.out.println(master.getName() + " " + Model.getMaster(message.getState().getState().getPlayers()).getName());
           if (master.getId() == m.getId()) {
               curModel.setGameState(message.getState().getState());
               createAndPutAck(message);
           }
       }

    }

    private void handleAnnouncement(GameMessage announcementMsg){
        GamePlayer master = Model.getMaster(announcementMsg.getAnnouncement().getPlayers());
        if (master == null) return;
        master = master.toBuilder().setIpAddress(senderIp.getHostAddress()).build();
        Message message = new Message(announcementMsg,master, null);
        for (Map.Entry<Message, Instant> entry : announcements.entrySet()){
            if (entry.getKey().from.getId() == master.getId()){
                announcements.remove(entry.getKey());
                announcements.put(message,Instant.now());
                break;
            }

        }
        announcements.put(message,Instant.now());
    }

    private void handleRoleChange(GameMessage message){


        if (message.getRoleChange().hasSenderRole()){
          //  System.out.println(message.getRoleChange().getSenderRole());
            if (message.getRoleChange().getSenderRole() == NodeRole.VIEWER){
                curModel.changeRole(message.getSenderId(), message.getRoleChange().getSenderRole(), GameState.Snake.SnakeState.ZOMBIE);
            }
        }

        if (message.getRoleChange().hasReceiverRole()){
            //System.out.println(message.getRoleChange().getReceiverRole());
            if (message.getRoleChange().getReceiverRole() == NodeRole.DEPUTY){
                nodeRole = NodeRole.DEPUTY;
            }
            if (message.getRoleChange().getReceiverRole() == NodeRole.MASTER){
                //мастер вышел по кнопкеи и мы его заместитель
                curModel.setIp(master.getId(), master.getIpAddress());
                nodeRole = NodeRole.MASTER;
                master = Model.makePlayer(uuid.hashCode(),name,0,myAdress.getHostAddress(),NodeRole.MASTER);
                curModel.repair(uuid.hashCode());
            }
            return;
        }
        createAndPutAck(message);
    }

    private void handleSteer(GameMessage message){
        Direction d = message.getSteer().getDirection();
        curModel.changeDirection(d, message.getSenderId(), message.getMsgSeq());
        createAndPutAck(message);
    }

    private void handleAck(GameMessage message){
       // System.out.println("sizebefore = " + messages.size());
        for (Map.Entry<Message, Instant> entry : messages.entrySet()){
            Message m = entry.getKey();
            if (m.gameMessage.getMsgSeq() == message.getMsgSeq() &&
                    message.getSenderId() == m.getTo().getId()){
               // System.out.println("f = " + message.getMsgSeq());
               // System.out.println("delete");
                messages.remove(m);
            }
        }

        for (Map.Entry<Message, Instant> entry : toResponse.entrySet()){
            Message m = entry.getKey();
            if (m.gameMessage.getMsgSeq() == message.getMsgSeq() &&
                    message.getSenderId() == m.getTo().getId()){
               // System.out.println("deleteRe");
                messages.remove(m);
            }
        }
       // System.out.println("size = " + messages.size());
    }

    private void handleJoin(GameMessage message){
        GamePlayer newPlayer = Model.makePlayer(message.getSenderId(),message.getJoin().getName(), senderPort, senderIp.getHostAddress(), NodeRole.NORMAL );
        createAndPutAck(message);
        if (curModel.addPlayer(newPlayer) == 1) return;
        if (deputy == null){
            deputy = newPlayer;
            curModel.changeRole(message.getSenderId(), NodeRole.DEPUTY, GameState.Snake.SnakeState.ALIVE);
            sendChangeRoleMsg(newPlayer,NodeRole.MASTER, NodeRole.DEPUTY);
        }
    }


    public void createAndPutAck(GameMessage message){

        GameMessage.AckMsg ackMsg = GameMessage.AckMsg.newBuilder().build();
        GameMessage mess = GameMessage.newBuilder()
                .setAck(ackMsg)
                .setMsgSeq(message.getMsgSeq())
                .setSenderId(uuid.hashCode())
                .build();
        Message message1 = new Message(mess,Model.makePlayer(uuid.hashCode(),name,myPort,"", nodeRole),
                Model.makePlayer(message.getSenderId(),"Вася", senderPort,senderIp.getHostAddress(), NodeRole.NORMAL));
        messages.put(message1, Instant.now());

    }

    public void sendJoinGame(GamePlayer to, GameConfig config){
        if (to.getId() == uuid.hashCode()) {
            curModel.addPlayer(master);
            return;
        }
        nodeRole = NodeRole.NORMAL;
        master = to;

        try {
            curModel.setMasterIp(master.getId());
        }
        catch (Exception ignored){}

        GameMessage.JoinMsg joinMsg = GameMessage.JoinMsg.newBuilder()
                .setOnlyView(false)
                .setName(name)
                .build();
        GameMessage message = GameMessage.newBuilder()
                .setMsgSeq(incState())
                .setJoin(joinMsg)
                .setSenderId(uuid.hashCode())
                .setReceiverId(to.getId())
                .build();
        Message mes = new Message(message, Model.makePlayer(uuid.hashCode(),name,myPort,"", nodeRole),to);
        synchronized (messages) {
            messages.put(mes, Instant.now());
        }
    }

    public void sendPingMsg(GamePlayer to){
        GameMessage message = GameMessage.newBuilder()
                .setMsgSeq(incState())
                .setPing(GameMessage.PingMsg.newBuilder().build())
                .setSenderId(uuid.hashCode())
                .setReceiverId(to.getId())
                .build();
        Message mes = new Message(message, Model.makePlayer(uuid.hashCode(),name,myPort,"", nodeRole),to);
        messages.put(mes, Instant.now());
    }

    public void sendChangeRoleMsg(GamePlayer to, NodeRole srole, NodeRole rrole){

        if (to == null) {
            if (nodeRole == NodeRole.MASTER){
                //мастер вышел и мы его заместитель
                to = deputy;
                master = to;
                rrole = NodeRole.MASTER;

            }
            else  to = master;
            nodeRole = NodeRole.VIEWER;

        }
        if (to == null) return;
        GameMessage.RoleChangeMsg roleChangeMsg = GameMessage.RoleChangeMsg.newBuilder()
                .setSenderRole(srole)
                .setReceiverRole(rrole)
                .build();
        GameMessage message = GameMessage.newBuilder()
                .setMsgSeq(incState())
                .setRoleChange(roleChangeMsg)
                .setSenderId(uuid.hashCode())
                .setReceiverId(to.getId())
                .build();
        Message mes = new Message(message, Model.makePlayer(uuid.hashCode(),name,myPort,"", nodeRole),to);
        messages.put(mes, Instant.now());
    }

    public void sendSteerMsg(Direction d){
        GameMessage.SteerMsg steerMsg = GameMessage.SteerMsg.newBuilder()
                .setDirection(d)
                .build();
        GameMessage message = GameMessage.newBuilder()
                .setMsgSeq(incState())
                .setSteer(steerMsg)
                .setSenderId(uuid.hashCode())
                .setReceiverId(master.getId())
                .build();
        Message mes = new Message(message, Model.makePlayer(uuid.hashCode(),name,myPort,"", nodeRole),null);
        messages.put(mes, Instant.now());
        isActive = true;
    }

    public  void Notify(int x){
        guiController.updateAnn(announcements);
    }

    public void changeDirection(Direction d){
        if (nodeRole == NodeRole.MASTER){
            curModel.changeDirection(d, uuid.hashCode(), curModel.changeHelper.get(uuid.hashCode()) + 1);
        }
        if (nodeRole == NodeRole.NORMAL || nodeRole ==NodeRole.DEPUTY){
            sendSteerMsg(d);
        }
    }
    public void changeRole(NodeRole nodeRole){
        master = Model.makePlayer(uuid.hashCode(),name,0,myAdress.getHostAddress(),NodeRole.MASTER);
        this.nodeRole = nodeRole;
    }
}
