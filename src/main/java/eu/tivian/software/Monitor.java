package eu.tivian.software;

import eu.tivian.hardware.CPU;

import java.util.HashMap;
import java.util.Map;

public class Monitor {
    public static class Mode {
        int bytes;
        String format;

        public Mode(int bytes, String format) {
            this.bytes = bytes;
            this.format = format;
        }
    }

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

    public static final Map<String, Mode> format = new HashMap<>() {{
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

    /*private CPU cpu;
    private Mode mode = null;

    public Monitor(CPU cpu) {
        this.cpu = cpu;
    }

    public String memory(int start) {
        var ascii = new StringBuilder();
        var sb = new StringBuilder(":");

        sb.append(String.format("%04X ", start));
        for (int i = 0; i < 8; i++) {
            byte b = cpu.peek(start + i);
            sb.append(String.format("%02X ", b));
            ascii.append(Character.isAlphabetic(b) ? Character.toString(b) : ".");
        }

        return sb.append(ascii.toString()).toString();
    }

    public String line() {
        return line(cpu.lastPC());
    }

    public String line(int address) {
        var sb = new StringBuilder();

        byte op = cpu.peek(address);
        sb.append(String.format(",%04X  %02X ", address & 0xFFFF, op));
        mode = format.get(addressing[op & 0xFF]);
        for (int i = 1; i <= mode.bytes; i++)
            sb.append(String.format("%02X ", cpu.peek(address + i)));
        sb.append("   ".repeat(2 - mode.bytes));
        sb.append(" " + opcode[op & 0xFF] + " ");
        sb.append(String.format(mode.format,
                cpu.peek(address + 1 + (mode.bytes == 2 ? 1 : 0)), cpu.peek(address + 1)));

        return sb.toString();
    }

    public void dump() {
        dump(0x0000);
    }

    public void dump(int from) {
        dump(from, 0xFFFF);
    }

    public void dump(int from, int to) {
        for (int a = from; a <= to; a++) {
            System.out.println(line(a));
            a += mode.bytes;
        }
    }*/
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
