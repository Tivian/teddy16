package eu.tivian.hardware;

import java.util.*;

// TODO
// interrupt handling
// external signal handling
public class MOS8501 implements CPU {
    private enum Stage {
        OPCODE, DECODE, EXECUTE
    };

    private interface AddressingMode { void decode(); }
    private interface Operation { void execute(); }

    public class Status {
        public class Bit {
            static final int N = (1 << 7); // Negative flag
            static final int V = (1 << 6); // oVerflow flag
            static final int O = (1 << 5); // ignore (always 1)
            static final int B = (1 << 4); // Break flag
            static final int D = (1 << 3); // Decimal mode flag
            static final int I = (1 << 2); // Interrupt flag (IRQ disable)
            static final int Z = (1 << 1); // Zero flag
            static final int C = (1 << 0); // Carry flag
        }

        // get flag
        public int negative() { return (SR & Bit.N) != 0 ? 1 : 0; }
        public int overflow() { return (SR & Bit.V) != 0 ? 1 : 0; }
        public int brk() {      return (SR & Bit.B) != 0 ? 1 : 0; }
        public int decimal() {  return (SR & Bit.D) != 0 ? 1 : 0; }
        public int irq() {      return (SR & Bit.I) != 0 ? 1 : 0; }
        public int zero() {     return (SR & Bit.Z) != 0 ? 1 : 0; }
        public int carry() {    return (SR & Bit.C) != 0 ? 1 : 0; }

        // set flag
        public void negative(boolean val) { if (val) SR |= Bit.N; else SR &= ~Bit.N; }
        public void overflow(boolean val) { if (val) SR |= Bit.V; else SR &= ~Bit.V; }
        public void brk(boolean val) {      if (val) SR |= Bit.B; else SR &= ~Bit.B; }
        public void decimal(boolean val) {  if (val) SR |= Bit.D; else SR &= ~Bit.D; }
        public void irq(boolean val) {      if (val) SR |= Bit.I; else SR &= ~Bit.I; }
        public void zero(boolean val) {     if (val) SR |= Bit.Z; else SR &= ~Bit.Z; }
        public void carry(boolean val) {    if (val) SR |= Bit.C; else SR &= ~Bit.C; }

        // helper functions
        void determineNegative(short val) { negative((val & 0x0080) != 0); }
        void determineOverflow(short res, byte val) { overflow(((res ^ AC) & (res ^ val) & 0x0080) != 0); }
        void determineZero(short val) { zero((val & 0x00FF) == 0); }
        void determineCarry(short val) { carry((val & 0xFF00) != 0); }

        // many instructions affect N and Z flags
        void determineNZ(short val) { determineNegative(val); determineZero(val);}
    }

    public static final short IO_DIR_VECT = (short) 0x0000;
    public static final short IO_VECT     = (short) 0x0001;
    public static final short STACK_VECT  = (short) 0x0100;
    public static final short RESET_VECT  = (short) 0xFFFC;
    public static final short IRQ_VECT    = (short) 0xFFFE;

    private static final byte ANE_MAGIC    = (byte) 0xFE; // taken from my actual C64
    private static final byte LXA_MAGIC    = (byte) 0xEE; // taken from my actual C64

    public byte  SR = 0b00100000;
    public short PC = 0x0000;
    public byte  AC = 0x00;
    public byte  XR = 0x00;
    public byte  YR = 0x00;
    public byte  SP = (byte) 0xFF;

    public final Status status = new Status();

    public final Pin phi0  = new Pin("PHI0", Pin.Direction.INPUT);   // pin  1
    public final Pin rdy   = new Pin("RDY" , Pin.Direction.INPUT);   // pin  2
    public final Pin irq   = new Pin("/IRQ", Pin.Direction.INPUT);   // pin  3
    public final Pin aec   = new Pin("AEC" , Pin.Direction.INPUT);   // pin  4
    public final Pin gate  = new Pin("Gate", Pin.Direction.INPUT);   // pin 23
    public final Pin RW    = new Pin("R/-W", Pin.Direction.OUTPUT, Pin.Level.HIGH); // pin 39
    public final Pin reset = new Pin("/RES", Pin.Direction.INPUT);   // pin 40

