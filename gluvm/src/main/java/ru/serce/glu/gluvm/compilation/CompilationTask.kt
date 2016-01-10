package ru.serce.glu.gluvm.compilation

import com.oracle.graal.asm.Label
import com.oracle.graal.asm.amd64.AMD64Assembler
import jdk.vm.ci.amd64.AMD64.*
import jdk.vm.ci.code.Register
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluc.bytecode.BYTECODES.*
import ru.serce.glu.gluvm.interpreter.GMethod
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter
import z.znr.MethodHandles
import java.util.*


data class Lbl(val index: Int)

data class Var(val index: Int, val register: Register) {
    override fun toString(): String = "Var{$index, ${register.name}}"
}

data class Line(val res: Any,
                val left: Any?,
                val op: BYTECODES? = null,
                val right: Any? = null)

class CompilationTask(var interpreter: Interpreter?, var method: GMethod?) : Runnable {

    private var lines: ArrayList<Line> = ArrayList()
    var stack = ArrayDeque<Var>()
    var locals = arrayOfNulls<Var>(method!!.argSize + method!!.locals)
    private var registers: List<Register> = arrayListOf<Register>(rcx, rdx, r8, r9, rbx, r10, r11, r12, r13, r14, r15)
    var counter = 0
    var lbls = HashMap<Int, Lbl>()

    fun nextVar(): Var {
        val c = counter++
        return Var(c, registers[c])
    }


    val lables: HashMap<Int, Label> = HashMap();
    val Lbl.label: Label
        get() = lables.computeIfAbsent(index, { Label(it) })

    fun createLbl(o: Int): Lbl = lbls.computeIfAbsent(o, { Lbl(it) })

    override fun run() {
        ssaTransform()
        // opt
        generateAsm()
    }

    fun generateAsm() {
        method!!.handle = MethodHandles.asm(Long::class.java,
                Long::class.java,
                Long::class.java,
                { a ->
                    lables.clear()
                    for (line in lines) {
                        if (!DefaultGroovyMethods.asBoolean(line.op)) {
                            if (line.left is Var) {
                                a.movq(register(line.res), register(line.left))
                            } else {
                                a.movq(register(line.res), line.left.toString().toLong())
                            }
                        } else {
                            when (line.op) {
                                IADD -> {
                                    a.movq(register(line.res), register(line.left!!))
                                    a.addq(register(line.res), register(line.right!!))
                                }
                                ISUB -> {
                                    a.movq(register(line.res), register(line.left))
                                    a.subq(register(line.res), register(line.right))
                                }
                                JZ -> {
                                    val l = (line.res as Lbl).label
                                    a.cmpl(rax, 0);
                                    a.jcc(AMD64Assembler.ConditionFlag.Equal, l)
                                }
                                GOTO -> {
                                    val l = (line.res as Lbl).label
                                    a.jmp(l)
                                }
                                LABEL -> {
                                    val lbl = line.res as Lbl
                                    val l = (lbl).label
                                    a.bind(l)
                                }
                                else -> throw RuntimeException("Unkonw op " + line.op.toString())
                            }
                        }


                    }
                    a.ret(0)
                })
    }

    private fun register(`var`: Any?): Register? {
        return (`var` as? Var)?.register!!
    }

    private fun ssaTransform() {
        var i = 0
        while (i < method!!.argSize) {
            val `var` = nextVar()
            lines.add(Line(`var`, `var`))
            locals[i] = `var`
            i++;
        }

        for (insr in method!!.instructions) {
            toSSA(insr)
        }

        for (line in lines) {
            System.err.println(line)
        }

    }

    fun toSSA(instruction: Instruction) {
        when (instruction.code) {
            BYTECODES.ILOAD -> iload(instruction.args.first())
            BYTECODES.LDC -> ldc(instruction.args.first())
            BYTECODES.ISTORE -> istore(instruction.args.first())
            BYTECODES.IADD -> iadd()
            BYTECODES.ISUB -> isub()
            BYTECODES.RETURN -> retFun(instruction)
            BYTECODES.ICONST_0 -> {
                val v = nextVar()
                lines.add(Line(v, 0))
                push(v)
            }
            BYTECODES.ICONST_1 -> {
                val v = nextVar()
                lines.add(Line(v, 1))
                push(v)
            }
            BYTECODES.IFEQ -> ifeq(instruction.args.first())
            GOTO -> gotof(instruction.args.first().toString().toInt())
            LABEL -> label(instruction.args.first().toString().toInt())
            else -> throw IllegalArgumentException("Unknown bytecode " + instruction.code.toString())
        }
    }

    fun gotof(o: Int) {
        lines.add(Line(createLbl(o), null, GOTO, null))
    }

    fun label(o: Int) {
        lines.add(Line(createLbl(o), null, LABEL, null))
    }

    fun retFun(instruction: Instruction) {
        val `var` = pop()
        lines.add(Line(accum(), `var`))
    }

    private fun accum(): Var = Var(++counter, rax)

    fun iadd() {
        val a = pop()
        val b = pop()
        val `var` = nextVar()
        lines.add(Line(`var`, a, IADD, b))
        push(`var`)
    }

    fun ifeq(`val`: Any) {
        val top = pop()
        lines.add(Line(accum(), top))
        lines.add(Line(createLbl(`val`.toString().toInt()), null, JZ, null))
    }

    fun isub() {
        val a = pop()
        val b = pop()
        val `var` = nextVar()
        lines.add(Line(`var`, a, ISUB, b))
        push(`var`)
    }

    fun istore(o: Any) {
        val localIdx = o.toString().toInt()
        val top = pop()
        if(locals[localIdx] == null) {
            locals[localIdx] = top
        }
        lines.add(Line(locals[localIdx]!!, top))
    }

    fun pop(): Var = stack.pop()

    private fun iload(o: Any) {
        val i = o.toString().toInt()
        val br = nextVar()
        lines.add(Line(br, locals[i]))
        push(br)
    }

    fun ldc(o: Any) {
        val vl = o.toString().toLong()
        val vr = nextVar()
        lines.add(Line(vr, vl))
        push(vr)
    }

    fun push(i: Var) {
        stack.push(i)
    }
}
