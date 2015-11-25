package ru.serce.glu.gluvm.compilation

import groovy.transform.Canonical
import groovy.transform.ToString
import jnr.udis86.X86Disassembler
import jnr.x86asm.Assembler
import jnr.x86asm.Label
import jnr.x86asm.Register
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.interpreter.GMethod
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter

import static jnr.x86asm.Asm.*
import static jnr.x86asm.Immediate.imm
import static ru.serce.glu.gluc.bytecode.BYTECODES.*
import static z.znr.MethodHandles.asm

@Canonical
class Var {
    int index
    Register register


    @Override
    public String toString() {
        return "Var{$index, ${register.code}}";
    }
}

@Canonical
class Lbl {
    int index;
    Register register

    @Override
    public String toString() {
        return "Var{$index, ${register.code}}";
    }
}


@ToString
class Line {
    def res
    def left
    BYTECODES op
    def right

    Line(res, left, BYTECODES op, right) {
        this.res = res
        this.left = left
        this.op = op
        this.right = right
    }

    Line(res, Object left) {

        this.res = res
        this.left = left
    }


    @Override
    public String toString() {
        return "Line{$res = $left ${op ?: ''} ${right ?: ''}}";
    }
}


class CompilationTask implements Runnable {

//    public static final Jssembly jsm = new Jssembly();

    Interpreter interpreter
    GMethod method

    CompilationTask(Interpreter interpreter, GMethod method) {
        this.interpreter = interpreter
        this.method = method
    }

    List<Line> lines = []
    ArrayDeque<Var> stack = new ArrayDeque<>()
    Var[] locals = new Var[method.argSize + method.locals]

    List<Register> registers = [rcx, rdx, r8, r9, rbx, rsi, rdi, r10, r11, r12, r13, r14, r15]


    int counter = 0

    Var nextVar() {
        def c = counter++
        new Var(c, registers[c])
    }

    HashMap<Integer, Lbl> lbls = new HashMap<Integer, Lbl>()

    Lbl createLbl(int o) {
        return lbls.computeIfAbsent(o, { Integer k ->
            new Lbl(k, nextReg())
        })
    }

    Register nextReg() {
        return registers[counter++]
    }


    @Override
    void run() {
        ssaTransform()
        // opt
        generateAsm()
    }

    void generateAsm() {
        method.HANDLE = asm(
                long.class, long.class, long.class,
                { Assembler a ->
                    for (line in lines) {
                        if (!line.op) {
                            if (line.left instanceof Var) {
                                a.mov(register(line.res), register(line.left))
                            } else {
                                a.mov(register(line.res), imm(line.left as long))
                            }
                        } else {
                            switch (line.op) {
                                case IADD:
                                    a.mov(register(line.res), register(line.left))
                                    a.add(register(line.res), register(line.right))
                                    break
                                case ISUB:
                                    a.mov(register(line.res), register(line.left))
                                    a.sub(register(line.res), register(line.right))
                                    break
                                case JZ:
                                    Label l = getLabel(a, line.res as Lbl)
                                    a.jz_short(l, 0)
                                    break
                                case GOTO:
                                    Label l = getLabel(a, line.res as Lbl)
                                    a.jmp_short(l)
                                    break
                                case LABEL:
                                    def lbl = line.res as Lbl
                                    Label l = getLabel(a, lbl)
                                    l.bind(a)
                                    break
                                default:
                                    throw new RuntimeException("Unkonw op ${line.op}")
                            }
                        }

                    }
                    a.ret()
                });

    }

    Label getLabel(Assembler a, Lbl lbl) {
        a.label(lbl.index)
    }

    private Register register(def var) {
        (var as Var).register
    }

    private void ssaTransform() {
        for (int i = 0; i < method.argSize; i++) {
            def var = nextVar()
            lines << new Line(var, var)
            locals[i] = var
        }
        for (insr in method.instructions) {
            toSSA(insr)
        }
        for (line in lines) {
            System.err.println line
        }
    }

    def toSSA(Instruction instruction) {
        switch (instruction.code) {
            case ILOAD:
                iload(instruction.args.first())
                break
            case LDC:
                ldc(instruction.args.first())
                break
            case ISTORE:
                istore(instruction.args.first())
                break
            case IADD:
                iadd()
                break
            case ISUB:
                isub()
                break
            case RETURN:
                retFun(instruction)
                break
            case ICONST_0:
                def var = nextVar()
                lines << new Line(var, 0)
                push(var)
                break
            case ICONST_1:
                def var = nextVar()
                lines << new Line(var, 1)
                push(var)
                break
            case IFEQ:
                ifeq(instruction.args.first())
                break
            case GOTO:
                gotof(instruction.args.first() as int)
                break
            case LABEL:
                label(instruction.args.first() as int)
                break
            default:
                throw new IllegalArgumentException("Unknown bytecode ${instruction.code}")
        }
    }

    def gotof(int o) {
        lines << new Line(createLbl(o), null, GOTO, null)
    }

    def label(int o) {
        lines << new Line(createLbl(o), null, LABEL, null)
    }

    def retFun(Instruction instruction) {
        def var = pop()
        lines << new Line(accum(), var)
    }

    private Var accum() {
        new Var(++counter, rax)
    }


    def iadd() {
        def a = pop()
        def b = pop()
        def var = nextVar()
        lines << new Line(var, a, IADD, b)
        push(var)
    }

    def ifeq(Object val) {
        def top = pop()
        lines << new Line(accum(), top)
        lines << new Line(createLbl(val as int), null, JZ, null)
    }

    def isub() {
        def a = pop()
        def b = pop()
        def var = nextVar()
        lines << new Line(var, a, ISUB, b)
        push(var)
    }


    def istore(Object o) {
        def localIdx = o as int
        def top = pop()
        locals[localIdx] = top
    }

    Var pop() {
        stack.pop()
    }

    private iload(Object o) {
        def i = o as int
        def var = nextVar()
        lines << new Line(var, locals[i])
        push(var)
    }

    def ldc(Object o) {
        def val = o as long
        def var = nextVar()
        lines << new Line(var, val)
        push(var)
    }

    def push(Var i) {
        stack.push(i)
    }
}
