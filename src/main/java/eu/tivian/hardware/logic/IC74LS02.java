package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quad 2-input NOR gate.
 * <br>
 * Logic implementation of 74LS02 chip.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see LogicGate
 * @see <a href="http://www.ti.com/lit/ds/symlink/sn74ls02.pdf">74LS02 datasheet</a>
  */

public class IC74LS02 {
    /**
     * Internal two NOR gates.
     */
    private final List<LogicGate> gate;

    /**
     * Initializes the logic gates.
     */
    public IC74LS02() {
        List<LogicGate> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new LogicGate("Gate " + i, LogicGate.Type.NOR));
        gate = Collections.unmodifiableList(temp);
    }

    /**
     * Returns the logic gate at the specified position.
     *
     * @param i index of the logic gate
     * @return the requested logic gate
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (i &lt; 0 || i &ge; 2)
     */
    public LogicGate get(int i) {
        return gate.get(i);
    }
}
