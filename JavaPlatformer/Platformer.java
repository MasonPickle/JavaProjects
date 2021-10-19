import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import javax.swing.*;
import java.awt.geom.Line2D;

public class Platformer extends JPanel implements Runnable {

    Thread gameThread;
    Font smallFont;
    volatile boolean gameOver = true;

    static class Player{
        float x, y, angle, xVel, yVel, oldY;
        int size;
        boolean left, right, up;
        Player(float x, float y, float angle, int size, boolean left, boolean right, boolean up,
               float xVel, float yVel, float oldY){
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.size = size;
            this.left = left;
            this.right = right;
            this.up = up;
            this.xVel = xVel;
            this.yVel = yVel;
            this.oldY = oldY;
        }
    }
    Player player;

    static class Platform{
        int x, y, width, height, angle;

        Platform(int x, int y, int width, int height, int angle){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angle = angle;
        }
    }
    static class Pickups{
        int x, y, size;
        boolean active;

        Pickups(int x, int y, int size, boolean active){
            this.x = x;
            this.y = y;
            this.size = size;
            this.active = active;
        }
    }
    static class Obstacles{
        int x, y, width, height, angle;

        Obstacles(int x, int y, int width, int height, int angle){
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.angle = angle;
        }
    }
    static class Goal{
        int x, y, size;
        boolean active;

        Goal(int x, int y, int size, boolean active){
            this.x = x;
            this.y = y;
            this.size = size;
            this.active = active;
        }
    }
    static class Level{
        Platform[] platforms;
        Pickups[] pickups;
        Obstacles[] obstacles;
        Goal goal;
        int xStart, yStart;

        Level(Platform[] platforms, Pickups[] pickups, Obstacles[] obstacles, Goal goal, int xStart, int yStart){
            this.platforms = platforms;
            this.pickups = pickups;
            this.obstacles = obstacles;
            this.goal = goal;
            this.xStart = xStart;
            this.yStart = yStart;
        }
    }
    Level[] levels;
    int levelNum = 0;

    int gravity = 1, jumpHeight = -12, wjH = 12, wjV = -12;
    float speed = 1.25f, friction = 0.8f;

    Point2D RotPoint(Point2D point, Point2D pivot, float pointAngle){
        Point2D result = new Point2D.Double();
        AffineTransform rotation = new AffineTransform();
        double angleInRadians = (pointAngle * Math.PI / 180);
        rotation.rotate(angleInRadians, pivot.getX(), pivot.getY());
        rotation.transform(point, result);
        return result;
    }
    boolean SAT(float x, float y, float w, float h, float angleOne, float x2, float y2, float w2,
                float h2, float angleTwo){
        boolean result = false;

        Line2D[] objOne = new Line2D[4];
        Line2D[] objTwo = new Line2D[4];
        Point2D[] p1 = new Point2D[4];
        Point2D[] p2 = new Point2D[4];

        p1[0] = new Point2D.Float(x, y);
        p1[1] = new Point2D.Float(x+w, y);
        p1[2] = new Point2D.Float(x+w, y+h);
        p1[3] = new Point2D.Float(x, y+h);
        for(int i = 0; i < 4; i++){
            p1[i] = RotPoint(p1[i], new Point2D.Float(x+(w/2), y+(h/2)),angleOne);
        }
        p2[0] = new Point2D.Float(x2, y2);
        p2[1] = new Point2D.Float(x2+w2, y2);
        p2[2] = new Point2D.Float(x2+w2, y2+h2);
        p2[3] = new Point2D.Float(x2, y2+h2);
        for(int i = 0; i < 4; i++){
            p2[i] = RotPoint(p2[i], new Point2D.Float(x2+(w2/2), y2+(h2/2)),angleTwo);
        }

        objOne[0] = new Line2D.Float(p1[0],p1[1]);
        objOne[1] = new Line2D.Float(p1[1],p1[2]);
        objOne[2] = new Line2D.Float(p1[2],p1[3]);
        objOne[3] = new Line2D.Float(p1[3],p1[0]);

        objTwo[0] = new Line2D.Float(p2[0],p2[1]);
        objTwo[1] = new Line2D.Float(p2[1],p2[2]);
        objTwo[2] = new Line2D.Float(p2[2],p2[3]);
        objTwo[3] = new Line2D.Float(p2[3],p2[0]);

        for(int o = 0; o < 4; o++){
            for(int t = 0; t < 4; t++){
                if(objOne[o].intersectsLine(objTwo[t])){
                    result = true;
                }
            }
        }
        return result;
    }

