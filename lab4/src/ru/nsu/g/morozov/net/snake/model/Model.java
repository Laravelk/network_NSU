package ru.nsu.g.morozov.net.snake.model;

import java.time.Instant;
import java.util.*;

import static java.lang.Math.abs;
import ru.nsu.g.morozov.net.snake.protocol.SnakesProto.*;
import ru.nsu.g.morozov.net.snake.utils.Publisher;

public class Model extends Publisher {
    public enum State{
        NOGAME,
        GAME
    }
    public State state = State.NOGAME;
    private int masterId;
    private volatile GameState gameState;
    private GamePlayers gamePlayers = GamePlayers.newBuilder().build();

    private HashMap<Integer, LinkedList<GameState.Coord>> Snakes = new HashMap<>();
    private HashMap<Integer, Direction> changeD = new HashMap<>();
    public HashMap<Integer, Long> changeHelper = new HashMap<>();
    public HashMap<Integer, Instant> playerActivity = new HashMap<>();

    public int getWidth() {
        return gameState.getConfig().getWidth();
    }
    public int getHeight() {
        return gameState.getConfig().getHeight();
    }
    public GameConfig getConfig(){return gameState.getConfig();}
    public State getState() {
        return state;
    }
    public void setMasterIp(int address){
        masterId = address;
    }
    public GameState getGameState() {
        return gameState;
    }

    public HashMap<Integer, LinkedList<GameState.Coord>> getSnakes() {
        return Snakes;
    }

    public int getMasterIp() {
        return masterId;
    }

    private int countAlive(){
        int i = 0;
        for(GameState.Snake shake : gameState.getSnakesList()){
            if (shake.getState() == GameState.Snake.SnakeState.ALIVE)
                i++;
        }
        return i;
    }

    public Model(int w, int h){
        GameStateInit(GameConfigInit(w,h));
    }

    public void newGameAsMaster(GameConfig gameConfig, String name, int id, int port){
        gamePlayers = GamePlayers.newBuilder().build();
        Snakes = new HashMap<>();
        changeD = new HashMap<>();
        changeHelper = new HashMap<>();
        playerActivity = new HashMap<>();
        GameStateInit(gameConfig);
        GamePlayer self = GamePlayer.newBuilder()
                .setId(id)
                .setName(name)
                .setPort(port)
                .setRole(NodeRole.MASTER)
                .setIpAddress("")
                .setScore(0)
                .build();
        masterId = id;
        state = State.GAME;

        addPlayer(self);

        NotifyAll(1);
    }

    public void setGameState(GameState gameState){

        this.gameState = gameState;
        for (GameState.Snake snake: gameState.getSnakesList()){
            Snakes.put(snake.getPlayerId(), snakeToList(snake));
        }
        NotifyAll(1);
    }


    private GameState.Snake addSnake(int id){
        for (GameState.Snake snake:gameState.getSnakesList()){
            if (snake.getPlayerId() == id) return null;
        }
        GameState.Snake.Builder snakeBuilder = GameState.Snake.newBuilder();
        LinkedList<GameState.Coord> coords = findEmptyPlace();
        if (coords == null) return null;
        snakeBuilder.addPoints(coords.get(0));
        snakeBuilder.addPoints(coords.get(1));

        Direction direction = Direction.DOWN;
        GameState.Snake snake = snakeBuilder
                .setPlayerId(id)
                .setHeadDirection(direction)
                .setState(GameState.Snake.SnakeState.ALIVE)
                .build();
        gameState = gameState.toBuilder().addSnakes(snake).build();
        changeD.put(snake.getPlayerId(), snake.getHeadDirection());
        changeHelper.put(snake.getPlayerId(), Long.parseLong("0"));
        return snakeBuilder.build();

    }

    public int addPlayer(GamePlayer player){
        GameState.Snake s = null;
        boolean ingame = false;
        for (GamePlayer player1 : gameState.getPlayers().getPlayersList()){
            if (player1.getId() == player.getId() ){
                ingame = true;
                if (player1.getId() != masterId) changeRole(player.getId(),NodeRole.NORMAL, GameState.Snake.SnakeState.ALIVE);
            }
        }
        if (player.getRole() != NodeRole.VIEWER) {
            s = addSnake(player.getId());
            if (s != null){
                Snakes.put(player.getId(), snakeToList(s));
            }
        }
        if (s == null){
            player = player.toBuilder().setRole(NodeRole.VIEWER).build();
        }
        if (!ingame) {
            gamePlayers = gameState.getPlayers().toBuilder().addPlayers(player).build();
            gameState = gameState.toBuilder().setPlayers(gamePlayers).build();

        }
        if (s == null) return 2;
        return 0;
    }

