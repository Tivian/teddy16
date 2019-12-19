package eu.tivian.hardware;

import eu.tivian.hardware.logic.*;
import eu.tivian.other.Logger;
import eu.tivian.other.SI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Motherboard {
    // Commodore 16 specific chips
    private final MOS8501 cpu;       // U2
    private final TED     ted;       // U1
    private final RAM     ram1;      // U5
    private final RAM     ram2;      // U6
    private final ROM     basic;     // U3
    private final ROM     kernal;    // U4
    private final PLA     pla;       // U16

    // Logic chips
    private final SystemClock     clock;     // Y1
    private final MonostableTimer timer;     // U10
    private final IC74LS257       ramMux1;   // U7
    private final IC74LS257       ramMux2;   // U8
    private final IC7406          invhex;    // U9
    private final IC74LS125       buffer;    // U11
    private final IC74LS02        norGate;   // U12
    private final IC74LS175       flipFlop;  // U15
    private final IC74LS139       demux;     // U14
    private final MOS6529         keyPort;   // U13

    // Physical layer
    private final Keyboard keyboard;

    private final Connector expansion;
    private final Connector cassette;
    private final Connector joy1;
    private final Connector joy2;
    private final Connector serial;
    private final Connector power;

    private final Pin VCC;
    private final Pin GND;

    private final Switch powerSw;
    private final Switch resetSw;

    private boolean running = true;

    public Motherboard() {
        this.cpu    = new MOS8501();
        this.ted    = new TED();
        this.ram1   = new RAM("RAM low ", 8, 4, 0x4000);
        this.ram2   = new RAM("RAM high", 8, 4, 0x4000);
        this.basic  = new ROM("BASIC", 0x4000);
        this.kernal = new ROM("KERNAL", 0x4000);
        this.pla    = new PLA();

        this.clock    = new SystemClock();
        this.timer    = new MonostableTimer(47 * SI.KILO, 10 * SI.MICRO);
        this.ramMux1  = new IC74LS257();
        this.ramMux2  = new IC74LS257();
        this.invhex   = new IC7406();
        this.buffer   = new IC74LS125();
        this.norGate  = new IC74LS02();
        this.flipFlop = new IC74LS175();
        this.demux    = new IC74LS139();
        this.keyPort  = new MOS6529();

        this.keyboard = new Keyboard();

        this.expansion = new Connector("CN1", Connector.Gender.FEMALE);
        this.cassette  = new Connector("CN3", Connector.Gender.FEMALE);
        this.joy1      = new Connector("CN4", Connector.Gender.FEMALE);
        this.joy2      = new Connector("CN5", Connector.Gender.FEMALE);
        this.serial    = new Connector("CN7", Connector.Gender.FEMALE);
        this.power     = new Connector("CN8", Connector.Gender.FEMALE, Pin.VCC, Pin.GND);

        this.VCC = new Pin("VCC", Pin.Direction.INPUT);
        this.GND = new Pin("GND", Pin.Direction.INPUT);

        this.powerSw = new Switch("SW1", VCC, power.get(0));
        this.resetSw = new Switch("SW2", GND, timer.trigger);

        if (Logger.ENABLE)
            Logger.info("Creating motherboard");

        try {
            byte[] buffer = new byte[0x4000];

            if (Logger.ENABLE)
                Logger.info("Loading KERNAL ROM");
            getClass().getResourceAsStream("/roms/kernal.318004-05.bin").read(buffer);
            kernal.preload(buffer);

            if (Logger.ENABLE)
                Logger.info("Loading BASIC ROM");
            getClass().getResourceAsStream("/roms/basic.318006-01.bin").read(buffer);
            basic.preload(buffer);
        } catch (IOException ex) {
            if (Logger.ENABLE)
                Logger.error("Cannot read the ROM files");
            System.exit(1);
        }

        VCC.onChange(() -> running = VCC.level() == Pin.Level.HIGH);

        if (Logger.ENABLE)
            Logger.info("Making connections between ICs");

        // NE555 timer connections [U10]
        timer.output.connect(invhex.get(0).inputA); // /Reset line
        timer.output.connect(invhex.get(1).inputA); // serial bus reset

        // CPU connections [U2]
        cpu.reset.connect(invhex.get(0).output).pullUp();
        cpu.aec.connect(ted.aec);
        cpu.rdy.connect(ted.ba);
        cpu.irq.connect(ted.irq).pullUp();
        cpu.phi0.connect(ted.phiOut);
        cpu.port.get(0).connect(invhex.get(5).inputA);
        cpu.port.get(1).connect(invhex.get(4).inputA);
        cpu.port.get(2).connect(invhex.get(3).inputA);
        //cpu.port.get(3).connect(); // cassette motor
        //cpu.port.get(4).connect(); // cassette read
        //cpu.port.get(6).connect().pullUp(true); // cassette write
        //cpu.port.get(7).connect().pullUp(true); // ???
        cpu.gate.connect(ted.mux)
            .connect(ramMux1.select).connect(ramMux2.select);
        cpu.rw.connect(ted.rw).connect(keyPort.rw)
            .connect(ram1.rw).connect(ram2.rw);
        cpu.data.connect(keyPort.data).connect(ted.data)
            .connect(basic.data).connect(kernal.data)
            .connect(ram1.data, i -> i > 3 ? -1 : i)
            .connect(ram2.data, i -> i < 4 ? -1 : i - 4);
        cpu.address.connect(ted.address)
            .connect(basic.address, i -> i > 13 ? -1 : i)
            .connect(kernal.address, i -> i > 13 ? -1 : i);

        // TED connections [U1]
        ted.mux.connect(pla.input.get(8));
        ted.phiIn.connect(clock.clock);
        ted.cs0.connect(demux.get(0).enable);
        ted.cs1.connect(demux.get(1).enable);
        ted.ras.connect(ram1.ras).connect(ram2.ras);
        ted.cas.connect(ram1.cas).connect(ram2.cas);
        ted.keyboard.connect(keyboard.column);

        // BASIC ROM connections [U3]
        basic.cs.get(0).connect(demux.get(0).O0);
        basic.cs.get(1).connect(GND);
        basic.cs.get(2).connect(VCC);

        // KERNAL ROM connections [U4]
        kernal.cs.get(0).connect(demux.get(1).O0);
        kernal.cs.get(1).connect(GND);
        kernal.cs.get(2).connect(VCC);

        // RAM multiplexer 1 [U7]
        ramMux1.get(0).inputA.connect(VCC);
        ramMux1.get(0).inputB.connect(cpu.address.get( 0));
        ramMux1.get(1).inputA.connect(cpu.address.get( 8));
        ramMux1.get(1).inputB.connect(cpu.address.get( 1));
        ramMux1.get(2).inputA.connect(cpu.address.get( 9));
        ramMux1.get(2).inputB.connect(cpu.address.get( 2));
        ramMux1.get(3).inputA.connect(cpu.address.get(10));
        ramMux1.get(3).inputB.connect(cpu.address.get( 3));
        ramMux1.enable.connect(GND);

        // RAM multiplexer 2 [U8]
        ramMux2.get(0).inputA.connect(cpu.address.get(11));
        ramMux2.get(0).inputB.connect(cpu.address.get( 4));
        ramMux2.get(1).inputA.connect(cpu.address.get(12));
        ramMux2.get(1).inputB.connect(cpu.address.get( 5));
        ramMux2.get(2).inputA.connect(cpu.address.get(13));
        ramMux2.get(2).inputB.connect(cpu.address.get( 6));
        ramMux2.get(3).inputA.connect(VCC);
        ramMux2.get(3).inputB.connect(cpu.address.get( 7));
        ramMux2.enable.connect(GND);

        // RAM 1 connections [U5]
        ram1.address.get(0).connect(ramMux1.get(0).output);
        ram1.address.get(1).connect(ramMux1.get(1).output);
        ram1.address.get(2).connect(ramMux1.get(2).output);
        ram1.address.get(3).connect(ramMux1.get(3).output);
        ram1.address.get(4).connect(ramMux2.get(0).output);
        ram1.address.get(5).connect(ramMux2.get(1).output);
        ram1.address.get(6).connect(ramMux2.get(2).output);
        ram1.address.get(7).connect(ramMux2.get(3).output);
        ram1.enable.connect(GND);

        // RAM 2 connections [U6]
        ram2.address.connect(ram1.address);
        ram2.enable.connect(GND);

        // PLA connections [U16]
        pla.input.get( 0).connect(pla.output.get(7));
        pla.input.get( 1).connect(cpu.phi0);
        pla.input.get( 2).connect(cpu.address.get(15));
        pla.input.get( 3).connect(cpu.address.get( 4));
        pla.input.get( 4).connect(cpu.address.get( 5));
        pla.input.get( 5).connect(cpu.address.get( 6));
        pla.input.get( 6).connect(cpu.address.get( 7));
        pla.input.get( 7).connect(cpu.address.get(12));
        pla.input.get( 8).connect(ted.mux);
        pla.input.get( 9).connect(cpu.address.get(14));
        pla.input.get(10).connect(cpu.address.get( 8));
        pla.input.get(11).connect(cpu.address.get( 9));
        pla.input.get(12).connect(cpu.address.get(13));
        pla.input.get(13).connect(cpu.address.get(11));
        pla.input.get(14).connect(cpu.address.get(10));
        pla.input.get(15).connect(ted.ras);
        //pla.output.get(0).connect(); // SCS
        //pla.output.get(1).connect(); // Phi2
        //pla.output.get(2).connect(); // Cassette $FD10 - $FD1F
        //pla.output.get(3).connect(); // $FD00 - $FD0F
        pla.output.get(4).connect(norGate.get(0).inputA);
        pla.output.get(5).connect(keyPort.cs);
        pla.output.get(6).connect(norGate.get(1).inputB)
            .connect(norGate.get(2).inputB);

        // Quad NOR gate connections [U12]
        norGate.get(0).inputB.connect(cpu.rw);
        norGate.get(1).inputA.connect(flipFlop.get(1).revOut);
        norGate.get(2).inputA.connect(flipFlop.get(2).revOut);
        norGate.get(0).output.connect(flipFlop.clock);
        norGate.get(1).output.connect(demux.get(1).A0);
        norGate.get(2).output.connect(demux.get(1).A1);

        // demultiplexer connections [U14]
        demux.get(0).A0.connect(flipFlop.get(0).output);
        demux.get(0).A1.connect(flipFlop.get(3).output);
        //demux.get(0).O2.connect(); // C1 low
        //demux.get(0).O3.connect(); // C2 low
        //demux.get(1).O2.connect(); // C1 high
        //demux.get(1).O3.connect(); // C2 high

        // quad flip-flop connections [U15]
        flipFlop.reset.connect(cpu.reset);
        flipFlop.get(0).input.connect(cpu.address.get(0));
        flipFlop.get(1).input.connect(cpu.address.get(2));
        flipFlop.get(2).input.connect(cpu.address.get(3));
        flipFlop.get(3).input.connect(cpu.address.get(1));

        // keyboard SPI [U13]
        keyPort.port.connect(keyboard.row);

        if (Logger.ENABLE)
            Logger.info("Initializing system clock frequency (4x PAL dot clock, for now)");

        clock.frequency(28.28800 * SI.MEGA);
    }

    public void start() {
        if (Logger.ENABLE)
            Logger.info("Switching on...");

        powerSw.on();
        loop();
    }

    public void stop() {
        if (Logger.ENABLE)
            Logger.info("Switching off...");

        powerSw.off();
        clock.clear();
    }

    private void loop() {
        Pin.Level old = Pin.Level.LOW;

        //log();
        while (running) {
            clock.pulse();
            if (Logger.ENABLE) {
                Pin.Level current = ted.phiOut.level();
                if (current != old) {
                    if (current == Pin.Level.LOW)
                        Logger.info("Current RAM state:\n" + RAMDump());
                    old = current;
                }
            }

            //if ((cpu.PC & 0xFFFF) >= 0xFC2A && (cpu.PC & 0xFFFF) <= 0xFC30)
                //log();
        }
        //log();
    }

    // TODO add CPU registry log
    private void log() {
        /*if (clock.halfcycle() == 0) {
            System.out.println("  ___________________________________________________________________________________________________________");
            System.out.println(" | halfcycle | PHI |  CPU | ROM | RAM |  RAS |  CAS |  MUX | RW | ADDR | DATA |     RAM STATE     | MNEMONIC |  PC  SR AC XR YR SP | MEMORY DUMP");
            System.out.println(" |-----------|-----|------|-----|-----|------|------|------|----|------|------|-------------------|----------|---------------------|");
        }*/

        if ((clock.halfcycle() % 2) == 1) {
            boolean romAccess = basic.cs.get(0).level() == Pin.Level.LOW || kernal.cs.get(0).level() == Pin.Level.LOW;

            System.out.printf(" | %9d |  %2d | %4s |  %c  |  %c  | %4s | %4s | %4s | %2s | %04X |  %02X  |  %7s[%02X, %02X]  |  %s[%d]  | %s | %s | \n",
                    clock.halfcycle() / 2, (clock.halfcycle() / 2) & 0xF, cpu.phi0.level(),
                    romAccess ? '+' : ' ', romAccess ? ' ' : '+',
                    ted.ras.level(), ted.cas.level(), ted.mux.level(), cpu.rw.level() == Pin.Level.HIGH ? "R " : " W",
                    cpu.address.value(), cpu.data.value(), ram1.state(), ram1.column, ram1.row, cpu.mnemonic(),
                    cpu.decodeCycle, cpu.reg(), RAMDump());
        }

        //if (!running)
            //System.out.println(" |___________|_____|______|_____|_____|______|______|______|____|______|______|___________|__________|");
        //"  ____________________________________________________________\n"
        //" | halfcycle | PHI |  CPU |  RAS |  CAS |  MUX | RW | ADDR | DATA |\n"
        //" |-----------|-----|------|------|------|------|----|------|------|\n"
        //
        //" | %9d |  %2d | %4s | %4s | %4s | %4s |  %1c | %04X |  %02X  |\n"
        //System.out.printf(" %8d[%04X] ", cpu.cycles(), cpu.PC);
        //System.out.print(cpu.rw.level() == Pin.Level.HIGH ? "Read from" : "Write to");
        //System.out.printf(" %04X [%02X]\n", cpu.address.value(), cpu.data.value());
    }

    private String RAMDump() {
        List<String> mem = new ArrayList<>();

        for (int i = 0; i < ram1.content.length; i++) {
            if (!(ram1.content[i] == (byte) 0xBB && ram2.content[i] == (byte) 0xBB))
                mem.add(String.format("%04X => %1X%1X", i, ram2.content[i], ram1.content[i]));
        }

        return "[ " + String.join(", ", mem) + " ]";
    }

    public void render(TED.Video fx) {
        ted.render(fx);
    }
}
