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
import com.axorion.chesslr.hardware.ChessLEDController;
import com.axorion.chesslr.hardware.ChessReedController;
import com.axorion.chesslr.hardware.InputController;
import com.axorion.chesslr.hardware.LEDController;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import oled.OLEDDisplay;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

/**
 * @author Lee Patterson
 */
public class AppFrame extends JFrame implements InvocationHandler {
    GpioController gpio;
    LEDController ledController;
    InputController reedController;
    OLEDDisplay display;

    String whitePieceLetters = "PNBRQK";
    String blackPieceLetters = "pnbrqk";
    BoardPanel board;
    ChessBoard chessBoard = new ChessBoard();
    Integer pieceUpIndex = -1;      //the square that a piece was lifted from
    Integer pieceDownIndex = -1;    //the square that a piece was dropped onto
    JFileChooser fileChooser;   //used by windows
    FileDialog fileDialog;      //used by mac
    boolean isMac = false;
    ChessPrefs prefs;
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
    Color buttonBackgroundColor = null;
    int colorIndex=0;
    boolean isShowingPieces = false;
    MoveThread moveThread = new MoveThread(this);
    boolean boardAttached;
    String waitForPieceUp = null;
    String waitForPieceDown = null;

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
        setButtonImage(resetButton,"button-reset.png");

