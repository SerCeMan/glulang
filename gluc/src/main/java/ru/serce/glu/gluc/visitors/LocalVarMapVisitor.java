package ru.serce.glu.gluc.visitors;

import ru.serce.glu.gluc.ast.ASTNode;
import ru.serce.glu.gluc.ast.IdentifierNode;
import ru.serce.glu.gluc.semantic.SymbolInfo;

public class LocalVarMapVisitor implements SimpleVisitor {
    private int nextLocalVarIndex;

    @Override
    public void visit(ASTNode node) {
        switch (node.getNodeType()) {
            case METHOD_DECLARATION:
                visitMethodDeclarationNode(node);
                break;

            case VARIABLE_DECLARATION:
                visitVariableDeclarationNode(node);
                break;

            default:
                visitAllChildren(node);
        }
    }

    private void visitAllChildren(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
    }

    private void visitMethodDeclarationNode(ASTNode node) {
        nextLocalVarIndex = 0;
        visitAllChildren(node);
    }

    private void visitVariableDeclarationNode(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node.getChild(0);
        SymbolInfo si = idNode.getSymbolInfo();
        si.setLocalVarIndex(nextLocalVarIndex);
        ++nextLocalVarIndex;
    }
}
