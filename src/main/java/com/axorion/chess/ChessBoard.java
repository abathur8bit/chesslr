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
import java.util.ArrayList;
import java.util.List;

/**
 * Chess board that keeps track of the state, and generates FEN notation. When constructed, you get a new board setup.
 * As you make moves, tell ChessBoard about them, and it will keep a log of all moves, and be able to export them as
 * a PGN as well as show the FEN notation of a given position in the game.
 *
 * Methods you are most interested in, besides the constructor is <b>move(), pieceAt(), validate()</b>
 */
public class ChessBoard
{
    public static final int WHITE = 'w';
    public static final int BLACK = 'b';
    public static final int EMPTY_SQUARE = ' ';

    String blackPieceLetters = "pnbrqk";
    String whitePieceLetters = "PNBRQK";

    ArrayList<String> moveCard = new ArrayList<String>();

    int[] gameBoard = new int[64];
    int currentMove = WHITE;
    int halfMoveCounter = 0;
    int fullMoveCounter = 0;
    
    boolean castleWhiteKingSide = true;
    boolean castleWhiteQueenSide = true;
    boolean castleBlackKingSide = true;
    boolean castleBlackQueenSide = true;

    String boardLetters =
            "rnbqkbnr"+
            "pppppppp"+
            "        "+
            "        "+
            "        "+
            "        "+
            "PPPPPPPP"+
            "RNBQKBNR";

    String horz = "abcdefgh";
    String vert = "87654321";

    int[] validPawnMoves = {-1,-2};
    int[] validKnightMoves = {15,17};

    public ChessBoard() {
        resetBoard();
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
        resetBoard();
        castleWhiteKingSide = false;
        castleWhiteQueenSide = false;
        castleBlackKingSide = false;
        castleBlackQueenSide = false;

        int fenPos = 0;
        int boardIndex = 0;
        int parsingSection = 0;
        while(fenPos < fen.length()) {
            char ch = fen.charAt(fenPos);
            if(ch == ' ') {
                parsingSection++;   //parsing the next part of fen
                fenPos++;
                continue;   //skip spaces
            }
            switch(parsingSection) {
                case 0:
                    if(Character.isDigit(ch)) {
                        int count = ch-'0';
                        putSpaces(count,boardIndex);
                        boardIndex += count;
                    } else if(ch == '/') {
                        break;   //ignore, and keep going
//                    } else if(ch == ' ') {
//                        parsingSection++;   //parsing the next part of fen
//                        continue;      //end of parsing of known stuff
                    } else {
                        gameBoard[boardIndex++] = ch;
                    }
                    break;

                case 1:
                    if(ch == 'w') {
                        currentMove = WHITE;
                    } else if(ch == 'b') {
                        currentMove = BLACK;
                    } else {
                        throw new InvalidParameterException(String.format("Color [%c] is not valid",ch));
                    }
                    break;

                case 2:
                    // Castling availability. If neither side can castle, this is "-". Otherwise, this has one or
                    // more letters: "K" (White can castle kingside), "Q" (White can castle queenside),
                    // "k" (Black can castle kingside), and/or "q" (Black can castle queenside).
                    if(ch == '-') {
                        castleWhiteKingSide = false;
                        castleWhiteQueenSide = false;
                        castleBlackKingSide = false;
                        castleBlackQueenSide = false;
                    } else if(ch == 'K') {
                        castleWhiteKingSide = true;
                    } else if(ch == 'Q') {
                        castleWhiteQueenSide = true;
                    } else if(ch == 'k') {
                        castleBlackKingSide = true;
                    } else if(ch == 'q') {
                        castleBlackQueenSide = true;
//                    } else if(ch == ' ') {
//                        parsingSection++;
                    }
                    break;

                default:
                    break;
            }
            fenPos++;
        }
    }

    private void putSpaces(int numSpaces,int boardIndex) {
        for(int i =0; i<numSpaces; i++) {
            gameBoard[boardIndex+i] = ' ';
        }
    }
    public int numSquares() {
        return gameBoard.length;
    }

    public void resetBoard() {
        for(int i=0; i<64; ++i) {
            gameBoard[i] = boardLetters.charAt(i);
        }
        moveCard.clear();
        currentMove = WHITE;
        castleWhiteKingSide = true;
        castleWhiteQueenSide = true;
        castleBlackKingSide = true;
        castleBlackQueenSide = true;
    }

    public void setPosition(String letters) {
        for(int i=0; i<64; ++i) {
            gameBoard[i] = letters.charAt(i);
        }
    }

    public void setWhoMoves(int color) {
        if(color == WHITE || color == BLACK) {
            currentMove = color;
        }
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
        int y=index/8;
        int x=index-y*8;
        return String.format("%s%s",horz.charAt(x),vert.charAt(y));
    }

    /** A list of all the moves in chess coordinates like "e2e3". Always has from and to coordinates. */
    public List<String> getScoreCard() {
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

    /** Enter a move in the form of "e2e4". Returns if the move was valid. */
    public boolean move(String s) {
        if(s.length() == 4) {
            String from = s.substring(0,2);
            String to = s.substring(2);
            int fromIndex = boardToIndex(from);
            int toIndex = boardToIndex(to);

            boolean isCapture = gameBoard[toIndex] != EMPTY_SQUARE;
            gameBoard[toIndex] = gameBoard[fromIndex];
            gameBoard[fromIndex] = EMPTY_SQUARE;
            moveCard.add(s);
            halfMoveCounter++;
            if(gameBoard[toIndex] == 'p' || gameBoard[toIndex] == 'P' || isCapture) {
                halfMoveCounter = 0;
            }
            if(currentMove == WHITE) {
                currentMove = BLACK;
            } else {
                currentMove = WHITE;
                fullMoveCounter++;
            }
            return true;
        }
        return false;
    }

    /**
     * Validate a move in the form "e2e4" and returns if the move is valid. A valid move is one the piece can make that
     * doesn't leave the king in check, is a move the piece can make, like making sure a pawn isn't moved backwards, and
     * the correct color is moved.
     *
     * @return true if the move is okay to make, false otherwise.
     */
    public boolean validate(String move) {
        return true;    //not yet implemented
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
        fen.append(" "+(char)currentMove+" ");

        fen.append((castleWhiteKingSide ?"K":""));
        fen.append((castleWhiteQueenSide?"Q":""));
        fen.append((castleBlackKingSide ?"k":""));
        fen.append((castleBlackQueenSide?"q":""));

        fen.append(" "+"-");  //todo En passant target square not implemented

        fen.append(" "+halfMoveCounter);
        fen.append(" "+(fullMoveCounter+1));

        return fen.toString();
    }
}
