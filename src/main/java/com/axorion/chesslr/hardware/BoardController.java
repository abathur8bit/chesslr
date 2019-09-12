/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
 *
 * Created 2019-09-05
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

package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Controls all the chess board hardware. User should assume an 8x8 board.
 * Controller takes care of mapping a 3x3 proto board to a full 8x8
 * coords and visa versa.
 *
 * - LED on and flash means waiting for a piece to be put down.
 * - LED off and flashing means wait for piece to be removed.
 *
 * <pre>
 *    a  b  c  d  e  f  g  h
 * 8  00 01 02 03 04 05 06 07  8
 * 7  08 09 10 11 12 13 14 15  7
 * 6  16 17 18 19 20 21 22 23  6
 * 5  24 25 26 27 28 29 30 31  5
 * 4  32 33 34 35 36 37 38 39  4
 * 3  40 41 42 43 44 45 46 47  3
 * 2  48 49 50 51 52 53 54 55  2
 * 1  56 57 58 59 60 61 62 63  1
 *    a  b  c  d  e  f  g  h
 * </pre>
 */
public class BoardController {
    LEDController ledController;
    InputController reedController;
    ArrayList<PieceListener> pieceListeners = new ArrayList<PieceListener>();

    FlashThread flashThread;

    public BoardController(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        ledController = new ChessLEDController(gpio,bus);
        reedController = new ChessReedController(gpio,bus);
        reedController.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                final Pin pin = event.getPin().getPin();
                final int index = reedController.findPinIndex(pin);

                if(reedController.stateIsDown(event.getState())) {
                    pieceDown(index);
                } else {
                    pieceUp(index);
                }
            }
        });
        flashThread = new FlashThread(ledController);
        flashThread.start();
    }

    public void flashOn(int ledIndex) {
        flashThread.addLed(mapToPin(ledIndex));
    }

    public void flashOff(int ledIndex) {
        flashThread.removeLed(mapToPin(ledIndex));
    }

    public InputController getInputController() {
        return reedController;
    }

    public LEDController getLedController() {
        return ledController;
    }

    public void led(int squareIndex,boolean on) {
        ledController.led(mapToPin(squareIndex),on);

    }

    /** Return if an LED is on at the specified index. */
    public boolean isLEDOn(int boardIndex) {
        return ledController.isOn(mapToPin(boardIndex));
    }

    /**
     * Return if a piece is down at the specified index.
     *
     * @param boardIndex Index to check.
     * @return true if there is a piece on the square, false otherwise.
     */
    public boolean hasPiece(int boardIndex) {
        return reedController.isSet(mapToPin(boardIndex));
    }

    public void addListener(PieceListener listener) {
        pieceListeners.add(listener);
    }

    /** Calls any listeners to tell them about a piece up event. */
    private void pieceUp(int pinIndex) {
        final int boardIndex = mapToBoard(pinIndex);
        for(PieceListener listener : pieceListeners) {
            listener.pieceUp(boardIndex);
        }
    }

    /** Call any listeners to tell them about a piece down event. */
    private void pieceDown(int pinIndex) {
        final int boardIndex = mapToBoard(pinIndex);
        for(PieceListener listener : pieceListeners) {
            listener.pieceDown(boardIndex);
        }
    }

    private int mapToBoard(int pinIndex) {
//        int[] pinToBoardMap = {3,4,5,11,12,13,19,20,21}; //upper middle
        int[] pinToBoardMap = {
                0,1,2,
                8,9,10,
                16,17,18}; //top left
//        int[] pinToBoardMap = {56,57,58,48,49,50,40,41,42}; //bottom leff
        if(pinIndex>pinToBoardMap.length) {
            return 0;
        }
        return pinToBoardMap[pinIndex];
    }

    /** Map a board index (0-63) to the correct pin #. */
    private int mapToPin(int boardIndex) {
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

    public void resetBoard() {
        for(int i=0; i<64; i++) {
            led(i,false);
        }
        flashThread.reset();
    }

    public void blink(final int count, final long delay, final boolean leaveOn, final int boardIndex) {
        new Thread(() -> {
                try {
                    if(isLEDOn(boardIndex)) {
                        led(boardIndex,false);
                        Thread.sleep(delay);
                    }
                    for(int i = 0; i < count; i++) {
                        led(boardIndex,true);
                        Thread.sleep(delay);
                        led(boardIndex,false);
                        Thread.sleep(delay);
                    }
                    led(boardIndex,leaveOn);
                } catch(InterruptedException e) {
                    led(boardIndex,leaveOn);
                }
            }).start();
    }
}
