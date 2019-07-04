# ChessLR
Electronic chess board project <https://www.8bitcoder.com/chesslr>

ChessLR will be a chessboard that can sense and record the moves of a game, the time between moves, and the game time using a tournament style clock. Software and schematic will be open source.

There are electronic chessboards on the market that will do what we want, however they are prohibitively expensive, and typically require a wooden board and pieces that are specialized. Cost is usually around $800-1000 for the board, pieces and clock. Of the ones I have seen, none have very nice looking pieces.

This will be a bit of a marriage of board hardware and software running on a Raspberry Pi. 



# Resources
[PI4J](https://pi4j.com/1.2/index.html) Java I/O library for Raspberry PI.

# MCP23017

Chip pinout.

```
          +----- -----+
GPB0 <--> |*1   -   28| <--> GPA7
GPB1 <--> |2        27| <--> GPA6
GPB2 <--> |3        26| <--> GPA5
GPB3 <--> |4    M   25| <--> GPA4
GPB4 <--> |5    C   24| <--> GPA3
GPB5 <--> |6    P   23| <--> GPA2
GPB6 <--> |7    2   22| <--> GPA1
GPB7 <--> |8    3   21| <--> GPA0
 VDD ---> |9    0   20| ---> INTA
 VSS ---> |10   1   19| ---> INTB
  NC ---- |11   7   18| ---> RESET
 SCL ---> |13       17| <--- A2
 SDA <--> |13       16| <--- A1
  NC ---- |14       15| <--- A0
          +-----------+
```
