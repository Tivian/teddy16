package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

/**
 * Generic logic gate.
 * <br><br>
 *
 * <table>
 *     <caption>Available logic gates:</caption>
 *     <tr>
 *         <td><b>NOT</b></td>
 *         <td>&not;A</td>
 *     </tr>
 *     <tr>
 *         <td><b>NAND</b></td>
 *         <td>&not;(A &and; B)</td>
 *     </tr>
 *     <tr>
 *         <td><b>NOR</b></td>
 *         <td>&not;(A &or; B)</td>
 *     </tr>
 *     <tr>
 *         <td><b>AND</b></td>
 *         <td>A &and; B</td>
 *     </tr>
 *     <tr>
 *         <td><b>OR</b></td>
 *         <td>A &or; B</td>
 *     </tr>
 *     <tr>
 *         <td><b>XOR</b></td>
 *         <td>A &oplus; B</td>
 *     </tr>
 *     <tr>
 *         <td><b>XNOR</b></td>
 *         <td>&not;(A &oplus; B)</td>
 *     </tr>
 * </table>
 *
 * @author Paweł Kania
 * @since 2019-11-06
 * @see IC74LS02
 * @see IC7406
 */
public class LogicGate {
    /**
     * Available types of the logic gate.
     */
    public enum Type {
        /**
         * &not;A
         */
        NOT,

        /**
         * &not;(A &and; B)
         */
        NAND,

        /**
         * &not;(A &or; B)
         */
        NOR,

        /**
         * A &and; B
         */
        AND,

        /**
         * A &or; B
         */
        OR,

        /**
         * A &oplus; B
         */
        XOR,

        /**
         * &not;(A &oplus; B)
         */
        XNOR
    }

    /**
     * Type of the logic gate.
     */
    public final Type type;
    /**
     * Name of the logic gate.
     */
    public final String name;

    /**
     * First input signal.
     */
    public final Pin inputA;
    /**
     * Second input signal.
     * Same as {@link LogicGate#inputA} if it's a <b>NOT</b> gate.
     */
    public final Pin inputB;
    /**
     * Output signal.
     */
    public final Pin output;

    /**
     * Constructs the logic gate with given type and default name.
     *
     * @param type selects the logic gate type
     * @see LogicGate#LogicGate(String, Type)
     */
    public LogicGate(Type type) {
        this(type.toString() + " gate", type);
    }

    /**
     * Constructs the logic gate with given type and name.
     *
     * @param name specify the logic gate name
     * @param type selects the logic gate type
     */
    public LogicGate(String name, Type type) {
        this.name = name;
        this.type = type;
        if (type != Type.NOT) {
            this.inputA = new Pin(name + " input A", Pin.Direction.INPUT);
            this.inputB = new Pin(name + " input B", Pin.Direction.INPUT);
        } else {
            this.inputA = this.inputB = new Pin(name + " input", Pin.Direction.INPUT);
        }

        this.output = new Pin(name + " output", Pin.Direction.OUTPUT);

        inputA.onChange(this::update);
        inputB.onChange(this::update);
        update();
    }

    /**
     * Updates the gate {@link #output} according to the levels on input pins.
     */
    private void update() {
        switch (type) {
            case NOT:
                output.level(!inputA.level().bool());
                break;
            case NAND:
                output.level(!(inputA.level().bool() && inputB.level().bool()));
                break;
            case NOR:
                output.level(!(inputA.level().bool() || inputB.level().bool()));
                break;
            case AND:
                output.level(inputA.level().bool() && inputB.level().bool());
                break;
            case OR:
                output.level(inputA.level().bool() || inputB.level().bool());
                break;
            case XOR:
                output.level(inputA.level() != inputB.level());
                break;
            case XNOR:
                output.level(inputA.level() == inputB.level());
                break;
        }
    }

    /**
     * Returns the name of the logic gate.
     * @return name of the logic gate
     */
    @Override
    public String toString() {
        return name + " " + type.toString();
    }
}
