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

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class ReedController3x3 implements GpioPinListenerDigital,InputController {
    static final int BANK_SIZE = 16;
    static final int BASE_ADDRESS = 0x21;

    GpioController gpio;
    MCP23017GpioProvider provider;
    GpioPinDigitalInput[] pinInput = new GpioPinDigitalInput[BANK_SIZE];
    List<GpioPinListenerDigital> listeners = new ArrayList<GpioPinListenerDigital>();

    int[] bankAddress = {
            BASE_ADDRESS+0,
            BASE_ADDRESS+1,
            BASE_ADDRESS+2,
            BASE_ADDRESS+3,
    };
    Pin[] pins = {
            MCP23017Pin.GPIO_A0,
            MCP23017Pin.GPIO_A1,
            MCP23017Pin.GPIO_A2,

            MCP23017Pin.GPIO_A3,
            MCP23017Pin.GPIO_A4,
            MCP23017Pin.GPIO_A5,

            MCP23017Pin.GPIO_A6,
            MCP23017Pin.GPIO_A7,
            MCP23017Pin.GPIO_B0,

            MCP23017Pin.GPIO_B1,
            MCP23017Pin.GPIO_B2,
            MCP23017Pin.GPIO_B3,
            MCP23017Pin.GPIO_B4,
            MCP23017Pin.GPIO_B5,
            MCP23017Pin.GPIO_B6,
            MCP23017Pin.GPIO_B7,
    };
    long[] bounceTimer = new long[64];
    PinState[] bounceState = new PinState[64];
    AtomicBoolean[] debouncing = new AtomicBoolean[64];
    long BOUNCE_DELAY = 100;

    public ReedController3x3() {}
    public ReedController3x3(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        init(gpio,bus);
    }

    public void init(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        provider = new MCP23017GpioProvider(bus,BASE_ADDRESS);
        for(int i=0; i<64; i++) {
            debouncing[i] = new AtomicBoolean(false);
        }
        initBank(0);
    }

    protected void initBank(int bank) {
        for(int i=0; i<16; ++i) {
            pinInput[i] = gpio.provisionDigitalInputPin(provider,pins[i],pins[i].toString(),PinPullResistance.PULL_UP);
        }
    }

    public void addListener(PieceListener listener) {
//        listeners.add(listener);
//        gpio.addListener(this,pinInput);
    }

    public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) {
        final PinState state = event.getState();
        final Pin pin = event.getPin().getPin();
        final int index = findPinIndex(pin);

        if(getBounceState(index) != state && debouncing[index].get() == false) {
            //debounce logic, state must hold it's state for a period of time
            debouncing[index].set(true);
            setBounceState(index,state);
            Runnable r = new Runnable() {
                public void run() {
//                    System.out.format("%d Debounce logic started\n",index);
                    final PinState prevState = state;    //remember what the state needs to be
                    try {
                        Thread.sleep(BOUNCE_DELAY);
//                        System.out.format("%d pin prev=[%s] current=[%s]\n",index,prevState,getBounceState(index));
                        if(getBounceState(index) == prevState) {
                            for(GpioPinListenerDigital listener : listeners) {
                                listener.handleGpioPinDigitalStateChangeEvent(event);
                            }
                        }
                    } catch(InterruptedException e) {}
                    setBounceState(index,null);
                    debouncing[index].set(false);
                }
            };
            new Thread(r).start();
        }  else {
            //just store the new state
            setBounceState(index,state);
        }
    }

    public void setBounceState(int index,PinState state) {
        synchronized(bounceState) {
            bounceState[index] = state;
        }
    }

    public PinState getBounceState(int index) {
        PinState state = null;
        synchronized(bounceState) {
            state = bounceState[index];
        }
        return state;
    }


    public void removeListener(GpioPinListenerDigital listener) {
        gpio.removeListener(listener);
    }

    public int findPinIndex(Pin p) {
        for(int i=0; i<pins.length; ++i) {
            if(pins[i].equals(p))
                return i;
        }
        return -1;
    }

    /** Return the pin index for the given name, or -1 if not found. */
    public int findPinIndex(String pinName) {
        for(int i=0; i<pins.length; ++i) {
            if(pins[i].toString().equals(pinName))
                return i;
        }
        return -1;
    }

    public PinState getState(int p) {
        return pinInput[p].getState();
    }

    public boolean isSet(int p) {
        return stateIsDown(pinInput[p].getState());
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
}
