package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

class QuadDemux {
    public final Pin enable = new Pin("enable", Pin.Direction.INPUT);
    public final Pin A0 = new Pin("A0", Pin.Direction.INPUT);
    public final Pin A1 = new Pin("A1", Pin.Direction.INPUT);
    public final Pin O0 = new Pin("O0", Pin.Direction.OUTPUT);
    public final Pin O1 = new Pin("O1", Pin.Direction.OUTPUT);
    public final Pin O2 = new Pin("O2", Pin.Direction.OUTPUT);
    public final Pin O3 = new Pin("O3", Pin.Direction.OUTPUT);

    public QuadDemux() {
        enable.onChange(this::update);
        A0.onChange(this::update);
        A1.onChange(this::update);
    }

    private void update(Pin.Level level) {
        O0.level(!(enable.level() == Pin.Level.LOW && enable.level() == A0.level() && enable.level() == A1.level()));
        O1.level(!(enable.level() == Pin.Level.LOW && enable.level() != A0.level() && enable.level() == A1.level()));
        O2.level(!(enable.level() == Pin.Level.LOW && enable.level() == A0.level() && enable.level() != A1.level()));
        O3.level(!(enable.level() == Pin.Level.LOW && enable.level() != A0.level() && enable.level() != A1.level()));
    }
}
