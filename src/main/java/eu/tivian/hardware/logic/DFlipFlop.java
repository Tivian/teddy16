package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

/**
 * Single edge-triggered D flip-flop.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see IC74LS175
 */
public class DFlipFlop {
    /**
     * Clock signal triggered by LOW to HIGH transition.
     */
    public final Pin clock;
    /**
     * Reset signal which restores output to LOW level and reverse output to HIGH level.
     */
    public final Pin reset;
    /**
     * Single input signal.
     */
    public final Pin input = new Pin("D flip-flop input", Pin.Direction.INPUT);
    /**
     * Output signal.
     */
    public final Pin output = new Pin("D flip-flop output", Pin.Direction.OUTPUT);
    /**
     * Reverse output signal.
     */
    public final Pin revOut = new Pin("D flip-flop reverse output", Pin.Direction.OUTPUT);

    /**
     * Sets outputs and control pins.
     *
     * @param clock used to chain multiple flip-flops
     * @param reset used to specify master reset pin
     * @throws NullPointerException if the {@code clock} or {@code reset} arguments are not specified
     */
    public DFlipFlop(Pin clock, Pin reset) {
        if (clock == null)
            throw new NullPointerException("Clock pin must be specified!");
        else if (reset == null)
            throw new NullPointerException("Reset pin must be specified!");

        this.clock = clock;
        this.reset = reset;

        output.level(Pin.Level.LOW);
        revOut.level(Pin.Level.HIGH);
    }

    /**
     * Resets the flip-flop if {@link #reset} pin is held LOW.
     */
    void reset() {
        if (reset.level() == Pin.Level.LOW) {
            output.level(Pin.Level.LOW);
            revOut.level(Pin.Level.HIGH);
        }
    }

    /**
     * Changes {@link #output} pin according to {@link #input} pin if the {@link #reset} pin is held HIGH
     * and {@link #clock} has transitioned from LOW to HIGH level.
     */
    void update() {
        if (reset.level() == Pin.Level.HIGH && clock.level() == Pin.Level.HIGH) {
            output.level(input.level());
            revOut.level(!input.level().bool());
        }
    }
}
