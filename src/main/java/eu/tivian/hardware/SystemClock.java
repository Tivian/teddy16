package eu.tivian.hardware;

import eu.tivian.other.SI;

/**
 * Main clock generator.
 * <br>For PAL system proper frequency is 17.734475MHz.
 * <br>For NTSC system proper frequency is 14.318180MHz.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see <a href="http://www.zimmers.net/anonftp/pub/cbm/schematics/computers/plus4/C16_Service_Manual_314001-03_(1984_Oct).pdf#page=12">
 *     Correct quartz values</a>
 */
public class SystemClock {
    /**
     * Frequency of the clock.
     */
    private double frequency;
    /**
     * Inverse of the frequency.
     */
    private double duration;
    /**
     * Number of level transitions.
     */
    private long halfcycle = 0;

    /**
     * Clock output pin.
     */
    public final Pin clock = new Pin("clock", Pin.Direction.OUTPUT);

    /**
     * Initializes the clock with minimal delay between pulses.
     */
    public SystemClock() {
        this(0);
    }

    /**
     * Initializes the clock with desired frequency.
     *
     * @param frequency the frequency of the clock
     */
    public SystemClock(double frequency) {
        frequency(frequency);
    }

    /**
     * Returns the frequency of the system clock.
     * @return the frequency of the system clock
     */
    public double frequency() {
        return frequency;
    }

    /**
     * Sets the frequency of the system clock.
     * @param freq frequency of the clock
     */
    public void frequency(double freq) {
        this.frequency = freq;
        this.duration = (SI.NANO / frequency) / 2.0;
    }

    /**
     * Pulses the system clock with selected frequency.
     */
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

    /**
     * Returns number of the clock level transitions.
     * @return number of the clock level transitions
     */
    public long halfcycle() {
        return halfcycle;
    }

    /**
     * Clears the level transitions counter.
     */
    public void clear() {
        halfcycle = 0;
    }
}
