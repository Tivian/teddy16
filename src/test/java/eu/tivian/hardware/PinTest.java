package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PinTest {
    @Test
    void connect() {
        Pin A = new Pin("A", Pin.Direction.OUTPUT);
        Pin B = new Pin("B", Pin.Direction.INPUT);

        assertNull(A.wire());
        assertNull(B.wire());

        A.connect(B);
        assertEquals(A.wire(), B.wire());

        A.disconnect(B);
        B.connect(A);
        assertEquals(A.wire(), B.wire());
    }

    @Test
    void disconnect() {
        Pin A = new Pin("A", Pin.Direction.OUTPUT);
        Pin B = new Pin("B", Pin.Direction.INPUT );
        Pin C = new Pin("C", Pin.Direction.INPUT );

        A.connect(B);
        A.connect(C);
        assertEquals(A.wire(), B.wire());
        assertEquals(A.wire(), C.wire());

        C.disconnect();
        assertEquals(A.wire(), B.wire());
        assertNotEquals(A.wire(), C.wire());
        assertNull(C.wire());

        B.disconnect();
        assertNotEquals(A.wire(), B.wire());
        assertNull(B.wire());
        assertNotNull(A.wire());

        A.connect(B);
        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());

        B.disconnect(A);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.LOW, B.level());
    }

    @Test
    void direction() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.INPUT);
        A.connect(B);

        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());

        A.direction(Pin.Direction.INPUT);
        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());

        B.direction(Pin.Direction.OUTPUT);
        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());

        A.pullUp();
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());

        B.direction(Pin.Direction.HI_Z);
        assertEquals(Pin.Level.HIGH, A.level());

        A.pullUp(false);
        assertEquals(Pin.Level.LOW, A.level());

        A.direction(Pin.Direction.OUTPUT);
        B.direction(Pin.Direction.INPUT);

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());

        A.direction(Pin.Direction.INPUT);
        B.direction(Pin.Direction.OUTPUT);

        B.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());

    }

    @Test
    void level() {
        Pin out = new Pin(Pin.Direction.OUTPUT);

        out.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, out.level());

        out.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, out.level());

        Pin in = new Pin(Pin.Direction.INPUT);
        assertThrows(IllegalArgumentException.class, () -> in.level(Pin.Level.LOW));

        Pin hiz = new Pin(Pin.Direction.HI_Z);
        assertEquals(Pin.Level.LOW, hiz.level());

        hiz.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, hiz.level());

        out.connect(in);
        assertEquals(Pin.Level.HIGH, out.level());
        assertEquals(Pin.Level.HIGH, in.level());
    }

    @Test
    void pullUp() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.INPUT);
        A.connect(B);
        B.pullUp();

        A.level(Pin.Level.HIGH);
        assertEquals(A.level(), B.level());
        assertEquals(Pin.Level.HIGH, A.level());

        A.level(Pin.Level.LOW);
        assertEquals(A.level(), B.level());
        assertEquals(Pin.Level.LOW, A.level());

        A.direction(Pin.Direction.HI_Z);
        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.HIGH, B.level());
    }

    @Test
    void update() {
        AtomicBoolean changedA = new AtomicBoolean(false);
        AtomicBoolean changedB = new AtomicBoolean(false);
        AtomicBoolean changedC = new AtomicBoolean(false);

        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.INPUT);
        Pin C = new Pin(Pin.Direction.INPUT);
        A.connect(B);
        B.connect(C);

        A.onChange(() -> changedA.set(true));
        B.onChange(() -> changedB.set(true));
        C.onChange(() -> changedC.set(true));

        assertFalse(changedA.get());
        assertFalse(changedB.get());
        assertFalse(changedC.get());

        A.level(Pin.Level.HIGH);
        assertFalse(changedA.get());
        assertTrue(changedB.get());
        assertTrue(changedC.get());
    }

    @Test
    void power() {
        Pin VCC = new Pin.Power();
        Pin GND = new Pin.Ground();

        assertEquals(Pin.Level.HIGH, VCC.level());
        assertEquals(Pin.Level.LOW , GND.level());

        VCC.level(Pin.Level.LOW);
        GND.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, VCC.level());
        assertEquals(Pin.Level.LOW , GND.level());
    }

    @Test
    void multiple() {
        Pin A = new Pin("A", Pin.Direction.OUTPUT);
        Pin B = new Pin("B", Pin.Direction.INPUT);
        Pin C = new Pin("C", Pin.Direction.INPUT);

        B.connect(A);
        C.connect(B);

        assertEquals(A.wire(), B.wire());
        assertEquals(B.wire(), C.wire());
        assertEquals(C.wire(), A.wire());

        assertEquals(Pin.Level.LOW, A.level());
        assertEquals(Pin.Level.LOW, B.level());
        assertEquals(Pin.Level.LOW, C.level());

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, A.level());
        assertEquals(Pin.Level.HIGH, B.level());
        assertEquals(Pin.Level.HIGH, C.level());
    }
}