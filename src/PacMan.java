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
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

import static java.lang.Math.abs;

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
                    return;
                }
            }
            this.x -= this.velocityX;
            this.y -= this.velocityY;
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

    class AStarNode implements Comparable<AStarNode> {
        Point point;
        int g;
        int h;
        int f;
        Point parent;

        AStarNode(Point point, int g, int h, Point parent) {
            this.point = point;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.parent = parent;
        }

        @Override
        public int compareTo(AStarNode o) {
            if (this.f > o.f) { return 1; }
            if (this.f < o.f) { return -1; }
            return 0;
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
    char queuedDirection = 'R'; // I added this to make movement cleaner and like the real game

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
            ghost.updateDirection('U');
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
                    pacman.direction = 'R';
                    pacman.updateVelocity();
                    queuedDirection = 'R';
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

    private boolean canChangeDirection(char direction) {
        int speed = tileSize / 4;
        int nextX = pacman.x;
        int nextY = pacman.y;

        if (direction == 'U') nextY -= speed;
        else if (direction == 'D') nextY += speed;
        else if (direction == 'L') nextX -= speed;
        else if (direction == 'R') nextX += speed;

        Block nextBlock = new Block(pacman.image, nextX, nextY, pacman.width, pacman.height);

        for (Block wall : walls) {
            if (collision(nextBlock, wall)) {
                return false;
            }
        }

        return true;
    }

    private void movePacman() {
        // i added this bit of logic for the queueing directions
        if (queuedDirection != pacman.direction && canChangeDirection(queuedDirection)) {
            pacman.updateDirection(queuedDirection);
            pacman.updateVelocity();
            updatePacmanImage();
        }

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
        ghost.x += ghost.velocityX;
        ghost.y += ghost.velocityY;
        checkWallCollision(ghost);
    }

    // blinky will use BFS to chase pacman
    private void moveBlinky(Block ghost) {
        //pick direction when blinky is on a tile
        if (isAtTile(ghost) && isAtIntersection(ghost)) {
            int ghostRow = getRow(ghost);
            int ghostCol = getCol(ghost);
            int pacmanRow = getRow(pacman);
            int pacmanCol = getCol(pacman);

            char nextDirection = bfsNextDirection(ghostRow, ghostCol, pacmanRow, pacmanCol, pacman.direction);
            ghost.updateDirection(nextDirection);
        }
        moveGhost(ghost);
    }

    // pinky will use A* later
    private void movePinky(Block ghost) {
        // update path after every tile full tile change for ghost
        if (isAtTile(ghost) && isAtIntersection(ghost)) {
            char nextDirection = aStarDirection(ghost, pacman);
            ghost.updateDirection(nextDirection);
        }
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

    // check if ghost is at an intersection, got some help from chatgpt for how to specifically figure this out because intersections are more like decision points so there are many cases
    private boolean isAtIntersection(Block block) {
        int row = getRow(block);
        int col = getCol(block);

        char dir = block.direction;

        boolean canForward = false;
        boolean canLeft = false;
        boolean canRight = false;

        if (dir == 'U' || dir == 'D') {
            canForward = !isWall(dir == 'U' ? row - 1 : row + 1, col);
            canLeft = !isWall(row, col - 1);
            canRight = !isWall(row, col + 1);
        } else {
            canForward = !isWall(row, dir == 'L' ? col - 1 : col + 1);
            canLeft = !isWall(row - 1, col);
            canRight = !isWall(row + 1, col);
        }

        // Case 1: can't go forward → must decide
        if (!canForward) return true;

        // Case 2: can go forward but also have options
        if (canLeft || canRight) return true;

        return false;
    }


    // gives all possible tiles we can move to from a given position (need for bfs, a*, and hybrid)
    private ArrayList<Point> getNeighbors(int row, int col) {

        ArrayList<Point> neighbors = new ArrayList<Point>();

        // check up
        if (!isWall(row - 1, col)) {
            neighbors.add(new Point(col, row - 1));
        }

        // check down
        if (!isWall(row + 1, col)) {
            neighbors.add(new Point(col, row + 1));
        }

        // check left
        if (!isWall(row, col - 1)) {
            neighbors.add(new Point(col - 1, row));
        }

        // check right
        if (!isWall(row, col + 1)) {
            neighbors.add(new Point(col + 1, row));
        }

        return neighbors;
    }


    // BFS method that blinky uses. this tells it the 1st direction it should go.
    private char bfsNextDirection(int startRow, int startCol, int targetRow, int targetCol, char currentDirection) {

        // queue used for BFS (fifo)
        Queue<Point> queue = new LinkedList<Point>();

        // keep track of visited tiles so we don’t loop forever
        HashSet<Point> visited = new HashSet<Point>();

        // parent map lets us go back the path later to go to the 2nd, 3rd, ... direction it should go to
        HashMap<Point, Point> parent = new HashMap<Point, Point>();

        // starting position of the ghost
        Point start = new Point(startCol, startRow);

        // target position (where pacman is)
        Point target = new Point(targetCol, targetRow);

        // begin BFS where blinky is
        queue.add(start);
        visited.add(start);

        //BFS loop
        while (!queue.isEmpty()) {

            // move to next tile
            Point current = queue.remove();

            // if we reached pacman, we’re done
            if (current.equals(target)) {
                break;
            }

            // check all possible moves from the current tile blinky is on
            for (Point next : getNeighbors(current.y, current.x)) {

                // if we havent visited
                if (!visited.contains(next)) {
                    visited.add(next);

                    // store how we got there (path back)
                    parent.put(next, current);

                    // add to queue to explore later
                    queue.add(next);
                }
            }
        }

        // if we somehow never reached pacman just go the same direction (left here)
        if (!visited.contains(target)) {
            return currentDirection;
        }

        // trace backward from pacman to the ghost until we get to the second tile after the starting tile
        Point step = target;

        
        while (parent.containsKey(step) && !parent.get(step).equals(start)) {
            step = parent.get(step);
        }

        // convert that step into a direction (up down left right)
        return getDirectionToward(startRow, startCol, step.y, step.x);
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
        pacman.direction = 'R';
        pacman.updateVelocity();
        queuedDirection = 'R';
        updatePacmanImage();
        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.updateDirection('U');
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
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            restartGame();
        } else {
            handleKeyPress(e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

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
            case KeyEvent.VK_UP -> queuedDirection = 'U';
            case KeyEvent.VK_DOWN -> queuedDirection = 'D';
            case KeyEvent.VK_LEFT -> queuedDirection = 'L';
            case KeyEvent.VK_RIGHT -> queuedDirection = 'R';
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

    private char aStarDirection(Block ghost, Block pacman) {
        Point target = getPinkyTarget(pacman);
        Point starting = new Point(getCol(ghost), getRow(ghost));

        PriorityQueue<AStarNode> availableQueue = new PriorityQueue<>();
        HashSet<Point> searched = new HashSet<>();
        HashMap<Point, Point> parentMap = new HashMap<>();
        HashMap<Point, Integer> costMap = new HashMap<>();
        AStarNode toSearch;
        AStarNode toAdd;
        AStarNode targetFound = null;

        availableQueue.add(new AStarNode(starting, 0, getManhattanDistance(starting, target), null));
        costMap.put(starting, 0);
        while (!availableQueue.isEmpty()) {
            toSearch = availableQueue.remove();
            if (searched.contains(toSearch.point)) {
                continue;
            }

            if (toSearch.point.equals(target)) {
                targetFound = toSearch;
                break;
            }

            searched.add(toSearch.point);

            for (Point neighbor : getNeighbors(toSearch.point.y, toSearch.point.x)) {
                if (searched.contains(neighbor)) { continue; }
                if (costMap.get(neighbor) == null || costMap.get(neighbor) > toSearch.g + 1) {
                    costMap.put(neighbor, toSearch.g + 1);
                    parentMap.put(neighbor, toSearch.point);
                    toAdd = new AStarNode(neighbor, toSearch.g + 1, getManhattanDistance(neighbor, target), toSearch.point);
                    availableQueue.add(toAdd);
                }
            }

        }

        if (targetFound != null) {
            ArrayList<Point> path = new ArrayList<>();
            path.add(targetFound.point);
            while (!path.getFirst().equals(starting)) {
                path.addFirst(parentMap.get(path.getFirst()));
            }

            if (path.size() == 1) { return ghost.direction; }
            return getDirectionToward(starting.y, starting.x, path.get(1).y, path.get(1).x);
        }

        return ghost.direction;
    }

    private int getManhattanDistance(Point first, Point second) {
        return abs(first.x - second.x) + abs(first.y - second.y);
    }

    private Point getPinkyTarget(Block pacman) {
        int targetRow = getRow(pacman);
        int targetCol = getCol(pacman);

        for (int i = 0; i < 2; i++) {
            int nextRow = targetRow;
            int nextCol = targetCol;

            if (pacman.direction == 'U') {
                nextRow--;
            } else if (pacman.direction == 'D') {
                nextRow++;
            } else if (pacman.direction == 'L') {
                nextCol--;
            } else if (pacman.direction == 'R') {
                nextCol++;
            }

            if (isWall(nextRow, nextCol)) {
                break;
            }

            targetRow = nextRow;
            targetCol = nextCol;
        }

        return new Point(targetCol, targetRow);
    }
}
