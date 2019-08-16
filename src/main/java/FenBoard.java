/* *****************************************************************************
 * Copyright 2019 Lee Patterson <https://8BitCoder.com> <https://github.com/abathur8bit>
 * 
 * Created 2019-08-11
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

import com.axorion.chess.ChessBoard;

public class FenBoard {
    public static void main(String[] args) {
        ChessBoard board = new ChessBoard();
        String fen = args[0];
        board.setFenPosition(fen);
        for(int y=0; y<8; y++) {
            System.out.format("%d ",8-y);
            for(int x=0; x<8; x++) {
                System.out.format("%c",board.pieceAt(x,y));
            }
            System.out.println("");
        }
        System.out.println("  abcdefgh");
    }
}
