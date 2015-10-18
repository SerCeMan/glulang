/*
 * CSE 431S Programming Assignment 3
 */

package ru.serce.glu.gluc.visitors;

import ru.serce.glu.gluc.ast.ASTNode;

/**
 * The AST Visitor interface.
 */
public interface ASTVisitor {
    void previsit(ASTNode node);
    void postvisit(ASTNode node);

    void executePrevisit(ASTNode node);
    void executePostvisit(ASTNode node);
}