    private LinkedList<GameState.Coord> findEmptyPlace(){
        Random random = new Random();
        boolean isJoinable = false;
        int x = random.nextInt() % getWidth();
        int y = random.nextInt() % getHeight();
        for (int i = x; i < x + getWidth(); i++){
            for(int j = y; j < y+ getHeight(); j++){
                if (isFreeRectangle(i,j,i+4,j+4)){
                    isJoinable = true;
                    break;
                }
            }
        }
        if (!isJoinable) return  null;


        LinkedList<GameState.Coord> coords = new LinkedList<>();
        GameState.Coord coord1 = makeCoord(x+2, y+2);
        GameState.Coord coord2 = makeCoord(0, 1);

        coords.add(coord1);
        coords.add(coord2);
        return coords;
    }
    private boolean isFreeRectangle(int x1, int x2, int y1, int y2){
        for (int i = x1; i < x2; i++){
            for (int j = y1; j< y2;j++){
                for (GameState.Snake snake: gameState.getSnakesList()){
                    for (GameState.Coord coord : snake.getPointsList()){
                        if (makeCoord(i, j).equals(coord)) return false;
                    }
                }
            }
        }
        return true;
    }
    public void addFood(){
        Vector<GameState.Coord> freePlaces = new Vector<>();
        for (int i = 0; i < getWidth(); i++){
            for (int j = 0; j< getHeight(); j++){
                GameState.Coord coord1 = makeCoord(i, j);
                for (GameState.Snake snake: gameState.getSnakesList()){
                    for (GameState.Coord coord : snake.getPointsList()){
                        if (!coord1.equals(coord))  freePlaces.add(makeCoord(i,j));
                    }
                }
                for (GameState.Coord coord : gameState.getFoodsList()){
                    if (!coord1.equals(coord))  {
                        freePlaces.add(coord1);
                    }
                }
            }
        }
        GameState.Builder builder = gameState.toBuilder();
        int foodCount = gameState.getFoodsCount();
        while (foodCount < gameState.getConfig().getFoodStatic() + gameState.getConfig().getFoodPerPlayer()*countAlive()){
            GameState.Coord food = tryAddFruit(freePlaces);
            if (food == null) break;

            builder.addFoods(food);
            freePlaces.remove(food);
            foodCount++;
        }
        gameState = builder.build();
    }

    private GameState.Coord tryAddFruit(Vector<GameState.Coord> coords){
        if (coords.size() == 0) return null;

        Random random = new Random();
        return coords.get(abs(random.nextInt()) % coords.size());
    }

    private GameConfig GameConfigInit(int w, int h){
        return  GameConfig.newBuilder()
                .setWidth(w)
                .setHeight(h)
                .setFoodStatic(10)
                .setFoodPerPlayer((float)0.2)
                .setStateDelayMs(1000)
                .setDeadFoodProb((float)0.2)
                .setPingDelayMs(3000)
                .setNodeTimeoutMs(9000)
                .build();
    }

    private void GameStateInit(GameConfig gameConfig) {
        GameState.Builder builder = GameState.newBuilder()
                .setConfig(gameConfig)
                .setPlayers(gamePlayers)
                .setStateOrder(0);

        gameState = builder.build();
    }



    public void computeNextStep(){
        LinkedList<GameState.Snake> snakes = new LinkedList<>();
        LinkedList<Integer> deadSnakes;
        for (GameState.Snake snake: gameState.getSnakesList()){
            snake = computeSnake(snake);
            snakes.add(snake);
        }
        deadSnakes = checkDeath();
        snakes = killSome(deadSnakes, snakes);
        gameState = gameState.toBuilder().clearSnakes().build();
        for (GameState.Snake snake: snakes){
            gameState =gameState.toBuilder().addSnakes(snake).build();
        }
        addFood();
        NotifyAll(1);

    }
    private GameState.Snake computeSnake(GameState.Snake snake){
        snake = snake.toBuilder().setHeadDirection(changeD.get(snake.getPlayerId())).build();
        snake = setPoints(snake, makeStep(Snakes.get(snake.getPlayerId()), snake.getHeadDirection(), snake.getPlayerId()));
        Snakes.replace(snake.getPlayerId(), snakeToList(snake));
        return snake;
    }

