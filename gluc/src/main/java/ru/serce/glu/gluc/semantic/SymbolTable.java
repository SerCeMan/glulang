package ru.serce.glu.gluc.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
    private List<HashMap<String, SymbolInfo>> scopes = new ArrayList<>();
    private HashMap<String, SymbolInfo> currentScope = new HashMap<>();

    public void enterScope() {
        scopes.add(currentScope);
        currentScope = new HashMap<>();
    }

    public void leaveScope() {
        int index = scopes.size() - 1;
        currentScope = scopes.remove(index);
    }

    public void put(String id, SymbolInfo si) {
        if (currentScope.containsKey(id)) {
            throw new SemanticException("current scope already contains an entry for " + id);
        }

        currentScope.put(id, si);
    }

    public SymbolInfo get(String id) {
        SymbolInfo si = currentScope.get(id);

        if (si != null) {
            return si;
        }

        for (int i = scopes.size() - 1; i >= 0; --i) {
            HashMap<String, SymbolInfo> scope = scopes.get(i);
            si = scope.get(id);
            if (si != null) {
                return si;
            }
        }

        return null;
    }
}
