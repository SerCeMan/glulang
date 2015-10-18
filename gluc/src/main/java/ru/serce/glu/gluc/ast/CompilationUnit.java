package ru.serce.glu.gluc.ast;

import java.util.HashMap;
import java.util.Map;

public class CompilationUnit extends BaseASTNode {

    private Map<String, String> methodMap = new HashMap<>();

    public CompilationUnit() {
        super(NodeType.COMPILATION_UNIT);
        methodMap.put("println", "println(I)V");
        methodMap.put("read", "read()I");
    }


    public void putMethod(String name, String sig) {
        methodMap.put(name, sig);
    }

    public String getMethod(String name) {
        return methodMap.get(name);
    }
}
