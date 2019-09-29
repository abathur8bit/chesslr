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

import com.axorion.chesslr.hardware.LEDController3x3;
import junit.framework.TestCase;

public class LEDController3x3Test extends TestCase {

    public void testCalcBank() {
        LEDController3x3 lc = new LEDController3x3();
        assertEquals(0,lc.calcBank(0));
        assertEquals(0,lc.calcBank(1));
        assertEquals(0,lc.calcBank(5));
        assertEquals(0,lc.calcBank(10));
        assertEquals(0,lc.calcBank(15));
        assertEquals(1,lc.calcBank(16));
        assertEquals(1,lc.calcBank(20));
        assertEquals(1,lc.calcBank(31));
        assertEquals(2,lc.calcBank(32));
        assertEquals(2,lc.calcBank(47));
        assertEquals(3,lc.calcBank(48));
        assertEquals(3,lc.calcBank(63));
    }

    public void testCalcBankLed() {
        LEDController3x3 lc = new LEDController3x3();
        int bank = 0;
        assertEquals(0,lc.calcBankLed(bank,0));
        assertEquals(5,lc.calcBankLed(bank,5));
        assertEquals(15,lc.calcBankLed(bank,15));
        bank++;
        assertEquals(0,lc.calcBankLed(bank,16));
    }
}