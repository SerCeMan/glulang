package ru.serce.glu.gluvm.compilation

import groovy.transform.ToString
import jnr.x86asm.Assembler
import ru.serce.glu.gluvm.interpreter.GMethod
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter

import static jnr.x86asm.Asm.*
import static ru.serce.glu.gluc.bytecode.BYTECODES.*
import static z.znr.MethodHandles.asm

@ToString
class Line {
    String res
    def left
    String op
    def right

    Line(String res, left, String op, right) {
        this.res = res
        this.left = left
        this.op = op
        this.right = right
    }

    Line(String res, Object left) {

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
    ArrayDeque<String> stack = new ArrayDeque<>()
    String[] locals = new String[method.argSize + method.locals]


    int counter = 0

    String nextVar() {
        "t${counter++}"
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
                    a.add(rdx, rcx);
                    a.mov(rax, rdx);
                    a.ret();
                });
    }

    private void ssaTransform() {
        for (int i = 0; i < method.argSize; i++) {
            def var = nextVar()
            lines << new Line(var, interpreter.frame().getLocals(i))
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
            case RETURN:
                retFun(instruction)
                break
        }
    }

    def retFun(Instruction instruction) {
        def var = pop()
        lines << new Line("RETURN", var)
    }


    def iadd() {
        def a = pop()
        def b = pop()
        def var = nextVar()
        lines << new Line(var, a, "+", b)
        push(var)
    }

    def istore(Object o) {
        def localIdx = o as int
        def top = pop()
        locals[localIdx] = top
    }

    def pop() {
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

    def push(String i) {
        stack.push(i)
    }
}
