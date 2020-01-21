package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dual 2-line to 4-line demultiplexer.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see QuadDemux
 * @see <a href="http://www.ti.com/lit/ds/symlink/sn74ls139a.pdf">74LS139 datasheet</a>
 */
public class IC74LS139 {
    /**
     * Internal two demultiplexers.
     */
    private final List<QuadDemux> decoder;

    /**
     * Initializes the inner logic.
     */
    public IC74LS139() {
        List<QuadDemux> temp = new ArrayList<>();
        for (int i = 1; i <= 2; i++)
            temp.add(new QuadDemux());
        decoder = Collections.unmodifiableList(temp);
    }

    /**
     * Returns the demultiplexer at the specified position.
     *
     * @param i index of the demultiplexer
     * @return the requested demultiplexer
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (i &lt; 0 || i &ge; 2)
     */
    public QuadDemux get(int i) {
        return decoder.get(i);
    }
}
