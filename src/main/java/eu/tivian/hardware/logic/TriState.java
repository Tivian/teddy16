package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

/**
 * Tri-state gate.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see IC74LS125
 */
class TriState {
    /**
     * Input signal.
     */
    public final Pin input = new Pin("tri-state input", Pin.Direction.INPUT);
    /**
     * Output signal which according to the {@link #enable}
     * is either equal to level of {@link #input} or HI-Z.
     */
    public final Pin output = new Pin("tri-state output", Pin.Direction.OUTPUT);
    /**
     * Signal controlling the state of the output pin.
     */
    public final Pin enable = new Pin("tri-state enable", Pin.Direction.INPUT);

    /**
     * Some tri-state gates takes inverted level of {@link #enable} pin to drive the gate.
     */
    public final boolean inverting;

    /**
     * Initializes non-inverting tri-state gate.
     */
    public TriState() {
        this(false);
    }

    /**
     * Initializes tri-state gate.
     * @param inverting choose if state of the {@link #enable} pin should be inverted
     */
    public TriState(boolean inverting) {
        this.inverting = inverting;
        this.input.onChange(this::update);
        this.enable.onChange(this::update);
        update();
    }

    /**
     * Updates the {@link #output} according to the state of the {@link #enable} pin.
     */
    private void update() {
        if (enable.level() == Pin.Level.HIGH) {
            output.direction(Pin.Direction.HI_Z);
        } else {
            output.direction(Pin.Direction.OUTPUT);
            output.level(input.level());
        }
    }
}