    private LinkedList<Integer> checkDeath(){
        LinkedList<Integer> death = new LinkedList<>();
        for (int i = 0; i < gameState.getSnakesCount(); i++){
            int id1 = gameState.getSnakes(i).getPlayerId();
            for (int j = 0; j < gameState.getSnakesCount(); j++){
                int id2 = gameState.getSnakes(j).getPlayerId();
                int begin = 1;
                if ( i!= j) begin = 0;
                if (isDead(Snakes.get(id1).getFirst(), Snakes.get(id2), begin)){
                    //System.out.println("СМЭРТЬ");
                    death.add(id1);
                }

            }
        }
        return death;
    }
    private LinkedList<GameState.Snake> killSome(LinkedList<Integer> death, LinkedList<GameState.Snake> s){
        LinkedList<GameState.Snake> snakes = new LinkedList<>();
        for (int j = 0; j < s.size(); j++){
            boolean isDead = false;
            for (int i = 0; i < death.size(); i++){
                if (death.get(i) == s.get(j).getPlayerId()){
                    killSnake(death.get(i));
                    isDead = true;
                    Snakes.remove(death.get(i));
                    break;
                }
            }
            if (!isDead)
                snakes.add(s.get(j));
        }
        return snakes;
    }

    private void killSnake(int id){
        setScore(id, -1);
        GameState.Builder builder = gameState.toBuilder();
        for (GameState.Coord coord : Snakes.get(id)){
            Random random = new Random();
            int proc = abs(random.nextInt() % 100);
            if (proc < gameState.getConfig().getDeadFoodProb()*100){
                builder.addFoods(coord);
            }
        }

        gameState = builder.build();
        if (id != masterId)
            changeRole(id, NodeRole.VIEWER, GameState.Snake.SnakeState.ZOMBIE);
        else
            changeRole(id, NodeRole.MASTER, GameState.Snake.SnakeState.ZOMBIE);
    }


    private boolean isDead(GameState.Coord head, LinkedList<GameState.Coord> s2, int begin){
        for (int i = begin; i < s2.size(); i++){
            if (s2.get(i).equals(head))
                return true;
        }
        return false;
    }

    public LinkedList<GameState.Coord> snakeToList(GameState.Snake snake){
        int i =0;
        LinkedList<GameState.Coord> list = new LinkedList<>();
        list.add(snake.getPoints(0));
        for (GameState.Coord coord : snake.getPointsList()){
            if ( i== 0){
                i++;
                continue;
            }
            if (coord.getY() != 0){
                int y = coord.getY();
                int it = 1;
                if ( y < 0) it = -1;
                while (y != 0){
                    list.add(makeCoord(list.getLast().getX(), list.getLast().getY() + it));
                    y -= it;
                }
            }

            if (coord.getX() != 0){
                int x = coord.getX();
                int it = 1;
                if ( x < 0) it = -1;
                while (x != 0){
                    list.add(makeCoord(list.getLast().getX() + it, list.getLast().getY()));
                    x -= it;
                }
            }
        }
        return list;
    }
    private LinkedList<GameState.Coord> makeStep(LinkedList<GameState.Coord> coords, Direction direction, int id){
        switch (direction){
            case UP: coords.addFirst(makeCoord(coords.getFirst().getX(), coords.getFirst().getY() - 1));
                break;
            case DOWN: coords.addFirst(makeCoord(coords.getFirst().getX(), coords.getFirst().getY() + 1));
                break;
            case LEFT: coords.addFirst(makeCoord(coords.getFirst().getX() - 1, coords.getFirst().getY()));
                break;
            case RIGHT: coords.addFirst(makeCoord(coords.getFirst().getX() + 1, coords.getFirst().getY()));
                break;
        }

        if (!isFood(coords.getFirst())) {
            coords.removeLast();
        }
        else {
            removeFood(coords.getFirst());
            setScore(id, 1);
        }
        return coords;
    }

