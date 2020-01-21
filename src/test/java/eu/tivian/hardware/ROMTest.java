package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the ROM chip.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see RAM
 */
class ROMTest {
    /**
     * Helper function to provide array of random bytes.
     *
     * @param size size of array
     * @return array of random bytes
     */
    private static byte[] randomArray(int size) {
        byte[] array = new byte[size];
        new Random().nextBytes(array);
        return array;
    }

    /**
     * Tests if chip correctly responds to the given addresses.
     */
    @Test
    void logic() {
        int size = 0x4000;
        Bus address = new Bus("address", "A", Pin.Direction.OUTPUT, 16);
        Bus data    = new Bus("data", "D", Pin.Direction.INPUT, 8);
        Pin cs      = new Pin("chip select", Pin.Direction.OUTPUT);
        ROM rom     = new ROM(size);

        rom.address.connect(address);
        rom.data.connect(data);
        rom.cs.get(0).connect(new Pin(Pin.Direction.OUTPUT));
        rom.cs.get(1).connect(new Pin(Pin.Direction.OUTPUT));
        rom.cs.get(2).connect(cs);

        byte[] array = randomArray(size);
        rom.preload(array);

        for (int i = 0; i < size; i++)
            assertEquals(array[i], rom.peek(i));

        address.value(0xFF0F);
        assertEquals(Pin.Direction.HI_Z, rom.data.direction());
        assertEquals(0, rom.data.value());

        cs.level(Pin.Level.HIGH);
        assertEquals(Pin.Direction.OUTPUT, rom.data.direction());
        for (int i = 0; i < size; i++) {
            address.value(i);
            assertEquals(array[i], (byte) data.value());
        }
    }

    /**
     * Checks if preload function loads the file correctly.
     */
    @Test
    void preload() {
        int size = 0x10000;
        String fileName = "/6502_functional_test.bin";

        ROM rom = new ROM(size);
        assertDoesNotThrow(() -> rom.preload(Paths.get(getClass().getResource(fileName).toURI())));

        byte[] array = new byte[size];
        assertDoesNotThrow(() -> getClass().getResourceAsStream(fileName).read(array));

        for (int i = 0; i < size; i++)
            assertEquals(array[i], rom.peek(i));
    }
}