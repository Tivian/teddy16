package eu.tivian.hardware;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An implementation of collection of pins. Usually called the bus.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see Pin
 */
public class Bus implements Iterable<Pin> {
    /**
     * The name of the bus.
     */
    private final String name;
    /**
     * List of pins associated with the bus.
     */
    private final List<Pin> pins;
    /**
     * Connection with other buses.
     */
    private final Set<Bus> connections;
    /**
     * Activates after changing the logical level of the bus.
     */
    private Runnable onChange = null;
    /**
     * Direction of the bus.
     */
    private Pin.Direction direction;

    /**
     * Initializes unnamed bus with given list of pins.
     *
     * @param pins a list of associated pins
     */
    Bus(List<Pin> pins) {
        this("", pins);
    }

    /**
     * Initializes bus with given name and list of pins.
     *
     * @param name the name of the bus
     * @param pins a list of associated pins
     */
    Bus(String name, List<Pin> pins) {
        this.name = name;
        this.pins = new ArrayList<>(pins);
        this.connections = new HashSet<>();
        this.direction = pins.get(0).direction();
    }

    /**
     * Initializes the bus pins with given prefix.
     *
     * @param name the name of the bus
     * @param prefix prefix of the pin names
     * @param length the number of pins
     */
    public Bus(String name, String prefix, int length) {
        this(name, prefix, Pin.Direction.HI_Z, length);
    }

    /**
     * Initializes the bus with full customization.
     *
     * @param name the name of the bus
     * @param prefix prefix of the pin names
     * @param direction direction of the bus
     * @param length the number of pins
     */
    public Bus(String name, String prefix, Pin.Direction direction, int length) {
        this.name = name;
        this.direction = direction;
        this.connections = new HashSet<>();

        List<Pin> temp = new ArrayList<>();
        for (int i = 0; i < length; i++)
            temp.add(new Pin(prefix + i, direction));
        pins = Collections.unmodifiableList(temp);
    }

    /**
     * Gets the pin at specified position.
     *
     * @param i index of the pin
     * @return the requested pin
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (check with {@link #size()})
     */
    public Pin get(int i) {
        return pins.get(i);
    }

    /**
     * Gets the size of the bus.
     * @return the size of the bus
     */
    public int size() {
        return pins.size();
    }

    /**
     * Connects the bus to another.
     *
     * @param other a bus to connect to
     * @return a reference to this object
     */
    public Bus connect(Bus other) {
        return connect(other, i -> i);
    }

    /**
     * Connects the bus to another and maps the pins.
     *
     * @param other a bus to connect to
     * @param mapper maps the pin indexes
     * @return a reference to this object
     */
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

    /**
     * Disconnects the bus from any other bus.
     */
    public void disconnect() {
        for (Bus b : connections)
            b.disconnect(this);
        connections.clear();
    }

    /**
     * Disconnects two buses from each other.
     * @param other a bus to disconnect
     */
    public void disconnect(Bus other) {
        connections.remove(other);
    }

    /**
     * Gets the direction of the bus.
     * @return the direction of the bus
     */
    public Pin.Direction direction() {
        return direction;
    }

    /**
     * Changes the direction of the bus.
     * @param direction new direction of the bus
     */
    public void direction(Pin.Direction direction) {
        pins.forEach(p -> p.direction(direction));
        this.direction = direction;
    }

    /**
     * Changes the direction of the bus, pin by pin.
     * @param direction each bit represents either output ({@code 1}) or input ({@code 0})
     */
    public void direction(long direction) {
        for (Pin p : pins) {
            p.direction((direction & 1) != 0 ? Pin.Direction.OUTPUT : Pin.Direction.INPUT);
            direction >>= 1;
        }
    }

    /**
     * Translates direction of all pins to bits.
     * @return bit representation of pin directions
     */
    public long dirValue() {
        long val = 0x00;
        for (int i = pins.size() - 1; i >= 0; i--) {
            val <<= 1;
            val |= pins.get(i).direction() == Pin.Direction.OUTPUT ? 1 : 0;
        }

        return val;
    }

    /**
     * Translates levels of all pins to bits.
     * @return bit representation of the levels of the pins
     */
    public long value() {
        long val = 0x00;
        for (int i = pins.size() - 1; i >= 0; i--) {
            val <<= 1;
            val |= pins.get(i).level() == Pin.Level.HIGH ? 1 : 0;
        }

        return val;
    }

    /**
     * Sets levels of the pins in bit format.
     * @param val bit representation of the levels of the pins
     */
    public void value(long val) {
        boolean changed = false;

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
            }
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
     * Gets the iterator through the pins associated with the bus.
     * @return the iterator through the pins associated with the bus
     */
    @Override
    public Iterator<Pin> iterator() {
        return pins.iterator();
    }

    /**
     * Returns the name of the bus.
     * @return the name of the bus.
     */
    @Override
    public String toString() {
        return name + " [" + Long.toString(value(), 2) + "]";
    }
}
