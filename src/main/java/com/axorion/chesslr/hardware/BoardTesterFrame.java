/*
 * Created by JFormDesigner on Sat Sep 07 14:36:35 EDT 2019
 */

package com.axorion.chesslr.hardware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Lee Patterson
 */
public class BoardTesterFrame extends JFrame {
    static GpioController gpio;
    LEDController ledController;
    InputController inputController;
    int ledIndex = 0;
    int bus = 1;
    Color defaultButtonColor;
    int maxLed = 8;
    JCheckBox inputCheckboxes[];

    public static void main(String[] args) throws Exception {
        BoardTesterFrame f = new BoardTesterFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }
    public BoardTesterFrame() throws Exception {
        initComponents();
        inputCheckboxes = new JCheckBox[] {buttonCheckBox0,buttonCheckBox1,buttonCheckBox2,buttonCheckBox3,buttonCheckBox4,buttonCheckBox5,buttonCheckBox6,buttonCheckBox7};
        movesAutoScroll();
    }

    private void movesAutoScroll() {
        DefaultCaret caret = (DefaultCaret) outputTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }


    public void initHardware() throws Exception {
       gpio = GpioFactory.getInstance();
       int address = toInt(addressTextField.getText());
       int bank = Integer.parseInt(bankTextField.getText());
       int inputAddress = toInt(inputAddressTextField.getText());
       int inputBank = Integer.parseInt(inputBankTextField.getText());
       maxLed = toInt(maxLedTextField.getText());
       log(String.format("Using BUS %d\nmaxLed %d\nled address 0x%02X (%d) led bank %d\ninput address 0x%02X (%d) input bank %d",bus,maxLed,address,address,bank,inputAddress,inputAddress,inputBank));
       ledController = new LEDTesterController(gpio,bus,address,bank);
       inputController = new InputTesterController(gpio,bus,inputAddress,inputBank);
//       inputController.addListener(new GpioPinListenerDigital() {
//            public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
//
//                final Pin pin = event.getPin().getPin();
//                final int index = inputController.findPinIndex(pin);
//
//                inputCheckboxes[index].setSelected(inputController.stateIsDown(event.getState()));
//            }
//        });

    }

    public int toInt(String s) {
        int radix = 10;
        if(s.startsWith("0x")) {
            s = s.substring(2);
            radix = 16;
        }
        return Integer.parseInt(s,radix);
    }

    public void log(String msg) {
        outputTextArea.setText(outputTextArea.getText()+msg+"\r\n");
        outputTextArea.setCaretPosition(outputTextArea.getText().length());
    }

    public void showBusIds() throws Exception {
        int[] ids = I2CFactory.getBusIds();
        System.out.println("Found follow I2C busses: " + Arrays.toString(ids));

        // find available busses
        for (int number = I2CBus.BUS_0; number <= I2CBus.BUS_17; ++number) {
            try {
                @SuppressWarnings("unused")
                I2CBus bus = I2CFactory.getInstance(number);
                System.out.println("Supported I2C BUS "+number+" found");
            } catch(IOException exception) {
                System.out.println("I/O error on I2C BUS "+number+" occurred");
            } catch(I2CFactory.UnsupportedBusNumberException exception) {
                System.out.println("Unsupported I2C BUS "+number);
            }
        }

    }

    private void ledButtonActionPerformed(ActionEvent e) {
        boolean state = !ledController.isOn(ledIndex);
        ledController.led(ledIndex,state);
        updateLEDColor();
    }

    private void updateLEDColor() {
        if(defaultButtonColor == null) {
            defaultButtonColor = ledButton.getBackground();
        }
        boolean state = ledController.isOn(ledIndex);
        if(state) {
            ledButton.setBackground(Color.yellow);
        } else {
            ledButton.setBackground(defaultButtonColor);
        }
    }

    private void enableButtons() {
        boolean enable = (ledController == null ? false:true);
        ledButton.setEnabled(enable);
        nextButton.setEnabled(enable);
        prevButton.setEnabled(enable);
    }

    private void nextButtonActionPerformed(ActionEvent e) {
        ledIndex++;
        if(ledIndex >= maxLed) {
            ledIndex = 0;
        }
        ledButton.setText("LED "+ledIndex);
        updateLEDColor();
    }

    private void prevButtonActionPerformed(ActionEvent e) {
        ledIndex--;
        if(ledIndex < 0) {
            ledIndex = maxLed-1;
        }
        ledButton.setText("LED "+ledIndex);
        updateLEDColor();
    }

