package eu.tivian.hardware;

import eu.tivian.other.Logger;
import eu.tivian.software.Monitor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An implementation of MOS8501 - the 8-bit CPU from 1985 used in the Commodore 16.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see <a href="doc-files/mos_7501_8501.pdf">MOS8501 datasheet</a>
 * @see <a href="http://archive.6502.org/books/mcs6500_family_programming_manual.pdf">
 *     6500 Family Programming Manual</a>
 */
public class MOS8501 implements CPU {
    /**
     * Possible stages of instruction execution.
     */
    public enum Stage {
        /**
         * The CPU will be fetching the opcode of the next instruction.
         */
        OPCODE,
        /**
         * The CPU is waiting for data from the memory.
         */
        FETCH,
        /**
         * The CPU is decoding the instruction.
         */
        DECODE,
        /**
         * The CPU will be executing the instruction.
         */
        EXECUTE,
        //MEMORY
    };

    /**
     * Functor used for decoding the instructions.
     */
    public interface AddressingMode {
        /**
         * Decodes next stage of the instruction.
         */
        void decode();
    }

    /**
     * Functor used for executing the instructions.
     */
    public interface Operation {
        /**
         * Executes the instruction.
         */
        void execute();
    }

    /**
     * CPU status register.
     */
    public class Status {
        /**
         * CPU status bit masks.
         */
        public class Bit {
            /**
             * Negative flag.
             */
            static final int N = (1 << 7);
            /**
             * oVerflow flag.
             */
            static final int V = (1 << 6);
            /**
             * Unused.
             * <br>Always {@code 1}.
             */
            static final int O = (1 << 5);
            /**
             * Break flag.
             * <br>This flag is used to determine the source of the IRQ.
             */
            static final int B = (1 << 4);
            /**
             * Decimal mode flag.
             * <br>When {@code 1} the CPU is in BCD mode.
             */
            static final int D = (1 << 3);
            /**
             * Interrupt flag.
             * <br>When {@code 1} the interrupts are disabled.
             */
            static final int I = (1 << 2);
            /**
             * Zero flag.
             */
            static final int Z = (1 << 1);
            /**
             * Carry flag.
             */
            static final int C = (1 << 0);
        }

        /**
         * Gets the negative flag value.
         * @return the negative flag
         */
        public int negative() { return (SR & Bit.N) != 0 ? 1 : 0; }
        /**
         * Gets the overflow flag value.
         * @return the overflow flag
         */
        public int overflow() { return (SR & Bit.V) != 0 ? 1 : 0; }
        /**
         * Gets the break flag value.
         * @return {@code 0} if the interrupt was a real interrupt
         */
        public int brk() {      return (SR & Bit.B) != 0 ? 1 : 0; }
        /**
         * Gets the decimal flag value.
         * @return {@code 1} if the CPU is in BCD mode
         */
        public int decimal() {  return (SR & Bit.D) != 0 ? 1 : 0; }
        /**
         * Gets the interrupt flag value.
         * @return {@code 1} if the interrupts are disabled
         */
        public int irq() {      return (SR & Bit.I) != 0 ? 1 : 0; }
        /**
         * Gets the zero flag value.
         * @return the zero flag
         */
        public int zero() {     return (SR & Bit.Z) != 0 ? 1 : 0; }
        /**
         * Gets the carry flag.
         * @return the carry flag
         */
        public int carry() {    return (SR & Bit.C) != 0 ? 1 : 0; }
        /**
         * Sets the negative flag.
         * @param val the negative flag value
         */
        public void negative(boolean val) { if (val) SR |= Bit.N; else SR &= ~Bit.N; }
        /**
         * Sets the overflow flag.
         * @param val the overflow flag value
         */
        public void overflow(boolean val) { if (val) SR |= Bit.V; else SR &= ~Bit.V; }
        /**
         * Sets the break flag.
         * @param val the break flag value
         */
        public void brk(boolean val) {      if (val) SR |= Bit.B; else SR &= ~Bit.B; }
        /**
         * Sets the decimal mode flag.
         * @param val {@code true} if the CPU should be in BCD mode
         */
        public void decimal(boolean val) {  if (val) SR |= Bit.D; else SR &= ~Bit.D; }
        /**
         * Sets the interrupt flag.
         * @param val {@code true} if the interrupts should be disabled.
         */
        public void irq(boolean val) {      if (val) SR |= Bit.I; else SR &= ~Bit.I; }
        /**
         * Sets the zero flag.
         * @param val the zero flag value
         */
        public void zero(boolean val) {     if (val) SR |= Bit.Z; else SR &= ~Bit.Z; }
        /**
         * Sets the carry flag.
         * @param val the carry flag
         */
        public void carry(boolean val) {    if (val) SR |= Bit.C; else SR &= ~Bit.C; }

        /**
         * Sets the negative flag if the low byte is negative.
         * @param val the result of the last operation
         */
        void determineNegative(short val) { negative((val & 0x0080) != 0); }
        /**
         * Sets the overflow flag if the operand and accumulator do not have different signs
         * and the operand sign is different from the result sign.
         * @param res result of the operation
         * @param val the operand
         */
        void determineOverflow(short res, byte val) { overflow(((res ^ AC) & (res ^ val) & 0x0080) != 0); }
        /**
         * Sets the negative flag if the low byte is equal zero.
         * @param val the result of the last operation
         */
        void determineZero(short val) { zero((val & 0x00FF) == 0); }
        /**
         * Sets the carry flag if the result overflowed into the high byte.
         * @param val the result of the last operation
         */
        void determineCarry(short val) { carry((val & 0xFF00) != 0); }

        /**
         * Sets negative and zero flag according to the parameter.
         * <br>Many instructions affects the N and Z flags.
         * @param val the result of the last operation
         */
        void determineNZ(short val) { determineNegative(val); determineZero(val);}
    }

    /**
     * Hardwired vector for CPU port pins direction.
     */
    public static final short IO_DIR_VECT = (short) 0x0000;
    /**
     * Hardwired vector for CPU port pins value.
     */
    public static final short IO_VECT     = (short) 0x0001;
    /**
     * Hardwired vector for CPU stack.
     */
    public static final short STACK_VECT  = (short) 0x0100;
    /**
     * Hardwired RESET vector.
     */
    public static final short RESET_VECT  = (short) 0xFFFC;
    /**
     * Hardwired IRQ vector.
     */
    public static final short IRQ_VECT    = (short) 0xFFFE;

    /**
     * Value for unofficial ANE instruction.
     * <br>Taken from my own C64.
     */
    protected static final byte ANE_MAGIC    = (byte) 0xFE;
    /**
     * Value for unofficial LXA instruction.
     * <br>Taken from my own C64.
     */
    protected static final byte LXA_MAGIC    = (byte) 0xEE;

    /**
     * Status registry.
     */
    protected byte  SR = 0b00100000;
    /**
     * Program counter.
     */
    protected short PC = 0x0000;
    /**
     * Accumulator.
     */
    protected byte  AC = 0x00;
    /**
     * X index registry.
     */
    protected byte  XR = 0x00;
    /**
     * Y index registry.
     */
    protected byte  YR = 0x00;
    /**
     * Stack pointer.
     */
    protected byte  SP = 0x00;

    /**
     * CPU status registry singleton.
     */
    protected final Status status = new Status();

    /**
     * Clock input. Pin 1
     */
    public final Pin phi0  = new Pin("PHI0", Pin.Direction.INPUT);
    /**
     * Ready signal. Pin 2
     * <br>When held LOW for more than 3 cycles then the CPU will halt.
     */
    public final Pin rdy   = new Pin("RDY" , Pin.Direction.INPUT);
    /**
     * Interrupt input. Pin 3
     * <br>LOW level means the interrupt occured.
     */
    public final Pin irq   = new Pin("/IRQ", Pin.Direction.INPUT);
    /**
     * Address enable control. Pin 4
     * <br>When held LOW the address bus pins becomes isolated from the bus.
     */
    public final Pin aec   = new Pin("AEC" , Pin.Direction.INPUT);
    /**
     * Gate in. Pin 23
     * <br>For memory management. Prevents unwanted writes to the RAM.
     */
    public final Pin gate  = new Pin("Gate", Pin.Direction.INPUT);
    /**
     * Read/write signal. Pin 39
     * <br>LOW logic level mean it's a write cycle, otherwise it's a read cycle.
     */
    public final Pin rw    = new Pin("R/-W", Pin.Direction.OUTPUT, Pin.Level.HIGH);
    /**
     * Reset pin. Pin 40
     * <br>After transition from LOW to HIGH the CPU will restart itself.
     */
    public final Pin reset = new Pin("/RES", Pin.Direction.INPUT);

