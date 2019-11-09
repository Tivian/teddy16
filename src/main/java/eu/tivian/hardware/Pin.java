package eu.tivian.hardware;

import java.util.function.Consumer;

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

    private Wire wire;
    private Direction direction;
    private Level level;
    private final String name;
    private Consumer<Level> onChange = null;

    public Pin() {
        this("");
    }

    public Pin(String name) {
        this(name, Direction.HI_Z);
    }

    public Pin(String name, Direction direction) {
        this(name, direction, Level.LOW);
    }

    public Pin(String name, Direction direction, Level level) {
        this.name = name;
        this.direction = direction;
        this.level = level;
        this.wire = null;
    }

    public void connect(Wire wire) {
        this.wire = wire;
    }

    public void connect(Pin other) {
        if (other.wire != null) {
            wire = other.wire;
        } else {
            wire = new Wire();
            other.wire = wire;
        }

        other.wire.connect(this);
    }

    public void disconnect() {
        this.wire = null;
    }

    public void direction(Direction dir) {
        this.direction = dir;
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
        return level;
    }

    private void update() {
        if (wire != null)
            wire.update(this);
    }

    void update(Wire notifier, Level level) {
        if (direction == Direction.OUTPUT) {
            throw new IllegalArgumentException("Output pin value cannot be changed externally!");
        } else if (direction != Direction.HI_Z && notifier == wire && this.level != level) {
            this.level = level;
            onChange.accept(level);
        }
    }

    public void onChange(Consumer<Level> onChange) {
        this.onChange = onChange;
    }

    @Override
    public String toString() {
        return name;
    }
}
