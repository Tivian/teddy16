package eu.tivian.hardware;

// FPGATED clocks
//  28.63636 MHz for NTSC
//  28.28800 MHz for PAL

import eu.tivian.other.Logger;

/**
 * TED - video chip.
 * <br><b>MOS 8360</b>
 * <br>Right now uses the Verilog version of TED from <a href="https://github.com/ishe/plus4">FPGATED project</a>.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see TED
 * @see <a href="https://github.com/ishe/plus4">FPGATED Project</a>
 * @see <a href="https://www.pagetable.com/docs/ted/TED%207360R0%20Preliminary%20Data%20Sheet.pdf">TED datasheet</a>
 */
public class TED implements AutoCloseable {
    /**
     * Rendering functional interface.
     */
    public interface Video {
        /**
         * Sets the pixel at x-th column and y-th row to given color of the palette.
         *
         * @param x column
         * @param y row
         * @param color color index
         */
        void accept(int x, int y, int color);
    }

    /**
     * Address bus.
     */
    public final Bus address  = new Bus("address" , "A" , Pin.Direction.INPUT, 16); // pins 1-3, 36-48
    /**
     * Keyboard matrix port.
     */
    public final Bus keyboard = new Bus("keyboard", "K" , Pin.Direction.INPUT,  8); // pins 15-22
    /**
     * Data bus.
     */
    public final Bus data     = new Bus("data"    , "DB", Pin.Direction.INPUT,  8); // pins 25-32

    /**
     * Low ROM chip select.
     */
    public final Pin cs0    = new Pin("low ROM cs"           , Pin.Direction.OUTPUT); // pin  5
    /**
     * High ROM chip select.
     */
    public final Pin cs1    = new Pin("high ROM cs"          , Pin.Direction.OUTPUT); // pin  6
    /**
     * Read/write pin.
     */
    public final Pin rw     = new Pin("read/write"           , Pin.Direction.INPUT ); // pin  7
    /**
     * Interrupt signal.
     */
    public final Pin irq    = new Pin("interrupt"            , Pin.Direction.OUTPUT); // pin  8
    /**
     * RAM multiplexers signal.
     */
    public final Pin mux    = new Pin("address mux"          , Pin.Direction.OUTPUT); // pin  9
    /**
     * Row address strobe.
     */
    public final Pin ras    = new Pin("row address strobe"   , Pin.Direction.OUTPUT); // pin 10
    /**
     * Column address strobe.
     */
    public final Pin cas    = new Pin("column address strobe", Pin.Direction.OUTPUT); // pin 11
    /**
     * Clock output.
     */
    public final Pin phiOut = new Pin("clock output"         , Pin.Direction.OUTPUT); // pin 12
    /**
     * Clock input.
     */
    public final Pin phiIn  = new Pin("clock input"          , Pin.Direction.INPUT ); // pin 14
    /**
     * Bus available signal.
     * <br>Stops the CPU after 3 cycles if held LOW.
     */
    public final Pin ba     = new Pin("bus available"        , Pin.Direction.OUTPUT); // pin 34
    /**
     * Address enable control.
     * <br>Makes the address bus of the CPU go into HI-Z state if held LOW.
     */
    public final Pin aec    = new Pin("tri-state control"    , Pin.Direction.OUTPUT); // pin 35

    /**
     * Video renderer.
     */
    private Video render;
    /**
     * Current video column.
     */
    private int x = 0;
    /**
     * Current video row.
     */
    private int y = 0;
    /**
     * Currently drawn color.
     */
    private int color = 0;

    /**
     * Verilog version of TED
     */
    private TEDNative internal;

    /**
     * Initializes inner logic of the video chip.
     */
    public TED() {
        render = null;
        internal = new TEDNative();
        internal.init();

        phiIn.onChange(this::step);
        rw.onChange(this::enable);
        rw.pullUp(); // R/-W is pulled-up internally

        keyboard.onChange(() -> internal.keyboard((int) keyboard.value()));

        address.onChange(() -> {
            if (address.direction() == Pin.Direction.INPUT)
                internal.addr_in((int) address.value());
        });

        data.onChange(() -> {
            if (data.direction() == Pin.Direction.INPUT)
                internal.data_in((int) data.value());
        });
    }

    /**
     * Sets the renderer.
     *
     * @param fx renderer
     * @throws NullPointerException if renderer is {@code null}
     */
    public void render(Video fx) {
        if (fx == null)
            throw new NullPointerException("Renderer cannot be null!");

        this.render = fx;
    }

    /**
     * Changes the direction of {@link #data} bus according to the {@link #rw} level.
     */
    private void enable() {
        if (rw.level() == Pin.Level.HIGH) {
            internal.rw(1);
            data.direction(internal.tedreg() == 1 ? Pin.Direction.OUTPUT : Pin.Direction.HI_Z);
            //data.direction(Pin.Direction.HI_Z);
        } else {
            internal.rw(0);
            data.direction(Pin.Direction.INPUT);
        }
    }

    //private int maxX = 0, maxY = 0;
    /**
     * Calculates current state of the video chip.
     */
    private void step() {
        internal.step();
        internal.clk(phiIn.level() == Pin.Level.HIGH ? 1 : 0);

        cs0.level(internal.cs0() != 0);
        cs1.level(internal.cs1() != 0);
        irq.level(internal.irq() != 0);
        mux.level(internal.mux() != 0);
        ras.level(internal.ras() != 0);
        cas.level(internal.cas() != 0);
        ba.level(internal.ba() != 0);
        aec.level(internal.aec() != 0);

        if (render != null) {
            int x = internal.vcount(), y = internal.hcount(), color = internal.color();
            if (this.x != x || this.y != y || this.color != color) {
                //render.accept(y, x, new Random().nextInt() & 0x7F);
                render.accept(y, x, color);

                //maxX = Math.max(maxX, x);
                //maxY = Math.max(maxY, y);

                this.x = x;
                this.y = y;
                this.color = color;
            }
        }

        address.direction(internal.ba() == 0 ? Pin.Direction.OUTPUT : Pin.Direction.INPUT);
        if (address.direction() == Pin.Direction.OUTPUT) {
            if (Logger.ENABLE)
                Logger.info(String.format("TED wants to read memory at 0x%04X", internal.addr_out()));

            address.value(internal.addr_out());
        }

        if (internal.tedreg() == 1 && data.direction() == Pin.Direction.OUTPUT) {
            if (Logger.ENABLE)
                Logger.info(String.format("TED wants to write memory at 0x%04X", internal.data_out()));

            data.value(internal.data_out());
        }

        phiOut.level(internal.cpuclk() != 0);
    }

    /**
     * Clears up the memory after TED chip is not needed.
     */
    @Override
    public void close() {
        internal.free();
    }

    // VCC pin 4
    //public final Pin chroma; // pin 13
    //public final Pin luma; // pin 23
    // GND pin 24
    //public final Pin sound; // pin 33
}
