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

package com.axorion.chess;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Unit test for ChessBoard.
 */
public class ChessBoardTest extends TestCase
{
    String initialPosition =
                "rnbqkbnr"+
                "pppppppp"+
                "        "+
                "        "+
                "        "+
                "        "+
                "PPPPPPPP"+
                "RNBQKBNR";

    public void testPgnHalfMove() {
        ChessBoard board = new ChessBoard();
        board.move("d2d4");
        String result = board.getMovesPgn();
        assertEquals("1.d4",result);
    }

    public void testPgnFullMove() {
        ChessBoard board = new ChessBoard();
        board.move("d2d4");
        board.move("e7e5");
        String result = board.getMovesPgn();
        assertEquals("1.d4 e5",result);
    }

    public void testPgnPawnCaptures() {
        ChessBoard board = new ChessBoard();
        board.move("d2d4");
        board.move("e7e5");
        board.move("d4e5");
        String result = board.getMovesPgn();
        assertEquals("1.d4 e5 2.dxe5",result);
    }

    public void testPgnQueenCaptures() {
        ChessBoard board = new ChessBoard();
        board.move("d2d4");
        board.move("e7e5");
        board.move("d4e5");
        board.move("f8d6");
        board.move("d1d6");
        String result = board.getMovesPgn();
        assertEquals("1.d4 e5 2.dxe5 Bd6 3.Qxd6",result);
    }

    public void testFromTo() {
        ChessBoard board = new ChessBoard();
        String from = "a2";
        String to = "a3";
        String move = from+to;
        assertEquals(from,board.from(move));
        assertEquals(to,board.to(move));
    }

    public void testTakeback() {
        ChessBoard board = new ChessBoard();
        assertEquals('p',board.pieceAt("a7"));
        board.move("a2a3");
        board.move("a7a6");
        String result = board.takeback();
        assertEquals("a7a6",result);
        assertEquals('p',board.pieceAt("a7"));
    }

    public void testTakebackPgn() {
        ChessBoard board = new ChessBoard();
        board.move("d2d4");
        board.move("e7e5");
        board.move("d4e5");
        board.move("f8d6");
        board.move("d1d6");
        assertEquals("1.d4 e5 2.dxe5 Bd6 3.Qxd6",board.getMovesPgn());
        assertEquals("d1d6",board.takeback());
        assertEquals("1.d4 e5 2.dxe5 Bd6",board.getMovesPgn());
    }

    public void testTakebackPgnHalfMove() {
        ChessBoard board = new ChessBoard();
        board.move("d2d4");
        board.move("e7e5");
        board.move("d4e5");
        board.move("f8d6");
        board.move("d1d6");
        board.move("c7d6");
        assertEquals("1.d4 e5 2.dxe5 Bd6 3.Qxd6 cxd6",board.getMovesPgn());
        assertEquals("c7d6",board.takeback());
        assertEquals("1.d4 e5 2.dxe5 Bd6 3.Qxd6",board.getMovesPgn());
    }

    public void testDateFormatted() {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(2019,Calendar.JANUARY,1,0,0,0);
        ChessBoard board = new ChessBoard();
        board.setGameDate(cal.getTime());
        assertEquals("2019.01.01",board.getGameDateFormatted());
    }

    public void testIndexToBoard() {
        ChessBoard board = new ChessBoard();
        assertEquals("a8",board.indexToBoard(0));
        assertEquals("a1",board.indexToBoard(7*8));
    }

    public void testFenInitialPosition() {
        ChessBoard board = new ChessBoard();
        String expected = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        System.out.println("toFen=["+board.toFen()+"]");
        assertEquals(expected,board.toFen());
    }

    public void testFenPosition() {
        String positionAfterE2E4 =
                "rnbqkbnr"+
                "pppppppp"+
                "        "+
                "        "+
                "    P   "+
                "        "+
                "PPPP PPP"+
                "RNBQKBNR";
        ChessBoard board = new ChessBoard();
        board.setWhoMoves(ChessBoard.Side.BLACK);
        board.setPosition(positionAfterE2E4);
        String expected = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"; //note that En passant target square not implemented
        System.out.println("toFen=["+board.toFen()+"]");
        assertEquals(expected,board.toFen());
        assertTrue(board.canWhiteCastleKingSide());
        assertTrue(board.canWhiteCastleQueenSide());
        assertTrue(board.canBlackCastleKingSide());
        assertTrue(board.canBlackCastleQueenSide());
    }

    public void testSetPosition() {
        ChessBoard board = new ChessBoard();
        board.setPosition(initialPosition);
        assertEquals(initialPosition,board.toLetters());
    }

    public void testBoardToIndex() {
        ChessBoard board = new ChessBoard();
        assertEquals(0,board.boardToIndex("a8"));
        assertEquals(1,board.boardToIndex("b8"));
        assertEquals(17,board.boardToIndex("b6"));
    }

