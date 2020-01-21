package eu.tivian.hardware;

/**
 * An implementation of a mechanical switch.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 */
public class Switch {
    /**
     * Internal counter of created switches.
     */
    private static int count = 0;

    /**
     * Name of the switch.
     */
    private final String name;
    /**
     * First pin connected to the switch.
     */
    private final Pin lhs;
    /**
     * Second pin connected to the switch.
     */
    private final Pin rhs;
    /**
     * State of the switch.
     * <br>{@code true} meas the switch has been closed.
     */
    private boolean state;

    /**
     * Connects the switch between two specified pins.
     *
     * @param lhs a first pin
     * @param rhs a second pin
     */
    public Switch(Pin lhs, Pin rhs) {
        this("SW" + count, lhs, rhs);
    }

    /**
     * Connect the switch between two specified pins.
     *
     * @param name the name of the switch
     * @param lhs a first pin
     * @param rhs a second pin
     */
    public Switch(String name, Pin lhs, Pin rhs) {
        this(name, lhs, rhs, false);
    }

    /**
     * Connects the switch between two specified pins and sets initial state of the switch.
     *
     * @param name tha name of the switch
     * @param lhs a first pin
     * @param rhs a second pin
     * @param state an initial state of the pin
     */
    public Switch(String name, Pin lhs, Pin rhs, boolean state) {
        count++;
        this.name = name;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * Turns on the switch.
     */
    public void on() {
        update(true);
    }

    /**
     * Turns off the switch.
     */
    public void off() {
        update(false);
    }

    /**
     * Toggles the switch.
     */
    public void toggle() {
        update(!state);
    }

    /**
     * Double toggle of the switch, for short pulse generation.
     */
    public void monostable() {
        toggle();
        toggle();
    }

    /**
     * Gets the state of the switch.
     * @return the state of the switch
     */
    public boolean state() {
        return state;
    }

    /**
     * Updates the state of the switch.
     * @param state new state of the switch
     */
    private void update(boolean state) {
        this.state = state;
        if (state)
            lhs.connect(rhs);
        else
            lhs.disconnect(rhs);
    }

    /**
     * Gets the name of the switch.
     * @return the name of the switch
     */
    @Override
    public String toString() {
        return name;
    }
}
