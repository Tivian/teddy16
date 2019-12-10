package eu.tivian.hardware;

import eu.tivian.other.Logger;

import java.util.Arrays;
import java.util.Random;

// pinout based on TMS4416
// TODO: CAS and RAS
public class RAM extends Memory {
    public final Pin cas    = new Pin("column address strobe", Pin.Direction.INPUT);
    public final Pin ras    = new Pin("row address strobe"   , Pin.Direction.INPUT);
    public final Pin rw     = new Pin("R/W"                  , Pin.Direction.INPUT);
    public final Pin enable = new Pin("chip select"          , Pin.Direction.INPUT);
    private final int width;

    public RAM(int inputs, int outputs, int size) {
        this("RAM", inputs, outputs, size);
    }

    public RAM(String name, int inputs, int outputs, int size) {
        this(name, inputs, outputs, size, true);
    }

    public RAM(String name, int inputs, int outputs, int size, boolean fillRandom) {
        super(
            name,
            new Bus("data"   , "D", Pin.Direction.HI_Z , outputs),
            new Bus("address", "A", Pin.Direction.INPUT, inputs ),
            size
        );

        //if (fillRandom)
        //new Random().nextBytes(content);
        Arrays.fill(content, (byte) 0xBB);

        rw.onChange(this::enable);
        enable.onChange(this::enable);
        cas.onChange(() -> {
            enable();
            update();
        });
        ras.onChange(this::update);

        width = (int) Math.pow(2, outputs) - 1;
        //address.onChange(this::update);
        //address.forEach(p -> p.onChange(lvl -> update()));
    }

    public State state() {
        return state;
    }

    private void enable() {
        data.direction((enable.level() == Pin.Level.HIGH || cas.level() == Pin.Level.HIGH) ? Pin.Direction.HI_Z
            : (rw.level() == Pin.Level.HIGH ? Pin.Direction.OUTPUT : Pin.Direction.INPUT));
    }

    private enum State {
        IDLE, ROW, COLUMN, EXECUTE, REFRESH
    }
    private State state = State.IDLE;

    int row    = 0x00;
    int column = 0x00;
    private void update() {
        boolean ras = this.ras.level().bool();
        boolean cas = this.cas.level().bool();
        boolean read = this.rw.level().bool();

        if (state == State.IDLE && !ras) {
            row = (int) address.value();
            state = State.ROW;
        } else if (state == State.ROW && !cas) {
            column = (int) (address.value() & 0b01111110) >> 1;
            state = State.COLUMN;

            int index = (column << 8) | row;
            if (read) {
                if (Logger.ENABLE)
                    Logger.info(String.format("Output: 0x%02X from %s at 0x%04X", content[index] & width, name, index));
                data.value(content[index] & width);
            } else {
                if (Logger.ENABLE)
                    Logger.info(String.format("Input: 0x%02X to %s at 0x%04X", data.value() & width, name, index));
                content[index] = (byte) (data.value() & width);
            }
            state = State.IDLE;
        /*} else if (state == State.COLUMN && (cas || ras)) {
            //int index = row * 64 + column;//(row * data.size() + column);
            int index = (column << 8) | row;
            if (read)
                data.value(content[index] & width);
            else
                content[index] = (byte) (data.value() & width);
            state = State.IDLE;*/
        } else {
            state = State.IDLE;
        }

        /*if (state == State.ROW && !ras) {
            row = (int) address.value();
            state = State.COLUMN;
        } else if (state == State.COLUMN && !cas) {
            column = (int) (address.value() & 0b01111110) >> 1;
            state = State.EXECUTE;
        } else if (state == State.COLUMN && ras) {
            state = State.ROW; // dram refresh
        } else if (state == State.EXECUTE && (ras || cas)) {
            int index = (row * data.size() + column) & data.size();
            if (read)
                data.value(content[index] & width);
            else
                content[index] = (byte) (data.value() & width);
            state = State.ROW;
        } else {
            state = State.ROW;
        }*/
    }
}

/*
read
IDLE -> row -> RAS[0] -> col -> CAS[0] -> W[1] -> output -> CAS[1] -> RAS[1] -> IDLE

write
IDLE -> row -> RAS[0] -> col -> CAS[0] -> W[0] -> input -> CAS[1] -> RAS[1] -> IDLE

refresh
IDLE -> row -> RAS[0] -> CAS[1] -> RAS[1] -> IDLE

------------------------------------------------------------------------------------------------------------------------

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


0 - 16383
row: 0 - 255
col: 0 - 63
256 * 64 = 16384
255 * 63 = 16065‬
0 * 0 = 0

row * width + col
255 * 64 + 63

 */
