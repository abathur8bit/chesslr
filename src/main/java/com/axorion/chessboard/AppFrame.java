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

package com.axorion.chessboard;

import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import javax.swing.*;

/**
 * @author Lee Patterson
 */
public class AppFrame extends JFrame implements InvocationHandler {
    String whitePieceLetters = "PNBRQK";
    String blackPieceLetters = "pnbrqk";
    BoardPanel board;
    int[] gameBoard = new int[64];
    int gamePieceSelected;
    int gamePieceCaptured;
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

    public AppFrame(String title) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
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
        simBoard.setLocation(getX()+getWidth(),getY());

        resetBoard();

        simBoard.setVisible(true);
    }

    public void pieceSelected(int x,int y) {
        final int idx= y*8+x;
        if(gamePieceSelected != 0) {
            System.out.println("piece ["+(char)gameBoard[idx]+"] removed during capture");
            gamePieceCaptured = gameBoard[idx];
            gameBoard[idx] = 0;
        } else {
            char c = (char)gameBoard[idx];
            gamePieceSelected = gameBoard[idx];
            gameBoard[idx] = 0;
            System.out.println("piece ["+c+"] at ["+x+","+y+"] picked up");
        }
    }

    public void pieceDropped(int x,int y) {
        gameBoard[y*8+x] = gamePieceSelected;
        System.out.println("piece ["+((char)gamePieceSelected)+"] at ["+x+","+y+"] dropped "+(gamePieceCaptured!=0 ? "completing capture":""));
        gamePieceSelected = 0;
        gamePieceCaptured = 0;
    }

    public void resetBoard() {
        String boardLetters =
                "rnbqkbnr"+
                "pppppppp"+
                "        "+
                "        "+
                "        "+
                "        "+
                "PPPPPPPP"+
                "RNBQKBNR";

        simBoard.boardInterface.reset();
        for(int i=0; i<64; ++i) {
            gameBoard[i] = boardLetters.charAt(i) == ' ' ? 0:boardLetters.charAt(i);
            if(gameBoard[i]>0) {
                simBoard.boardInterface.setOccupied(i,true);
            }
        }
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

    public ChessPrefs getPrefs() {
        return prefs;
    }

    private void aboutMenuItemActionPerformed(ActionEvent e) {
        AboutDialog dlg = getAboutDialog();
        if(!dlg.isVisible()) {
            dlg.setVisible(true);
        }
    }

    public AboutDialog getAboutDialog() {
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
