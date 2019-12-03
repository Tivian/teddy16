package eu.tivian.hardware.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// dual 1-4 demux
public class IC74LS139 {
    private final List<QuadDemux> decoder;

    public IC74LS139() {
        List<QuadDemux> temp = new ArrayList<>();
        for (int i = 1; i <= 2; i++)
            temp.add(new QuadDemux());
        decoder = Collections.unmodifiableList(temp);
    }

    public QuadDemux get(int i) {
        return decoder.get(i);
    }
}
