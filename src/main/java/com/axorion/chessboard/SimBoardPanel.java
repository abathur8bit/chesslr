package com.axorion.chessboard;

import javax.swing.*;
import java.awt.*;

public class SimBoardPanel extends JPanel {
    protected Color whiteSquare = Color.LIGHT_GRAY;
    protected Color blackSquare = Color.DARK_GRAY;
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        drawBoard(g2);
        drawLEDs(g2);
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
        Color currentColor = Color.orange;
        g2.setColor(currentColor);
        for(int y=0; y<8; ++y) {
            for(int x = 0; x < 8; ++x) {
                g2.fillOval(x*sw+size,y*sh+sh-size-size,size,size);
            }
        }
    }

}
