package eu.tivian.hardware;

/**
 * A basic CPU interface.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see MOS8501
 */
public interface CPU {
    /**
     * Returns the number of elapsed CPU cycles.
     * @return the number of elapsed CPU cycles
     */
    long cycles();

    /**
     * Returns current program counter.
     * @return current program counter.
     */
    short counter();

    /**
     * Returns the position of the last opcode.
     * @return the position of the last opcode
     */
    short lastOpcodePosition();

    /**
     * Returns currently processed instruction mnemonics.
     * @return currently processed instruction mnemonics
     */
    String mnemonic();

    /**
     * Returns currently processed opcode.
     * @return currently processed opcode
     */
    byte opcode();

    /**
     * Returns CPU registers in string format.
     * @return CPU registers in string format
     */
    String reg();
}
