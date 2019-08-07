/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
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

import java.io.IOException;

public class BoardApp {
    ChessBoard gameBoard;
    final GpioController gpio = GpioFactory.getInstance();
    ChessLEDController ledController;
    ChessReedController reedController;
    OLEDDisplay display;
    int gamePieceSelected=0;
    int gamePieceCaptured=0;

    public static void main(String[] args) throws Exception {
        BoardApp app = new BoardApp();
        app.startup();
        app.play();
    }


    public BoardApp() throws I2CFactory.UnsupportedBusNumberException, IOException {
        gameBoard = new ChessBoard();
        ledController = new ChessLEDController(gpio,I2CBus.BUS_1);
        reedController = new ChessReedController(gpio,I2CBus.BUS_1);
        reedController.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                boolean state = event.getState() == PinState.HIGH ? false:true;
                int ledIndex = reedController.findPinIndex(event.getPin().getPin());

//                System.out.println("application gpio pin state change: " + event.getPin() + " = " + event.getState() + " led="+ledIndex);

                if(state) {
                    pieceDropped(ledIndex);
                } else {
                    pieceSelected(ledIndex);
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

    public void pieceSelected(int index) {
        int boardIndex = mapToBoard(index);
        gamePieceSelected = boardIndex;
        System.out.format("Piece at [%d] [%s] was selected\n",index,gameBoard.indexToBoard(boardIndex));
        blink(1,100,index);
        display.clear();
        showBoard();
        updateDisplay();
    }

    public void pieceDropped(int index) {
        int boardIndex = mapToBoard(index);
        String playersMove = gameBoard.indexToBoard(gamePieceSelected)+gameBoard.indexToBoard(boardIndex);
        gameBoard.move(playersMove);
        gamePieceSelected = -1;
        System.out.format("Piece dropped at [%d] [%s] move=[%s]\n",index,gameBoard.indexToBoard(boardIndex),playersMove);
        showBoard();
        updateDisplay();
        blink(2,100,index);
    }

    public int mapToBoard(int index) {
        int[] map = {56,57,58,48,49,50,40,41,42};
        return map[index];
    }
    public void showBoard() {
        StringBuilder builder = new StringBuilder();
        int y=0,x=0;
        int index=0;
        int w= Font.FONT_5X8.getOuterWidth()+1;
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
                    display.drawChar(gameBoard.pieceAt(index++),Font.FONT_5X8,x,y,false);
                } else {
                    display.clearRect(x,y,w,h,false);
                    display.drawChar(gameBoard.pieceAt(index++),Font.FONT_5X8,x,y,true);
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
        int w= Font.FONT_5X8.getOuterWidth()+1;
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

    public void play() {
        try {
            while(true) {
//                System.out.println("tick");
                Thread.sleep(100);
            }
        } catch(InterruptedException e) {
            //do nothing
        }

        ledController.led(0,false);
    }

}
