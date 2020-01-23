package eu.tivian.software;

import eu.tivian.hardware.MOS8501;
import eu.tivian.hardware.Pin;

import java.util.function.Consumer;

/**
 * Special version of {@link MOS8501} with simple memory management.
 * <br>Meant for debugging.
 *
 * @author Paweł Kania
 * @since 2020-01-21
 */
public class SimpleCPU extends MOS8501 {
    /**
     * 64KB of memory
     */
    public byte[] memory = new byte[0x10000];

    /**
     * Sets the program counter.
     * @param newPC the program counter
     */
    public void counter(short newPC) {
        stage = Stage.OPCODE;
        this.PC = newPC;
    }

    /**
     * Starts the CPU.
     */
    public void start() {
        stage = Stage.OPCODE;
        halt = false;
    }

    /**
     * Read value from the memory.
     *
     * @param address   cell index in the memory
     * @param readCycle functor which operates on the read memory
     * @throws ArrayIndexOutOfBoundsException if the {@code address} is out of range
     */
    @Override
    protected void read(short address, Consumer<Byte> readCycle) {
        if (readCycle != null)
            readCycle.accept(memory[address & 0xFFFF]);
        //halfCycleIn = () -> memory[address & 0xFFFF];
        //halfCycleOut = readCycle;
    }

    /**
     * Write value to the memory
     *
     * @param address cell index in the memory
     * @param value   value which should be written into the memory
     * @throws ArrayIndexOutOfBoundsException if the {@code address} is out of range
     */
    @Override
    protected void write(short address, byte value) {
        memory[address & 0xFFFF] = value;
        //halfCycleIn = () -> value;
        //halfCycleOut = data -> memory[address & 0xFFFF] = data;
    }

    /**
     * Initializes the CPU, disables external RESET and IRQ signal and enables support for undocumented instructions.
     */
    public SimpleCPU() {
        super();

        useUndocumented = true;
        super.reset.connect(new Pin(Pin.Direction.OUTPUT, Pin.Level.HIGH));
        super.irq.connect(new Pin(Pin.Direction.OUTPUT, Pin.Level.HIGH));
    }

    /**
     * Sets selected CPU registry.
     *
     * @param reg which CPU registry should be changed
     * @param val new value for selected registry
     */
    public void setReg(String reg, byte val) {
        switch (reg) {
            case "A":
                AC = val;
                break;
            case "X":
                XR = val;
                break;
            case "Y":
                YR = val;
                break;
            case "S":
                SP = val;
                break;
            case "P":
                SR = val;
                break;
        }
    }

    /**
     * Executes next CPU cycle.
     */
    @Override
    public void step() {
        super.step();
    }

    /**
     * Returns current execution stage of CPU.
     * @return execution stage
     */
    public Stage stage() {
        return stage;
    }
}
