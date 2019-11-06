package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

public class DFlipFlop {
    public final Pin clock;
    public final Pin reset;
    public final Pin input = new Pin("D flip-flop input", Pin.Direction.INPUT);
    public final Pin output = new Pin("D flip-flop output", Pin.Direction.OUTPUT);
    public final Pin revOut = new Pin("D flip-flop reverse output", Pin.Direction.OUTPUT);

    public DFlipFlop(Pin clock, Pin reset) {
        this.clock = clock;
        this.reset = reset;
    }

    void reset() {
        if (reset.level() == Pin.Level.LOW) {
            output.level(Pin.Level.LOW);
            revOut.level(Pin.Level.HIGH);
        }
    }

    void update() {
        if (reset.level() == Pin.Level.HIGH && clock.level() == Pin.Level.HIGH) {
            output.level(input.level());
            revOut.level(!input.level().bool());
        }
    }
}
