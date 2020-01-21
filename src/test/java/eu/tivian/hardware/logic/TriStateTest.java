package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tri-state gate unit tests.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see TriState
 */
class TriStateTest {
    /**
     * Tests all possible states of the input pins against correct output pin values.
     */
    @Test
    void logic() {
        TriState tri = new TriState();
        Pin enable = new Pin(Pin.Direction.OUTPUT);
        Pin driver = new Pin(Pin.Direction.OUTPUT);

        tri.input.connect(driver);
        tri.enable.connect(enable);

        enable.level(Pin.Level.LOW);
        assertEquals(Pin.Direction.OUTPUT, tri.output.direction());
        assertEquals(Pin.Level.LOW, tri.output.level());

        driver.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, tri.output.level());

        enable.level(Pin.Level.HIGH);
        assertEquals(Pin.Direction.HI_Z, tri.output.direction());
        assertEquals(Pin.Level.LOW, tri.output.level());
    }
}