package eu.tivian.hardware;

import eu.tivian.software.SimpleCPU;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CPU test suite.
 * <br>Processor functionality tested thanks to assembly language code made by Klaus Dormann.
 * <br>Source code is available on <a href="https://github.com/amb5l/6502_65C02_functional_tests">GitHub</a>.
 *
 * @author Paweł Kania
 * @author Klaus Dormann
 * @since 2019-12-06
 * @see MOS8501
 */
class MOS8501Test {
    /**
     * List of assembly language test programs with start and stop positions.
     */
    private static Map<String, int[]> testCases = new HashMap<>() {{
        put("/6502_functional_test.bin", new int[] { 0x0400, 0x3469, 150195818 });
        put("/6502_decimal_test.bin"   , new int[] { 0x0400, 0x044B,  53953824 });
    }};

    /**
     * Checks if all valid instructions are evaluate correctly.
     */
    @Test
    void step() {
        SimpleCPU cpu = new SimpleCPU();
        Pin reset = new Pin(Pin.Direction.OUTPUT, Pin.Level.HIGH);
        Pin irq = new Pin(Pin.Direction.OUTPUT, Pin.Level.HIGH);
        Pin clock = new Pin(Pin.Direction.OUTPUT);

        /*Monitor monitor = new Monitor(
            addr -> cpu.memory[addr & 0xFFFF],
            (addr, data) -> cpu.memory[addr & 0xFFFF] = data,
            cpu::reg
        );*/

        //Path path = Paths.get("C:/Users/Pawel/Desktop/good.txt");

        cpu.reset.connect(reset);
        cpu.irq.connect(irq);
        cpu.phi0.connect(clock);

        for (Map.Entry<String, int[]> entry : testCases.entrySet()) {
            String fileName = entry.getKey();
            short start = (short) entry.getValue()[0];
            short end = (short) entry.getValue()[1];
            int cycles = entry.getValue()[2];

            System.out.printf("\n\nNow testing: %s\n", fileName);
            //System.out.println("  PC  SR AC XR YR SP  instruction");
            assertDoesNotThrow(() -> getClass().getResourceAsStream(fileName).read(cpu.memory));
            cpu.start();
            cpu.counter(start);

            assertEquals(start, cpu.counter());

            int counter = 0;
            short current = cpu.lastOpcodePosition();
            short last = current;

            while (cpu.counter() != end && counter < 16 && cpu.cycles() < 1e9) {
                clock.level(clock.level() == Pin.Level.LOW ? Pin.Level.HIGH : Pin.Level.LOW);
                if (clock.level() == Pin.Level.HIGH)
                    continue;
                //cpu.step();

                current = cpu.lastOpcodePosition();
                //System.out.printf("%04X %16d\n", cpu.PC, cpu.cycles());
                /*try {
                    Files.write(path, String.format("%04X %16d\n", cpu.PC, cpu.cycles()).getBytes(), StandardOpenOption.APPEND);
                } catch (IOException ex) {}*/

                if (current == last) {
                    counter++;
                } else {
                    counter = 0;
                    //System.out.println(monitor.walk(current));
                }
                last = current;
            }

            assertEquals(end, cpu.counter());
            //assertEquals(cycles, cpu.cycles());
            System.out.println(cpu.cycles());
            //assertTrue(cpu.cycles() > 1e8);
        }
    }

    /**
     * Checks if CPU is halted while the {@link MOS8501#reset} pin is held LOW.
     */
    @Test
    void reset() {
        MOS8501 cpu = new MOS8501();
        assertTrue(cpu.isHalted());

        Pin reset = new Pin(Pin.Direction.OUTPUT);
        cpu.reset.connect(reset);
        reset.level(Pin.Level.HIGH);

        assertFalse(cpu.isHalted());
    }
}