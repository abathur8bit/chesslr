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
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.PinState;
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
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;

/**
 * @author Lee Patterson
 */
public class AppFrame extends JFrame implements InvocationHandler {
    final GpioController gpio = GpioFactory.getInstance();
    ChessLEDController ledController;
    ChessReedController reedController;
    OLEDDisplay display;

    String whitePieceLetters = "PNBRQK";
    String blackPieceLetters = "pnbrqk";
    BoardPanel board;
    ChessBoard chessBoard = new ChessBoard();
    int gamePieceSelected;
    int gamePieceCaptured;
    int selectedIndex = -1; //what board index was selected (piece was lifted from the square)
    int capturedIndex = -1; //if you have already picked up a piece, this is the piece you are capturing
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
    int colorIndex=0;

    public AppFrame(String title) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, IOException, I2CFactory.UnsupportedBusNumberException {
        super(title);
        prefs = new ChessPrefs(this);
        prefs.loadPrefs();

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
        getContentPane().add(board,BorderLayout.CENTER);
        //some
//        movesTextArea.setText("1.d4 Nf6 2.c4 g6 3.f3 c5 4.d5 d6 5.Nc3 e6 6.e4 Bg7 7.Nge2 exd5 "+
//                "8.cxd5 a6 9.a4 Nbd7 10.Ng3 h5 11.Be2 h4 12.Nf1 Nh5 13.Be3 f5 "+
//                "14.exf5 gxf5 15.Nd2 Ne5 16.f4 Ng4 17.Nc4 O-O 18.O-O Ng3 19.hxg3 "+
//                "hxg3 20.Bxg4 fxg4 21.Ne4 Qh4 22.Nxg3 Qxg3 23.Qe1 Qxe1 24.Raxe1 "+
//                "Bd7 25.Nxd6 Bxa4 26.Bxc5 b6 27.Ba3 Bb3 28.Kh2 Bxd5 29.Kg3 b5 "+
//                "30.Rd1 Ba2 31.Kxg4 Rab8 32.Bc5 b4 33.Rd2 a5 34.Ra1 b3 35.Re1 "+
//                "Rxf4+ 36.Kxf4 Bh6+ 37.Kg4 Bxd2 38.Re7 Bb4 39.Bxb4 Rxb4+ 40.Kg5 "+
//                "Bb1 41.Ra7 a4 42.Ne8 Kf8 43.Nf6 Rd4");
        movesAutoScroll();
        pack();

        setButtonImage(backButton,"button-back.png");
        setButtonImage(fastBackButton,"button-fastback.png");
        setButtonImage(forwardButton,"button-forward.png");
        setButtonImage(fastForwardButton,"button-fastforward.png");

        initHardware();
    }

