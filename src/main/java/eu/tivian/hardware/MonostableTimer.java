package eu.tivian.hardware;

public class MonostableTimer {
    public double resistor  = 0.0;
    public double capacitor = 0.0;

    public void trigger() throws InterruptedException {
        Thread.sleep(Math.round(Math.log(3.0) * resistor * capacitor * 1000));
    }
}
