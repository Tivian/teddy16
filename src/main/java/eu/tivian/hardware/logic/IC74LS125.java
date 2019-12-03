package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// quad tri-state buffer
public class IC74LS125 {
    private final List<TriState> buffer;

    public IC74LS125() {
        List<TriState> temp = new ArrayList<>();
        for (int i = 1; i <= 4; i++)
            temp.add(new TriState());
        buffer = Collections.unmodifiableList(temp);
    }

    public TriState get(int i) {
        return buffer.get(i);
    }
}
