package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

/**
 * Single 2-line to 4-line demultiplexer.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see IC74LS139
 */
public class QuadDemux {
    /**
     * When HIGH all output pins are held HIGH.
     * Equivalent of chip select pin.
     */
    public final Pin enable = new Pin("enable", Pin.Direction.INPUT);
    /**
     * First input signal.
     */
    public final Pin A0 = new Pin("A0", Pin.Direction.INPUT);
    /**
     * Second input signal.
     */
    public final Pin A1 = new Pin("A1", Pin.Direction.INPUT);
    /**
     * First output signal.
     * <br>LOW only when {@link #A0} and {@link #A1} are LOW.
     */
    public final Pin O0 = new Pin("O0", Pin.Direction.OUTPUT);
    /**
     * Second output signal.
     * <br>LOW only when {@link #A0} is LOW and {@link #A1} is HIGH.
     */
    public final Pin O1 = new Pin("O1", Pin.Direction.OUTPUT);
    /**
     * Third output signal.
     * <br>LOW only when {@link #A0} is HIGH and {@link #A1} is LOW.
     */
    public final Pin O2 = new Pin("O2", Pin.Direction.OUTPUT);
    /**
     * Forth output signal.
     * <br>LOW only when {@link #A0} and {@link #A1} are HIGH.
     */
    public final Pin O3 = new Pin("O3", Pin.Direction.OUTPUT);

    /**
     * Initializes the demultiplexer logic and sets initial state of outputs.
     */
    public QuadDemux() {
        enable.onChange(this::update);
        A0.onChange(this::update);
        A1.onChange(this::update);
        update();
    }

    /**
     * Changes output pins according state of the {@link #enable} pin and input pins.
     *
     * @see #O0
     * @see #O1
     * @see #O2
     * @see #O3
     */
    private void update() {
        O0.level(enable.level() != Pin.Level.LOW || enable.level() != A0.level() || enable.level() != A1.level());
        O1.level(enable.level() != Pin.Level.LOW || enable.level() == A0.level() || enable.level() != A1.level());
        O2.level(enable.level() != Pin.Level.LOW || enable.level() != A0.level() || enable.level() == A1.level());
        O3.level(enable.level() != Pin.Level.LOW || enable.level() == A0.level() || enable.level() == A1.level());
    }
}
