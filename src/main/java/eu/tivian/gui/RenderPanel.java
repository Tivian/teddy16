package eu.tivian.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

public class RenderPanel extends JPanel {
    private int width;
    private int height;
    private Palette pal;

    public RenderPanel() {
        this.width = 720;
        this.height = 576;
        this.pal = new Palette();

        this.setSize(width, height);
    }

    private Image render() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        /*var graph = image.getGraphics();

        int i = 0;
        for (int j = 0; j < 8; j++) {
            for (int k = 0; k < 16; k++) {
                graph.setColor(pal.get(i++));
                graph.fillRect(k * 64, j * 8, 64, 8 );
            }
        }*/

        byte[] raw = TestImage.get(0);
        int[] rgb = IntStream.range(0, raw.length * 2).map(i -> pal.get(raw[i / 2]).getRGB()).toArray();
        image.setRGB(0, 0, 320, 200, rgb, 0, 320);

        return image;
    }

    public void paint(Graphics g){
        g.drawImage(render(), 0, 0, this);
    }
}
