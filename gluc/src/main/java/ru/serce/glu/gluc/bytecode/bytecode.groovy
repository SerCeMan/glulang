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
    IF_CMPEQ,
    IF_ICMPGT,
    IF_ICMPGE,
    IF_ICMPLT,
    IF_ICMPNE,
    IF_EQ,
    GOTO,
    INVOKE,
    IMUL,
    LABEL,
    RETURN,
    ISUB,
    INEG,
    ILOAD,
    IFEQ;

    private byte inx = 0;

    String name = name().toLowerCase()
    byte index = ++inx;
}
