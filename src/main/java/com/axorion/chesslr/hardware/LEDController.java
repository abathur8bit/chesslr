package com.axorion.chesslr.hardware;

public interface LEDController {
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
