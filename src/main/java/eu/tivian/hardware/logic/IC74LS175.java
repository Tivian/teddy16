package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quad D flip-flop.
 * <br>
 * Logic implementation of 74LS175 chip.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see DFlipFlop
 * @see <a href="https://www.ti.com/lit/ds/symlink/sn74ls175.pdf">74LS175 datasheet</a>
 */
public class IC74LS175 {
    /**
     * Internal four flip-flops.
     */
    private final List<DFlipFlop> flipFlop;

    /**
     * Clock signal.
     */
    public final Pin clock = new Pin("clock", Pin.Direction.INPUT);
    /**
     * Master reset signal.
     */
    public final Pin reset = new Pin("master reset", Pin.Direction.INPUT);

    /**
     * Initializes the inner logic of quad D flip-flop.
     */
    public IC74LS175() {
        List<DFlipFlop> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new DFlipFlop(clock, reset));
        flipFlop = Collections.unmodifiableList(temp);

        reset.onChange(() -> flipFlop.forEach(DFlipFlop::reset));
        clock.onChange(() -> flipFlop.forEach(DFlipFlop::update));
    }

    /**
     * Returns the flip-flop at the specified position.
     *
     * @param i index of the flip-flop
     * @return the requested flip-flop
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (i &lt; 0 || i &ge; 4)
     */
    public DFlipFlop get(int i) {
        return flipFlop.get(i);
    }
}
