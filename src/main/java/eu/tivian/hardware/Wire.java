package eu.tivian.hardware;

import java.util.HashSet;
import java.util.Set;

public class Wire {
    private final Set<Pin> driver = new HashSet<>();
    private final Set<Pin> wired = new HashSet<>();
    private Pin.Level level = Pin.Level.LOW;

    Wire() {}

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

    public void disconnect(Pin pin) {
        if (pin.direction() == Pin.Direction.OUTPUT) {
            driver.remove(pin);
            update(pin);
        } else {
            wired.remove(pin);
        }
    }

    public Pin.Level level() {
        return level;
    }

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

    @Override
    public String toString() {
        return level.toString();
    }
}
