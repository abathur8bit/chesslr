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
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CBus;
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
public class BoardController implements PieceListener, ProviderListener {

    final int BUS = I2CBus.BUS_1;
    static final int BASE_ADDRESS = 0x20;
    static final int BANK_SIZE = 8;
    static final int NUM_ROWS = 8;
    static final int NUM_COLS = 8;

    GpioController gpio;
    LEDController ledController;
    InputController reedController;
    ArrayList<PieceListener> pieceListeners = new ArrayList<PieceListener>();

    FlashThread flashThread;

    int[] boardToPinMap3x3 = {
            0,1,2,0,0,0,0,0,
            3,4,5,0,0,0,0,0,
            6,7,8,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0
    };

    int[] pinToBoardMap3x3 = {
            0,1,2,
            8,9,10,
            16,17,18}; //top left

    /**
     * Normally the reeds go from 0 through 63 in order. But if you wired them in the
     * wrong order, this index will be used to map them from the pin index to the board
     * index.
     */
    int[] remap = {
             0, 1, 2, 3, 4, 5, 6, 7,
             8, 9,10,11,12,13,14,15,
            16,17,18,19,20,21,22,23,
            24,25,26,27,28,29,30,31,
            32,33,34,36,35,37,38,39,
            40,41,42,43,44,45,46,47,
            48,49,50,51,52,53,54,55,
            56,57,58,59,60,61,62,63
    };


    RowProvider[] rowProviders = new RowProvider[NUM_ROWS];

    class BoardLEDController implements LEDController {
        @Override
        public void led(int boardIndex,boolean on) {
            int row = boardIndex/8;
            int col = boardIndex-row*8;
            RowProvider provider = rowProviders[row];
            provider.setState(on,col);
        }

        @Override
        public boolean isOn(int boardIndex) {
            int row = boardIndex/8;
            int col = boardIndex-row*8;
            RowProvider provider = rowProviders[row];
            return provider.outputPins[col].getState().isHigh();
        }
    }

    class BoardInputController implements InputController {
        @Override
        public void addListener(PieceListener listener) {
            pieceListeners.add(listener);
        }

        @Override
        public int findPinIndex(Pin pin) {
            return 0;   //not used
        }

        @Override
        /** Returns if the state means a piece is down or not.
         *
         * @param state State to check.
         * @return true if piece is detected, false otherwise.
         */
        public boolean stateIsDown(PinState state) {
            return state == PinState.HIGH ? false:true;
        }

        @Override
        public boolean isSet(int boardIndex) {
            boardIndex = remap[boardIndex];     //to handle reed switches that are wired incorrectly
            int row = boardIndex/8;
            int col = boardIndex-row*8;
            RowProvider provider = rowProviders[row];
            return provider.inputPins[col].getState().isLow();
        }
    }

    public BoardController(LEDController led,InputController input) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.ledController = led;
        this.reedController = input;
        flashThread = new FlashThread(ledController);
        flashThread.start();
    }

    public BoardController(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        if(gpio == null) {
            // use simulated board
        } else {
            ledController = new BoardLEDController();
            reedController = new BoardInputController();
            for(int i = 0; i < NUM_ROWS; i++) {
                rowProviders[i] = new RowProvider(gpio,BUS,BASE_ADDRESS+i,this);
            }
        }
        flashThread = new FlashThread(ledController);
        flashThread.start();
    }

    public void flashOn(int ledIndex) {
        flashThread.addLed(ledIndex);
    }

    public void flashOff(int ledIndex) {
        flashThread.removeLed(ledIndex);
    }

    public InputController getInputController() {
        return reedController;
    }

    public LEDController getLedController() {
        return ledController;
    }

    public void led(int squareIndex,boolean on) {
        if(squareIndex == -1) {
            for(int i = 0; i < 64; i++) {
                ledController.led(i,on);
            }
        } else {
            ledController.led(squareIndex,on);
        }
    }

    /** Return if an LED is on at the specified index. */
    public boolean isLEDOn(int boardIndex) {
        return ledController.isOn(boardIndex);
    }

    /**
     * Return if a piece is down at the specified index.
     *
     * @param boardIndex Index to check.
     * @return true if there is a piece on the square, false otherwise.
     */
    public boolean hasPiece(int boardIndex) {
        return reedController.isSet(boardIndex);
    }

    public void addListener(PieceListener listener) {
        pieceListeners.add(listener);
    }

    @Override
    public void pieceUp(int address,int index) {
        final int pinIndex = (address-BASE_ADDRESS)*8+index;
        pieceUp(remap[pinIndex]);
    }

    @Override
    public void pieceDown(int address,int index) {
        final int pinIndex = (address-BASE_ADDRESS)*8+index;
        pieceDown(remap[pinIndex]);
    }


    /** Calls any listeners to tell them about a piece up event. */
    public void pieceUp(int boardIndex) {
        for(PieceListener listener : pieceListeners) {
            listener.pieceUp(boardIndex);
        }
    }

    /** Call any listeners to tell them about a piece down event. */
    public void pieceDown(int boardIndex) {
        for(PieceListener listener : pieceListeners) {
            listener.pieceDown(boardIndex);
        }
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
                } catch(InterruptedException e) {
                    //do nothing
                }
            led(boardIndex,leaveOn);
            }).start();
    }
}
