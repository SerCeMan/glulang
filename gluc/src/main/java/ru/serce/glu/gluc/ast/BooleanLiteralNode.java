package ru.serce.glu.gluc.ast;

public class BooleanLiteralNode extends BaseASTNode {
    private final boolean value;

    public BooleanLiteralNode(boolean value) {
        super(NodeType.BOOLEAN_LITERAL);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getNodeType() + " (" + value + ")";
    }
}
