package eu.tivian.software;

import eu.tivian.hardware.MOS8501;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
     * Addressing mode formatter.
     */
    public static class Mode {
        /**
         * Name (abbreviation) of the addressing mode.
         */
        String name;
        /**
         * Number of bytes taken by the addressing mode.
         */
        int bytes;
        /**
         * Format string for the addressing mode.
         */
        String format;
        /**
         * Regex for identifying if given string matches any of the addressing modes.
         */
        String regex;

        /**
         * All possible addressing modes.
         */
        private static final Set<Mode> modes = new HashSet<>() {{
            add(new Mode("abs", 2, "$%02X%02X"  , "^[A-F\\d]{3,4}$"        ));
            add(new Mode("abx", 2, "$%02X%02X,X", "^[A-F\\d]{3,4},X$"      ));
            add(new Mode("aby", 2, "$%02X%02X,Y", "^[A-F\\d]{3,4},Y$"      ));
            add(new Mode("acc", 0, "A"          , "^A$"                    ));
            add(new Mode("imm", 1, "#$%02X"     , "^#[A-F\\d]{1,2}$"       ));
            add(new Mode("imp", 0, ""           , ""                       ));
            add(new Mode("ind", 2, "($%02X%02X)", "^\\([A-F\\d]{1,4}\\)$"  ));
            add(new Mode("izx", 1, "($%02X,X)"  , "^\\([A-F\\d]{1,2},X\\)$"));
            add(new Mode("izy", 1, "($%02X),Y"  , "^\\([A-F\\d]{1,2}\\),Y$"));
            add(new Mode("rel", 1, "%X"         , "^[A-F\\d]{1,2}$"        ));
            add(new Mode("zpg", 1, "$%02X"      , "^[A-F\\d]{1,2}$"        ));
            add(new Mode("zpx", 1, "$%02X,X"    , "^[A-F\\d]{1,2},X$"      ));
            add(new Mode("zpy", 1, "$%02X,Y"    , "^[A-F\\d]{1,2},Y$"      ));
        }};

        /**
         * Initializes the formatter.
         *
         * @param name the abbreviation of the addressing mode
         * @param bytes number of bytes taken by the addressing mode
         * @param format format string
         * @param regex regex which matches the format of the addressing mode
         */
        public Mode(String name, int bytes, String format, String regex) {
            this.name = name;
            this.bytes = bytes;
            this.format = format;
            this.regex = regex;
        }

        /**
         * Transforms current instruction found at {@code PC} into {@link String}.
         * @param PC program counter
         * @return appropriate string representation of addressing mode present at {@code PC}
         */
        public String toString(int PC) {
            Byte[] operands = new Byte[bytes];
            for (int i = 0; i < bytes; i++)
                operands[i] = peek(PC + bytes - i);

            return String.format(format, name.equals("rel") ?
                new Object[] { (PC + operands[0] + 2) & 0xFFFF } : operands);
        }

        /**
         * Parses the instruction with appropriate addressing mode.
         * @param instr mnemonic
         * @param str string containing the addressing mode we're looking for
         * @return the addressing mode object
         */
        public static Mode parse(String instr, String str) {
            if (instr.charAt(0) == 'B' && !instr.equals("BIT") && !instr.equals("BRK"))
                return find("rel");

            str = str.replaceAll("[\\s$]", "");
            for (Mode mode : modes)
                if (!mode.name.equals("rel") && Pattern.matches(mode.regex, str))
                    return mode;

            return null;
        }

        /**
         * Finds the appropriate addressing mode for given abbreviation.
         * @param name addressing mode abbreviation
         * @return the addressing mode object
         */
        public static Mode find(String name) {
            for (Mode mode : modes)
                if (mode.name.equals(name))
                    return mode;

            return null;
        }
    }

    /**
     * Mnemonics indexed by the opcode.
     */
    public static final List<String> mnemonic = Arrays.asList(
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
    );

    /**
     * Addressing modes indexed by the opcode.
     */
    public static final List<String> addressing = Arrays.asList(
/*  H/L      0xH0   0xH1   0xH2   0xH3   0xH4   0xH5   0xH6   0xH7   0xH8   0xH9   0xHA   0xHB   0xHC   0xHD   0xHE   0xHF */
/* 0x0L */  "imp", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "acc", "imm", "abs", "abs", "abs", "abs",
/* 0x1L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x2L */  "abs", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "acc", "imm", "abs", "abs", "abs", "abs",
/* 0x3L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x4L */  "imp", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "acc", "imm", "abs", "abs", "abs", "abs",
/* 0x5L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x6L */  "imp", "izx", "imp", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "acc", "imm", "ind", "abs", "abs", "abs",
/* 0x7L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0x8L */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0x9L */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpy", "zpy", "imp", "aby", "imp", "aby", "abx", "abx", "aby", "aby",
/* 0xAL */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0xBL */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpy", "zpy", "imp", "aby", "imp", "aby", "abx", "abx", "aby", "aby",
/* 0xCL */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0xDL */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx",
/* 0xEL */  "imm", "izx", "imm", "izx", "zpg", "zpg", "zpg", "zpg", "imp", "imm", "imp", "imm", "abs", "abs", "abs", "abs",
/* 0xFL */  "rel", "izy", "imp", "izy", "zpx", "zpx", "zpx", "zpx", "imp", "aby", "imp", "aby", "abx", "abx", "abx", "abx"
    );

    /**
     * Instructions which breaks the flow of the program.
     */
    public static final List<String> jumping = Arrays.asList(
        "BRK", "JMP", "RTI", "RTS"
    );

    /**
     * Text with all available commands of this assembly language monitor.
     */
    public static final String helpStr =
        " Available commands:\n" +
        "\tA XXXX             - Assembly code starting at XXXX\n" +
        "\tD XXXX             - Disassemble the program starting at XXXX\n" +
        "\tM XXXX [YYYY]      - Display the memory contents from XXXX to YYYY\n" +
        "\tC XXXX YY [ZZ ...] - Change the memory starting at XXXX with values YY, ZZ, ...\n" +
        "\tF XXXX YYYY XX     - Fill the memory range XXXX to YYYY with the value XX\n" +
        "\tR                  - Show the CPU registry\n" +
        "\tR(A,X,Y,S,P) XX    - Change registry\n" +
        "\tG [XXXX]           - Execute the machine program at XXXX or current PC\n" +
        "\tW [XXXX]           - Walk one instruction at the time from XXXX or current PC\n" +
        "\tP [STEP]           - Walk through the power-up sequence\n" +
        "\tL [XXXX] \"PATH\"    - Load file [into the specified location]\n" +
        "\tS \"PATH\"           - Dump whole memory to specified file\n" +
        "\tV                  - Show CPU hardware vectors\n" +
        "\tY                  - Display number of cycles\n" +
        "\t#DDDDD             - Decimal conversion\n" +
        "\t$XXXX              - Hexadecimal conversion\n" +
        "\t%BBBBBBBB          - Binary conversion\n" +
        "\tH                  - Display this message\n" +
        "\tX                  - Exit machine language monitor.";

    /**
     * Stripped-down version of the CPU without need for external signals.
     */
    private static SimpleCPU cpu = null;

    /**
     * Command-line reader.
     */
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * List of ROM images used for default memory loading.
     */
    private static final Map<Integer, String> defaultRoms = new HashMap<>() {{
        put(0x8000, "/roms/basic.318006-01.bin");
        put(0xC000, "/roms/kernal.318004-05.bin");
    }};

    /**
     * Helper function, which tells if the character is within printable range.
     * @param ch the character to test
     * @return true if {@code ch} &ge; 32 &and; {@code ch} &lt; 127.
     */
    private static boolean isASCII(char ch) {
        return ch >= 32 && ch < 127;
    }

    /**
     * Read one line from command-line console.
     * @return one line of text without line feed
     */
    private static String readLine() {
        try {
            return reader.readLine();
        } catch (IOException ex) {
            return "";
        }
    }

    /**
     * Peeks into the memory.
     * @param address the memory cell index
     * @return value at specified location
     */
    private static byte peek(int address) {
        return cpu.memory[address & 0xFFFF];
    }

    /**
     * Pokes value into the memory.
     * @param address the memory cell index
     * @param value value to write
     */
    private static void poke(int address, int value) {
        cpu.memory[address & 0xFFFF] = (byte) (value & 0xFF);
    }

    /**
     * Shows the memory in hexdump format.
     * @param from starting position
     * @param to last position
     * @param width the number of bytes displayed in one line
     * @param loop {@code true} if the function should show memory until user doesn't enter empty line
     */
    private static void showMemory(int from, int to, int width, boolean loop) {
        StringBuilder ascii = new StringBuilder();

        if (loop)
            to = from + width;

        do {
            for (int i = from; i < to; i += width) {
                ascii.setLength(0);
                ascii.append("  ");

                System.out.printf(":%04X  ", i % 0x10000);
                for (int j = 0; j < width; j++) {
                    byte b = peek((i + j) % 0x10000);
                    ascii.append(isASCII((char) b) ? (char) b : '.');
                    System.out.printf("%02X ", b);
                }
                System.out.print(ascii.toString());

                if (!loop)
                    System.out.println();
            }

            from += width;
            to += width;
        } while (loop && readLine().isEmpty());
    }

    /**
     * Changes memory starting from specified location with supplied values.
     * @param from starting position
     * @param values the values to write
     */
    private static void changeMemory(int from, int... values) {
        for (int i = 0; i < values.length; i++)
            poke(from + i, values[i]);
    }

    /**
     * Fills memory in specified range with value.
     * @param from starting position
     * @param to last position
     * @param value value to fill memory with
     */
    private static void fillMemory(int from, int to, byte value) {
        for (int i = 0; i < to; i++)
            poke(from + i, value);
    }

    /**
     * Walks through the memory one instruction at the time.
     * @param start starting position
     */
    private static void walk(short start) {
        walk(start, -1);
    }

    /**
     * Walks through the memory {@code step} instructions at the time.
     * @param start starting position
     * @param step number of instructions after which the monitor will wait for user input
     */
    private static void walk(short start, int step) {
        int counter = 0;
        boolean auto = step >= 0;

        cpu.counter(start);
        do {
            do
                cpu.step();
            while (cpu.stage() != MOS8501.Stage.FETCH);

            Mode mode = Mode.find(addressing.get(cpu.opcode() & 0xFF));
            int PC = (cpu.counter() - 1) & 0xFFFF;
            System.out.printf(" %04X %s  %s %s", PC, cpu.reg(), cpu.mnemonic(), mode.toString(PC));

            if (auto) {
                if (++counter >= step) {
                    counter = 0;
                    if (!readLine().isEmpty())
                        break;
                } else {
                    System.out.println();
                }
            }
        } while (auto || readLine().length() == 0);

        System.out.println("\n" + cpu);
    }

    /**
     * Executes the code found at the specified location.
     * @param start starting position
     */
    private static void run(short start) {
        cpu.counter(start);
        do
            cpu.step();
        while (cpu.stage() != MOS8501.Stage.FETCH || !cpu.mnemonic().equals("BRK"));

        System.out.println(cpu);
    }

    /**
     * Assembles the machine language code starting at specified location.
     * @param start starting location
     */
    private static void assemble(short start) {
        String asm;
        Matcher matcher;
        short PC = start;

        do {
            System.out.printf(" %04X ", PC);
            asm = readLine().toUpperCase();
            matcher = asmRegex.matcher(asm);

            if (matcher.matches()) {
                String instr = matcher.group(1);
                String addr = matcher.group(2);
                String rawOp = matcher.group(3);
                int opcode = -1;
                int operand = rawOp == null ? -1 : Integer.parseInt(rawOp, 16);
                Mode mode = Mode.parse(instr, addr == null ? "" : addr);

                if (!mnemonic.contains(instr)) {
                    System.out.println("\tUnknown instruction.");
                } else if (mode == null) {
                    System.out.println("\tUnknown addressing mode.");
                } else {
                    for (int i = 0; i < 256; i++) {
                        if (mnemonic.get(i).equals(instr) && addressing.get(i).equals(mode.name)) {
                            opcode = i;
                            break;
                        }
                    }

                    if (opcode != -1) {
                        if (mode.name.equals("rel")) {
                            short rel = (short) (operand - PC - 1);

                            if (rel >= -128 && rel < 127) {
                                poke(PC++, opcode);
                                poke(PC++, rel & 0xFF);
                            } else {
                                System.out.printf("\tThe offset is too big. (%d)\n", rel);
                            }
                        } else if (mode.bytes >= 1) {
                            poke(PC++, opcode);
                            poke(PC++, operand & 0xFF);

                            if (mode.bytes == 2)
                                poke(PC++, operand >> 8);
                        }
                    } else {
                        System.out.println("\tInvalid addressing mode.");
                    }
                }
            } else if (!asm.isEmpty()) {
                System.out.println("\tInvalid format!");
            }
        } while (!asm.isEmpty());
    }

    /**
     * Disassembles the machine code starting from the specified location.
     * @param start starting position
     */
    private static void disassemble(short start) {
        short PC = start;

        do {
            byte opcode = peek(PC);
            String instr = mnemonic.get(opcode & 0xFF);
            String modeName = addressing.get(opcode & 0xFF);
            Mode mode = Mode.find(modeName);

            System.out.printf(",%04X  %02X", PC, opcode & 0xFF);
            for (int i = 0; i < mode.bytes; i++)
                System.out.printf(" %02X", peek(PC + i + 1));
            System.out.print("   ".repeat(2 - mode.bytes));
            System.out.printf("  %s %s", instr, mode.toString(PC));

            if (jumping.contains(instr))
                System.out.print("\n" + "-".repeat(32));

            PC += mode.bytes + 1;
        } while (readLine().isEmpty());
    }

    /**
     * Loads data from specified file or resource into the memory starting at specified address.
     * @param from starting position
     * @param pathStr path to file or resource
     * @return the size of loaded data
     */
    private static int loadMemory(int from, String pathStr) {
        int size = -1;
        Path path = null;

        try {
            path = Path.of(Monitor.class.getResource(pathStr).toURI());
        } catch (URISyntaxException | FileSystemNotFoundException e) {
            try {
                return loadMemory(from, Monitor.class.getResourceAsStream(pathStr).readAllBytes());
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }

        if (path != null && !Files.exists(path)) {
            System.out.printf("File \"%s\" doesn't exist!\n", path);
        } else {
            try {
                return loadMemory(from, Files.readAllBytes(path));
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }

        return size;
    }

    /**
     * Loads raw byte data into the memory starting at specified location.
     * @param from starting position
     * @param data data to write
     * @return the size of loaded data
     */
    private static int loadMemory(int from, byte[] data) {
        int size = Math.min(cpu.memory.length - from, data.length);
        if (size >= 0)
            System.arraycopy(data, 0, cpu.memory, from, size);
        return size;
    }

    /**
     * Saves whole 64KB of memory into specified file.
     * @param path path to file
     */
    private static void saveMemory(Path path) {
        try {
            Files.write(path, cpu.memory, StandardOpenOption.CREATE);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Sets the hardware vector to specified address.
     * @param address address of the vector
     * @param vector address at which the vector should point
     */
    private static void setVector(short address, short vector) {
        poke(address, vector & 0xFF);
        poke(address + 1, vector >> 8);
    }

    /**
     * Gets the hardware vector from specified address.
     * @param address address of the vector
     * @return vector address
     */
    private static short getVector(short address) {
        return (short) (peek(address) | peek(address + 1) << 8);
    }

    /**
     * Regex which matches all possible formats of assembly language instruction in the CPU.
     */
    private static final Pattern asmRegex = Pattern.compile(
        "^([A-Z]{3})\\s*(A|\\s*\\(?\\s*#?\\$?([A-F\\d]{1,4})(\\s*,\\s*([XY]))?\\s*\\)?\\s*(\\s*,\\s*([XY]))?)?$");

    /**
     * List of all available commands with corresponding regex patterns.
     */
    private static final Map<Character, Pattern> commands = new HashMap<>() {{
        put('M', Pattern.compile("^M\\s*([A-F\\d]{1,4})(\\s+([A-F\\d]{1,4}))?$"));
        put('F', Pattern.compile("^F\\s*([A-F\\d]{1,4})\\s+([A-F\\d]{1,4})\\s+([A-F\\d]{1,2})$"));
        put('C', Pattern.compile("^C\\s*([A-F\\d]{1,4})((\\s+[A-F\\d]{1,2})+)$"));
        put('G', Pattern.compile("^G\\s*([A-F\\d]{1,4})?$"));
        put('W', Pattern.compile("^W\\s*([A-F\\d]{1,4})?$"));
        put('A', Pattern.compile("^A\\s*([A-F\\d]{1,4})$"));
        put('D', Pattern.compile("^D\\s*([A-F\\d]{1,4})$"));
        put('R', Pattern.compile("^R([AXYSP])\\s*([A-F\\d]{1,2})$"));
        put('L', Pattern.compile("^L\\s*([A-F\\d]{1,4})?\\s*\"([^\"]+)\"$"));
        put('S', Pattern.compile("^S\\s*\"([^\"]+)\"$"));
        put('V', Pattern.compile("^V([IR])\\s*([A-F\\d]{1,4})$"));
        put('#', Pattern.compile("^#(\\d{1,5})$"));
        put('$', Pattern.compile("^\\$(?=([A-F\\d]*)$)(?:.{2}|.{4})$"));
        put('%', Pattern.compile("^%([01]{8})$"));
        put('P', Pattern.compile("^P\\s*(\\d+)$"));
        put('Y', Pattern.compile(""));
        put('H', Pattern.compile(""));
        put('X', Pattern.compile(""));
    }};

    /**
     * List of all numeric conversions symbols with corresponding numeric bases.
     * <br><ul>
     *     <li>% &rarr; binary</li>
     *     <li># &rarr; decimal</li>
     *     <li>$ &rarr; hexadecimal</li>
     * </ul>
     */
    private static final Map<Character, Integer> conversion = new HashMap<>() {{
        put('%',  2);
        put('#', 10);
        put('$', 16);
    }};

    /**
     * Starts the machine language monitor. Waits for user input.
     */
    public static void start() {
        String line;
        boolean running = true;
        Matcher matcher = null;
        Pattern pattern = null;
        cpu = new SimpleCPU();

        cpu.start();
        while (running) {
            System.out.print("> ");
            line = readLine().toUpperCase();
            if (line.isEmpty())
                continue;

            pattern = commands.get(line.charAt(0));
            if (pattern == null) {
                System.out.println("\tUnknown command!\n\t Enter h for help.");
                continue;
            } else {
                matcher = pattern.matcher(line);
            }

            switch (line.charAt(0)) {
                case 'M':
                    if (matcher.matches()) {
                        boolean loop = matcher.group(2) == null;
                        int from = Integer.parseInt(matcher.group(1), 16);
                        int to = loop ? from + 16 : Integer.parseInt(matcher.group(3), 16);

                        showMemory(from, to, 16, loop);
                    } else {
                        System.out.println("\tInvalid format!\n\t Correct format is: M AAAA [BBBB]");
                    }
                    break;

                case 'F':
                    if (matcher.matches()) {
                        int from = Integer.parseInt(matcher.group(1), 16);
                        int to = Integer.parseInt(matcher.group(2), 16);
                        int operand = Integer.parseInt(matcher.group(3), 16);
                        if (operand > 255) {
                            System.out.println("\tOperand cannot be greater than 255!");
                            continue;
                        }

                        fillMemory(from, to, (byte) operand);
                        System.out.printf("\tMemory filled with $%02X in range [$%04X, $%04X].\n", operand, from, to);
                    } else {
                        System.out.println("\tInvalid format!\n\t Correct format is: F AAAA BBBB XX");
                    }
                    break;

                case 'C':
                    if (matcher.matches()) {
                        int[] values = Stream.of(matcher.group(2).trim().split(" "))
                            .mapToInt(val -> Integer.parseInt(val, 16)).toArray();
                        int from = Integer.parseInt(matcher.group(1), 16);

                        changeMemory(from, values);
                        System.out.printf("\tMemory changed in range [$%04X, $%04X].\n",
                            from, from + values.length - 1);
                    } else {
                        System.out.println("\tInvalid format!\n\t Correct format is: C AAAA XX [YY ...]");
                    }
                    break;

                case 'G':
                    run(matcher.matches() ? (short) Integer.parseInt(matcher.group(1), 16) : cpu.counter());
                    break;

                case 'W':
                    walk(matcher.matches() ? (short) Integer.parseInt(matcher.group(1), 16) : cpu.counter());
                    break;

                case 'A':
                    if (matcher.matches())
                        assemble((short) Integer.parseInt(matcher.group(1), 16));
                    else
                        System.out.println("\tInvalid format!\n\t Correct format is: A AAAA");
                    break;

                case 'D':
                    if (matcher.matches())
                        disassemble((short) Integer.parseInt(matcher.group(1), 16));
                    else
                        System.out.println("\tInvalid format!\n\t Correct format is: D AAAA");
                    break;

                case 'R':
                    if (matcher.matches()) {
                        int operand = Integer.parseInt(matcher.group(2), 16);
                        if (operand > 255) {
                            System.out.println("\tOperand cannot be greater than 255!");
                            continue;
                        }

                        cpu.setReg(matcher.group(1), (byte) operand);
                    }

                    System.out.println(cpu);
                    break;

                case 'L':
                    if (matcher.matches()) {
                        int from = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1), 16);
                        int size = loadMemory(from, matcher.group(2));
                        System.out.printf("\tLoaded file to locations in range [$%04X, $%04X].\n",
                            from, from + size - 1);
                    } else if (line.charAt(1) == 'D' && line.length() == 2) {
                        for (var rom : defaultRoms.entrySet())
                            loadMemory(rom.getKey(), rom.getValue());
                    } else {
                        System.out.println("\tInvalid format!\n\t Correct format is: L AAAA \"path\"");
                    }
                    break;

                case 'S':
                    if (matcher.matches()) {
                        saveMemory(Path.of(matcher.group(1)));
                        System.out.println("\tMemory dumped.");
                    } else {
                        System.out.println("\tInvalid format!\n\t Correct format is: S \"path\"");
                    }
                    break;

                case 'V':
                    if (matcher.matches()) {
                        short vector = (short) (Integer.parseInt(matcher.group(2), 16));
                        boolean isReset = matcher.group(1).equals("R");
                        short address = isReset ? MOS8501.RESET_VECT : MOS8501.IRQ_VECT;
                        setVector(address, vector);
                        System.out.printf("\tChanged %s vector to $%04X address.\n",
                            isReset ? "RESET" : "IRQ  ", vector);
                    } else {
                        System.out.printf("\tRESET = $%04X\n\tIRQ   = $%04X\n",
                            getVector(MOS8501.RESET_VECT), getVector(MOS8501.IRQ_VECT));
                    }
                    break;

                case '%': // binary
                case '$': // hex
                case '#': // decimal
                    if (matcher.matches()) {
                        int operand = Integer.parseInt(matcher.group(1), conversion.get(line.charAt(0))) % 0x10000;
                        String bin = String.format("%8s", Integer.toBinaryString(operand & 0xFF))
                            .replace(' ', '0');
                        if (operand < 0x100)
                            System.out.printf("  %02X %s  %-3d\n", operand, bin, operand);
                        else
                            System.out.printf("%04X  %-5d\n", operand, operand);
                    }
                    break;

                case 'P':
                    walk(getVector(MOS8501.RESET_VECT), matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1);
                    break;

                case 'Y':
                    System.out.printf("\tCycles: %d\n", cpu.cycles());
                    break;

                case 'H':
                    System.out.println(helpStr);
                    break;

                case 'X':
                    running = false;
                    break;
            }
        }
    }
}
