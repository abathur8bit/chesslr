/*
 * Created by JFormDesigner on Thu Sep 12 18:51:54 EDT 2019
 */

package com.axorion.chesslr;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * @author Lee Patterson
 */
public class SettingsDialog extends JDialog {
    AppFrame parent;
    protected int selectedOption = 0;
    public boolean newOnePlayer = false;
    public boolean newTwoPlayer = false;
    public boolean asBlack = false;

    public SettingsDialog(AppFrame owner) {
        super(owner);
        this.parent = owner;    //using the AppFrame for button image loading for the moment
        initComponents();
        setSize(480,780);
        parent.setButtonImage(onePlayerButton,"button-onePlayerWhite.png");
        parent.setButtonImage(twoPlayerButton,"button-twoPlayer.png");
        parent.setButtonImage(onePlayerBlackButton,"button-onePlayerBlack.png");
    }

    public SettingsDialog(Dialog owner) {
        super(owner);
        initComponents();
    }

    /** Show the dialog and reset new game settings. */
    public void open() {
        newOnePlayer = false;
        newTwoPlayer = false;
        asBlack = false;
        super.setVisible(true);
    }

    public void setPgnNotation(boolean pgn) {
        pgnNotationCheckbox.setSelected(pgn);
    }

    public boolean isPgnNotation() {
        return pgnNotationCheckbox.isSelected();
    }

    public boolean isNewOnePlayer() {
        boolean b = newOnePlayer;
        newOnePlayer = false;
        return b;
    }

    public boolean isNewTwoPlayer() {
        boolean b = newTwoPlayer;
        newTwoPlayer = false;
        return b;
    }

    public int getSelectedOption() {return selectedOption;}

    private void okButtonActionPerformed() {
        selectedOption = JOptionPane.OK_OPTION;
        setVisible(false);
    }

    private void cancelButtonActionPerformed() {
        selectedOption = JOptionPane.CANCEL_OPTION;
        setVisible(false);
    }

    private void onePlayerButtonActionPerformed() {
        newOnePlayer = true;
        setVisible(false);
    }

    private void twoPlayerButtonActionPerformed() {
        newTwoPlayer = true;
        setVisible(false);
    }

    private void onePlayerBlackButtonActionPerformed() {
        newOnePlayer = true;
        asBlack = true;
        setVisible(false);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        generalPanel = new JPanel();
        pgnNotationCheckbox = new JCheckBox();
        gamePanel = new JPanel();
        onePlayerButton = new JButton();
        onePlayerBlackButton = new JButton();
        twoPlayerButton = new JButton();
        enginePanel = new JPanel();
        checkBox7 = new JCheckBox();
        buttonBar = new JPanel();
        okButton = new JButton();

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

                    //---- onePlayerButton ----
                    onePlayerButton.setText("1");
                    onePlayerButton.setPreferredSize(new Dimension(78, 50));
                    onePlayerButton.addActionListener(e -> onePlayerButtonActionPerformed());
                    gamePanel.add(onePlayerButton);

                    //---- onePlayerBlackButton ----
                    onePlayerBlackButton.setText("1 B");
                    onePlayerBlackButton.setPreferredSize(new Dimension(78, 50));
                    onePlayerBlackButton.addActionListener(e -> onePlayerBlackButtonActionPerformed());
                    gamePanel.add(onePlayerBlackButton);

                    //---- twoPlayerButton ----
                    twoPlayerButton.setText("2");
                    twoPlayerButton.setPreferredSize(new Dimension(78, 50));
                    twoPlayerButton.addActionListener(e -> twoPlayerButtonActionPerformed());
                    gamePanel.add(twoPlayerButton);
                }
                contentPanel.add(gamePanel);

                //======== enginePanel ========
                {
                    enginePanel.setBorder(new TitledBorder("Engine"));
                    enginePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- checkBox7 ----
                    checkBox7.setText("Show evaluation");
                    enginePanel.add(checkBox7);
                }
                contentPanel.add(enginePanel);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new FlowLayout(FlowLayout.RIGHT));

                //---- okButton ----
                okButton.setText("Close");
                okButton.setPreferredSize(new Dimension(78, 50));
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton);
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
    private JButton onePlayerButton;
    private JButton onePlayerBlackButton;
    private JButton twoPlayerButton;
    private JPanel enginePanel;
    private JCheckBox checkBox7;
    private JPanel buttonBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
