package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

// dual input multiplexer
public class DualMux {
    public final Pin inputA = new Pin("input A", Pin.Direction.INPUT);
    public final Pin inputB = new Pin("input B", Pin.Direction.INPUT);
    public final Pin output = new Pin("output", Pin.Direction.OUTPUT);
    private final Pin enable; // input pin
    private final Pin select; // input pin
    public boolean inverting;

    public DualMux(Pin enable, Pin select) {
        this(enable, select, false);
    }

    public DualMux(Pin enable, Pin select, boolean selfChange) {
        this(enable, select, selfChange, false);
    }

    public DualMux(Pin enable, Pin select, boolean selfChange, boolean inverting) {
        this.enable = enable;
        this.select = select;
        this.inverting = inverting;

        inputA.onChange(this::update);
        inputB.onChange(this::update);

        if (selfChange) {
            enable.onChange(this::update);
            select.onChange(this::update);
        }
    }

    void update() {
        if (enable.level() == Pin.Level.HIGH) {
            output.direction(Pin.Direction.HI_Z);
        } else {
            output.direction(Pin.Direction.OUTPUT);

            Pin subject = select.level() == Pin.Level.LOW ? inputA : inputB;
            output.level(inverting != subject.level().bool()); // logical XOR
        }
    }

    @Override
    public String toString() {
        return "[" + output.level() + "]";
    }
}

/*

I L O
0 0 0
0 1 1
1 0 1
1 1 0

 */