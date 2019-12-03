package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// quad d flip-flop
public class IC74LS175 {
    private final List<DFlipFlop> flipFlop;

    public final Pin clock = new Pin("clock", Pin.Direction.INPUT);
    public final Pin reset = new Pin("master reset", Pin.Direction.INPUT);

    public IC74LS175() {
        List<DFlipFlop> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new DFlipFlop(clock, reset));
        flipFlop = Collections.unmodifiableList(temp);

        reset.onChange(() -> flipFlop.forEach(DFlipFlop::reset));
        clock.onChange(() -> flipFlop.forEach(DFlipFlop::update));
    }

    public DFlipFlop get(int i) {
        return flipFlop.get(i);
    }
}
