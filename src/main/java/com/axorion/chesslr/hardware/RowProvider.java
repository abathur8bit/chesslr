package com.axorion.chesslr.hardware;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

class RowProvider {
    static final int BANK_SIZE = 8;
    static final int NUM_ROWS = 8;
    static final int NUM_COLS = 8;

    GpioController gpio;
    MCP23017GpioProvider gpioProvider;
    GpioPinDigitalOutput[] outputPins;
    GpioPinDigitalInput[] inputPins;
    int bus,address;

    Pin[] inputPinNames = {
            MCP23017Pin.GPIO_A0,
            MCP23017Pin.GPIO_A1,
            MCP23017Pin.GPIO_A2,
            MCP23017Pin.GPIO_A3,
            MCP23017Pin.GPIO_A4,
            MCP23017Pin.GPIO_A5,
            MCP23017Pin.GPIO_A6,
            MCP23017Pin.GPIO_A7,
    };
    Pin[] outputPinNames = {
            MCP23017Pin.GPIO_B7,
            MCP23017Pin.GPIO_B6,
            MCP23017Pin.GPIO_B5,
            MCP23017Pin.GPIO_B4,
            MCP23017Pin.GPIO_B3,
            MCP23017Pin.GPIO_B2,
            MCP23017Pin.GPIO_B1,
            MCP23017Pin.GPIO_B0,
    };
    ProviderListener listener;

    public RowProvider(GpioController gpio,int bus,int address,ProviderListener listener) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.bus = bus;
        this.address = address;
        this.listener = listener;

        gpioProvider = new MCP23017GpioProvider(bus,address);
        outputPins = new GpioPinDigitalOutput[BANK_SIZE];
        inputPins = new GpioPinDigitalInput[BANK_SIZE];

        //output
        for(int i=0; i<BANK_SIZE; i++) {
            String name = String.format("OUTPUT %02X - %s",address,outputPinNames[i].toString());
            System.out.printf("pin %02d name [%s]\n",i,name);
            outputPins[i] = gpio.provisionDigitalOutputPin(gpioProvider,outputPinNames[i],name,PinState.LOW);
        }
        //input
        for(int i=0; i<BANK_SIZE; ++i) {
            String name = String.format("INPUT %02X - %s",address,inputPinNames[i].toString());
            System.out.printf("pin %02d name [%s]\n",i,name);
            inputPins[i] = gpio.provisionDigitalInputPin(gpioProvider,inputPinNames[i],name,PinPullResistance.PULL_UP);
        }

        gpio.addListener((GpioPinListenerDigital)event -> {
            final PinState state = event.getState();
            final Pin pin = event.getPin().getPin();
            final int index = findPinIndex(pin,inputPinNames);

            System.out.printf("Handling event [%s] for pin index [%d] state [%s] listener [%s]\n",event.getPin().getName(),index,state,listener==null?"null":listener.toString());

            if(listener != null) {
                if(stateIsDown(state))
                    listener.pieceDown(address,index);
                else
                    listener.pieceUp(address,index);
            }
        },inputPins);
    }

    public void setState(boolean state,int index) {
        gpio.setState(state,outputPins[index]);
    }

    public int findPinIndex(Pin p,Pin[] pins) {
        for(int i=0; i<pins.length; ++i) {
            if(pins[i].equals(p))
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
}

