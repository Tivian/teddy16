package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quad 2-data multiplexer.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see DualMux
 * @see <a href="http://www.ti.com/lit/ds/symlink/sn74ls257b.pdf">74LS257 datasheet</a>
 */
// Quad 2-data multiplexer
public class IC74LS257 {
    /**
     * Internal 2-data multiplexers.
     */
    private final List<DualMux> mux;

    /**
     * Chip select signal.
     * When HIGH outputs are set to HI-Z.
     */
    public final Pin enable = new Pin("Output Control", Pin.Direction.INPUT);
    /**
     * Input select signal.
     * When LOW the output depends on A inputs, otherwise on B inputs.
     */
    public final Pin select = new Pin("Select", Pin.Direction.INPUT);

    /**
     * Initializes inner logic of the multiplexer.
     */
    public IC74LS257() {
        List<DualMux> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new DualMux(enable, select));
        mux = Collections.unmodifiableList(temp);

        enable.onChange(() -> mux.forEach(DualMux::update));
        select.onChange(() -> mux.forEach(DualMux::update));
    }

    /**
     * Returns the multiplexer at the specified position.
     *
     * @param i index of the multiplexer
     * @return the requested multiplexer
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (i &lt; 0 || i &ge; 4)
     */
    public DualMux get(int i) {
        return mux.get(i);
    }
}