    private void removeFood(GameState.Coord coord){
        Vector<GameState.Coord> vector = new Vector<>();
        for (GameState.Coord coord1: gameState.getFoodsList()){
            if (!coord.equals(coord1)) vector.add(coord1);
        }
        GameState.Builder builder = gameState.toBuilder().clearFoods();
        for (GameState.Coord coord1 : vector){
            builder.addFoods(coord1);
        }
        gameState = builder.build();
    }
    private boolean isFood(GameState.Coord c){
        for (GameState.Coord coord : gameState.getFoodsList()){
            if (c.equals(coord)) return true;
        }
        return false;
    }
    private GameState.Snake setPoints(GameState.Snake snake, LinkedList<GameState.Coord> coords){
        GameState.Snake.Builder builder = snake.toBuilder().clearPoints();
        LinkedList<GameState.Coord> list = new LinkedList<>();
        list.add(coords.getFirst());
        builder.addPoints(list.getLast());
        int prevDIffX = coords.get(1).getX()  - coords.get(0).getX();
        int prevDiffY = coords.get(1).getY()  - coords.get(0).getY();

        prevDIffX = normalizeDiff(prevDIffX);
        prevDiffY = normalizeDiff(prevDiffY);

        for (int i = 2; i < coords.size(); i++){
            int diffX = coords.get(i).getX()  - coords.get(i -1).getX();
            int diffY = coords.get(i).getY()  - coords.get(i -1).getY();

            diffX = normalizeDiff(diffX);
            diffY = normalizeDiff(diffY);

            if ((diffX == 0 &&  prevDIffX !=0) || (diffY == 0 && prevDiffY != 0)){
                list.add(makeNativeCoord(prevDIffX, prevDiffY));
                builder.addPoints(list.getLast());
                prevDIffX = diffX;
                prevDiffY = diffY;
            }
            else {
                prevDIffX += diffX;
                prevDiffY += diffY;
            }

        }

        builder.addPoints(makeNativeCoord(prevDIffX, prevDiffY));
        return builder.build();
    }
    private int normalizeDiff(int diff){
        if (diff*(-1) == getHeight() - 1) return 1;
        if (diff == getHeight() - 1) return -1;
        if (diff < 0) return -1;
        if (diff == 0) return 0;
        else return 1;
    }

