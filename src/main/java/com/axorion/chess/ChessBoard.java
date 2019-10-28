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

import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Chess board that keeps track of the state, and generates FEN notation. When constructed, you get a new board setup.
 * As you make moves, tell ChessBoard about them, and it will keep a log of all moves, and be able to export them as
 * a PGN as well as show the FEN notation of a given position in the game.
 *
 * Methods you are most interested in, besides the constructor is <b>move(), pieceAt(), validate()</b>
 */
public class ChessBoard
{
    public enum Side {
        WHITE,
        BLACK
    }
    public static final int WHITE = 'w';
    public static final int BLACK = 'b';
    public static final int EMPTY_SQUARE = ' ';

    private long gameId = 1000;
    Calendar startDate = GregorianCalendar.getInstance();
    DateFormat pgnFormatter = new SimpleDateFormat("yyyy.MM.dd");

    String blackPieceLetters = "pnbrqk";
    String whitePieceLetters = "PNBRQK";

    ArrayList<ChessMove> moveCard = new ArrayList<ChessMove>();

    int[] gameBoard = new int[64];
    Side currentMove = Side.WHITE;
    int halfMoveCounter = 0;
    int fullMoveCounter = 0;

    boolean castleWhiteKingSide = true;
    boolean castleWhiteQueenSide = true;
    boolean castleBlackKingSide = true;
    boolean castleBlackQueenSide = true;

    //    String boardLetters =
//            "ppp     "+
//            "        "+
//            "PPP     "+
//            "        "+
//            "        "+
//            "        "+
//            "        "+
//            "        ";
    String boardLettersWhite =
            "rnbqkbnr"+
                    "pppppppp"+
                    "        "+
                    "        "+
                    "        "+
                    "        "+
                    "PPPPPPPP"+
                    "RNBQKBNR";

    String boardLettersBlack =
            "RNBKQBNR"+
                    "PPPPPPPP"+
                    "        "+
                    "        "+
                    "        "+
                    "        "+
                    "pppppppp"+
                    "rnbkqbnr";

    String horz = "abcdefgh";
    String vert = "87654321";

    int[] validPawnMoves = {-1,-2};
    int[] validKnightMoves = {15,17};

    public ChessBoard() {
        resetBoard(Side.WHITE);
    }

    public void setGameId(long id) {
        this.gameId = id;
    }

    public long getGameId() {
        return gameId;
    }

    /** Return if it is white or black to move. */
    public Side getCurrentMove() {
        return currentMove;
    }

    /** Returns the date in PGN format of YYYY.MM.DD, zero padded. ie: 2019.01.31. */
    public String getGameDateFormatted() {
        return pgnFormatter.format(startDate.getTime());
    }

    public void setGameDate(Date d) {
        startDate.setTime(d);
    }

    public boolean canWhiteCastleKingSide() {
        return castleWhiteKingSide;
    }

    public boolean canWhiteCastleQueenSide() {
        return castleWhiteQueenSide;
    }

    public boolean canBlackCastleKingSide() {
        return castleBlackKingSide;
    }

    public boolean canBlackCastleQueenSide() {
        return castleBlackQueenSide;
    }
    /**
     * A FEN "record" defines a particular game position, all in one text line and using only the
     * ASCII character set. A text file with only FEN data records should have the file
     * extension ".fen".
     *
     * A FEN record contains six fields. The separator between fields is a space. The fields are:
     *
     * - Piece placement (from White's perspective). Each rank is described, starting with rank 8
     *   and ending with rank 1; within each rank, the contents of each square are described from
     *   file "a" through file "h". Following the Standard Algebraic Notation (SAN), each piece is
     *   identified by a single letter taken from the standard English names (pawn = "P", knight = "N",
     *   bishop = "B", rook = "R", queen = "Q" and king = "K").[1] White pieces are designated using
     *   upper-case letters ("PNBRQK") while black pieces use lowercase ("pnbrqk"). Empty squares are
     *   noted using digits 1 through 8 (the number of empty squares), and "/" separates ranks.
     *
     * - Active color. "w" means White moves next, "b" means Black moves next.
     *
     * - Castling availability. If neither side can castle, this is "-". Otherwise, this has one or
     *   more letters: "K" (White can castle kingside), "Q" (White can castle queenside),
     *   "k" (Black can castle kingside), and/or "q" (Black can castle queenside).
     *
     * - En passant target square in algebraic notation. If there's no en passant target square, this
     *   is "-". If a pawn has just made a two-square move, this is the position "behind" the pawn.
     *   This is recorded regardless of whether there is a pawn in position to make an en passant
     *   capture.
     *
     * - Halfmove clock: This is the number of halfmoves since the last capture or pawn advance. This
     *   is used to determine if a draw can be claimed under the fifty-move rule.
     *
     * - Fullmove number: The number of the full move. It starts at 1, and is incremented after Black's
     *   move.
     *
     *   abcdefgh
     * 8 rnbqkbnr
     * 7 pppppppp
     * 6
     * 5
     * 4
     * 3
     * 2 PPPPPPPP
     * 1 RNBQKBNR
     *   abcdefgh
     *
     * @param fen
     */
    public void setFenPosition(String fen) {
        resetBoard(Side.WHITE);
        String[] sections = fen.split(" ");
//        System.out.println(sections);
        parsePieces(sections[0]);
        parseColor(sections[1]);
        parseCastling(sections[2]);
        parseEnpassant(sections[3]);
        parseHalfMoves(sections[4]);
        parseFullMoves(sections[5]);
    }

