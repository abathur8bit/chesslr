package com.rahul.stockfish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * A simple and efficient client to run Stockfish from Java
 *
 * @author Rahul A R
 */
public class Stockfish {

    private Process engineProcess;
    private BufferedReader processReader;
    private OutputStreamWriter processWriter;

    private static final String PATH = "/home/pi/workspace/stockfish/src/stockfish";
    private String stockfishPath = PATH;

    private int skillLevel = 0;
    private int slowMover = 10;
    private int moveTime = 0;

    public boolean startEngine(String path) {
        stockfishPath = path;
        return startEngine();
    }

    /**
     * Starts Stockfish engine as a process and initializes it
     *
     * @return True on success. False otherwise
     */
    public boolean startEngine() {
        try {
            engineProcess = Runtime.getRuntime().exec(stockfishPath);
            processReader = new BufferedReader(new InputStreamReader(
                    engineProcess.getInputStream()));
            processWriter = new OutputStreamWriter(
                    engineProcess.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }

    public int getSlowMover() {
        return slowMover;
    }

    public void setSlowMover(int slowMover) {
        this.slowMover = slowMover;
    }

    public int getMoveTime() {
        return moveTime;
    }

    public void setMoveTime(int moveTime) {
        this.moveTime = moveTime;
    }

    /**
     * Takes in any valid UCI command and executes it
     *
     * @param command
     */
    public void sendCommand(String command) throws IOException {
        System.out.println("Sending command ["+command+"]");
        processWriter.write(command + "\n");
        processWriter.flush();
    }

    /**
     * This is generally called right after 'sendCommand' for getting the raw
     * output from Stockfish
     *
     * @param waitTime
     *            Time in milliseconds for which the function waits before
     *            reading the output. Useful when a long running command is
     *            executed
     * @return Raw output from Stockfish
     */
    public String getOutput(int waitTime) throws IOException {
        StringBuffer buffer = new StringBuffer();
            sleep(waitTime);
            sendCommand("isready");
            while (true) {
                String text = processReader.readLine();
                if (text.equals("readyok"))
                    break;
                else
                    buffer.append(text + "\n");
            }
        return buffer.toString();
    }

    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            //do nothing
        }
    }

    /**
     * This function returns the best move for a given position after
     * calculating for 'waitTime' ms.
     *
     * If there is an error when sending the command(s) the engine will be
     * restarted, and commands sent again. If there is a second error, or
     * the engine didn't initialize, exception will be passed back to the
     * caller.
     *
     * @param fen
     *            Position string
     * @param waitTime
     *            in milliseconds
     * @return Best Move in PGN format
     */
    public String getBestMove(String fen, int waitTime) throws IOException {
        String output = null;
        try {
            output = sendGetBestMove(fen,waitTime);
        } catch(IOException e) {
            System.out.println("ERROR got IO Error from Stockfish, trying 1 more time. Stacktrace:");
            e.printStackTrace();
            snore(1000);
            closeIgnoreException();     //close the existing process, ignore errors since Stockfish is already dead
            if(startEngine() == true) { //try to restart the engine
                output = sendGetBestMove(fen,waitTime);  //engine restarted, resend the commands
            } else {
                throw(e);                       //unable to restart
            }
        }
        return output;
    }

    /** Send the best move commands. Sets the options and fen position before getting the move. */
    public String sendGetBestMove(String fen,int waitTime) throws IOException {
        sendCommand("setoption name Skill Level value "+skillLevel);
        sendCommand("setoption name Slow Mover value "+slowMover);
        sendCommand("position fen " + fen);
        if(moveTime == 0)
            sendCommand("go");
//            sendCommand("go depth 1");
        else
            sendCommand("go movetime " + moveTime);
//            sendCommand("go movetime " + moveTime+" depth 1");
        String output = getOutput(waitTime+20);
        System.out.println("Got result\n"+output);
        return output.split("bestmove ")[1].split(" ")[0].substring(0,4);
    }

    /** Sleep and if interrupted ignore exception. */
    public void snore(long ms) {
        try {
            Thread.sleep(ms);
        } catch(InterruptedException e) {
            //ignore
        }
    }

    /**
     * Stops Stockfish and cleans up before closing it
     */
    public void stopEngine() {
        try {
            sendCommand("quit");
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        processReader.close();
        processWriter.close();
    }

    public void closeIgnoreException() {
        try {
            close();
        } catch(IOException e) {
            //ignore
        }
    }

//    /**
//     * Get a list of all legal moves from the given position
//     *
//     * @param fen
//     *            Position string
//     * @return String of moves
//     */
//    public String getLegalMoves(String fen) {
//        sendCommand("position fen " + fen);
//        sendCommand("d");
//        return getOutput(0).split("Legal moves: ")[1];
//    }

    /**
     * Draws the current state of the chess board
     *
     * @param fen
     *            Position string
     */
    public void drawBoard(String fen) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("d");

        String[] rows = getOutput(0).split("\n");

        for (int i = 1; i < 21; i++) {
            System.out.println(rows[i]);
        }
    }

    /**
     * Get the evaluation score of a given board position
     * @param fen Position string
     * @param waitTime in milliseconds
     * @return evalScore
     */
    public float getEvalScore(String fen, int waitTime) throws IOException {
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + waitTime);

        float evalScore = 0.0f;
        String[] dump = getOutput(waitTime + 20).split("\n");
        System.out.println("Dump length="+dump.length);
        for (int i = dump.length - 1; i >= 0; i--) {
            System.out.println(dump[i]);
            if (dump[i].startsWith("info depth ")) {
                try {
                    evalScore = Float.parseFloat(dump[i].split("score cp ")[1]
                            .split(" nodes")[0]);
                } catch(Exception e) {
                    evalScore = Float.parseFloat(dump[i].split("score cp ")[1]
                            .split(" upperbound nodes")[0]);
                }
            }
        }
        return evalScore/100;
    }
}