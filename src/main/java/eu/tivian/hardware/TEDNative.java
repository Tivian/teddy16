package eu.tivian.hardware;

class TEDNative {
    static {
        System.loadLibrary("jni/ted");
    }

    public native void init();
    public native void step();
    public native void free();

    public native void clk(int state);
    public native int cpuclk();

    public native void rw(int state);
    public native int irq();
    public native int ba();
    public native int mux();
    public native int ras();
    public native int cas();
    public native int cs0();
    public native int cs1();
    public native int aec();

    public native int snd();

    public native int pal();
    public native int color();
    public native int hcount();
    public native int vcount();

    public native void data_in(int data);
    public native int data_out();
    public native int tedreg();
    
    public native void addr_in(int addr);
    public native int addr_out();

    public native void keyboard(int value);
}
