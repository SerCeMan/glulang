package ru.serce.glu.gluc.ast;

import ru.serce.glu.gluc.semantic.SymbolInfo;
import ru.serce.glu.gluc.visitors.ASTVisitor;
import ru.serce.glu.gluc.visitors.SimpleVisitor;

import java.util.ArrayList;
import java.util.List;

public class BaseASTNode implements ASTNode {
    protected List<ASTNode> children = new ArrayList<>();
    protected NodeType nodeType;
    protected SymbolInfo symbolInfo;

    public BaseASTNode(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setSymbolInfo(SymbolInfo si) {
        this.symbolInfo = si;
    }

    public SymbolInfo getSymbolInfo() {
        return symbolInfo;
    }

    @Override
    public String toString() {
        String str = nodeType.toString();

        if (symbolInfo != null) {
            str += " (" + symbolInfo.toString() + ")";
        }

        return str;
    }

    @Override
    public void accept(SimpleVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.previsit(this);

        for (ASTNode child : children) {
            child.accept(visitor);
        }

        visitor.postvisit(this);
    }

    @Override
    public void addChild(ASTNode node) {
        children.add(node);
    }

    @Override
    public ASTNode getChild(int index) {
        return children.get(index);
    }

    @Override
    public List<ASTNode> getChildren() {
        return children;
    }

}
