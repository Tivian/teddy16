package eu.tivian.hardware;

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
}
