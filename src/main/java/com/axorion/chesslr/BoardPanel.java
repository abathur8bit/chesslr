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

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {
    enum SquareStatus {
        NORMAL,
        WARNING,
        SELECTED,
        ERROR,
        MOVED
    }
    AppFrame parent;
    Image[] blackPieceImages;
    Image[] whitePieceImages;
    Image[] letters;

    int xoffset=0;  //40
    int yoffset=0;
    int squareWidth=60;     //80
    int squareHeight=60;

    SquareStatus[] squareStatus = new SquareStatus[64];

    int shadeWhite = 200;
    int shadeBlack = 100;
    Color whiteColor = new Color(shadeWhite,shadeWhite,shadeWhite);
    Color blackColor = new Color(shadeBlack,shadeBlack,shadeBlack);

    public BoardPanel(AppFrame parent) {
        this.parent = parent;
        Image pieceStrip = parent.loadImage("alpha_black.png");

        blackPieceImages = parent.loadImageStrip("alpha_black.png",6,pieceStrip.getWidth(null)/6,pieceStrip.getHeight(null),2);
        whitePieceImages = parent.loadImageStrip("alpha_white.png",6,pieceStrip.getWidth(null)/6,pieceStrip.getHeight(null),2);

        pieceStrip = parent.loadImage("alpha_black.png");
        letters = parent.loadImageStrip("letters.png",16,172/16,12,0);

        setPreferredSize(new Dimension(squareWidth*8,squareHeight*8));
        for(SquareStatus status : squareStatus) {
            status = SquareStatus.NORMAL;
        }
    }

    public void setSquareStatus(int boardIndex,SquareStatus status) {
        squareStatus[boardIndex] = status;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        renderBoard(g2,xoffset,yoffset,squareWidth);
        drawPieces(g2);
    }

    public void resetBoard() {
        for(int i=0; i<squareStatus.length; i++) {
            squareStatus[i] = SquareStatus.NORMAL;
        }
        repaint();
    }
    public void renderBoard(Graphics2D g2,int xoffset,int yoffset,int size) {
        Color currentColor = whiteColor;
        for(int y=0; y<8; y++) {
            if(y%2 == 0) {
                currentColor = whiteColor;
            } else {
                currentColor = blackColor;
            }

            for(int x=0; x<8; x++) {
                final int index = y*8+x;
                switch(squareStatus[index]) {
                    case NORMAL:
                        g2.setColor(currentColor);
                        break;
                    case ERROR:
                        setColor(g2,currentColor,Color.red);
                        break;
                    case WARNING:
                        setColor(g2,currentColor,Color.yellow);
                        break;
                    case SELECTED:
                        setColor(g2,currentColor,Color.green);
                        break;
                    case MOVED:
                        setColor(g2,currentColor,Color.cyan);
                        break;
                }
                g2.fillRect(xoffset+x*size,yoffset+y*size,size,size);
                if(currentColor == whiteColor) {
                    currentColor = blackColor;
                } else {
                    currentColor = whiteColor;
                }
            }
        }
        drawBoardOutline(g2,blackColor,xoffset,yoffset,size);
//        drawBoardLetters(g2,xoffset,yoffset,size);
    }

    protected void setColor(Graphics2D g2,Color currentColor,Color newColor) {
        if(currentColor == whiteColor)
            g2.setColor(newColor.darker());
        else
            g2.setColor(newColor.darker().darker());

    }

    public void drawBoardOutline(Graphics2D g2,Color blackColor,int xoffset,int yoffset,int size) {
        g2.setColor(blackColor);
        g2.drawRect(xoffset,yoffset,size*8,size*8);
        g2.drawRect(xoffset+1,yoffset+1,size*8-2,size*8-2);
        g2.drawRect(xoffset+2,yoffset+2,size*8-4,size*8-4);
    }

    public void drawBoardLetters(Graphics2D g2,int xoffset,int yoffset,int size) {
        int x=0,y=0;
        int width = letters[0].getWidth(null);
        int height = letters[0].getHeight(null);
//        g2.drawImage(letters[0],xoffset,yoffset,null);
        for(y=0; y<8; y++) {
            g2.drawImage(letters[7-y],xoffset+x*size-width-5,yoffset-5+y*size+size/2+height/2,null);
        }
        y = yoffset+size*8;
        for(x=0; x<8; x++) {
            g2.drawImage(letters[8+x],xoffset-5+x*size+size/2+width/2,y,null);
        }
    }

    public void drawPieces(Graphics2D g2) {
        for(int y=0; y<8; ++y) {
            for(int x=0; x<8; ++x) {
                drawPiece(parent.chessBoard.pieceAt(y*8+x),x,y,g2);
            }
        }
    }

    public void drawPiece(int piece,int x,int y,Graphics2D g2) {
        Image pieceImage = findPieceImage(piece);
        if(pieceImage != null) {
            g2.drawImage(pieceImage,x*squareWidth+xoffset+5,y*squareHeight+yoffset+5,squareWidth-10,squareHeight-10,null);
        } else {
//            g2.setColor(Color.RED);
//            g2.drawRect(x*width+xoffset+5,y*height+yoffset+5,width-10,height-10);
        }
    }

    public Image findPieceImage(int piece) {
        String whitePieceLetters = "PNBRQK";
        String blackPieceLetters = "pnbrqk";
        for(int i=0; i<blackPieceLetters.length(); ++i) {
            if(blackPieceLetters.charAt(i) == piece) {
                return blackPieceImages[i];
            }
        }
        for(int i=0; i<whitePieceLetters.length(); ++i) {
            if(whitePieceLetters.charAt(i) == piece) {
                return whitePieceImages[i];
            }
        }
        return null;
    }
}
