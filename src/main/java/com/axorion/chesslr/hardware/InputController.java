package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

public interface InputController {
    void init(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException;
    void addListener(GpioPinListenerDigital listener);
    int findPinIndex(Pin pin);
    boolean stateIsDown(PinState state);
    boolean isSet(int pinIndex);
}