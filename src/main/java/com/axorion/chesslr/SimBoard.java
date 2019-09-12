/*
 * Created by JFormDesigner on Fri Apr 12 22:17:42 EDT 2019
 */

package com.axorion.chesslr;

import com.axorion.chesslr.hardware.InputController;
import com.axorion.chesslr.hardware.LEDController;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Lee Patterson
 */
public class SimBoard extends JDialog implements LEDController,InputController {
    AppFrame parent;
    SimBoardPanel boardPanel;
    SimBoardInterface boardInterface = new SimBoardInterface();

    public SimBoard(AppFrame owner) {
        super(owner);
        this.parent = owner;
        initComponents();

        boardPanel = new SimBoardPanel(boardInterface);
        setSize(new Dimension(400,400));
        getContentPane().add(boardPanel,BorderLayout.CENTER);

        boardInterface.reset();
    }

    public void reset() {
        boardInterface.reset();
        repaint();
    }

    private void mouseClicked(MouseEvent e) {
        int mx = e.getX()-getInsets().left;
        int my = e.getY()-getInsets().top;
        int sx = mx / (boardPanel.getWidth()/8);
        int sy = my / (boardPanel.getHeight()/8);

        System.out.println("clicked on square ["+sx+","+sy+"] mouse ["+mx+","+my+"]");

        if(boardInterface.isOccupied(sx,sy)) {
            parent.chessBoardController.pieceUp(sy*8+sx);
        } else {
            parent.chessBoardController.pieceDown(sy*8+sx);
        }
        boardInterface.setOccupied(sx,sy,!boardInterface.isOccupied(sx,sy));
        boardPanel.repaint();
        parent.repaint();
    }

    @Override
    public void addListener(GpioPinListenerDigital listener) {

    }

    @Override
    public int findPinIndex(Pin pin) {
        return 0;
    }

    @Override
    public boolean stateIsDown(PinState state) {
        return false;
    }

    @Override
    public boolean isSet(int pinIndex) {
        return boardInterface.isOccupied(pinIndex);
    }

    @Override
    public void led(int led,boolean on) {
        boardInterface.setLED(led,on);
        repaint();
    }

    @Override
    public boolean isOn(int led) {
        return boardInterface.isSetLED(led);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license

        //======== this ========
        setTitle("Sim Board");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SimBoard.this.mouseClicked(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