    private void parsePieces(String fen) {
        int boardIndex = 0;
        for(char ch : fen.toCharArray()) {
//            char ch = fen.charAt(fenPos);
            if(Character.isDigit(ch)) {
                int count = ch-'0';
                putSpaces(count,boardIndex);
                boardIndex += count;
            } else if(ch == '/') {
                continue;
            } else {
                gameBoard[boardIndex++] = ch;
            }
        }
    }

    private void parseColor(String fen) {
        char ch = fen.charAt(0);
        if(ch == WHITE) {
            currentMove = Side.WHITE;
        } else if(ch == BLACK) {
            currentMove = Side.BLACK;
        } else {
            throw new InvalidParameterException(String.format("Color [%c] is not valid",ch));
        }
    }

    private void parseCastling(String fen) {
        castleWhiteKingSide = false;
        castleWhiteQueenSide = false;
        castleBlackKingSide = false;
        castleBlackQueenSide = false;

        int fenPos = 0;
        while(fenPos < fen.length()) {
            char ch = fen.charAt(fenPos);
            // Castling availability. If neither side can castle, this is "-". Otherwise, this has one or
            // more letters: "K" (White can castle kingside), "Q" (White can castle queenside),
            // "k" (Black can castle kingside), and/or "q" (Black can castle queenside).
            if(ch == 'K') {
                castleWhiteKingSide = true;
            } else if(ch == 'Q') {
                castleWhiteQueenSide = true;
            } else if(ch == 'k') {
                castleBlackKingSide = true;
            } else if(ch == 'q') {
                castleBlackQueenSide = true;
            }
            fenPos++;
        }
    }

    private void parseEnpassant(String fen) {

    }

    private void parseHalfMoves(String fen) {
        halfMoveCounter = Integer.parseInt(fen);
    }

    private void parseFullMoves(String fen) {
        fullMoveCounter = Integer.parseInt(fen)-1;
    }

    private void putSpaces(int numSpaces,int boardIndex) {
        for(int i =0; i<numSpaces; i++) {
            gameBoard[boardIndex+i] = ' ';
        }
    }
    public int numSquares() {
        return gameBoard.length;
    }

    public void resetBoard(Side s) {
        String letters;
        if(s == Side.WHITE)
            letters = boardLettersWhite;
        else
            letters = boardLettersBlack;
        for(int i=0; i<64; ++i) {
            gameBoard[i] = letters.charAt(i);
        }
        moveCard.clear();
        currentMove = Side.WHITE;

        castleWhiteQueenSide = true;
        castleWhiteKingSide = true;
        castleBlackQueenSide = true;
        castleBlackKingSide = true;
    }

    public void setPosition(String letters) {
        for(int i=0; i<64; ++i) {
            gameBoard[i] = letters.charAt(i);
        }
    }

    public void setWhoMoves(Side color) {
        if(color == Side.WHITE || color == Side.BLACK) {
            currentMove = color;
        }
    }

    /**
     * Return the source square in the given 4 char move.
     * @param move Move like "a2a3".
     * @return source square like "a2".
     */
    public String from(String move) {
        return move.substring(0,2);
    }

    /**
     * Return the destination square in the given 4 char move.
     * @param move Move like "a2a3".
     * @return destination square like "a3".
     */
    public String to(String move) {
        return move.substring(2,4);
    }
    /**
     * Convert the board coordinate to an index with 0 being the upper left corner.
     * used internally.
     *
     * @param coord Board coordinate like "e1"
     * @return index into the gameBoard array.
     */
    public int boardToIndex(String coord) {
        int x=0,y=0,c=0;

        c = coord.charAt(0);
        for(int i=0; i<8; i++) {
            if(horz.charAt(i) == c) {
                x = i;
                break;
            }
        }

        c = coord.charAt(1);
        for(int i=0; i<8; i++) {
            if(vert.charAt(i) == c) {
                y = i;
                break;
            }
        }

        int index= y*8+x;
        return index;
    }

