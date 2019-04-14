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

import com.sun.corba.se.impl.orbutil.graph.Graph;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BoardPanel extends JPanel {
    AppFrame parent;
    Image boardImage;
    Image[] blackPieceImages;
    Image[] whitePieceImages;

    public BoardPanel(AppFrame parent) {
        this.parent = parent;
        boardImage = parent.loadImage("board4.png");
        Image pieceStrip = parent.loadImage("alpha_black.png");
        blackPieceImages = parent.loadImageStrip("alpha_black.png",6,pieceStrip.getWidth(null)/6,pieceStrip.getHeight(null),2);
        whitePieceImages = parent.loadImageStrip("alpha_white.png",6,pieceStrip.getWidth(null)/6,pieceStrip.getHeight(null),2);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.drawImage(boardImage,0,0,null);
        drawPieces(g2);
    }

    public void drawPieces(Graphics2D g2) {
        if(parent.simBoard.boardInterface.isOccupied(0,0)) {
            for(int y=0; y<8; ++y) {
                for(int x=0; x<8; ++x) {
                    drawPiece(parent.gameBoard[y*8+x],x,y,g2);
                }
            }
        }
    }

    public void drawPiece(int piece,int x,int y,Graphics2D g2) {
        int xoffset = 40,yoffset=40;
        int width = 80,height = 80;
        Image pieceImage = findPieceImage(piece);
        if(pieceImage != null) {
            g2.drawImage(pieceImage,x*width+xoffset+5,y*height+yoffset+5,width-10,height-10,null);
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
