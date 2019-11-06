package eu.tivian.hardware.logic;

import eu.tivian.hardware.Pin;

class LogicGate {
    public enum Type {
        NOT, NAND, NOR, AND, OR, XOR, XNOR
    }

    public final Type type;
    public final String name;

    public final Pin inputA;
    public final Pin inputB;
    public final Pin output;

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
    }

    private void update(Pin.Level level) {
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

    @Override
    public String toString() {
        return name + " " + type.toString();
    }
}
