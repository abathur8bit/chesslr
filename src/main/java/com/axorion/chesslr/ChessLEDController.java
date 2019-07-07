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

package com.axorion.chesslr;

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CBus;
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
public class ChessLEDController {
    static final int BANK_SIZE = 16;
    static final int BASE_ADDRESS = 0x20;

    GpioController gpio;
    MCP23017GpioProvider provider;
    GpioPinDigitalOutput[] out = new GpioPinDigitalOutput[BANK_SIZE];

    int[] bankAddress = {
            BASE_ADDRESS+0,
            BASE_ADDRESS+1,
            BASE_ADDRESS+2,
            BASE_ADDRESS+3,
    };
    Pin[] pins = {
          MCP23017Pin.GPIO_B0,
          MCP23017Pin.GPIO_B1,
          MCP23017Pin.GPIO_B2,
          MCP23017Pin.GPIO_B3,
          MCP23017Pin.GPIO_B4,
          MCP23017Pin.GPIO_B5,
          MCP23017Pin.GPIO_B6,
          MCP23017Pin.GPIO_B7,
          MCP23017Pin.GPIO_A0,
          MCP23017Pin.GPIO_A1,
          MCP23017Pin.GPIO_A2,
          MCP23017Pin.GPIO_A3,
          MCP23017Pin.GPIO_A4,
          MCP23017Pin.GPIO_A5,
          MCP23017Pin.GPIO_A6,
          MCP23017Pin.GPIO_A7,
    };

    public ChessLEDController() {}
    public ChessLEDController(GpioController gpio,int bus) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        provider = new MCP23017GpioProvider(bus,BASE_ADDRESS);
        initBank(0);
    }

    protected void initBank(int bank) {
        for(int i=0; i<16; ++i) {
            out[i] = gpio.provisionDigitalOutputPin(provider,pins[i],pins[i].toString(),PinState.LOW);
        }
    }

    public void led(int led,boolean on) {
        int bank = calcBank(led);
        int bankLed = calcBankLed(bank,led);
        set(bank,bankLed,on);
    }

    protected void set(int bank,int led,boolean on) {
        PinState state = on?PinState.HIGH:PinState.LOW;
        out[led].setState(state);
    }

    public int calcBank(int led) {
        return led/BANK_SIZE;
    }

    public int calcBankLed(int bank,int led) {
        return led-bank*BANK_SIZE;
    }
}
