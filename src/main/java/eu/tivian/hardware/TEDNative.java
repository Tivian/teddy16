package eu.tivian.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * JNI bridge to Verilog version of TED.
 * <br>Original source code is available on <a href="https://github.com/ishe/plus4/blob/master/ted.v">Github</a>.
 *
 * @author Paweł Kania
 * @author Istvan Hegedus
 * @since 2019-12-03
 * @see TED
 * @see <a href="https://github.com/ishe/plus4">FPGATED Project</a>
 * @see <a href="https://www.pagetable.com/docs/ted/TED%207360R0%20Preliminary%20Data%20Sheet.pdf">TED datasheet</a>
 */
class TEDNative {
    static {
        var stream = TEDNative.class.getResourceAsStream(System.mapLibraryName("/jni/ted"));

        try {
            var temp = Files.createTempFile("ted", System.mapLibraryName(""));
            temp.toFile().deleteOnExit();
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toString());
        } catch (IOException ex) { }
    }

    /**
     * Creates TED object.
     */
    public native void init();

    /**
     * Calculates current state of the TED based on input signals.
     */
    public native void step();

    /**
     * Clears up the memory after TED.
     */
    public native void free();

    /**
     * Clock input signal.
     * @param state clock signal value
     */
    public native void clk(int state);

    /**
     * Clock output.
     * @return CPU clock
     */
    public native int cpuclk();

    /**
     * Read/write pin.
     * @param state read/write pin state
     */
    public native void rw(int state);

    /**
     * Interrupt signal.
     * @return {@code 0} if interrupt occurred
     */
    public native int irq();

    /**
     * Bus available pin.
     * @return {@code 0} stops the CPU
     */
    public native int ba();

    /**
     * Signal for memory multiplexers.
     * @return MUX signal
     */
    public native int mux();

    /**
     * RAS signal for RAM operations.
     * @return row address strobe signal
     */
    public native int ras();

    /**
     * CAS signal for RAM operations.
     * @return column address strobe signal
     */
    public native int cas();

    /**
     * Low ROM chip select.
     * @return low ROM chip select signal
     */
    public native int cs0();

    /**
     * High ROM chip select.
     * @return high ROM chip select signal
     */
    public native int cs1();

    /**
     * Address enable control.
     * @return address enable control signal
     */
    public native int aec();

    /**
     * Sound output.
     * @return sound value [0 - 255]
     */
    public native int snd();

    /**
     * Returns which video standard is selected.
     * @return {@code 0} if PAL output is selected, {@code 1} if NTSC output is selected
     */
    public native int pal();

    /**
     * Returns color index of currently drawn pixel.
     * @return color index of currently drawn pixel
     */
    public native int color();

    /**
     * Returns current video column.
     * @return current video column
     */
    public native int hcount();

    /**
     * Returns current video row.
     * @return current video row
     */
    public native int vcount();

    /**
     * Sets value in internal data bus.
     * @param data value to write to the internal data bus
     */
    public native void data_in(int data);

    /**
     * Gets value of internal data bus.
     * @return value of internal data bus
     */
    public native int data_out();

    /**
     * Returns {@code 1} if current address at address bus correlates to memory location in TED chip.
     * @return {@code 1} if current address at address bus correlates to memory location in TED chip
     */
    public native int tedreg();

    /**
     * Sets address in internal address bus.
     * @param addr address to write to the internal address bus
     */
    public native void addr_in(int addr);

    /**
     * Gets address from internal address bus.
     * @return address from internal address bus
     */
    public native int addr_out();

    /**
     * Sets lines at keyboard matrix port.
     * @param value value for keyboard matrix port
     */
    public native void keyboard(int value);
}
