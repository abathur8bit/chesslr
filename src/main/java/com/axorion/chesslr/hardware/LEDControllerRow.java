/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
 *
 * Created 2019-09-29
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

import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

/**
 * Class for manipulating the LEDs of a single row on the chess e-board.
 * 8 LEDs in a row. You can turn them on/off via index (0-7).
 *
 * Uses MCP23017 ports 0x20-0x27 (000-111 binary). LEDs are always on bank B.
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
public class LEDControllerRow implements LEDController {
    static final int BANK_SIZE = 8;
    final int baseAddress;
    final GpioController gpio;
    final MCP23017GpioProvider provider;
    final GpioPinDigitalOutput[] out = new GpioPinDigitalOutput[BANK_SIZE];

    int[] bankAddress;
    Pin[] pins = {
            MCP23017Pin.GPIO_B7,
            MCP23017Pin.GPIO_B6,
            MCP23017Pin.GPIO_B5,
            MCP23017Pin.GPIO_B4,
            MCP23017Pin.GPIO_B3,
            MCP23017Pin.GPIO_B2,
            MCP23017Pin.GPIO_B1,
            MCP23017Pin.GPIO_B0,
    };

    public LEDControllerRow(GpioController gpio,int bus,int baseAddress) throws IOException, I2CFactory.UnsupportedBusNumberException {
        this.gpio = gpio;
        this.baseAddress = baseAddress;
        provider = new MCP23017GpioProvider(bus,baseAddress);

        bankAddress = new int[BANK_SIZE];
        for(int i=0; i<BANK_SIZE; i++) {
            bankAddress[i] = baseAddress+i;
        }

        if(baseAddress == 0x21) {
            System.out.println("bank 0x21");
        }
        for(int i=0; i<BANK_SIZE; i++) {
            String name = baseAddress+"-"+pins[i].toString();
            System.out.printf("pin %02d name [%s]\n",i,name);
            out[i] = gpio.provisionDigitalOutputPin(provider,pins[i],name,PinState.LOW);
        }

//        for(int i=0; i<BANK_SIZE; i++) {
//            led(i,true);
//            led(i,false);
//        }
    }

    @Override
    public void led(int led,boolean on) {
        PinState state = on ? PinState.HIGH:PinState.LOW;
        out[led].setState(state);
    }

    @Override
    public boolean isOn(int led) {
        return out[led].getState() == PinState.HIGH;
    }
}
