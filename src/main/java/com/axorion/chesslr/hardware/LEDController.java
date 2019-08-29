package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

public interface LEDController {
    /**
     * Initialize the controllers.
     *
     * @param gpio GPIO controller to use.
     * @param bus The i2c bus number. Typically 1.
     * @throws IOException If not ablet o communicate with i2c device(s).
     * @throws I2CFactory.UnsupportedBusNumberException If the bus number passed in isn't supported by the RPi.
     */
    void init(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException;

    /**
     * Turn the specified LED on or off.
     *
     * @param led LED index number.
     * @param on true to turn on, false to turn off.
     */
    public void led(int led,boolean on);

    /**
     * Return if specified LED is on.
     *
     * @param led LED index number.
     * @return true if LED is on, false otherwise.
     */
    public boolean isOn(int led);
}
