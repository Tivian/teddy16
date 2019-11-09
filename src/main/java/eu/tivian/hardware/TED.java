package eu.tivian.hardware;

// FPGATED clocks
//  28.63636 MHz for NTSC
//  28.28800 MHz for PAL

// MOS 8360
// based on "TED 7360R0 Preliminary Data Sheet" and FPGATED project
//      https://www.pagetable.com/docs/ted/TED%207360R0%20Preliminary%20Data%20Sheet.pdf
//      https://github.com/ishe/plus4/blob/master/ted.v
public class TED {
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

    // VCC pin 4
    //public final Pin chroma; // pin 13
    //public final Pin luma; // pin 23
    // GND pin 24
    //public final Pin sound; // pin 33

    public TED() {
        phiIn.onChange(lvl -> step());
    }

    public void step() {

    }
}

/*
 1: ras<=1;
    cas<=1;
    mux<=1;
 6: ras<=0;			// RAS goes low 35ns before MUX (20ns on real system)
 7: mux<=0;			// MUX goes low when double phi changes to high at half double clock cycle, CS0,CS1 changes together with MUX when needed
 8: if (rw & cs0 & cs1 & ~io & ~tedreg)		// when read cycle, CAS goes low 35ns after MUX (40ns on real system)
     cas<=0;
11:	if (~rw & ~io & ~tedreg)					// when write cycle, CAS goes low 160ns after MUX
     cas<=0;

RAS: 1[1] -> 0[6]
CAS: 1[1] -> 0[8 (read) / 11 (write)]
MAX: 1[1] -> 0[7]
 */
