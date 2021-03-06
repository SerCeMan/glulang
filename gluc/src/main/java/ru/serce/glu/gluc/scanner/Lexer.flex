package ru.serce.glu.gluc.scanner;

import java_cup.runtime.*;
import ru.serce.glu.gluc.parser.sym;

import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

%%

%class Lexer
%type Symbol
%unicode
%line
%column

%{
    StringBuffer stringBuffer = new StringBuffer();
%}

NEWLINE    = \r|\n|\r\n
WHITESPACE = [ \t\f]
DIGIT=[0-9]
ALPHA=[A-Za-z_]

%state STRING

%%

<YYINITIAL> {
"/*"([^*]|"*"+[^/*])*"*"+"/" { /* ignore */}
"//"([^\n])*{NEWLINE}        { /* ignore */}

{WHITESPACE}+ { /* ignore */ }
{NEWLINE}     { /* ignore */ }

"boolean"      { return new Symbol(sym.BOOLEAN,      yychar, yychar + yytext().length() - 1); }
"int"          { return new Symbol(sym.INT,          yychar, yychar + yytext().length() - 1); }
"double"       { return new Symbol(sym.DOUBLE,       yychar, yychar + yytext().length() - 1); }
"{"            { return new Symbol(sym.LBRACE,       yychar, yychar + yytext().length() - 1); }
"}"            { return new Symbol(sym.RBRACE,       yychar, yychar + yytext().length() - 1); }
"("            { return new Symbol(sym.LPAREN,       yychar, yychar + yytext().length() - 1); }
")"            { return new Symbol(sym.RPAREN,       yychar, yychar + yytext().length() - 1); }
","            { return new Symbol(sym.COMMA,        yychar, yychar + yytext().length() - 1); }
";"            { return new Symbol(sym.SEMICOLON,    yychar, yychar + yytext().length() - 1); }
"void"         { return new Symbol(sym.VOID,         yychar, yychar + yytext().length() - 1); }
"if"           { return new Symbol(sym.IF,           yychar, yychar + yytext().length() - 1); }
"else"         { return new Symbol(sym.ELSE,         yychar, yychar + yytext().length() - 1); }
"while"        { return new Symbol(sym.WHILE,        yychar, yychar + yytext().length() - 1); }
"return"       { return new Symbol(sym.RETURN,       yychar, yychar + yytext().length() - 1); }
"+"            { return new Symbol(sym.PLUSOP,       yychar, yychar + yytext().length() - 1); }
"-"            { return new Symbol(sym.MINUSOP,      yychar, yychar + yytext().length() - 1); }
"/"            { return new Symbol(sym.SLASH,        yychar, yychar + yytext().length() - 1); }
"*"            { return new Symbol(sym.ASTERISK,     yychar, yychar + yytext().length() - 1); }
"!"            { return new Symbol(sym.BANG,         yychar, yychar + yytext().length() - 1); }
"<"            { return new Symbol(sym.OP_LT,        yychar, yychar + yytext().length() - 1); }
">"            { return new Symbol(sym.OP_GT,        yychar, yychar + yytext().length() - 1); }
"<="           { return new Symbol(sym.OP_LE,        yychar, yychar + yytext().length() - 1); }
">="           { return new Symbol(sym.OP_GE,        yychar, yychar + yytext().length() - 1); }
"=="           { return new Symbol(sym.OP_EQ,        yychar, yychar + yytext().length() - 1); }
"!="           { return new Symbol(sym.OP_NE,        yychar, yychar + yytext().length() - 1); }
"&&"           { return new Symbol(sym.OP_AND,       yychar, yychar + yytext().length() - 1); }
"||"           { return new Symbol(sym.OP_OR,        yychar, yychar + yytext().length() - 1); }
"="            { return new Symbol(sym.EQUALS,       yychar, yychar + yytext().length() - 1); }
"true"         { return new Symbol(sym.TRUE,         yychar, yychar + yytext().length() - 1); }
"false"        { return new Symbol(sym.FALSE,        yychar, yychar + yytext().length() - 1); }

{ALPHA}({ALPHA}|{DIGIT})* { return new Symbol(sym.IDENTIFIER, yychar, yychar + yytext().length() - 1, yytext()); }
[\"]                      { stringBuffer.setLength(0); stringBuffer.append('\"'); yybegin(STRING); }
{DIGIT}+\.{DIGIT}+        { return new Symbol(sym.FLOAT_LIT,  yychar, yychar + yytext().length() - 1, new Double(yytext())); }
{DIGIT}+                  { return new Symbol(sym.INT_LIT,    yychar, yychar + yytext().length() - 1, new Integer(yytext())); }
}

<STRING> {
    \"            { yybegin(YYINITIAL); stringBuffer.append('\"'); return new Symbol(sym.STRING_LIT, yychar, yychar + yytext().length() - 1, stringBuffer.toString()); }
    [^\n\r\"\\]+  { stringBuffer.append(yytext()); }
    \\t           { stringBuffer.append('\t'); }
    \\n           { stringBuffer.append('\n'); }
    \\r           { stringBuffer.append('\r'); }
    \\\"          { stringBuffer.append('\\').append('\"'); }
    \\\\          { stringBuffer.append('\\').append('\\'); }
    \\            { stringBuffer.append('\\'); }
  }

. { throw new ScannerException("Illegal character \"" + yytext() + "\""); }
