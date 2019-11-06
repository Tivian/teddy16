package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Quad 2-input NOR gate
public class IC74LS02 {
    public final List<LogicGate> gate;

    public IC74LS02() {
        List<LogicGate> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new LogicGate("Gate " + i, LogicGate.Type.NOR));
        gate = Collections.unmodifiableList(temp);
    }
}
