package eu.tivian.gui;

import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

class RenderPanelTest {
    private class RenderPanelMock extends RenderPanel {
        public int img = 0;

        BufferedImage render() {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            byte[] raw = TestImage.get(img);
            /*for (int x = 0; x < 160; x++) {
                for (int y = 0; y < 200 ; y++) {
                    image.setRGB(x, y, raw[y * 160 + x]);
                }
            }*/
            int[] rgb = IntStream.range(0, raw.length).map(i -> pal.get(raw[i]).getRGB()).toArray();
            image.setRGB(10, 20, 160, 200, rgb, 0, 160);

            return RenderPanel.scale(image, width * 4, height * 2);
        }

        @Override
        public void paint(Graphics g) {
            g.drawImage(render(), 0, 0, this);
        }
    }

    @Test
    @Ignore
    void show() {
        Thread gui = new Thread(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            RenderPanelMock panel = new RenderPanelMock();
            panel.img = 0;
            panel.setOpaque(true);
            frame.setContentPane(panel);

            frame.setTitle(TestImage.name(panel.img));
            frame.setResizable(false);
            frame.setVisible(true);
            frame.setSize(720, 576);
        });

        SwingUtilities.invokeLater(gui);
        for (;;);
    }
}