/*
 * Created by JFormDesigner on Thu Sep 12 18:51:54 EDT 2019
 */

package com.axorion.chesslr;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;

/**
 * @author Lee Patterson
 */
public class SettingsDialog extends JDialog {
    AppFrame parent;
    protected int selectedOption = 0;
    public boolean newOnePlayer = false;
    public boolean newTwoPlayer = false;
    public boolean asBlack = false;
    ChessPrefs prefs;

    public SettingsDialog(AppFrame owner) {
        super(owner);
        this.parent = owner;    //using the AppFrame for button image loading for the moment
        initComponents();
        setSize(480,780);
        parent.setButtonImage(onePlayerWhiteButton,"button-onePlayerWhite.png");
        parent.setButtonImage(onePlayerBlackButton,"button-onePlayerBlack.png");
        parent.setButtonImage(twoPlayerWhiteButton,"button-twoPlayerWhite.png");
        parent.setButtonImage(twoPlayerBlackButton,"button-twoPlayerBlack.png");
    }

    public SettingsDialog(Dialog owner) {
        super(owner);
        initComponents();
    }

    /** Show the dialog and reset new game settings. */
    public void open(ChessPrefs prefs) {
        this.prefs = prefs;
        newOnePlayer = false;
        newTwoPlayer = false;
        asBlack = false;
        pgnNotationCheckbox.setSelected(prefs.isPgnNotation());
        showEvalCheckBox.setSelected(prefs.isShowEvaluation());
        disableEngineCheckBox.setSelected(prefs.isDisableEngine());
        setLevel(prefs.getLevel());
        setSlowMover(prefs.getSlowMover());
        setMoveTime(prefs.getMoveTime());

        super.setVisible(true);
    }

    protected void close() {
        prefs.setPgnNotation(pgnNotationCheckbox.isSelected());
        prefs.setShowEvaluation(showEvalCheckBox.isSelected());
        prefs.setDisableEngine(disableEngineCheckBox.isSelected());
        prefs.setLevel(getLevel());
        prefs.setSlowMover(getSlowMover());

        setVisible(false);
    }


    public int getLevel() {
        try {
            return Integer.parseInt(levelTextField.getText());
        } catch(NumberFormatException e) {
//            setLevel(0);
            return 0;
        }
    }

    public void setMoveTime(int moveTime) {
        if(moveTime < 0)
            moveTime = 0;
        this.moveTimeTextField.setText(""+moveTime);
    }

    public int getMoveTime() {
        try {
            return Integer.parseInt(moveTimeTextField.getText());
        } catch(NumberFormatException e) {
            setMoveTime(0);
            return 0;
        }
    }

    public void setLevel(int level) {
        if(level < 0)
            level = 0;
        if(level > 20)
            level = 20;

        this.levelTextField.setText(""+level);
    }

    public int getSlowMover() {
        try {
            return Integer.parseInt(slowMoverTextField.getText());
        } catch(NumberFormatException e) {
//            setSlowMover(10);
            return 10;
        }
    }

    public void setSlowMover(int level) {
        if(level < 10)
            level = 10;
        if(level > 1000)
            level = 1000;
        this.slowMoverTextField.setText(""+level);
    }

    public void setPgnNotation(boolean pgn) {
        pgnNotationCheckbox.setSelected(pgn);
    }

    public boolean isPgnNotation() {
        return pgnNotationCheckbox.isSelected();
    }

    public boolean isShowEvaluations() {
        return showEvalCheckBox.isSelected();
    }

    public boolean isDisableEngine() {
        return disableEngineCheckBox.isSelected();
    }

    public boolean isNewOnePlayer() {
        return newOnePlayer;
    }

    public boolean isNewTwoPlayer() {
        return newTwoPlayer;
    }

    public int getSelectedOption() {return selectedOption;}

    private void closeButtonActionPerformed() {
        selectedOption = JOptionPane.OK_OPTION;
        close();
    }

    private void onePlayerWhiteButtonActionPerformed() {
        newOnePlayer = true;
        asBlack = false;
        close();
    }

    private void onePlayerBlackButtonActionPerformed() {
        newOnePlayer = true;
        asBlack = true;
        close();
    }

    private void twoPlayerWhiteButtonActionPerformed() {
        newTwoPlayer = true;
        asBlack = false;
        close();
    }

    private void twoPlayerBlackButtonActionPerformed() {
        newTwoPlayer = true;
        asBlack = true;
        close();
    }

    private void ledButtonActionPerformed() {
        new Thread(() -> {
            try
            {
                for(int i=0; i<64; i++) {
                    parent.chessBoardController.led(i,true);
                    Thread.sleep(250);
                }
                for(int i=0; i<64; i++) {
                    parent.chessBoardController.led(i,false);
                    Thread.sleep(100);
                }
            }
            catch(Exception ex)
            {
                //do nothing
            }
        }).start();
    }

    private void xanimButtonActionPerformed() {
        new Thread(() -> {
            try {
                parent.showMatrixAnim("x-appear.gif");
            } catch(InterruptedException| IOException ex) {
                //do nothing
            }
        }).start();
    }

