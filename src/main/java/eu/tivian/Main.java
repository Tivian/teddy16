package eu.tivian;

import eu.tivian.gui.MainWindow;
import eu.tivian.hardware.Motherboard;
import eu.tivian.other.SI;

import javax.swing.*;
import java.io.IOException;
import java.util.Random;

/*
      TODO list
-----------------------
1) CPU              [X]
2) TED              [ ]
3) PLA              [X]
4) ROM              [X]
5) RAM              [ ]
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
    //public static MOS8501 cpu = new MOS8501();
    //public static Monitor monitor = new Monitor(cpu);
    private static MainWindow window;

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("teddy16");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        window = new MainWindow();
        window.setOpaque(true); //content panes must be opaque
        frame.setContentPane(window);

        //Display the window.
        //frame.pack();
        frame.setSize(window.getSize());
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            new Thread(() -> {
                // X = [0, 455]
                // Y = [0, 311]
                /*long last = System.nanoTime();
                int x = 0, y = 0;
                while (true) {
                    window.set(x, y, new Random().nextInt() & 0x7F);

                    if (++x > 455) {
                        x = 0;
                        if (++y > 311) {
                            y = 0;
                            long now = System.nanoTime();
                            System.out.printf("%fms\n", (now - last) * (SI.NANO / SI.MILLI));
                            last = now;
                        }
                    }
                }*/
                Motherboard mb = new Motherboard();
                mb.render(window::set);
                mb.start();
            }).start();
        });
        //TED ted = new TED();

        //var window = new MainWindow();
        //window.setVisible(true);

        /*Scanner scanner = new Scanner(System.in);

        if (args.length != 0) {
            var file = Path.of(args[0]);
            if (Files.exists(file)) {
                try {
                    cpu.preload(Files.readAllBytes(file));
                } catch (IOException ex) { }
            }
        }

        /*cpu.preload(new byte[] {
            (byte) 0x08, (byte) 0x28,
            (byte) 0xa9, (byte) 0x00, (byte) 0x18,
            (byte) 0xe9, (byte) 0xff, (byte) 0x00
        }, 0x00);
        Wire reset = new Wire("reset");
        Pin start = new Pin("start button", Pin.Direction.OUTPUT);*/
        //reset.connect(cpu.reset).connect(start);
        //start.set(Pin.Value.HIGH);
        //cpu.fastBoot();
        /*cpu.PC = 0x0400;

        String line = "";
        int address = 0x0000;
        long last = 0;
        boolean cont = false;
        int counter = 0;
        long waitFor = 0;

        try {
            while (true) {
                if (cpu.step()) {
                    if (cont) {
                        if (waitFor != 0) {
                            if (cpu.getCycles() >= waitFor)
                                cont = false;
                        } else {
                            if (address == cpu.PC)
                                counter++;
                            if (counter == 10)
                                cont = false;
                        }

                        address = cpu.PC;
                        continue;
                    }

                    if (line == "") {
                        line = scanner.nextLine().toUpperCase();
                        try {
                            if (line.startsWith("M")) {
                                System.out.println(monitor.memory(Integer.parseInt(line.substring(1), 16)));
                                line = "";
                                continue;
                            } else if (line.startsWith("X")) {
                                cont = true;
                                waitFor = 0;
                                counter = 0;
                                System.out.println("Wait for trap...");
                            } else if (line.startsWith("C")) {
                                waitFor = Integer.parseInt(line.substring(1));
                                cont = true;
                            }

                            address = Integer.parseInt(line, 16);
                        } catch (NumberFormatException ex) {
                            line = "";
                        }
                    } else if (address <= cpu.PC && (address >> 8) == (cpu.PC >> 8)) {
                        line = "";
                    } else {
                        continue;
                    }

                    //cls();
                    System.out.println(cpu);
                    System.out.println(monitor.line());
                    //System.out.println(String.format("Test number: %d", cpu.peek(0x200)));
                    System.out.println("Cycles: " + cpu.getCycles());
                    //System.out.println(" (+" + (cpu.getCycles() - last) + ")");
                    //last = cpu.getCycles();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //Monitor monitor = new Monitor(cpu);
        //monitor.dump();*/
    }

    public static void cls(String... arg) throws IOException, InterruptedException {
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
    }
}
