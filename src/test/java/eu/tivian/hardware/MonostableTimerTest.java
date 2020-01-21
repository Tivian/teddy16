package eu.tivian.hardware;

import eu.tivian.other.SI;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for mono-stable operating NE555 timer.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see MonostableTimer
 */
class MonostableTimerTest {
    /**
     * Tests if timer output changes from LOW to HIGH and then to LOW due to time elapsed.
     */
    @Test
    void logic() {
        AtomicInteger result = new AtomicInteger();
        MonostableTimer timer = new MonostableTimer(47 * SI.KILO, 10 * SI.MICRO);
        Pin sense = new Pin(Pin.Direction.INPUT);

        timer.output.connect(sense);
        sense.onChange(() -> result.addAndGet(sense.level() == Pin.Level.HIGH ? 13 : 7));

        timer.trigger();

        assertEquals(20, result.get());
    }
}