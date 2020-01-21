package eu.tivian.hardware;

import eu.tivian.other.Logger;

import java.util.Arrays;
import java.util.Random;

/**
 * RAM chip, based on TMS4416 used in C16.
 * <br>Only basic RAS and CAS functionality is available.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see Memory
 * @see <a href="https://www.digchip.com/datasheets/download_datasheet.php?id=3180581&part-number=TMS4416">
 *     TMS4416 datasheet</a>
 */
public class RAM extends Memory {
    /**
     * Column address strobe.
     */
    public final Pin cas    = new Pin("column address strobe", Pin.Direction.INPUT);
    /**
     * Row address strobe.
     */
    public final Pin ras    = new Pin("row address strobe"   , Pin.Direction.INPUT);
    /**
     * Read/write pin.
     */
    public final Pin rw     = new Pin("R/W"                  , Pin.Direction.INPUT);
    /**
     * Chip select pin.
     */
    public final Pin enable = new Pin("chip select"          , Pin.Direction.INPUT);
    /**
     * Width of the word stored in the RAM chip.
     */
    private final int width;

    /**
     * Initializes memory with default name and fills memory with random bytes.
     *
     * @param inputs number of input pins
     * @param outputs number of output pins
     * @param size size of the word stored in the memory
     */
    public RAM(int inputs, int outputs, int size) {
        this("RAM", inputs, outputs, size);
    }

    /**
     * Initializes memory with given name and fills memory with random bytes.
     *
     * @param name name of the chip
     * @param inputs number of input pins
     * @param outputs number of output pins
     * @param size size of the word stored in the memory
     */
    public RAM(String name, int inputs, int outputs, int size) {
        this(name, inputs, outputs, size, true);
    }

    /**
     * Initializes memory with given parameters.
     *
     * @param name name of the chip
     * @param inputs number of input pins
     * @param outputs number of output pins
     * @param size size of the word stored in the memory
     * @param fillRandom if {@code true} then memory will with random bytes
     */
    public RAM(String name, int inputs, int outputs, int size, boolean fillRandom) {
        super(
            name,
            new Bus("data"   , "D", Pin.Direction.HI_Z , outputs),
            new Bus("address", "A", Pin.Direction.INPUT, inputs ),
            size
        );

        //if (fillRandom)
            //new Random().nextBytes(content);
        //else
            Arrays.fill(content, (byte) 0xBB);

        rw.onChange(this::enable);
        enable.onChange(this::enable);
        cas.onChange(() -> {
            enable();
            update();
        });
        ras.onChange(this::update);

        width = (int) Math.pow(2, outputs) - 1;
    }

    /**
     * Returns internal state of the memory chip.
     * @return internal state of the memory chip.
     */
    public State state() {
        return state;
    }

    /**
     * Changes the direction of the data bus according to the level at {@link #enable} and {@link #rw}.
     */
    private void enable() {
        data.direction((enable.level() == Pin.Level.HIGH || cas.level() == Pin.Level.HIGH) ? Pin.Direction.HI_Z
            : (rw.level() == Pin.Level.HIGH ? Pin.Direction.OUTPUT : Pin.Direction.INPUT));
    }

    /**
     * Internal state of the RAM chip.
     */
    private enum State {
        /**
         * Chip is waiting for level change at the input pins.
         */
        IDLE,
        /**
         * Memory row has been latched.
         */
        ROW,
        /**
         * Memory column has been latched.
         * The read from or write to the memory will take place there.
         */
        COLUMN
    }

    /**
     * Current internal state of the chip.
     */
    private State state = State.IDLE;

    /**
     * Latched memory row.
     */
    int row    = 0x00;
    /**
     * Latched memory column.
     */
    int column = 0x00;

    /**
     * Updates the state of the memory according to the {@link #ras}, {@link #cas} and {@link #rw} pins.
     */
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
        } else {
            state = State.IDLE;
        }
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
