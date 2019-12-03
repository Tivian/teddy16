package eu.tivian.hardware;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// pinout based on CSG23128
public class ROM extends Memory {
    public final String name;
    public final List<Pin> cs; // cs1 - inverted, cs2 - inverted, cs3 - not inverted

    public ROM(int size) {
        this("ROM", size);
    }

    public ROM(String name, int size) {
        super(
            new Bus("data"   , "D", Pin.Direction.HI_Z ,  8),
            new Bus("address", "A", Pin.Direction.INPUT, 14),
            size
        );
        this.name = name;

        List<Pin> temp = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Pin pin = new Pin("cs" + i, Pin.Direction.INPUT);
            temp.add(pin);
            pin.onChange(() -> {
                enable();
                update();
            });
        }
        cs = Collections.unmodifiableList(temp);
        address.onChange(this::update);
    }

    public void preload(byte[] content) {
        System.arraycopy(content, 0, this.content, 0, this.content.length);
    }

    public void preload(Path file) throws IOException {
        preload(Files.readAllBytes(file));
    }

    private void enable() {
        data.direction((!cs.get(0).level().bool() && !cs.get(1).level().bool() && cs.get(2).level().bool())
            ? Pin.Direction.OUTPUT : Pin.Direction.HI_Z);
    }

    private void update() {
        if (data.direction() == Pin.Direction.OUTPUT)
            data.value(content[(int) address.value()]);
    }

    @Override
    public String toString() {
        return name + " [" + content.length + "]";
    }
}
