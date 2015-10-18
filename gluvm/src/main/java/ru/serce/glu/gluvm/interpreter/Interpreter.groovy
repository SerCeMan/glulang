package ru.serce.glu.gluvm.interpreter

import groovy.transform.Canonical
import jdk.nashorn.internal.ir.annotations.Immutable
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.Program

import static ru.serce.glu.gluc.bytecode.BYTECODES.*

@Canonical
class Instruction {
    BYTECODES code
    List<? extends Object> args
}

class Frame {
    def final ArrayDeque<Long> operandStack = new ArrayDeque<Long>()
}

@Canonical
@Immutable
class GMethod {
    int execCounter = 0
    String signature
    def List<Instruction> instructions
    int locals;
}


class Interpreter {

    static final long STACK_SIZE = 2000;
    private final Map<String, GMethod> methods
    private final long[] globalStack = new long[STACK_SIZE]
    private int gsp = 0
    private final ArrayDeque<Frame> frames = new ArrayDeque<>()
    private ArrayDeque<Long> stack


    Interpreter(Program program) {
        this.methods = program.methods
    }

    def interpret(Instruction instruction) {
        switch (instruction.code) {
            case ILOAD:
                iload(instruction.args.first())
                break
            case INVOKE:
                invoke(instruction)
                break
            case IADD:
                iadd()
                break
            case LDC:
                ldc(instruction.args.first())
                break
            case RETURN:
                retFun(instruction)
                break
            default:
                throw new RuntimeException("Unknown instruction! $instruction")

        }
    }

    def retFun(Instruction instruction) {
        def ops = instruction.args
        def top = frames.pop()
        stack = frames.peek().operandStack
        if (ops.size() > 0) {
            def res = top.operandStack.pop()
            stack.push(res)
        }
    }

    def ldc(Object o) {
        def val = o as long
        stack.push(val)
    }

    private iadd() {
        def a = stack.pop()
        def b = stack.pop()
        stack.push(a + b)
    }

    private invoke(Instruction instruction) {
        def frame = new Frame();
        frames.push(frame)
        stack = frame.operandStack
        def method = methods.get(instruction.args.first())
        if (!method) {
            throw new RuntimeException("Unknown method signature $method")
        }
        gsp -= method.locals
        method.execCounter++
        for (instr in method.instructions) {
            interpret(instr)
        }
    }

    private iload(Object o) {
        def i = o as int
        def frame = frames.pop()
        long[] prevStack = frames.peek().operandStack
        stack.push(prevStack[prevStack.size() - i - 1])
        frames.push(frame)

    }
}
