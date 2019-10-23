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

import com.axorion.chess.ChessBoard;

import java.awt.*;
import java.io.*;
import java.util.prefs.Preferences;

public class ChessPrefs {
    static final String GAME_ID = "gameId";
    static final String WINDOW_POSITION = "window";
    static final String PGN_NOTATION = "pgnNotation";
    static final String SHOW_EVALUATION = "showEvaluation";
    static final String DISABLE_ENGINE = "disableEngine";
    static final String FEN = "fen";
    static final String PLAYERS = "players";
    AppFrame parent;
    Preferences prefs;
    boolean selectOutput = true;
    long gameId = 1000;
    String fen;
    boolean pgnNotation = true;
    boolean showEvaluation = true;
    boolean disableEngine = false;
    int players;

    Rectangle bounds;
    File prefsFile; //lazy load in #getPrefsFile

    public ChessPrefs(AppFrame parent) {
        prefs = Preferences.userRoot().node(this.getClass().getName());
        this.parent = parent;
    }

    private File getPrefsFile() {
        if(prefsFile == null) {
            prefsFile = new File(System.getProperty("user.home")+File.separator+".chesslr");
        }
        return prefsFile;
    }

    public void loadPrefs() {
        if(getPrefsFile().exists()) {
            try {
                InputStream in = new FileInputStream(getPrefsFile());
                Preferences.importPreferences(in);
                String position = prefs.get(WINDOW_POSITION,null);
                if(position != null) {
                    String[] nums = position.split(",");
                    bounds = new Rectangle(Integer.parseInt(nums[0]),Integer.parseInt(nums[1]),Integer.parseInt(nums[2]),Integer.parseInt(nums[3]));
                }
                gameId = prefs.getLong(GAME_ID,gameId);
                pgnNotation = prefs.getBoolean(PGN_NOTATION,pgnNotation);
                showEvaluation = prefs.getBoolean(SHOW_EVALUATION,showEvaluation);
                showEvaluation = prefs.getBoolean(DISABLE_ENGINE,disableEngine);
                fen = prefs.get(FEN,null);
                players = prefs.getInt(PLAYERS,players);
            } catch(Exception e) {
                ChessLR.handleError("Unable to load preferences from ["+getPrefsFile().getAbsolutePath()+"]",e);
            }
        }
    }

    public void savePrefs(ChessBoard board) {
        this.fen = board.toFen();
        this.gameId = board.getGameId();
        savePrefs();
    }

    public void savePrefs() {
        try {
            if(parent != null) {
                String position = String.format("%d,%d,%d,%d",parent.getX(),parent.getY(),parent.getWidth(),parent.getHeight());
                prefs.put(WINDOW_POSITION,position);
                prefs.put(GAME_ID,Long.toString(gameId));
                prefs.putBoolean(PGN_NOTATION,pgnNotation);
                prefs.putBoolean(SHOW_EVALUATION,showEvaluation);
                prefs.putBoolean(DISABLE_ENGINE,disableEngine);
                if(fen != null) {
                    prefs.put(FEN,fen);
                }
                prefs.putInt(PLAYERS,players);
            }

            OutputStream os = new FileOutputStream(getPrefsFile());
            prefs.exportNode(os);
        } catch(Exception e) {
            ChessLR.handleError("Unable to load prefs from ["+getPrefsFile().getAbsolutePath()+"]",e);
        }
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public long getGameId() {
        return gameId;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public boolean isPgnNotation() {
        return pgnNotation;
    }

    public void setPgnNotation(boolean pgnNotation) {
        this.pgnNotation = pgnNotation;
    }

    public boolean isShowEvaluation() {
        return showEvaluation;
    }

    public void setShowEvaluation(boolean showEvaluation) {
        this.showEvaluation = showEvaluation;
    }

    public boolean isDisableEngine() {
        return disableEngine;
    }

    public void setDisableEngine(boolean disableEngine) {
        this.disableEngine = disableEngine;
    }

    public int getPlayers() {
        return players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }
}

