package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quad bus buffer with tri-state outputs.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see TriState
 * @see <a href="http://www.ti.com/lit/ds/symlink/sn74ls125a.pdf">74LS125 datasheet</a>
 */
public class IC74LS125 {
    /**
     * Internal tri-state gates.
     */
    private final List<TriState> buffer;

    /**
     * Initializes the inner logic of the bus buffer.
     */
    public IC74LS125() {
        List<TriState> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new TriState());
        buffer = Collections.unmodifiableList(temp);
    }

    /**
     * Returns the tri-state gate at the specified position.
     *
     * @param i index of the tri-state gate
     * @return the requested tri-state gate
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (i &lt; 0 || i &ge; 4)
     */
    public TriState get(int i) {
        return buffer.get(i);
    }
}
