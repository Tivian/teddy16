package eu.tivian.hardware;

import eu.tivian.other.Logger;

/**
 * An implementation of an actual IC pin.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 */
public class Pin {
    /**
     * Direction of the pin.
     */
    public enum Direction {
        /**
         * Signal goes to the pin.
         */
        INPUT,
        /**
         * Signal goes from the pin.
         */
        OUTPUT,
        /**
         * Pin is in isolated state.
         */
        HI_Z
    }

    /**
     * Logic levels.
     */
    public enum Level {
        /**
         * Low level signal.
         * <br>Equivalent of {@code false}.
         */
        LOW,
        /**
         * High level signal.
         * <br>Equivalent of {@code true}.
         */
        HIGH;

        /**
         * Returns the boolean value of the signal.
         *
         * @return {@code true} if {@link #HIGH} otherwise {@code false}
         */
        public boolean bool() {
            return this == HIGH;
        }
    }

    /**
     * Inner private class for power pin.
     */
    private static class Power extends Pin {
        public Power() { super("Power", Direction.OUTPUT, Level.HIGH); }
        @Override
        public void direction(Direction dir) { }
        @Override
        public boolean level(Level value) { return false; }
    }

    /**
     * Inner private class for ground pin.
     */
    private static class Ground extends Pin {
        public Ground() { super("Ground", Direction.OUTPUT, Level.LOW); }
        @Override
        public void direction(Direction dir) { }
        @Override
        public boolean level(Level value) { return false; }
    }

    /**
     * Power pin.
     * <br>Always stays at {@link Level#HIGH} level.
     */
    public static final Pin VCC = new Power();
    /**
     * Ground pin.
     * <br>Always stays at {@link Level#LOW} level.
     */
    public static final Pin GND = new Ground();

    /**
     * Name of the pin.
     */
    private final String name;

    /**
     * Wire to which the pin is connected.
     */
    private Wire wire;
    /**
     * Direction of the pin.
     */
    private Direction direction;
    /**
     * Activates after changing the logical level of the pin.
     */
    private Runnable onChange = null;
    /**
     * {@code true} if pin should be pulled-up.
     */
    private boolean pullUp;
    /**
     * Internal state of the pin.
     */
    private Level level = Level.LOW;

    /**
     * Initializes isolated pin without the name.
     */
    public Pin() {
        this("");
    }

    /**
     * Initializes isolated pin with given name.
     *
     * @param name the name of the pin
     */
    public Pin(String name) {
        this(name, Direction.HI_Z);
    }

    /**
     * Initializes nameless pin with given direction.
     * @param direction direction of the pin
     */
    public Pin(Direction direction) {
        this("", direction);
    }

    /**
     * Initializes nameless pin with given direction and initial level.
     *
     * @param direction direction of the pin
     * @param level initial level of the pin
     */
    public Pin(Direction direction, Level level) {
        this("", direction, level);
    }

    /**
     * Initializes the pin with given name and direction.
     *
     * @param name name of the pin
     * @param direction direction of the pin
     */
    public Pin(String name, Direction direction) {
        this(name, direction, Level.LOW);
    }

    /**
     * Initializes the pin with given name, direction and initial level.
     *
     * @param name name of the pin
     * @param direction direction of the pin
     * @param level initial level of the pin
     */
    public Pin(String name, Direction direction, Level level) {
        this(name, direction, level, false);
    }

    /**
     * Initializes the pin with full customization.
     *
     * @param name name of the pin
     * @param direction direction of the pin
     * @param level initial level of the pin
     * @param pullUp {@code true} if pin should be pulled-up
     */
    public Pin(String name, Direction direction, Level level, boolean pullUp) {
        this.name = name;
        this.direction = direction;
        if (direction == Direction.OUTPUT)
            this.level = level;
        this.pullUp = pullUp;
        this.wire = null;
    }

    /**
     * Returns {@code true} if the pin is pulled-up.
     * @return {@code true} if the pin is pulled-up
     */
    public boolean isPulled() {
        return pullUp;
    }

