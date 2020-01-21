package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hex inverter.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see LogicGate
 * @see <a href="http://www.ti.com/lit/ds/symlink/sn7406.pdf">7406 datasheet</a>
 */
public class IC7406 {
    /**
     * Internal NOT gates.
     */
    private final List<LogicGate> gate;

    /**
     * Initializes logic of the inverter.
     */
    public IC7406() {
        List<LogicGate> temp = new ArrayList<>();
        for (int i = 1; i <= 6; i++)
            temp.add(new LogicGate("Gate " + i, LogicGate.Type.NOT));
        gate = Collections.unmodifiableList(temp);
    }

    /**
     * Returns the logic gate at the specified position.
     *
     * @param i index of the logic gate
     * @return the requested logic gate
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (i &lt; 0 || i &ge; 6)
     */
    public LogicGate get(int i) {
        return gate.get(i);
    }
}
