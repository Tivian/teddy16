package eu.tivian.hardware.logic;

import eu.tivian.hardware.Bus;
import eu.tivian.hardware.Pin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MOS6529Test {
    @Test
    void logic() {
        int[] testValues = new int[] { 0x00, 0x01, 0x10, 0x55, 0xAA, 0xF0, 0x0F, 0xFF };

        MOS6529 spi = new MOS6529();
        Bus data = new Bus("data", "D", Pin.Direction.HI_Z, 8);
        Bus port = new Bus("port", "P", Pin.Direction.HI_Z, 8);
        Pin rw = new Pin("R/-W", Pin.Direction.OUTPUT);
        Pin cs = new Pin("chip select", Pin.Direction.OUTPUT);

        spi.data.connect(data);
        spi.port.connect(port);
        spi.rw.connect(rw);
        spi.cs.connect(cs);

        cs.level(Pin.Level.LOW);
        rw.level(Pin.Level.LOW);
        assertEquals(Pin.Direction.OUTPUT, spi.data.direction());
        assertEquals(Pin.Direction.INPUT , spi.port.direction());
        data.direction(Pin.Direction.INPUT);
        port.direction(Pin.Direction.OUTPUT);

        for (int val : testValues) {
            port.value(val);
            assertEquals(val, spi.data.value());
        }

        rw.level(Pin.Level.HIGH);
        assertEquals(Pin.Direction.INPUT, spi.data.direction());
        assertEquals(Pin.Direction.OUTPUT , spi.port.direction());
        data.direction(Pin.Direction.OUTPUT);
        port.direction(Pin.Direction.INPUT);

        for (int val : testValues) {
            data.value(val);
            assertEquals(val, spi.port.value());
        }

        cs.level(Pin.Level.HIGH);
        assertEquals(Pin.Direction.HI_Z, spi.data.direction());
        assertEquals(Pin.Direction.HI_Z, spi.port.direction());
    }
}