    /**
     * Turns on the pull-up.
     */
    public void pullUp() {
        pullUp(true);
    }

    /**
     * Sets or resets the pull-up.
     * @param pullUp {@code true} if the pin should be pulled-up
     */
    public void pullUp(boolean pullUp) {
        this.pullUp = pullUp;
        if (wire != null)
            wire.update(this);
    }

    /**
     * Connects the pin to the wire.
     *
     * @param wire a wire to which the pin should be connected
     * @return a reference to this object
     */
    public Pin connect(Wire wire) {
        this.wire = wire;
        return this;
    }

    /**
     * Connects the pin to another pin.
     *
     * @param other a pin which should be connected with this pin.
     * @return a reference to this object
     */
    public Pin connect(Pin other) {
        if (other != this) {
            if (wire != null) {
                wire.connect(other);
            } else if (other.wire == null) {
                wire = new Wire();
                wire.connect(this);
                wire.connect(other);
            } else {
                other.wire.connect(this);
            }
        }

        return this;
    }

    /**
     * Disconnects the pin from the wire.
     */
    public void disconnect() {
        if (wire != null) {
            wire.disconnect(this);
            this.wire = null;
        }

        if (direction == Direction.INPUT)
            update(pullUp ? Level.HIGH : Level.LOW);
    }

    /**
     * Disconnects the pin if the caller was another pin.
     *
     * @param other caller
     */
    public void disconnect(Pin other) {
        if (other == this)
            return;

        disconnect();
    }

    /**
     * Changes the direction of the pin.
     * @param dir new direction of the pin
     */
    public void direction(Direction dir) {
        Direction old = this.direction;
        this.direction = dir;

        if (wire != null)
            wire.update(this, dir);
        else if (old == Direction.OUTPUT && dir == Direction.HI_Z)
            level = pullUp ? Level.HIGH : Level.LOW;
    }

    /**
     * Gets the direction of the pin.
     * @return the direction of the pin
     */
    public Direction direction() {
        return direction;
    }

    /**
     * Changes the level of the pin.
     *
     * @param value boolean value of the logical level
     * @return {@code true} if the value was changed
     */
    public boolean level(boolean value) {
        return level(value ? Level.HIGH : Level.LOW);
    }

    /**
     * Changes the level of the pin.
     *
     * @param value new logical value of the pin
     * @return {@code true} if the value was changed
     */
    public boolean level(Level value) {
        if (direction == Direction.INPUT) {
            throw new IllegalArgumentException("Input pin value cannot be changed, only checked!");
        } else if (direction != Direction.HI_Z && this.level != value) {
            this.level = value;
            update();

            return true;
        }

        return false;
    }

    /**
     * Gets the level of the pin.
     * @return the level of the pin.
     */
    public Level level() {
        if (Logger.ENABLE && direction == Direction.HI_Z)
            Logger.warn("Reading from HI-Z pin!");

        return level;
    }

    /**
     * Gets the wire associated with this pin.
     * @return the wire associated with this pin
     */
    public Wire wire() {
        return wire;
    }

    /**
     * Updates state of the connected wire.
     */
    private void update() {
        if (wire != null)
            wire.update(this);
    }

    /**
     * Updates state of the wire and fires the event.
     * @param level new logical level of the pin
     */
    private void update(Level level) {
        if (direction == Direction.INPUT) {
            this.level = level;
            if (onChange != null)
                onChange.run();
        }
    }

    /**
     * Updates state of the wire and fires the event if the level changed.
     * @param notifier the wire which called this function
     */
    void update(Wire notifier) {
        Level level = wire.level();
        if (direction == Direction.INPUT && this.level != level) {
            this.level = level;
            if (onChange != null)
                onChange.run();
        }
    }

    /**
     * Sets the event handler.
     * @param onChange the event handler
     */
    public void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /**
     * Gets the name of the pin.
     * @return the name of the pin
     */
    @Override
    public String toString() {
        return name + " [" + (level == Level.HIGH ? 1 : 0) + "]";
    }
}