    private void sweapAnimButtonActionPerformed() {
        new Thread(() -> {
            try {
                parent.showMatrixAnim("sweap.gif");
            } catch(InterruptedException| IOException ex) {
                //do nothing
            }
        }).start();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        generalPanel = new JPanel();
        pgnNotationCheckbox = new JCheckBox();
        gamePanel = new JPanel();
        onePlayerWhiteButton = new JButton();
        onePlayerBlackButton = new JButton();
        twoPlayerWhiteButton = new JButton();
        twoPlayerBlackButton = new JButton();
        enginePanel = new JPanel();
        showEvalCheckBox = new JCheckBox();
        disableEngineCheckBox = new JCheckBox();
        label1 = new JLabel();
        levelTextField = new JTextField();
        label2 = new JLabel();
        slowMoverTextField = new JTextField();
        testPanel = new JPanel();
        xanimButton = new JButton();
        sweapAnimButton = new JButton();
        ledButton = new JButton();
        buttonBar = new JPanel();
        closeButton = new JButton();

        //======== this ========
        setModal(true);
        setResizable(false);
        setMinimumSize(new Dimension(480, 780));
        setTitle("Options");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridLayout(0, 1));

                //======== generalPanel ========
                {
                    generalPanel.setBorder(new TitledBorder("General"));
                    generalPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- pgnNotationCheckbox ----
                    pgnNotationCheckbox.setText("PGN Notation");
                    generalPanel.add(pgnNotationCheckbox);
                }
                contentPanel.add(generalPanel);

                //======== gamePanel ========
                {
                    gamePanel.setBorder(new TitledBorder("Game"));
                    gamePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- onePlayerWhiteButton ----
                    onePlayerWhiteButton.setText("1W");
                    onePlayerWhiteButton.setPreferredSize(new Dimension(78, 50));
                    onePlayerWhiteButton.addActionListener(e -> onePlayerWhiteButtonActionPerformed());
                    gamePanel.add(onePlayerWhiteButton);

                    //---- onePlayerBlackButton ----
                    onePlayerBlackButton.setText("1B");
                    onePlayerBlackButton.setPreferredSize(new Dimension(78, 50));
                    onePlayerBlackButton.addActionListener(e -> onePlayerBlackButtonActionPerformed());
                    gamePanel.add(onePlayerBlackButton);

                    //---- twoPlayerWhiteButton ----
                    twoPlayerWhiteButton.setText("2W");
                    twoPlayerWhiteButton.setPreferredSize(new Dimension(78, 50));
                    twoPlayerWhiteButton.addActionListener(e -> twoPlayerWhiteButtonActionPerformed());
                    gamePanel.add(twoPlayerWhiteButton);

                    //---- twoPlayerBlackButton ----
                    twoPlayerBlackButton.setText("2B");
                    twoPlayerBlackButton.setPreferredSize(new Dimension(78, 50));
                    twoPlayerBlackButton.addActionListener(e -> twoPlayerBlackButtonActionPerformed());
                    gamePanel.add(twoPlayerBlackButton);
                }
                contentPanel.add(gamePanel);

                //======== enginePanel ========
                {
                    enginePanel.setBorder(new TitledBorder("Engine"));
                    enginePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- showEvalCheckBox ----
                    showEvalCheckBox.setText("Show evaluation");
                    enginePanel.add(showEvalCheckBox);

                    //---- disableEngineCheckBox ----
                    disableEngineCheckBox.setText("Disable Engine");
                    enginePanel.add(disableEngineCheckBox);

                    //---- label1 ----
                    label1.setText("Level (0-20)");
                    enginePanel.add(label1);

                    //---- levelTextField ----
                    levelTextField.setPreferredSize(new Dimension(75, 30));
                    enginePanel.add(levelTextField);

                    //---- label2 ----
                    label2.setText("Slow Mover");
                    enginePanel.add(label2);

                    //---- slowMoverTextField ----
                    slowMoverTextField.setPreferredSize(new Dimension(75, 30));
                    enginePanel.add(slowMoverTextField);
                }
                contentPanel.add(enginePanel);

                //======== testPanel ========
                {
                    testPanel.setBorder(new TitledBorder("Test"));
                    testPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- xanimButton ----
                    xanimButton.setText("X");
                    xanimButton.setPreferredSize(new Dimension(78, 50));
                    xanimButton.addActionListener(e -> xanimButtonActionPerformed());
                    testPanel.add(xanimButton);

                    //---- sweapAnimButton ----
                    sweapAnimButton.setText("Sweap");
                    sweapAnimButton.setPreferredSize(new Dimension(78, 50));
                    sweapAnimButton.addActionListener(e -> sweapAnimButtonActionPerformed());
                    testPanel.add(sweapAnimButton);

                    //---- ledButton ----
                    ledButton.setText("LEDs");
                    ledButton.setPreferredSize(new Dimension(78, 50));
                    ledButton.addActionListener(e -> ledButtonActionPerformed());
                    testPanel.add(ledButton);
                }
                contentPanel.add(testPanel);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new FlowLayout(FlowLayout.RIGHT));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setPreferredSize(new Dimension(78, 50));
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton);
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel generalPanel;
    private JCheckBox pgnNotationCheckbox;
    private JPanel gamePanel;
    private JButton onePlayerWhiteButton;
    private JButton onePlayerBlackButton;
    private JButton twoPlayerWhiteButton;
    private JButton twoPlayerBlackButton;
    private JPanel enginePanel;
    private JCheckBox showEvalCheckBox;
    private JCheckBox disableEngineCheckBox;
    private JLabel label1;
    private JTextField levelTextField;
    private JLabel label2;
    private JTextField slowMoverTextField;
    private JPanel testPanel;
    private JButton xanimButton;
    private JButton sweapAnimButton;
    private JButton ledButton;
    private JPanel buttonBar;
    private JButton closeButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
