package ru.serce.glu.gluvm

import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter

class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: gluvm name");
            System.exit(-1);
        }

        Program program = new ByteCodeParser(args[0]).parse()
        new Interpreter(program).interpret(new Instruction(BYTECODES.INVOKE, ["main()V"]))
    }
}
