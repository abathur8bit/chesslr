package com.axorion.chesslr.hardware;

import com.axorion.chess.ChessBoard;
import com.pi4j.gpio.extension.mcp.MCP23017GpioProvider;
import com.pi4j.gpio.extension.mcp.MCP23017Pin;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import java.io.IOException;

public class BoardDiagnostics implements PieceListener {
    class RowProvider {
        int bus,address;
        MCP23017GpioProvider gpioProvider;
        GpioPinDigitalOutput[] outputPins;
        GpioPinDigitalInput[] inputPins;

        Pin[] inputPinNames = {
                MCP23017Pin.GPIO_A0,
                MCP23017Pin.GPIO_A1,
                MCP23017Pin.GPIO_A2,
                MCP23017Pin.GPIO_A3,
                MCP23017Pin.GPIO_A4,
                MCP23017Pin.GPIO_A5,
                MCP23017Pin.GPIO_A6,
                MCP23017Pin.GPIO_A7,
        };
        Pin[] outputPinNames = {
                MCP23017Pin.GPIO_B7,
                MCP23017Pin.GPIO_B6,
                MCP23017Pin.GPIO_B5,
                MCP23017Pin.GPIO_B4,
                MCP23017Pin.GPIO_B3,
                MCP23017Pin.GPIO_B2,
                MCP23017Pin.GPIO_B1,
                MCP23017Pin.GPIO_B0,
        };
        PieceListener listener;

        public RowProvider(int bus,int address,PieceListener listener) throws Exception {
            this.bus = bus;
            this.address = address;
            this.listener = listener;

            gpioProvider = new MCP23017GpioProvider(bus,address);
            outputPins = new GpioPinDigitalOutput[BANK_SIZE];
            inputPins = new GpioPinDigitalInput[BANK_SIZE];

            //output
            for(int i=0; i<BANK_SIZE; i++) {
                String name = String.format("OUTPUT %02X - %s",address,outputPinNames[i].toString());
                System.out.printf("pin %02d name [%s]\n",i,name);
                outputPins[i] = gpio.provisionDigitalOutputPin(gpioProvider,outputPinNames[i],name,PinState.LOW);
            }
            //input
            for(int i=0; i<BANK_SIZE; ++i) {
                String name = String.format("INPUT %02X - %s",address,inputPinNames[i].toString());
                System.out.printf("pin %02d name [%s]\n",i,name);
                inputPins[i] = gpio.provisionDigitalInputPin(gpioProvider,inputPinNames[i],name,PinPullResistance.PULL_UP);
            }

            gpio.addListener((GpioPinListenerDigital)event -> {
                    final PinState state = event.getState();
                    final Pin pin = event.getPin().getPin();
                    final int pinIndex = findPinIndex(pin,inputPinNames);
                    final int boardIndex = (address-BASE_ADDRESS)*8+pinIndex;

                    System.out.printf("Handling event [%s] state [%s] for pin index [%d] board index [%d]\n",event.getPin().getName(),state,pinIndex,boardIndex);

                    if(listener != null) {
                        if(stateIsDown(state))
                            listener.pieceDown(boardIndex);
                        else
                            listener.pieceUp(boardIndex);
                    }
                },inputPins);
        }

        public void setState(boolean state,int index) {
            gpio.setState(state,outputPins[index]);
        }
    }

    final int BUS = I2CBus.BUS_1;
    static final int BASE_ADDRESS = 0x20;
    static final int BANK_SIZE = 8;
    static final int NUM_ROWS = 8;
    static final int NUM_COLS = 8;

    ChessBoard chessBoard;
    GpioController gpio;
    BoardController chessBoardController;
    RowProvider[] providers = new RowProvider[NUM_ROWS];

    public static void main(String[] args) throws Exception {
        BoardDiagnostics diag = new BoardDiagnostics();
//        diag.fullBoardTest();
//        diag.singleRowTest();
        diag.multiRowTest();
    }


    public BoardDiagnostics() throws Exception {
    }

    class MyListener implements PieceListener {
        RowProvider provider;

        @Override
        public void pieceUp(int boardIndex) {
            showRow(provider.outputPins,provider.inputPins);
        }

        @Override
        public void pieceDown(int boardIndex) {
            showRow(provider.outputPins,provider.inputPins);
        }
    }

    class MultiListener implements PieceListener {
        RowProvider[] providers;

        @Override
        public void pieceUp(int boardIndex) {
            showAll(providers);
        }

        @Override
        public void pieceDown(int boardIndex) {
            showAll(providers);
        }
    }
    public void singleRowTest() throws Exception {
        int address = BASE_ADDRESS+7;

        gpio = GpioFactory.getInstance();
        MyListener listener = new MyListener();
        RowProvider provider = new RowProvider(BUS,address,listener);
        listener.provider = provider;

        System.out.println("Ready to look for state changes");
        showRow(provider.outputPins,provider.inputPins);

        while(true) {

        }
    }

    public void multiRowTest() throws Exception {
        int address = BASE_ADDRESS;
        MultiListener listener = new MultiListener();

        gpio = GpioFactory.getInstance();
        for(int i = 0; i < NUM_ROWS; i++) {
            providers[i] = new RowProvider(BUS,address+i,listener);
        }

        listener.providers = providers;

        System.out.println("Ready to look for state changes");
        showAll(providers);
        while(true) {

        }
    }

    public int findPinIndex(Pin p,Pin[] pins) {
        for(int i=0; i<pins.length; ++i) {
            if(pins[i].equals(p))
                return i;
        }
        return -1;
    }

    public boolean stateIsDown(PinState state) {
        return state == PinState.HIGH ? false:true;
    }


    public void showAll(RowProvider[] providers) {
        for(RowProvider p : providers) {
            showRow(p.outputPins,p.inputPins);
        }
    }

    public void showRow(GpioPinDigitalOutput[] outputPins,GpioPinDigitalInput[] inputPins) {
        for(int i=0; i<BANK_SIZE; i++) {
            System.out.printf("%c ",stateIsDown(inputPins[i].getState()) ? 'X':'O');
            gpio.setState(stateIsDown(inputPins[i].getState()),outputPins[i]);
        }
        System.out.printf("\n");
    }

    public void fullBoardTest() throws Exception {
        chessBoard = new ChessBoard();
        gpio = GpioFactory.getInstance();
        initHardware();
        showBoard();
    }
    private void initHardware() throws IOException, I2CFactory.UnsupportedBusNumberException {
        final int bus = I2CBus.BUS_1;

        chessBoardController = new BoardController(gpio,bus);
        chessBoardController.addListener(this);
    }

    @Override
    public void pieceUp(int boardIndex) {
        show("UP  ",boardIndex);
    }

    @Override
    public void pieceDown(int boardIndex) {
        show("DOWN",boardIndex);
    }

    public void show(String msg,int boardIndex) {
        System.out.println(msg+" "+chessBoard.indexToBoard(boardIndex)+" "+boardIndex);
        showBoard();
    }

    public void showBoard() {
        for(int i = 0; i < 64; i++) {
            if(i>0 && i%8==0)
                System.out.println("");
            System.out.printf("%c ",chessBoardController.hasPiece(i) ? 'X':'O');
            chessBoardController.led(i,false);
//            chessBoardController.led(i,chessBoardController.hasPiece(i));
        }
        System.out.printf("\n");
    }
}
