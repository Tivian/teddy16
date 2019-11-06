package eu.tivian.hardware;

import java.util.Random;

// pinout based on TMS4416
// TODO: CAS and RAS
public class RAM extends Memory {
    public final Pin cas = new Pin("column address strobe", Pin.Direction.INPUT);
    public final Pin ras = new Pin("row address strobe", Pin.Direction.INPUT);
    public final Pin rw = new Pin("R/W", Pin.Direction.INPUT);
    public final Pin enable = new Pin("chip select", Pin.Direction.INPUT);

    public RAM(int inputs, int outputs, int size) {
        this(inputs, outputs, size, true);
    }

    public RAM(int inputs, int outputs, int size, boolean fillRandom) {
        super(
            new Bus("data"   , "D", Pin.Direction.HI_Z , outputs),
            new Bus("address", "A", Pin.Direction.INPUT, inputs ),
            size
        );

        if (fillRandom)
            new Random().nextBytes(content);

        rw.onChange(this::enable);
        enable.onChange(this::enable);
        address.onChange(this::update);
    }

    private void enable(Pin.Level level) {
        data.direction((enable.level() == Pin.Level.HIGH) ? Pin.Direction.HI_Z
            : (rw.level() == Pin.Level.HIGH ? Pin.Direction.INPUT
                : Pin.Direction.OUTPUT));
    }

    private void update(long value) {
        if (data.direction() == Pin.Direction.OUTPUT)
            data.value(content[(int) value]);
        else if (data.direction() == Pin.Direction.INPUT)
            content[(int) value] = (byte) data.value();
    }
}