    /**
     * Address bus. Pins 6 - 19, 21, 22
     */
    public final Bus address = new Bus("Address", "A" , Pin.Direction.OUTPUT, 16);
    /**
     * Data bus. Pins 31 - 38
     */
    public final Bus data    = new Bus("Data"   , "DB", Pin.Direction.OUTPUT,  8);
    /**
     * CPU IO port. Pins 24 - 30
     * <br>In 8501/7501 this port is 7-pins wide.
     */
    public final Bus port    = new Bus("Port"   , "P" , Pin.Direction.OUTPUT,  7);

    /**
     * CPU halt flag.
     * <br>If {@code true} then the CPU is halted.
     */
    protected boolean halt       = true;
    /**
     * Denotes the pending interrupt request.
     */
    protected boolean irqPending = false;
    /**
     * Denotes if situation occurred which prevents interrupt request from being processed.
     * <br>Due to a design flaw it's possible to mask the interrupt.
     */
    protected boolean maskIRQ    = false;
    /**
     * Counts how many consecutive cycles the RDY pin was held LOW.
     * <br>If this value exceed 3 cycles then the CPU halts its operation.
     */
    protected byte    rdyCounter = 0;

    /**
     * Current stage of instruction processing.
     */
    protected Stage          stage       = Stage.OPCODE;
    /**
     * Current cycle in decoding the addressing mode.
     */
              byte           decodeCycle = 0;
    /**
     * Functor for addressing mode decoding.
     */
    protected AddressingMode decoding    = null;
    /**
     * Denotes if current one byte instruction is in implied or accumulator mode.
     */
    protected boolean        isAccu      = false;

    /**
     * CPU cycles counter.
     */
    protected long  cycles  = 0;
    /**
     * Current opcode.
     */
    protected byte  opcode  = 0x00;
    /**
     * Program counter value of last opcode.
     */
    protected short lastPos = 0x0000;
    /**
     * Current instruction operand.
     */
    protected byte  operand = 0x00;
    /**
     * Calculated effective address.
     */
    protected short ea      = 0x0000;
    /**
     * Temporary pointer.
     * <br>Used in indexed zeropage addressing.
     */
    protected short pointer = 0x0000;
    /**
     * Branch offset.
     * <br>Used in relative addressing.
     */
    protected byte  offset  = 0x00;

    /**
     * Functor for handling the output data during the halfcycle.
     */
    protected Consumer<Byte> halfCycleOut = null;
    /**
     * Functor for handing the input data during the halfcycle.
     */
    protected Supplier<Byte> halfCycleIn  = null;
    /**
     * Data used in last write cycle.
     * <br>Saved in case of gate in signal transition.
     */
    protected Byte           lastData     = null;

    /**
     * Reads the memory and discards the result.
     * @param address the memory address
     */
    protected void read(short address) {
        read(address, null);
    }

    /**
     * Reads the memory
     * @param address the memory address
     * @param readCycle functor for received data
     */
    protected void read(short address, Consumer<Byte> readCycle) {
        if (!halt && (rdy.level() == Pin.Level.LOW))
            halt = true;

        if (Logger.ENABLE)
            Logger.info(String.format("%s from 0x%04X", readCycle == null ? "Dummy read" : "Read", address));

        halfCycleOut = readCycle;
        if (address == IO_DIR_VECT) {
            halfCycleIn = () -> (byte) port.dirValue();
        } else if (address == IO_VECT) {
            halfCycleIn = () -> (byte) port.value();
        } else {
            rw.level(Pin.Level.HIGH);
            data.direction(Pin.Direction.INPUT);
            this.address.value(address);
            halfCycleIn = () -> (byte) (data.value() & 0xFF);
        }
    }

    /**
     * Writes to the memory.
     * @param address the memory address
     * @param value value to write
     */
    protected void write(short address, byte value) {
        if (Logger.ENABLE)
            Logger.info(String.format("Write 0x%02X to memory at 0x%04X", value, address));

        if (address == IO_DIR_VECT) {
            port.direction(value);
        } else if (address == IO_VECT) {
            port.value(value);
        } else if (aec.level() == Pin.Level.HIGH) {
            rw.level(Pin.Level.LOW);
            data.direction(Pin.Direction.OUTPUT);
            this.address.value(address);
            halfCycleIn = () -> {
                lastData = value;
                return value;
            };
            halfCycleOut = this.data::value;
        }
    }

    /**
     * Pulls the value from the stack and discards it.
     */
    private void pull() {
        pull(null);
    }

    /**
     * Pulls the value from the stack.
     * @param operation functor for received data
     */
    private void pull(Consumer<Byte> operation) {
        read((short) (STACK_VECT + (SP & 0xFF)), operation);
    }

    /**
     * Pushes the value onto the stack.
     * <br>Also decrements the stack pointer, because after all push operations the SP need to be decremented anyway.
     * @param value the value to push
     */
    private void push(byte value) {
        write((short) (STACK_VECT + (SP & 0xFF)), value);
        SP--; // all push operations require SP decrementation
    }

    /**
     * Mnemonics for all of 256 CPU instructions.
     */
    protected static final String[] mnemonic = {
        "BRK", "ORA", "JAM", "SLO", "NOP", "ORA", "ASL", "SLO", "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO",
        "BPL", "ORA", "JAM", "SLO", "NOP", "ORA", "ASL", "SLO", "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO",
        "JSR", "AND", "JAM", "RLA", "BIT", "AND", "ROL", "RLA", "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA",
        "BMI", "AND", "JAM", "RLA", "NOP", "AND", "ROL", "RLA", "SEC", "AND", "NOP", "RLA", "NOP", "AND", "ROL", "RLA",
        "RTI", "EOR", "JAM", "SRE", "NOP", "EOR", "LSR", "SRE", "PHA", "EOR", "LSR", "ASR", "JMP", "EOR", "LSR", "SRE",
        "BVC", "EOR", "JAM", "SRE", "NOP", "EOR", "LSR", "SRE", "CLI", "EOR", "NOP", "SRE", "NOP", "EOR", "LSR", "SRE",
        "RTS", "ADC", "JAM", "RRA", "NOP", "ADC", "ROR", "RRA", "PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA",
        "BVS", "ADC", "JAM", "RRA", "NOP", "ADC", "ROR", "RRA", "SEI", "ADC", "NOP", "RRA", "NOP", "ADC", "ROR", "RRA",
        "NOP", "STA", "NOP", "SAX", "STY", "STA", "STX", "SAX", "DEY", "NOP", "TXA", "ANE", "STY", "STA", "STX", "SAX",
        "BCC", "STA", "JAM", "SHA", "STY", "STA", "STX", "SAX", "TYA", "STA", "TXS", "SHS", "SHY", "STA", "SHX", "SHA",
        "LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX", "TAY", "LDA", "TAX", "LXA", "LDY", "LDA", "LDX", "LAX",
        "BCS", "LDA", "JAM", "LAX", "LDY", "LDA", "LDX", "LAX", "CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX",
        "CPY", "CMP", "NOP", "DCP", "CPY", "CMP", "DEC", "DCP", "INY", "CMP", "DEX", "SBX", "CPY", "CMP", "DEC", "DCP",
        "BNE", "CMP", "JAM", "DCP", "NOP", "CMP", "DEC", "DCP", "CLD", "CMP", "NOP", "DCP", "NOP", "CMP", "DEC", "DCP",
        "CPX", "SBC", "NOP", "ISB", "CPX", "SBC", "INC", "ISB", "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISB",
        "BEQ", "SBC", "JAM", "ISB", "NOP", "SBC", "INC", "ISB", "SED", "SBC", "NOP", "ISB", "NOP", "SBC", "INC", "ISB"
    };

    /**
     * List of undocumented instructions.
     * <br>In most cases if CPU wants to execute any of them, then probably the CPU doesn't behave as it should.
     */
    protected static final List<String> undocumented = Arrays.asList(
        "ANC", "ANE", "ARR", "ASR", "DCP", "ISB", "LAS", "LAX", "LXA", "RLA", "RRA", "SAX", "SBX", "SHA", "SHS", "SHX",
        "SHY", "SLO", "SRE"
    );

    /**
     * Dictates if the use of undocumented instruction will cause exception.
     */
    protected static boolean useUndocumented = false;

    /**
     * The addressing modes.
     *
     * @see <a href="http://archive.6502.org/books/mcs6500_family_programming_manual.pdf">
     *     MCS6500 Microcomputer Family Programming Manual, 1976</a>
     * @see <a href="http://www.zimmers.net/anonftp/pub/cbm/documents/chipdata/64doc">
     *     Detailed information on addressing modes</a>
     * @see <a href="http://www.unusedino.de/ec64/technical/aay/c64/bmain.htm">
     *     Another site about addressing modes, including the undocumented ones</a>
     */
    private class Addressing {
        /**
         * Flag if effective address crosses the memory page.
         */
        private boolean carry = false;
        /**
         * Temporary address.
         */
        private short temp = 0x0000;

