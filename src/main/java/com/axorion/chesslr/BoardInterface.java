/* *****************************************************************************
 * Copyright 2018 Lee Patterson <https://github.com/abathur8bit>
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

/** Inteface to the hardware. You check the current signal of a board location. */
public interface BoardInterface {
    /** Check if a square is occipied via a single dimension y*8+x.*/
    boolean isOccupied(int x,int y);

    /** Turn on or off an LED. This might be the wrong place for this. */
    void setLED(int x,int y,boolean on);

    boolean isSetLED(int x,int y);

    void reset();
}
