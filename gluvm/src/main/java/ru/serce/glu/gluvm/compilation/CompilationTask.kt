package ru.serce.glu.gluvm.compilation

import com.oracle.graal.asm.Label
import com.oracle.graal.asm.amd64.AMD64Address
import com.oracle.graal.asm.amd64.AMD64Assembler
import jdk.vm.ci.amd64.AMD64.*
import jdk.vm.ci.code.Register
import jnr.x86asm.Immediate
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluc.bytecode.BYTECODES.*
import ru.serce.glu.gluvm.interpreter.GMethod
import ru.serce.glu.gluvm.interpreter.Instruction
import ru.serce.glu.gluvm.interpreter.Interpreter
import z.znr.MethodHandles
import java.util.*

class Reg(val index: Int) {
    var color: Int = 0
    var reg: Register? = null
    val connected: ArrayList<Reg> = arrayListOf()

    var used: Boolean = false

    fun connect(reg: Reg) {
        connected.add(reg)
    }

    override fun toString(): String = "Reg{$index, ${reg?.name ?: "noreg"}}"
}

data class Lbl(val index: Int)

data class Var(val index: Int, val register: Reg) {
    override fun toString(): String = "Var{$index, $register}"
}

data class Line(val res: Any,
                val left: Any?,
                val op: BYTECODES? = null,
                val right: Any? = null)

class CompilationTask(var interpreter: Interpreter?, var method: GMethod?) : Runnable {

    private var lines: ArrayList<Line> = ArrayList()
    var stack = ArrayDeque<Var>()
    var locals = arrayOfNulls<Var>(method!!.argSize + method!!.locals)
    var counter = 0
    var lbls = HashMap<Int, Lbl>()

