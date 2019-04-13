/*
 * Created by JFormDesigner on Fri Apr 12 22:17:42 EDT 2019
 */

package com.axorion.chessboard;

import java.awt.*;
import javax.swing.*;

/**
 * @author Lee Patterson
 */
public class SimBoard extends JDialog {
    AppFrame parent;
    SimBoardPanel boardPanel;
    public SimBoard(AppFrame owner) {
        super(owner);
        this.parent = owner;
        initComponents();

        boardPanel = new SimBoardPanel();
        setSize(new Dimension(320,320));
        getContentPane().add(boardPanel,BorderLayout.CENTER);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license

        //======== this ========
        setTitle("Sim Board");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
