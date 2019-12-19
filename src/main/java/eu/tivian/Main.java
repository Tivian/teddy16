package eu.tivian;

import eu.tivian.gui.MainWindow;
import eu.tivian.hardware.Motherboard;

import javax.swing.*;

/*
      TODO list
-----------------------
1) CPU              [X]
2) TED              [ ]
3) PLA              [X]
4) ROM              [X]
5) RAM              [X]
6) IC:
 - timer            [X]
 - quad flip-flop D [X]
 - quad NOR         [X]
 - hex inverter     [X]
 - dual 1-4 demux   [X]
 - quad 2-1 mux     [X]
 - quad buffer      [X]
 - SPI              [X]

 */

public class Main {
    private static MainWindow window;

    private static void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("teddy16");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        window = new MainWindow();
        window.setOpaque(true); //content panes must be opaque
        frame.setContentPane(window);

        // Display the window.
        //frame.pack();
        frame.setSize(window.getSize());
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            new Thread(() -> {
                Motherboard mb = new Motherboard();
                mb.render(window::set);
                mb.start();
            }).start();
        });
    }
}
