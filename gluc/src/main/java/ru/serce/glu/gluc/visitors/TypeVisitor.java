package ru.serce.glu.gluc.visitors;

import ru.serce.glu.gluc.ast.ASTNode;
import ru.serce.glu.gluc.ast.CompilationUnit;
import ru.serce.glu.gluc.ast.IdentifierNode;
import ru.serce.glu.gluc.ast.PrimitiveType;
import ru.serce.glu.gluc.ast.TypeNode;
import ru.serce.glu.gluc.semantic.SymbolInfo;
import ru.serce.glu.gluc.semantic.SymbolTable;

public class TypeVisitor implements SimpleVisitor {
    private SymbolTable symbolTable = new SymbolTable();
    private PrimitiveType currentType;
    private CompilationUnit declarationsNode;
    
    @Override
    public void visit(ASTNode node) {
        switch (node.getNodeType()) {
        case BLOCK:
            visitBlockNode(node);
            break;
        
        case DECLARATIONS:
            visitDeclNode(node);
            break;
            
        case IDENTIFIER:
            visitIdentifierNode(node);
            break;
            
        case LOCAL_VAR_DECLARATION:
            visitLocalVarDeclarationNode(node);
            break;
            
        case METHOD_DECLARATION:
            visitMethodDeclarationNode(node);
            break;
            
        case PARAMETER:
            visitParameterNode(node);
            break;
            
        case VARIABLE_DECLARATION:
            visitVariableDeclarationNode(node);
            break;

        case COMPILATION_UNIT:
            visitCompUnit(node);
            break;

        default:
            visitAllChildren(node);
        }
    }

    private void visitCompUnit(ASTNode node) {
        declarationsNode = (CompilationUnit) node;
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
    }

    private void visitAllChildren(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
    }
    
    private void visitBlockNode(ASTNode node) {
        symbolTable.enterScope();
        visitAllChildren(node);
        symbolTable.leaveScope();
    }
    
    private void visitDeclNode(ASTNode node) {
        symbolTable.enterScope();
        visitAllChildren(node);
        symbolTable.leaveScope();
    }
    
    private void visitIdentifierNode(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node;
        String id = idNode.getValue();
        SymbolInfo si = symbolTable.get(id);
        node.setSymbolInfo(si);
    }

    private void visitLocalVarDeclarationNode(ASTNode node) {
        TypeNode typeNode = (TypeNode) node.getChild(0);
        currentType = typeNode.getType();
        
        node.getChild(1).accept(this);
        
        currentType = null;
    }
    
    private void visitMethodDeclarationNode(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node.getChild(0);
        String methodName = idNode.getValue();
        
        String sig = methodName + "(";
        
        for (ASTNode paramNode : node.getChild(1).getChildren()) {
            TypeNode typeNode = (TypeNode) paramNode.getChild(1);
            sig += typeNode.getType().getSignature();
        }
        
        sig += ")";
        
        TypeNode typeNode = (TypeNode) node.getChild(2);
        sig += typeNode.getType().getSignature();
        
        declarationsNode.putMethod(methodName, sig);
        
        symbolTable.enterScope();
        visitAllChildren(node);
        symbolTable.leaveScope();
    }
    
    private void visitParameterNode(ASTNode node) {
        TypeNode typeNode = (TypeNode) node.getChild(1);
        currentType = typeNode.getType();
        
        node.getChild(0).accept(this);
        
        currentType = null;
    }
    
    private void visitVariableDeclarationNode(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node.getChild(0);
        String id = idNode.getValue();
        
        SymbolInfo si = new SymbolInfo(node);
        si.setType(currentType);
        
        symbolTable.put(id, si);
        
        visitAllChildren(node);
    }
}
