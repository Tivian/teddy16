package eu.tivian.hardware;

/**
 * Mono-stable operating of NE555 timer.
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see <a href="http://www.ti.com/lit/ds/symlink/lm555.pdf">NE555 datasheet</a>
 */
public class MonostableTimer {
    /**
     * Value of resistor connected between pins 4 and 7 of real NE555 timer.
     */
    public double resistor;
    /**
     * Value of capacitor connected between pins 6 and ground of real NE555 timer.
     */
    public double capacitor;
    /**
     * LOW at {@link #trigger} pin will cause the timer to fire.
     */
    public final Pin trigger = new Pin("trigger", Pin.Direction.INPUT);
    /**
     *
     */
    public final Pin output = new Pin("output", Pin.Direction.OUTPUT);

    /**
     * Initialize the values of discrete elements
     *
     * @param resistor value of resistor in ohms
     * @param capacitor value of capacitor in farads
     * @throws IllegalArgumentException if either {@code resistor} value or {@code capacitor} value is negative
     */
    public MonostableTimer(double resistor, double capacitor) {
        if (resistor < 0)
            throw new IllegalArgumentException("Resistance cannot be negative!");
        else if (capacitor < 0)
            throw new IllegalArgumentException("Capacitance cannot be negative!");

        this.resistor = resistor;
        this.capacitor = capacitor;

        trigger.onChange(() -> {
            if (trigger.level() == Pin.Level.LOW)
                trigger();
        });
    }

    /**
     * Level at {@link #output} pin will change from HIGH to LOW after chosen amount of time.
     * <br>The timeout is calculated with the formula: {@code ln(3)*R*C}
     * @see <a href="http://www.ti.com/lit/ds/symlink/lm555.pdf#page=9">Formula source</a>
     */
    public void trigger() {
        output.level(Pin.Level.HIGH);
        try {
            Thread.sleep(Math.round(Math.log(3.0) * resistor * capacitor * 1000));
        } catch (InterruptedException ex) { }
        output.level(Pin.Level.LOW);
    }
}