    public final Bus address = new Bus("Address", "A" , Pin.Direction.OUTPUT, 16); // pins 6 - 19, 21, 22
    public final Bus data    = new Bus("Data"   , "DB", Pin.Direction.OUTPUT,  8); // pins 31 - 38
    public final Bus port    = new Bus("Port"   , "P" , Pin.Direction.OUTPUT,  7); // pins 24 - 30

    private boolean halt = true;
    private boolean irqPending = false;

    private Stage stage = Stage.OPCODE;
    private byte decodeCycle = 0;
    private AddressingMode decoding = null;
    private boolean isAccu = false;

    private long  cycles  = 0;      // CPU cycle counter
    private byte  opcode  = 0x00;   // current opcode
    private byte  operand = 0x00;   // current operand
    private short ea      = 0x0000; // effective address
    private short pointer = 0x0000; // temporary pointer
    private byte  offset  = 0x00;   // branch offset

    /*private byte read(short address) {
        if (address == IO_DIR_VECT) {
            byte value = 0x00;
            for (Pin p : port) {
                value |= p.direction() == Pin.Direction.OUTPUT ? 1 : 0;
                value <<= 1;
            }
            return value;
        } else if (address == IO_VECT) {
            return (byte) port.value();
        } else if (aec.level() == Pin.Level.LOW) {
            return 0x00;
        } else {
            RW.level(Pin.Level.LOW);
            data.direction(Pin.Direction.INPUT);
            this.address.value(address);
            return (byte) (data.value() & 0xFF);
        }
    }

    private void write(short address, byte value) {
        if (address == IO_DIR_VECT) {
            for (Pin p : port) {
                p.direction((value & 1) != 0 ? Pin.Direction.OUTPUT : Pin.Direction.INPUT);
                value >>= 1;
            }
        } else if (address == IO_VECT) {
            port.value(value);
        } else if (aec.level() == Pin.Level.HIGH) {
            RW.level(Pin.Level.HIGH);
            data.direction(Pin.Direction.OUTPUT);
            this.address.value(address);
            this.data.value(value);
        }
    }*/

    private byte[] memory = new byte[0x10000];

    private byte read(short address) {
        return memory[address & 0xFFFF];
    }

    private void write(short address, byte value) {
        memory[address & 0xFFFF] = value;
    }

    private byte pull() {
        return read((short) (STACK_VECT + (SP & 0xFF)));
    }

    private void push(byte value) {
        write((short) (STACK_VECT + (SP & 0xFF)), value);
        SP--; // all push operations require SP decrementation
    }

