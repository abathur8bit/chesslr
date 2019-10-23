/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
 *
 * Created 2019-09-21
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

package com.axorion.chess;

public class ChessMove {
    protected ChessBoard board;
    protected String from;
    protected String to;
    protected int fromIndex = -1;
    protected int toIndex = -1;
    protected boolean capture = false;
    protected char capturedPiece = ChessBoard.EMPTY_SQUARE;
    protected char movedPiece = ChessBoard.EMPTY_SQUARE;
    protected char promotedTo = 0;
    protected boolean isCastleQueenSide = false;
    protected boolean isCastleKingSide = false;
    protected boolean takebackMove = false;

    protected boolean capturedUp = false;
    protected boolean fromUp = false;
    protected boolean toUp = false;
    protected boolean toDown = false;
    protected boolean fromDown = false;   //when a piece was catured during a takeback, this is replacing the piece that was removed.

    /** You should construct your move before making the move on the board. Otherwise
     * you won't know if you are capturing. Move does not include a capture
     *
     * @param board Source board.
     * @param ean move in extended algebraic notation format, like "d2d4".
     */
    public ChessMove(ChessBoard board,String ean) {
        this.board = board;
        this.from = board.from(ean);
        this.to = board.to(ean);
        this.fromIndex = board.boardToIndex(from);
        this.toIndex = board.boardToIndex(to);
        this.movedPiece = (char)board.pieceAt(from);
        this.takebackMove = false;

        checkForCastle();

        if(board.pieceAt(to) != ChessBoard.EMPTY_SQUARE) {
            capture = true;
            capturedPiece = (char)board.pieceAt(to);
        }

        if(ean.length() == 5) {
            promotedTo = ean.charAt(4);
        }
    }

    protected void checkForCastle() {
        if(movedPiece == 'K') {
            if(fromIndex == 60 && toIndex == 62 && board.castleWhiteKingSide) {
                isCastleKingSide = true;
            } else if(fromIndex == 60 && toIndex == 58 && board.castleWhiteQueenSide) {
                isCastleQueenSide = true;
            }
        } else if(movedPiece == 'k') {
            if(fromIndex == 4 && toIndex == 2 && board.castleBlackQueenSide) {
                isCastleQueenSide = true;
            } else if(fromIndex == 4 && toIndex == 6 && board.castleBlackKingSide) {
                isCastleKingSide = true;
            }
        }
    }

    /**
     * Cause this move to be a takeback move. From and to squares will be reversed
     * and takeback flag is set.
     */
    public void takeback() {
        String temp = to;
        to = from;
        from = temp;
        this.fromIndex = board.boardToIndex(from);
        this.toIndex = board.boardToIndex(to);
        this.takebackMove = true;

        reset();
    }

    /** Reset up/down states. */
    public void reset() {
        capturedUp = false;
        fromDown = toDown = fromUp = toUp = false;
    }
    public String toString() {
        StringBuilder buff = new StringBuilder(from);
        if(capture)
            buff.append('x');
        buff.append(to);
        if(isPromoted())
            buff.append(promotedTo);
        return buff.toString();
    }

    public String toEan() {
        if(promotedTo != 0) {
            return from+to+promotedTo;
        } else {
            return from+to;
        }
    }

    public char getPromotedTo() {
        return promotedTo;
    }

    public boolean isPromoted() {
        return (promotedTo == 0 ? false:true);
    }

    public boolean isCapture() {
        return capture;
    }

    public boolean isComplete() {
        boolean complete = false;
        if(takebackMove && capture) {
            if(fromUp && fromDown && toDown) {
                complete = true;
            }
        } else if(takebackMove && !capture) {
            if(fromUp && toDown) {
                complete = true;
            }
        } else if(!takebackMove && capture) {
            if(fromUp && toUp && toDown) {
                complete = true;
            }
        } else {
            if(fromUp && toDown) {
                complete = true;
            }
        }
        return complete;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    public char getCapturedPiece() {
        return capturedPiece;
    }

    public char getMovedPiece() {
        return movedPiece;
    }

    public boolean isCapturedUp() {
        return capturedUp;
    }

    public boolean isFromUp() {
        return fromUp;
    }

    public boolean isToUp() {
        return toUp;
    }

    public boolean isToDown() {
        return toDown;
    }

    public void setCapturedUp(boolean capturedUp) {
        this.capturedUp = capturedUp;
    }

    public void setFromUp(boolean fromUp) {
        this.fromUp = fromUp;
    }

    public void setToUp(boolean toUp) {
        this.toUp = toUp;
    }

    public void setToDown(boolean toDown) {
        this.toDown = toDown;
    }

    public boolean isFromDown() {
        return fromDown;
    }

    public void setFromDown(boolean fromDown) {
        this.fromDown = fromDown;
    }

    public boolean isTakebackMove() {
        return takebackMove;
    }

    /** Castle long. */
    public boolean isCastleQueenSide() {
        return isCastleQueenSide;
    }

    public void setCastleQueenSide(boolean castleQueenSide) {
        isCastleQueenSide = castleQueenSide;
    }

    /** Castle short. */
    public boolean isCastleKingSide() {
        return isCastleKingSide;
    }

    public void setCastleKingSide(boolean castleKingSide) {
        isCastleKingSide = castleKingSide;
    }
}