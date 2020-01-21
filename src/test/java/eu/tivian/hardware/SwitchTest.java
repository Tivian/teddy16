package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for switches.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see Switch
 */
class SwitchTest {
    /**
     * Checks if internal switch counter counts correctly.
     */
    @Test
    void count() {
        new Switch(new Pin(), new Pin());
        new Switch(new Pin(), new Pin());
        new Switch(new Pin(), new Pin());

        assertEquals("SW3", new Switch(new Pin(), new Pin()).toString());
    }

    /**
     * Checks if the switch behaves as it should.
     */
    @Test
    void logic() {
        Pin power = Pin.VCC;
        Pin output = new Pin(Pin.Direction.INPUT);
        Switch sw = new Switch(power, output);

        assertEquals(Pin.Level.HIGH,  power.level());
        assertEquals(Pin.Level.LOW , output.level());

        sw.on();
        assertEquals(Pin.Level.HIGH,  power.level());
        assertEquals(Pin.Level.HIGH, output.level());

        sw.off();
        assertEquals(Pin.Level.HIGH,  power.level());
        assertEquals(Pin.Level.LOW , output.level());

        AtomicInteger result = new AtomicInteger();
        output.onChange(() -> result.addAndGet(output.level() == Pin.Level.HIGH ? 13 : 7));
        sw.monostable();
        assertEquals(20, result.get());

        sw.toggle();
        assertEquals(Pin.Level.HIGH,  power.level());
        assertEquals(Pin.Level.HIGH, output.level());

        sw.toggle();
        assertEquals(Pin.Level.HIGH,  power.level());
        assertEquals(Pin.Level.LOW , output.level());
    }
}