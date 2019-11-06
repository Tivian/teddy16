package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Quad 2-data multiplexer
public class IC74LS257 {
    public final List<DualMux> mux;

    public final Pin enable = new Pin("Output Control", Pin.Direction.INPUT);
    public final Pin select = new Pin("Chip Select", Pin.Direction.INPUT);

    public IC74LS257() {
        List<DualMux> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new DualMux(enable, select));
        mux = Collections.unmodifiableList(temp);
    }
}
