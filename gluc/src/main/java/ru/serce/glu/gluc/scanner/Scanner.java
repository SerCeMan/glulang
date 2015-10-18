package ru.serce.glu.gluc.scanner;

import java_cup.runtime.*;
import ru.serce.glu.gluc.parser.sym;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * A CUP compatible scanner.
 */
public class Scanner implements java_cup.runtime.Scanner {

    protected Lexer scanner; // The JFlex produced scanner.
    protected Symbol nextToken; // The lookahead token.

    public Scanner(DataInputStream istream) {
        super();

        scanner = new Lexer(new InputStreamReader(istream));

        try {
            nextToken = scanner.yylex();
        } catch (IOException | ScannerException e) {
            nextToken = null;
        }
    }

    public Symbol peek() {
        return (nextToken == null) ? new Symbol(sym.EOF) : nextToken;
    }

    public Symbol next_token() {
        Symbol old = peek();
        advance();
        return old;
    }

    public void advance() {
        if (nextToken != null) {
            try {
                nextToken = scanner.yylex();
            } catch (IOException | ScannerException e) {
                nextToken = null;
            }
        }
    }
}
