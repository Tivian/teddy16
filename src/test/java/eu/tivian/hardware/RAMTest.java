package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RAMTest {
    @Test
    void read() {
        TED ted = new TED();
        Pin clock = new Pin(Pin.Direction.OUTPUT);
        Pin rw = new Pin(Pin.Direction.OUTPUT);
        RAM ramA = new RAM(8, 4, 65536);
        RAM ramB = new RAM(8, 4, 65536);
        Bus address = new Bus("addr", "A", Pin.Direction.OUTPUT, 16);
        Bus data    = new Bus("data", "D", Pin.Direction.OUTPUT, 8);

        ted.phiIn.connect(clock);
        ted.rw.connect(rw).connect(ramA.rw).connect(ramB.rw);
        ted.address.connect(address);
        ted.data.connect(data);
        ted.ras.connect(ramA.ras).connect(ramB.ras);
        ted.cas.connect(ramA.cas).connect(ramB.cas);
        ramA.enable.connect(Pin.GND);
        ramB.enable.connect(Pin.GND);
        data.connect(ramA.data, i -> i > 3 ? -1 : i)
            .connect(ramB.data, i -> i < 4 ? -1 : i - 4);
        address.connect(ramA.address, i -> i > 7 ? -1 : i)
               .connect(ramB.address, i -> i < 8 ? -1 : i - 8);
    }

    @Test
    void write() {

    }

    @Test
    void refresh() {

    }
}