        if(boardAttached) {
            gpio = GpioFactory.getInstance();
            initHardware();
            moveThread.start();
        }
    }

    /** Start waiting for a specific move to be made. */
    private void waitForMove(String move) {
        clearTakeback();

        String from = chessBoard.from(move);
        String to = chessBoard.to(move);
        ledController.led(mapToPin(chessBoard.boardToIndex(from)),true);
        ledController.led(mapToPin(chessBoard.boardToIndex(to)),true);

        waitForPieceUp = from;
        waitForPieceDown = to;
    }

    /** If there is an existing move beign taken back, turn off LED's for that move. */
    private void clearTakeback() {
        if(waitForPieceUp != null) {
            ledController.led(mapToPin(chessBoard.boardToIndex(waitForPieceUp)),false);
            waitForPieceUp = null;
        }
        if(waitForPieceDown != null) {
            ledController.led(mapToPin(chessBoard.boardToIndex(waitForPieceDown)),false);
            waitForPieceDown = null;
        }
    }
    /** Any text that is added to the moves text area will automatically scroll into view. */
    private void movesAutoScroll() {
        DefaultCaret caret = (DefaultCaret) movesTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    private void setButtonImage(JButton bn,String iconName) {
        final int size = 50;
        Dimension preferredSize = new Dimension(size,size);
        bn.setText("");
        Image im = loadImage(iconName).getScaledInstance((int)(size*0.8),(int)(size*0.8),SCALE_SMOOTH);
        bn.setIcon(new ImageIcon(im));
        bn.setPreferredSize(preferredSize);
    }

    private void initHardware() throws IOException, I2CFactory.UnsupportedBusNumberException {
        final int bus = I2CBus.BUS_1;

        ledController = new ChessLEDController(gpio,bus);

        reedController = new ChessReedController(gpio,bus);
        reedController.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                //TODO If there is a piece up and we got a down, then we moved the up location to down location
                //TODO If there was a down, but no up, then wait for an up, and move that up to the down location.

                final Pin pin = event.getPin().getPin();
                final int index = reedController.findPinIndex(pin);
//                System.out.println("application gpio pin state change: " + event.getPin() + " = " + event.getState() + " index = "+index);

                if(reedController.stateIsDown(event.getState())) {
                    pieceDown(index);
                } else {
                    pieceUp(index);
                }
            }
        });
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

    public void drawRect(OLEDDisplay display,int xpos,int ypos,int width,int height,boolean on) {
        int y=ypos;
        int x=xpos;
        for(x=xpos; x<xpos+width-1; ++x) {
            display.setPixel(x,y,on);
        }
        y=ypos+height-1;
        for(x=xpos; x<xpos+width-1; ++x) {
            display.setPixel(x,y,on);
        }

        x=xpos;
        for(y=ypos; y<ypos+height-1; ++y) {
            display.setPixel(x,y,on);
        }
        x=xpos+width-1;
        for(y=ypos; y<ypos+height-1; ++y) {
            display.setPixel(x,y,on);
        }
    }

    public void startApp() throws IOException {
        resetBoard();
        startup();
        setVisible(true);
    }

    /**
     * Piece was lifted off the board.
     *
     * @param index location piece was picked up from.
     */
    public void pieceUp(int index) {
        if(isShowingPieces) {
            showPieces(true);
        } else {
            if(waitForPieceUp != null) {
                ledController.led(index,false);
                waitForPieceUp = null;
            } else {
                if(pieceUpIndex == -1 && waitForPieceDown == null) {
                    //only process the first lift, as others are the piece moving around
                    //also if we are waiting for a piece down, we can't start a new move
                    pieceUpIndex = mapToBoard(index);
                    ledController.led(mapToPin(pieceUpIndex),true);
                    processMove();
                }
            }
        }
    }

    /**
     * Piece was put back onto the board at the given index.
     *
     * @param index location piece was dropped.
     */
    public void pieceDown(int index) {
        enableButtons();
        if(isShowingPieces) {
            showPieces(true);
        } else {
            if(waitForPieceDown != null) {
                ledController.led(index,false);
                waitForPieceDown = null;

            } else {
                if(pieceDownIndex != -1) {
                    ledController.led(mapToPin(pieceDownIndex),false);    //turn off previous led
                }
                pieceDownIndex = mapToBoard(index);
                ledController.led(mapToPin(pieceDownIndex),true);
                processMove();
            }
        }
    }

    /** If we have a square that had a piece up and one down, then we can process a move. */
    private void processMove() {
        if(pieceUpIndex != -1 && pieceDownIndex != -1) {
            moveThread.waitForMoveComplete(pieceUpIndex,pieceDownIndex);
        }
    }

    public void recordMove(String move) {
        chessBoard.move(move);
        movesTextArea.setText(chessBoard.getMovesPgn());
        board.repaint();
        enableButtons();
    }

    public void enableButtons() {
        forwardButton.setEnabled(false);
        fastForwardButton.setEnabled(true);

        if(chessBoard.getScoreCard().size() > 0) {
            backButton.setEnabled(true);
//            fastBackButton.setEnabled(true);
        } else {
            backButton.setEnabled(false);
            fastBackButton.setEnabled(false);
        }
    }

    public void blink(final int count, final long delay, final int ledIndex) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    if(ledController.isOn(ledIndex)) {
                        ledController.led(ledIndex,false);
                        Thread.sleep(delay);
                    }
                    for(int i = 0; i < count; i++) {
                        ledController.led(ledIndex,true);
                        Thread.sleep(delay);
                        ledController.led(ledIndex,false);
                        Thread.sleep(delay);
                    }
                } catch(InterruptedException e) {
                    ledController.led(ledIndex,false);
                }
            }
        }).start();
    }

    public int mapToBoard(int pinIndex) {
//        int[] pinToBoardMap = {3,4,5,11,12,13,19,20,21}; //upper middle
        int[] pinToBoardMap = {0,1,2,8,9,10,16,17,18}; //top left
//        int[] pinToBoardMap = {56,57,58,48,49,50,40,41,42}; //bottom leff
        return pinToBoardMap[pinIndex];
    }

    public int mapToPin(int boardIndex) {
        //upper middle
//        int[] boardToPinMap = {
//                0,0,0,0,1,2,0,0,
//                0,0,0,3,4,5,0,0,
//                0,0,0,6,7,8,0,0,
//                0,0,0,0,0,0,0,0,
//                0,0,0,0,0,0,0,0,
//                0,0,0,0,0,0,0,0,
//                0,0,0,0,0,0,0,0,
//                0,0,0,0,0,0,0,0
//        };
        //top left
        int[] boardToPinMap = {
                0,1,2,0,0,0,0,0,
                3,4,5,0,0,0,0,0,
                6,7,8,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0
        };
        return boardToPinMap[boardIndex];

    }

    public void startup() throws IOException {
        if(boardAttached) {
            try {
                for(int i = 0; i < 9; ++i) {
                    ledController.led(i,true);
                    Thread.sleep(100);
                    ledController.led(i,false);
                }
            } catch(InterruptedException e) {
                //do nothing
            }
        }
    }

    private void resetBoard() {
        showPieces(false);
        chessBoard.resetBoard();
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
            optionsMenuItemActionPerformed(null);
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
        // TODO add your code here
    }

    private void openMenuItemActionPerformed(ActionEvent e) {
        // TODO add your code here
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
        getPrefs().savePrefs();
    }

    private void windowResized(ComponentEvent e) {
        getPrefs().savePrefs();
    }

    private void newMenuItemActionPerformed(ActionEvent e) {
        resetBoard();
    }

    private void showPiecesActionPerformed(ActionEvent e) {

        showPieces(!isShowingPieces);
        showPieces.setSelected(false);
//        showPieces.transferFocus();
        requestFocus();
    }

    private void showPieces(boolean show) {
        clearTakeback();

        isShowingPieces = show;
        if(boardAttached) {
            if(isShowingPieces) {
                for(int i = 0; i < 9; i++) {
                    ledController.led(i,reedController.isSet(i));
                }
            } else {
                for(int i = 0; i < 9; i++) {
                    ledController.led(i,false);
                }
            }
        }
        if(buttonBackgroundColor == null) {
            buttonBackgroundColor = showPieces.getBackground();
        }
        if(isShowingPieces) {
            showPieces.setBackground(Color.gray);
        }
        else {
            showPieces.setBackground(buttonBackgroundColor);
        }

        enableButtons();
    }

    private void resetButtonActionPerformed(ActionEvent e) {
        resetBoard();
    }

    private void forwardButtonActionPerformed(ActionEvent e) {
        // TODO add your code here
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
        String move = chessBoard.takeback();
        if(move != null) {
            String from = chessBoard.from(move);
            String to = chessBoard.to(move);
            final String waitMove = to+from;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    waitForMove(waitMove);
                    movesTextArea.setText(chessBoard.getMovesPgn());
                    enableButtons();
                    board.repaint();
                }
            });
        }
    }

    private void fastForwardButtonActionPerformed(ActionEvent e) {
        final String move = "a7a6";
        recordMove(move);
        waitForMove(move);
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
        panel2 = new JPanel();
        fastBackButton = new JButton();
        backButton = new JButton();
        forwardButton = new JButton();
        fastForwardButton = new JButton();
        showPieces = new JButton();
        resetButton = new JButton();

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
                saveGameMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveGameMenuItemActionPerformed(e);
                    }
                });
                fileMenu.add(saveGameMenuItem);

                //---- newMenuItem ----
                newMenuItem.setText("New");
                newMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        newMenuItemActionPerformed(e);
                    }
                });
                fileMenu.add(newMenuItem);

                //---- openMenuItem ----
                openMenuItem.setText("Open...");
                openMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        openMenuItemActionPerformed(e);
                    }
                });
                fileMenu.add(openMenuItem);

                //---- optionsMenuItem ----
                optionsMenuItem.setText("Optons...");
                optionsMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        optionsMenuItemActionPerformed(e);
                    }
                });
                fileMenu.add(optionsMenuItem);

                //---- exitMenuItem ----
                exitMenuItem.setText("Exit");
                exitMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        exitMenuItemActionPerformed(e);
                    }
                });
                fileMenu.add(exitMenuItem);
            }
            menuBar1.add(fileMenu);

            //======== helpMenu ========
            {
                helpMenu.setText("Help");

                //---- helpMenuItem ----
                helpMenuItem.setText("ChessLR Help");
                helpMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        helpMenuItemActionPerformed(e);
                    }
                });
                helpMenu.add(helpMenuItem);

                //---- aboutMenuItem ----
                aboutMenuItem.setText("About...");
                aboutMenuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        aboutMenuItemActionPerformed(e);
                    }
                });
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
                movesScrollPane.setViewportView(movesTextArea);
            }
            mainPanel.add(movesScrollPane, BorderLayout.CENTER);

            //======== panel2 ========
            {
                panel2.setLayout(new FlowLayout());

                //---- fastBackButton ----
                fastBackButton.setText("<<");
                panel2.add(fastBackButton);

                //---- backButton ----
                backButton.setText("<");
                backButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        backButtonActionPerformed(e);
                    }
                });
                panel2.add(backButton);

                //---- forwardButton ----
                forwardButton.setText(">");
                forwardButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        forwardButtonActionPerformed(e);
                    }
                });
                panel2.add(forwardButton);

                //---- fastForwardButton ----
                fastForwardButton.setMinimumSize(new Dimension(78, 78));
                fastForwardButton.setText(">>");
                fastForwardButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        fastForwardButtonActionPerformed(e);
                    }
                });
                panel2.add(fastForwardButton);

                //---- showPieces ----
                showPieces.setText("Show");
                showPieces.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        showPiecesActionPerformed(e);
                    }
                });
                panel2.add(showPieces);

                //---- resetButton ----
                resetButton.setText("Reset");
                resetButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        resetButtonActionPerformed(e);
                    }
                });
                panel2.add(resetButton);
            }
            mainPanel.add(panel2, BorderLayout.SOUTH);
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
    private JPanel panel2;
    private JButton fastBackButton;
    private JButton backButton;
    private JButton forwardButton;
    private JButton fastForwardButton;
    private JButton showPieces;
    private JButton resetButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
