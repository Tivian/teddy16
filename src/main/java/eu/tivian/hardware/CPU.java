package eu.tivian.hardware;

public interface CPU {
    long cycles();
    short counter();
    short lastOpcodePosition();
    String mnemonic();
    String reg();
}
