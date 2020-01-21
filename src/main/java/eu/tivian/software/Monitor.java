package eu.tivian.software;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Assembly language monitor.
 * <br>Meant for the CPU debugging.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see SimpleCPU
 * @see eu.tivian.hardware.MOS8501
 */
public class Monitor {
    /**
     * Functional interface for writing to the memory.
     */
    public interface Poke {
        /**
         * Takes the byte and writes it into the memory at given address.
         * @param address the memory cell index
         * @param data value to write
         */
        void accept(short address, byte data);
    }

    /**
     * Functional interface for reading the memory.
     */
    public interface Peek {
        /**
         * Gets the value from the memory.
         * @param address the memory cell index
         * @return value stored in memory at specified address
         */
        byte get(short address);
    }

    /**
     * Addressing mode formatter.
     */
    public static class Mode {
        /**
         * Number of bytes taken by the addressing mode.
         */
        int bytes;
        /**
         * Format string for the addressing mode.
         */
        String format;

        /**
         * Initializes the formatter.
         *
         * @param bytes number of bytes taken by the addressing mode
         * @param format format string
         */
        public Mode(int bytes, String format) {
            this.bytes = bytes;
            this.format = format;
        }
    }

    /**
     * Mnemonics indexed by the opcode.
     */
    public static final String[] opcode = {
/*  H/L      0xH0   0xH1   0xH2   0xH3   0xH4   0xH5   0xH6   0xH7   0xH8   0xH9   0xHA   0xHB   0xHC   0xHD   0xHE   0xHF */
/* 0x0L */  "BRK", "ORA", "JAM", "SLO", "NOP", "ORA", "ASL", "SLO", "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO",
/* 0x1L */  "BPL", "ORA", "JAM", "SLO", "NOP", "ORA", "ASL", "SLO", "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO",
/* 0x2L */  "JSR", "AND", "JAM", "RLA", "BIT", "AND", "ROL", "RLA", "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA",
/* 0x3L */  "BMI", "AND", "JAM", "RLA", "NOP", "AND", "ROL", "RLA", "SEC", "AND", "NOP", "RLA", "NOP", "AND", "ROL", "RLA",
/* 0x4L */  "RTI", "EOR", "JAM", "SRE", "NOP", "EOR", "LSR", "SRE", "PHA", "EOR", "LSR", "ASR", "JMP", "EOR", "LSR", "SRE",
/* 0x5L */  "BVC", "EOR", "JAM", "SRE", "NOP", "EOR", "LSR", "SRE", "CLI", "EOR", "NOP", "SRE", "NOP", "EOR", "LSR", "SRE",
/* 0x6L */  "RTS", "ADC", "JAM", "RRA", "NOP", "ADC", "ROR", "RRA", "PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA",
/* 0x7L */  "BVS", "ADC", "JAM", "RRA", "NOP", "ADC", "ROR", "RRA", "SEI", "ADC", "NOP", "RRA", "NOP", "ADC", "ROR", "RRA",
/* 0x8L */  "NOP", "STA", "NOP", "SAX", "STY", "STA", "STX", "SAX", "DEY", "NOP", "TXA", "ANE", "STY", "STA", "STX", "SAX",
/* 0x9L */  "BCC", "STA", "JAM", "SHA", "STY", "STA", "STX", "SAX", "TYA", "STA", "TXS", "SHS", "SHY", "STA", "SHX", "SHA",
/* 0xAL */  "LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX", "TAY", "LDA", "TAX", "LXA", "LDY", "LDA", "LDX", "LAX",
/* 0xBL */  "BCS", "LDA", "JAM", "LAX", "LDY", "LDA", "LDX", "LAX", "CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX",
/* 0xCL */  "CPY", "CMP", "NOP", "DCP", "CPY", "CMP", "DEC", "DCP", "INY", "CMP", "DEX", "SBX", "CPY", "CMP", "DEC", "DCP",
/* 0xDL */  "BNE", "CMP", "JAM", "DCP", "NOP", "CMP", "DEC", "DCP", "CLD", "CMP", "NOP", "DCP", "NOP", "CMP", "DEC", "DCP",
/* 0xEL */  "CPX", "SBC", "NOP", "ISB", "CPX", "SBC", "INC", "ISB", "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISB",
/* 0xFL */  "BEQ", "SBC", "JAM", "ISB", "NOP", "SBC", "INC", "ISB", "SED", "SBC", "NOP", "ISB", "NOP", "SBC", "INC", "ISB"
    };

    /**
     * Addressing modes indexed by the opcode.
     */
    public static final String[] addressing = {
/*  H/L      0xH0   0xH1   0xH2   0xH3   0xH4   0xH5   0xH6   0xH7   0xH8   0xH9   0xHA   0xHB   0xHC   0xHD   0xHE   0xHF */
/* 0x0L */  "imp", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0x1L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x2L */  "abs", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0x3L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x4L */  "imp", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0x5L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x6L */  "imp", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "ind", "abs", "abs", "abs",
/* 0x7L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x8L */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0x9L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpy", "zpy", "imp", "aby", "imp", "aby", "abx", "abx", "aby", "aby",
/* 0xAL */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0xBL */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpy", "zpy", "imp", "aby", "imp", "aby", "abx", "abx", "aby", "aby",
/* 0xCL */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0xDL */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0xEL */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0xFL */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx"
    };

