package eu.tivian.hardware;

import eu.tivian.hardware.logic.*;

public class Motherboard {
    // Commodore 16 specific chips
    public final MOS8501 cpu;       // U2
    public final TED     ted;       // U1
    public final RAM     ram1;      // U5
    public final RAM     ram2;      // U6
    public final ROM     basic;     // U3
    public final ROM     kernal;    // U4
    public final PLA     pla;       // U16

    // Logic chips
    public final SystemClock     clock;     // Y1
    public final MonostableTimer timer;     // U10
    public final IC74LS257       ramMux1;   // U7
    public final IC74LS257       ramMux2;   // U8
    public final IC7406          invhex;    // U9
    public final IC74LS125       buffer;    // U11
    public final IC74LS02        norGate;   // U12
    public final IC74LS175       flipFlop;  // U15
    public final IC74LS139       demux;     // U14

    public Motherboard() {
        this.cpu    = new MOS8501();
        this.ted    = new TED();
        this.ram1   = new RAM(8, 4, 65536);
        this.ram2   = new RAM(8, 4, 65536);
        this.basic  = new ROM(16384);
        this.kernal = new ROM(16384);
        this.pla    = new PLA();

        this.clock    = new SystemClock();
        this.timer    = new MonostableTimer();
        this.ramMux1  = new IC74LS257();
        this.ramMux2  = new IC74LS257();
        this.invhex   = new IC7406();
        this.buffer   = new IC74LS125();
        this.norGate  = new IC74LS02();
        this.flipFlop = new IC74LS175();
        this.demux    = new IC74LS139();
    }
}
