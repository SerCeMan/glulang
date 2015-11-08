package ru.serce.glu.gluvm.interpreter

import groovy.transform.Canonical
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.Program
import ru.serce.glu.gluvm.compilation.CompilationTask

import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

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
    int invCounter = 0

    GMethod(String signature, List<Instruction> instructions, int argSize, int locals, boolean stdlib = false) {
        this.locals = locals
        this.signature = signature
        this.instructions = instructions
        this.stdlib = stdlib
        this.argSize = argSize
    }
}


class Interpreter {

    static final long COMPILE_THRESHOLD = 2

    static final long STACK_SIZE = 2000;
    private final Map<String, GMethod> methods
    private final long[] stack = new long[STACK_SIZE]
    private int sp = 0
    private final ArrayDeque<Frame> frames = new ArrayDeque<>()
    private final Map<String, Closure> stdlib = new HashMap<>()

    private final ExecutorService compilationService = new ForkJoinPool()

    class Frame {
        def GMethod method
        private int fp = sp - method.argSize
        int instructionIndex = 0

        Frame(GMethod method) {
            this.method = method
        }

        int getLocals(int index) {
            stack[fp + index]
        }

        def store(long idx, long val) {
            stack[(int) (fp + idx)] = val
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
            def top = pop()
            println top
            retFun(new Instruction(RETURN, []))
        }
    }

    def interpret(Instruction instruction) {
        System.err.println "$instruction.code, $instruction.args, SP = $sp"
        System.err.println(stack.toList().take(sp + 1))
        System.err.println()
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
            case ISUB:
                isub()
                break
            case IMUL:
                imul()
                break
            case IDIV:
                idiv()
                break
            case IOR:
                ior()
                break
            case IAND:
                iand()
                break
            case IXOR:
                ixor()
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
            case LABEL:
                break
            case ICONST_0:
                push(0)
                break
            case ICONST_1:
                push(1)
                break
            case IFEQ:
                ifeq(instruction.args.first())
                break
            case GOTO:
                gotof(instruction.args.first())
                break
            case IF_ICMPLT:
                compareOp(instruction.args.first()) { Comparable a, b -> a < b }
                break
            case IF_ICMPLE:
                compareOp(instruction.args.first()) { Comparable a, b -> a <= b }
                break
            case IF_ICMPGT:
                compareOp(instruction.args.first()) { Comparable a, b -> a > b }
                break
            case IF_ICMPGE:
                compareOp(instruction.args.first()) { Comparable a, b -> a >= b }
                break
            case IF_ICMPEQ:
                compareOp(instruction.args.first()) { Comparable a, b -> a == b }
                break
            case IF_ICMPNE:
                compareOp(instruction.args.first()) { Comparable a, b -> a != b }
                break

            default:
                throw new RuntimeException("Unknown instruction! $instruction")

        }
        System.err.println(stack.toList().take(sp + 1))
        System.err.println('-------------')
    }

    def compareOp(Object o, Closure op) {
        def index = o
        def a = pop()
        def b = pop()
        if (op(b, a))
            gotof(index)
    }

    def gotof(Object idx) {
        def instructions = frames.peek().method.instructions
        def index = instructions.findIndexOf { Instruction i -> i.code == LABEL && i.args.first().toString() == idx }
        if (index == -1) {
            throw new RuntimeException("Unknown label index $idx")
        }
        frames.peek().instructionIndex = index
    }

    def ifeq(Object val) {
        def top = pop()
        if (0L == top) {
            gotof(val)
        }
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

//        if (ops.size() > 0) {
        def res = pop()
        sp = fp
        push(res)
//        } else {
//            sp = fp
//        }
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

    def ixor() {
        def a = pop()
        def b = pop()
        push((b ^ a) ? 1 : 0)
    }

    private iadd() {
        def a = pop()
        def b = pop()
        push(a + b)
    }

    private isub() {
        def a = pop()
        def b = pop()
        push(b - a)
    }

    private imul() {
        def a = pop()
        def b = pop()
        push(a * b)
    }

    private idiv() {
        def a = pop()
        def b = pop()
        push((b / a) as long)
    }

    private ior() {
        def a = pop()
        def b = pop()
        push((b || a) ? 1 : 0)
    }

    private iand() {
        def a = pop()
        def b = pop()
        push((b && a) ? 1 : 0)
    }


    private invoke(Instruction instruction) {
        def sig = instruction.args.first()
        def method = methods.get(sig)
        if (!method) {
            throw new RuntimeException("Unknown method signature $sig")
        }


        def frame = new Frame(method);
        frames.push(frame)
        method.invCounter++
        sp += method.locals


        if (method.invCounter >= COMPILE_THRESHOLD) {
            def task = new CompilationTask(this, method)
            task.run()
        }

        if (method.isStdlib()) {
            if (method.isStdlib()) {
                stdlib[method.signature](instruction.args)
            }
        } else {
            def instructions = method.instructions
            for (; frame.instructionIndex < instructions.size(); frame.instructionIndex++) {
                def instr = instructions[frame.instructionIndex]
                interpret(instr)
            }
//            for (instr in method.instructions) {
//                interpret(instr)
//            }
        }
    }

    private iload(Object o) {
        def i = o as int
        def frame = frame()
        push(frame.getLocals(i))
    }

    def Frame frame() {
        frames.peek()
    }
}
