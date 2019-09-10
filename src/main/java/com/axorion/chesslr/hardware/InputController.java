package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public interface InputController {
    void addListener(GpioPinListenerDigital listener);
    int findPinIndex(Pin pin);
    boolean stateIsDown(PinState state);
    boolean isSet(int pinIndex);
}