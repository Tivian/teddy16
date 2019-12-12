package eu.tivian.hardware;

import eu.tivian.other.Logger;

public class Pin {
    public enum Direction {
        INPUT, OUTPUT, HI_Z
    }

    public enum Level {
        LOW, HIGH;
        public boolean bool() {
            return this == HIGH;
        }
    }

    public static class Power extends Pin {
        public Power() { super("Power", Direction.OUTPUT, Level.HIGH); }
        @Override
        public void direction(Direction dir) { return; }
        @Override
        public boolean level(Level value) { return false; }
    }

    public static class Ground extends Pin {
        public Ground() { super("Ground", Direction.OUTPUT, Level.LOW); }
        @Override
        public void direction(Direction dir) { return; }
        @Override
        public boolean level(Level value) { return false; }
    }

    public static final Pin VCC = new Power();
    public static final Pin GND = new Ground();

    private final String name;

    private Wire wire;
    private Direction direction;
    private Runnable onChange = null;
    private boolean pullUp;
    private Level level = Level.LOW;

    public Pin() {
        this("");
    }

    public Pin(String name) {
        this(name, Direction.HI_Z);
    }

    public Pin(Direction direction) {
        this("", direction);
    }

    public Pin(Direction direction, Level level) {
        this("", direction, level);
    }

    public Pin(String name, Direction direction) {
        this(name, direction, Level.LOW);
    }

    public Pin(String name, Direction direction, Level level) {
        this(name, direction, level, false);
    }

    public Pin(String name, Direction direction, Level level, boolean pullUp) {
        this.name = name;
        this.direction = direction;
        if (direction == Direction.OUTPUT)
            this.level = level;
        this.pullUp = pullUp;
        this.wire = null;
    }

    public boolean isPulled() {
        return pullUp;
    }

    public void pullUp() {
        pullUp(true);
    }

    public void pullUp(boolean pullUp) {
        this.pullUp = pullUp;
        if (wire != null)
            wire.update(this);
    }

    public Pin connect(Wire wire) {
        this.wire = wire;
        return this;
    }

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

    public void disconnect() {
        if (wire != null) {
            wire.disconnect(this);
            this.wire = null;
        }

        if (direction == Direction.INPUT)
            update(pullUp ? Level.HIGH : Level.LOW);
    }

    public void disconnect(Pin other) {
        if (other == this)
            return;

        disconnect();
    }

    public void direction(Direction dir) {
        Direction old = this.direction;
        this.direction = dir;

        if (wire != null)
            wire.update(this, dir);
        else if (old == Direction.OUTPUT && dir == Direction.HI_Z)
            level = pullUp ? Level.HIGH : Level.LOW;
    }

    public Direction direction() {
        return direction;
    }

    public boolean level(boolean value) {
        return level(value ? Level.HIGH : Level.LOW);
    }

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

    public Level level() {
        if (Logger.ENABLE && direction == Direction.HI_Z)
            Logger.warn("Reading from HI-Z pin!");

        return level;
    }

    public Wire wire() {
        return wire;
    }

    private void update() {
        if (wire != null)
            wire.update(this);
    }

    private void update(Level level) {
        if (direction == Direction.INPUT) {
            this.level = level;
            if (onChange != null)
                onChange.run();
        }
    }

    void update(Wire notifier) {
        Level level = wire.level();
        if (direction == Direction.INPUT && this.level != level) {
            this.level = level;
            if (onChange != null)
                onChange.run();
        }
    }

    public void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public String toString() {
        return name + " [" + (level == Level.HIGH ? 1 : 0) + "]";
    }
}
