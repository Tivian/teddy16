package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quad demultiplexer unit tests.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see QuadDemux
 */
class QuadDemuxTest {
    /**
     * Tests all truth table combination for quad demultiplexer.
     */
    @Test
    void logic() {
        QuadDemux demux = new QuadDemux();
        Pin enable = new Pin(Pin.Direction.OUTPUT);
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);

        demux.enable.connect(enable);
        demux.A0.connect(A);
        demux.A1.connect(B);

        assertEquals(Pin.Level.LOW , demux.O0.level());
        assertEquals(Pin.Level.HIGH, demux.O1.level());
        assertEquals(Pin.Level.HIGH, demux.O2.level());
        assertEquals(Pin.Level.HIGH, demux.O3.level());

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, demux.O0.level());
        assertEquals(Pin.Level.LOW , demux.O1.level());
        assertEquals(Pin.Level.HIGH, demux.O2.level());
        assertEquals(Pin.Level.HIGH, demux.O3.level());

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, demux.O0.level());
        assertEquals(Pin.Level.HIGH, demux.O1.level());
        assertEquals(Pin.Level.HIGH, demux.O2.level());
        assertEquals(Pin.Level.LOW , demux.O3.level());

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, demux.O0.level());
        assertEquals(Pin.Level.HIGH, demux.O1.level());
        assertEquals(Pin.Level.LOW , demux.O2.level());
        assertEquals(Pin.Level.HIGH, demux.O3.level());

        enable.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, demux.O0.level());
        assertEquals(Pin.Level.HIGH, demux.O1.level());
        assertEquals(Pin.Level.HIGH, demux.O2.level());
        assertEquals(Pin.Level.HIGH, demux.O3.level());
    }
}
