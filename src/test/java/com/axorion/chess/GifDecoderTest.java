package com.axorion.chess;

import com.axorion.chesslr.GifDecoder;
import org.junit.Test;

import java.awt.image.BufferedImage;

public class GifDecoderTest {
    @Test public void testRead() {
        GifDecoder gif = new GifDecoder();
        gif.read("/Users/lee/workspace/chesslr/chesslr/images/sweap.gif");
        int n = gif.getFrameCount();
        for(int i=0; i<n; i++) {
            BufferedImage frame = gif.getFrame(i);
            int delay = gif.getDelay(i);
            System.out.printf("frame %d delay %d\n",i,delay);
            showFrame(frame);
        }
    }

    public void showFrame(BufferedImage frame) {
        int MASK_RGB    = 0x00FFFFFF;

        int w = frame.getWidth();
        int h = frame.getHeight();
        for(int y=0; y<h; y++) {
            for(int x=0; x<w; x++) {
                int rgb = frame.getRGB(x,y);
                if((rgb&MASK_RGB) > 0)
                    System.out.printf("X");
                else
                    System.out.printf("O");
            }
            System.out.printf("\n");
        }
    }
}
