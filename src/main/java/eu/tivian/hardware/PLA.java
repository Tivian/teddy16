package eu.tivian.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// based on custom PLA from C16
// http://www.zimmers.net/anonftp/pub/cbm/firmware/computers/plus4/pla.txt
// http://www.zimmers.net/anonftp/pub/cbm/firmware/computers/plus4/pla.c
public class PLA {
    public final List<Pin> input;
    public final List<Pin> output;

    public PLA() {
        this(8, 16);
    }

    public PLA(int inputs, int outputs) {
        List<Pin> temp = new ArrayList<>();
        for (int i = 0; i < inputs; i++) {
            Pin pin = new Pin("I" + i, Pin.Direction.INPUT);
            pin.onChange(this::update);
            temp.add(pin);
        }
        this.input = Collections.unmodifiableList(temp);

        temp = new ArrayList<>();
        for (int i = 0; i < outputs; i++)
            temp.add(new Pin("F" + i, Pin.Direction.OUTPUT));
        this.output = Collections.unmodifiableList(temp);
    }

    private void update(Pin.Level level) {
        boolean[] in = new boolean[input.size()];
        for (int i = 0; i < in.length; i++)
            in[i] = input.get(i).level().bool();

        boolean[] out = new boolean[output.size()];
        out[0] =  in[15] || !in[14] || !in[13] || !in[12] ||  in[11] || !in[10] || !in[9] || !in[7] ||  in[6] ||  in[5] || !in[4] ||  in[3] || !in[2] || !in[1];
        out[1] = !in[15] &&   in[1] &&   in[0];
        out[2] =  in[15] || !in[14] || !in[13] || !in[12] ||  in[11] || !in[10] || !in[9] || !in[7] ||  in[6] ||  in[5] ||  in[4] || !in[3] || !in[2] || !in[1] || !in[0];
        out[3] = !in[14] || !in[13] || !in[12] ||  in[11] || !in[10] ||  !in[9] || !in[7] ||  in[6] ||  in[5] ||  in[4] ||  in[3] || !in[2] || !in[1];
        out[4] =  in[15] || !in[14] || !in[13] || !in[12] ||  in[11] || !in[10] || !in[9] || !in[7] || !in[6] || !in[5] ||  in[4] || !in[3] || !in[2] || !in[1] || !in[0];
        out[5] =  in[15] || !in[14] || !in[13] || !in[12] ||  in[11] || !in[10] || !in[9] || !in[7] ||  in[6] ||  in[5] || !in[4] || !in[3] || !in[2] || !in[1] || !in[0];
        out[6] =  in[14] &&  in[13] &&  in[12] && !in[11] && !in[10] &&   in[9] &&  in[7] &&  in[2];
        out[7] =   in[8] || out[1];

        for (int i = 0; i < out.length; i++)
            output.get(i).level(out[i]);
    }
}

/*

  Inputs
I0    F7         X X - X 1 1 1 X 1
I1    phi 0      1 1 - X 1 1 1 X 1
I2    A15        1 1 - 1 1 1 1 X X
I3    A4         0 0 - X 1 1 1 X X
I4    A5         0 1 - X 1 0 0 X X
I5    A6         0 0 - X 0 1 0 X X
I6    A7         0 0 - X 0 1 0 X X
I7    A12        1 1 - 1 1 1 1 X X
I8    MUX        X X - X X X X 1 X
I9    A14        1 1 - 1 1 1 1 X X
I10   A8         1 1 - 0 1 1 1 X X
I11   A9         0 0 - 0 0 0 0 X X
I12   A13        1 1 - 1 1 1 1 X X
I13   A11        1 1 - 1 1 1 1 X X
I14   A10        1 1 - 1 1 1 1 X X
I15  -RAS        X 0 - X 0 0 0 X 0

  Outputs
F0   -?          X 0 1 X X X X X X
F1    phi 2      X X 0 X 1 1 1 X 1
F2   -USER       X X 1 X X X 0 X X
F3   -6551       0 X 1 X X X X X X
F4   ^ADDR CLK   X X 1 X X 0 X X X
F5   -KEYPORT    X X 1 X 0 X X X X
F6    KERNAL     X X 0 1 X X X X X
F7    I0         X X 0 X 1 1 1 1 1

 */