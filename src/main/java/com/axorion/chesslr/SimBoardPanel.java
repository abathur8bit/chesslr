package com.axorion.chesslr;

import javax.swing.*;
import java.awt.*;

public class SimBoardPanel extends JPanel {
    protected Color whiteSquare = Color.LIGHT_GRAY;
    protected Color blackSquare = Color.DARK_GRAY;
    protected BoardInterface boardInterface;

    public SimBoardPanel(BoardInterface boardInterface) {
        this.boardInterface = boardInterface;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        drawBoard(g2);
        drawLEDs(g2);
        drawSensors(g2);
    }

    protected void drawBoard(Graphics2D g2) {
        int sw = getWidth()/8;
        int sh = getHeight()/8;
        Color currentColor = whiteSquare;
        for(int y=0; y<8; ++y) {
            if(y % 2 != 0) {
                currentColor = blackSquare;
            } else {
                currentColor = whiteSquare;
            }

            for(int x = 0; x < 8; ++x) {
                g2.setColor(currentColor);
                g2.fillRect(x*sw,y*sh,sw,sh);
                if(currentColor == whiteSquare) {
                    currentColor = blackSquare;
                } else {
                    currentColor = whiteSquare;
                }
            }
        }
    }

    protected void drawLEDs(Graphics2D g2) {
        int size = getWidth()/8/8;
        int sw = getWidth()/8;
        int sh = getHeight()/8;
        for(int y=0; y<8; ++y) {
            for(int x = 0; x < 8; ++x) {
                g2.setColor(boardInterface.isSetLED(x,y) ? Color.YELLOW : Color.ORANGE);
                g2.fillOval(x*sw+size,y*sh+sh-size-size,size,size);
            }
        }
    }

    protected void drawSensors(Graphics2D g2) {
        for(int y=0; y<8; ++y) {
            for(int x = 0; x < 8; ++x) {
                drawSensor(g2,x,y,boardInterface.isOccupied(x,y));
            }
        }
    }

    protected void drawSensor(Graphics2D g2,int x,int y,boolean on) {
        int size = getWidth()/8/4;
        int squareWidth = getWidth()/8;
        int squareHeight = getHeight()/8;
        g2.setColor(Color.BLACK);

        if(on) {
            size /= 2;
            g2.setColor(Color.RED);
            g2.fillRect(x*squareWidth+squareWidth/2-size/2,y*squareHeight+squareHeight/2-size/2,size,size);
        }
    }

    protected void drawPieces(Graphics2D g2) {

    }

}
