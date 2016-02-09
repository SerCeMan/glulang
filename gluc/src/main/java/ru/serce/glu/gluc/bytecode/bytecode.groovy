package ru.serce.glu.gluc.bytecode

import groovy.transform.CompileStatic

@CompileStatic
enum BYTECODES {
    ALOAD,
    IADD,
    ISTORE,
    IAND,
    ICONST_1,
    ICONST_0,
    IXOR,
    IOR,
    IDIV,
    LDC,
    IF_ICMPEQ,
    IF_ICMPGT,
    IF_ICMPGE,
    IF_ICMPLT,
    IF_ICMPLE,
    IF_ICMPNE,
    GOTO,
    INVOKE,
    IMUL,
    LABEL,
    RETURN,
    ISUB,
    INEG,
    ILOAD,
    IFEQ,

    private byte inx = 0;

    String name = name().toLowerCase()
    byte index = ++inx;




}
