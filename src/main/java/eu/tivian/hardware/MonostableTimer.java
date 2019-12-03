package eu.tivian.hardware;

public class MonostableTimer {
    public double resistor  = 0.0;
    public double capacitor = 0.0;
    public final Pin trigger = new Pin("trigger", Pin.Direction.INPUT);
    public final Pin output = new Pin("output", Pin.Direction.OUTPUT);

    public MonostableTimer(double resistor, double capacitor) {
        this.resistor = resistor;
        this.capacitor = capacitor;

        trigger.onChange(() -> {
            if (trigger.level() == Pin.Level.LOW)
                trigger();
        });
    }

    public void trigger() {
        output.level(Pin.Level.HIGH);
        try {
            Thread.sleep(Math.round(Math.log(3.0) * resistor * capacitor * 1000));
        } catch (InterruptedException ex) { }
        output.level(Pin.Level.LOW);
    }
}
