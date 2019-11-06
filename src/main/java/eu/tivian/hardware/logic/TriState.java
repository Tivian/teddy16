package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

class TriState {
    public final Pin input = new Pin("tri-state input", Pin.Direction.INPUT);
    public final Pin output = new Pin("tri-state output", Pin.Direction.OUTPUT);
    public final Pin enable = new Pin("tri-state enable", Pin.Direction.INPUT);

    public final boolean inverting;

    public TriState() {
        this(false);
    }

    public TriState(boolean inverting) {
        this.inverting = inverting;
        this.input.onChange(this::update);
        this.enable.onChange(this::update);
    }

    private void update(Pin.Level level) {
        if (enable.level() == Pin.Level.HIGH) {
            output.direction(Pin.Direction.HI_Z);
        } else {
            output.direction(Pin.Direction.OUTPUT);
            output.level(input.level());
        }
    }
}
