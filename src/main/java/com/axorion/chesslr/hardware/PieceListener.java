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

/**
 * Listener for handling when a piece is picked up or put down. Index passed is the
 * index of a 64 square board, with 0 being the top left, and 63 being the bottom right.
 *
 * <pre>
 *    a  b  c  d  e  f  g  h
 * 8  00 01 02 03 04 05 06 07  8
 * 7  08 09 10 11 12 13 14 15  7
 * 6  16 17 18 19 20 21 22 23  6
 * 5  24 25 26 27 28 29 30 31  5
 * 4  32 33 34 35 36 37 38 39  4
 * 3  40 41 42 43 44 45 46 47  3
 * 2  48 49 50 51 52 53 54 55  2
 * 1  56 57 58 59 60 61 62 63  1
 *    a  b  c  d  e  f  g  h
 * </pre>
 */
public interface PieceListener {
    void pieceUp(int boardIndex);
    void pieceDown(int boardIndex);
}
