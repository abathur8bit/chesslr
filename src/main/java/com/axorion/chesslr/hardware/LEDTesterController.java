package com.axorion.chesslr.hardware;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

public class LEDTesterController implements LEDController {
    GpioController gpio;
    MCP23017GpioProvider provider;
    int addr;
    GpioPinDigitalOutput[] out = new GpioPinDigitalOutput[BANK_SIZE];
    static final int BANK_SIZE = 8;
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

    public LEDTesterController(GpioController gpio,int bus,int addr,int bank) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.addr = addr;
        provider = new MCP23017GpioProvider(bus,addr);
        if(bank == 0)
            setupPins(pinsBankA);
        else
            setupPins(pinsBankB);
    }

    @Override
    public void led(int led,boolean on) {
        PinState state = on?PinState.HIGH:PinState.LOW;
        out[led].setState(state);
    }

    @Override
    public boolean isOn(int led) {
        return out[led].getState() == PinState.HIGH;
    }

    protected void setupPins(Pin[] bank) {
        pins = bank;
        for(int i=0; i<bank.length; ++i) {
            out[i] = gpio.provisionDigitalOutputPin(provider,pins[i],pins[i].toString(),PinState.LOW);
        }
    }

}
