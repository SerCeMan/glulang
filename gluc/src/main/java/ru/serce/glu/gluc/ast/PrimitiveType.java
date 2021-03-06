package ru.serce.glu.gluc.ast;

public enum PrimitiveType {
    BOOLEAN ("Z"),
    CHAR ("C"),
    DOUBLE ("D"),
    INT ("I"),
    VOID ("V");
    
    private final String signature;
    
    PrimitiveType(String signature) {
        this.signature = signature;
    }
    
    public String getSignature() {
        return signature;    
    }
}
