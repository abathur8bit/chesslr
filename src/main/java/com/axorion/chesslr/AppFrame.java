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
import oled.Font;
import oled.OLEDDisplay;

import javax.swing.*;
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
    SimBoard simBoard;
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
        board.setSize(720,720);
        getContentPane().add(board,BorderLayout.CENTER);
        pack();

        simBoard = new SimBoard(this);

        initHardware();
    }

    private void initHardware() throws IOException, I2CFactory.UnsupportedBusNumberException {
        ledController = new ChessLEDController(gpio,I2CBus.BUS_1);
        reedController = new ChessReedController(gpio,I2CBus.BUS_1);
        reedController.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                boolean state = event.getState() == PinState.HIGH ? false:true;
                final int ledIndex = reedController.findPinIndex(event.getPin().getPin());

//                System.out.println("application gpio pin state change: " + event.getPin() + " = " + event.getState() + " led="+ledIndex);

                if(state) {
                    pieceDown(ledIndex);
                } else {
                    pieceUp(ledIndex);
                }

//                if(state) {
//                    display.clear();
//                    showBoard();
//                    final int x=9;
//                    textxy("B00:00:00",x,0);
//                    textxy("W00:00:00",x,1);
//                    textxy("led="+ledIndex,x,7);
//                    updateDisplay();
//                }

            }
        });
        display = new OLEDDisplay(I2CBus.BUS_1,0x3D);

    }
    public void startApp() throws IOException {
        resetBoard();
        startup();
        setVisible(true);
        simBoard.setLocation(getX()+getWidth(),getY());
//        simBoard.setVisible(true);
    }

    /**
     * Piece was lifted off the board.
     *
     * @param index location piece was picked up from.
     */
    public void pieceUp(int index) {
        int boardIndex = mapToBoard(index);
        gamePieceSelected = boardIndex;
        System.out.format("Piece at [%d] [%s] was selected\n",index,chessBoard.indexToBoard(boardIndex));
        blink(1,100,index);
        display.clear();
        showBoard();
        updateDisplay();
    }

    /**
     * Piece was put back onto the board at the given index.
     *
     * @param index location piece was dropped.
     */
    public void pieceDown(int index) {
        int boardIndex = mapToBoard(index);
        String playersMove = chessBoard.indexToBoard(gamePieceSelected)+chessBoard.indexToBoard(boardIndex);
        chessBoard.move(playersMove);
        gamePieceSelected = -1;
        System.out.format("Piece dropped at [%d] [%s] move=[%s]\n",index,chessBoard.indexToBoard(boardIndex),playersMove);
//        showBoard();
//        updateDisplay();
        repaint();
        board.repaint();
        blink(2,100,index);

    }

    public void blink(int count, long delay, int ledIndex) {
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

    public int mapToBoard(int index) {
        int[] map = {56,57,58,48,49,50,40,41,42};
        return map[index];
    }
    public void showBoard() {
        StringBuilder builder = new StringBuilder();
        int y=0,x=0;
        int index=0;
        int w= oled.Font.FONT_5X8.getOuterWidth()+1;
        int h=8;
        int tx;
        int ty;

        for(ty=0; ty<8; ty++) {
            builder.delete(0,8);
            for(tx=0; tx<8; tx++) {
                x=tx*w;
                y=ty*h;
                if(gamePieceSelected == index) {
                    display.clearRect(x,y,w,h,true);
                    display.drawChar(chessBoard.pieceAt(index++),oled.Font.FONT_5X8,x,y,false);
                } else {
                    display.clearRect(x,y,w,h,false);
                    display.drawChar(chessBoard.pieceAt(index++),oled.Font.FONT_5X8,x,y,true);
                }
            }
        }
    }

    public void showBoardComponents() throws IOException {
        display.clear();
        showBoard();
        final int x=9;
        int y=0;
        textxy("B00:00:00",x,y++);
        textxy("W00:00:00",x,y++);
        textxy("1 b3  d5",x,y++);
        textxy("2 O-O-O",x,y++);
        textxy("2.. O-O-O",x,y++);
        textxy("3 Nf3 Nf6",x,y++);
        textxy("4 g3  Bc5",x,y++);
        textxy("5 Bg2 O-O",x,y++);
        textxy("6 *",x,y++);
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

    public void textxy(String s,int tx,int ty) {
        int w= oled.Font.FONT_5X8.getOuterWidth()+1;
        int h=8;
        int x=tx*w;
        int y=ty*h;
        for(int i=0; i<s.length(); i++) {
            display.drawChar(s.charAt(i),Font.FONT_5X8,(tx+i)*w,ty*h,true);
        }
    }

    public void updateDisplay() {
        try {
            display.update();
        } catch(IOException ex) {
            //do nothing
        }
    }

    public void startup() throws IOException {
        drawRect(display,0,0,display.getWidth(),display.getHeight(),true);
        display.drawStringCentered("ChessLR",Font.FONT_5X8,display.getHeight()/2-4,true);
        display.update();

        try {
            for(int i = 0; i < 9; ++i) {
                ledController.led(i,true);
                Thread.sleep(100);
                ledController.led(i,false);
            }

//            for(int i=0; i<9; ++i) {
//                ledController.led(i,reedController.isSet(i));
//            }

        } catch(InterruptedException e) {
            //do nothing
        }
//        ledController.led(1,true);
//        ledController.blink(0,5000);

        display.clear();
        showBoardComponents();
        display.update();
    }

    /** Player has lifted a piece, or in other words, the sensor no longer sees a piece. */
    public void pieceUp(int x,int y) {
        final int idx = chessBoard.toIndex(x,y);
        final int piece = chessBoard.pieceAt(idx);
//        if(selectedIndex == -1) {
//            capturedIndex = idx;
//            System.out.println("piece ["+(char)chessBoard.pieceAt(idx)+"] removed during capture");
//            gamePieceCaptured = gameBoard[idx];
//            gameBoard[idx] = 0;
//        } else {
//            char c = (char)gameBoard[idx];
//            gamePieceSelected = gameBoard[idx];
//            gameBoard[idx] = 0;
//            System.out.println("piece ["+c+"] at ["+x+","+y+"] picked up");
//        }

        selectedIndex = idx;

//        if(gamePieceSelected != 0) {
//            System.out.println("piece ["+(char)gameBoard[idx]+"] removed during capture");
//            gamePieceCaptured = gameBoard[idx];
//            gameBoard[idx] = 0;
//        } else {
//            char c = (char)gameBoard[idx];
//            gamePieceSelected = gameBoard[idx];
//            gameBoard[idx] = 0;
//            System.out.println("piece ["+c+"] at ["+x+","+y+"] picked up");
//        }
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
        simBoard.boardInterface.reset();
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
        simBoard.boardInterface.reset();
        simBoard.repaint();
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
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