    private void initButtonActionPerformed(ActionEvent ev) {
        try {
            initHardware();
        } catch(Exception e) {
            log("Unable to init, error "+e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        scrollPane1 = new JScrollPane();
        outputTextArea = new JTextArea();
        lowerPanel = new JPanel();
        buttonPanel = new JPanel();
        initButton = new JButton();
        prevButton = new JButton();
        nextButton = new JButton();
        ledButton = new JButton();
        panel3 = new JPanel();
        buttonCheckBox0 = new JCheckBox();
        buttonCheckBox1 = new JCheckBox();
        buttonCheckBox2 = new JCheckBox();
        buttonCheckBox3 = new JCheckBox();
        buttonCheckBox4 = new JCheckBox();
        buttonCheckBox5 = new JCheckBox();
        buttonCheckBox6 = new JCheckBox();
        buttonCheckBox7 = new JCheckBox();
        inputPanel = new JPanel();
        label2 = new JLabel();
        bankTextField = new JTextField();
        label1 = new JLabel();
        addressTextField = new JTextField();
        label5 = new JLabel();
        maxLedTextField = new JTextField();
        label3 = new JLabel();
        inputBankTextField = new JTextField();
        label4 = new JLabel();
        inputAddressTextField = new JTextField();

        //======== this ========
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== scrollPane1 ========
        {

            //---- outputTextArea ----
            outputTextArea.setPreferredSize(new Dimension(1, 150));
            outputTextArea.setLineWrap(true);
            outputTextArea.setWrapStyleWord(true);
            scrollPane1.setViewportView(outputTextArea);
        }
        contentPane.add(scrollPane1, BorderLayout.CENTER);

        //======== lowerPanel ========
        {
            lowerPanel.setLayout(new BorderLayout());

            //======== buttonPanel ========
            {
                buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

                //---- initButton ----
                initButton.setText("Init");
                initButton.setPreferredSize(new Dimension(78, 50));
                initButton.addActionListener(e -> initButtonActionPerformed(e));
                buttonPanel.add(initButton);

                //---- prevButton ----
                prevButton.setText("-");
                prevButton.setPreferredSize(new Dimension(78, 50));
                prevButton.addActionListener(e -> prevButtonActionPerformed(e));
                buttonPanel.add(prevButton);

                //---- nextButton ----
                nextButton.setText("+");
                nextButton.setPreferredSize(new Dimension(78, 50));
                nextButton.addActionListener(e -> nextButtonActionPerformed(e));
                buttonPanel.add(nextButton);

                //---- ledButton ----
                ledButton.setText("LED 0");
                ledButton.setPreferredSize(new Dimension(78, 50));
                ledButton.addActionListener(e -> ledButtonActionPerformed(e));
                buttonPanel.add(ledButton);
            }
            lowerPanel.add(buttonPanel, BorderLayout.SOUTH);

            //======== panel3 ========
            {
                panel3.setLayout(new GridLayout(0, 4, -2, 0));

                //---- buttonCheckBox0 ----
                buttonCheckBox0.setText("Button 0");
                panel3.add(buttonCheckBox0);

                //---- buttonCheckBox1 ----
                buttonCheckBox1.setText("Button 1");
                panel3.add(buttonCheckBox1);

                //---- buttonCheckBox2 ----
                buttonCheckBox2.setText("Button 2");
                panel3.add(buttonCheckBox2);

                //---- buttonCheckBox3 ----
                buttonCheckBox3.setText("Button 3");
                panel3.add(buttonCheckBox3);

                //---- buttonCheckBox4 ----
                buttonCheckBox4.setText("Button 4");
                panel3.add(buttonCheckBox4);

                //---- buttonCheckBox5 ----
                buttonCheckBox5.setText("Button 5");
                panel3.add(buttonCheckBox5);

                //---- buttonCheckBox6 ----
                buttonCheckBox6.setText("Button 6");
                panel3.add(buttonCheckBox6);

                //---- buttonCheckBox7 ----
                buttonCheckBox7.setText("Button 7");
                panel3.add(buttonCheckBox7);
            }
            lowerPanel.add(panel3, BorderLayout.CENTER);
        }
        contentPane.add(lowerPanel, BorderLayout.SOUTH);

        //======== inputPanel ========
        {
            inputPanel.setLayout(new GridLayout(0, 2));

            //---- label2 ----
            label2.setText("LED Bank");
            inputPanel.add(label2);

            //---- bankTextField ----
            bankTextField.setText("1");
            inputPanel.add(bankTextField);

            //---- label1 ----
            label1.setText("Address");
            inputPanel.add(label1);

            //---- addressTextField ----
            addressTextField.setText("0x20");
            inputPanel.add(addressTextField);

            //---- label5 ----
            label5.setText("Num LEDs");
            inputPanel.add(label5);

            //---- maxLedTextField ----
            maxLedTextField.setText("8");
            inputPanel.add(maxLedTextField);

            //---- label3 ----
            label3.setText("Input Bank");
            inputPanel.add(label3);

            //---- inputBankTextField ----
            inputBankTextField.setText("0");
            inputPanel.add(inputBankTextField);

            //---- label4 ----
            label4.setText("Input Address");
            inputPanel.add(label4);

            //---- inputAddressTextField ----
            inputAddressTextField.setText("0x20");
            inputPanel.add(inputAddressTextField);
        }
        contentPane.add(inputPanel, BorderLayout.NORTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JScrollPane scrollPane1;
    private JTextArea outputTextArea;
    private JPanel lowerPanel;
    private JPanel buttonPanel;
    private JButton initButton;
    private JButton prevButton;
    private JButton nextButton;
    private JButton ledButton;
    private JPanel panel3;
    private JCheckBox buttonCheckBox0;
    private JCheckBox buttonCheckBox1;
    private JCheckBox buttonCheckBox2;
    private JCheckBox buttonCheckBox3;
    private JCheckBox buttonCheckBox4;
    private JCheckBox buttonCheckBox5;
    private JCheckBox buttonCheckBox6;
    private JCheckBox buttonCheckBox7;
    private JPanel inputPanel;
    private JLabel label2;
    private JTextField bankTextField;
    private JLabel label1;
    private JTextField addressTextField;
    private JLabel label5;
    private JTextField maxLedTextField;
    private JLabel label3;
    private JTextField inputBankTextField;
    private JLabel label4;
    private JTextField inputAddressTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
