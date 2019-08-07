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

import javax.swing.*;
import java.awt.*;

/**
 * Hello world!
 *
 */
public class ChessLR {
    static AppFrame instance;

    public static void main( String[] args ) {
        try {
            instance = new AppFrame("ChessLR");
            if(instance.prefs.getBounds() != null) {
                Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                if(maxBounds.contains(instance.prefs.getBounds())) {
                    instance.setBounds(instance.prefs.getBounds());
                } else {
                    setDefaultSize();   //part or all of the window is positioned outside of the screen
                }
            } else {
                setDefaultSize();
            }
            instance.startApp();
        } catch(Exception e) {
            handleError("Unable to start application",e);
        }
    }

    public static void setDefaultSize() {
        instance.setSize(720,720);
        instance.setLocationRelativeTo(null);
    }

    public static void handleError(String msg,Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(instance,msg,"Error",JOptionPane.ERROR_MESSAGE);
    }

}
