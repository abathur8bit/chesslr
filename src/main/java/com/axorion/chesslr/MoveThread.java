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

import com.axorion.chess.ChessMove;

/**
 * Thread waits a period of time for the pieces to sit still *after* a complete move,
 * then checks if a move has been completed.
 * By waiting for a time, it allows the user to slide pieces over other squares before
 * coming to a stop.
 */
public class MoveThread extends Thread {
    AppFrame parent;
    Object lock = new Object();
    Integer up = -1,down = -1,secondUp = -1;
    boolean sleepInterrupted = false;
    volatile boolean running = false;
    long waitTime = 750;

    public MoveThread(AppFrame parent) {
        this(parent,750);
    }

    public MoveThread(AppFrame parent,long waitTime) {
        this.parent = parent;
        this.waitTime = waitTime;
    }

    public void run() {
        running = true;
        while(running) {
            try {
                //first wait for a time ...
                Thread.sleep(waitTime);

                synchronized(lock) {
                    if(sleepInterrupted) {
                        // check if we got here after the sleep, and after we wanted to check for move complete
                        sleepInterrupted = false;
                        continue;
                    }

                    //...then check if what is on the board is the same.
                    // If so, player completed move.
                    // If not, player is still sliding his piece.
                    if(running && up != -1 && down != -1) {
                        //check if player has moved since we got both up and down events
                        if((up == parent.pieceUpIndex || up == parent.secondPieceUpIndex) && down == parent.pieceDownIndex) {
                            if(up != down || secondUp != -1) {   //are we putting the piece back down or capturing?
                                if(down == up) {
                                    up = secondUp;  //you picked up the captured piece first, so the second piece up is what we are moving from
                                }
                                parent.chessBoardController.led(up,false);
                                parent.chessBoardController.led(down,false);
                                System.out.println("Player moved piece from "+up+" to "+down);
                                parent.recordMove(new ChessMove(parent.chessBoard,parent.chessBoard.indexToBoard(up)+parent.chessBoard.indexToBoard(down)));
                            } else {
                                //putting the piece down without moving it
                                parent.chessBoardController.led(up,false);
                                parent.chessBoardController.led(down,false);
                                System.out.println("Player put the piece down without moving it");
                            }

                            //show move on display board
                            parent.showLastMove();

                            parent.pieceUpIndex = -1;
                            parent.pieceDownIndex = -1;
                            parent.secondPieceUpIndex = -1;
                            up = down = secondUp = -1;
                        }
                    }
                }
            } catch(InterruptedException e) {
                //sleep was interrupted, allow sleep to start again
                sleepInterrupted = false;
            }
        }
    }

    public void waitForMoveComplete(int up,int down,int secondUp) {
        synchronized(lock) {
            sleepInterrupted = true;
            this.up = up;
            this.down = down;
            this.secondUp = secondUp;
            this.interrupt(); //interrupt sleep cycle so it starts again
        }
    }
}
