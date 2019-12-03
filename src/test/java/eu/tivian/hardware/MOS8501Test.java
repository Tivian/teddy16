package eu.tivian.hardware;

import eu.tivian.software.Monitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class MOS8501Test {
    private class MockCPU extends MOS8501 {
        byte[] memory = new byte[0x10000];

        @Override
        protected void read(short address, Consumer<Byte> readCycle) {
            if (readCycle != null)
                readCycle.accept(memory[address & 0xFFFF]);
            //halfCycleIn = () -> memory[address & 0xFFFF];
            //halfCycleOut = readCycle;
        }

        @Override
        protected void write(short address, byte value) {
            memory[address & 0xFFFF] = value;
            //halfCycleIn = () -> value;
            //halfCycleOut = data -> memory[address & 0xFFFF] = data;
        }
    }

    private static Map<String, int[]> testCases = new HashMap<String, int[]>() {{
        put("/6502_functional_test.bin", new int[] { 0x0400, 0x3469, 150195818 });
        put("/6502_decimal_test.bin"   , new int[] { 0x0400, 0x044B,  53953824 });
    }};

    //@Disabled
    @Test
    void step() {
        MockCPU cpu = new MockCPU();
        Pin reset = new Pin(Pin.Direction.OUTPUT, Pin.Level.HIGH);
        Pin irq = new Pin(Pin.Direction.OUTPUT, Pin.Level.HIGH);
        Pin clock = new Pin(Pin.Direction.OUTPUT);

        Monitor monitor = new Monitor(
            addr -> cpu.memory[addr & 0xFFFF],
            (addr, data) -> cpu.memory[addr & 0xFFFF] = data,
            cpu::reg
        );

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
            //System.out.println(cpu.cycles());
            //assertTrue(cpu.cycles() > 1e8);
        }
    }

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