package eu.tivian.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for PLA chip.
 *
 * @author Paweł Kania
 * @since 2019-12-03
 * @see PLA
 */
class PLATest {
    /**
     * Checks if at idle the output is correct.
     */
    @Test
    void logic() {
        PLA pla    = new PLA();
        Bus plaIn  = new Bus(pla.input);
        Bus plaOut = new Bus(pla.output);
        Bus input  = new Bus("input" , "I", Pin.Direction.OUTPUT, 16);
        Bus output = new Bus("output", "F", Pin.Direction.INPUT ,  8);

        input.connect(plaIn);
        output.connect(plaOut);

        assertEquals(0b00111101, output.value());
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