    public static final String[] mnemonic = {
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

    // based on "MCS6500 Microcomputer Family Programming Manual, 1976"
    //      http://archive.6502.org/books/mcs6500_family_programming_manual.pdf
    // also http://www.zimmers.net/anonftp/pub/cbm/documents/chipdata/64doc
    private class Addressing {
        private boolean carry = false;
        private short temp = 0x0000;

        void fetchOp() { // fetch opcode, increment PC
            opcode = read(PC++);
            stage = Stage.DECODE;
        }

        // implied addressing [2 cycles]
        void imp() { // read next instruction byte (and throw it away)
            read(PC);
            stage = Stage.EXECUTE;
        }

        // accumulator addressing [2 cycles]
        void acc() { // read next instruction byte (and throw it away)
            read(PC);
            stage = Stage.EXECUTE;
            isAccu = true;
        }

        // immediate addressing [2 cycles]
        void imm() { // fetch value, increment PC
            operand = read(PC++);
            stage = Stage.EXECUTE;
        }

        // absolute addressing (JMP) [3 cycles]
        void absJMP() {
            if (decodeCycle == 2) { // fetch low address byte, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else { // copy low address byte to PCL, fetch high address byte to PCH
                ea |= read(PC) << 8;
                stage = Stage.EXECUTE;
            }
        }

        // absolute addressing (Read/Write) [4 cycles]
        private void absR_W(boolean read) {
            if (decodeCycle == 2) { // fetch low byte of address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch high byte of address, increment PC
                ea |= read(PC++) << 8;
            } else { // read from/write to effective address
                if (read) operand = read(ea);
                stage = Stage.EXECUTE;
            }
        }
        void absR_() { absR_W(true);  }
        void abs_W() { absR_W(false); }

        // absolute addressing (Read-Modify-Write) [6 cycles]
        void absRW() {
            if (decodeCycle == 2) { // fetch low byte of address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch high byte of address, increment PC
                ea |= read(PC++) << 8;
            } else if (decodeCycle == 4) { // read from effective address
                operand = read(ea);
            } else if (decodeCycle == 5) { // write the value back to effective address
                write(ea, operand);
            } else if (decodeCycle == 6) { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }

        // zeropage addressing (Read/Write) [3 cycles]
        private void zpgR_W(boolean read) {
            if (decodeCycle == 2) { // fetch address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else { // read from/write to effective address
                if (read) operand = read(ea);
                stage = Stage.EXECUTE;
            }
        }
        void zpgR_() { zpgR_W(true);  }
        void zpg_W() { zpgR_W(false); }

        // zeropage addressing (Read-Modify-Write) [5 cycles]
        void zpgRW() {
            if (decodeCycle == 2) { // fetch address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // read from effective address
                operand = read(ea);
            } else if (decodeCycle == 4) { // write the value back to effective address
                write(ea, operand);
            } else { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }

        // zeropage indexed addressing (Read/Write) [4 cycles]
        private void zpiR_W(byte reg, boolean read) {
            if (decodeCycle == 2) { // fetch address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // read from address, add index register to it
                read(ea);
                ea = (short) ((ea + (reg & 0xFF)) & 0xFF); // page boundary crossings are not handled
            } else { // read from/write to effective address
                if (read) operand = read(ea);
                stage = Stage.EXECUTE;
            }
        }
        void zpxR_() { zpiR_W(XR, true);  }
        void zpx_W() { zpiR_W(XR, false); }
        void zpyR_() { zpiR_W(YR, true);  }
        void zpy_W() { zpiR_W(YR, false); }

        // zeropage indexed addressing (Read-Modify-Write) [6 cycles]
        private void zpiRW(byte reg) {
            if (decodeCycle == 2) { // fetch address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // read from address, add index register X to it
                read(ea);
                ea = (short) ((ea + (reg & 0xFF)) & 0xFF); // page boundary crossings are not handled
            } else if (decodeCycle == 4) { // read from effective address
                operand = read(ea);
            } else if (decodeCycle == 5) { // write the value back to effective address
                write(ea, operand);
            } else { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }
        void zpxRW() { zpiRW(XR); }
        void zpyRW() { zpiRW(YR); }

        // absolute indexed addressing (Read/Write) [4-5 cycles]
        private void abxR_W(byte reg, boolean read) {
            if (decodeCycle == 2) { // fetch low byte of address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch high byte of address, increment PC
                ea |= read(PC++) << 8;
                // add index register to low address byte
                temp = (short) (ea + (reg & 0xFF));
                ea = (short) ((ea & 0xFF00) | ((ea + (reg & 0xFF)) & 0x00FF));
                carry = temp != ea;
            } else if (decodeCycle == 4) { // read from effective address
                operand = read(ea);
                if (carry) // fix the high byte of effective address
                    ea = temp;
                else
                    stage = Stage.EXECUTE;
            } else { // re-read from / write to effective address
                if (read) operand = read(ea);
                stage = Stage.EXECUTE;
            }
        }
        void abxR_() { abxR_W(XR, true);  }
        void abx_W() { abxR_W(XR, false); }
        void abyR_() { abxR_W(YR, true);  }
        void aby_W() { abxR_W(YR, false); }

        // absolute indexed addressing (Read-Modify-Write) [7 cycles]
        private void abiRW(byte reg) {
            if (decodeCycle == 2) { // fetch low byte of address, increment PC
                ea = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch high byte of address, increment PC
                ea |= read(PC++) << 8;
                // add index register X to low address byte
                temp = (short) (ea + (reg & 0xFF));
                ea = (short) ((ea & 0xFF00) | (temp & 0x00FF));
                carry = temp != ea;
            } else if (decodeCycle == 4) { // read from effective address
                operand = read(ea);
                if (carry) // fix the high byte of effective address
                    ea = temp;
            } else if (decodeCycle == 5) { // re-read from effective address
                operand = read(ea);
            } else if (decodeCycle == 6) { // write the value back to effective address
                write(ea, operand);
            } else { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }
        void abxRW() { abiRW(XR); }
        void abyRW() { abiRW(YR); }

        // relative addressing [3-5 cycles]
        void rel() {
            if (decodeCycle == 2) { // fetch operand, increment PC
                operand = read(PC++);
            } else if (decodeCycle == 3) { // fetch opcode of next instruction
                ops.get(mnemonic[opcode & 0xFF]).execute();
                if (offset == 0) { // branch not taken
                    stage = Stage.OPCODE;
                }  else { // if branch is taken, add operand to PCL
                    temp = (short) (PC + offset);
                    PC = (short) ((PC & 0xFF00) | (temp & 0x00FF));
                    carry = temp != PC;
                }
            } else if (decodeCycle == 4) { // fetch opcode of next instruction
                read(PC);
                if (carry)
                    PC = temp;
                else
                    stage = Stage.OPCODE;
            } else { // fetch opcode of next instruction, increment PC
                stage = Stage.OPCODE;
            }
        }

        // indexed indirect addressing (Read/Write) [6 cycles]
        private void izxR_W(boolean read) {
            if (decodeCycle == 2) { // fetch pointer address, increment PC
                operand = read(PC++);
            } else if (decodeCycle == 3) { // read from the address, add X to it
                read(operand);
                pointer = (short) ((operand + (XR & 0xFF)) & 0xFF); // page boundary crossings are not handled
            } else if (decodeCycle == 4) { // fetch effective address low
                ea = (short) (read(pointer) & 0xFF);
            } else if (decodeCycle == 5) { // fetch effective address high
                ea |= read((byte) (pointer + 1)) << 8;
            } else { // read from / write to effective address
                if (read) operand = read(ea);
                stage = Stage.EXECUTE;
            }
        }
        void izxR_() { izxR_W(true);  }
        void izx_W() { izxR_W(false); }

        // indexed indirect addressing (Read-Modify-Write) [8 cycles]
        void izxRW() {
            if (decodeCycle == 2) { // fetch pointer address, increment PC
                operand = read(PC++);
            } else if (decodeCycle == 3) { // read from the address, add X to it
                read(operand);
                pointer = (short) ((operand + (XR & 0xFF)) & 0xFF); // page boundary crossings are not handled
            } else if (decodeCycle == 4) { // fetch effective address low
                ea = (short) (read(pointer) & 0xFF);
            } else if (decodeCycle == 5) { // fetch effective address high
                ea |= read((byte) (pointer + 1)) << 8;
            } else if (decodeCycle == 6) { // read from effective address
                operand = read(ea);
            } else if (decodeCycle == 7) { // write the value back to effective address
                write(ea, operand);
            } else { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }

        // indirect indexed addressing (Read/Write) [5-6 cycles]
        private void izyR_W(boolean read) {
            if (decodeCycle == 2) { // fetch pointer address, increment PC
                pointer = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch effective address low
                ea = (short) (read(pointer) & 0xFF);
            } else if (decodeCycle == 4) { // fetch effective address high
                ea |= read((byte) (pointer + 1)) << 8; // page boundary crossing is not handled
                // add Y to low byte of effective address
                temp = (short) (ea + (YR & 0xFF));
                ea = (short) ((ea & 0xFF00) | ((ea + (YR & 0xFF)) & 0x00FF));
                carry = temp != ea;
            } else if (decodeCycle == 5) { // read from effective address
                operand = read(ea);
                if (carry)  // fix high byte of effective address
                    ea = temp;
                else
                    stage = Stage.EXECUTE;
            } else { // read from / write to effective address
                if (read) operand = read(ea);
                stage = Stage.EXECUTE;
            }
        }
        void izyR_() { izyR_W(true);  }
        void izy_W() { izyR_W(false); }

        // indirect indexed addressing (Read-Modify-Write) [8 cycles]
        void izyRW() {
            if (decodeCycle == 2) { // fetch pointer address, increment PC
                pointer = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch effective address low
                ea = (short) (read(pointer) & 0xFF);
            } else if (decodeCycle == 4) { // fetch effective address high
                ea |= read((byte) (pointer + 1)) << 8; // page boundary crossing is not handled
                // add Y to low byte of effective address
                temp = (short) (ea + (YR & 0xFF));
                ea = (short) ((ea & 0xFF00) | ((ea + (YR & 0xFF)) & 0x00FF));
                carry = temp != ea;
            } else if (decodeCycle == 5) { // read from effective address
                operand = read(ea);
                if (carry) // fix high byte of effective address
                    ea = temp;
            } else if (decodeCycle == 6) {// read from effective address
                operand = read(ea);
            } else if (decodeCycle == 7) { // write the value back to effective address
                write(ea, operand);
            } else {
                stage = Stage.EXECUTE;
            }
        }

        // absolute indirect addressing (JMP) [5 cycles]
        void indJMP() {
            if (decodeCycle == 2) { // fetch pointer address low, increment PC
                pointer = (short) (read(PC++) & 0xFF);
            } else if (decodeCycle == 3) { // fetch pointer address high, increment PC
                pointer |= read(PC++) << 8;
            } else if (decodeCycle == 4) { // fetch low address to latch
                ea = (short) (read(pointer) & 0xFF);
            } else { // fetch PCH, copy latch to PCL
                // page boundary crossing is not handled
                ea |= read((short) ((pointer & 0xFF00) | ((pointer + 1) & 0x00FF))) << 8;
                stage = Stage.EXECUTE;
            }
        }

        // BRK addressing [7 cycles]
        void stkBRK() {
            if (decodeCycle == 2) { // read next instruction byte (and throw it away), increment PC
                read(PC++);
            } else if (decodeCycle == 3) { // push PCH on stack, decrement S
                push((byte) (PC >> 8));
            } else if (decodeCycle == 4) { // push PCL on stack, decrement S
                push((byte) (PC & 0x00FF));
            } else if (decodeCycle == 5) { // push P on stack (with B flag set), decrement S
                push((byte) (SR | Status.Bit.B));
            } else if (decodeCycle == 6) { // fetch PCL
                PC = (short) ((PC & 0xFF00) | (read(IRQ_VECT) & 0xFF));
            } else { // fetch PCH
                status.irq(true);
                PC = (short) ((read((short) (IRQ_VECT + 1)) << 8) | (PC & 0x00FF));
                stage = Stage.EXECUTE;
            }
        }

        // RTI addressing [6 cycles]
        void stkRTI() {
            if (decodeCycle == 2) { // read next instruction byte (and throw it away)
                read(PC);
            } else if (decodeCycle == 3) { // increment S
                SP++;
            } else if (decodeCycle == 4) { // pull P from stack, increment S
                SR = (byte) (pull() | Status.Bit.O);
                SP++;
            } else if (decodeCycle == 5) { // pull PCL from stack, increment S
                PC = (short) ((PC & 0xFF00) | (pull() & 0xFF));
                SP++;
            } else { // pull PCH from stack
                PC = (short) ((pull() << 8) | (PC & 0x00FF));
                stage = Stage.EXECUTE;
            }
        }

        // RTS addressing [6 cycles]
        void stkRTS() {
            if (decodeCycle == 2) { // read next instruction byte (and throw it away)
                read(PC);
            } else if (decodeCycle == 3) { // increment S
                SP++;
            } else if (decodeCycle == 4) { // pull PCL from stack, increment S
                PC = (short) ((PC & 0xFF00) | (pull() & 0xFF));
                SP++;
            } else if (decodeCycle == 5) { // pull PCH from stack
                PC = (short) ((pull() << 8) | (PC & 0x00FF));
            } else { // increment PC
                PC++;
                stage = Stage.EXECUTE;
            }
        }

        // push stack addressing [3 cycles]
        void stk_PH() {
            if (decodeCycle == 2) { // read next instruction byte (and throw it away)
                read(PC);
            } else { // push register on stack, decrement S
                stage = Stage.EXECUTE;
            }
        }

        // pull stack addressing [4 cycles]
        void stk_PL() {
            if (decodeCycle == 2) { // read next instruction byte (and throw it away)
                read(PC);
            } else if (decodeCycle == 3) { // increment S
                SP++;
            } else { // pull register from stack
                stage = Stage.EXECUTE;
            }
        }

        // JSR addressing [6 cycles]
        void stkJSR() {
            if (decodeCycle == 2) { // fetch low address byte, increment PC
                operand = read(PC++);
            } else if (decodeCycle == 3) { // internal operation (predecrement S?)
                pull();
            } else if (decodeCycle == 4) { // push PCH on stack, decrement S
                push((byte) (PC >> 8));
            } else if (decodeCycle == 5) { // push PCL on stack, decrement S
                push((byte) (PC & 0x00FF));
            } else if (decodeCycle == 6) { // copy low address byte to PCL, fetch high address byte to PCH
                PC = (short) ((read(PC) << 8) | (operand & 0xFF));
                stage = Stage.EXECUTE;
            }
        }

        // illegal indexed indirect addressing (Read-Modify-Write) [8 cycles]
        void izxILL() {
            if (decodeCycle == 2) { // fetch pointer address, increment PC
                operand = read(PC++);
            } else if (decodeCycle == 3) { // dummy cycle, read the same data
                read(PC);
            } else if (decodeCycle == 4) { // add X to pointer, fetch effective address low
                pointer = (short) ((operand + (XR & 0xFF)) & 0xFF); // page boundary crossings are not handled
                ea = (short) (read(pointer) & 0xFF);
            } else if (decodeCycle == 5) { // fetch effective address high
                ea |= read((byte) (pointer + 1)) << 8;
            } else if (decodeCycle == 6) { // read from effective address
                operand = read(ea);
            } else if (decodeCycle == 7) { // write the value back to effective address
                write(ea, operand);
            } else { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }

        // illegal indirect indexed addressing (Read-Modify-Write) [8 cycles]
        void izyILL() {
            if (decodeCycle == 2) { // fetch pointer address, increment PC
                operand = read(PC++);
            } else if (decodeCycle == 3) { // fetch effective address low
                ea = (short) (read(operand++) & 0xFF);
            } else if (decodeCycle == 4) { // fetch effective address high
                ea |= read(operand) << 8;
            } else if (decodeCycle == 5) { // add Y to effective address
                ea += YR;
                read(ea);
            } else if (decodeCycle == 6) { // read from effective address
                operand = read(ea);
            } else if (decodeCycle == 7) { // write the value back to effective address
                write(ea, operand);
            } else { // write the new value to effective address
                stage = Stage.EXECUTE;
            }
        }

        // reset sequence [7 cycles]
        void reset() {
            if (decodeCycle == 2) { // don't care (address bus ?? + 1)
                read((short) (PC + 1));
            } else if (decodeCycle == 3) { // don't care (address bus 0x0100 + SP)
                read((short) (0x0100 + (SP & 0xFF)));
            } else if (decodeCycle == 4) { // don't care (address bus 0x0100 + SP-1)
                read((short) (0x0100 + ((SP - 1) & 0xFF)));
            } else if (decodeCycle == 5) { // don't care (address bus 0x0100 + SP-2)
                read((short) (0x0100 + ((SP - 2) & 0xFF)));
            } else if (decodeCycle == 6) { // fetch PCL from RESET_VECT
                PC = (short) ((PC & 0xFF00) | (read(RESET_VECT)));
            } else { // fetch PCH from RESET_VECT+1
                PC = (short) ((read((short) ((RESET_VECT & 0xFFFF) + 1)) << 8) | (PC & 0x00FF));
                stage = Stage.OPCODE;
            }
        }

        // hardware interrupt sequence [7 cycles]
        void irq() {
            if (decodeCycle == 2) { // fetch again the same instruction and throw it away
                read(PC);
            } else if (decodeCycle == 3) { // push PCH on stack, decrement S
                push((byte) (PC >> 8));
            } else if (decodeCycle == 4) { // push PCL on stack, decrement S
                push((byte) (PC & 0xFF));
            } else if (decodeCycle == 5) { // push P on stack (with B flag cleared), decrement S
                push((byte) (SR & ~Status.Bit.B));
            } else if (decodeCycle == 6) { // fetch new PCL from IRQ_VECT
                PC = (short) ((PC & 0xFF00) | (read(IRQ_VECT)));
            } else { // fetch new PCH from IRQ_VECT+1, set I flag
                PC = (short) ((read((short) ((IRQ_VECT & 0xFFFF) + 1)) << 8) | (PC & 0x00FF));
                status.irq(true);
                stage = Stage.OPCODE;
            }
        }
    }

    private short result = 0x0000;
    private final Addressing addr = new Addressing();
    private final AddressingMode[] modes = new AddressingMode[] {
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
/*0x80*/addr::imm   , addr::izxRW, addr::imm, addr::izxRW , addr::zpg_W , addr::zpg_W, addr::zpg_W, addr::zpg_W,
/*0x88*/addr::imp   , addr::imm  , addr::imp, addr::imm   , addr::abs_W , addr::abs_W, addr::abs_W, addr::abs_W,
/*0x90*/addr::rel   , addr::izyRW, addr::imp, addr::izyRW , addr::zpxRW , addr::zpxRW, addr::zpyRW, addr::zpyRW,
/*0x98*/addr::imp   , addr::abyRW, addr::imp, addr::aby_W , addr::abxRW , addr::abxRW, addr::abyRW, addr::abyRW,
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

    private void accuOrMem() {
        if (isAccu)
            AC = (byte) result;
        else
            write(ea, (byte) result);
        isAccu = false;
    }

    private void opCMP(byte reg) { // Compare Memory and given register
        result = (short) (reg - operand);
        status.determineNZ(result);
        status.carry((reg & 0xFF) >= (operand & 0xFF));
    }

    // based on "MCS6500 Microcomputer Family Programming Manual, 1976"
    //      http://archive.6502.org/books/mcs6500_family_programming_manual.pdf
    // also https://www.masswerk.at/6502/6502_instruction_set.html
    //      http://www.unusedino.de/ec64/technical/aay/c64/bmain.htm
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
        put("PLA", () -> status.determineNZ(AC = pull()));
        put("PLP", () -> SR = (byte) (pull() | Status.Bit.O));
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
            System.err.println(cycles);
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

    private void reset(Pin.Level level) {
        if (level == Pin.Level.HIGH) {
            System.out.println("Starting...");
            stage = Stage.DECODE;
            decodeCycle = 1;
            decoding = addr::reset;
            halt = false;
        } else {
            halt = true;
        }
    }

    private void irq(Pin.Level level) {
        // TODO
        // handle IRQ properly
        //irqPending = value == Pin.Value.HIGH;
    }

    public MOS8501() {
        /*
public final Pin phi0  = new Pin("PHI0", Pin.Direction.INPUT);   // pin  1
public final Pin rdy   = new Pin("RDY" , Pin.Direction.INPUT);   // pin  2
public final Pin irq   = new Pin("/IRQ", Pin.Direction.INPUT);   // pin  3
public final Pin aec   = new Pin("AEC" , Pin.Direction.INPUT);   // pin  4
public final Pin gate  = new Pin("Gate", Pin.Direction.INPUT);   // pin 23
public final Pin RW    = new Pin("R/-W", Pin.Direction.OUTPUT, Pin.Level.HIGH); // pin 39
public final Pin reset = new Pin("/RES", Pin.Direction.INPUT);   // pin 40
         */
        phi0.onChange((lvl) -> step());
        reset.onChange(this::reset);
        irq.onChange(this::irq);
    }

    public long getCycles() {
        return cycles;
    }

    public void preload(byte[] memory) {
        preload(memory, (short) 0x0000);
    }

    public void preload(byte[] memory, int begin) {
        for (byte v : memory)
            this.memory[begin++ & 0xFFFF] = v;
    }

    public byte peek(int address) {
        return memory[address & 0xFFFF];
    }

    public short lastPC() {
        return lastIrrPC;
    }

    public void fastBoot() {
        fastForward(6);
    }

    public void fastForward(int cycles) {
        while (cycles-- != 0)
            step();
    }

    private boolean change = false;
    public short lastIrrPC = 0x0000;
    public boolean step() {
        if (halt)
            halt = false;
            //return false;

        cycles++;
        change = false;

        if (stage == Stage.DECODE) {
            decodeCycle++;
            decoding.decode();
        }

        if (stage == Stage.OPCODE) {
            if (irqPending) {
                irqPending = false;
                addr.irq();
            }

            change = true;
            lastIrrPC = PC;
            addr.fetchOp();
            decodeCycle = 1;
            decoding = modes[opcode & 0xFF];
        }

        if (stage == Stage.EXECUTE) {
            ops.get(mnemonic[opcode & 0xFF]).execute();
            stage = Stage.OPCODE;
        }

        return change;//stage == Stage.OPCODE;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("  PC  SR AC XR YR SP  NV-BDIZC\n");
        sb.append(String.format(";%04X %02X %02X %02X %02X %02X  ", PC, SR, AC, XR, YR, SP));
        sb.append(String.format("%8s", Integer.toString(SR & 0xFF, 2)).replace(' ', '0'));
        if (halt)
            sb.append("\n\tCPU is halted!");

        return sb.toString();
    }
}
