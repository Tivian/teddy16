package eu.tivian.hardware;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class Bus implements Iterable<Pin> {
    private final String name;
    private final List<Pin> pins;
    private final Set<Bus> connections;
    //private Consumer<Long> onChange = null;
    private Runnable onChange = null;
    private Pin.Direction direction;

    Bus(List<Pin> pins) {
        this("", pins);
    }

    Bus(String name, List<Pin> pins) {
        this.name = name;
        this.pins = new ArrayList<>(pins);
        this.connections = new HashSet<>();
        this.direction = pins.get(0).direction();
    }

    public Bus(String name, String prefix, int length) {
        this(name, prefix, Pin.Direction.HI_Z, length);
    }

    public Bus(String name, String prefix, Pin.Direction direction, int length) {
        this.name = name;
        this.direction = direction;
        this.connections = new HashSet<>();

        List<Pin> temp = new ArrayList<>();
        for (int i = 0; i < length; i++)
            temp.add(new Pin(prefix + i, direction));
        pins = Collections.unmodifiableList(temp);
    }

    public Pin get(int i) {
        return pins.get(i);
    }

    public int size() {
        return pins.size();
    }

    public Bus connect(Bus other) {
        return connect(other, i -> i);
    }

    public Bus connect(Bus other, Function<Integer, Integer> mapper) {
        if (!connections.contains(other)) {
            for (int i = 0; i < pins.size(); i++) {
                int index = mapper.apply(i);
                if (index >= 0)
                    other.pins.get(index).connect(pins.get(i));
            }

            connections.add(other);
            other.connections.add(this);
        }

        return this;
    }

    public void disconnect() {
        for (Bus b : connections)
            b.disconnect(this);
        connections.clear();
    }

    public void disconnect(Bus other) {
        connections.remove(other);
    }

    public Pin.Direction direction() {
        return direction;
    }

    public void direction(Pin.Direction direction) {
        pins.forEach(p -> p.direction(direction));
        this.direction = direction;
    }

    public void direction(long direction) {
        for (Pin p : pins) {
            p.direction((direction & 1) != 0 ? Pin.Direction.OUTPUT : Pin.Direction.INPUT);
            direction >>= 1;
        }
    }

    public long dirValue() {
        long val = 0x00;
        for (int i = pins.size() - 1; i >= 0; i--) {
            val <<= 1;
            val |= pins.get(i).direction() == Pin.Direction.OUTPUT ? 1 : 0;
        }

        return val;
    }

    public long value() {
        long val = 0x00;
        for (int i = pins.size() - 1; i >= 0; i--) {
            val <<= 1;
            val |= pins.get(i).level() == Pin.Level.HIGH ? 1 : 0;
        }

        return val;
    }

    public void value(long val) {
        boolean changed = false;
        //long newValue = val;

        if (direction == Pin.Direction.HI_Z)
            return;

        for (Pin pin : pins) {
            if (pin.level((val & 1) != 0))
                changed = true;
            val >>= 1;
        }

        if (changed) {
            for (Bus b : connections) {
                if (b.direction == Pin.Direction.INPUT && b.onChange != null)
                    b.onChange.run();
                    //b.onChange.accept(newValue);
            }
        }
    }

    //public void onChange(Consumer<Long> onChange) {
    public void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public Iterator<Pin> iterator() {
        return pins.iterator();
    }

    @Override
    public String toString() {
        return name + " [" + Long.toString(value(), 2) + "]";
    }
}
