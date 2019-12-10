package eu.tivian.hardware;

public abstract class Memory {
    public final Bus data;
    public final Bus address;
    public final String name;

    protected final byte[] content;

    protected Memory(String name, Bus data, Bus address, int size) {
        this.name = name;
        this.data = data;
        this.address = address;
        this.content = new byte[size];
    }

    public byte peek(int address) {
        return content[address];
    }

    public void poke(int address, byte data) {
        content[address] = data;
    }

    public int size() {
        return content.length;
    }
}
