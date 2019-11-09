package eu.tivian.hardware;

import java.util.HashSet;
import java.util.Set;

public class Wire {
    private final Set<Pin> pins = new HashSet<>();

    public Wire connect(Pin pin) {
        pin.connect(this);
        pins.add(pin);
        return this;
    }

    public Wire disconnect(Pin pin) {
        pins.remove(pin);
        return this;
    }

    void update(Pin notifier) {
        if (!pins.contains(notifier))
            return;

        var value = notifier.level();
        for (Pin p : pins) {
            if (p == notifier)
                continue;
            p.update(this, value);
        }
    }
}