    public Platformer(){
        setPreferredSize(new Dimension(640,440));
        setBackground(Color.black);
        setFont(new Font("TimesNewRoman", Font.BOLD, 48));
        setFocusable(true);

        smallFont = getFont().deriveFont(Font.BOLD, 18);
        addKeyListener(
                new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if(e.getKeyCode() == KeyEvent.VK_SPACE){
                            if(gameOver){
                                StartNewGame();
                                repaint();
                            }
                        }
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_LEFT -> player.left = true;
                            case KeyEvent.VK_RIGHT -> player.right = true;
                            case KeyEvent.VK_UP -> player.up = true;
                        }
                    }
                    public void keyReleased(KeyEvent e) {
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_LEFT -> player.left = false;
                            case KeyEvent.VK_RIGHT -> player.right = false;
                            case KeyEvent.VK_UP -> player.up = false;
                        }
                    }
                }
        );
    }

    void InitLevels(){
        levels = new Level[3];

        levels[0] = new Level(new Platform[3], new Pickups[2], new Obstacles[1],
                new Goal(getPreferredSize().width - 75, getPreferredSize().height/2-50, 50, false),
                getPreferredSize().width/2, getPreferredSize().height-100);
        levels[1] = new Level(new Platform[4], new Pickups[4], new Obstacles[1],
                new Goal(getPreferredSize().width-100, 25, 50, false), getPreferredSize().width-100,
                getPreferredSize().height-100);
        levels[2] = new Level(new Platform[5], new Pickups[3], new Obstacles[1],
                new Goal(50, getPreferredSize().height/2-75, 50, false), getPreferredSize().width-50,
                getPreferredSize().height/2-50);

        for (Level level : levels) {
            for (int i = 0; i < level.platforms.length; i++) {
                level.platforms[i] = new Platform(0, 0, 0, 0, 0);
            }
            for (int i = 0; i < level.pickups.length; i++) {
                level.pickups[i] = new Pickups(0, 0, 10, true);
            }
            for (int i = 0; i < level.obstacles.length; i++) {
                level.obstacles[i] = new Obstacles(0, 0, 0, 0, 0);
            }
        }
        MakePlatforms();
        MakePickups();
        MakeObstacles();
    }
    void MakePlatforms(){
        CreatePlat(0,0,0,getPreferredSize().height-50,getPreferredSize().width,50,0);
        CreatePlat(0,1,0,getPreferredSize().height-200,100,150,0);
        CreatePlat(0,2,getPreferredSize().width-100,getPreferredSize().height-200,100,150,0);

        CreatePlat(1,0,0,getPreferredSize().height-50,getPreferredSize().width,50,0);
        CreatePlat(1,1,0,getPreferredSize().height-200,100,150,0);
        CreatePlat(1,2,150,getPreferredSize().height-300,200,20,345);
        CreatePlat(1,3,500,getPreferredSize().height-325,100,20,0);

        CreatePlat(2,0,0,getPreferredSize().height/2,150,getPreferredSize().height/2,0);
        CreatePlat(2,1,getPreferredSize().width-150,getPreferredSize().height/2,150,getPreferredSize().height/2,0);
        CreatePlat(2,2,200,getPreferredSize().height/2+50,50,50,0);
        CreatePlat(2,3,300,getPreferredSize().height/2+100,50,50,0);
        CreatePlat(2,4,400,getPreferredSize().height/2+50,50,50,0);
    }
    void CreatePlat(int lvl, int pNum, int x, int y, int width, int height, int angle){
        levels[lvl].platforms[pNum].x = x;
        levels[lvl].platforms[pNum].y = y;
        levels[lvl].platforms[pNum].width = width;
        levels[lvl].platforms[pNum].height = height;
        levels[lvl].platforms[pNum].angle = angle;
    }
    void MakePickups(){
        CreatePickups(0,0,20,getPreferredSize().height-230);
        CreatePickups(0,1,getPreferredSize().width-150,getPreferredSize().height-80);

        CreatePickups(1,0,getPreferredSize().width/2-5,getPreferredSize().height-80);
        CreatePickups(1,1,20,getPreferredSize().height-250);
        CreatePickups(1,2,150,getPreferredSize().height-80);
        CreatePickups(1,3,200,getPreferredSize().height-350);

        CreatePickups(2,0,220,getPreferredSize().height/2);
        CreatePickups(2,1,320,getPreferredSize().height/2+50);
        CreatePickups(2,2,420,getPreferredSize().height/2);
    }
    void CreatePickups(int lvl, int pNum, int x, int y){
        levels[lvl].pickups[pNum].x = x;
        levels[lvl].pickups[pNum].y = y;
    }
    void MakeObstacles(){
        CreateObstacles(0,0,100,getPreferredSize().height-60,100,10,0);

        CreateObstacles(1,0,200,getPreferredSize().height-250,getPreferredSize().width-100,20,345);

        CreateObstacles(2,0,150,getPreferredSize().height/2+175,getPreferredSize().width-300,getPreferredSize().height/2-100,0);
    }
    void CreateObstacles(int lvl, int oNum, int x, int y, int width, int height, int angle){
        levels[lvl].obstacles[oNum].x = x;
        levels[lvl].obstacles[oNum].y = y;
        levels[lvl].obstacles[oNum].width = width;
        levels[lvl].obstacles[oNum].height = height;
        levels[lvl].obstacles[oNum].angle = angle;
    }

    void StartNewGame(){
        gameOver = false;
        InitLevels();
        player = new Player(levels[0].xStart, levels[0].yStart, 0, 20, false, false, false, 0, 0, 0);
        Stop();

        (gameThread = new Thread(this)).start();
    }
    void Stop(){
        if(gameThread != null){
            Thread tmp = gameThread;
            gameThread = null;
            tmp.interrupt();
        }
    }

    @Override
    public void run(){
        while (Thread.currentThread() == gameThread){
            try{
                Thread.sleep(25);
            }catch(InterruptedException e){
                return;
            }
            Movement();
            PickupDetect();
            ObstacleDetect();
            GoalDetect();
            repaint();
        }
    }

    void PickupDetect(){
        for(int i = 0; i < levels[levelNum].pickups.length; i++) {
            Pickups p = levels[levelNum].pickups[i];
            if (p.active && SAT(player.x,player.y,player.size,player.size,player.angle,
                    p.x,p.y,p.size,p.size,0)) {
                p.active = false;
            }
        }
    }
    void ObstacleDetect(){
        for(int i = 0; i < levels[levelNum].obstacles.length; i++) {
            Obstacles o = levels[levelNum].obstacles[i];
            if(SAT(player.x,player.y,player.size,player.size,player.angle,
                    o.x,o.y,o.width,o.height,o.angle)){
                player.x = levels[levelNum].xStart;
                player.y = levels[levelNum].yStart;
            }
        }
    }
    void GoalDetect(){
        Goal goal = levels[levelNum].goal;
        if(goal.active && SAT(player.x,player.y,player.size,player.size,player.angle,
                goal.x,goal.y,goal.size,goal.size,0)){
            NewLevel();
        }else{
            int count = 0;
            for(int i = 0; i < levels[levelNum].pickups.length; i++) {
                Pickups p = levels[levelNum].pickups[i];
                if(!p.active)
                    count++;
            }
            if(count == levels[levelNum].pickups.length)
                goal.active = true;
        }
    }

    void NewLevel(){
        if(levelNum < levels.length-1)
            levelNum++;
        else
            levelNum = 0;
        player.x = levels[levelNum].xStart;
        player.y = levels[levelNum].yStart;
        for(int i = 0; i < levels[levelNum].pickups.length; i++)
            levels[levelNum].pickups[i].active = true;
        levels[levelNum].goal.active = false;
    }

    void Movement(){
        player.y += player.yVel;

        if(EdgeDetect() || PlatDetect()){
            for(int i = 0; i < Math.abs(player.yVel); i++){
                if(EdgeDetect() || PlatDetect()) {
                    player.y += (-1 * Math.abs(player.yVel) / player.yVel);
                }
            }
            if(player.up && player.yVel > 0) {
                player.yVel = jumpHeight;
            }
            else {
                player.yVel = 0;
            }
        }
        else {
            player.yVel += gravity;
        }
        if(player.right)
            player.xVel += speed;
        if(player.left)
            player.xVel -= speed;
        player.x += player.xVel;

        if(EdgeDetect() || PlatDetect()){
            player.oldY = player.y;
            for(int i = 0; i < Math.abs(player.xVel) + 1; i++){
                if(EdgeDetect() || PlatDetect()){
                    player.y--;
                }
            }
            if(EdgeDetect() || PlatDetect()){
                player.y = player.oldY;
                for(int i = 0; i < Math.ceil(Math.abs(player.xVel)); i++){
                    if(EdgeDetect() || PlatDetect()){
                        player.x += -1 * (Math.abs(player.xVel)/player.xVel);
                    }
                }
                if(player.up){
                    if(player.xVel < 0){
                        player.xVel = 0;
                        if(player.left){
                            player.xVel = wjH;
                            player.yVel = wjV;
                        }
                    }
                    else{
                        player.xVel = 0;
                        if(player.right){
                            player.xVel = -wjH;
                            player.yVel = wjV;
                        }
                    }
                }
            }
        }
        player.xVel = player.xVel * friction;
    }

    boolean EdgeDetect(){
        return SAT(player.x, player.y, player.size, player.size, player.angle, 0,
                getPreferredSize().height, getPreferredSize().width, 10, 0)
                || SAT(player.x, player.y, player.size, player.size, player.angle, 0, -10,
                getPreferredSize().width, 10, 0)
                || SAT(player.x, player.y, player.size, player.size, player.angle, -10, 0, 10,
                getPreferredSize().height, 0)
                || SAT(player.x, player.y, player.size, player.size, player.angle,
                getPreferredSize().width, 0, 10, getPreferredSize().height, 0);
    }
    boolean PlatDetect(){
        for(int i = 0; i < levels[levelNum].platforms.length; i++){
            Platform p = levels[levelNum].platforms[i];
            if(SAT(player.x, player.y, player.size,player.size,player.angle,p.x,p.y,p.width,p.height,p.angle))
                return true;
        }
        return false;
    }

    void DrawPlatforms(Graphics2D g){
        for(int i = 0; i < levels[levelNum].platforms.length; i++) {
            Platform p = levels[levelNum].platforms[i];
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(p.angle),p.x+(float)p.width/2,p.y+(float)p.height/2);
            g.setColor(Color.white);
            g.fillRect(p.x,p.y,p.width,p.height);
            g.setTransform(old);
        }
    }
    void DrawPickups(Graphics2D g){
        for(int i = 0; i < levels[levelNum].pickups.length; i++) {
            Pickups p = levels[levelNum].pickups[i];
            if(p.active){
                g.setColor(Color.gray);
                g.fillRect(p.x, p.y, p.size, p.size);
            }
        }
    }
    void DrawObstacles(Graphics2D g){
        for(int i = 0; i < levels[levelNum].obstacles.length; i++) {
            Obstacles o = levels[levelNum].obstacles[i];
            AffineTransform old = g.getTransform();
            g.rotate(Math.toRadians(o.angle),o.x+(float)o.width/2,o.y+(float)o.height/2);
            g.setColor(Color.black);
            g.fillRect(o.x,o.y,o.width,o.height);
            g.setColor(Color.white);
            g.drawRect(o.x,o.y,o.width,o.height);
            g.setTransform(old);
        }
    }
    void DrawGoal(Graphics2D g){
        Goal goal = levels[levelNum].goal;
        if (goal.active) {
            g.setColor(Color.green);
        }
        else{
            g.setColor(Color.red);
        }
        g.fillRect(goal.x,goal.y,goal.size,goal.size);
    }
    void DrawPlayer(Graphics2D g){
        g.setColor(Color.white);
        g.fillRect(Math.round(player.x), Math.round(player.y), player.size, player.size);
        g.setColor(Color.black);
        g.fillRect(Math.round(player.x) + (player.xVel < 0 ? 0 : (player.size-5)),
                (int)player.y + 5, 5, 5);
        g.setColor(Color.black);
        g.drawRect(Math.round(player.x),Math.round(player.y),player.size,player.size);
    }
    void DrawStartScreen(Graphics2D g){
        g.setColor(Color.white);
        g.setFont(getFont());
        g.drawString("PLATFORMER", 150, 190);
        g.setColor(Color.white);
        g.setFont(smallFont);
        g.drawString("(Press SPACE To START)", 200, 240);
    }

    @Override
    public void paintComponent(Graphics gg){
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if(gameOver){
            DrawStartScreen(g);
        }else{
            DrawPlayer(g);
            DrawPlatforms(g);
            DrawPickups(g);
            DrawObstacles(g);
            DrawGoal(g);
        }
    }
    public static void main(String[] args){
        SwingUtilities.invokeLater(
                () -> {
                    JFrame mainFrame = new JFrame();
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    mainFrame.setTitle("Platformer");
                    mainFrame.setResizable(true);
                    mainFrame.add(new Platformer(), BorderLayout.CENTER);
                    mainFrame.pack();
                    mainFrame.setLocationRelativeTo(null);
                    mainFrame.setVisible(true);
                });
    }
}