    public void testPieceAt() {
        ChessBoard board = new ChessBoard();
        assertEquals('r',board.pieceAt("a8"));
        assertEquals('p',board.pieceAt("h7"));
        assertEquals('R',board.pieceAt("a1"));
        assertEquals('P',board.pieceAt("a2"));
        assertEquals('Q',board.pieceAt("d1"));
    }

    public void testPieceIndex() {
        ChessBoard board = new ChessBoard();
        assertEquals('r',board.pieceAt(0));
        assertEquals('p',board.pieceAt(8));
        assertEquals('R',board.pieceAt(8*8-1));
    }

    public void testFENAfterSinglePawnMove() {
        ChessBoard board = new ChessBoard();
        board.move("e2e4");
        String expected = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"; //note that En passant target square not implemented
        System.out.println("toFen=["+board.toFen()+"]");
        assertEquals(expected,board.toFen());
    }

    public void testFENAfterSingleKnightMove() {
        ChessBoard board = new ChessBoard();
        board.move("g1f3");
        String expected = "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1"; //note that En passant target square not implemented
        System.out.println("toFen=["+board.toFen()+"]");
        assertEquals(expected,board.toFen());
    }

    public void testGetMoveString() {
        ChessBoard board = new ChessBoard();
        board.move("g1f3");
        board.move("b8c6");
        board.move("b1c3");

        assertEquals("g1f3 b8c6 b1c3",board.getMoveString());
    }

    public void testShowMoves() {
        ChessBoard board = new ChessBoard();
        board.move("g1f3");
        board.move("b8c6");
        board.move("b1c3");
        List<String> moves = board.getScoreCard();
        int fullMove = 0;
        for(int i=0; i<moves.size(); i++) {
            if(i%2 == 0) {
                if(i != 0) {
                    System.out.println(" ");
                }
                fullMove++;
                System.out.print(fullMove+". ");
                System.out.print(moves.get(i));
            } else {
                System.out.print(" "+moves.get(i));
            }
        }
    }
    /*
    rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2
    rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2
    rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3
    rnbqkb1r/pppp1ppp/5n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R b KQkq - 0 3
    rnbqk2r/ppppbppp/5n2/4p3/4P3/3P1N2/PPP1BPPP/RNBQK2R b KQkq - 2 4
    rnbqkb1r/pppp1ppp/5n2/4p3/4P3/3P1N2/PPP1BPPP/RNBQK2R w KQkq - 3 5
    rnbqkb1r/pppp1ppp/5n2/4p3/4P3/3P1N2/PPP1BPPP/RNBQK2R w KQkq - 3 5
    rnbqk2r/ppppbppp/5n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 5 6
     */

    public void testSetFenPosition() {
        // 8/kp6/pn2r3/1p3Np1/3PB3/2r2P1P/P5P1/R5K1 w - - 0 33
        String fen = "8/kp6/pn2r3/1p3Np1/3PB3/2r2P1P/P5P1/R5K1 w - - 0 33";
        String boardLetters =
                "        "+
                "kp      "+
                "pn  r   "+
                " p   Np "+
                "   PB   "+
                "  r  P P"+
                "P     P "+
                "R     K ";
        ChessBoard board = new ChessBoard();
        board.setFenPosition(fen);
        System.out.println("Expecting:");
        System.out.println(boardLetters);
        System.out.println("Got:");
        System.out.println(board.toLetters());
        assertEquals(boardLetters,board.toLetters());
        assertFalse(board.canWhiteCastleKingSide());
        assertFalse(board.canWhiteCastleQueenSide());
        assertFalse(board.canBlackCastleKingSide());
        assertFalse(board.canBlackCastleQueenSide());


    }

    public void testSetFenPositionCastling() {
        ChessBoard board = new ChessBoard();
        String fen = "8/kp6/pn2r3/1p3Np1/3PB3/2r2P1P/P5P1/R5K1 w - - 0 33";
        board.setFenPosition(fen);
        assertFalse(board.canWhiteCastleKingSide());
        assertFalse(board.canWhiteCastleQueenSide());
        assertFalse(board.canBlackCastleKingSide());
        assertFalse(board.canBlackCastleQueenSide());

        board.setFenPosition("8/kp6/pn2r3/1p3Np1/3PB3/2r2P1P/P5P1/R5K1 w K - 0 33");
        assertTrue(board.canWhiteCastleKingSide());
        assertFalse(board.canWhiteCastleQueenSide());
        assertFalse(board.canBlackCastleKingSide());
        assertFalse(board.canBlackCastleQueenSide());

        board.setFenPosition("8/kp6/pn2r3/1p3Np1/3PB3/2r2P1P/P5P1/R5K1 w Kq - 0 33");
        assertTrue(board.canWhiteCastleKingSide());
        assertFalse(board.canWhiteCastleQueenSide());
        assertFalse(board.canBlackCastleKingSide());
        assertTrue(board.canBlackCastleQueenSide());
    }
}
