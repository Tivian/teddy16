package eu.tivian.hardware;

// FPGATED clocks
//  28.63636 MHz for NTSC
//  28.28800 MHz for PAL

import java.util.Random;

// MOS 8360
// based on "TED 7360R0 Preliminary Data Sheet" and FPGATED project
//      https://www.pagetable.com/docs/ted/TED%207360R0%20Preliminary%20Data%20Sheet.pdf
//      https://github.com/ishe/plus4/blob/master/ted.v
public class TED implements AutoCloseable {
    public interface Video {
        void accept(int x, int y, int color);
    }

    public final Bus address  = new Bus("address" , "A" , Pin.Direction.INPUT, 16); // pins 1-3, 36-48
    public final Bus keyboard = new Bus("keyboard", "K" , Pin.Direction.INPUT,  8); // pins 15-22
    public final Bus data     = new Bus("data"    , "DB", Pin.Direction.INPUT,  8); // pins 25-32

    public final Pin cs0    = new Pin("low ROM cs"           , Pin.Direction.OUTPUT); // pin  5
    public final Pin cs1    = new Pin("high ROM cs"          , Pin.Direction.OUTPUT); // pin  6
    public final Pin rw     = new Pin("read/write"           , Pin.Direction.INPUT ); // pin  7
    public final Pin irq    = new Pin("interrupt"            , Pin.Direction.OUTPUT); // pin  8
    public final Pin mux    = new Pin("address mux"          , Pin.Direction.OUTPUT); // pin  9
    public final Pin ras    = new Pin("row address strobe"   , Pin.Direction.OUTPUT); // pin 10
    public final Pin cas    = new Pin("column address strobe", Pin.Direction.OUTPUT); // pin 11
    public final Pin phiOut = new Pin("clock output"         , Pin.Direction.OUTPUT); // pin 12
    public final Pin phiIn  = new Pin("clock input"          , Pin.Direction.INPUT ); // pin 14
    public final Pin ba     = new Pin("bus available"        , Pin.Direction.OUTPUT); // pin 34
    public final Pin aec    = new Pin("tri-state control"    , Pin.Direction.OUTPUT); // pin 35

    private Video render;
    private int x = 0;
    private int y = 0;
    private int color = 0;

    private TEDNative internal;

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

    public void render(Video fx) {
        this.render = fx;
    }

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
        if (address.direction() == Pin.Direction.OUTPUT)
            address.value(internal.addr_out());

        if (internal.tedreg() == 1 && data.direction() == Pin.Direction.OUTPUT)
            data.value(internal.data_out());

        phiOut.level(internal.cpuclk() != 0);
    }

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
