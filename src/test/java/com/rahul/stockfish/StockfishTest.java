package com.rahul.stockfish;

import com.axorion.chesslr.ChessLR;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class StockfishTest {
    long startTime,endTime;
    String stockfishPath = ChessLR.stockfishPath;
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
    @Ignore
    public void testShredder() throws Exception {
//        String shredder = "~/Desktop/DeepShredder12Mac";
        String shredder = "/Applications/Shredder/DeepShredder12.app/Contents/Resources/Java/engines/DeepShredder12Mac";
        Stockfish b = new Stockfish();
        boolean started = b.startEngine(shredder);
        assertTrue(started);
        b.sendCommand("uci");
        String out = b.getOutput(1000);
        System.out.println("Shredder: "+out);
//        String fen = "rn1qkb1r/1b1p3p/p3pp1n/1p4p1/3P4/1P1B4/PBPN1PPP/RN1QR1K1 b kq - 1 10";
//        b.sendCommand("position fen "+fen);
//        b.sendCommand("go movetime 1000");
//        out = b.getOutput(1500);
//        System.out.println("move output="+out);

        String move = b.getBestMove(fen,1000);
        System.out.println("move="+move);
//        float score = b.getEvalScore("rn1qkb1r/1b1p3p/p3pp1n/1p4p1/3P4/1P1B4/PBPN1PPP/RN1QR1K1 b kq - 1 10",1000);
//        System.out.println("score = "+score);
    }
    @Test
    @Ignore
    public void regex() throws Exception {
        String fen = "4r3/P4ppk/3R3p/8/8/2p2P1P/3p1KP1/8 w - - 5 39";
        fish.sendCommand("position fen "+fen);
        fish.sendCommand("go movetime 1000");
        String[] output = fish.getOutput(2000).split("\n");
        for(String s : output) {
            System.out.println(s);
            String[] split = s.split("score cp ");
        }
    }

    @Test
    @Ignore
    public void score() throws Exception {
//        String fen = "Q3r3/5ppk/R6p/8/8/2p2P1P/5KP1/3q4 b - - 0 40";
        String fen = "4r3/P4ppk/3R3p/8/8/2p2P1P/3p1KP1/8 w - - 5 39";
        float score = fish.getEvalScore(fen,1000);
        System.out.println("score = "+score);
    }
    @Test
    @Ignore
    public void sendCommand() throws Exception {
        fish.sendCommand("uci");
        String output = fish.getOutput(0);
        System.out.println("Output: "+output);
    }

    @Test
    @Ignore
    public void drawBoard() throws Exception  {
        fish.drawBoard(fen);
    }

    @Test
    @Ignore
    public void getBestMove() throws Exception  {
        String move = fish.getBestMove(fen,1000);
        System.out.println("Best move = ["+move+"]");
    }
}