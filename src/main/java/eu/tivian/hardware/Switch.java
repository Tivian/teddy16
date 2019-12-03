package eu.tivian.hardware;

public class Switch {
    private static int count = 0;

    private final String name;
    private final Pin lhs;
    private final Pin rhs;
    private boolean state;

    public Switch(Pin lhs, Pin rhs) {
        this("SW" + count, lhs, rhs);
    }

    public Switch(String name, Pin lhs, Pin rhs) {
        this(name, lhs, rhs, false);
    }

    public Switch(String name, Pin lhs, Pin rhs, boolean state) {
        count++;
        this.name = name;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public void on() {
        update(true);
    }

    public void off() {
        update(false);
    }

    public void toggle() {
        update(!state);
    }

    public void monostable() {
        on();
        off();
    }

    public boolean state() {
        return state;
    }

    private void update(boolean state) {
        this.state = state;
        if (state)
            lhs.connect(rhs);
        else
            lhs.disconnect(rhs);
    }

    @Override
    public String toString() {
        return name;
    }
}
