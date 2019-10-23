package com.axorion.chess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ChessMoveTest {
    ChessBoard board;

    @Before
    public void setUp() throws Exception {
        board = new ChessBoard();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testWhiteCastleShortValid() {
        board.setFenPosition("rnbqk2r/ppppbppp/4pn2/8/8/4PN2/PPPPBPPP/RNBQK2R w KQkq - 2 4");
        String ean = "e1g1";
        ChessMove move = new ChessMove(board,ean);
        assertTrue(move.isCastleKingSide());
        assertFalse(move.isCapture());
        assertFalse(move.isTakebackMove());
        assertFalse(move.isPromoted());
    }

    /** Ensure from and to are reversed. */
    @Test
    public void takebackFromTo() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        String ean = "d4e5";
        ChessMove move = new ChessMove(board,ean);
        board.move(move);
        ChessMove takeback = board.takeback();
        assertEquals(board.from(ean),takeback.getTo());
        assertEquals(board.to(ean),takeback.getFrom());
    }
    @Test
    public void takebackYesCaptureYes_fromDownFirst() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        String ean = "d4e5";
        ChessMove move = new ChessMove(board,ean);
        board.move(move);
        ChessMove takeback = board.takeback();
        assertFalse(takeback.isComplete());
        assertFalse(takeback.isFromUp());
        assertFalse(takeback.isToUp());
        assertFalse(takeback.isFromDown());
        assertFalse(takeback.isToDown());

        takeback.setFromUp(true);     //player picks up the piece to move back
        assertFalse(takeback.isComplete());
        takeback.setFromDown(true);     //player puts piece captured
        assertFalse(takeback.isComplete());
        takeback.setToDown(true);       //player puts the piece back where it was before move
        assertTrue(takeback.isComplete());
    }
    @Test
    public void takebackYesCaptureYes_toDownFirst() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        String ean = "d4e5";
        ChessMove move = new ChessMove(board,ean);
        board.move(move);
        ChessMove takeback = board.takeback();
        assertFalse(takeback.isComplete());
        assertFalse(takeback.isFromUp());
        assertFalse(takeback.isToUp());
        assertFalse(takeback.isFromDown());
        assertFalse(takeback.isToDown());

        takeback.setFromUp(true);     //player picks up the piece to move back
        assertFalse(takeback.isComplete());
        takeback.setToDown(true);       //player puts the piece back where it was before move
        assertFalse(takeback.isComplete());
        takeback.setFromDown(true);     //player puts piece captured
        assertTrue(takeback.isComplete());
    }
    @Test
    public void takebackYesCaptureNo() {
    }
    @Test
    public void takebackNoCaptureYes() {
    }

    @Test
    public void moveToString() {
        String ean = "d2d4";
        ChessMove move = new ChessMove(board,ean);
        assertEquals(ean,move.toString());
    }
    @Test
    public void moveToEanCapture() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        String ean = "d4e5";
        ChessMove move = new ChessMove(board,ean);
        assertEquals(ean,move.toEan());
    }
    @Test
    public void moveToEanPromotion() {
        String ean = "d7d8q";
        ChessMove move = new ChessMove(board,ean);
        assertEquals(ean,move.toEan());
        assertEquals("d7",move.getFrom());
        assertEquals("d8",move.getTo());
        assertEquals('q',move.getPromotedTo());
        assertTrue(move.isPromoted());
    }

    @Test
    public void isCaptureTrue() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        ChessMove move = new ChessMove(board,"d4e5");
        assertTrue(move.isCapture());
        assertEquals('p',move.getCapturedPiece());
        assertEquals('P',move.getMovedPiece());
    }
    @Test
    public void isCaptureFalse() {
        ChessMove move = new ChessMove(board,"d2d4");
        assertFalse(move.isCapture());
        assertEquals(ChessBoard.EMPTY_SQUARE,move.getCapturedPiece());
        assertEquals('P',move.getMovedPiece());
    }
    @Test
    public void moveSquenceNormal() {
        ChessMove move = new ChessMove(board,"d2d4");
        assertFalse(move.isCapture());
        assertFalse(move.isFromUp());
        assertFalse(move.isToUp());
        assertFalse(move.isToDown());
        assertFalse(move.isComplete());

        move.setFromUp(true);   //player picked up from piece
        assertFalse(move.isComplete());

        move.setToDown(true);   //player put piece down
        assertTrue(move.isComplete());
    }
    @Test
    public void moveSequenceCaptureFromFirst() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        ChessMove move = new ChessMove(board,"d4e5");   //dxe5
        assertTrue(move.isCapture());
        assertFalse(move.isFromUp());
        assertFalse(move.isToUp());
        assertFalse(move.isToDown());
        assertFalse(move.isComplete());

        move.setFromUp(true);     //player picked up from piece
        assertFalse(move.isComplete());

        move.setToUp(true);     //player picked up the to piece
        assertFalse(move.isComplete());

        move.setToDown(true);   //player put from piece down
        assertTrue(move.isComplete());
    }
    @Test
    public void moveSequenceCaptureToFirst() {
        board.setFenPosition("rnbqkbnr/pppp1ppp/8/4p3/3P4/8/PPP1PPPP/RNBQKBNR w KQkq - 0 2");
        ChessMove move = new ChessMove(board,"d4e5");   //dxe5
        assertTrue(move.isCapture());
        assertFalse(move.isFromUp());
        assertFalse(move.isToUp());
        assertFalse(move.isToDown());
        assertFalse(move.isComplete());

        move.setToUp(true);     //player picked up the to piece
        assertFalse(move.isComplete());

        move.setFromUp(true);     //player picked up from piece
        assertFalse(move.isComplete());

        move.setToDown(true);   //player put from piece down
        assertTrue(move.isComplete());
    }
}