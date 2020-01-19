package eu.tivian.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class TEDNative {
    static {
        //System.loadLibrary("jni/ted");
        var stream = TEDNative.class.getResourceAsStream(System.mapLibraryName("/jni/ted"));

        try {
            var temp = Files.createTempFile("ted", System.mapLibraryName(""));
            Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toString());
        } catch (IOException ex) { }
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
