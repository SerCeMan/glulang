package ru.serce.glu.gluc.visitors

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import ru.serce.glu.gluc.ast.*
import ru.serce.glu.gluc.semantic.SymbolInfo

import static ru.serce.glu.gluc.ast.NodeType.*
import static ru.serce.glu.gluc.bytecode.BYTECODES.*

@CompileStatic
public class CodeGenVisitor implements SimpleVisitor {
    private PrintStream stream
    private int labelIndex
    private CompilationUnit declarationsNode
    private int localsCount


    public CodeGenVisitor(PrintStream stream) {
        this.stream = stream
    }

    @Override
    public void visit(ASTNode node) {
        switch (node.getNodeType()) {
            case ADDITION:
                visitAdditionNode(node)
                break

            case ASSIGN:
                visitAssignNode(node)
                break

            case BOOLEAN_AND:
                visitBooleanAndNode(node)
                break

            case BOOLEAN_LITERAL:
                visitBooleanLiteralNode(node)
                break

            case BOOLEAN_NOT:
                visitBooleanNotNode(node)
                break

            case BOOLEAN_OR:
                visitBooleanOrNode(node)
                break

            case DIVISION:
                visitDivisionNode(node)
                break

            case EQUAL:
                visitEqualNode(node)
                break

            case GREATER_THAN:
                visitGreaterThanNode(node)
                break

            case GREATER_THAN_OR_EQUAL:
                visitGreaterThanOrEqualNode(node)
                break

            case IF_STATEMENT:
                visitIfStatementNode(node)
                break

            case INTEGER_LITERAL:
                visitIntegerLiteralNode(node)
                break

            case LESS_THAN:
                visitLessThanNode(node)
                break

            case LESS_THAN_OR_EQUAL:
                visitLessThanOrEqualNode(node)
                break

            case METHOD_ACCESS:
                visitMethodAccessNode(node)
                break

            case METHOD_DECLARATION:
                visitMethodDeclarationNode(node)
                break

            case MULTIPLICATION:
                visitMultiplicationNode(node)
                break

            case NOT_EQUAL:
                visitNotEqualNode(node)
                break

            case PARAMETER:
                visitParameterNode(node)
                break

            case RETURN_STATEMENT:
                visitReturnStatementNode(node)
                break

            case SUBTRACTION:
                visitSubtractionNode(node)
                break

            case UNARY_MINUS:
                visitUnaryMinusNode(node)
                break

            case UNARY_PLUS:
                visitUnaryPlusNode(node)
                break

            case VAR_USE:
                visitVarUse(node)
                break

            case WHILE_STATEMENT:
                visitWhileStatementNode(node)
                break


            case COMPILATION_UNIT:
                visitCompUnit(node)
                break

            case VARIABLE_DECLARATION:
                visitVarDecl(node)
                break;

            default:
                visitAllChildren(node)
        }
    }

    def visitVarDecl(ASTNode node) {
        localsCount++
        visitAllChildren(node)
    }

    private void visitCompUnit(ASTNode node) {
        declarationsNode = (CompilationUnit) node
        for (ASTNode child : node.getChildren()) {
            child.accept(this)
        }
    }


