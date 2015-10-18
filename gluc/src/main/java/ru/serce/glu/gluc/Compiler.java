package ru.serce.glu.gluc;

import ru.serce.glu.gluc.ast.CompilationUnit;
import ru.serce.glu.gluc.parser.Parser;
import ru.serce.glu.gluc.scanner.Scanner;
import ru.serce.glu.gluc.visitors.TypeVisitor;
import ru.serce.glu.gluc.visitors.CodeGenVisitor;
import ru.serce.glu.gluc.visitors.LocalVarMapVisitor;
import ru.serce.glu.gluc.visitors.PrinterVisitor;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;

/**
 * @author serce
 * @since 17.10.15.
 */
public class Compiler {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: gluc name");
            System.exit(-1);
        }

        Compiler compiler = new Compiler(args[0]);
        compiler.run();
    }

    private final String source;

    private Compiler(String source) {
        this.source = source;
    }

    private void run() {
        CompilationUnit cu = parse();
        typeAnalysis(cu);
        localVarMapping(cu);
        generateCode(cu);
//        printAST(cu);
    }

    private CompilationUnit parse() {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new FileInputStream(source)))) {
            Parser parser = new Parser(new Scanner(stream));
            parser.parse();
            return parser.getRoot();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printAST(CompilationUnit cu) {
        cu.accept(new PrinterVisitor(System.out));
    }

    private void typeAnalysis(CompilationUnit cu) {
        cu.accept(new TypeVisitor());
    }

    private void localVarMapping(CompilationUnit cu) {
        cu.accept(new LocalVarMapVisitor());
    }

    private void generateCode(CompilationUnit cu) {
        cu.accept(new CodeGenVisitor(System.out));
    }
}
