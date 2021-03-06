/* *****************************************************************************
 * Copyright 2018 Lee Patterson <https://github.com/abathur8bit>
 *
 * You may use and modify at will. Please credit me in the source.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ******************************************************************************/
/* *****************************************************************************
 * Copyright 2018 Lee Patterson <https://github.com/abathur8bit>
 *
 * You may use and modify at will. Please credit me in the source.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ******************************************************************************/

package com.axorion.chesslr;

import com.axorion.chess.ChessBoard;
import com.axorion.chess.ChessMove;
import com.axorion.chesslr.hardware.BoardController;
import com.axorion.chesslr.hardware.PieceListener;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.rahul.stockfish.Stockfish;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;

import static java.awt.Image.SCALE_SMOOTH;

//import com.github.bhlangonijr.chesslib.Board;

//import oled.OLEDDisplay;

/**
 * @author Lee Patterson
 */
public class AppFrame extends JFrame implements InvocationHandler,PieceListener {
    enum GameMode {
        PLAYING,                //normal waiting for any piece up and down for a move
        WAIT_FOR_PIECE_UP,      //waiting for a specific square to be picked up
        WAIT_FOR_PIECE_DOWN,    //waiting for a specific square to get a piece
        SHOW_PIECES,            //showing all pieces that are detected on the board
        SHOW_LAYOUT,            //show where pieces should be placed on e-board
    };

    GpioController gpio;
    BoardController chessBoardController;
//    OLEDDisplay display;

    String whitePieceLetters = "PNBRQK";
    String blackPieceLetters = "pnbrqk";
    BoardPanel board;
    ChessBoard chessBoard = new ChessBoard();
//    Board boardValidator = new Board();
    Integer pieceUpIndex = -1;      //the square that a piece was lifted from
    Integer pieceDownIndex = -1;    //the square that a piece was dropped onto
    Integer secondPieceUpIndex = -1;
    JFileChooser fileChooser;   //used by windows
    FileDialog fileDialog;      //used by mac
    boolean isMac = false;
    ChessPrefs prefs;
    SettingsDialog settingsDialog;
//    OptionsDialog optionsDialog;
//    HelpDialog helpDialog;
    AboutDialog aboutDialog;
    MediaTracker mediaTracker;
    Color[] colors = {
            Color.green,
            Color.yellow,
            Color.blue,
            Color.red,
            Color.white,
            Color.cyan,
            Color.magenta,
            Color.orange,
    };
    Color buttonDefaultColor = null;
    Color buttonSelectedColor = Color.gray;
    int colorIndex=0;
    boolean isShowingPieces = false;
    MoveThread moveThread;
    boolean boardAttached;
    ChessMove waitForMove = null;
    ArrayList<ChessMove> waitForMoveList = new ArrayList<ChessMove>();
    GameMode mode = GameMode.PLAYING;
    GameMode previousMode = null;

    int numberPlayers = 2;
    ChessBoard.Side playerSide = ChessBoard.Side.WHITE;

    SimBoard simBoard;
    Stockfish fish = new Stockfish();
    String stockfishPath = "../stockfish/cmake-build-debug/stockfish";

    public AppFrame(String title,boolean boardAttached,long waitTime) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, IOException, I2CFactory.UnsupportedBusNumberException {
        super(title);
        moveThread = new MoveThread(this,waitTime);
        this.boardAttached = boardAttached;
        prefs = new ChessPrefs(this);
        prefs.loadPrefs();
        chessBoard.setGameId(prefs.gameId);
        numberPlayers = prefs.getPlayers();

        mediaTracker = new MediaTracker(this);
        String lcOSName = System.getProperty("os.name").toLowerCase();
        isMac = lcOSName.startsWith("mac os x");
        if(isMac) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();

        fileDialog = new FileDialog(this);

        fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new OpenFileFilter("csv","Comma Separated") );
        fileChooser.addChoosableFileFilter(new OpenFileFilter("txt","Tab Separated") );

        if(isMac) {
            helpMenu.remove(aboutMenuItem); //no about shown in the help menu, shows under app name menu at left of screen
            fileMenu.remove(optionsMenuItem);

            try {
                Class quitHandlerClass = Class.forName("com.apple.mrj.MRJQuitHandler");
                Class aboutHandlerClass = Class.forName("com.apple.mrj.MRJAboutHandler");
                Class prefHandlerClass = Class.forName("com.apple.mrj.MRJPrefsHandler");

                Class mrjapputilsClass = Class.forName("com.apple.mrj.MRJApplicationUtils");
                Object methodHandler = Proxy.newProxyInstance(quitHandlerClass.getClassLoader(),new Class[] {quitHandlerClass,aboutHandlerClass,prefHandlerClass},this);

                Method appUtilsObj = mrjapputilsClass.getMethod("registerQuitHandler",new Class[] {quitHandlerClass});
                appUtilsObj.invoke(null,new Object[] {methodHandler});

                appUtilsObj = mrjapputilsClass.getMethod("registerAboutHandler",new Class[] {aboutHandlerClass});
                appUtilsObj.invoke(null,new Object[] {methodHandler});

                appUtilsObj = mrjapputilsClass.getMethod("registerPrefsHandler",new Class[] {prefHandlerClass});
                appUtilsObj.invoke(null,new Object[] {methodHandler});

            } catch(Exception e) {
                ChessLR.handleError("Error during application initialization",e);
            }
        }

