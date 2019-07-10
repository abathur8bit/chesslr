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

import com.axorion.chesslr.ChessLEDController;
import com.axorion.chesslr.ChessReedController;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;

public class ThreeByThree {
    final GpioController gpio = GpioFactory.getInstance();
    ChessLEDController ledController;
    ChessReedController reedController;
    public static void main(String[] args) throws Exception {
        ThreeByThree app = new ThreeByThree();
        app.startup();
        app.play();
    }

    public ThreeByThree() throws Exception {
        ledController = new ChessLEDController(gpio,I2CBus.BUS_1);
        reedController = new ChessReedController(gpio,I2CBus.BUS_1);
        reedController.addListener(new GpioPinListenerDigital() {
            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
                boolean state = event.getState() == PinState.HIGH ? false:true;
                int ledIndex = reedController.findPinIndex(event.getPin().getPin());
                ledController.led(ledIndex,state);

                System.out.println("application gpio pin state change: " + event.getPin() + " = " + event.getState() + " led="+ledIndex);
            }
        });
    }

    public void startup() {
        try {
            for(int i = 0; i < 9; ++i) {
                ledController.led(i,true);
                Thread.sleep(100);
                ledController.led(i,false);
            }

            for(int i=0; i<9; ++i) {
                ledController.led(i,reedController.isSet(i));
            }

        } catch(InterruptedException e) {
            //do nothing
        }
//        ledController.led(1,true);
//        ledController.blink(0,5000);
    }

    public void play() {
        try {
            while(true) {
//                System.out.println("tick");
                Thread.sleep(100);
            }
        } catch(InterruptedException e) {
            //do nothing
        }

        ledController.led(0,false);
    }
}

