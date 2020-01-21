package eu.tivian.hardware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * An implementation of the electrical connector.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 */
public class Connector {
    /**
     * Gender of the connector.
     * <br>Only opposite genders can be interconnected.
     */
    public enum Gender {
        FEMALE, MALE
    }

    /**
     * Internal connector counter.
     */
    private static int count = 0;

    /**
     * Gender of the connector.
     */
    public final Gender gender;
    /**
     * Name of the connector.
     */
    private final String name;
    /**
     * Pins present in this connector.
     */
    private final List<Pin> pins;
    /**
     * Other connector connected to this component.
     */
    private Connector connection = null;

    /**
     * Creates the connector with default name and given gender.
     * @param gender gender of the connector
     */
    public Connector(Gender gender) {
        this("CN" + count, gender);
    }

    /**
     * Creates the connector with given name, gender and pins.
     *
     * @param name the name of the connector
     * @param gender the gender of the connector
     * @param pins pins associated with this connector
     */
    public Connector(String name, Gender gender, Pin... pins) {
        count++;
        this.gender = gender;
        this.name = name;
        this.pins = pins == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(pins));
    }

    /**
     * Connects this connector to another.
     *
     * @param other the connector to connect
     * @throws IllegalArgumentException if {@code other} has the same gender
     */
    public void connect(Connector other) {
        if (connection != null)
            return;

        if (gender == other.gender)
            throw new IllegalArgumentException("You cannot connect two connectors of the same gender");

        connection = other;
        other.connection = this;
        for (int i = 0; i < pins.size(); i++)
            get(i).connect(connection.get(i));
    }

    /**
     * Disconnects the connector from another.
     */
    public void disconnect() {
        if (connection == null)
            return;

        for (int i = 0; i < pins.size(); i++)
            get(i).disconnect(connection.get(i));

        connection.connection = null;
        connection = null;
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
     * Adds pin to the connector.
     *
     * @param pin the pin to add
     * @return a reference to this object
     */
    public Connector add(Pin pin) {
        if (!pins.contains(pin)) {
            pins.add(pin);
            if (connection != null && pins.size() == connection.pins.size())
                pin.connect(connection.get(pins.size() - 1));
        }

        return this;
    }

    /**
     * Removes the pin from the connector.
     *
     * @param i index of the pin
     * @return a reference to this object
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (check with {@link #size()})
     */
    public Connector remove(int i) {
        return remove(pins.get(i));
    }

    /**
     * Removes the pin from the connector.
     *
     * @param pin the pin to remove
     * @return a reference to this object
     */
    public Connector remove(Pin pin) {
        int i = pins.indexOf(pin);
        if (connection != null) {
            Pin other = connection.get(i);
            pin.disconnect(other);
            connection.pins.remove(other);
        }
        pins.remove(pin);

        return this;
    }

    /**
     * Returns the number of pins in this connector.
     * @return the number of pins in this connector
     */
    public int size() {
        return pins.size();
    }

    /**
     * Gets the name of the connector.
     * @return the name of the connector
     */
    @Override
    public String toString() {
        return name;
    }
}
