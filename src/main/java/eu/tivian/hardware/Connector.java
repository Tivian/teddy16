package eu.tivian.hardware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Connector {
    public enum Gender {
        FEMALE, MALE
    }

    private static int count = 0;

    public final Gender gender;
    private final String name;
    private final List<Pin> pins;
    private Connector connection = null;

    public Connector(Gender gender) {
        this("CN" + count, gender);
    }

    public Connector(String name, Gender gender, Pin... pins) {
        count++;
        this.gender = gender;
        this.name = name;
        this.pins = pins == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(pins));
    }

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

    public void disconnect() {
        if (connection == null)
            return;

        for (int i = 0; i < pins.size(); i++)
            get(i).disconnect(connection.get(i));

        connection.connection = null;
        connection = null;
    }

    public Pin get(int i) {
        return pins.get(i);
    }

    public Connector add(Pin pin) {
        if (!pins.contains(pin)) {
            pins.add(pin);
            if (connection != null && pins.size() == connection.pins.size())
                pin.connect(connection.get(pins.size() - 1));
        }

        return this;
    }

    public Connector remove(int i) {
        return remove(pins.get(i));
    }

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

    public int size() {
        return pins.size();
    }

    @Override
    public String toString() {
        return name;
    }
}