    private GameState.Coord makeCoord(int x, int y){
        x = (x + getWidth()) % getWidth();
        y = (y + getHeight()) % getHeight();
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    private GameState.Coord makeNativeCoord(int x, int y){
        return GameState.Coord.newBuilder().setX(x).setY(y).build();
    }

    private Direction reversed(Direction direction){
        switch (direction){
            case UP: return Direction.DOWN;
            case DOWN: return Direction.UP;
            case LEFT: return Direction.RIGHT;
            case RIGHT: return Direction.LEFT;
        }
        return null;
    }

    public void changeDirection(Direction direction, int id, long num){
        GameState.Snake s = null;
        for (GameState.Snake snake : gameState.getSnakesList()){
            if (snake.getPlayerId() == id && snake.getState() != GameState.Snake.SnakeState.ZOMBIE){
                s = snake;
                break;
            }
        }

        if (s == null) return;
        if (s.getHeadDirection() != reversed(direction)) {
            if (!changeHelper.containsKey(id)) return;
            if( num > changeHelper.get(id)){
                changeHelper.put(id, num);
                changeD.put(s.getPlayerId(), direction);
            }
        }

    }
    public GamePlayer setDeputy(){
        GamePlayer player = null;
        GamePlayers.Builder players = GamePlayers.newBuilder();
        for (GamePlayer gamePlayer : gameState.getPlayers().getPlayersList()){
            if (gamePlayer.getRole() == NodeRole.NORMAL) {
                gamePlayer = gamePlayer.toBuilder().setRole(NodeRole.DEPUTY).build();
                player = gamePlayer;
            }
            players.addPlayers(gamePlayer);
        }
        gameState = gameState.toBuilder().setPlayers(players).build();
        return player;
    }
    public void updatePlayer(int id){
        playerActivity.put(id,Instant.now());
    }

    public void repair(int id){
        masterId = id;
        gamePlayers = GamePlayers.newBuilder().build();
        Snakes = new HashMap<>();
        changeD = new HashMap<>();
        changeHelper = new HashMap<>();
        playerActivity = new HashMap<>();

        gamePlayers = gameState.getPlayers();
        changeRole(id, NodeRole.MASTER, GameState.Snake.SnakeState.ALIVE);

        for (GameState.Snake snake:gameState.getSnakesList()){
            Snakes.put(snake.getPlayerId(), snakeToList(snake));
            changeD.put(snake.getPlayerId(),getDirection(snake));
        }
        for(GamePlayer player : gameState.getPlayers().getPlayersList()){
           // System.out.println(player.getName() + " " +player.getRole());
            changeHelper.put(player.getId(), 0L);
            playerActivity.put(player.getId(), Instant.now());

        }
      //  System.out.println(gameState);

    }
    public Direction getDirection(GameState.Snake snake){
        List<GameState.Coord> coords = snake.getPointsList();
        if (coords.get(1).getX() < 0) return Direction.RIGHT;
        if (coords.get(1).getX() > 0) return Direction.LEFT;
        if (coords.get(1).getY() < 0) return Direction.DOWN;
        if (coords.get(1).getY() > 0) return Direction.UP;
        return Direction.DOWN;
    }
    public void setIp(int id, String address){
        GamePlayers.Builder players = GamePlayers.newBuilder();
        for (GamePlayer gamePlayer : gameState.getPlayers().getPlayersList()){
            if (gamePlayer.getId() == id) {
                gamePlayer = gamePlayer.toBuilder().setIpAddress(address).build();
            }
            players.addPlayers(gamePlayer);
        }
        gameState = gameState.toBuilder().setPlayers(players).build();
    }
    public void setScore(int id, int score){
        GamePlayers.Builder players = GamePlayers.newBuilder();
        for (GamePlayer gamePlayer : gameState.getPlayers().getPlayersList()){
            if (gamePlayer.getId() == id) {
                if(score == -1)
                    gamePlayer = gamePlayer.toBuilder().setScore(0).build();
                else
                    gamePlayer = gamePlayer.toBuilder().setScore(gamePlayer.getScore() + score).build();
            }
            players.addPlayers(gamePlayer);
        }
        gameState = gameState.toBuilder().setPlayers(players).build();
    }
    public void changeRole(int id, NodeRole role, GameState.Snake.SnakeState snakeState){
        GamePlayers.Builder players = GamePlayers.newBuilder();
        for (GamePlayer gamePlayer : gameState.getPlayers().getPlayersList()){
            if (gamePlayer.getId() == id && role != null) gamePlayer = gamePlayer.toBuilder().setRole(role).build();
            players.addPlayers(gamePlayer);
        }
        LinkedList<GameState.Snake> snakes = new LinkedList<>();
        for (GameState.Snake snake : gameState.getSnakesList()){
            if (snake.getPlayerId() == id && snakeState!= null) snake = snake.toBuilder().setState(snakeState).build();
            snakes.add(snake);
        }
        GameState.Builder builder = gameState.toBuilder();
        builder.clearSnakes();
        for( GameState.Snake snake : snakes){
            builder.addSnakes(snake);
        }
        gameState = builder.build();
        gameState = gameState.toBuilder().setPlayers(players).build();
    }

    public GamePlayer getPlayer(int id){
        for (GamePlayer player:getGameState().getPlayers().getPlayersList()){
            if (player.getId() == id) return player;
        }
        return null;
    }

    public boolean isAlive(int id){
        for (GameState.Snake snake : gameState.getSnakesList()){
            if (snake.getPlayerId() == id && snake.getState() == GameState.Snake.SnakeState.ALIVE)
                return  true;
        }
        return false;
    }

    public static GamePlayer getMaster(GamePlayers players){
        for (GamePlayer player : players.getPlayersList()){
            if (player.getRole() == NodeRole.MASTER) return  player;
        }
        return  null;
    }

    public static GamePlayer getDeputy(GamePlayers players){
        for (GamePlayer player : players.getPlayersList()){
            if (player.getRole() == NodeRole.DEPUTY) return  player;
        }
        return  null;
    }

    public static GamePlayer makePlayer(int id, String name, int port, String address, NodeRole role){
        GamePlayer player = GamePlayer.newBuilder()
                .setId(id)
                .setName(name)
                .setPort(port)
                .setRole(role)
                .setIpAddress(address)
                .setScore(0)
                .build();
        return player;
    }
}
