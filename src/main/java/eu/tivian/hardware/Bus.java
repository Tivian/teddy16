package eu.tivian.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class Bus implements Iterable<Pin> {
    public final String name;
    public final List<Pin> pins;
    private List<Wire> wires = null;
    private Consumer<Long> onChange = null;
    private Pin.Direction direction;

    public Bus(String name, String prefix, int length) {
        this(name, prefix, Pin.Direction.HI_Z, length);
    }

    public Bus(String name, String prefix, Pin.Direction direction, int length) {
        this.name = name;
        this.direction = direction;

        List<Pin> temp = new ArrayList<>();
        for (int i = 0; i < length; i++)
            temp.add(new Pin(prefix + i, direction));
        pins = Collections.unmodifiableList(temp);
    }

    public void connect(Bus other) {
        if (other.pins.size() != pins.size())
            throw new IllegalArgumentException("Buses that you want to connect don't have the same number of pins");

        if (other.wires != null) {
            wires = other.wires;
        } else {
            wires = new ArrayList<>();
            other.wires = wires;
        }

        for (int i = 0; i < pins.size(); i++)
            other.wires.get(i).connect(pins.get(i));
    }

    public Pin.Direction direction() {
        return direction;
    }

    public void direction(Pin.Direction direction) {
        pins.forEach(p -> p.direction(direction));
        this.direction = direction;
    }

    public long value() {
        long val = 0x00;
        for (Pin p : pins) {
            val |= p.level() == Pin.Level.HIGH ? 1 : 0;
            val <<= 1;
        }

        return val;
    }

    public void value(long val) {
        boolean changed = false;
        long newValue = val;

        if (direction == Pin.Direction.HI_Z)
            return;

        for (Pin p : pins) {
            changed = changed || p.level((val & 1) != 0);
            val >>= 1;
        }

        if (changed)
            onChange.accept(newValue);
    }

    public void onChange(Consumer<Long> onChange) {
        this.onChange = onChange;
    }

    @Override
    public Iterator<Pin> iterator() {
        return pins.iterator();
    }

    @Override
    public String toString() {
        return name;
    }
}
