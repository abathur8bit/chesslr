package com.axorion.chesslr;

/** Simulated board interface. */
public class SimBoardInterface implements BoardInterface {
    public final static int NUM_SQUARES=64;
    protected boolean ledState[] = new boolean[NUM_SQUARES];
    protected boolean squareState[] = new boolean[NUM_SQUARES];

    public boolean isOccupiec(int i) {
        return squareState[i];
    }

    public boolean isOccupied(int x,int y) {
        return squareState[index(x,y)];
    }

    public void setOccupied(int i,boolean occupied) {
        squareState[i] = occupied;
    }
    public void setOccupied(int x,int y,boolean occupied) {
        squareState[index(x,y)] = occupied;
    }

    public void setLED(int x,int y,boolean on) {
        ledState[index(x,y)] = on;
    }

    public boolean isSetLED(int x,int y) {
        return ledState[index(x,y)];
//        return isOccupied(x,y);
    }

    public void reset() {
        for(int i=0; i<NUM_SQUARES; i++) {
            squareState[i] = false;
            ledState[i] = false;

            if(i<16 || i>=6*8) {
                squareState[i] = true;
            }
        }
    }

    int index(int x,int y) {
        return y*8+x;
    }
}
