/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
 *
 * Created 2019-09-05
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

import java.util.ArrayList;
import java.util.List;

/** Flashes LEDs. */
public class FlashThread extends Thread {
//    enum FlashType {
//        BLINK,  //blink at same rate
//        TWICE,  //blink twice, then longer delay, repeat
//        THRICE, //blink three times, then longer delay, repeat
//    }
//
//    class Flasher {
//        int ledIndex;
//        int type;
//    }
    LEDController controller;
    final List<Integer> ledList = new ArrayList<>();
    boolean running = false;

    public FlashThread(LEDController controller) {
        this.controller = controller;
    }

    public void addLed(int ledIndex) {
        synchronized(ledList) {
            ledList.add(ledIndex);
        }
    }

    public void removeLed(int ledIndex) {
        synchronized(ledList) {
            for(Integer index : ledList) {
                if(index == ledIndex) {
                    ledList.remove(index);
                    controller.led(ledIndex,false);
                    break;
                }
            }
        }
    }

    public void reset() {
        synchronized(ledList) {
            ledList.clear();
        }
    }

    public void run() {
        running = true;
        while(running) {
            try {
                Thread.sleep(100);
                led(true);
                Thread.sleep(100);
                led(false);
            } catch(InterruptedException e) {}
        }
    }

    protected void led(boolean on) {
        synchronized(ledList) {
            for(Integer ledIndex : ledList) {
                controller.led(ledIndex,on);
            }
        }
    }

    public void stopFlashing() {
        running = false;
    }
}
