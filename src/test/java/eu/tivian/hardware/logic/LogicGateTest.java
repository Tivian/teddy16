package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Logic gates unit tests.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see LogicGate
 */
class LogicGateTest {
    /**
     * Tests single <b>NOT</b> logic gate.
     */
    @Test
    void not() {
        Pin input = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.NOT);
        assertEquals(Pin.Level.HIGH, gate.output.level());

        input.connect(gate.inputA).connect(gate.inputB);
        input.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~0 = 1

        input.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~1 = 0
    }

    /**
     * Tests single <b>NAND</b> logic gate.
     */
    @Test
    void nand() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.NAND);
        assertEquals(Pin.Level.HIGH, gate.output.level());

        A.connect(gate.inputA);
        B.connect(gate.inputB);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~(0 & 0) = 1

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~(1 & 0) = 1

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~(1 & 1) = 0

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~(0 & 1) = 1
    }

    /**
     * Tests single <b>NOR</b> logic gate.
     */
    @Test
    void nor() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.NOR);
        assertEquals(Pin.Level.HIGH, gate.output.level());

        A.connect(gate.inputA);
        B.connect(gate.inputB);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~(0 | 0) = 1

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~(1 | 0) = 0

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~(1 | 1) = 0

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~(0 | 1) = 0
    }

    /**
     * Tests single <b>AND</b> logic gate.
     */
    @Test
    void and() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.AND);
        assertEquals(Pin.Level.LOW, gate.output.level());

        A.connect(gate.inputA);
        B.connect(gate.inputB);
        assertEquals(Pin.Level.LOW, gate.output.level()); // 0 & 0 = 0

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // 1 & 0 = 0

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // 1 & 1 = 1

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, gate.output.level()); // 0 & 1 = 0
    }

    /**
     * Tests single <b>OR</b> logic gate.
     */
    @Test
    void or() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.OR);
        assertEquals(Pin.Level.LOW, gate.output.level());

        A.connect(gate.inputA);
        B.connect(gate.inputB);
        assertEquals(Pin.Level.LOW, gate.output.level()); // 0 | 0 = 0

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // 1 | 0 = 1

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // 1 | 1 = 1

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // 0 | 1 = 1
    }

    /**
     * Tests single <b>XOR</b> logic gate.
     */
    @Test
    void xor() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.XOR);
        assertEquals(Pin.Level.LOW, gate.output.level());

        A.connect(gate.inputA);
        B.connect(gate.inputB);
        assertEquals(Pin.Level.LOW, gate.output.level()); // 0 ^ 0 = 0

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // 1 ^ 0 = 1

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // 1 ^ 1 = 0

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // 0 ^ 1 = 1
    }

    /**
     * Tests single <b>XNOR</b> logic gate.
     */
    @Test
    void xnor() {
        Pin A = new Pin(Pin.Direction.OUTPUT);
        Pin B = new Pin(Pin.Direction.OUTPUT);
        LogicGate gate = new LogicGate(LogicGate.Type.XNOR);
        assertEquals(Pin.Level.HIGH, gate.output.level());

        A.connect(gate.inputA);
        B.connect(gate.inputB);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~(0 ^ 0) = 1

        A.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~(1 ^ 0) = 0

        B.level(Pin.Level.HIGH);
        assertEquals(Pin.Level.HIGH, gate.output.level()); // ~(1 ^ 1) = 1

        A.level(Pin.Level.LOW);
        assertEquals(Pin.Level.LOW, gate.output.level()); // ~(0 ^ 1) = 0
    }
}