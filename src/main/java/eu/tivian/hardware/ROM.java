package eu.tivian.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ROM chip, based on CSG23128 used in C16.
 *
 * @since 2019-11-06
 * @see Memory
 * @see <a href="http://www.zimmers.net/anonftp/pub/cbm/documents/chipdata/23128.zip">CSG23128 datasheet</a>
 */
public class ROM extends Memory {
    /**
     * This ROM chip has three chip select pins.
     * <br><b>CS1</b> and <b>CS2</b> are inverted, <b>CS3</b> isn't.
     */
    public final List<Pin> cs; // cs1 - inverted, cs2 - inverted, cs3 - not inverted

    /**
     * Initializes the chip with default name.
     * @param size size of the memory array
     */
    public ROM(int size) {
        this("ROM", size);
    }

    /**
     * Initializes the chip with given parameters.
     *
     * @param name name of the chip
     * @param size size of the memory array
     */
    public ROM(String name, int size) {
        super(
            name,
            new Bus("data"   , "D", Pin.Direction.HI_Z ,  8),
            new Bus("address", "A", Pin.Direction.INPUT, 14),
            size
        );

        List<Pin> temp = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Pin pin = new Pin("cs" + i, Pin.Direction.INPUT);
            temp.add(pin);
            pin.onChange(() -> {
                enable();
                update();
            });
        }
        cs = Collections.unmodifiableList(temp);
        address.onChange(this::update);
    }

    /**
     * Preloads the contents of the chip.
     * @param content data which should be loaded onto the chip
     */
    public void preload(byte[] content) {
        System.arraycopy(content, 0, this.content, 0, this.content.length);
    }

    /**
     * Preloads from file contents of the chip.
     *
     * @param file the path to the file
     * @throws IOException if an I/O error occurs reading from the file
     */
    public void preload(Path file) throws IOException {
        preload(Files.readAllBytes(file));
    }

    /**
     * Enables the chip if the LOW level is at CS1 and CS2, and HIGH level on CS3 pins.
     * <br>Otherwise the data bus is changed to HI-Z state.
     */
    private void enable() {
        data.direction((!cs.get(0).level().bool() && !cs.get(1).level().bool() && cs.get(2).level().bool())
            ? Pin.Direction.OUTPUT : Pin.Direction.HI_Z);
    }

    /**
     * Updates data bus according to the value on address bus if chip was enabled.
     */
    private void update() {
        if (data.direction() == Pin.Direction.OUTPUT) {
            //if (Logger.ENABLE)
                //Logger.info(String.format("Output: 0x%02X from %s ROM at 0x%04X", content[(int) address.value()], name, address.value()));
            data.value(content[(int) address.value()]);
        }
    }

    /**
     * Returns the name of the chip.
     * @return the name of the chip.
     */
    @Override
    public String toString() {
        return name + " [" + content.length + "]";
    }
}
