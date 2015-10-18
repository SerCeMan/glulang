package ru.serce.glu.gluvm

import groovy.transform.Canonical
import ru.serce.glu.gluvm.interpreter.GMethod

class Program {
    def Map<String, GMethod> methods

    Program(Map<String, GMethod> methods) {
        this.methods = methods
    }
}
