# PacMan Java Game

A classic PacMan game implemented in Java using `JFrame` for the graphical user interface. This game features the main character, PacMan, navigating through a maze, collecting food, avoiding ghosts, and aiming to achieve the highest score possible.

## Features

- **PacMan Movement:** Use arrow keys (Up, Down, Left, Right) to control PacMan.
- **Ghosts:** Four types of ghosts with different behaviors.
- **Map Layout:** A 21x19 grid with walls, food, and ghosts.
- **Score System:** Collect food to earn points. Lives decrease if PacMan collides with a ghost.
- **Game Over:** The game ends when PacMan loses all lives.

## Getting Started

### Prerequisites

Make sure you have Java installed on your system. You can download the latest version of Java from [here](https://www.oracle.com/java/technologies/javase-downloads.html).

### Running the Game

1. Clone this repository:

   ```bash
   git clone https://github.com/Nipuna-Lakruwan/pacman-java.git
   ```

2. Navigate to the project directory:

   ```bash
   cd pacman-java
   ```

3. Compile and run the game:

   ```bash
   javac App.java
   java App
   ```

## How It Works

- **Game Window:** The game uses a `JFrame` to create the window and a custom `JPanel` ([`PacMan`](src/PacMan.java)) to render the game board.
- **Game State:** The game state is stored in a 2D grid (`tileMap`), where different characters represent different elements:
  - 'X' for walls
  - ' ' for food
  - 'P' for PacMan
  - 'b', 'o', 'p', 'r' for different colored ghosts
- **Game Loop:** The game loop is driven by a `Timer` that updates the positions of PacMan and the ghosts, checks for collisions, and redraws the screen at a fixed rate.

## Code Breakdown

### [App.java](src/App.java)

This is the entry point of the game. It creates a `JFrame`, sets its size based on the grid dimensions, and adds the PacMan panel to the frame.

```java
public class App {
    public static void main(String[] args) throws Exception {
        // Set up game board dimensions and create a JFrame window.
        int rowCount = 21;
        int columnCount = 19;
        int tileSize = 32;
        int boardWidth = columnCount * tileSize;
        int boardHeight = rowCount * tileSize;

        JFrame frame = new JFrame("Pac Man");
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the PacMan game panel and add it to the frame.
        PacMan pacmanGame = new PacMan();
        frame.add(pacmanGame);
        frame.pack();
        pacmanGame.requestFocus();
        frame.setVisible(true);
    }
}
```

### [PacMan.java](src/PacMan.java)

This class defines the game logic, including PacMan’s movement, collision detection, and ghost AI.

- **Block Class:** Represents game objects like walls, ghosts, food, and PacMan itself. Each Block has properties like position (x, y), dimensions (width, height), and a direction (U, D, L, R).
- **Movement and Collisions:** The `move()` method updates PacMan’s and the ghosts’ positions and checks for collisions with walls, ghosts, and food.
- **Graphics Rendering:** The `paintComponent()` method is responsible for rendering the game board, drawing walls, food, and characters on the screen.

```java
public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        // Defines the properties of a game object (e.g., PacMan, ghosts, walls)
    }

    // Game board setup and initialization
    private void loadMap() {
        // Loads the map tiles (walls, food, PacMan, and ghosts) into the game.
    }

    // Game loop logic and collision detection
    private void move() {
        // Updates PacMan and ghosts' positions, handles collisions and scoring.
    }

    // Handles key events to control PacMan's movement.
    @Override
    public void keyReleased(KeyEvent e) {
        // Detects key events and updates PacMan's direction accordingly.
    }
}
```

## Game Map (tileMap)

The `tileMap` defines the layout of the game world. Each string in the array represents a row of the map:
- 'X': Wall block
- ' ': Empty space, where PacMan can move
- 'P': PacMan’s starting position
- 'b', 'o', 'p', 'r': Different colored ghosts
- ' ': Food for PacMan to collect

## Contributing

Feel free to fork this repository and make your contributions. If you’d like to suggest improvements or fix bugs, submit a pull request with a detailed description of the changes.

## License

This project is open-source and available under the MIT License.

Happy gaming!