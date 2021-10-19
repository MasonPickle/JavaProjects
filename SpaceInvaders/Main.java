import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class Main extends JPanel implements Runnable {

    enum  Dir{
        up(0,-1), right(1,0), down(0,1), left(-1,0), none(0,0);

        Dir(int dX, int dY){
            this.dX = dX; this.dY = dY;
        }

        final int dX, dY;
    }

    class Enemy{
        int x;
        int y;
        int type;
        boolean active;

        Enemy(int x, int y, int type, boolean active){
            this.x = x;
            this.y = y;
            this.type = type;
            this.active = active;
        }
    }
    Enemy[][] enemyGrid;
    int enemySpeed = 15;
    Dir enemyDir = Dir.right;
    long enemyTimer = 0;
    int moveTime = 500;

    Enemy saucer;
    Dir saucerDir = Dir.none;
    long saucerTimer = System.currentTimeMillis() + 20000;
    long saucerMoveTimer = System.currentTimeMillis();

    class Bunker{
        int x;
        int y;
        int lives;
        int height;

        Bunker(int x, int y, int lives){
            this.x = x;
            this.y = y;
            this.lives = lives;
        }
    }
    Bunker[] bunkers;

    Thread gameThread;
    Font smallFont;

    static final Random rand = new Random();

    volatile boolean gameOver = true;

    int x = 280;
    int y = 380;
    int speed = 10;
    Dir dir;
    int score = 0;
    int lives = 3;

    long timer = 0;
    boolean spacePressed = false;
    boolean leftPress = false;
    boolean rightPress = false;

    List<Point> laser;
    List<Point> enemyLaser;
    List<Enemy> activeEnemies;
    long eLaserTimer = System.currentTimeMillis() + 1000;

    public Main(){
        setPreferredSize(new Dimension(640, 440));
        setBackground(Color.WHITE);
        setFont(new Font("TimesNewRoman", Font.BOLD, 48));
        setFocusable(true);

        smallFont = getFont().deriveFont(Font.BOLD, 18);

        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if(gameOver){
                            StartNewGame();
                            repaint();
                        }
                    }
                });

        addKeyListener(
                new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        switch (e.getKeyCode()){
                            case KeyEvent.VK_LEFT:
                                dir = Dir.left;
                                leftPress = true;
                                break;
                            case KeyEvent.VK_RIGHT:
                                dir = Dir.right;
                                rightPress = true;
                                break;
                        }

                        if(e.getKeyCode() == KeyEvent.VK_SPACE){
                            spacePressed = true;
                        }
                        repaint();
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        if(e.getKeyCode() == KeyEvent.VK_LEFT){
                            leftPress = false;
                            if(rightPress){
                                dir = Dir.right;
                            }else{
                                dir = Dir.none;
                            }
                        }
                        if(e.getKeyCode() == KeyEvent.VK_RIGHT){
                            rightPress = false;
                            if(leftPress){
                                dir = Dir.left;
                            }else{
                                dir = Dir.none;
                            }

                        }
                        if(e.getKeyCode() == KeyEvent.VK_SPACE){
                            spacePressed = false;
                        }
                    }

                });
    }

    void InitEnemies() {
        enemyGrid = new Enemy[5][8];
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 8; c++) {
                    switch (r) {
                    case 0:
                        enemyGrid[r][c] = new Enemy((c + 1) * 30, (r + 1) * 30, 1, true);
                        activeEnemies.add(enemyGrid[r][c]);
                        continue;
                    case 1, 2:
                        enemyGrid[r][c] = new Enemy((c + 1) * 30, (r + 1) * 30, 2, true);
                        activeEnemies.add(enemyGrid[r][c]);
                        continue;
                    case 3, 4:
                        enemyGrid[r][c] = new Enemy((c + 1) * 30, (r + 1) * 30, 3, true);
                        activeEnemies.add(enemyGrid[r][c]);
                        continue;
                }
            }
        }
    }
    void InitBunkers(){
        bunkers = new Bunker[4];
        for(int i = 0; i < 4; i++){
            bunkers[i] = new Bunker(((i + 1) * 140)-60, 300, 3);
        }
    }
    void InitSaucer(){
        saucer = new Enemy(0, 20, 4, false);
        saucerDir = Dir.none;
    }

    void FireButton(){
        long time = System.currentTimeMillis();
        if(time > timer + 500){
            FireLaser();
            timer = time;
        }
    }

    void StartNewGame(){
        gameOver = false;

        Stop();
        laser = new LinkedList<>();
        enemyLaser = new LinkedList<>();
        activeEnemies = new LinkedList<>();

        dir = Dir.none;
        x = 300;
        y = 380;
        InitEnemies();
        InitBunkers();
        InitSaucer();

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
    public void run() {

        while (Thread.currentThread() == gameThread) {

            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                return;
            }

            MovePlayer();
            MoveLaser();
            HitEnemy();
            EnemyWall();
            MoveEnemy();
            EnemyFire();
            MoveEnemyLaser();
            HitPlayer();
            BunkerHit();
            Saucer();
            HitSaucer();
            if(saucer.active == true)
                SaucerMove();
            if(spacePressed){
                FireButton();
            }

            repaint();
        }
    }

    void gameOver(){
        gameOver = false;
        Stop();
    }
    void NextLevel(){
        moveTime -= 12;
        if(activeEnemies.isEmpty()){
            moveTime = 500;
            enemyDir = Dir.right;
            InitEnemies();
            InitBunkers();
            InitSaucer();
        }
    }

    public int GetRandom(Random rnd, int start, int end, int... exclude){
        int random = start + rnd.nextInt(end - start + 1 - exclude.length);
        for(int ex: exclude){
            if(random < ex){
                break;
            }
            random++;
        }
        return random;
    }

    void MovePlayer(){
        x += dir.dX * speed;
        if(x < 0){
            x = 0;
        }
        if(x + 40 > 640){
            x = 600;
        }
    }
    void MoveLaser(){
        if(!laser.isEmpty()){
            for(Point p : laser){
                p.y -= 15;
                if(p.y < 0){
                    laser.remove(p);
                }
            }
        }
    }
    void HitEnemy(){
        if(!laser.isEmpty()) {
            for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 8; c++) {
                    Enemy e = enemyGrid[r][c];
                    if (e.active) {
                        for (Point p : laser) {
                            if(p.x > e.x - 10 && p.x + 10 < e.x + 30 && p.y + 20 > e.y - 20 && p.y < e.y + 20){
                                switch(r){
                                    case 0:
                                        score += 40;
                                        e.active = false;
                                        activeEnemies.remove(e);
                                        NextLevel();
                                        laser.remove(p);
                                        continue;
                                    case 1, 2:
                                        score += 20;
                                        e.active = false;
                                        activeEnemies.remove(e);
                                        NextLevel();
                                        laser.remove(p);
                                        continue;
                                    case 3, 4:
                                        score += 10;
                                        e.active = false;
                                        activeEnemies.remove(e);
                                        NextLevel();
                                        laser.remove(p);
                                        continue;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void EnemyWall(){
        outerloop:
        for(int r = 0; r < 5; r++) {
            for (int c = 0; c < 8; c++) {
                Enemy e = enemyGrid[r][c];
                if (e.active) {
                    if(e.x + 20 > 640){
                        EnemyDown();
                        enemyDir = Dir.left;
                        enemyTimer = 0;
                        break outerloop;
                    }
                    else if(e.x < 0){
                        EnemyDown();
                        enemyDir = Dir.right;
                        enemyTimer = 0;
                        break outerloop;
                    }

                }
            }
        }
    }
    void EnemyDown(){
        for(int r = 0; r < 5; r++) {
            for (int c = 0; c < 8; c++) {
                Enemy e = enemyGrid[r][c];
                e.y += 3;
                if(e.y + 20 > 300)
                    gameOver();
            }
        }
    }
    void MoveEnemy(){
        long time = System.currentTimeMillis();
        for(int r = 0; r < 5; r++) {
            for (int c = 0; c < 8; c++) {
                Enemy e = enemyGrid[r][c];
                if (e.active && time > enemyTimer) {
                    e.x += enemyDir.dX * enemySpeed;
                }
                if(r == 4 && c == 7 && time > enemyTimer)
                    enemyTimer = time + moveTime;
            }
        }
    }
    void MoveEnemyLaser(){
        if(!enemyLaser.isEmpty()){
            for(Point p : enemyLaser){
                p.y += 15;
                if(p.y > 420){
                    enemyLaser.remove(p);
                }
            }
        }
    }
    void HitPlayer(){
        if(!enemyLaser.isEmpty()){
            for (Point p : enemyLaser) {
                if(p.x + 10 > x - 10 && p.x + 10 < x + 50 && p.y + 20 > y && p.y + 20 < y + 40){
                    lives--;
                    if(lives <= 0)
                        gameOver();
                    enemyLaser.remove(p);
                }
            }
        }
    }

    void EnemyFire(){
        long time = System.currentTimeMillis();
        if(time > eLaserTimer){
            int e = rand.nextInt(activeEnemies.size());
            Point p = new Point(activeEnemies.get(e).x + 15, activeEnemies.get(e).y + 15);
            enemyLaser.add(p);
            eLaserTimer = time + 3000;
        }
    }

    void FireLaser(){
        Point p = new Point(x + 15, y + 15);
        laser.add(p);
    }

    void BunkerHit(){
        if(!enemyLaser.isEmpty()){
            for(int i = 0; i < 4; i++){
                Bunker b = bunkers[i];
                if(b.lives != 0){
                    for(Point p : enemyLaser){
                        if(p.x + 10 > b.x - 5 && p.x + 10 < b.x + 65 && p.y + 20 > b.y){
                            b.lives--;
                            enemyLaser.remove(p);
                        }
                    }
                }
            }
        }
        if(!laser.isEmpty()){
            for(int i = 0; i < 4; i++){
                Bunker b = bunkers[i];
                if(b.lives != 0){
                    for(Point p : laser){
                        if(p.x + 10 > b.x - 5 && p.x + 10 < b.x + 65 && p.y + 20 > b.y){
                            b.lives--;
                            laser.remove(p);
                        }
                    }
                }
            }
        }
    }

    void Saucer(){
        long time = System.currentTimeMillis();

        if(time > saucerTimer){
            System.out.println("timer done");
            int side = rand.nextInt(2);
            switch(side){
                case 0:
                    //left
                    saucer.x = 0;
                    saucerDir = Dir.right;
                    saucer.active = true;
                    break;
                case 1:
                    //right
                    saucer.x = 610;
                    saucerDir = Dir.left;
                    saucer.active = true;
                    break;
            }
            saucerTimer = time + GetRandom(rand, 20000, 40000);
        }
    }
    void SaucerMove(){
        long time = System.currentTimeMillis();
        if(time > saucerMoveTimer){
            saucer.x += saucerDir.dX * 15;
            if(saucerDir == Dir.right && saucer.x < 0){
                saucer.active = false;
            }
            else if(saucerDir == Dir.left && saucer.x > 610){
                saucer.active = false;
            }
            saucerMoveTimer = time + 125;
        }
    }
    void HitSaucer(){
        if(!laser.isEmpty() && saucer.active){
            for (Point p : laser){
                if(p.x + 10 > saucer.x - 10 && p.x < saucer.x + 40 && p.y + 20 > saucer.y
                        && p.y < saucer.y + 40){
                    score += 100;
                    saucer.active = false;
                    laser.remove(p);
                }
            }
        }
    }

    void DrawPlayer(Graphics2D g){
        g.setColor(Color.green);
        g.fillRect(x, y, 40, 40);
    }

    void DrawStartScreen(Graphics2D g){
        g.setColor(Color.red);
        g.setFont(getFont());
        g.drawString("SPACE INVADERS", 100, 190);
        g.setColor(Color.orange);
        g.setFont(smallFont);
        g.drawString("(Click To START)", 250, 240);
    }

    void DrawLaser(Graphics2D g){
        if(!laser.isEmpty()){
            g.setColor(Color.green);
            for(Point p : laser)
                g.fillRect(p.x, p.y, 10, 20);
        }
    }

    void DrawEnemyLaser(Graphics2D g){
        if(!enemyLaser.isEmpty()){
            g.setColor(Color.pink);
            for(Point p : enemyLaser){
                g.fillRect(p.x, p.y, 10, 20);
            }
        }
    }

    void DrawEnemies(Graphics2D g){
        for(int r = 0; r < 5; r++){
            for(int c = 0; c < 8; c++){
                Enemy e = enemyGrid[r][c];
                if(e.active){
                    switch (r) {
                        case 0:
                            g.setColor(Color.red);
                            g.fillRect(e.x, e.y, 20, 20);
                            continue;
                        case 1, 2:
                            g.setColor(Color.magenta);
                            g.fillRect(e.x, e.y, 20, 20);
                            continue;
                        case 3, 4:
                            g.setColor(Color.blue);
                            g.fillRect(e.x, e.y, 20, 20);
                            continue;
                    }
                }
            }
        }
    }

    void DrawBunkers(Graphics2D g){
        for(int i = 0; i < 4; i++){
            Bunker b = bunkers[i];
            if(b.lives != 0){
                g.setColor(Color.darkGray);
                g.fillRect(b.x, b.y + (60-(b.lives * 15)), 60, b.lives * 15);
            }
        }
    }

    void DrawSaucer(Graphics2D g){
        if(saucer.active){
            g.setColor(Color.cyan);
            g.fillRect(saucer.x, saucer.y, 30, 20);
        }
    }

    void DrawScore(Graphics2D g){
        g.setColor(Color.black);
        g.setFont(smallFont);
        g.drawString("SCORE: " + score, 10, 435);
    }

    void DrawLives(Graphics2D g){
        g.setColor(Color.black);
        g.setFont(smallFont);
        g.drawString("LIVES: " + lives, 550, 435);
    }

    @Override
    public void paintComponent(Graphics gg){
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if(gameOver){
            DrawStartScreen(g);
        } else{
            DrawPlayer(g);
            DrawLaser(g);
            DrawEnemies(g);
            DrawEnemyLaser(g);
            DrawBunkers(g);
            DrawSaucer(g);
            DrawScore(g);
            DrawLives(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    JFrame mainFrame = new JFrame();
                    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    mainFrame.setTitle("GAME");
                    mainFrame.setResizable(true);
                    mainFrame.add(new Main(), BorderLayout.CENTER);
                    mainFrame.pack();
                    mainFrame.setLocationRelativeTo(null);
                    mainFrame.setVisible(true);
                });
    }
}