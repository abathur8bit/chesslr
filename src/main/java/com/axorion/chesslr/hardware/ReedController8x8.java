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

package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for reading switches on the chess board. To be notified when a switch state changes,
 * add a listener via addListener:
 *
 * <pre>
 * reedController.addListener(new GpioPinListenerDigital() {
 *     public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
 *         System.out.println("application gpio pin state change: " + event.getPin() + " = " + event.getState());
 *     }
 * });
 *</pre>
 *
 * Switch state of LOW is closed, HIGH switch is open.
 *
 * Uses MCP23017 ports 0x24-0x27 (100-111 binary).
 *
 * <pre>
 *           +----- -----+
 * GPB0 <--> |*1   -   28| <--> GPA7
 * GPB1 <--> |2        27| <--> GPA6
 * GPB2 <--> |3        26| <--> GPA5
 * GPB3 <--> |4    M   25| <--> GPA4
 * GPB4 <--> |5    C   24| <--> GPA3
 * GPB5 <--> |6    P   23| <--> GPA2
 * GPB6 <--> |7    2   22| <--> GPA1
 * GPB7 <--> |8    3   21| <--> GPA0
 *  VDD ---> |9    0   20| ---> INTA
 *  VSS ---> |10   1   19| ---> INTB
 *   NC ---- |11   7   18| ---> RESET
 *  SCL ---> |12       17| <--- A2
 *  SDA <--> |13       16| <--- A1
 *   NC ---- |14       15| <--- A0
 *           +-----------+
 *</pre>
 */
public class ReedController8x8 implements InputController,PieceListener,PieceListenerRow {
    static final int BASE_ADDRESS = 0x20;
    static final int NUM_ROWS = 8;
    static final int NUM_COLS = 8;

    GpioController gpio;
    ReedControllerRow[] reedControllerRows;
    int bus;
    List<PieceListener> listeners = new ArrayList<PieceListener>();

    public ReedController8x8(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        init(gpio,bus);
    }

    public void init(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.bus = bus;

        reedControllerRows = new ReedControllerRow[NUM_ROWS];
        for(int i=0; i<NUM_ROWS; i++) {
            reedControllerRows[i] = new ReedControllerRow(gpio,bus,BASE_ADDRESS+i);
            reedControllerRows[i].addRowListener(this);
        }
    }

    public void addListener(PieceListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PieceListener listener) {
        listeners.remove(listener);
    }

    public int findPinIndex(Pin p) {
        for(int y=0; y<NUM_ROWS; y++) {
            ReedControllerRow row = reedControllerRows[y];
            int index = row.findPinIndex(p);
            if(index != -1) {
                return y*8+index;
            }
        }
        return -1;
    }

    public boolean isSet(int p) {
        ReedControllerRow row = calcRow(p);
        return row.isSet(calcRowIndex(p));
    }

    public boolean isPieceDown(int p) {
        return isSet(p);
    }

    /** Returns if the state means a piece is down or not.
     *
     * @param state State to check.
     * @return true if piece is detected, false otherwise.
     */
    public boolean stateIsDown(PinState state) {
        return state == PinState.HIGH ? false:true;
    }

    public ReedControllerRow calcRow(int pin) {
        return reedControllerRows[pin/8];
    }

    public int calcRowIndex(int pin) {
        int row = pin/8;
        int i = pin-row*8;
        return i;
    }

    @Override
    public void pieceUp(int boardIndex) {
        System.out.printf("Got piece up on index %d\n",boardIndex);
    }

    @Override
    public void pieceDown(int boardIndex) {
        System.out.printf("Got piece down on index %d\n",boardIndex);
    }

    @Override
    public void pieceUp(int id,int index) {
        int boardIndex = (id-BASE_ADDRESS)*NUM_COLS+index;
        System.out.printf("Got piece up on row id %x index %d board index %d\n",id,index,boardIndex);
        for(PieceListener listener : listeners) {
            listener.pieceUp(boardIndex);
        }
    }

    @Override
    public void pieceDown(int id,int index) {
        int boardIndex = (id-BASE_ADDRESS)*NUM_COLS+index;
        System.out.printf("Got piece down on row id %x index %d board index %d\n",id,index,boardIndex);
        for(PieceListener listener : listeners) {
            listener.pieceDown(boardIndex);
        }
    }
}
