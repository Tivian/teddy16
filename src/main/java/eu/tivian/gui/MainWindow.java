package eu.tivian.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Main window of the emulator.
 *
 * @author Paweł Kania
 * @since 2019-11-11
 * @see RenderPanel
 */
public class MainWindow extends JPanel {
    /**
     * Pixel panel to display screen information provided by the emulator.
     */
    private RenderPanel panel;

    /**
     * Initializes the main window.
     */
    public MainWindow() {
        super(new BorderLayout());

        //this.setTitle("teddy16");
        // TODO make icon more visible
        /*try {
            this.setIconImage(ImageIO.read(Objects.requireNonNull(getClass().getClassLoader().getResource("icon.png"))));
        } catch (IOException ex) { }*/

        panel = new RenderPanel();
        this.setSize(720, 576);
        //this.setLayout();
        this.add(panel);
        //this.pack();
        //this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /**
     * Sets pixel at position ({@code x}, {@code y}) with color given by the {@code color} parameter
     * which corresponds to the n-th color of the {@link Palette}.
     *
     * @param x column
     * @param y row
     * @param color n-th color of the selected palette
     * @throws IndexOutOfBoundsException if the {@code x} parameter is out of range (x &lt; 0 || x &ge; 720)
     * @throws IndexOutOfBoundsException if the {@code y} parameter is out of range (y &lt; 0 || y &ge; 576)
     * @throws IndexOutOfBoundsException if the {@code color} parameter exceeds its range
     *         (color &lt; 0 || color &ge; 121)
     */
    public void set(int x, int y, int color) {
        //if (x == 0 && y == 0)
            panel.repaint();

        panel.image().setRGB(x, y, panel.palette().get(color).getRGB());
    }
}
