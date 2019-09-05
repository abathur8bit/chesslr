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

/** Controls all the chess board hardware. Takes care of mapping a 3x3 proto board to a full 8x8 coords and visa versa. */
public class BoardController {
    LEDController ledController;
    InputController reedController;
    ArrayList<PieceListener> pieceListeners = new ArrayList<PieceListener>();

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
    public boolean isOn(int boardIndex) {
        return ledController.isOn(mapToPin(boardIndex));
    }

    public boolean isSet(int boardIndex) {
        return reedController.isSet(mapToPin(boardIndex));
    }

    public void addListener(PieceListener listener) {
        pieceListeners.add(listener);
    }

    private void pieceUp(int pinIndex) {
        final int boardIndex = mapToBoard(pinIndex);
        for(PieceListener listener : pieceListeners) {
            listener.pieceUp(boardIndex);
        }
    }

    private void pieceDown(int pinIndex) {
        final int boardIndex = mapToBoard(pinIndex);
        for(PieceListener listener : pieceListeners) {
            listener.pieceDown(boardIndex);
        }
    }

    public int mapToBoard(int pinIndex) {
//        int[] pinToBoardMap = {3,4,5,11,12,13,19,20,21}; //upper middle
        int[] pinToBoardMap = {0,1,2,8,9,10,16,17,18}; //top left
//        int[] pinToBoardMap = {56,57,58,48,49,50,40,41,42}; //bottom leff
        return pinToBoardMap[pinIndex];
    }

    public int mapToPin(int boardIndex) {
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

    }

}
