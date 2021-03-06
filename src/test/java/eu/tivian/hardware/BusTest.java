package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the bus.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see Bus
 */
class BusTest {
    /**
     * Checks if connecting two buses yields correct values.
     */
    @Test
    void connect() {
        Bus output = new Bus("out", "O", Pin.Direction.OUTPUT, 16);
        Bus input  = new Bus("in" , "I", Pin.Direction.INPUT , 16);
        output.connect(input);

        long[] testValues = new long[] { 0x0000, 0x0001, 0x1001, 0x1000, 0x5555, 0xAAAA, 0xFF00, 0x00FF, 0xFFFF };
        for (long val : testValues) {
            output.value(val);
            assertEquals(val, output.value());
            assertEquals(val, input.value());
        }
    }

    /**
     * Checks if change at one end makes appropriate changes on the other end.
     */
    @Test
    void change() {
        AtomicLong result = new AtomicLong();
        Bus output = new Bus("out", "O", Pin.Direction.OUTPUT, 16);
        Bus input  = new Bus("in" , "I", Pin.Direction.INPUT , 16);

        output.connect(input);
        input.onChange(() -> result.set(input.value()));

        long[] testValues = new long[] { 0x0000, 0x0001, 0x1001, 0x1000, 0x5555, 0xAAAA, 0xFF00, 0x00FF, 0xFFFF };
        for (long val : testValues) {
            output.value(val);
            assertEquals(val, result.get());
        }
    }
}