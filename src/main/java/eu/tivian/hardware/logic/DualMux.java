package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

/**
 * Single dual input multiplexer.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see IC74LS257
 */
public class DualMux {
    /**
     * Chosen when {@link #enable} and {@link #select} pins are held LOW.
     */
    public final Pin inputA = new Pin("input A", Pin.Direction.INPUT);
    /**
     * Chosen when {@link #enable} pin is held LOW and {@link #select} pin is at HIGH level.
     */
    public final Pin inputB = new Pin("input B", Pin.Direction.INPUT);
    /**
     * Outputs either signal from first or second input only if {@link #enable} pin is held LOW.
     * Otherwise this pin is in HI-Z state.
     */
    public final Pin output = new Pin("output", Pin.Direction.OUTPUT);
    /**
     * Controls if output is in HI-Z state or not.
     */
    private final Pin enable;
    /**
     * Selects which input should be present at the output.
     */
    private final Pin select;
    /**
     * Some multiplexers inverts input signal at the output.
     */
    public boolean inverting;

    /**
     * Initializes multiplexer suited for applications where there are multiple multiplexers.
     *
     * @param enable specify output enabling pin
     * @param select specify selecting pin
     * @throws NullPointerException if {@code enable} or {@code select} are null
     */
    public DualMux(Pin enable, Pin select) {
        this(enable, select, false);
    }

    /**
     * Initializes non-inverting multiplexer.
     *
     * @param enable specify output enabling pin
     * @param select specify selecting pin
     * @param selfChange pass {@code true} if in single multiplexer configuration
     * @throws NullPointerException if {@code enable} or {@code select} are null
     */
    public DualMux(Pin enable, Pin select, boolean selfChange) {
        this(enable, select, selfChange, false);
    }

    /**
     * Initializes multiplexer according to the arguments.
     *
     * @param enable specify output enabling pin
     * @param select specify selecting pin
     * @param selfChange pass {@code true} if in single multiplexer configuration
     * @param inverting choose if this multiplexer should be inverting the input
     * @throws NullPointerException if {@code enable} or {@code select} are null
     */
    public DualMux(Pin enable, Pin select, boolean selfChange, boolean inverting) {
        if (enable == null)
            throw new NullPointerException("Enable pin must be specified!");
        else if (select == null)
            throw new NullPointerException("Select pin must be specified!");

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

    /**
     * Updates the output according to the {@link #enable} and {@link #select} pins.
     *
     * @see #enable
     * @see #select
     */
    void update() {
        if (enable.level() == Pin.Level.HIGH) {
            output.direction(Pin.Direction.HI_Z);
        } else {
            output.direction(Pin.Direction.OUTPUT);

            Pin subject = select.level() == Pin.Level.LOW ? inputA : inputB;
            output.level(inverting != subject.level().bool()); // logical XOR
        }
    }

    /**
     * Returns current state of the multiplexer.
     * @return current state of the multiplexer
     */
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