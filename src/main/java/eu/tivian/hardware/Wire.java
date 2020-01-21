package eu.tivian.hardware;

import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of copper wire or PCB trace.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see Pin
 */
public class Wire {
    /**
     * List of pins that drive the level of this wire.
     */
    private final Set<Pin> driver = new HashSet<>();
    /**
     * List of pins that are changing with the level of this wire.
     */
    private final Set<Pin> wired = new HashSet<>();
    /**
     * Current level of the wire.
     */
    private Pin.Level level = Pin.Level.LOW;

    /**
     * Connects the pin to the wire.
     * @param pin the pin to connect
     */
    public void connect(Pin pin) {
        pin.connect(this);

        if (pin.direction() == Pin.Direction.OUTPUT) {
            driver.add(pin);
            update(pin);
        } else {
            wired.add(pin);
            update();
            pin.update(this);
        }
    }

    /**
     * Disconnects the pin from the wire.
     * @param pin the pin to disconnect
     */
    public void disconnect(Pin pin) {
        if (pin.direction() == Pin.Direction.OUTPUT) {
            driver.remove(pin);
            update(pin);
        } else {
            wired.remove(pin);
        }
    }

    /**
     * Gets the logical level of the wire.
     * @return the logical level of the wire
     */
    public Pin.Level level() {
        return level;
    }

    /**
     * Recalculates the level of the wire.
     */
    private void update() {
        Pin.Level newLevel = Pin.Level.LOW;
        if (driver.size() == 0) {
            for (Pin p : wired) {
                if (p.isPulled()) {
                    newLevel = Pin.Level.HIGH;
                    break;
                }
            }
        } else {
            for (Pin p : driver) {
                if (p.level() == Pin.Level.HIGH) {
                    newLevel = Pin.Level.HIGH;
                    break;
                }
            }
        }

        level = newLevel;
        /*level = ((driver.size() == 0)
            ? (wired.stream().anyMatch(Pin::isPulled))
            : driver.stream().map(p -> p.level().bool()).reduce(false, (sum, lvl) -> sum | lvl))
                ? Pin.Level.HIGH : Pin.Level.LOW;*/
    }

    /**
     * Updates the level of the wire.
     * @param notifier the pin which called this function
     */
    void update(Pin notifier) {
        //if (notifier == null)
            //return;

        /*if (driver.contains(notifier) && notifier.direction() != Pin.Direction.OUTPUT) {
            driver.remove(notifier);
            wired.add(notifier);
        } else if (wired.contains(notifier) && notifier.direction() == Pin.Direction.OUTPUT) {
            wired.remove(notifier);
            driver.add(notifier);
        }*/

        update();
        if (!wired.isEmpty()) {
            for (Pin p : wired)
                p.update(this);
        }
    }

    /**
     * Updates the level of the wire and rearranges the list of wire drivers.
     *
     * @param notifier the pin which called this function
     * @param direction new direction of the callee
     */
    void update(Pin notifier, Pin.Direction direction) {
        if (driver.contains(notifier) && notifier.direction() != Pin.Direction.OUTPUT) {
            driver.remove(notifier);
            wired.add(notifier);
        } else if (wired.contains(notifier) && notifier.direction() == Pin.Direction.OUTPUT) {
            wired.remove(notifier);
            driver.add(notifier);
        }

        update(notifier);
    }

    /**
     * Gets the name of the wire.
     * @return the name of the wire
     */
    @Override
    public String toString() {
        return level.toString();
    }
}