    /**
     * All addressing modes formats.
     */
    public static final Map<String, Mode> format = new HashMap<String, Mode>() {{
        put("abs", new Mode(2, "$%02X%02X"));
        put("abx", new Mode(2, "$%02X%02X,X"));
        put("aby", new Mode(2, "$%02X%02X,Y"));
        put("imm", new Mode(1, "#$%02X"));
        put("imp", new Mode(0, ""));
        put("ind", new Mode(2, "($%02X%02X)"));
        put("izx", new Mode(1, "($%02X,X)"));
        put("izy", new Mode(1, "($%02X),Y"));
        put("rel", new Mode(1, "$%02X"));
        put("zpg", new Mode(1, "$%02X"));
        put("zpx", new Mode(1, "$%02X,X"));
        put("zpy", new Mode(1, "$%02X,Y"));
    }};

    /**
     *
     */
    private final Peek peek;
    /**
     * Functor for writing to the memory.
     */
    private final Poke poke;
    /**
     * Supplies the monitor with string containing the processor status registry.
     */
    private final Supplier<String> reg;
    /**
     * Current addressing mode.
     */
    private Mode mode = null;

    /**
     * Initializes the assembly language monitor.
     * @param peek functor for reading the memory
     * @param poke functor for writing to the memory
     * @param reg supplier which returns current processor status registry
     */
    public Monitor(Peek peek, Poke poke, Supplier<String> reg) {
        this.peek = peek;
        this.poke = poke;
        this.reg = reg;
    }

    /**
     * Dumps the memory from specified address into the string.
     * @param start the starting address
     * @return dump of the memeory
     */
    public String memory(int start) {
        StringBuilder ascii = new StringBuilder();
        StringBuilder sb = new StringBuilder(":");

        sb.append(String.format("%04X ", start));
        for (int i = 0; i < 8; i++) {
            byte b = peek.get((short) (start + i));
            sb.append(String.format("%02X ", b));
            //ascii.append(Character.isAlphabetic(b) ? Character.toString(b) : ".");
            ascii.append(Character.isAlphabetic(b) ? new String(new byte[] { b }, 0, 1) : ".");
        }

        return sb.append(ascii.toString()).toString();
    }

    /**
     * Formats instruction found at given address into assembly language.
     * @param address memory location
     * @return assembly language line
     */
    public String line(int address) {
        StringBuilder sb = new StringBuilder();

        byte op = peek.get((short) address);
        sb.append(String.format(",%04X  %02X ", address & 0xFFFF, op));
        mode = format.get(addressing[op & 0xFF]);
        for (int i = 1; i <= mode.bytes; i++)
            sb.append(String.format("%02X ", peek.get((short) (address + i))));
        //sb.append("   ".repeat(2 - mode.bytes));
        sb.append(String.join("", Collections.nCopies(2 - mode.bytes, "   ")));
        sb.append(" ").append(opcode[op & 0xFF]).append(" ");
        sb.append(String.format(mode.format,
                peek.get((short) (address + 1 + (mode.bytes == 2 ? 1 : 0))), peek.get((short) (address + 1))));

        return sb.toString();
    }

    /**
     * Prints out whole memory.
     */
    public void dump() {
        dump(0x0000);
    }

    /**
     * Prints out the memory from specified location.
     * @param from starting location
     */
    public void dump(int from) {
        dump(from, 0xFFFF);
    }

    /**
     * Prints out the memory from specified range.
     * @param from starting location
     * @param to ending location
     */
    public void dump(int from, int to) {
        for (int a = from; a <= to; a++) {
            System.out.println(line(a));
            a += mode.bytes;
        }
    }

    /**
     * Formats specified address into assembly language.
     * @param address the memory location
     * @return assembly language line
     */
    public String walk(int address) {
        StringBuilder sb = new StringBuilder();
        byte op = peek.get((short) address);
        mode = format.get(addressing[op & 0xFF]);

        sb.append(" ").append(reg.get());
        sb.append("  ").append(opcode[op & 0xFF]).append(" ");
        sb.append(String.format(mode.format,
                peek.get((short) (address + 1 + (mode.bytes == 2 ? 1 : 0))), peek.get((short) (address + 1))));

        return sb.toString();
    }
}

/* addressing modes

abs     2   OPC $LLHH       "$%02X%02X"
abx	    2   OPC $LLHH,X     "$%02X%02X,X"
aby     2   OPC $LLHH,Y     "$%02X%02X,Y"
imm     1   OPC #$BB        "#$%02X"
imp     0   OPC             ""
ind     2   OPC ($LLHH)     "($%02X%02X)"
inx     1   OPC ($LL,X)     "($%02X,X)"
iny     1   OPC ($LL),Y     "($%02X),Y"
rel     1   OPC $BB         "$%02X"
zpg     1   OPC $LL         "$%02X"
zpx	    1   OPC $LL,X       "$%02X,X"
zpy     1   OPC $LL,Y       "$%02X,Y"
 */
