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

package examples;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;
import java.util.Arrays;

public class I2CTest {
    static final int BACKPACK_ADDR = 0x20;
    static final int HT16K33_BLINK_CMD = 0x80;
    static final int HT16K33_BLINK_DISPLAYON = 0x01;
    static final int HT16K33_BLINK_OFF = 0;
    static final int HT16K33_BLINK_2HZ  = 1;
    static final int HT16K33_BLINK_1HZ  = 2;
    static final int HT16K33_BLINK_HALFHZ  = 3;

    static final int HT16K33_CMD_BRIGHTNESS = 0xE0;

    static final int SEVENSEG_DIGITS = 5;

    public static void usage() {
        System.out.println("examples.I2CTest (www.8BitCoder.com)");
        System.out.println("Usage: examples.I2CTest device_address_hex");
        System.out.println("Usage: examples.I2CTest 70");
        System.out.println("Address is in hex without the 0x in front of it");
        System.exit(0);
    }

    static public void main(String[] args) throws Exception {
        String saddr = "0x70";
        if(args.length>0) {
            saddr = args[0];
        }
        if(args.length < 1) {
            usage();
        }
        System.out.println("I2C with 7 segment backpack test");

        // fetch all available busses
        try {
            int[] ids = I2CFactory.getBusIds();
            System.out.println("Found follow I2C busses: " + Arrays.toString(ids));

            // find available busses
            for (int number = I2CBus.BUS_0; number <= I2CBus.BUS_17; ++number) {
                try {
                    @SuppressWarnings("unused")
                    I2CBus bus = I2CFactory.getInstance(number);
                    System.out.println("Supported I2C bus "+number+" found");
                } catch(IOException exception) {
                    System.out.println("I/O error on I2C bus "+number+" occurred");
                } catch(I2CFactory.UnsupportedBusNumberException exception) {
                    System.out.println("Unsupported I2C bus "+number+" required");
                }
            }

            I2CBus bus = I2CFactory.getInstance(I2CBus.BUS_1);
            System.out.println("Getting device");
            int addr = Integer.parseInt(saddr,16);
            System.out.format("Checking address 0x%02X\n",addr);
            I2CDevice device = bus.getDevice(addr);

            System.out.println("Got the device");
//            System.out.println("Writting byte");
//            device.write((byte)0); // start at address $00
//
//            for (int i=0; i<8; i++) {
//                device.write((byte)0xFF);
//                device.write((byte)((byte)0xAA >> (byte)8));
//            }

            Thread.sleep(10000);
        } catch (IOException exception) {
            System.out.println("I/O error during fetch of I2C busses occurred "+exception);
            exception.printStackTrace();
        }
    }
}