    private void visitAllChildren(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            child.accept(this)
        }
    }

    private void visitAdditionNode(ASTNode node) {
        visitAllChildren(node)
        stream.println(IADD.name)
    }

    private void visitAssignNode(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node.getChild(0)
        SymbolInfo si = idNode.getSymbolInfo()
        int idx = si.localVarIndex
        List<ASTNode> children = node.children
        for (child in children) {
            child.accept(this)
        }
        stream.println("istore $idx")
    }

    private void visitBooleanAndNode(ASTNode node) {
        node.getChild(0).accept(this)
        node.getChild(1).accept(this)
        stream.println("iand")
    }

    private void visitBooleanLiteralNode(ASTNode node) {
        BooleanLiteralNode boolNode = (BooleanLiteralNode) node
        if (boolNode.getValue()) {
            stream.println("iconst_1")
        } else {
            stream.println("iconst_0")
        }
    }

    private void visitBooleanNotNode(ASTNode node) {
        node.getChild(0).accept(this)
        stream.println('''iconst_1
                       ixor''')
    }

    private void visitBooleanOrNode(ASTNode node) {
        node.getChild(0).accept(this)
        node.getChild(1).accept(this)
        stream.println("ior")
    }

    private void visitDivisionNode(ASTNode node) {
        visitAllChildren(node)
        stream.println("idiv")
    }

    private void visitIntegerLiteralNode(ASTNode node) {
        int val = ((IntegerLiteralNode) node).getValue()
        stream.println("ldc $val")
    }

    private void compare(ASTNode node, String inst) {
        node.getChild(0).accept(this)
        node.getChild(1).accept(this)
        def trueLabel = nextLabel()
        def falseLabel = nextLabel()
        stream.println("""
        $inst $trueLabel
        iconst_0
        goto $falseLabel.index
        $trueLabel
        iconst_1
        $falseLabel
        """)
    }

    private void visitEqualNode(ASTNode node) {
        compare(node, "if_icmpeq")
    }

    private void visitGreaterThanNode(ASTNode node) {
        compare(node, "if_icmpgt")
    }

    private void visitGreaterThanOrEqualNode(ASTNode node) {
        compare(node, "if_icmpge")
    }

    private void visitLessThanNode(ASTNode node) {
        compare(node, "if_icmplt")
    }

    private void visitLessThanOrEqualNode(ASTNode node) {
        compare(node, "if_icmple")
    }

    private void visitNotEqualNode(ASTNode node) {
        compare(node, "if_icmpne")
    }

    private void visitIfStatementNode(ASTNode node) {
        node.getChild(0).accept(this) //predicate
        def endIfLabel = nextLabel()
        def elseLabel = nextLabel()
        stream.println("ifeq $elseLabel.index")
        node.getChild(1).accept(this)
        stream.println("goto $endIfLabel.index")
        stream.println(elseLabel)
        if (node.getChildren().size() == 3) {
            node.getChild(2).accept(this)
        }
        stream.println(endIfLabel)
    }

    private void visitMethodAccessNode(ASTNode node) {
        node.getChild(1).accept(this)
        IdentifierNode iden = (IdentifierNode) node.getChild(0)
        String methodName = iden.getValue()
        String sig = declarationsNode.getMethod(methodName)
        stream.println("invoke $sig")
    }

    private void visitMethodDeclarationNode(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node.getChild(0)
        String methodName = idNode.getValue()

        TypeNode typeNode = (TypeNode) node.getChild(2)
        String returnType = typeNode.getType().getSignature()
        def parameters = node.getChild(1)
        int argSize = parameters.children.size()

        int prevLocalsCount = localsCount
        localsCount = 0

        stream.print(".method $methodName(")
        parameters.accept(this)
        stream.println(")$returnType")
        node.getChild(3).accept(this)
        stream.println(".endmethod $argSize $localsCount")

        localsCount = prevLocalsCount
    }

    private void visitMultiplicationNode(ASTNode node) {
        visitAllChildren(node)
        stream.println("imul")
    }

    private void visitParameterNode(ASTNode node) {
        TypeNode typeNode = (TypeNode) node.getChild(1)
        String typeSig = typeNode.getType().getSignature()
        stream.print(typeSig)
    }

    private void visitReturnStatementNode(ASTNode node) {
        visitAllChildren(node)
        stream.println("return")
    }

    private void visitSubtractionNode(ASTNode node) {
        visitAllChildren(node)
        stream.println("isub")
    }

    private void visitUnaryMinusNode(ASTNode node) {
        visitAllChildren(node)
        stream.println('ineg')
    }

    private void visitUnaryPlusNode(ASTNode node) {
        visitAllChildren(node)
    }

    private void visitVarUse(ASTNode node) {
        IdentifierNode idNode = (IdentifierNode) node.getChild(0)
        SymbolInfo si = idNode.getSymbolInfo()
        int lvIndex = si.getLocalVarIndex()
        stream.println("iload $lvIndex")
    }

    private void visitWhileStatementNode(ASTNode node) {
        def startLabel = nextLabel()
        def exitLabel = nextLabel()

        stream.println("$startLabel")
        node.getChild(0).accept(this)
        stream.println("ifeq $exitLabel.index")
        node.getChild(1).accept(this)
        stream.println("""
        goto $startLabel.index
        $exitLabel
        """)
    }

    static class Label {
        int index

        @Override
        String toString() {
            "label $index"
        }
    }

    private Label nextLabel() {
        int idx = ++labelIndex
        new Label(index: idx)
    }
}