    /** Convert a board index number to a board position like "a1". */
    public String indexToBoard(int index) {
        int y = index/8;
        int x = index-y*8;
        try {
            return String.format("%s%s",horz.charAt(x),vert.charAt(y));
        } catch(StringIndexOutOfBoundsException e) {
            System.out.println("coords x=["+x+"] y=["+y+"] horz=["+horz+"] vert=["+vert+"]");
            e.printStackTrace();
        }
        return "";
    }

    /** A list of all the moves in chess coordinates like "e2e3". Always has from and to coordinates. */
    public List<ChessMove> getScoreCard() {
        return moveCard;
    }

    /** Returns all the moves in a single string in chess board "e1e3" format. For example "g1f3 b8c6 b1c3". */
    public String getMoveString() {
        StringBuilder moves = new StringBuilder();
        int fullMove = 0;
        for(int i=0; i<moveCard.size(); i++) {
            if(i>0) {
                moves.append(" ");
            }
            moves.append(moveCard.get(i));
        }
        return moves.toString();
    }

    public String getMovesPgn() {
        ChessBoard board = new ChessBoard();
        StringBuilder buff = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        int moveNumber = 0;
        for(ChessMove move : moveCard) {
            moveNumber = board.fullMoveCounter+1;
            Side currentMove = board.currentMove;
            if(currentMove == Side.WHITE) {
                temp.append(moveNumber+".");
            } else {
                temp.append(' ');
            }

            if(move.isCastleKingSide()) {
                temp.append("O-O");
            } else if(move.isCastleQueenSide()) {
                temp.append("O-O-O");
            } else {
                String from = move.getFrom();
                String to = move.getTo();
                char pieceFrom = move.getMovedPiece();
                char pieceTo = (char)board.pieceAt(to);
                if(pieceFrom >= 'a') {
                    pieceFrom = (char)(pieceFrom-'a'+'A');
                }
                boolean take = pieceTo != EMPTY_SQUARE;
                if(pieceFrom == 'P') {
                    if(take) {
                        temp.append(from.charAt(0)+"x");
                    }
                    temp.append(to);
                } else {
                    temp.append(pieceFrom);
                    if(pieceFrom == 'n' || pieceFrom == 'N' || pieceFrom == 'r' || pieceFrom == 'R')
                        temp.append(from.charAt(0));
                    if(take) {
                        temp.append('x');
                    }
                    temp.append(to);
                }
            }

            if(currentMove == Side.BLACK) {
                if(moveNumber > 1) {
                    buff.append(' ');
                }
                buff.append(temp.toString());
                temp.delete(0,buff.length());
            }

            board.move(move);
        }
        //check if we have a half move
        if(temp.length()>0)
            if(moveNumber > 1) {
                buff.append(' ');
            }
        buff.append(temp.toString());

        return buff.toString();
    }

    /**
     * Convert x,y to a board index.
     * Top left is 0,0, bottom right is 7,7.
     */
    public int toIndex(int x,int y) {
        return y*8+x;
    }

    /** Return what piece is at the given location on the board. Top left is 0,0, bottom right is 7,7.
     * See pieceAt(int);
     *
     * @param x Column of piece location.
     * @param y Row of piece location
     * @return Letter of the piece, or a space for an empty square.
     */
    public char pieceAt(int x,int y) {
        return pieceAt(y*8+x);
    }

    /**
     * Return what pieces is at the given index in the board. Top left is 0, bottom right is 63. Will return an integer
     * representing the ascii value (char) of the letter, or a space.
     *
     * @param index index into the gameBoard.
     * @return Letter of the piece, or a space for an empty square.
     */
    public char pieceAt(int index) {
        return (char)gameBoard[index];
    }

    /** Return what piece is at the chess coordinate specified, like "a1" would return 'R' on a new board. */
    public int pieceAt(String s) {
        if(s.length() == 2) {
            int index = boardToIndex(s.substring(0,2));
            return gameBoard[index];
        }
        return 0;
    }

    public ChessMove move(ChessMove m) {
        gameBoard[m.getToIndex()] = gameBoard[m.getFromIndex()];
        gameBoard[m.getFromIndex()] = EMPTY_SQUARE;
        checkForCastle(m);
        moveCard.add(m);
        halfMoveCounter++;
        if(gameBoard[m.getToIndex()] == 'p' || gameBoard[m.getToIndex()] == 'P' || m.isCapture()) {
            halfMoveCounter = 0;
        }
        if(currentMove == Side.WHITE) {
            currentMove = Side.BLACK;
        } else {
            currentMove = Side.WHITE;
            fullMoveCounter++;
        }
        return m;
    }