    /** Any text that is added to the moves text area will automatically scroll into view. */
    private void movesAutoScroll() {
        DefaultCaret caret = (DefaultCaret) movesTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    private void setButtonImage(JButton bn,String iconName) {
        Dimension size = new Dimension(75,75);
        bn.setText("");
        bn.setIcon(new ImageIcon(loadImage(iconName)));
        bn.setPreferredSize(size);
    }

    private void initHardware() throws IOException, I2CFactory.UnsupportedBusNumberException {
        final int bus = I2CBus.BUS_1;

//        final int baseAddress = 0x21;
//        MCP23017GpioProvider provider = new MCP23017GpioProvider(bus,baseAddress);

        ledController = new ChessLEDController(gpio,bus);

        reedController = new ChessReedController(gpio,bus);
        reedController.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                //TODO
                // Implement a delay to ensure the state of the switch holds open or closed.
                // Like debouncing, but this handles when the piece slides over the dead zone of a reed switch.
                boolean state = event.getState() == PinState.HIGH ? false:true;
                final int ledIndex = reedController.findPinIndex(event.getPin().getPin());

//                System.out.println("application gpio pin state change: " + event.getPin() + " = " + event.getState() + " led="+ledIndex);

                if(state) {
                    pieceDown(ledIndex);
                } else {
                    pieceUp(ledIndex);
                }

            }
        });
        display = new OLEDDisplay(I2CBus.BUS_1,0x3D);

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
        int boardIndex = mapToBoard(index);
        gamePieceSelected = boardIndex;
        blink(1,100,index);
    }

    /**
     * Piece was put back onto the board at the given index.
     *
     * @param index location piece was dropped.
     */
    public void pieceDown(int index) {
        //TODO
        // If we detect that a piece dropped without a piece being picked up, a piece was added to the board
        // or the board detected the piece up after the down, as would be the case of sliding a piece from one
        // square to the next without picking it up.
        if(gamePieceSelected == -1) {
            System.out.println("Piece dropped but didn't detect piece picked up, finding if there is a piece missing on the board");
            int missingPieceIndex = findMissingPiece();
        } else {
            int boardIndex = mapToBoard(index);
            String playersMove = chessBoard.indexToBoard(gamePieceSelected)+chessBoard.indexToBoard(boardIndex);
            chessBoard.move(playersMove);
            movesTextArea.setText(chessBoard.getMoveString());

            gamePieceSelected = -1;
            System.out.format("Piece dropped at [%d] [%s] move=[%s]\n",index,chessBoard.indexToBoard(boardIndex),playersMove);
        }
        board.repaint();
        blink(2,100,index);
    }

    /**
     * Compares what is on the e-board to what is in the chessBoard array. If they don't match up, return the piece
     * that is in the chessBoard and not on the e-board.
     * @return Index of missing piece, -1 if none missing.
     */
    private int findMissingPiece() {
        for(int i = 0; i < 64; i++) {
        }
        return -1;
    }

    public void blink(final int count, final long delay, final int ledIndex) {
        new Thread(new Runnable() {
            public void run() {
                try {
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

    public int mapToBoard(int index) {
        int[] map = {56,57,58,48,49,50,40,41,42};
        return map[index];
    }

    public void startup() throws IOException {
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

    /** Player has lifted a piece, or in other words, the sensor no longer sees a piece. */
    public void pieceUp(int x,int y) {
        final int idx = chessBoard.toIndex(x,y);
        final int piece = chessBoard.pieceAt(idx);

        selectedIndex = idx;
    }

    public void pieceDown(int x,int y) {
        final int droppedIndex = chessBoard.toIndex(x,y);
        String move = chessBoard.indexToBoard(selectedIndex)+chessBoard.indexToBoard(droppedIndex);
        System.out.printf("Move [%s] piece [%c]\n",move,chessBoard.pieceAt(selectedIndex));
        chessBoard.move(move);
        selectedIndex = -1;
        capturedIndex = -1;
    }

    private void resetBoard() {
        chessBoard.resetBoard();
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


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        newMenuItem = new JMenuItem();
        openMenuItem = new JMenuItem();
        optionsMenuItem = new JMenuItem();
        exitMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        helpMenuItem = new JMenuItem();
        aboutMenuItem = new JMenuItem();
        panel1 = new JPanel();
        movesScrollPane = new JScrollPane();
        movesTextArea = new JTextArea();
        panel2 = new JPanel();
        fastBackButton = new JButton();
        backButton = new JButton();
        forwardButton = new JButton();
        fastForwardButton = new JButton();

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

        //======== panel1 ========
        {
            panel1.setMinimumSize(new Dimension(337, 50));
            panel1.setLayout(new BorderLayout());

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
            panel1.add(movesScrollPane, BorderLayout.CENTER);

            //======== panel2 ========
            {
                panel2.setLayout(new FlowLayout());

                //---- fastBackButton ----
                fastBackButton.setText("<<");
                panel2.add(fastBackButton);

                //---- backButton ----
                backButton.setText("<");
                panel2.add(backButton);

                //---- forwardButton ----
                forwardButton.setText(">");
                panel2.add(forwardButton);

                //---- fastForwardButton ----
                fastForwardButton.setMinimumSize(new Dimension(78, 78));
                fastForwardButton.setText(">>");
                panel2.add(fastForwardButton);
            }
            panel1.add(panel2, BorderLayout.SOUTH);
        }
        contentPane.add(panel1, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem optionsMenuItem;
    private JMenuItem exitMenuItem;
    private JMenu helpMenu;
    private JMenuItem helpMenuItem;
    private JMenuItem aboutMenuItem;
    private JPanel panel1;
    private JScrollPane movesScrollPane;
    private JTextArea movesTextArea;
    private JPanel panel2;
    private JButton fastBackButton;
    private JButton backButton;
    private JButton forwardButton;
    private JButton fastForwardButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
