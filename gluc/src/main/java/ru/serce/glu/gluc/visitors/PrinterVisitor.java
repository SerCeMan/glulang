package ru.serce.glu.gluc.visitors;

import ru.serce.glu.gluc.ast.ASTNode;

import java.io.PrintStream;


public class PrinterVisitor implements ASTVisitor {
    private PrintStream stream;
    private int level = -1;


    public PrinterVisitor(PrintStream stream) {
        this.stream = stream;
    }


    public void executePrevisit(ASTNode node) {
        stream.println(indent() + node.toString());
    }


    public void executePostvisit(ASTNode node) {
        /* do nothing */
    }

    public final void previsit(ASTNode node) {
        ++level;
        executePrevisit(node);
    }

    public final void postvisit(ASTNode node) {
        executePostvisit(node);
        --level;
    }

    public int getLevel() {
        return level;
    }

    protected String indent() {
        StringBuilder offset = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            offset.append("    ");
        }
        return offset.toString();
    }
}