        /**
         * Fetch opcode, increment PC.
         * <br><b>Every addressing mode starts with this step.</b>
         */
        void fetchOp() {
            read(PC++, data -> opcode = data);
            stage = Stage.FETCH;
        }

        /**
         * Implied addressing [2 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away)</li>
         * </ol>
         * <br>Used by: CLC, CLD, CLI, CLV, DEX, DEY, INX, INY, JAM, NOP, SEC, SED, SEI, TAX, TAY, TCD, TCS,
         *  TDC, TSC, TSX, TXA, TXS, TXY, TYA, TYX, XCE
         */
        void imp() {
            read(PC);
            stage = Stage.EXECUTE;
        }

        /**
         * Accumulator addressing [2 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away)</li>
         * </ol>
         * <br>Used by: ASL, DEC, INC, LSR, ROL, ROR
         */
        void acc() {
            read(PC);
            stage = Stage.EXECUTE;
            isAccu = true;
        }

        /**
         * Immediate addressing [2 cycles].
         * <br><ol start="2">
         *     <li>Fetch value, increment PC</li>
         * </ol>
         * <br>Used by: ADC, ANC, AND, ANE, ARR, ASR, BIT, CMP, CPX, CPY, EOR, LDA, LDX, LDY, LXA, NOP, ORA,
         *  REP, SBC, SBX, SEP
         */
        void imm() {
            read(PC++, (data) -> operand = data);
            stage = Stage.EXECUTE;
        }

