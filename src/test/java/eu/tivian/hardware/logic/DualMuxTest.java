package eu.tivian.hardware.logic;

import eu.tivian.hardware.Bus;
import eu.tivian.hardware.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2-line to 4-line input multiplexer unit tests.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see DualMux
 */
class DualMuxTest {
    /**
     * Tests basic logic functionality of the multiplexer.
     */
    @Test
    void logic() {
        Pin inEnable = new Pin("enable", Pin.Direction.INPUT);
        Pin inSelect = new Pin("select", Pin.Direction.INPUT);
        DualMux mux = new DualMux(inEnable, inSelect, true);
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        Pin select = new Pin(Pin.Direction.OUTPUT);
        Pin enable = new Pin(Pin.Direction.OUTPUT);

        mux.inputA.connect(A);
        mux.inputB.connect(B);
        inEnable.connect(enable);
        inSelect.connect(select);
        assertEquals(Pin.Level.LOW, mux.output.level());

        enable.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, mux.output.level());
        assertEquals(Pin.Direction.HI_Z, mux.output.direction());

        enable.level(Pin.Level.LOW);
        select.level(Pin.Level.LOW);

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, mux.output.level());
        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, mux.output.level());

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, mux.output.level());
        B.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, mux.output.level());

        select.level(Pin.Level.HIGH);

        B.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, mux.output.level());
        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, mux.output.level());

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, mux.output.level());
        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, mux.output.level());
    }

    /**
     * Tests if RAM multiplexing works as supposed.
     */
    @Test
    void ramMux() {
        IC74LS257 mux1 = new IC74LS257(), mux2 = new IC74LS257();
        Bus address = new Bus("address", "A", Pin.Direction.OUTPUT, 16);
        Bus output = new Bus("output", "O", Pin.Direction.INPUT, 8);
        Pin VCC = Pin.VCC, GND = Pin.GND;
        Pin select = new Pin(Pin.Direction.OUTPUT);

        mux1.get(0).inputA.connect(VCC);
        mux1.get(0).inputB.connect(address.get( 0));
        mux1.get(1).inputA.connect(address.get( 8));
        mux1.get(1).inputB.connect(address.get( 1));
        mux1.get(2).inputA.connect(address.get( 9));
        mux1.get(2).inputB.connect(address.get( 2));
        mux1.get(3).inputA.connect(address.get(10));
        mux1.get(3).inputB.connect(address.get( 3));
        mux1.enable.connect(GND);
        mux1.select.connect(select);

        mux2.get(0).inputA.connect(address.get(11));
        mux2.get(0).inputB.connect(address.get( 4));
        mux2.get(1).inputA.connect(address.get(12));
        mux2.get(1).inputB.connect(address.get( 5));
        mux2.get(2).inputA.connect(address.get(13));
        mux2.get(2).inputB.connect(address.get( 6));
        mux2.get(3).inputA.connect(VCC);
        mux2.get(3).inputB.connect(address.get( 7));
        mux2.enable.connect(GND);
        mux2.select.connect(select);

        output.get(0).connect(mux1.get(0).output);
        output.get(1).connect(mux1.get(1).output);
        output.get(2).connect(mux1.get(2).output);
        output.get(3).connect(mux1.get(3).output);
        output.get(4).connect(mux2.get(0).output);
        output.get(5).connect(mux2.get(1).output);
        output.get(6).connect(mux2.get(2).output);
        output.get(7).connect(mux2.get(3).output);

        address.value(0x01FF);

        select.level(Pin.Level.LOW);
        assertEquals(0x83, output.value());

        select.level(Pin.Level.HIGH);
        assertEquals(0xFF, output.value());
    }
}