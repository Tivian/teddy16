package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// hex inverter
public class IC7406 {
    private final List<LogicGate> gate;

    public IC7406() {
        List<LogicGate> temp = new ArrayList<>();
        for (int i = 1; i <= 6; i++)
            temp.add(new LogicGate("Gate " + i, LogicGate.Type.NOT));
        gate = Collections.unmodifiableList(temp);
    }

    public LogicGate get(int i) {
        return gate.get(i);
    }
}
