package com.rahul.stockfish;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StockfishTest {
    long startTime,endTime;
    String stockfishPath = "../stockfish/cmake-build-debug/stockfish";
    String fen = "2R5/1R6/5p1k/5P2/P5pP/6P1/8/6K1 w - - 1 44";
    // +---+---+---+---+---+---+---+---+
    // |   |   | R |   |   |   |   |   |
    // +---+---+---+---+---+---+---+---+
    // |   | R |   |   |   |   |   |   |
    // +---+---+---+---+---+---+---+---+
    // |   |   |   |   |   | p |   | k |
    // +---+---+---+---+---+---+---+---+
    // |   |   |   |   |   | P |   |   |
    // +---+---+---+---+---+---+---+---+
    // | P |   |   |   |   |   | p | P |
    // +---+---+---+---+---+---+---+---+
    // |   |   |   |   |   |   | P |   |
    // +---+---+---+---+---+---+---+---+
    // |   |   |   |   |   |   |   |   |
    // +---+---+---+---+---+---+---+---+
    // |   |   |   |   |   |   | K |   |
    // +---+---+---+---+---+---+---+---+
    Stockfish fish;

    @Before
    public void setUp() throws Exception {
        fish = new Stockfish();
        if(!fish.startEngine(stockfishPath)) {
            System.out.println("Unable to start stockfish");
        }
        startTime = System.currentTimeMillis();
    }

    @After
    public void tearDown() throws Exception {
        endTime = System.currentTimeMillis();
        fish.stopEngine();
    }

    @Test
    public void sendCommand() {
        fish.sendCommand("uci");
        String output = fish.getOutput(0);
        System.out.println("Output: "+output);
    }

    @Test
    public void drawBoard() {
        fish.drawBoard(fen);
    }

    @Test
    public void getBestMove() {
        String move = fish.getBestMove(fen,1000);
        System.out.println("Best move = ["+move+"]");
    }
}