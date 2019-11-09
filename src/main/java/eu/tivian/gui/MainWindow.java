package eu.tivian.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class MainWindow extends JPanel {
    private RenderPanel panel;

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
}
