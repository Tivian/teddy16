package eu.tivian.hardware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: internal logic
//  product term array
//  sum term array
public class PLA {
    public final List<Pin> input;
    public final List<Pin> output;

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

// unused_	0 when 0111011x1001011x
#define F0	I15 || !I14 || !I13 || !I12 || I11 || !I10 || !I9 || !I7 || I6 || I5 || !I4 || I3 || !I2 || !I1

// PHI2		1 when 0xxxxxxx xxxxxx11
#define F1	!I15 && I1 && I0

// USER_	0 when 0111011x10001111
#define F2	I15 || !I14 || !I13 || !I12 || I11 || !I10 || !I9 || !I7 || I6 || I5 || I4 || !I3 || !I2 || !I1 || !I0

// 6551_	0 when x111011x1000011x
#define F3	!I14 || !I13 || !I12 || I11 || !I10 || !I9 || !I7 || I6 || I5 || I4 || I3 || !I2 || !I1

// ADDR_CLK	0 when 1111011x11101111
#define F4	I15 || !I14 || !I13 || !I12 || I11 || !I10 || !I9 || !I7 || !I6 || !I5 || I4 || !I3 || !I2 || !I1 || !I0

// KEYPORT_	0 when 0111011x10011111
#define F5	I15 || !I14 || !I13 || !I12 || I11 || !I10 || !I9 || !I7 || I6 || I5 || !I4 || !I3 || !I2 || !I1 || !I0

// KERNAL_	1 when x111001x1xxxx1xx
#define F6	I14 && I13 && I12 && !I11 && !I10 && I9 && I7 && I2

// I0	1 when xxxxxxx1xxxxxxxx or when 0xxxxxxxxxxxxx11
#define F7	I8 || (F1)

 */