    fun nextVar(): Var {
        val c = counter++
        return Var(c, Reg(c))
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

    val r1 = r8;
    val r2 = r9;

    fun generateAsm() {
        method!!.handle = MethodHandles.asm(Long::class.java,
                Long::class.java,
                Long::class.java,
                { a ->
                    lables.clear()
                    a.push(rbp)
                    a.movq(rbp, rsp)
                    a.subq(rsp, (counter + 1) * 8)
                    for (line in lines) {
                        if (line.op == null) {
                            if (line.left is Var) {
                                if(line.left.register.reg != rax) {
                                    a.movq(rax, a.address(line.left))
                                }
                                a.movq(a.address(line.res), line.left.register.reg ?: rax)
                            } else {
                                a.movq(rax, line.left.toString().toLong())
                                a.movq(a.address(line.res), rax)
                            }
                        } else {
                            when (line.op) {
                                IADD -> {
                                    a.movq(rax, a.address(line.left))
                                    a.movq(r2, a.address(line.right))
                                    a.addq(rax, r2)
                                    a.movq(a.address(line.res), rax)
                                }
                                ISUB -> {
                                    a.movq(rax, a.address(line.left))
                                    a.movq(r2, a.address(line.right))
                                    a.subq(rax, r2)
                                    a.movq(a.address(line.res), rax)
                                }
                                IFEQ -> {
                                    if((line.left as Var).register.reg != rax) {
                                        a.movq(rax, a.address(line.left))
                                    }
                                    a.cmpq(rax, 0);
                                    a.jcc(AMD64Assembler.ConditionFlag.Equal, (line.res as Lbl).label)
                                }
                                IF_ICMPLE -> {
                                    a.movq(r1, a.address(line.left))
                                    a.movq(r2, a.address(line.right))
                                    a.cmpq(r2, r1)
                                    a.jcc(AMD64Assembler.ConditionFlag.LessEqual, (line.res as Lbl).label)
                                }
                                IF_ICMPGT -> {
                                    a.movq(r1, a.address(line.left))
                                    a.movq(r2, a.address(line.right))
                                    a.cmpq(r2, r1)
                                    a.jcc(AMD64Assembler.ConditionFlag.Greater, (line.res as Lbl).label)
                                }
                                GOTO -> {
                                    val l = (line.res as Lbl).label
                                    a.jmp(l)
                                }
                                LABEL -> {
                                    val lbl = line.res as Lbl
                                    val l = (lbl).label
                                    a.nop()
                                    a.bind(l)
                                }
                                else -> throw RuntimeException("Unkonw op " + line.op.toString())
                            }
                        }


                    }
                    a.movq(rax, a.address(accum))
                    a.addq(rsp, (counter + 1) * 8)
                    a.pop(rbp)
                    a.ret(0)
                })
    }


    private fun AMD64Assembler.address(v: Any?): AMD64Address? {
        return makeAddress(rbp, -8 * ((v as Var).index + 1))
    }


    fun dfs(r: Reg, ans: ArrayList<Reg>) {
        r.used = true
        for (n in r.connected) {
            if (!n.used) {
                dfs(n, ans)
            }
        }
        ans.add(r)
    }

    fun topologicalSort(allRegs: ArrayList<Reg>): ArrayList<Reg> {
        var ans = ArrayList<Reg>()
        for (reg in allRegs) {
            if (!reg.used) {
                dfs(reg, ans)
            }
        }
        ans.reverse()
        return ans
    }


    private fun ssaTransform() {
        var i = 0
        val registers = arrayListOf<Register>(rcx, rdx, r8, r9)
        while (i < method!!.argSize) {
            val `var` = nextVar().apply {
                register.reg = registers[i]
            }
            lines.add(Line(`var`, `var`))
            locals[i] = `var`
            i++;
        }

        for (insr in method!!.instructions) {
            toSSA(insr)
        }

//        val allRegs = ArrayList<Reg>()
//        for (line in lines) {
//            val regs = arrayListOf(line.left, line.right)
//                    .map { it as? Var }
//                    .filterNotNull()
//                    .map { it.register }
//
//            if (line.res is Var) {
//                allRegs.add(line.res.register)
//                for (r in regs) {
//                    allRegs.add(r)
//                    r.connect(line.res.register)
//                }
//            }
//        }
//
//        val workRegs = ArrayDeque(arrayListOf(rcx, rdx, r8, r9, r10, r11, r12, r13, r14))
//
//
//        val sorted: ArrayList<Reg> = topologicalSort(allRegs)
//        sorted.forEachIndexed { i, reg ->
//            reg.color = i
//            if (reg.reg != null) {
//                workRegs.remove(reg.reg)
//            }
//        }
//        val hold = HashMap<Int, Register>()
//        sorted.forEachIndexed { i, reg ->
//
//            if (reg.reg == null) {
//                reg.reg = workRegs.poll()
//            }
//            hold[reg.connected.maxBy { it.index }?.index ?: Integer.MAX_VALUE] = reg.reg!!
//            val listToR = arrayListOf<Int>()
//            for (k in hold.keys) {
//                if (k <= i) {
//                    workRegs.push(hold[k])
//                    listToR.add(k)
//                }
//            }
//            for (l in listToR) {
//                hold.remove(l)
//            }
//        }

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
                val v = nextVar().apply { register.reg = rax }
                lines.add(Line(v, 0))
                push(v)
            }
            BYTECODES.ICONST_1 -> {
                val v = nextVar().apply { register.reg = rax }
                lines.add(Line(v, 1))
                push(v)
            }
            BYTECODES.IFEQ -> ifeq(instruction.code, instruction.args.first())
            BYTECODES.IF_ICMPLE -> ifcmpop(instruction.code, instruction.args.first())
            BYTECODES.IF_ICMPGT -> ifcmpop(instruction.code, instruction.args.first())
            GOTO -> gotof(instruction.args.first().toString().toInt())
            LABEL -> label(instruction.args.first().toString().toInt())
            else -> throw IllegalArgumentException("Unknown bytecode " + instruction.code.toString())
        }
    }

    private fun ifcmpop(code: BYTECODES, par: Any) {
        val top1 = pop()
        val top2 = pop()
        lines.add(Line(createLbl(par.toString().toInt()), top1, code, top2))
    }

    fun gotof(o: Int) {
        lines.add(Line(createLbl(o), null, GOTO, null))
    }

    fun label(o: Int) {
        lines.add(Line(createLbl(o), null, LABEL, null))
    }

    fun retFun(instruction: Instruction) {
        val `var` = pop()
        lines.add(Line(accum, `var`))
    }

    private val accum: Var = nextVar()

    fun iadd() {
        val a = pop()
        val b = pop()
        val `var` = nextVar()
        lines.add(Line(`var`, a, IADD, b))
        push(`var`)
    }

    fun ifeq(code: BYTECODES, par: Any) {
        val top = pop()
//        lines.add(Line(accum, top))
        lines.add(Line(createLbl(par.toString().toInt()), top, code, null))
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
        if (locals[localIdx] == null) {
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
