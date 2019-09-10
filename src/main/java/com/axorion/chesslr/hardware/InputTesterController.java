package com.axorion.chesslr.hardware;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputTesterController implements InputController {
    static final int BANK_SIZE = 8;

    GpioController gpio;
    MCP23017GpioProvider provider;
    int addr;
    GpioPinDigitalInput[] pinInput = new GpioPinDigitalInput[BANK_SIZE];
    List<GpioPinListenerDigital> listeners = new ArrayList<GpioPinListenerDigital>();
    Pin[] pins;
    Pin[] pinsBankA = {
            MCP23017Pin.GPIO_A0,
            MCP23017Pin.GPIO_A1,
            MCP23017Pin.GPIO_A2,
            MCP23017Pin.GPIO_A3,
            MCP23017Pin.GPIO_A4,
            MCP23017Pin.GPIO_A5,
            MCP23017Pin.GPIO_A6,
            MCP23017Pin.GPIO_A7,
    };
    Pin[] pinsBankB = {
            MCP23017Pin.GPIO_B0,
            MCP23017Pin.GPIO_B1,
            MCP23017Pin.GPIO_B2,
            MCP23017Pin.GPIO_B3,
            MCP23017Pin.GPIO_B4,
            MCP23017Pin.GPIO_B5,
            MCP23017Pin.GPIO_B6,
            MCP23017Pin.GPIO_B7,
    };

    public InputTesterController(GpioController gpio,int bus,int addr,int bank) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.addr = addr;
        provider = new MCP23017GpioProvider(bus,addr);
        if(bank == 0)
            setupPins(pinsBankA);
        else
            setupPins(pinsBankB);

    }

    protected void setupPins(Pin[] bank) {
        pins = bank;
        for(int i=0; i<bank.length; ++i) {
            pinInput[i] = gpio.provisionDigitalInputPin(provider,pins[i],pins[i].toString(),PinPullResistance.PULL_UP);
        }
    }

    public void addListener(GpioPinListenerDigital listener) {
        gpio.addListener(listener,pinInput);
    }

//    public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) {
//        final PinState state = event.getState();
//        final Pin pin = event.getPin().getPin();
//        final int index = findPinIndex(pin);
//
//        System.out.println("pin "+index+" state "+state);
//
//        for(List)
//    }

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
