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

public class ReedControllerRow  implements GpioPinListenerDigital,InputController {
    static final int BANK_SIZE = 8;
    final int baseAddress;
    final GpioController gpio;
    final MCP23017GpioProvider provider;
    GpioPinDigitalInput[] pinInput = new GpioPinDigitalInput[BANK_SIZE];
    List<PieceListenerRow> listeners = new ArrayList<PieceListenerRow>();

    int[] bankAddress;
    Pin[] pins = {
            MCP23017Pin.GPIO_A0,
            MCP23017Pin.GPIO_A1,
            MCP23017Pin.GPIO_A2,
            MCP23017Pin.GPIO_A3,
            MCP23017Pin.GPIO_A4,
            MCP23017Pin.GPIO_A5,
            MCP23017Pin.GPIO_A6,
            MCP23017Pin.GPIO_A7,
    };
    long[] bounceTimer = new long[BANK_SIZE];
    PinState[] bounceState = new PinState[BANK_SIZE];
    AtomicBoolean[] debouncing = new AtomicBoolean[BANK_SIZE];
    long BOUNCE_DELAY = 100;

    public ReedControllerRow(GpioController gpio,int bus,int baseAddress) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.baseAddress = baseAddress;
        provider = new MCP23017GpioProvider(bus,baseAddress);

        bankAddress = new int[BANK_SIZE];
        for(int i = 0; i < BANK_SIZE; i++) {
            bankAddress[i] = baseAddress+i;
        }

        initDebouce();
        initPins();
        gpio.addListener(this,pinInput);
    }

    protected void initDebouce() {
        for(int i=0; i<BANK_SIZE; i++) {
            debouncing[i] = new AtomicBoolean(false);
        }
    }

    protected void initPins() {
        for(int i=0; i<BANK_SIZE; ++i) {
            pinInput[i] = gpio.provisionDigitalInputPin(provider,pins[i],pins[i].toString(),PinPullResistance.PULL_UP);
        }
    }

    @Override
    public void addListener(PieceListener listener) {
        //TODO Adding a piece listener is no longer needed for a reed controller row
    }

    public void addRowListener(PieceListenerRow listener) {
        listeners.add(listener);
    }

    public void removeListener(PieceListener listener) {
        listeners.remove(listener);
    }

    public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) {
        final PinState state = event.getState();
        final Pin pin = event.getPin().getPin();
        final int index = findPinIndex(pin);
//        System.out.printf("Handling event [%s] for pin index [%d]\n",event.getPin().getName(),index);

        if(index != -1 && getBounceState(index) != state && debouncing[index].get() == false) {
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
                            for(PieceListenerRow listener : listeners) {
//                                System.out.printf("row [%02X] notifying listener\n",baseAddress);
                                if(stateIsDown(event.getState())) {
                                    listener.pieceDown(baseAddress,index);
                                } else {
                                    listener.pieceUp(baseAddress,index);
                                }
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

    /** Returns if the state means a piece is down or not.
     *
     * @param state State to check.
     * @return true if piece is detected, false otherwise.
     */
    public boolean stateIsDown(PinState state) {
        return state == PinState.HIGH ? false:true;
    }

    @Override
    public boolean isSet(int p) {
        return stateIsDown(pinInput[p].getState());
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


}
