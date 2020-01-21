package eu.tivian.hardware;

/**
 * Fundamental memory class.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see ROM
 * @see RAM
 */
public abstract class Memory {
    /**
     * Data bus.
     */
    public final Bus data;
    /**
     * Address bus.
     */
    public final Bus address;
    /**
     * Name of the memory chip.
     */
    public final String name;

    /**
     * Contents of the memory chip.
     */
    protected final byte[] content;

    /**
     * Initialize memory chip with given parameters.
     *
     * @param name name of the chip
     * @param data specify the data bus
     * @param address specify the address bus
     * @param size specify size of the memory chip
     */
    protected Memory(String name, Bus data, Bus address, int size) {
        if (size <= 0)
            throw new IllegalArgumentException("Memory chip must have size greater than zero!");
        else if (data == null)
            throw new NullPointerException("Data bus must be specified!");
        else if (address == null)
            throw new NullPointerException("Address bus must be specified!");

        this.name = name;
        this.data = data;
        this.address = address;
        this.content = new byte[size];
    }

    /**
     * Peeks into the memory.
     *
     * @param address index of the cell in the memory
     * @return value at given address
     * @throws ArrayIndexOutOfBoundsException if address is out of boundaries (check {@link #size()})
     */
    public byte peek(int address) {
        return content[address];
    }

    /**
     * Pokes desired address with given value.
     *
     * @param address index of the cell in the memory
     * @param data value to write
     * @throws ArrayIndexOutOfBoundsException if address is out of boundaries (check {@link #size()})
     */
    public void poke(int address, byte data) {
        content[address] = data;
    }

    /**
     * Returns the size of the memory chip.
     * @return the size of the memory chip.
     */
    public int size() {
        return content.length;
    }
}
