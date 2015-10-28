package ru.serce.glu.gluvm

import groovy.transform.TupleConstructor
import ru.serce.glu.gluc.bytecode.BYTECODES
import ru.serce.glu.gluvm.interpreter.GMethod
import ru.serce.glu.gluvm.interpreter.Instruction

@TupleConstructor
class ByteCodeParser {
    String fileName

    Program parse() {
        def lines = new File(fileName).readLines()
        new Program(parseMethods(lines))
    }

    private Map<String, GMethod> parseMethods(List<String> lines) {
        def bc = lines
                .collect { it.trim() }
                .findAll { !it.isEmpty() }
                .collect { it.split("\\s+") };
        def methods = [:]
        String sig = ''
        List<Instruction> instructions = []

        for (line in bc) {
            if (line[0] == '.method') {
                sig = line[1]
                instructions = []
            } else if (line[0] == '.endmethod') {
                def vars = line[1..-1].collect { it.toInteger() }
                def argSize = vars[0]
                def locals = vars[1]
                methods[sig] = new GMethod(sig, instructions, argSize, locals)
            } else {
                def op = line[0].toUpperCase() as BYTECODES
                instructions << new Instruction(op, line[1..<line.size()])
            }
        }
        return methods
    }
}
