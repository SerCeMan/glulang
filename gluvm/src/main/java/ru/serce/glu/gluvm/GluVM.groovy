package ru.serce.glu.gluvm

import ru.serce.glu.gluc.Compiler
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter

class GluVM {
    def static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: gluvm name");
            System.exit(-1);
        }

        def lines = new File(args[0]).readLines()
        new GluVM().run(lines)
    }

    def run(List<String> lines) {
        def program = new ByteCodeParser().parseLines(lines)
        new Interpreter(program)
                .interpret(mainInstr())
    }

    def eval(String text) {
        def bytecode = new Compiler("").eval(text)
        System.err.println("$bytecode");
        def program = new ByteCodeParser().parseText(bytecode)
        new Interpreter(program).interpret(mainInstr())
    }

    private Instruction mainInstr() {
        new Instruction(BYTECODES.INVOKE, ["main()V"])
    }
}
