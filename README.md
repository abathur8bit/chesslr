# ChessLR
Electronic chess board project <https://www.8bitcoder.com/chesslr>

ChessLR is will be an chess e-Board and Chess Computer that can sense and record the movement of chess pieces. 

# Features

- Record games between two players.
- Play against a computer, and record the game.
- Recorded games can be downloaded. 
- Moves by the computer are indicated on the board using LEDs.
- Shows when you make a move that isn't legal.
- Setup any board position.
 

# How it works
There will be sensors under the board, as well as on the pieces. In the current design, reed switches will be under each square and magnets under the pieces. Reed switches are sensitive to magnetic fields that the magnets generate, and will activate when a piece is over it. This doesn't allow for piece identification, so it doens't know if a knight or a queen is over the square. By using a little bit of clever software, it should be fairly easy to keep track of what piece is where, and what piece moved. 

# Hardware
As the project evolves, the schematics will be updated. At present the board will be utilizing:

- Raspberry Pi for control logic and chess engine.
- LEDs for each square.
- Reed switches to sense pieces.
- Magnets under the pieces.
- MCP23017 I2C port expander to allow 64 inputs, and 64 outputs.

# Software
The driver for the board will be written in Java and meant to be run on a Raspberry Pi. It will utilize read reed switch state information, turn LEDs on and off, and talk with the chess engine. Software will also need to implement chess game logic, such as if a move is legal. I was surprised to learn that a chess engine doesn't acutally do that.

[Stockfish](https://stockfishchess.org) is the chess engine of choise. It's an open source chess engine written in C. It communicates via the Universal Chess Interface (UCI). ChessLR will send moves to Stockfish and when Stockfish sends it move back, the move will be indicated on the chess board via the LEDs.


# Resources
[PI4J](https://pi4j.com/1.2/index.html) Java I/O library for Raspberry PI.

