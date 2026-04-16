/*
 * Megan Dowdell and Jacob Igielski
 * CSC301
 * Program 3 Pacman
 *
 * This project is based on the Java Pac-Man game from:
 * https://github.com/Nipuna-Lakruwan/PacMan-Java-Game
 *
 * The ghost behavior was modified for this project using ideas from:
 * "Exploring the Possibilities of MADDPG for UAV Swarm Control by Simulating in Pac-Man Environment"
 * Novikov, Yakovlev, and Gushchin, 2025.
 *
 * For this assignment, Blinky uses BFS, Pinky uses A*, Inky uses a hybrid
 * of the two, and Clyde uses a simple random chase behavior.
 */

import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if (this.direction == 'L') {
                this.velocityX = -tileSize/4;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = tileSize/4;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo       O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;

    PacMan() {
        initializeGame();
    }

    private void initializeGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        loadImages();
        loadMap();
        initializeGhosts();
        startGameLoop();
    }

    private void loadImages() {
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();
        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();
    }

    private void initializeGhosts() {
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    private void startGameLoop() {
        gameLoop = new Timer(50, this); // 20fps (1000/50)
        gameLoop.start();
    }

    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c*tileSize;
                int y = r*tileSize;

                if (tileMapChar == 'X') { //block wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b') { //blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') { //pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') { //red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'P') { //pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                }
                else if (tileMapChar == ' ') { //food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        drawPacman(g);
        drawGhosts(g);
        drawWalls(g);
        drawFoods(g);
        drawScore(g);
    }

    private void drawPacman(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);
    }

    private void drawGhosts(Graphics g) {
        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }
    }

    private void drawWalls(Graphics g) {
        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }
    }

    private void drawFoods(Graphics g) {
        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }
    }

    private void drawScore(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.drawString("Game Over: " + score, tileSize / 2, tileSize / 2);
        } else {
            g.drawString("x" + lives + " Score: " + score, tileSize / 2, tileSize / 2);
        }
    }

    public void move() {
        movePacman();
        moveGhosts();
        checkFoodCollision();
        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    private void movePacman() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        checkWallCollision(pacman);
    }

    // update all ghosts each frame and give each one its own behavior
    private void moveGhosts() {
        for (Block ghost : ghosts) {
            // check collision with pacman
            if (collision(ghost, pacman)) {
                handleGhostCollision();
                return;
            }

            // pick behavior based on ghost color
            if (ghost.image == redGhostImage) {
                moveBlinky(ghost);
            }
            else if (ghost.image == pinkGhostImage) {
                movePinky(ghost);
            }
            else if (ghost.image == blueGhostImage) {
                moveInky(ghost);
            }
            else if (ghost.image == orangeGhostImage) {
                moveClyde(ghost);
            }
        }
    }

    private void moveGhost(Block ghost) {
        if (ghost.y == tileSize * 9 && ghost.direction != 'U' && ghost.direction != 'D') {
            ghost.updateDirection('U');
        }
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    // blinky will use BFS to chase pacman
    private void moveBlinky(Block ghost) {
        //pick direction when blinky is on a tile
        if (isAtTile(ghost) && isAtTile(pacman)) {
            int ghostRow = getRow(ghost);
            int ghostCol = getCol(ghost);
            int pacmanRow = getRow(pacman);
            int pacmanCol = getCol(pacman);

            char nextDirection = bfsNextDirection(ghostRow, ghostCol, pacmanRow, pacmanCol);
            ghost.updateDirection(nextDirection);
        }
    }

    // pinky will use A* later
    private void movePinky(Block ghost) {
        moveGhost(ghost);
    }

    // inky will combine blinky and pinky logic
    private void moveInky(Block ghost) {
        moveGhost(ghost);
    }

    // clyde will have simple or random behavior
    private void moveClyde(Block ghost) {
        moveGhost(ghost);
    }

    // convert pixel position to grid row
    private int getRow(Block b) {
        return b.y / tileSize;
    }

    // convert pixel position to grid column
    private int getCol(Block b) {
        return b.x / tileSize;
    }

    // check if a tile is a wall (or out of bounds)
    private boolean isWall(int row, int col) {
        if (row < 0 || row >= rowCount || col < 0 || col >= columnCount) {
            return true;
        }

        return tileMap[row].charAt(col) == 'X';
    }

    // check if ghost is exactly on a tile (not between tiles)
    private boolean isAtTile(Block b) {
        return b.x % tileSize == 0 && b.y % tileSize == 0;
    }

    // figure out which direction to move from one tile to another
    private char getDirectionToward(int fromRow, int fromCol, int toRow, int toCol) {
        if (toRow == fromRow - 1 && toCol == fromCol) return 'U';
        if (toRow == fromRow + 1 && toCol == fromCol) return 'D';
        if (toRow == fromRow && toCol == fromCol - 1) return 'L';
        if (toRow == fromRow && toCol == fromCol + 1) return 'R';

        // backup if something weird happens
        return 'L';
    }

    private void checkWallCollision(Block block) {
        for (Block wall : walls) {
            if (collision(block, wall) || block.x <= 0 || block.x + block.width >= boardWidth) {
                block.x -= block.velocityX;
                block.y -= block.velocityY;
                if (block != pacman) {
                    char newDirection = directions[random.nextInt(4)];
                    block.updateDirection(newDirection);
                }
                break;
            }
        }
    }

    private void handleGhostCollision() {
        lives -= 1;
        if (lives == 0) {
            gameOver = true;
        } else {
            resetPositions();
        }
    }

    private void checkFoodCollision() {
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
            }
        }
        foods.remove(foodEaten);
    }

    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            restartGame();
        } else {
            handleKeyPress(e);
        }
    }

    private void restartGame() {
        loadMap();
        resetPositions();
        lives = 3;
        score = 0;
        gameOver = false;
        gameLoop.start();
    }

    private void handleKeyPress(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> pacman.updateDirection('U');
            case KeyEvent.VK_DOWN -> pacman.updateDirection('D');
            case KeyEvent.VK_LEFT -> pacman.updateDirection('L');
            case KeyEvent.VK_RIGHT -> pacman.updateDirection('R');
        }
        updatePacmanImage();
    }

    private void updatePacmanImage() {
        switch (pacman.direction) {
            case 'U' -> pacman.image = pacmanUpImage;
            case 'D' -> pacman.image = pacmanDownImage;
            case 'L' -> pacman.image = pacmanLeftImage;
            case 'R' -> pacman.image = pacmanRightImage;
        }
    }
}
