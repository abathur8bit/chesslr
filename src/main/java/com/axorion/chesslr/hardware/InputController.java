package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

public interface InputController {
    void addListener(PieceListener listener);
    int findPinIndex(Pin pin);
    boolean stateIsDown(PinState state);
    boolean isSet(int pinIndex);
}