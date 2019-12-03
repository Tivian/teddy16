package eu.tivian.hardware.logic;

import eu.tivian.hardware.Bus;
import eu.tivian.hardware.Pin;

// MOS6529 - SPI (Single Port Interface)
public class MOS6529 {
    public final Bus data = new Bus("data", "D", Pin.Direction.HI_Z, 8);
    public final Bus port = new Bus("port", "P", Pin.Direction.HI_Z, 8);
    public final Pin cs = new Pin("chip select", Pin.Direction.INPUT);
    public final Pin rw = new Pin("read/write", Pin.Direction.INPUT);

    public MOS6529() {
        cs.onChange(this::enable);
        rw.onChange(this::enable);
        data.onChange(this::update);
        port.onChange(this::update);

        enable();
    }

    private void enable() {
        if (cs.level() == Pin.Level.HIGH) {
            data.direction(Pin.Direction.HI_Z);
            port.direction(Pin.Direction.HI_Z);
        } else if (rw.level() == Pin.Level.HIGH) {
            data.direction(Pin.Direction.INPUT);
            port.direction(Pin.Direction.OUTPUT);
        } else {
            data.direction(Pin.Direction.OUTPUT);
            port.direction(Pin.Direction.INPUT);
        }

        update();
    }

    private void update() {
        if (data.direction() == Pin.Direction.INPUT)
            port.value(data.value());
        else if (port.direction() == Pin.Direction.INPUT)
            data.value(port.value());
    }
}
