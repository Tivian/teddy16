package eu.tivian.hardware;

import java.util.Random;

// pinout based on TMS4416
// TODO: CAS and RAS
public class RAM extends Memory {
    public final Pin cas    = new Pin("column address strobe", Pin.Direction.INPUT);
    public final Pin ras    = new Pin("row address strobe"   , Pin.Direction.INPUT);
    public final Pin rw     = new Pin("R/W"                  , Pin.Direction.INPUT);
    public final Pin enable = new Pin("chip select"          , Pin.Direction.INPUT);

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
            : (rw.level() == Pin.Level.HIGH ? Pin.Direction.OUTPUT
                : Pin.Direction.INPUT));
    }

    private void update(long value) {
        /*if (data.direction() == Pin.Direction.OUTPUT)
            data.value(content[(int) value]);
        else if (data.direction() == Pin.Direction.INPUT)
            content[(int) value] = (byte) data.value();*/
    }
}

/*
read
RAS[0], row -> CAS[0], col, W[1] -> RAS[1], COL[1] -> output

write
RAS[0], row -> CAS[0], col, W[0] -> RAS[1], COL[1] -> input

read-modify-write
RAS[0], row -> CAS[0], col, W[1] -> output -> W[0] -> input -> RAS[1], COL[1]

page-mode read
RAS[0], row -> |CAS[0], col, W[1] -> CAS[1] -> output| -> |CAS[0], col, W[1] -> CAS[1] -> output| -> ... -> RAS[1], COL[1]

page-mode write
RAS[0], row -> |CAS[0], col, W[0], input -> CAS[1]| -> |CAS[0], col, W[0], input -> CAS[1]| -> ... -> RAS[1], COL[1]

page-mode read-modify-write
RAS[0], row -> |CAS[0], col, W[1] -> output -> W[0], input -> CAS[1]| -> CAS[0], col ... -> RAS[1], COL[1]

RAS-only refresh
row -> RAS[0], CAS[1] -> RAS[1]




 */
