package eu.tivian.hardware;

public abstract class Memory {
    public final Bus data;
    public final Bus address;

    protected final byte[] content;

    protected Memory(Bus data, Bus address, int size) {
        this.data = data;
        this.address = address;
        this.content = new byte[size];
    }
}
