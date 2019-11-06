package eu.tivian.hardware;

// clock frequencies:
//   14.318180MHz for NTSC
//   17.734475MHz for PAL
public class SystemClock {
    public double frequency;

    public final Pin clock = new Pin("clock", Pin.Direction.OUTPUT);
}