        /**
         * Absolute addressing [3 cycles].
         * <br><ol start="2">
         *     <li>Fetch low address byte, increment PC</li>
         *     <li>Copy low address byte to PCL, fetch high address byte to PCH</li>
         * </ol>
         * <br>Used by: JMP
         */
        void absJMP() {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else {
                read(PC, data -> ea |= data << 8);
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Absolute addressing (Read/Write) [4 cycles].
         * <br><ol start="2">
         *     <li>Fetch low address byte, increment PC</li>
         *     <li>Fetch high address byte, increment PC</li>
         *     <li>Read from/write to the effective address</li>
         * </ol>
         * @param read {@code true} if the associated instruction reads the memory
         * @see #absR_()
         * @see #abs_W()
         */
        private void absR_W(boolean read) {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(PC++, data -> ea |= data << 8);
            } else {
                if (read) read(ea, data -> operand = data);
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Absolute addressing (Read) [4 cycles].
         * <br><br>Used by: LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP
         * @see #absR_W(boolean)
         */
        void absR_() { absR_W(true);  }
        /**
         * Absolute addressing (Write) [4 cycles].
         * <br><br>Used by: STA, STX, STY, SAX
         * @see #absR_W(boolean)
         */
        void abs_W() { absR_W(false); }

        /**
         * Absolute addressing (Read-Modify-Write) [6 cycles].
         * <br><ol start="2">
         *     <li>Fetch low address byte, increment PC</li>
         *     <li>Fetch high address byte, increment PC</li>
         *     <li>Read from the effective address</li>
         *     <li>Write the value back to the effective address</li>
         *     <li>Write the new value to the effective address</li>
         * </ol>
         * <br>Used by: ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP
         */
        void absRW() {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(PC++, data -> ea |= data << 8);
            } else if (decodeCycle == 4) {
                read(ea, data -> operand = data);
            } else if (decodeCycle == 5) {
                write(ea, operand);
            } else if (decodeCycle == 6) {
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Zeropage addressing (Read/Write) [3 cycles].
         * <br><ol start="2">
         *     <li>Fetch address, increment PC</li>
         *     <li>Read from/write to the effective address</li>
         * </ol>
         * @param read {@code true} if the associated instruction reads the memory
         * @see #zpgR_()
         * @see #zpg_W()
         */
        private void zpgR_W(boolean read) {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else {
                if (read) read(ea, data -> operand = data);
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Zeropage addressing (Read) [3 cycles].
         * <br><br>Used by: LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP
         * @see #zpgR_W(boolean)
         */
        void zpgR_() { zpgR_W(true);  }
        /**
         * Zeropage addressing (Write) [3 cycles].
         * <br><br>Used by: STA, STX, STY, SAX
         * @see #zpgR_W(boolean)
         */
        void zpg_W() { zpgR_W(false); }

        /**
         * Zeropage addressing (Read-Modify-Write) [5 cycles].
         * <br><ol start="2">
         *     <li>Fetch address, increment PC</li>
         *     <li>Read from the effective address</li>
         *     <li>Write the value back to the effective address</li>
         *     <li>Write the new value to the effective address</li>
         * </ol>
         * <br>Used by: ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP
         */
        void zpgRW() {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(ea, data -> operand = data);
            } else if (decodeCycle == 4) {
                write(ea, operand);
            } else {
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Zeropage indexed addressing (Read/Write) [4 cycles].
         * <br><ol start="2">
         *     <li>Fetch address, increment PC</li>
         *     <li>Read from the address</li>
         *     <li>Read from/write to the effective address (page boundary crossings are not handled)</li>
         * </ol>
         * @param reg the indexing register
         * @param read {@code true} if the associated instruction reads the memory
         * @see #zpxR_()
         * @see #zpx_W()
         * @see #zpyR_()
         * @see #zpy_W()
         */
        private void zpiR_W(byte reg, boolean read) {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(ea);
            } else {
                ea = (short) ((ea + (reg & 0xFF)) & 0xFF);
                if (read) read(ea, data -> operand = data);
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Zeropage X-indexed addressing (Read) [4 cycles].
         * <br><br>Used by: LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP
         * @see #zpiR_W(byte, boolean)
         */
        void zpxR_() { zpiR_W(XR, true);  }
        /**
         * Zeropage X-indexed addressing (Write) [4 cycles].
         * <br><br>Used by: STA, STX, STY, SAX
         * @see #zpiR_W(byte, boolean)
         */
        void zpx_W() { zpiR_W(XR, false); }
        /**
         * Zeropage Y-indexed addressing (Read) [4 cycles].
         * <br><br>Used by: LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, NOP
         * @see #zpiR_W(byte, boolean)
         */
        void zpyR_() { zpiR_W(YR, true);  }
        /**
         * Zeropage Y-indexed addressing (Write) [4 cycles].
         * <br><br>Used by: STA, STX, STY, SAX
         * @see #zpiR_W(byte, boolean)
         */
        void zpy_W() { zpiR_W(YR, false); }

        /**
         * Zeropage indexed addressing (Read-Modify-Write) [6 cycles].
         * <br><ol start="2">
         *     <li>Fetch address, increment PC</li>
         *     <li>Read from the address</li>
         *     <li>Add index register X to the address and read from it (page boundary crossings are not handled)</li>
         *     <li>Write the value back to the effective address</li>
         *     <li>Write the new value to the effective address</li>
         * </ol>
         * @param reg the indexing register
         * @see #zpxRW()
         * @see #zpyRW()
         */
        private void zpiRW(byte reg) {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(ea);
            } else if (decodeCycle == 4) {
                ea = (short) ((ea + (reg & 0xFF)) & 0xFF);
                read(ea, data -> operand = data);
            } else if (decodeCycle == 5) {
                write(ea, operand);
            } else {
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Zeropage X-indexed addressing (Read-Modify-Write) [6 cycles].
         * <br><br>Used by: ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP
         * @see #zpiRW(byte)
         */
        void zpxRW() { zpiRW(XR); }
        /**
         * Zeropage Y-indexed addressing (Read-Modify-Write) [6 cycles].
         * @see #zpiRW(byte)
         */
        void zpyRW() { zpiRW(YR); }

        /**
         * Absolute indexed addressing (Read/Write) [4-5 cycles].
         * <br><ol start="2">
         *     <li>Fetch low address byte, increment PC</li>
         *     <li>Fetch high address byte, increment PC</li>
         *     <li>Read from the effective address, add index register to low address byte</li>
         *     <li><i>[if page boundary was crossed]</i> Re-read from / write to the effective address</li>
         * </ol>
         * @param reg the indexing register
         * @param read {@code true} if the associated instruction reads the memory
         * @see #abxR_()
         * @see #abx_W()
         * @see #abyR_()
         * @see #aby_W()
         */
        private void abxR_W(byte reg, boolean read) {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(PC++, data -> ea |= data << 8);
            } else if (decodeCycle == 4) {
                temp = (short) (ea + (reg & 0xFF));
                ea = (short) ((ea & 0xFF00) | ((ea + (reg & 0xFF)) & 0x00FF));
                carry = temp != ea;

                read(ea, data -> operand = data);
                if (!carry) // high byte doesn't need fixing of effective address
                    stage = Stage.EXECUTE;
            } else {
                ea = temp;
                if (read) read(ea, data -> operand = data);
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Absolute X-indexed addressing (Read) [4-5 cycles].
         * <br><br>Used by: LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, LAE, SHS, NOP
         * @see #abxR_W(byte, boolean)
         */
        void abxR_() { abxR_W(XR, true);  }
        /**
         * Absolute X-indexed addressing (Write) [4-5 cycles].
         * <br><br>Used by: STA, STX, STY, SHA, SHX, SHY
         * @see #abxR_W(byte, boolean)
         */
        void abx_W() { abxR_W(XR, false); }
        /**
         * Absolute Y-indexed addressing (Read) [4-5 cycles].
         * <br><br>Used by: LDA, LDX, LDY, EOR, AND, ORA, ADC, SBC, CMP, BIT, LAX, LAE, SHS, NOP
         * @see #abxR_W(byte, boolean)
         */
        void abyR_() { abxR_W(YR, true);  }
        /**
         * Absolute Y-indexed addressing (Write) [4-5 cycles].
         * <br><br>Used by: STA, STX, STY, SHA, SHX, SHY
         * @see #abxR_W(byte, boolean)
         */
        void aby_W() { abxR_W(YR, false); }

        /**
         * Absolute indexed addressing (Read-Modify-Write) [7 cycles].
         * <br><ol start="2">
         *     <li>Fetch low address byte, increment PC</li>
         *     <li>Fetch high address byte, increment PC</li>
         *     <li>Read from the effective address, add index register X to low address byte</li>
         *     <li>Re-read from the effective address, fix the high byte of the effective address (if needed)</li>
         *     <li>Write the value back to the effective address</li>
         *     <li>Write the new value to the effective address</li>
         * </ol>
         * @param reg the indexing register
         * @see #abxRW()
         * @see #abyRW()
         */
        private void abiRW(byte reg) {
            if (decodeCycle == 2) {
                read(PC++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(PC++, data -> ea |= data << 8);
            } else if (decodeCycle == 4) {
                temp = (short) (ea + (reg & 0xFF));
                ea = (short) ((ea & 0xFF00) | (temp & 0x00FF));
                carry = temp != ea;
                read(ea, data -> operand = data);
            } else if (decodeCycle == 5) {
                if (carry)
                    ea = temp;
                read(ea, data -> operand = data);
            } else if (decodeCycle == 6) {
                write(ea, operand);
            } else {
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Absolute X-indexed addressing (Read-Modify-Write) [7 cycles].
         * <br><br>Used by: ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP
         * @see #abiRW(byte)
         */
        void abxRW() { abiRW(XR); }
        /**
         * Absolute Y-indexed addressing (Read-Modify-Write) [7 cycles].
         * <br><br>Used by: ASL, LSR, ROL, ROR, INC, DEC, SLO, SRE, RLA, RRA, ISB, DCP
         * @see #abiRW(byte)
         */
        void abyRW() { abiRW(YR); }

        /**
         * Relative addressing [3-5 cycles].
         * <br><ol start="2">
         *     <li>Fetch operand, increment PC</li>
         *     <li>Fetch opcode of the next instruction and throw it away,
         *      if branch is taken then add operand to PCL</li>
         *     <li><i>[if branch taken]</i> Fetch opcode of the next instruction and throw it away</li>
         *     <li><i>[if memory page was crossed]</i> Fetch opcode of the next instruction, increment PC</li>
         * </ol>
         * <br>Used by: BCC, BCS, BNE, BEQ, BPL, BMI, BVC, BVS
         */
        void rel() {
            if (decodeCycle == 2) {
                read(PC++, data -> operand = data);
            } else if (decodeCycle == 3) {
                read(PC);
                ops.get(mnemonic[opcode & 0xFF]).execute();
                if (offset == 0) { // branch not taken
                    maskIRQ = true; // sort of bug of the cpu, which causes IRQ to be missed if it occurred on skipped branch
                    stage = Stage.OPCODE;
                }  else { // if branch is taken, add operand to PCL
                    temp = (short) (PC + offset);
                    PC = (short) ((PC & 0xFF00) | (temp & 0x00FF));
                    carry = temp != PC;
                }
            } else if (decodeCycle == 4) {
                read(PC);
                if (!carry)
                    stage = Stage.OPCODE;
            } else {
                PC = temp;
                stage = Stage.OPCODE;
            }
        }

        /**
         * Indexed indirect addressing (Read/Write) [6 cycles].
         * <br><ol start="2">
         *     <li>Fetch pointer address, increment PC</li>
         *     <li>Read from the address, add the X registry to it (page boundary crossings are not handled)</li>
         *     <li>Fetch low byte of the effective address</li>
         *     <li>Fetch high byte of the effective address</li>
         *     <li>Read from / write to the effective address</li>
         * </ol>
         * @param read {@code true} if the associated instruction reads the memory
         * @see #izxR_()
         * @see #izx_W()
         */
        private void izxR_W(boolean read) {
            if (decodeCycle == 2) {
                read(PC++, data -> operand = data);
            } else if (decodeCycle == 3) {
                read(operand);
                pointer = (short) ((operand + (XR & 0xFF)) & 0xFF);
            } else if (decodeCycle == 4) {
                read(pointer, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 5) {
                read((byte) (pointer + 1), data -> ea |= data << 8);
            } else {
                if (read) read(ea, data -> operand = data);
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Indexed indirect addressing (Read) [6 cycles].
         * <br><br>Used by: LDA, ORA, EOR, AND, ADC, CMP, SBC, LAX
         * @see #izxR_W(boolean)
         */
        void izxR_() { izxR_W(true);  }
        /**
         * Indexed indirect addressing (Write) [6 cycles].
         * <br><br>Used by: STA, SAX
         * @see #izxR_W(boolean)
         */
        void izx_W() { izxR_W(false); }

        /**
         * Indirect indexed addressing (Read/Write) [5-6 cycles].
         * <br><ol start="2">
         *     <li>Fetch pointer address, increment PC</li>
         *     <li>Fetch low byte of the effective address</li>
         *     <li>Fetch high byte of the effective address (page boundary crossing is not handled)</li>
         *     <li>Read from the effective address, add Y to low byte of the effective address</li>
         *     <li><i>[if memory page was crossed]</i> Read from / write to the effective address</li>
         * </ol>
         * @param read {@code true} if the associated instruction reads the memory
         * @see #izyR_()
         * @see #izy_W()
         */
        private void izyR_W(boolean read) {
            if (decodeCycle == 2) {
                read(PC++, data -> pointer = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(pointer, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 4) {
                read((byte) (pointer + 1), data -> ea |= data << 8);
            } else if (decodeCycle == 5) {
                temp = (short) (ea + (YR & 0xFF));
                ea = (short) ((ea & 0xFF00) | ((ea + (YR & 0xFF)) & 0x00FF));
                carry = temp != ea;

                read(ea, data -> operand = data);
                if (!carry) // the high byte of effective address doesn't need fixing
                    stage = Stage.EXECUTE;
            } else {
                ea = temp;
                if (read) read(ea, data -> operand = data);
                stage = Stage.EXECUTE;
            }
        }
        /**
         * Indirect indexed addressing (Read) [5-6 cycles].
         * <br><br>Used by: LDA, EOR, AND, ORA, ADC, SBC, CMP
         * @see #izyR_W(boolean)
         */
        void izyR_() { izyR_W(true);  }
        /**
         * Indirect indexed addressing (Write) [5-6 cycles].
         * <br><br>Used by: STA, SHA
         * @see #izyR_W(boolean)
         */
        void izy_W() { izyR_W(false); }

        /**
         * Absolute indirect addressing [5 cycles].
         * <br><ol start="2">
         *     <li>Fetch low byte of the pointer address, increment PC</li>
         *     <li>Fetch high byte of the pointer address, increment PC</li>
         *     <li>Fetch low address byte to latch</li>
         *     <li>Fetch PCH, copy latch to PCL (page boundary crossing is not handled)</li>
         * </ol>
         * <br>Used by: JMP
         */
        void indJMP() {
            if (decodeCycle == 2) {
                read(PC++, data -> pointer = (short) (data & 0xFF));
            } else if (decodeCycle == 3) {
                read(PC++, data -> pointer |= data << 8);
            } else if (decodeCycle == 4) {
                read(pointer, data -> ea = (short) (data & 0xFF));
            } else {
                read((short) ((pointer & 0xFF00) | ((pointer + 1) & 0x00FF)), data -> ea |= data << 8);
                stage = Stage.EXECUTE;
            }
        }

        /**
         * BRK addressing [7 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away), increment PC</li>
         *     <li>Push PCH on stack, decrement SP</li>
         *     <li>Push PCL on stack, decrement SP</li>
         *     <li>Push SR on stack (with B flag set), decrement SP,
         *      if IRQ occurred between 1 and 4 cycle of BRK it morphs into IRQ</li>
         *     <li>Fetch PCL</li>
         *     <li>Fetch PCH</li>
         * </ol>
         */
        void stkBRK() {
            if (decodeCycle == 2) {
                read(PC++);
            } else if (decodeCycle == 3) {
                push((byte) (PC >> 8));
            } else if (decodeCycle == 4) {
                push((byte) (PC & 0x00FF));
            } else if (decodeCycle == 5) {
                if (irqPending && status.irq() == 0) {
                    irqPending = false;
                    decoding = this::irq;
                    decoding.decode();
                    return;
                }

                push((byte) (SR | Status.Bit.O | Status.Bit.B));
            } else if (decodeCycle == 6) {
                read(IRQ_VECT, data -> PC = (short) ((PC & 0xFF00) | (data & 0xFF)));
            } else {
                status.irq(true);
                read((short) (IRQ_VECT + 1), data -> PC = (short) ((data << 8) | (PC & 0x00FF)));
                stage = Stage.EXECUTE;
            }
        }

        /**
         * RTI addressing [6 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away)</li>
         *     <li>Increment S</li>
         *     <li>Pull SR from stack, increment SP</li>
         *     <li>Pull PCL from stack, increment SP</li>
         *     <li>Pull PCH from stack</li>
         * </ol>
         */
        void stkRTI() {
            if (decodeCycle == 2) {
                read(PC);
            } else if (decodeCycle == 3) {
                SP++;
            } else if (decodeCycle == 4) {
                pull(data -> {
                    SR = (byte) (data | Status.Bit.O);
                    SP++;
                });
            } else if (decodeCycle == 5) {
                pull(data -> {
                    PC = (short) ((PC & 0xFF00) | (data & 0xFF));
                    SP++;
                });
            } else {
                pull(data -> PC = (short) ((data << 8) | (PC & 0x00FF)));
                stage = Stage.EXECUTE;
            }
        }

        /**
         * RTS addressing [6 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away)</li>
         *     <li>Increment SP</li>
         *     <li>Pull PCL from stack, increment SP</li>
         *     <li>Pull PCH from stack</li>
         *     <li>Increment PC</li>
         * </ol>
         */
        void stkRTS() {
            if (decodeCycle == 2) {
                read(PC);
            } else if (decodeCycle == 3) {
                SP++;
            } else if (decodeCycle == 4) {
                pull(data -> {
                    PC = (short) ((PC & 0xFF00) | (data & 0xFF));
                    SP++;
                });
            } else if (decodeCycle == 5) {
                pull(data -> PC = (short) ((data << 8) | (PC & 0x00FF)));
            } else {
                PC++;
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Push stack addressing [3 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away)</li>
         *     <li>Push register on stack, decrement SP</li>
         * </ol>
         * <br>Used by: PHA, PHP
         */
        void stk_PH() {
            if (decodeCycle == 2) {
                read(PC);
            } else {
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Pull stack addressing [4 cycles].
         * <br><ol start="2">
         *     <li>Read next instruction byte (and throw it away)</li>
         *     <li>Increment SP</li>
         *     <li>Pull register from stack</li>
         * </ol>
         * <br>Used by: PLA, PLP
         */
        void stk_PL() {
            if (decodeCycle == 2) {
                read(PC);
            } else if (decodeCycle == 3) {
                SP++;
            } else {
                stage = Stage.EXECUTE;
            }
        }

        /**
         * JSR addressing [6 cycles].
         * <br><ol start="2">
         *     <li>Fetch low address byte, increment PC</li>
         *     <li>Internal operation <i>(predecrement SP)</i></li>
         *     <li>Push PCH on stack, decrement SP</li>
         *     <li>Push PCL on stack, decrement SP</li>
         *     <li>Copy low address byte to PCL, fetch high address byte to PCH</li>
         * </ol>
         */
        void stkJSR() {
            if (decodeCycle == 2) {
                read(PC++, data -> operand = data);
            } else if (decodeCycle == 3) {
                pull();
            } else if (decodeCycle == 4) {
                push((byte) (PC >> 8));
            } else if (decodeCycle == 5) {
                push((byte) (PC & 0x00FF));
            } else if (decodeCycle == 6) {
                read(PC, data -> PC = (short) ((data << 8) | (operand & 0xFF)));
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Illegal indexed indirect addressing (Read-Modify-Write) [8 cycles].
         * <br><ol start="2">
         *     <li>Fetch pointer address, increment PC</li>
         *     <li>Dummy cycle, read the same data</li>
         *     <li>Add the X registry to the pointer, fetch low byte of the effective address
         *      (page boundary crossings are not handled)</li>
         *     <li>Fetch high byte of the effective address</li>
         *     <li>Read from the effective address</li>
         *     <li>Write the value back to the effective address</li>
         *     <li>Write the new value to the effective address</li>
         * </ol>
         * <br>Used by: SLO, SRE, RLA, RRA, ISB, DCP
         */
        void izxILL() {
            if (decodeCycle == 2) {
                read(PC++, data -> operand = data);
            } else if (decodeCycle == 3) {
                read(PC);
            } else if (decodeCycle == 4) {
                pointer = (short) ((operand + (XR & 0xFF)) & 0xFF);
                read(pointer, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 5) {
                read((byte) (pointer + 1), data -> ea |= data << 8);
            } else if (decodeCycle == 6) {
                read(ea, data -> operand = data);
            } else if (decodeCycle == 7) {
                write(ea, operand);
            } else {
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Illegal indirect indexed addressing (Read-Modify-Write) [8 cycles].
         * <br><ol start="2">
         *     <li>Fetch pointer address, increment PC</li>
         *     <li>Fetch low byte of effective address</li>
         *     <li>Fetch high byte of effective address</li>
         *     <li>Add the Y registry to the effective address</li>
         *     <li>Read from the effective address</li>
         *     <li>Write the value back to the effective address</li>
         *     <li>Write the new value to the effective address</li>
         * </ol>
         * <br>Used by: SLO, SRE, RLA, RRA, ISB, DCP
         */
        void izyILL() {
            if (decodeCycle == 2) {
                read(PC++, data -> operand = data);
            } else if (decodeCycle == 3) {
                read(operand++, data -> ea = (short) (data & 0xFF));
            } else if (decodeCycle == 4) {
                read(operand, data -> ea |= data << 8);
            } else if (decodeCycle == 5) {
                ea += YR;
                read(ea);
            } else if (decodeCycle == 6) {
                read(ea, data -> operand = data);
            } else if (decodeCycle == 7) {
                write(ea, operand);
            } else {
                stage = Stage.EXECUTE;
            }
        }

        /**
         * Reset sequence [7 cycles].
         * <br><ol start="2">
         *     <li>Dummy read from PC + 1</li>
         *     <li>Dummy read from 0x0100 + SP</li>
         *     <li>Dummy read from 0x0100 + SP - 1</li>
         *     <li>Dummy read from 0x0100 + SP - 2</li>
         *     <li>Fetch PCL from RESET vector</li>
         *     <li>Fetch PCH from RESET vector + 1</li>
         * </ol>
         */
        void reset() {
            if (decodeCycle == 2) {
                read((short) (PC + 1));
            } else if (decodeCycle == 3) {
                read((short) (0x0100 + (SP & 0xFF)));
            } else if (decodeCycle == 4) {
                read((short) (0x0100 + ((SP - 1) & 0xFF)));
            } else if (decodeCycle == 5) {
                read((short) (0x0100 + ((SP - 2) & 0xFF)));
            } else if (decodeCycle == 6) {
                read(RESET_VECT, data -> PC = (short) ((PC & 0xFF00) | data));
            } else {
                read((short) ((RESET_VECT & 0xFFFF) + 1), data -> PC = (short) ((data << 8) | (PC & 0x00FF)));
                stage = Stage.OPCODE;
            }
        }

        /**
         * Hardware interrupt sequence [7 cycles].
         * <br><ol start="2">
         *     <li>Dummy read from PC</li>
         *     <li>Push PCH on stack, decrement SP</li>
         *     <li>Push PCL on stack, decrement SP</li>
         *     <li>Push SR on stack (with B flag cleared), decrement SP</li>
         *     <li>Fetch new PCL from IRQ vector</li>
         *     <li>Fetch new PCH from IRQ vector + 1, set I flag</li>
         * </ol>
         */
        void irq() {
            if (decodeCycle == 2) {
                read(PC);
            } else if (decodeCycle == 3) {
                push((byte) (PC >> 8));
            } else if (decodeCycle == 4) {
                push((byte) (PC & 0xFF));
            } else if (decodeCycle == 5) {
                push((byte) (SR & ~Status.Bit.B));
            } else if (decodeCycle == 6) {
                read(IRQ_VECT, data -> PC = (short) ((PC & 0xFF00) | data));
            } else {
                read((short) ((IRQ_VECT & 0xFFFF) + 1), data -> PC = (short) ((data << 8) | (PC & 0x00FF)));
                status.irq(true);
                stage = Stage.OPCODE;
            }
        }
    }

    /**
     * Result of the latest operation.
     */
    private short result = 0x0000;
    /**
     * A singleton of addressing mode decoding stages.
     */
    protected final Addressing addr = new Addressing();
    /**
     * Addressing modes list.
     */
    protected final AddressingMode[] modes = new AddressingMode[] {
/*0x00*/addr::stkBRK, addr::izxR_, addr::imp, addr::izxILL, addr::zpgR_ , addr::zpgR_, addr::zpgRW, addr::zpgRW,
/*0x08*/addr::stk_PH, addr::imm  , addr::acc, addr::imm   , addr::absR_ , addr::absR_, addr::absRW, addr::absRW,
/*0x10*/addr::rel   , addr::izyR_, addr::imp, addr::izyILL, addr::zpxR_ , addr::zpxR_, addr::zpxRW, addr::zpxRW,
/*0x18*/addr::imp   , addr::abyR_, addr::imp, addr::abyRW , addr::abxR_ , addr::abxR_, addr::abxRW, addr::abxRW,
/*0x20*/addr::stkJSR, addr::izxR_, addr::imp, addr::izxILL, addr::zpgR_ , addr::zpgR_, addr::zpgRW, addr::zpgRW,
/*0x28*/addr::stk_PL, addr::imm  , addr::acc, addr::imm   , addr::absR_ , addr::absR_, addr::absRW, addr::absRW,
/*0x30*/addr::rel   , addr::izyR_, addr::imp, addr::izyILL, addr::zpxR_ , addr::zpxR_, addr::zpxRW, addr::zpxRW,
/*0x38*/addr::imp   , addr::abyR_, addr::imp, addr::abyRW , addr::abxR_ , addr::abxR_, addr::abxRW, addr::abxRW,
/*0x40*/addr::stkRTI, addr::izxR_, addr::imp, addr::izxILL, addr::zpgR_ , addr::zpgR_, addr::zpgRW, addr::zpgRW,
/*0x48*/addr::stk_PH, addr::imm  , addr::acc, addr::imm   , addr::absJMP, addr::absR_, addr::absRW, addr::absRW,
/*0x50*/addr::rel   , addr::izyR_, addr::imp, addr::izyILL, addr::zpxR_ , addr::zpxR_, addr::zpxRW, addr::zpxRW,
/*0x58*/addr::imp   , addr::abyR_, addr::imp, addr::abyRW , addr::abxR_ , addr::abxR_, addr::abxRW, addr::abxRW,
/*0x60*/addr::stkRTS, addr::izxR_, addr::imp, addr::izxILL, addr::zpgR_ , addr::zpgR_, addr::zpgRW, addr::zpgRW,
/*0x68*/addr::stk_PL, addr::imm  , addr::acc, addr::imm   , addr::indJMP, addr::absR_, addr::absRW, addr::absRW,
/*0x70*/addr::rel   , addr::izyR_, addr::imp, addr::izyILL, addr::zpxR_ , addr::zpxR_, addr::zpxRW, addr::zpxRW,
/*0x78*/addr::imp   , addr::abyR_, addr::imp, addr::abyRW , addr::abxR_ , addr::abxR_, addr::abxRW, addr::abxRW,
/*0x80*/addr::imm   , addr::izx_W, addr::imm, addr::izx_W , addr::zpg_W , addr::zpg_W, addr::zpg_W, addr::zpg_W,
/*0x88*/addr::imp   , addr::imm  , addr::imp, addr::imm   , addr::abs_W , addr::abs_W, addr::abs_W, addr::abs_W,
/*0x90*/addr::rel   , addr::izy_W, addr::imp, addr::izy_W , addr::zpx_W , addr::zpx_W, addr::zpy_W, addr::zpy_W,
/*0x98*/addr::imp   , addr::aby_W, addr::imp, addr::aby_W , addr::abx_W , addr::abx_W, addr::aby_W, addr::aby_W,
/*0xA0*/addr::imm   , addr::izxR_, addr::imm, addr::izxR_ , addr::zpgR_ , addr::zpgR_, addr::zpgR_, addr::zpgR_,
/*0xA8*/addr::imp   , addr::imm  , addr::imp, addr::imm   , addr::absR_ , addr::absR_, addr::absR_, addr::absR_,
/*0xB0*/addr::rel   , addr::izyR_, addr::imp, addr::izyR_ , addr::zpxR_ , addr::zpxR_, addr::zpyR_, addr::zpyR_,
/*0xB8*/addr::imp   , addr::abyR_, addr::imp, addr::abyR_ , addr::abxR_ , addr::abxR_, addr::abyR_, addr::abyR_,
/*0xC0*/addr::imm   , addr::izxR_, addr::imm, addr::izxILL, addr::zpgR_ , addr::zpgR_, addr::zpgRW, addr::zpgRW,
/*0xC8*/addr::imp   , addr::imm  , addr::imp, addr::imm   , addr::absR_ , addr::absR_, addr::absRW, addr::absRW,
/*0xD0*/addr::rel   , addr::izyR_, addr::imp, addr::izyILL, addr::zpxR_ , addr::zpxR_, addr::zpxRW, addr::zpxRW,
/*0xD8*/addr::imp   , addr::abyR_, addr::imp, addr::abyRW , addr::abxR_ , addr::abxR_, addr::abxRW, addr::abxRW,
/*0xE0*/addr::imm   , addr::izxR_, addr::imm, addr::izxILL, addr::zpgR_ , addr::zpgR_, addr::zpgRW, addr::zpgRW,
/*0xE8*/addr::imp   , addr::imm  , addr::imp, addr::imm   , addr::absR_ , addr::absR_, addr::absRW, addr::absRW,
/*0xF0*/addr::rel   , addr::izyR_, addr::imp, addr::izyILL, addr::zpxR_ , addr::zpxR_, addr::zpxRW, addr::zpxRW,
/*0xF8*/addr::imp   , addr::abyR_, addr::imp, addr::abyRW , addr::abxR_ , addr::abxR_, addr::abxRW, addr::abxRW
    };

    /**
     * Writes to memory or accumulator depending on value of {@link #isAccu}.
     */
    private void accuOrMem() {
        if (isAccu)
            AC = (byte) result;
        else
            write(ea, (byte) result);
        isAccu = false;
    }

    /**
     * Compares the memory with given register.
     * @param reg the register to compare
     */
    private void opCMP(byte reg) {
        result = (short) (reg - operand);
        status.determineNZ(result);
        status.carry((reg & 0xFF) >= (operand & 0xFF));
    }

    /**
     * The CPU instruction implementations.
     *
     * @see <a href="http://archive.6502.org/books/mcs6500_family_programming_manual.pdf">
     *     MCS6500 Microcomputer Family Programming Manual, 1976</a>
     * @see <a href="https://www.masswerk.at/6502/6502_instruction_set.html">
     *     6502 Instruction Set</a>
     */
    private final Map<String, Operation> ops = new HashMap<>() {{
        put("ADC", () -> { // add with carry
            result = (short) ((AC & 0xFF) + (operand & 0xFF) + status.carry());
            status.determineZero(result);

            if (status.decimal() == 1) {
                result = (short)((AC & 0xF) + (operand & 0xF) + status.carry());
                if (result > 0x9) result += 0x6;
                result = (short) ((result & 0xF) + (AC & 0xF0) + (operand & 0xF0) + (result <= 0x0F ? 0x00 : 0x10));
                status.determineNegative(result);
                status.determineOverflow(result, operand);
                if ((result & 0x1F0) > 0x90) result += 0x60;
            } else {
                status.determineNegative(result);
                status.determineOverflow(result, operand);
            }

            status.determineCarry(result);
            AC = (byte) result;
        });
        put("AND", () -> status.determineNZ(AC &= operand));
        put("ASL", () -> { // arithmetic shift left
            result = (short) ((operand & 0xFF) << 1);
            status.determineNZ(result);
            status.determineCarry(result);
            accuOrMem();
        });
        put("BCC", () -> offset = status.carry() == 0 ? operand : 0);
        put("BCS", () -> offset = status.carry() == 1 ? operand : 0);
        put("BEQ", () -> offset = status.zero()  == 1 ? operand : 0);
        put("BIT", () -> { // bit test
            status.negative((operand & (1 << 7)) != 0);
            status.overflow((operand & (1 << 6)) != 0);
            status.determineZero((short) (AC & operand));
        });
        put("BMI", () -> offset = status.negative() == 1 ? operand : 0);
        put("BNE", () -> offset = status.zero()     == 0 ? operand : 0);
        put("BPL", () -> offset = status.negative() == 0 ? operand : 0);
        put("BRK", () -> {}); // handled in address mode
        put("BVC", () -> offset = status.overflow() == 0 ? operand : 0);
        put("BVS", () -> offset = status.overflow() == 1 ? operand : 0);
        put("CLC", () -> status.carry(false));
        put("CLD", () -> status.decimal(false));
        put("CLI", () -> status.irq(false));
        put("CLV", () -> status.overflow(false));
        put("CMP", () -> opCMP(AC));
        put("CPX", () -> opCMP(XR));
        put("CPY", () -> opCMP(YR));
        put("DEC", () -> { // decrement
            result = (short) (operand - 1);
            status.determineNZ(result);
            write(ea, (byte) result);
        });
        put("DEX", () -> status.determineNZ(--XR));
        put("DEY", () -> status.determineNZ(--YR));
        put("EOR", () -> status.determineNZ(AC ^= operand));
        put("INC", () -> { // increment
            result = (short) (operand + 1);
            status.determineNZ(result);
            write(ea, (byte) result);
        });
        put("INX", () -> status.determineNZ(++XR));
        put("INY", () -> status.determineNZ(++YR));
        put("JMP", () -> PC = ea);
        put("JSR", () -> {}); // handled in address mode
        put("LDA", () -> status.determineNZ(AC = operand));
        put("LDX", () -> status.determineNZ(XR = operand));
        put("LDY", () -> status.determineNZ(YR = operand));
        put("LSR", () -> { // logical shift right
            result = (short) ((operand & 0xFF) >> 1);
            status.carry((operand & 1) != 0);
            status.determineNZ(result);
            accuOrMem();
        });
        put("NOP", () -> {}); // no operation
        put("ORA", () -> status.determineNZ(AC |= operand));
        put("PHA", () -> push(AC));
        put("PHP", () -> push((byte) (SR | Status.Bit.O | Status.Bit.B)));
        put("PLA", () -> pull(data -> status.determineNZ(AC = data)));
        put("PLP", () -> pull(data -> SR = (byte) (data | Status.Bit.O)));
        put("ROL", () -> { // rotate left
            result = (short) (((operand & 0xFF) << 1) | status.carry());
            status.carry((operand & (1 << 7)) != 0);
            status.determineNZ(result);
            accuOrMem();
        });
        put("ROR", () -> { // rotate right
            result = (short) ((status.carry() << 7) | ((operand & 0xFF) >> 1));
            status.negative(status.carry() == 1);
            status.carry((operand & 1) != 0);
            status.determineZero(result);
            accuOrMem();
        });
        put("RTI", () -> {}); // handled in address mode
        put("RTS", () -> {}); // handled in address mode
        put("SBC", () -> { // subtract with carry
            byte carry = (byte) (status.carry() == 0 ? 1 : 0);
            result = (short) ((AC & 0xFF) - (operand & 0xFF) - carry);

            // why do I need to AND 0xFFFF here, but same code in C don't need this ?!
            status.carry((result & 0xFFFF) < 0x100);
            status.determineOverflow(result, (byte) (operand ^ 0xFF));
            status.determineNZ(result);

            if (status.decimal() == 1) {
                result = (short) ((AC & 0xF) - (operand & 0xF) - carry);
                if ((result & 0x10) != 0)
                    result = (short) (((result - 6) & 0xF) | ((AC & 0xF0) - (operand & 0xF0) - 0x10));
                else
                    result = (short) ((result & 0xF) | ((AC & 0xF0) - (operand & 0xF0)));
                if ((result & 0x100) != 0) result -= 0x60;
            }

            AC = (byte) result;
        });
        put("SEC", () -> status.carry(true));
        put("SED", () -> status.decimal(true));
        put("SEI", () -> status.irq(true));
        put("STA", () -> write(ea, AC));
        put("STX", () -> write(ea, XR));
        put("STY", () -> write(ea, YR));
        put("TAX", () -> status.determineNZ(XR = AC));
        put("TAY", () -> status.determineNZ(YR = AC));
        put("TSX", () -> status.determineNZ(XR = SP));
        put("TXA", () -> status.determineNZ(AC = XR));
        put("TXS", () -> SP = XR);
        put("TYA", () -> status.determineNZ(AC = YR));

        // special instruction
        // should put data bus to 0xFF
        put("JAM", () -> {
            System.err.printf("PC = %04X, cycle = %d\n", PC, cycles);
            throw new RuntimeException("Machine is jammed!");
        });

        // undocumented opcodes
        // unstable instructions tested and mimicked from my personal C64
        put("ANC", () -> { // AND #immediate, copy accu-bit 7 to carry
            status.determineNZ(AC &= operand);
            status.carry((AC & (1 << 7)) != 0);
        });
        put("ANE", () -> status.determineNZ(AC = (byte) ((AC | ANE_MAGIC) & XR & operand)));
        put("ARR", () -> { // AND #immediate, ROR accu
            result = (short) (AC & operand);
            short carry = (short) status.carry();

            if (status.decimal() == 1) {
                short resultDEC = result;
                resultDEC |= carry << 8;
                resultDEC >>= 1;

                status.negative(carry == 1);
                status.zero(resultDEC == 0);
                status.overflow(((resultDEC ^ result) & 0x40) != 0);

                if (((result & 0x0f) + (result & 0x01)) > 0x05)
                    resultDEC = (short) ((resultDEC & 0xf0) | ((resultDEC + 0x06) & 0x0f));
                if (((result & 0xf0) + (result & 0x10)) > 0x50) {
                    resultDEC = (short) ((resultDEC & 0x0f) | ((resultDEC + 0x60) & 0xf0));
                    status.carry(true);
                } else {
                    status.carry(false);
                }

                result = resultDEC;
            } else {
                result |= carry << 8;
                result >>= 1;

                status.determineNZ(result);
                status.carry((result & 0x40) != 0);
                status.overflow(((result & 0x40) ^ ((result & 0x20) << 1)) != 0);
            }

            AC = (byte) result;
        });
        put("ASR", () -> { // AND #immediate, LSR accu
            result = (short) ((AC & operand) >> 1);
            status.determineNZ(result);
            status.determineCarry(result);
            AC = (byte) result;
        });
        put("DCP", () -> { // DEC memory, CMP memory
            short cmp = (short) ((AC & 0xFF) - operand);
            status.determineNZ(cmp);
            status.determineCarry(cmp);
            write(ea, (byte) (operand - 1));
        });
        put("ISB", () -> { // INC memory, SBC memory
            operand = (byte) (operand + 1);
            write(ea, operand);
            ops.get("SBC").execute();
        });
        put("LAS", () -> { // SP AND with memory, TSX, TXA
            result = (short) (SP & operand);
            status.determineNZ(result);
            SP = XR = AC = (byte) result;
        });
        put("LAX", () -> status.determineNZ(XR = AC = operand));
        put("LXA", () -> AC = XR = (byte) ((AC | LXA_MAGIC) & operand));
        put("RLA", () -> { // ROL memory, AND memory
            result = (short) ((operand << 1) & AC);
            status.determineNZ(result);
            status.determineCarry(result);
            AC = (byte) result;
        });
        put("RRA", () -> { // ROR memory, ADC memory
            operand >>= 1;
            ops.get("ADC").execute();
        });
        put("SAX", () -> write(ea, (byte) (AC & XR)));
        put("SBX", () -> { // Accu AND X-Register, subtract operand, result into X-Register
            result = (short) ((XR & AC) - operand);
            status.determineNZ(result);
            status.determineCarry(result);
            XR = (byte) result;
        });
        put("SHA", () -> write(ea, (byte) (AC & XR & ((ea >> 8) + 1))));
        put("SHS", () -> {
            SP = (byte) (AC & XR);
            write(ea, (byte) (SP & ((ea >> 8) + 1)));
        });
        put("SHX", () -> write(ea, (byte) (XR & ((ea >> 8) + 1))));
        put("SHY", () -> write(ea, (byte) (YR & ((ea >> 8) + 1))));
        put("SLO", () -> { // ASL memory, ORA memory
            result = (short) ((operand << 1) | AC);
            status.determineNZ(result);
            status.determineCarry(result);
            AC = (byte) result;
        });
        put("SRE", () -> { // LSR memory, EOR memory
            result = (short) ((operand >> 1) ^ AC);
            status.determineNZ(result);
            status.determineCarry(result);
            AC = (byte) result;
        });
    }};

    /**
     * Behaviour of the RDY pin. After 3 cycles of RDY held LOW the CPU halts its operation.
     */
    private void ready() {
        if (Logger.ENABLE)
            Logger.info(String.format("RDY pin changed to %s", rdy.level()));

        Pin.Level level = rdy.level();
        if (level == Pin.Level.LOW && !halt)
            rdyCounter = 3;
        else if (level == Pin.Level.HIGH && halt)
            halt = false;
    }

    /**
     * Behaviour of the AEC pin. When held LOW the address bus pins are isolated from the bus.
     */
    // TODO check AEC pin change behaviour
    private void aec() {
        if (Logger.ENABLE)
            Logger.info(String.format("AEC pin changed to %s", aec.level()));

        address.direction(aec.level() == Pin.Level.LOW ? Pin.Direction.HI_Z : Pin.Direction.OUTPUT);
    }

    /**
     * Bahaviour of the GATE_IT pin. Prevents unwanted writes to the RAM memory.
     */
    // TODO check GATE_IN pin change behaviour
    private void gateIn() {
        if (Logger.ENABLE)
            Logger.info(String.format("GATE_IN pin changed to %s", gate.level()));

        if (gate.level() == Pin.Level.HIGH && aec.level() == Pin.Level.LOW) {
            rw.direction(Pin.Direction.HI_Z);
        } else if (gate.level() == Pin.Level.LOW && aec.level() == Pin.Level.HIGH) {
            rw.direction(Pin.Direction.OUTPUT);
            if (rw.level() == Pin.Level.LOW && lastData != null) {
                if (Logger.ENABLE)
                    Logger.info("Output again data to data bus");
                data.value(lastData);
                lastData = null;
            }
        }

        /*if (gate.level() == Pin.Level.HIGH) {
            if (aec.level() == Pin.Level.LOW) {
                rw.direction(Pin.Direction.HI_Z);
            } else {
                rw.direction(Pin.Direction.OUTPUT);
                if (rw.level() == Pin.Level.LOW && lastData != null) {
                    if (Logger.ENABLE)
                        Logger.info("Output again data to data bus");
                    data.value(lastData);
                    //lastData = null;
                }
            }
            //rw.direction(aec.level() == Pin.Level.LOW ? Pin.Direction.HI_Z : Pin.Direction.OUTPUT);
        }*/
    }

    /**
     * Behaviour of the RESET pin.
     * <br>When held LOW the CPU is halted, and after transition to HIGH its state is restarted.
     */
    private void reset() {
        if (Logger.ENABLE)
            Logger.info("Resetting CPU");

        if (reset.level() == Pin.Level.HIGH) {
            maskIRQ = false;
            irqPending = false;
            rdyCounter = 0;

            stage = Stage.DECODE;
            decodeCycle = 1;
            decoding = addr::reset;
            halt = false;
        } else {
            halt = true;
        }
    }

    /**
     * Manages the data coming in to or out from the CPU when clock signal is HIGH.
     */
    protected void halfstep() {
        //if (halfCycleOut != null && halfCycleIn != null && aec.level() == Pin.Level.HIGH) {

        if (halfCycleIn != null) {
            if (Logger.ENABLE)
                Logger.info(String.format("Halfcycle memory access [0x%02X]", halfCycleIn.get()));

            if (halfCycleOut != null)
                halfCycleOut.accept(halfCycleIn.get());
        }

        if (aec.level() == Pin.Level.HIGH && gate.level() == Pin.Level.LOW) {
            halfCycleOut = null;
            halfCycleIn = null;
        }
    }

    /**
     * Calculates the current state of the CPU.
     */
    protected void step() {
        if (Logger.ENABLE)
            Logger.info("phi0 is " + phi0.level());

        if (phi0.level() == Pin.Level.HIGH) {
            halfstep();
            return;
        }

        lastData = null;
        irqPending = (irq.level() == Pin.Level.LOW && status.irq() == 0 && !maskIRQ);
        if (maskIRQ)
            maskIRQ = false;

        if (halt) {
            if (Logger.ENABLE)
                Logger.info("The CPU is halted");
            return;
        }

        if (rdy.level() == Pin.Level.LOW && rdyCounter != 0) {
            rdyCounter--;
            if (rdyCounter == 0)
                halt = true;
        }

        if (Logger.ENABLE)
            Logger.info("New CPU cycle");

        cycles++;
        //if (stage == Stage.MEMORY)
            //stage = Stage.OPCODE;

        if (stage == Stage.FETCH) {
            decoding = modes[opcode & 0xFF];

            if (Logger.ENABLE)
                Logger.info(String.format("Fetched %s with %s addressing",
                        mnemonic[opcode & 0xFF], Monitor.addressing.get(opcode & 0xFF)));

            if (!useUndocumented && undocumented.contains(mnemonic[opcode & 0xFF]))
                throw new RuntimeException("Undocumented instructions support is disabled!");

            stage = Stage.DECODE;
        }

        if (stage == Stage.EXECUTE) {
            try {
                if (Logger.ENABLE)
                    Logger.info(String.format("Executing %s", mnemonic[opcode & 0xFF]));

                ops.get(mnemonic[opcode & 0xFF]).execute();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                System.err.printf("PC = %04X, cycle = %d\n", PC, cycles);
                System.exit(1);
            }

            //stage = (halfCycleOut == null && halfCycleIn == null) ? Stage.OPCODE : Stage.MEMORY;
            stage = Stage.OPCODE;
            if (halfCycleOut != null || halfCycleIn != null)
                return;
        }

        if (stage == Stage.OPCODE) {
            decodeCycle = 1;
            lastPos = PC;

                /*if (irqPending) {
                    irqPending = false;
                    addr.irq();

                    if (Logger.ENABLE)
                        Logger.info("External interrupt occurred!");
                }*/

            if (irqPending && status.irq() == 0) {
                irqPending = false;
                decoding = addr::irq;
                stage = Stage.DECODE;

                if (Logger.ENABLE)
                    Logger.info("External interrupt occurred!");
            } else {
                if (Logger.ENABLE) {
                    Logger.info(String.format("Current CPU state (%d cycle):\n%s", cycles, this.toString()));
                    Logger.info("Fetching new opcode");
                }

                addr.fetchOp();
            }
        }

        if (stage == Stage.DECODE) {
            decodeCycle++;

            if (Logger.ENABLE)
                Logger.info(String.format("Decoding %s, cycle %d", mnemonic[opcode & 0xFF], decodeCycle));

            decoding.decode();
        }
    }

    /**
     * Initializes events for all input pins.
     */
    public MOS8501() {
        phi0.onChange(this::step);
        rdy.onChange(this::ready);
        aec.onChange(this::aec);
        gate.onChange(this::gateIn);
        reset.onChange(this::reset);
    }

    /**
     * Returns {@code true} if CPU is halted.
     * @return {@code true} if CPU is halted
     */
    public boolean isHalted() {
        return halt;
    }

    /**
     * Returns the number of CPU cycles.
     * @return the number of CPU cycles
     */
    public long cycles() {
        return cycles;
    }

    /**
     * Returns the program counter.
     * @return the program counter
     */
    public short counter() {
        return PC;
    }

    /**
     * Returns program counter which corresponds to current opcode.
     * @return the program counter which corresponds to current opcode
     */
    public short lastOpcodePosition() {
        return lastPos;
    }

    /**
     * Returns currently processed opcode.
     * @return currently processed opcode
     */
    public byte opcode() {
        return opcode;
    }

    /**
     * Returns mnemonic of current opcode.
     * @return the mnemonic of current opcode
     */
    public String mnemonic() {
        String op = mnemonic[opcode & 0xFF];
        return ops.containsKey(op) ? op : "***";
    }

    /**
     * Returns all processor registers in string format.
     * @return registers in string format
     */
    public String reg() {
        return String.format("%02X %02X %02X %02X %02X", SR, AC, XR, YR, SP);
    }

    /**
     * Returns current state of the CPU.
     * @return current state of the CPU.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  PC  SR AC XR YR SP  NV-BDIZC\n");
        sb.append(String.format(";%04X %s  ", PC, reg()));
        sb.append(String.format("%8s", Integer.toString(SR & 0xFF, 2)).replace(' ', '0'));
        if (halt)
            sb.append("\n\tThe CPU is halted!");

        return sb.toString();
    }
}
