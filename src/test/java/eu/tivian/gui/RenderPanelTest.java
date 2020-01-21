package eu.tivian.gui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

/**
 * Unit tests for {@link RenderPanel}.
 *
 * @author Paweł Kania
 * @since 2019-12-08
 */
class RenderPanelTest {
    /**
     * Modified version of {@link RenderPanel} which takes example artwork as pixel data.
     */
    private class RenderPanelMock extends RenderPanel {
        /**
         * Index of artwork to display.
         */
        public int img = 0;

        /**
         * Renders the artwork to the {@link BufferedImage}.
         * @return image object representing chosen artwork
         */
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

        /**
         * Draws chosen artwork onto the panel.
         * @param g the {@code Graphics} context in which to paint
         */
        @Override
        public void paint(Graphics g) {
            g.drawImage(render(), 0, 0, this);
        }
    }

    /**
     * {@link RenderPanel} test.
     * Should show chosen artwork.
     */
    @Test
    @Disabled
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