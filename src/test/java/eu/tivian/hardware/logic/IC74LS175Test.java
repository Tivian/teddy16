package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quad D flip-flop unit tests.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see IC74LS175
 */
class IC74LS175Test {
    /**
     * Tests basic logic functionality of the chip.
     */
    @Test
    void logic() {
        IC74LS175 flipFlop = new IC74LS175();
        Pin clock = new Pin(Pin.Direction.OUTPUT);
        Pin reset = new Pin(Pin.Direction.OUTPUT);
        List<Pin> inputs = new ArrayList<>();

        flipFlop.clock.connect(clock);
        flipFlop.reset.connect(reset);
        for (int i = 0; i < 4; i++) {
            Pin input = new Pin("I" + i, Pin.Direction.OUTPUT);
            inputs.add(input);
            flipFlop.get(i).input.connect(input);
        }

        reset.level(Pin.Level.HIGH);
        for (int i = 0; i < 4; i++) {
            assertEquals(Pin.Level.LOW , flipFlop.get(i).output.level());
            assertEquals(Pin.Level.HIGH, flipFlop.get(i).revOut.level());
        }

        for (int i = 0; i < 4; i++)
            inputs.get(i).level((i % 2) == 0 ? Pin.Level.LOW : Pin.Level.HIGH);
        clock.level(Pin.Level.HIGH);
        clock.level(Pin.Level.LOW);

        for (int i = 0; i < 4; i++) {
            inputs.get(i).level((i % 2) == 0 ? Pin.Level.HIGH : Pin.Level.LOW);
            assertNotEquals(flipFlop.get(i).input.level(), flipFlop.get(i).output.level());
            assertEquals((i % 2) == 0 ? Pin.Level.LOW : Pin.Level.HIGH, flipFlop.get(i).output.level());
            assertEquals((i % 2) == 0 ? Pin.Level.HIGH : Pin.Level.LOW, flipFlop.get(i).revOut.level());
        }
    }
}