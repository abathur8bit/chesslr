/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
 *
 * Created 2019-08-20
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

/**
 * Thread waits a period of time for the pieces to sit still, then checks if a move has been completed.
 * By waiting for a time, it allows the user to slide pieces over other squares and come to a stop.
 */
public class MoveThread extends Thread {
    AppFrame parent;
    Object lock = new Object();
    Integer up = -1,down = -1;
    boolean sleepInterrupted = false;
    volatile boolean running = false;

    public MoveThread(AppFrame parent) {
        this.parent = parent;
    }

    public void run() {
        running = true;
        while(running) {
            try {
                Thread.sleep(750);

                synchronized(lock) {
                    if(sleepInterrupted) {
                        // check if we got here after the sleep, and after we wanted to check for move complete
                        sleepInterrupted = false;
                        continue;
                    }

                    if(running && up != -1 && down != -1) {
                        //check if player has moved since we got both up and down events
                        if(up == parent.pieceUpIndex && down == parent.pieceDownIndex) {
                            if(up != down) {   //make sure we are not dropping the piece on the same square
                                String playersMove = parent.chessBoard.indexToBoard(up)+parent.chessBoard.indexToBoard(down);
                                parent.recordMove(playersMove);
                            }
                            parent.ledController.led(parent.mapToPin(up),false);
                            parent.ledController.led(parent.mapToPin(down),false);
                            parent.pieceUpIndex = -1;
                            parent.pieceDownIndex = -1;
                            up = -1;
                            down = -1;
                        }
                    }
                }
            } catch(InterruptedException e) {
                //sleep was interrupted, allow sleep to start again
                sleepInterrupted = false;
            }
        }
    }

    public void waitForMoveComplete(int up,int down) {
        synchronized(lock) {
            sleepInterrupted = true;
            this.up = up;
            this.down = down;
            this.interrupt(); //interrupt sleep cycle so it starts again
        }
    }
}
