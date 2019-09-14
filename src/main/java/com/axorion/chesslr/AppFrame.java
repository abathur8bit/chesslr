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
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
    MoveThread moveThread = new MoveThread(this);
    boolean boardAttached;
    String waitForMove = null;
    String waitForPieceUp = null;
    String waitForPieceDown = null;
    boolean takebackCapture = false;
    GameMode mode = GameMode.PLAYING;
    GameMode previousMode = null;

    int numberPlayers = 1;
    ChessBoard.Side playerSide = ChessBoard.Side.WHITE;

    SimBoard simBoard;
    Stockfish fish = new Stockfish();
    String stockfishPath = "../stockfish/cmake-build-debug/stockfish";

    public AppFrame(String title,boolean boardAttached) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, IOException, I2CFactory.UnsupportedBusNumberException {
        super(title);
        this.boardAttached = boardAttached;
        prefs = new ChessPrefs(this);
        prefs.loadPrefs();
        chessBoard.setGameId(prefs.gameId);

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
            chessBoardController.set3x3Maps();
        } else {
            simBoard = new SimBoard(this);
            chessBoardController = new BoardController(simBoard,simBoard);
            chessBoardController.addListener(this);
        }
        moveThread.start();

        resetBoard();

        if(fish.startEngine(ChessLR.stockfishPath) == false)
            setMessage("Unable to start stockfish");
        else
            setMessage("Stockfish ready");
    }

    /** Start waiting for a specific move to be made.
     * To wait for a move:
     * Light LEDs on board.
     * If a piece is lifted from the wrong square, flash that LED till the piece is put back down.
     * If a piece is dropped on the wrong square, flast that LED till the piece is lefted up.
     * */
    private void setWaitForMove(String move) {
        if(move != null && move.length() != 4) throw new IllegalArgumentException("Move must be in format of 'd2d3'");

        clearTakeback();

        waitForMove = move;
        if(move != null) {
            waitForPieceUp = chessBoard.from(move);
            waitForPieceDown = chessBoard.to(move);
            mode = GameMode.WAIT_FOR_PIECE_UP;

            chessBoardController.led(chessBoard.boardToIndex(chessBoard.from(move)),true);
            chessBoardController.led(chessBoard.boardToIndex(chessBoard.to(move)),true);
            board.setSquareStatus(chessBoard.boardToIndex(chessBoard.from(move)),BoardPanel.SquareStatus.SELECTED);
            board.setSquareStatus(chessBoard.boardToIndex(chessBoard.to(move)),BoardPanel.SquareStatus.SELECTED);
        }
    }

    /** If there is an existing move beign taken back, turn off LED's for that move. */
    private void clearTakeback() {
        //TODO clear takeback
        waitForMove = null;

        if(waitForPieceUp != null) {
            chessBoardController.led(chessBoard.boardToIndex(waitForPieceUp),false);
            waitForPieceUp = null;
        }
        if(waitForPieceDown != null) {
            chessBoardController.led(chessBoard.boardToIndex(waitForPieceDown),false);
            waitForPieceDown = null;
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
        resetBoard();
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
                    pieceUpIndex = boardIndex;
                    chessBoardController.led(pieceUpIndex,true);
                    board.setSquareStatus(pieceUpIndex,BoardPanel.SquareStatus.SELECTED);
                    processMove();
                } else {
                    System.out.println("Picking up a second piece");
                    secondPieceUpIndex = boardIndex;
                    chessBoardController.led(boardIndex,true);
                }
                break;

            case WAIT_FOR_PIECE_UP:
                if(!chessBoard.from(waitForMove).equals(square)) {
                    System.out.println("Expecting "+waitForPieceUp+" but got "+square);

                    SwingUtilities.invokeLater(() -> {
                        chessBoardController.flashOn(boardIndex);
                        board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.ERROR);
                    });

                    JOptionPane.showMessageDialog(this,"Replace piece at "+square.toUpperCase(),"Invalid move",JOptionPane.ERROR_MESSAGE);
                    SwingUtilities.invokeLater(() -> {
                        chessBoardController.flashOff(boardIndex);
                        board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.NORMAL);
                    });
                } else {
                    System.out.println("Got expected piece up ");
                    mode = GameMode.WAIT_FOR_PIECE_DOWN;
                    chessBoardController.led(boardIndex,false);
                }
                break;

            case WAIT_FOR_PIECE_DOWN:
                SwingUtilities.invokeLater(() -> {
                    chessBoardController.flashOn(boardIndex);
                    board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.ERROR);
                });
                JOptionPane.showMessageDialog(this,"Replace piece at "+square.toUpperCase()+".\nWaiting for piece at "+waitForPieceDown,"Invalid move",JOptionPane.ERROR_MESSAGE);
                SwingUtilities.invokeLater(() -> {
                    chessBoardController.flashOff(boardIndex);
                    board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.NORMAL);
                });

            default:
                System.out.println("Ignoring piece up at "+boardIndex+" for mode "+mode);
                break;
        }
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
                if(!waitForPieceDown.equals(square)) {
                    SwingUtilities.invokeLater(() -> {
                        chessBoardController.flashOn(boardIndex);
                        board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.ERROR);
                    });

                    JOptionPane.showMessageDialog(this,"Remove piece at "+square.toUpperCase(),"Invalid move",JOptionPane.ERROR_MESSAGE);
                    SwingUtilities.invokeLater(() -> {
                        chessBoardController.flashOff(boardIndex);
                        board.setSquareStatus(boardIndex,BoardPanel.SquareStatus.NORMAL);
                    });
                } else {
                    System.out.println("Got expected piece down");
                    chessBoardController.led(boardIndex,false);
                    if(takebackCapture) {
                        waitForPieceDown = chessBoard.from(waitForMove);
                        int waitIndex = chessBoard.boardToIndex(waitForPieceDown);
                        takebackCapture = false;
                        chessBoardController.blink(3,100,true,waitIndex);
                        chessBoardController.led(waitIndex,true);
                        board.setSquareStatus(waitIndex,BoardPanel.SquareStatus.WARNING);
                    } else {
                        board.resetBoard();
                        showLastMove();
                        setWaitForMove(null);
                        mode = GameMode.PLAYING;
                    }

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

    public void showLastMove() {
        board.resetBoard();
        java.util.List<String> scoreCard = chessBoard.getScoreCard();
        if(scoreCard.size()>0) {
            String move = scoreCard.get(scoreCard.size()-1);
            int from = chessBoard.boardToIndex(chessBoard.from(move));
            int to = chessBoard.boardToIndex(chessBoard.to(move));
            board.setSquareStatus(from,BoardPanel.SquareStatus.MOVED);
            board.setSquareStatus(to,BoardPanel.SquareStatus.MOVED);
        }
    }

    public void recordMove(String move) {
//        boardValidator.loadFromFen(chessBoard.toFen());
//        boolean legal = boardValidator.isMoveLegal(new Move(
//                Square.fromValue(chessBoard.from(move).toUpperCase()),
//                Square.fromValue(chessBoard.to(move).toUpperCase())),true);
//        System.out.println("Move legal "+legal);
        chessBoard.move(move);
        updateMovesText();
        board.repaint();
        enableButtons();

        if(numberPlayers == 1) {

        }
//        try {
//            float score = fish.getEvalScore(chessBoard.toFen(),1);
//            setMessage("Score "+score);
//        } catch(IOException e) {
//            setMessage("Stockfish restarted");
//            fish.startEngine(ChessLR.stockfishPath);
//        }
    }

    public void enableButtons() {
        forwardButton.setEnabled(false);
        fastForwardButton.setEnabled(true);

        if(chessBoard.getScoreCard().size() > 0) {
            backButton.setEnabled(true);
//            fastBackButton.setEnabled(true);  //TODO fast back should take back again after the player finishes the forced move until the button is clicked again.
        } else {
            backButton.setEnabled(false);
            fastBackButton.setEnabled(false);
        }
    }

    public void startup() {
        new Thread(() -> {
            try {
                for(int i = 0; i < 64; ++i) {
                    chessBoardController.led(i,true);
                    Thread.sleep(100);
                    chessBoardController.led(i,false);
                }

                Thread.sleep(1000);
                for(int i=1; i<9; i++) {
                    row(i,true);
                    Thread.sleep(100);
                    row(i,false);
                }
            } catch(InterruptedException e) {
                //do nothing
            }
        }).start();
    }

    private void row(int r,boolean on){
        int start = (r-1)*8;
        for(int i=0; i<8; i++) {
            chessBoardController.led(start+i,on);
        }
    }

    private void resetBoard() {
        showPieces(false);
        if(simBoard != null)
            simBoard.reset();
        chessBoardController.resetBoard();
        chessBoard.resetBoard(playerSide);
        board.resetBoard();
        pieceUpIndex = -1;
        pieceDownIndex = -1;
        isShowingPieces = false;
        movesTextArea.setText("");
        enableButtons();
        board.repaint();
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
        System.out.println("ImageUtil filename="+target+" url="+url);
        if(url == null) {
            return createPlaceholder(128,128,Color.RED);
        }
        Image im = Toolkit.getDefaultToolkit().getImage(url);
        mediaTracker.addImage(im,0);
        System.out.println("Waiting for "+filename+" to load");
        try {
            long start = System.currentTimeMillis();
            mediaTracker.waitForAll();
            long end = System.currentTimeMillis();
            System.out.println(filename+" loaded in "+(end-start)+"ms");
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
        getPrefs().savePrefs(chessBoard.toFen());
    }

    private void windowResized(ComponentEvent e) {
        getPrefs().savePrefs(chessBoard.toFen());
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
        clearTakeback();

        isShowingPieces = show;
        if(isShowingPieces) {
            for(int i = 0; i < 64; i++) {
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
        final String move = "a7a6";
        recordMove(move);
        setWaitForMove(move);
    }

    private void saveGameMenuItemActionPerformed(ActionEvent event) {
        try {
            saveGame();
            savePgn();
            JOptionPane.showMessageDialog(this,"Game "+chessBoard.getGameId()+" saved.");
        } catch(IOException e) {
            ChessLR.handleError("Unable to save game Id"+chessBoard.getGameId(),e);
        }
    }

    private void backButtonActionPerformed(ActionEvent e) {
        String move = chessBoard.takeback();    //TODO If a move removed a piece, flash the square that needs the piece replaced after the move is complete.
        if(move != null) {
            String to = chessBoard.from(move);  //reversing move
            String from = chessBoard.to(move);  //reversing move
            final String waitMove = from+to;

            board.resetBoard();
            board.setSquareStatus(chessBoard.boardToIndex(from),BoardPanel.SquareStatus.SELECTED);
            board.setSquareStatus(chessBoard.boardToIndex(to),BoardPanel.SquareStatus.SELECTED);

            if(chessBoard.pieceAt(from) != ChessBoard.EMPTY_SQUARE) {
                System.out.println("Move was a capture");
                takebackCapture = true;
            }

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setWaitForMove(waitMove);
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
        if(mode == GameMode.SHOW_LAYOUT) {
            layoutButton.setBackground(buttonDefaultColor);
            layoutButton.setSelected(false);
            mode = GameMode.PLAYING;
            for(int i = 0; i < 64; i++) {
                chessBoardController.led(i,false);
            }
            showLastMove();
        } else {
            layoutButton.setBackground(buttonSelectedColor);
            layoutButton.setSelected(true);
            mode = GameMode.SHOW_LAYOUT;
            for(int i = 0; i < 64; i++) {
                boolean on = chessBoard.pieceAt(i) == ChessBoard.EMPTY_SQUARE ? false : true;
                chessBoardController.led(i,on);
            }
        }
    }

    private void settingsButtonActionPerformed() {
        settingsDialog.open(getPrefs());
        enableButtons();
        updateMovesText();
        repaint();

        if(settingsDialog.isNewOnePlayer() || settingsDialog.isNewTwoPlayer()) {
            numberPlayers = settingsDialog.isNewOnePlayer() ? 1 : 2;
            if(settingsDialog.asBlack) {
                playerSide = ChessBoard.Side.BLACK;
            } else {
                playerSide = ChessBoard.Side.WHITE;
            }
            resetBoard();

            setMessage("New "+numberPlayers+" player as "+playerSide+" started");
        }
        //TODO Options should be available in the settings dialog
        //  and settings should be persisted when dialog is closed.
        getPrefs().savePrefs(chessBoard.toFen());
    }

    /** Set the text on the message bar. */
    public void setMessage(String s) {
        messageLabel.setText(s);
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
