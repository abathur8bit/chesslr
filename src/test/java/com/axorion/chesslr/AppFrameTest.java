package com.axorion.chesslr;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppFrameTest {
    @Test
    @Ignore
    public void testGetFilename() throws Exception {
        AppFrame f = new AppFrame("xxx",false);

        String filename = f.getFilename();
        assertEquals("games/ChessLR-01000.txt",filename);
    }
}
