package ru.serce.glu.gluvm.interpreter

import groovy.transform.Canonical
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.Program

import static ru.serce.glu.gluc.bytecode.BYTECODES.*

@Canonical
class Instruction {
    BYTECODES code
    List<? extends Object> args
}

class GMethod {
    final String signature
    final def List<Instruction> instructions
    final int argSize
    final int locals

    boolean stdlib = false
    int execCounter = 0

    GMethod(String signature, List<Instruction> instructions, int argSize, int locals, boolean stdlib = false) {
        this.locals = locals
        this.signature = signature
        this.instructions = instructions
        this.stdlib = stdlib
        this.argSize = argSize
    }
}


class Interpreter {

    static final long STACK_SIZE = 2000;
    private final Map<String, GMethod> methods
    private final long[] stack = new long[STACK_SIZE]
    private int sp = 0
    private final ArrayDeque<Frame> frames = new ArrayDeque<>()
    private final Map<String, Closure> stdlib = new HashMap<>()

    class Frame {
        def GMethod method
        private int fp = sp - method.argSize + method.locals

        Frame(GMethod method) {
            this.method = method
        }

        int getLocals(int index) {
            stack[fp + index]
        }

        def store(long idx, long val) {
            stack[(int)(fp + method.argSize + idx)] = val
        }
    }


    Interpreter(Program program) {
        this.methods = program.methods
        init()
    }

    def init() {
        def prlnSig = "println(I)V"
        methods[prlnSig] = new GMethod(prlnSig, [], 1, 0, true)
        stdlib[prlnSig] = { List<? extends Object> args ->
            println frames.peek().getLocals(1)
            retFun(new Instruction(RETURN, []))
        }
    }

    def interpret(Instruction instruction) {
//        println "$instruction.code, $instruction.args, SP = $sp"
//        println(stack.toList().takeWhile { it != 0 })
//        println()
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
            case ISTORE:
                istore(instruction.args.first())
                break
            default:
                throw new RuntimeException("Unknown instruction! $instruction")

        }
//        println(stack.toList().takeWhile { it != 0 })
//        println('-------------')
    }

    def istore(Object o) {
        def localIdx = o as long
        def top = pop()
        frames.peek().store(localIdx, top)
    }

    def retFun(Instruction instruction) {
        def ops = instruction.args
        def frame = frames.pop()
        def fp = frame.fp

        if (ops.size() > 0) {
            def res = pop()
            sp = fp
            push(res)
        } else {
            sp = fp
        }
    }

    def ldc(Object o) {
        def val = o as long
        push(val)
    }

    def push(long l) {
        stack[sp++] = l
    }

    long pop() {
        stack[--sp]
    }

    private iadd() {
        def a = pop()
        def b = pop()
        sp -= 2
        push(a + b)
    }

    private invoke(Instruction instruction) {
        def sig = instruction.args.first()
        def method = methods.get(sig)
        if (!method) {
            throw new RuntimeException("Unknown method signature $sig")
        }
        def frame = new Frame(method);
        frames.push(frame)
        method.execCounter++
        if (method.isStdlib()) {
            if (method.isStdlib()) {
                stdlib[method.signature](instruction.args)
            }
        } else {
            for (instr in method.instructions) {
                interpret(instr)
            }
        }
    }

    private iload(Object o) {
        def i = o as int
        def frame = frames.peek()
        push(frame.getLocals(i))
    }
}
