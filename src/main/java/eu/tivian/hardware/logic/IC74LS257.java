package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Quad 2-data multiplexer
public class IC74LS257 {
    private final List<DualMux> mux;

    public final Pin enable = new Pin("Output Control", Pin.Direction.INPUT);
    public final Pin select = new Pin("Select", Pin.Direction.INPUT);

    public IC74LS257() {
        List<DualMux> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new DualMux(enable, select));
        mux = Collections.unmodifiableList(temp);

        enable.onChange(() -> mux.forEach(DualMux::update));
        select.onChange(() -> mux.forEach(DualMux::update));
    }

    public DualMux get(int i) {
        return mux.get(i);
    }
}
