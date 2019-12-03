package eu.tivian.hardware;

// TODO
//  first make sure TED chips works!!!
public class Keyboard {
    public final Bus column = new Bus("column", "C", Pin.Direction.OUTPUT, 8);
    public final Bus row    = new Bus("row"   , "R", Pin.Direction.INPUT , 8);


}