    protected void checkForCastle(ChessMove m) {
        if(currentMove == Side.WHITE) {
            if(m.isCastleKingSide()) {
                castleWhiteKingSide = false;
                gameBoard[63] = EMPTY_SQUARE;
                gameBoard[61] = 'R';
            } else if(m.isCastleQueenSide()) {
                castleWhiteQueenSide = false;
                gameBoard[56] = EMPTY_SQUARE;
                gameBoard[59] = 'R';
            }
        } else {
            if(m.isCastleKingSide()) {
                castleBlackKingSide = false;
                gameBoard[7] = EMPTY_SQUARE;
                gameBoard[5] = 'r';
            } else if(m.isCastleQueenSide()) {
                castleBlackQueenSide = false;
                gameBoard[0] = EMPTY_SQUARE;
                gameBoard[3] = 'r';
            }
        }
    }

    /** Enter a move in the form of "e2e4". Returns a move object, or null if move was not valid. */
    public ChessMove move(String s) {
        if(s.length() >= 4) {
            ChessMove m = new ChessMove(this,s);
            return move(m);
        }
        return null;
    }

    /**
     * Validate a move in the form "e2e4" and returns if the move is valid. A valid move is one the piece can make that
     * doesn't leave the king in check, is a move the piece can make, like making sure a pawn isn't moved backwards, and
     * the correct color is moved.
     *
     * @return true if the move is okay to make, false otherwise.
     */
    public boolean isValid(String ean) {
        return isValid(new ChessMove(this,ean));
    }

    public boolean isValid(ChessMove m) {
        return true;
    }

    public ChessMove takeback() {
        if(moveCard.size() == 0)
            return null;
        int index = moveCard.size()-1;
        ChessMove move = moveCard.get(index);
        move.takeback();
        moveCard.remove(index);
        replayMoves();
        return move;
    }

    /** Replay all moves so board state is restored. */
    private void replayMoves() {
        ChessBoard replay = new ChessBoard();
        for(ChessMove move : moveCard) {
            replay.move(move);
        }
        this.gameBoard = replay.gameBoard;
        this.currentMove = replay.currentMove;
        this.halfMoveCounter = replay.halfMoveCounter;
        this.fullMoveCounter = replay.fullMoveCounter;
        this.castleWhiteKingSide = replay.castleWhiteKingSide;
        this.castleWhiteQueenSide = replay.castleWhiteQueenSide;
        this.castleBlackKingSide = replay.castleBlackKingSide;
        this.castleBlackQueenSide = replay.castleBlackQueenSide;
    }

    public String toLetters() {
        StringBuilder letters = new StringBuilder();
        for(int value : gameBoard) {
            letters.append((char)value);
        }
        return letters.toString();
    }

    public String toFen() {
        StringBuilder fen = new StringBuilder();
        int emptyCount = 0;

        for(int i=0; i<gameBoard.length; ++i) {
            if(i>0 && i%8==0) {
                if(emptyCount > 0) {
                    fen.append(emptyCount);
                }
                emptyCount = 0;
                fen.append('/');
            }

            if(gameBoard[i] == EMPTY_SQUARE) {
                emptyCount++;
            } else {
                if(emptyCount > 0) {
                    fen.append(emptyCount);
                    emptyCount = 0;
                }
                fen.append((char)gameBoard[i]);
            }
        }
        if(emptyCount>0) {
            fen.append(emptyCount);
        }

        //what side plays
        fen.append(" ");
        if(currentMove == Side.WHITE)
            fen.append('w');
        else
            fen.append('b');
        fen.append(" ");

        fen.append((castleWhiteKingSide ?"K":""));
        fen.append((castleWhiteQueenSide?"Q":""));
        fen.append((castleBlackKingSide ?"k":""));
        fen.append((castleBlackQueenSide?"q":""));

        fen.append(" "+"-");  //todo En passant target square not implemented

        fen.append(" "+halfMoveCounter);
        fen.append(" "+(fullMoveCounter+1));

        return fen.toString();
    }

    /** Returns if the given piece is white. */
    public boolean isWhite(char piece) {
        return Character.isUpperCase(piece);
    }

    /** Returns if the given piece is black. */
    public boolean isBlack(char piece) {
        return Character.isLowerCase(piece);
    }

    public String toString() {
        return toFen();
    }
}
