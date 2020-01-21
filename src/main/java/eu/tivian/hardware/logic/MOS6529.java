package eu.tivian.hardware.logic;

import eu.tivian.hardware.Bus;
import eu.tivian.hardware.Pin;

/**
 * SPI (Single Port Interface)
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see <a href="http://archive.6502.org/datasheets/mos_6529_spi.pdf">MOS6529 datasheet</a>
 */
public class MOS6529 {
    /**
     * Usually connected to the data bus of the CPU.
     */
    public final Bus data = new Bus("data", "D", Pin.Direction.HI_Z, 8);
    /**
     * Usually connected to the keyboard matrix.
     */
    public final Bus port = new Bus("port", "P", Pin.Direction.HI_Z, 8);
    /**
     * When HIGH the data bus is changed to HI-Z state.
     */
    public final Pin cs = new Pin("chip select", Pin.Direction.INPUT);
    /**
     * When HIGH the levels on port bus are copied to the data bus.
     * Otherwise signals from data bus are copied to the port bus.
     */
    public final Pin rw = new Pin("read/write", Pin.Direction.INPUT);

    /**
     * Initializes chip logic and sets initial state of the pins.
     */
    public MOS6529() {
        cs.onChange(this::enable);
        rw.onChange(this::enable);
        data.onChange(this::update);
        port.onChange(this::update);

        enable();
    }

    /**
     * Changes direction of {@link #data} and {@link #port} buses according to the {@link #rw} pin.
     * <br>If {@link #cs} is held HIGH then both buses are in HI-Z state.
     */
    private void enable() {
        if (cs.level() == Pin.Level.HIGH) {
            data.direction(Pin.Direction.HI_Z);
            port.direction(Pin.Direction.HI_Z);
        } else if (rw.level() == Pin.Level.HIGH) {
            data.direction(Pin.Direction.INPUT);
            port.direction(Pin.Direction.OUTPUT);
        } else {
            data.direction(Pin.Direction.OUTPUT);
            port.direction(Pin.Direction.INPUT);
        }

        update();
    }

    /**
     * Copies levels from one bus to another accordingly to the direction of the buses.
     */
    private void update() {
        if (data.direction() == Pin.Direction.INPUT)
            port.value(data.value());
        else if (port.direction() == Pin.Direction.INPUT)
            data.value(port.value());
    }
}