        board = new BoardPanel(this);
        getContentPane().add(board,BorderLayout.NORTH);
        movesAutoScroll();
        pack();

        setButtonImage(backButton,"button-back.png");
        setButtonImage(fastBackButton,"button-fastback.png");
        setButtonImage(forwardButton,"button-forward.png");
        setButtonImage(fastForwardButton,"button-fastforward.png");
        setButtonImage(showPieces,"button-show.png");
        setButtonImage(settingsButton,"button-settings.png");
        setButtonImage(layoutButton,null);

        buttonPanel.remove(fastBackButton);
        buttonPanel.remove(fastForwardButton);


        if(boardAttached) {
            gpio = GpioFactory.getInstance();
            initHardware();
//            chessBoardController.set3x3Maps();
        } else {
            simBoard = new SimBoard(this);
            chessBoardController = new BoardController(simBoard,simBoard);
            chessBoardController.addListener(this);
        }
        moveThread.start();

//        resetBoard();
        enableButtons();

        if(fish.startEngine(ChessLR.stockfishPath) == false) {
            System.out.println("Unable to start stockfish");
        } else {
            System.out.println("Stockfish ready");
        }

        setMessage("New "+numberPlayers+" player game started, game id "+chessBoard.getGameId());
    }

    /** Start waiting for a specific move to be made.
     * To wait for a move:
     * Light LEDs on board.
     * If a piece is lifted from the wrong square, flash that LED till the piece is put back down.
     * If a piece is dropped on the wrong square, flast that LED till the piece is lefted up.
     * */
    private void addWaitForMove(ChessMove move) {
        waitForMoveList.add(move);
        if(waitForMoveList.size() == 1) {
            if(move != null) {
                setWaitForMove(move);
            }
            repaint();
        }
    }

    private void setWaitForMove(ChessMove move) {
        waitForMove = move;
        if(move != null) {
            mode = GameMode.WAIT_FOR_PIECE_UP;
            chessBoardController.led(move.getFromIndex(),true);
            chessBoardController.led(move.getToIndex(),true);
            board.setSquareStatus(move.getFromIndex(),BoardPanel.SquareStatus.SELECTED);
            board.setSquareStatus(move.getToIndex(),BoardPanel.SquareStatus.SELECTED);
        }
    }

    /** Clears any pending moves, and clears the current wait. */
    private void removeAllWaitForMoves() {
        waitForMoveList.clear();
        waitForMove = null;
    }

    private void removeWaitForMove() {
        if(waitForMoveList.size() > 0) {
            waitForMoveList.remove(0);
            if(waitForMoveList.size()>0) {
                setWaitForMove(waitForMoveList.get(0));
            } else {
                setWaitForMove(null);
                //check if we need to make a computer move
                computerMove();
            }
        }
    }

    private void computerMove() {
        if(numberPlayers == 1 && !prefs.isDisableEngine() && waitForMove == null) {
            if(chessBoard.getCurrentMove() != playerSide) {
                System.out.println("Making computer move");
                try {
                    String cpuEan = fish.getBestMove(chessBoard.toFen(),1000);
                    ChessMove move = new ChessMove(chessBoard,cpuEan);
                    ChessMove finishCastle = null;
                    if(move.isCastleQueenSide() || move.isCastleKingSide()) {
                        if(chessBoard.getCurrentMove() == ChessBoard.Side.WHITE) {
                            if(move.isCastleKingSide()) {
                                finishCastle = new ChessMove(chessBoard,"h1f1");
                            } else {
                                finishCastle = new ChessMove(chessBoard,"a1d1");
                            }
                        } else {
                            if(move.isCastleKingSide()) {
                                finishCastle = new ChessMove(chessBoard,"h8f8");
                            } else {
                                finishCastle = new ChessMove(chessBoard,"a8d8");
                            }
                        }
                    }
                    chessBoard.move(move);
                    addWaitForMove(move);
                    if(finishCastle != null)
                        addWaitForMove(finishCastle);
                } catch(IOException e) {
                    String msg = "Unable to get move from engine";
                    System.out.println(msg);
                    e.printStackTrace();
                    setMessage(msg);
                }
            }
        }
    }
    /** Any text that is added to the moves text area will automatically scroll into view. */
    private void movesAutoScroll() {
        DefaultCaret caret = (DefaultCaret) movesTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void setButtonImage(JButton bn,String iconName) {
        final int size = 50;
        Dimension preferredSize = new Dimension(size,size);
        if(iconName != null) {
            bn.setText("");
            Image im = loadImage(iconName).getScaledInstance((int)(size*0.8),(int)(size*0.8),SCALE_SMOOTH);
            bn.setIcon(new ImageIcon(im));
        }
        bn.setPreferredSize(preferredSize);
    }

    private void initHardware() throws IOException, I2CFactory.UnsupportedBusNumberException {
        final int bus = I2CBus.BUS_1;

        chessBoardController = new BoardController(gpio,bus);
        chessBoardController.addListener(this);
    }

    public String getFilename() {
        return String.format(String.format("games/ChessLR-%05d.txt",chessBoard.getGameId()));
    }
    public String getPgnFilename() {
        return String.format(String.format("games/ChessLR-%05d.pgn",chessBoard.getGameId()));
    }
    public void saveGame() throws IOException {
        File f = new File(getFilename());
        PrintWriter out = new PrintWriter(f);
        out.println("ChessLR Game Id: "+chessBoard.getGameId());
        out.println("Date "+chessBoard.getGameDateFormatted());
        out.println("");
        out.println(chessBoard.getMoveString());
        out.flush();
        out.close();
    }

    public void savePgn() throws IOException {
        File f = new File(getPgnFilename());
        PrintWriter out = new PrintWriter(f);
        out.println("[Event \"Game ID "+chessBoard.getGameId()+"\"]");
        out.println("[Site \"?\"]");
        out.println("[Date \""+chessBoard.getGameDateFormatted()+"\"]");
        out.println("[Round \"?\"]");
        out.println("[White \"?\"]");
        out.println("[Black \"?\"]");
        out.println("[Result \"*\"]");
        out.println("");
        out.println(chessBoard.getMovesPgn());
        out.flush();
        out.close();
    }

//    public void drawRect(OLEDDisplay display,int xpos,int ypos,int width,int height,boolean on) {
//        int y=ypos;
//        int x=xpos;
//        for(x=xpos; x<xpos+width-1; ++x) {
//            display.setPixel(x,y,on);
//        }
//        y=ypos+height-1;
//        for(x=xpos; x<xpos+width-1; ++x) {
//            display.setPixel(x,y,on);
//        }
//
//        x=xpos;
//        for(y=ypos; y<ypos+height-1; ++y) {
//            display.setPixel(x,y,on);
//        }
//        x=xpos+width-1;
//        for(y=ypos; y<ypos+height-1; ++y) {
//            display.setPixel(x,y,on);
//        }
//    }

    public void startApp() throws IOException {
        long tempGameId = chessBoard.getGameId();
        resetBoard();
        chessBoard.setGameId(tempGameId);
        setVisible(true);
        settingsDialog = new SettingsDialog(this);  //construct here, so correct window size and position can be determined.
        SwingUtilities.invokeLater(() -> {
            startup();
        });

        if(!boardAttached) {
            simBoard.setLocation(getX()+getWidth(),getY());
            simBoard.setVisible(true);
        }
    }

    /**
     * Piece was lifted off the board.
     *
     * TODO If we are showing valid moves, light up all squares that piece can be dropped on.
     *
     * @param boardIndex location piece was picked up from.
     */
    public void pieceUp(int boardIndex) {
        String square = chessBoard.indexToBoard(boardIndex);
        switch(mode) {
            case SHOW_PIECES:
                showPieces(true);
                break;

            case PLAYING:
                if(pieceUpIndex == -1) {
                    //only process the first lift, as others are the piece moving around
                    //also if we are waiting for a piece down, we can't start a new move
                    System.out.println("Piece up at "+chessBoard.indexToBoard(boardIndex));
                    pieceUpIndex = boardIndex;
                    chessBoardController.led(boardIndex,true);
                    board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.SELECTED);
                    processMove();
                } else {
                    System.out.println("Picking up second piece at "+chessBoard.indexToBoard(boardIndex));
                    secondPieceUpIndex = boardIndex;
                    chessBoardController.led(boardIndex,true);
                    board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.SELECTED);
                    flashCapturedPiece(pieceUpIndex,secondPieceUpIndex);
                    processMove();
                }
                break;

            case WAIT_FOR_PIECE_UP:
                if(waitForMove.isTakebackMove() && waitForMove.isCapture()) {
                    //from up
                    if(waitForMove.getFrom().equals(square)) {
                        waitForMove.setFromUp(true);
                        mode = GameMode.WAIT_FOR_PIECE_DOWN;
                    } else {
                        showWrongPiece(waitForMove,boardIndex,square);
                    }
                }
                else if(waitForMove.isTakebackMove() && !waitForMove.isCapture()) {
                    //from up
                    if(waitForMove.getFrom().equals(square)) {
                        waitForMove.setFromUp(true);
                        mode = GameMode.WAIT_FOR_PIECE_DOWN;
                    } else {
                        showWrongPiece(waitForMove,boardIndex,square);
                    }
                }
                else if(!waitForMove.isTakebackMove() && waitForMove.isCapture()) {
                    //from and to up
                    if(waitForMove.getFrom().equals(square)) {
                        waitForMove.setFromUp(true);
                    }
                    else if(waitForMove.getTo().equals(square)) {
                        waitForMove.setToUp(true);
                    }
                    else {
                        showWrongPiece(waitForMove,boardIndex,square);
                    }
                    if(waitForMove.isFromUp() && waitForMove.isToUp()) {
                        mode = GameMode.WAIT_FOR_PIECE_DOWN;
                    }
                }
                else if(waitForMove.getFrom().equals(square)) {
                    waitForMove.setFromUp(true);
                    mode = GameMode.WAIT_FOR_PIECE_DOWN;
                }
                else {
                    showWrongPiece(waitForMove,boardIndex,square);
                }
                break;

            case WAIT_FOR_PIECE_DOWN:
                System.out.println("Got piece up while waiting for piece down");
//                SwingUtilities.invokeLater(() -> {
//                    chessBoardController.flashOn(boardIndex);
//                    board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.ERROR);
//                });
//                JOptionPane.showMessageDialog(this,"Replace piece at "+square.toUpperCase()+".\nWaiting for piece at "+waitForPieceDown,"Invalid move",JOptionPane.ERROR_MESSAGE);
//                SwingUtilities.invokeLater(() -> {
//                    chessBoardController.flashOff(boardIndex);
//                    board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.NORMAL);
//                });
                break;

            default:
                System.out.println("Ignoring piece up at "+boardIndex+" for mode "+mode);
                break;
        }
    }

    protected void showWrongPiece(ChessMove expectedMove,int boardIndex,String square) {
        System.out.println("Expecting "+waitForMove.toString());

        SwingUtilities.invokeLater(() -> {
            chessBoardController.flashOn(boardIndex);
            board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.ERROR);
        });

        JOptionPane.showMessageDialog(this,square.toUpperCase()+" is wrong. Expecting "+expectedMove.toEan()+".","Invalid move",JOptionPane.ERROR_MESSAGE);
        SwingUtilities.invokeLater(() -> {
            chessBoardController.flashOff(boardIndex);
            board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.NORMAL);
        });

    }

    /**
     * Piece was put back onto the board at the given boardIndex.
     *
     * TODO If king is in check, flash kings square once or twice.
     * TODO If showing valid moves, turn off all LEDs.
     *
     * @param boardIndex location piece was dropped.
     */
    public void pieceDown(int boardIndex) {
        String square = chessBoard.indexToBoard(boardIndex);
        switch(mode) {
            case SHOW_PIECES:
                showPieces(true);
                break;

            case PLAYING:
                if(pieceDownIndex != -1) {
                    chessBoardController.led(pieceDownIndex,false);    //turn off previous led
                    board.resetBoard();
                }
                pieceDownIndex = boardIndex;
                chessBoardController.led(pieceDownIndex,true);
                board.setSquareStatus(pieceDownIndex,BoardPanel.SquareStatus.SELECTED);
                processMove();
                break;

            case WAIT_FOR_PIECE_DOWN:
                if(waitForMove.isTakebackMove() && waitForMove.isCapture()) {
                    //from and to down
                    if(waitForMove.getFrom().equals(square)) {
                        waitForMove.setFromDown(true);
                    } else if(waitForMove.getTo().equals(square)) {
                        waitForMove.setToDown(true);
                        if(!waitForMove.isFromDown()) {
                            chessBoardController.blink(2,100,true,waitForMove.getFromIndex());
                        }
                    } else {
                        showWrongPiece(waitForMove,boardIndex,square);
                    }
                }
                else if(waitForMove.isTakebackMove() && !waitForMove.isCapture()) {
                    //to down
                    if(waitForMove.getTo().equals(square)) {
                        waitForMove.setToDown(true);
                    } else {
                        showWrongPiece(waitForMove,boardIndex,square);
                    }
                }
                else if(!waitForMove.isTakebackMove() && waitForMove.isCapture()) {
                    //to down
                    if(waitForMove.getTo().equals(square)) {
                        waitForMove.setToDown(true);
                    } else {
                        showWrongPiece(waitForMove,boardIndex,square);
                    }
                }
                else if(waitForMove.getTo().equals(square)) {
                    waitForMove.setToDown(true);
                }
                else {
                    showWrongPiece(waitForMove,boardIndex,square);
                }

                if(waitForMove.isComplete()) {
                    System.out.println("Move complete");
                    chessBoardController.led(waitForMove.getFromIndex(),false);
                    chessBoardController.led(waitForMove.getToIndex(),false);
                    board.resetBoard();
                    showLastMove();
                    mode = GameMode.PLAYING;
                    removeWaitForMove();
                }
                break;

            default:
                System.out.println("Ignoring piece down for mode "+mode);
                break;
        }
    }

    /** If we have a square that had a piece up and one down, then we can process a move. */
    private void processMove() {
        if(pieceUpIndex != -1 && pieceDownIndex != -1) {
            moveThread.waitForMoveComplete(pieceUpIndex,pieceDownIndex,secondPieceUpIndex);
        }
        enableButtons();
    }

    /** Flashes the piece of the opposite color.
     * @param firstPiece First board index that was piece up.
     * @param secondPiece Second board index that was piece up.
     */
    public void flashCapturedPiece(int firstPiece,int secondPiece) {
        if(chessBoard.getCurrentMove() == ChessBoard.Side.WHITE) {
            if(chessBoard.isBlack(chessBoard.pieceAt(firstPiece))) {
                chessBoardController.blink(2,100,true,firstPiece);
            } else {
                chessBoardController.blink(2,100,true,secondPiece);
            }
        } else {
            if(chessBoard.isWhite(chessBoard.pieceAt(firstPiece))) {
                chessBoardController.blink(2,100,true,firstPiece);
            } else {
                chessBoardController.blink(2,100,true,secondPiece);
            }
        }
    }

    public void showLastMove() {
        board.resetBoard();
        java.util.List<ChessMove> scoreCard = chessBoard.getScoreCard();
        if(scoreCard.size()>0) {
            ChessMove move = scoreCard.get(scoreCard.size()-1);
            int from = move.getFromIndex();
            int to = move.getToIndex();
            board.setSquareStatus(from,BoardPanel.SquareStatus.MOVED);
            board.setSquareStatus(to,BoardPanel.SquareStatus.MOVED);
        }
    }

    public void recordMove(ChessMove move) {
        ChessMove finishCastle = null;
        if(move.isCastleQueenSide() || move.isCastleKingSide()) {
            if(chessBoard.getCurrentMove() == ChessBoard.Side.WHITE) {
                if(move.isCastleKingSide()) {
                    finishCastle = new ChessMove(chessBoard,"h1f1");
                } else {
                    finishCastle = new ChessMove(chessBoard,"a1d1");
                }
            } else {
                if(move.isCastleKingSide()) {
                    finishCastle = new ChessMove(chessBoard,"h8f8");
                } else {
                    finishCastle = new ChessMove(chessBoard,"a8d8");
                }
            }
        }
        chessBoard.move(move);
        if(finishCastle != null)
            addWaitForMove(finishCastle);

        computerMove();
        updateMovesText();
        board.repaint();
        enableButtons();

        if(getPrefs().isShowEvaluation()) {
            try {
                float score = fish.getEvalScore(chessBoard.toFen(),1);
                setMessage("Score "+score);
            } catch(IOException e) {
                e.printStackTrace();
                setMessage("Stockfish restarted");
                fish.startEngine(ChessLR.stockfishPath);
            }
        }

        saveGameMenuItemActionPerformed(null);  //save current game
    }

    public void enableButtons() {
        forwardButton.setEnabled(false);
        fastForwardButton.setEnabled(false);

        if(chessBoard.getScoreCard().size() > 0) {
            backButton.setEnabled(true);
//            fastBackButton.setEnabled(true);  //TODO fast back should take back again after the player finishes the forced move until the button is clicked again.
        } else {
            backButton.setEnabled(false);
            fastBackButton.setEnabled(false);
        }
    }

    public void startup() {
//        new Thread(() -> {
//            try {
//                showMatrixAnim("sweap.gif");
//            } catch(InterruptedException|IOException e) {
//                //do nothing
//            }
//        }).start();
    }

    public void showMatrixAnim(String filename) throws InterruptedException,IOException {
        for(int i=0; i<64; i++) {
            chessBoardController.led(i,false);
        }
        String target = "/"+filename;
        URL url = AppFrame.class.getResource(target);
        GifDecoder gif = new GifDecoder();
        gif.read(url.openStream());
        int n = gif.getFrameCount();
        for(int i=0; i<n; i++) {
            BufferedImage frame = gif.getFrame(i);
            int delay = gif.getDelay(i);
            showFrame(frame);
            Thread.sleep(delay);
        }
        for(int i=0; i<64; i++) {
            chessBoardController.led(i,false);
        }
    }

    public void showFrame(BufferedImage frame) {
        int MASK_RGB    = 0x00FFFFFF;
        boolean ON = true;
        boolean OFF = false;

        int w = frame.getWidth();
        int h = frame.getHeight();
        for(int y=0; y<h; y++) {
            for(int x=0; x<w; x++) {
                int rgb = frame.getRGB(x,y);
                boolean on = (rgb&MASK_RGB) > 0;
                chessBoardController.led(y*8+x,on);
            }
        }
    }


    private void row(int r,boolean on){
        int start = (r-1)*8;
        for(int i=0; i<8; i++) {
            chessBoardController.led(start+i,on);
        }
    }

    private void resetBoard() {
        showPieces(false);
        showLayout(false);
        if(simBoard != null)
            simBoard.reset();
        chessBoardController.resetBoard();
        chessBoard.resetBoard(playerSide);
        chessBoard.setGameId(chessBoard.getGameId()+1);
        board.resetBoard();
        pieceUpIndex = -1;
        pieceDownIndex = -1;
        isShowingPieces = false;
        movesTextArea.setText("");
        removeAllWaitForMoves();
        enableButtons();
        board.repaint();
        mode = GameMode.PLAYING;
        getPrefs().savePrefs(chessBoard);
    }

    /**
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     *      java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy,Method meth,Object[] args) throws Throwable {
        if (meth.getName().equals("handleQuit")) {
            exitMenuItemActionPerformed(null);
        } else if (meth.getName().equals("handleAbout")) {
            aboutMenuItemActionPerformed(null);
        } else if (meth.getName().equals("handlePrefs")) {
            settingsButtonActionPerformed();
        }

        return null;
    }

    private ChessPrefs getPrefs() {
        return prefs;
    }

    private void aboutMenuItemActionPerformed(ActionEvent e) {
        AboutDialog dlg = getAboutDialog();
        if(!dlg.isVisible()) {
            dlg.setVisible(true);
        }
    }

    private AboutDialog getAboutDialog() {
        if(aboutDialog == null) {
            aboutDialog = new AboutDialog(this);
        }
        return aboutDialog;
    }


    private void helpMenuItemActionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(this,"Help");
    }

    private void exitMenuItemActionPerformed(ActionEvent e) {
        System.exit(0);
    }

    private void optionsMenuItemActionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(this,"Options");
    }

    private void menuItem3ActionPerformed(ActionEvent e) {
    }

    private void openMenuItemActionPerformed(ActionEvent e) {
    }

    public Image loadImage(String filename) {
        String target = "/"+filename;
        URL url = AppFrame.class.getResource(target);
//        System.out.println("ImageUtil filename="+target+" url="+url);
        if(url == null) {
            return createPlaceholder(128,128,Color.RED);
        }
        Image im = Toolkit.getDefaultToolkit().getImage(url);
        mediaTracker.addImage(im,0);
//        System.out.println("Waiting for "+filename+" to load");
        try {
            long start = System.currentTimeMillis();
            mediaTracker.waitForAll();
            long end = System.currentTimeMillis();
//            System.out.println(filename+" loaded in "+(end-start)+"ms");
        } catch(InterruptedException e) {
            //ignore
        }
        return im;
    }

    public Image createPlaceholder(int width,int height,Color c) {
        BufferedImage bi;
        bi = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setPaint(colors[colorIndex++]);
        if(colorIndex >= colors.length) {
            colorIndex = 0;
        }
        g2.fillRect(0,0,width,height);
        return bi;
    }

    public Image[] loadImageStrip(String filename,int numImages,int cellWidth,int cellHeight,int cellBorder) {
        Image[] images = new Image[numImages];
        Image img = loadImage(filename);
        System.out.println("Image w="+img.getWidth(null)+" h="+img.getHeight(null));
        int numCols = img.getWidth(null) / cellWidth;
        ImageProducer sourceProducer = img.getSource();
        for(int i=0; i<numImages; i++) {
            images[i] = //createPlaceholder(cellWidth,cellHeight,new Color(i%255,10,10));
                    loadCell(
                            sourceProducer,
                            ((i%numCols)*cellWidth)+cellBorder,
                            ((i/numCols)*cellHeight)+cellBorder,
                            cellWidth-cellBorder,
                            cellHeight-cellBorder);
            mediaTracker.addImage(images[i],0);
        }
        try {
            mediaTracker.waitForAll();
        } catch(InterruptedException e) {
            //ignore
        }
        return images;
    }

    public Image loadCell(ImageProducer ip,int x,int y,int w,int h) {
        return createImage(new FilteredImageSource(ip,new CropImageFilter(x,y,w,h)));
    }

    private void windowMoved(ComponentEvent e) {
        getPrefs().savePrefs(chessBoard);
    }

    private void windowResized(ComponentEvent e) {
        getPrefs().savePrefs(chessBoard);
    }

    private void newMenuItemActionPerformed(ActionEvent e) {
        resetBoard();
    }

    private void showPiecesActionPerformed(ActionEvent e) {

        showPieces(!isShowingPieces);
        showPieces.setSelected(false);
        if(isShowingPieces) {
            mode = GameMode.SHOW_PIECES;
        } else {
            mode = GameMode.PLAYING;
        }
//        showPieces.transferFocus();
        requestFocus();
    }

    private void showPieces(boolean show) {
        removeAllWaitForMoves();

        isShowingPieces = show;
        if(isShowingPieces) {
            for(int i = 0; i < 64; i++) {
                if(i == 35 || i==36) {
                    System.out.println("Stop here");
                }
                chessBoardController.led(i,chessBoardController.hasPiece(i));
            }
        } else {
            for(int i = 0; i < 64; i++) {
                chessBoardController.led(i,false);
            }
        }
        if(buttonDefaultColor == null) {
            buttonDefaultColor = showPieces.getBackground();
        }
        if(isShowingPieces) {
            showPieces.setBackground(buttonSelectedColor);
        }
        else {
            showPieces.setBackground(buttonDefaultColor);
        }

        enableButtons();
    }

    private void forwardButtonActionPerformed(ActionEvent e) {
    }

    private void fastForwardButtonActionPerformed(ActionEvent e) {
    }

    private void saveGameMenuItemActionPerformed(ActionEvent event) {
        try {
            saveGame();
            savePgn();
//            JOptionPane.showMessageDialog(this,"Game "+chessBoard.getGameId()+" saved.");
        } catch(IOException e) {
            ChessLR.handleError("Unable to save game Id"+chessBoard.getGameId(),e);
        }
    }

    private void backButtonActionPerformed(ActionEvent e) {
        ChessMove move = chessBoard.takeback();    //TODO If a move removed a piece, flash the square that needs the piece replaced after the move is complete.
        if(move != null) {

            board.resetBoard();
            chessBoardController.led(-1,false); //turn off all leds
            board.setSquareStatus(move.getFromIndex(),BoardPanel.SquareStatus.SELECTED);
            board.setSquareStatus(move.getToIndex(),BoardPanel.SquareStatus.SELECTED);

            if(move.isCapture()) {
                System.out.println("Move was a capture");
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    addWaitForMove(move);
                    updateMovesText();
                    enableButtons();
                    board.repaint();
                }
            });
        }
    }

    /** Sets the text in the moves text area, taking into account what notation display mode
     * we are using.     */
    private void updateMovesText() {
        if(getPrefs().isPgnNotation())
            movesTextArea.setText(chessBoard.getMovesPgn());
        else
            movesTextArea.setText(chessBoard.getMoveString());
    }

    private void layoutButtonActionPerformed() {
        //clear any move that is being waited on and make sure mode is setup correctly for layout to show
        removeAllWaitForMoves();
        if(mode != GameMode.PLAYING && mode != GameMode.SHOW_LAYOUT)
            mode = GameMode.PLAYING;

        showLayout(mode == GameMode.SHOW_LAYOUT ? false:true);
    }

    protected void showLayout(boolean show) {
        if(waitForMove != null)
            return; //don't change layout mode if in the middle of a move

        if(show) {
            previousMode = mode;
            mode = GameMode.SHOW_LAYOUT;
        } else if(previousMode != null) {
            mode = previousMode;
            previousMode = null;
        }

        if(mode == GameMode.SHOW_LAYOUT) {
            layoutButton.setBackground(buttonSelectedColor);
            layoutButton.setSelected(true);
            for(int i = 0; i < 64; i++) {
                boolean on = chessBoard.pieceAt(i) == ChessBoard.EMPTY_SQUARE ? false : true;
                chessBoardController.led(i,on);
            }
        } else {
            layoutButton.setBackground(buttonDefaultColor);
            layoutButton.setSelected(false);
            for(int i = 0; i < 64; i++) {
                chessBoardController.led(i,false);
            }
            showLastMove();
        }
    }

    private void settingsButtonActionPerformed() {
        getPrefs().setFen(chessBoard.toFen());
        settingsDialog.open(getPrefs());
        chessBoard.setFenPosition(getPrefs().getFen());
        enableButtons();
        updateMovesText();
        repaint();
        fish.setSkillLevel(getPrefs().getLevel());
        fish.setSlowMover(getPrefs().getSlowMover());
        fish.setMoveTime(getPrefs().getMoveTime());


        if(settingsDialog.isNewOnePlayer() || settingsDialog.isNewTwoPlayer()) {
            numberPlayers = settingsDialog.isNewOnePlayer() ? 1 : 2;
            prefs.setPlayers(numberPlayers);

            if(settingsDialog.asBlack) {
                playerSide = ChessBoard.Side.BLACK;
            } else {
                playerSide = ChessBoard.Side.WHITE;
            }
            resetBoard();
            setMessage("New "+numberPlayers+" player as "+playerSide+" started, game id "+chessBoard.getGameId());
        }
        getPrefs().savePrefs(chessBoard);
    }

    /** Set the text on the message bar. */
    public void setMessage(String s) {
        messageLabel.setText(s);
    }

    private void thisMouseReleased(MouseEvent e) {

        System.out.printf("window size %dx%d title bar %d\n",getWidth(),getHeight(),getInsets().top);
        int squareSize = getWidth()/8;
        int squarex = e.getX()/squareSize;
        int squarey = (e.getY()-(this.getInsets().top+2))/squareSize;
        System.out.printf("Mouse released at %d,%d square %d,%d %s\n",e.getX(),e.getY(),squarex,squarey,chessBoard.indexToBoard(chessBoard.toIndex(squarex,squarey)));
        if(squarex<8 && squarey<8)
            simBoard.mouseClicked(squarex,squarey);

    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        saveGameMenuItem = new JMenuItem();
        newMenuItem = new JMenuItem();
        openMenuItem = new JMenuItem();
        optionsMenuItem = new JMenuItem();
        exitMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        helpMenuItem = new JMenuItem();
        aboutMenuItem = new JMenuItem();
        mainPanel = new JPanel();
        movesScrollPane = new JScrollPane();
        movesTextArea = new JTextArea();
        messageButtonPanel = new JPanel();
        messageLabel = new JLabel();
        buttonPanel = new JPanel();
        fastBackButton = new JButton();
        backButton = new JButton();
        forwardButton = new JButton();
        fastForwardButton = new JButton();
        showPieces = new JButton();
        layoutButton = new JButton();
        settingsButton = new JButton();

        //======== this ========
        setResizable(false);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                windowMoved(e);
            }
            @Override
            public void componentResized(ComponentEvent e) {
                windowResized(e);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                thisMouseReleased(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== menuBar1 ========
        {

            //======== fileMenu ========
            {
                fileMenu.setText("File");

                //---- saveGameMenuItem ----
                saveGameMenuItem.setText("Save");
                saveGameMenuItem.addActionListener(e -> saveGameMenuItemActionPerformed(e));
                fileMenu.add(saveGameMenuItem);

                //---- newMenuItem ----
                newMenuItem.setText("New");
                newMenuItem.addActionListener(e -> newMenuItemActionPerformed(e));
                fileMenu.add(newMenuItem);

                //---- openMenuItem ----
                openMenuItem.setText("Open...");
                openMenuItem.addActionListener(e -> openMenuItemActionPerformed(e));
                fileMenu.add(openMenuItem);

                //---- optionsMenuItem ----
                optionsMenuItem.setText("Optons...");
                optionsMenuItem.addActionListener(e -> optionsMenuItemActionPerformed(e));
                fileMenu.add(optionsMenuItem);

                //---- exitMenuItem ----
                exitMenuItem.setText("Exit");
                exitMenuItem.addActionListener(e -> exitMenuItemActionPerformed(e));
                fileMenu.add(exitMenuItem);
            }
            menuBar1.add(fileMenu);

            //======== helpMenu ========
            {
                helpMenu.setText("Help");

                //---- helpMenuItem ----
                helpMenuItem.setText("ChessLR Help");
                helpMenuItem.addActionListener(e -> helpMenuItemActionPerformed(e));
                helpMenu.add(helpMenuItem);

                //---- aboutMenuItem ----
                aboutMenuItem.setText("About...");
                aboutMenuItem.addActionListener(e -> aboutMenuItemActionPerformed(e));
                helpMenu.add(aboutMenuItem);
            }
            menuBar1.add(helpMenu);
        }
        setJMenuBar(menuBar1);

        //======== mainPanel ========
        {
            mainPanel.setMinimumSize(new Dimension(337, 50));
            mainPanel.setLayout(new BorderLayout());

            //======== movesScrollPane ========
            {
                movesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                movesScrollPane.setAutoscrolls(true);
                movesScrollPane.setPreferredSize(new Dimension(3629, 150));

                //---- movesTextArea ----
                movesTextArea.setWrapStyleWord(true);
                movesTextArea.setEditable(false);
                movesTextArea.setLineWrap(true);
                movesTextArea.setFont(new Font("Monospaced", Font.PLAIN, 18));
                movesTextArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                movesScrollPane.setViewportView(movesTextArea);
            }
            mainPanel.add(movesScrollPane, BorderLayout.CENTER);

            //======== messageButtonPanel ========
            {
                messageButtonPanel.setLayout(new BorderLayout());

                //---- messageLabel ----
                messageLabel.setText("text");
                messageButtonPanel.add(messageLabel, BorderLayout.NORTH);

                //======== buttonPanel ========
                {
                    buttonPanel.setLayout(new FlowLayout());

                    //---- fastBackButton ----
                    fastBackButton.setText("<<");
                    buttonPanel.add(fastBackButton);

                    //---- backButton ----
                    backButton.setText("<");
                    backButton.addActionListener(e -> backButtonActionPerformed(e));
                    buttonPanel.add(backButton);

                    //---- forwardButton ----
                    forwardButton.setText(">");
                    forwardButton.addActionListener(e -> forwardButtonActionPerformed(e));
                    buttonPanel.add(forwardButton);

                    //---- fastForwardButton ----
                    fastForwardButton.setMinimumSize(new Dimension(78, 78));
                    fastForwardButton.setText(">>");
                    fastForwardButton.addActionListener(e -> fastForwardButtonActionPerformed(e));
                    buttonPanel.add(fastForwardButton);

                    //---- showPieces ----
                    showPieces.setText("Show");
                    showPieces.addActionListener(e -> showPiecesActionPerformed(e));
                    buttonPanel.add(showPieces);

                    //---- layoutButton ----
                    layoutButton.setText("L");
                    layoutButton.addActionListener(e -> layoutButtonActionPerformed());
                    buttonPanel.add(layoutButton);

                    //---- settingsButton ----
                    settingsButton.setText("Settings");
                    settingsButton.addActionListener(e -> settingsButtonActionPerformed());
                    buttonPanel.add(settingsButton);
                }
                messageButtonPanel.add(buttonPanel, BorderLayout.CENTER);
            }
            mainPanel.add(messageButtonPanel, BorderLayout.SOUTH);
        }
        contentPane.add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem saveGameMenuItem;
    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem optionsMenuItem;
    private JMenuItem exitMenuItem;
    private JMenu helpMenu;
    private JMenuItem helpMenuItem;
    private JMenuItem aboutMenuItem;
    private JPanel mainPanel;
    private JScrollPane movesScrollPane;
    protected JTextArea movesTextArea;
    private JPanel messageButtonPanel;
    private JLabel messageLabel;
    private JPanel buttonPanel;
    private JButton fastBackButton;
    private JButton backButton;
    private JButton forwardButton;
    private JButton fastForwardButton;
    private JButton showPieces;
    private JButton layoutButton;
    private JButton settingsButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
