package eu.tivian.hardware;

import eu.tivian.other.SI;

// clock frequencies:
//   14.318180MHz for NTSC
//   17.734475MHz for PAL
public class SystemClock {
    private double frequency;
    private double duration;
    private long halfcycle = 0;

    public final Pin clock = new Pin("clock", Pin.Direction.OUTPUT);

    public SystemClock() {
        this(0);
    }

    public SystemClock(double frequency) {
        frequency(frequency);
    }

    public double frequency() {
        return frequency;
    }

    public void frequency(double freq) {
        this.frequency = freq;
        this.duration = (SI.NANO / frequency) / 2.0;
    }

    public void pulse() {
        halfcycle++;

        long before = System.nanoTime();
        clock.level(!clock.level().bool());
        long after = System.nanoTime();
        double diff = duration - (after - before);
        if (diff > 0) {
            try {
                Thread.sleep(Math.round(diff * 1000));
            } catch (InterruptedException ex) { }
        }
    }

    public long halfcycle() {
        return halfcycle;
    }

    public void clear() {
        halfcycle = 0;
    }
}
