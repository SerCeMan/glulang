package ru.serce.glu.gluc.ast;

import ru.serce.glu.gluc.semantic.SymbolInfo;
import ru.serce.glu.gluc.visitors.ASTVisitor;
import ru.serce.glu.gluc.visitors.SimpleVisitor;

import java.util.List;

public interface ASTNode {
    NodeType getNodeType();

    void setSymbolInfo(SymbolInfo si);

    SymbolInfo getSymbolInfo();

    void accept(SimpleVisitor visitor);

    void accept(ASTVisitor visitor);

    void addChild(ASTNode node);

    List<ASTNode> getChildren();

    ASTNode getChild(int index);

}
