package ru.serce.glu.gluvm.compilation

import groovy.transform.Canonical
import groovy.transform.ToString
import jnr.x86asm.Assembler
import jnr.x86asm.Register
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.interpreter.GMethod
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter

import static jnr.x86asm.Asm.*
import static ru.serce.glu.gluc.bytecode.BYTECODES.*
import static z.znr.MethodHandles.asm

@Canonical
class Var {
    int index
    Register register
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
}


class CompilationTask implements Runnable {

    Interpreter interpreter
    GMethod method

    CompilationTask(Interpreter interpreter, GMethod method) {
        this.interpreter = interpreter
        this.method = method
    }

    List<Line> lines = []
    ArrayDeque<Var> stack = new ArrayDeque<>()
    Var[] locals = new Var[method.argSize + method.locals]

    List<Register> registers = [rcx, rdx, r8, r9, rbx, rsi, rdi]


    int counter = 0

    Var nextVar() {
        def c = counter++
        new Var(c, registers[c])
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
                            a.mov(register(line.res), register(line.left))
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
                                default:
                                    throw new RuntimeException("Unkonw op ${line.op}")
                            }
                        }

                    }
                    a.ret()
                });
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
            default:
                throw new IllegalArgumentException("Unknown bytecode ${instruction.code}")
        }
    }

    def retFun(Instruction instruction) {
        def var = pop()
        lines << new Line(new Var(++counter, rax), var)
    }


    def iadd() {
        def a = pop()
        def b = pop()
        def var = nextVar()
        lines << new Line(var, a, IADD, b)
        push(var)
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
