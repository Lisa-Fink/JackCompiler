import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private class SymbolTableEntry {
        public final String type;
        public final KIND kind;
        public final int number;

        public SymbolTableEntry(String type, KIND kind, int number) {
            this.type = type;
            this.kind = kind;
            this.number = number;
        }
    }
    private Map<String, SymbolTableEntry> table;
    private Map<KIND, Integer> indexMap;
    
    public enum KIND {STATIC, FIELD, ARG, VAR, NONE}
    public SymbolTable() {
        this.table = new HashMap<>();
        this.indexMap = new HashMap<>();
        
        indexMap.put(KIND.STATIC, 0);
        indexMap.put(KIND.FIELD, 0);
        indexMap.put(KIND.ARG, 0);
        indexMap.put(KIND.VAR, 0);
    }
    
    public void reset() {
        this.table = new HashMap<>();
        indexMap.put(KIND.STATIC, 0);
        indexMap.put(KIND.FIELD, 0);
        indexMap.put(KIND.ARG, 0);
        indexMap.put(KIND.VAR, 0);
    }
    
    public void define(String name, String type, KIND kind) {
        int number = indexMap.get(kind);
        indexMap.put(kind, number + 1);

        SymbolTableEntry entry = new SymbolTableEntry(type, kind, number);
        table.put(name, entry);
    }
    
    public int varCount(KIND kind) {
        return indexMap.get(kind);
    }
    
    public KIND kindOf(String name) {
        SymbolTableEntry entry = table.get(name);
        if (entry == null) return KIND.NONE;
        return entry.kind;
    }
    
    public String typeOf(String name) {
        return table.get(name).type;
    }
    
    public int indexOf(String name) {
        return table.get(name).number;
    }
}
