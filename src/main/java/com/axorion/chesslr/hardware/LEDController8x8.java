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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

/**
 * Class for manipulating the LEDs on the chess board. 8x8 for a total of
 * 64 LEDs controlled. You can turn them on/off via index (0-63) or x,y position.
 *
 * Uses MCP23017 ports 0x20-0x23 (000-011 binary).
 *
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
 */
public class LEDController8x8 implements LEDController {

    static final int BASE_ADDRESS = 0x20;

    GpioController gpio;
    LEDControllerRow[] ledControllerRows;
    int bus;

    public LEDController8x8() {}
    public LEDController8x8(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        init(gpio,bus);
    }

    public void init(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.bus = bus;
        ledControllerRows = new LEDControllerRow[8];
        for(int i=0; i<8; i++) {
            ledControllerRows[i] = new LEDControllerRow(gpio,bus,BASE_ADDRESS+i);
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                //do nothing
            }
        }
    }

    /**
     * Turn the specified LED on or off.
     *
     * @param led LED index number.
     * @param on true to turn on, false to turn off.
     */
    public void led(int led,boolean on) {
        LEDControllerRow row = calcRow(led);
        row.led(calcRowLed(led),on);
    }

    public void blink(int led) {
    }
    public void blink(int led,long duration) {
    }

    public boolean isOn(int led) {
        LEDControllerRow row = calcRow(led);
        return row.isOn(calcRowLed(led));
    }

    public LEDControllerRow calcRow(int led) {
        return ledControllerRows[led/8];
    }

    public int calcRowLed(int led) {
        int row = led/8;
        int i = led-row*8;
        return i;
    }
}
