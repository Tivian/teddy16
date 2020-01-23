package eu.tivian;

import eu.tivian.gui.MainWindow;
import eu.tivian.hardware.Motherboard;
import eu.tivian.other.Logger;
import eu.tivian.software.Monitor;

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

/**
 * Starting point of the Commodore 16 emulator.
 * <br><img src="https://www.c64-wiki.com/images/1/13/C16.jpg" alt="Commodore 16 photo">
 *
 * @author Paweł Kania
 * @since 2019-10-29
 * @see <a href="https://www.c64-wiki.com/wiki/Commodore_16">C16 wiki page</a>
 */
public class Main {
    /**
     * Main window of the emulator.
     */
    private static MainWindow window;

    /**
     * Creates GUI.
     */
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

    /**
     * Parses arguments given by the {@code args} parameter.
     * <br>Right now only '-l' is supported.
     *
     * @param args command-line arguments
     */
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-"))
                continue;

            switch (args[i].charAt(1)) {
                case 'l':
                    Logger.ENABLE = true;
                    if (i < args.length - 1 && !args[i + 1].startsWith("-"))
                        Logger.redirect(args[++i]);
                    break;

                case 'm':
                    Monitor.start();
                    System.exit(0);
                    break;

                case 'v':
                    System.out.println("Teddy16 0.1\n2020-01-21\t by Paul Kania");
                    System.exit(0);
                    break;
            }
        }
    }

    /**
     * Starting point of the emulator.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        parseArgs(args);

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
