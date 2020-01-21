package eu.tivian.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Panel to draw the pixel data from the emulator.
 *
 * @author Paweł Kania
 * @since 2019-12-08
 * @see MainWindow
 */
public class RenderPanel extends JPanel {
    /**
     * Width of the panel.
     * <br>For PAL output it should be 720 pixels wide.
     */
    protected int width;
    /**
     * Height of the panel.
     * <br>For PAL output it should be 576 pixels tall.
     */
    protected int height;

    /**
     * Palette which should be used to translate supplied data to corresponding colors.
     */
    protected Palette pal;
    /**
     * Image which should be drawn onto the emulator window.
     */
    protected BufferedImage image;

    /**
     * Initialization of the panel, by default for the PAL output.
     */
    public RenderPanel() {
        this.width = 720;
        this.height = 576;
        this.pal = new Palette();
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        this.setSize(width, height);
    }

    //private Image render() {
        /*int size = pal.size();
        byte r[] = new byte[size];
        byte g[] = new byte[size];
        byte b[] = new byte[size];

        for (int i = 0; i < size; i++) {
            Color c = pal.get(i);
            r[i] = (byte) c.getRed();
            g[i] = (byte) c.getGreen();
            b[i] = (byte) c.getBlue();
        }

        IndexColorModel cm = new IndexColorModel(8, size, r, g, b);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
         */
        //BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        /*var graph = image.getGraphics();

        int i = 0;
        for (int j = 0; j < 8; j++) {
            for (int k = 0; k < 16; k++) {
                graph.setColor(pal.get(i++));
                graph.fillRect(k * 64, j * 8, 64, 8 );
            }
        }*/

        //byte[] raw = TestImage.get(0);
        /*for (int x = 0; x < 160; x++) {
            for (int y = 0; y < 200 ; y++) {
                image.setRGB(x, y, raw[y * 160 + x]);
            }
        }*/
        //int[] rgb = IntStream.range(0, raw.length).map(i -> pal.get(raw[i]).getRGB()).toArray();
        //image.setRGB(0, 0, 160, 200, rgb, 0, 160);

        //return scale(image, width * 4, height * 2);
        //return image;
    //}

    /**
     * Returns the image which represents the C16 screen.
     * @return the image which represents the C16 screen
     */
    public BufferedImage image() {
        return image;
    }

    /**
     * Returns currently used palette.
     * @return currently used palette
     */
    public Palette palette() {
        return pal;
    }

    /**
     * Scales given image by given factors.
     *
     * @param imageToScale image which should be scaled
     * @param dWidth width factor
     * @param dHeight height factor
     * @return scaled image
     * @see Graphics2D#drawImage(Image, int, int, ImageObserver)
     */
    public static BufferedImage scale(BufferedImage imageToScale, int dWidth, int dHeight) {
        BufferedImage scaledImage = null;

        if (imageToScale != null) {
            scaledImage = new BufferedImage(dWidth, dHeight, imageToScale.getType());
            Graphics2D graphics2D = scaledImage.createGraphics();
            graphics2D.drawImage(imageToScale, 0, 0, dWidth, dHeight, null);
            graphics2D.dispose();
        }

        return scaledImage;
    }

    /**
     * Invoked by Swing to draw components.
     *
     * @param g the {@code Graphics} context in which to paint
     * @see JPanel#paint(Graphics)
     */
    @Override
    public void paint(Graphics g){
        g.drawImage(image, 0, 0, this);
    }
}
