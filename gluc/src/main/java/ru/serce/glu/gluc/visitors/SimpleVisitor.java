/*
 * CSE 431S Programming Assignment 3
 */

package ru.serce.glu.gluc.visitors;

import ru.serce.glu.gluc.ast.ASTNode;

public interface SimpleVisitor {
    void visit(ASTNode node);
}
