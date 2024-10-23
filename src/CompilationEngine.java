import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CompilationEngine {
    FileWriter output;
    Tokenizer tokenizer;
    SymbolTable classTable;
    SymbolTable subroutineTable;
    VMWriter vmWriter;
    String className;
    int length;

    private static final Map<Tokenizer.KeyWord, String> KEYMAP = new HashMap<>();
    static {
        KEYMAP.put(Tokenizer.KeyWord.CLASS, "class");
        KEYMAP.put(Tokenizer.KeyWord.CONSTRUCTOR, "constructor");
        KEYMAP.put(Tokenizer.KeyWord.FUNCTION, "function");
        KEYMAP.put(Tokenizer.KeyWord.METHOD, "method");
        KEYMAP.put(Tokenizer.KeyWord.FIELD, "field");
        KEYMAP.put(Tokenizer.KeyWord.STATIC, "static");
        KEYMAP.put(Tokenizer.KeyWord.VAR, "var");
        KEYMAP.put(Tokenizer.KeyWord.INT, "int");
        KEYMAP.put(Tokenizer.KeyWord.CHAR, "char");
        KEYMAP.put(Tokenizer.KeyWord.BOOLEAN, "boolean");
        KEYMAP.put(Tokenizer.KeyWord.VOID, "void");
        KEYMAP.put(Tokenizer.KeyWord.TRUE, "true");
        KEYMAP.put(Tokenizer.KeyWord.FALSE, "false");
        KEYMAP.put(Tokenizer.KeyWord.NULL, "null");
        KEYMAP.put(Tokenizer.KeyWord.THIS, "this");
        KEYMAP.put(Tokenizer.KeyWord.LET, "let");
        KEYMAP.put(Tokenizer.KeyWord.DO, "do");
        KEYMAP.put(Tokenizer.KeyWord.IF, "if");
        KEYMAP.put(Tokenizer.KeyWord.ELSE, "else");
        KEYMAP.put(Tokenizer.KeyWord.WHILE, "while");
        KEYMAP.put(Tokenizer.KeyWord.RETURN, "return");
    }

    public CompilationEngine(Tokenizer tokenizer, FileWriter output) {
        this.output = output;
        this.tokenizer = tokenizer;
        this.vmWriter = new VMWriter(output);
    }

    public void compileClass() throws IOException {
        this.classTable = new SymbolTable();
        this.subroutineTable = new SymbolTable();

        // class
        tokenizer.advance();
        // className
        this.className = tokenizer.identifier();
        tokenizer.advance();

        // opening  {
        tokenizer.advance();

        while (tokenizer.hasMoreTokens() &
                tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD &
                (tokenizer.keyWord() == Tokenizer.KeyWord.STATIC |
                        tokenizer.keyWord() == Tokenizer.KeyWord.FIELD)) {
            compileClassVarDec();
        }

        while (tokenizer.hasMoreTokens() &
                (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL &
                        tokenizer.symbol() != '}')) {
            // remaining spots have to be subroutines
            compileSubroutine();
        }

        // closing }
        tokenizer.advance();
        vmWriter.close();
    }

    public void compileClassVarDec() throws IOException {
        output.write("<classVarDec>\n");

        // field or static
        SymbolTable.KIND kind;
        if (Objects.equals(KEYMAP.get(tokenizer.keyWord()), "field")) {
            kind = SymbolTable.KIND.FIELD;
        } else {
            kind = SymbolTable.KIND.STATIC;
        }
        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // field/static
        tokenizer.advance();

        // type
        String type;
        // can be a keyword or an identifier (if it's a class type)
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // type
            type = KEYMAP.get(tokenizer.keyWord());
            tokenizer.advance();
        } else {
            output.write("<classIdentifierDef> " + tokenizer.identifier() + " </classIdentifierDef>\n");  // class type
            type = tokenizer.identifier();
            tokenizer.advance();
        }
        char last;
        String name;
        do {
            name = tokenizer.identifier();
            classTable.define(name, type, kind);
            output.write("<fieldStaticIdentifierDef> " + tokenizer.identifier() + classTable.kindOf(name) + classTable.indexOf(name) + " </fieldStaticIdentifierDef>\n");
            tokenizer.advance();


            writeSymbol();  // ending ; or ,
            last = tokenizer.symbol();
            tokenizer.advance();
        } while (last == ',');

        output.write("</classVarDec>\n");
    }

    public void compileSubroutine() throws IOException {
        boolean isVoid = false;
        String subroutineName;
        subroutineTable.reset();
        boolean hasThis = false;

        // constructor/method/function
        // Add this to symbol table if method, don't do this for function or constructor
        if (tokenizer.keyWord() == Tokenizer.KeyWord.METHOD)  {
            subroutineTable.define("this", this.className, SymbolTable.KIND.ARG);
            hasThis = true;
        }

        if (tokenizer.keyWord() == Tokenizer.KeyWord.CONSTRUCTOR) {
            tokenizer.advance();
             // if constructor
             // class type
             tokenizer.advance();
             // "new"
             subroutineName = tokenizer.identifier();
             tokenizer.advance();
        } else {
            tokenizer.advance();
            // in a method or function
            // return type
            if (tokenizer.keyWord() == Tokenizer.KeyWord.VOID) isVoid = true;
            tokenizer.advance();

            // method/function name
            subroutineName = tokenizer.identifier();
            tokenizer.advance();
        }

        // opening (
        tokenizer.advance();

        compileParameterList();
        int nArgs = subroutineTable.varCount(SymbolTable.KIND.ARG);
        if (hasThis) nArgs--;
        vmWriter.writeFunction(className + "." + subroutineName, nArgs);

        // closing )
        tokenizer.advance();

        compileSubroutineBody();
    }

    public void compileParameterList() throws IOException {
        String type, name;
        // process 0 or more parameters
        while (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL & tokenizer.symbol() != ')') {
            // type
            type = KEYMAP.get(tokenizer.keyWord());
            tokenizer.advance();
            // name
            name = tokenizer.identifier();
            subroutineTable.define(name, type, SymbolTable.KIND.ARG);

            tokenizer.advance();

            // only if there is a ','
            if (tokenizer.symbol() != ')') {
                tokenizer.advance();
            }
        }
    }

    public void compileSubroutineBody() throws IOException {
        // opening {
        tokenizer.advance();

        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD &
                tokenizer.keyWord() == Tokenizer.KeyWord.VAR) {
            compileVarDec();
        }

        compileStatements();

        // closing }
        tokenizer.advance();
    }

    public void compileVarDec() throws IOException {
        // var
        tokenizer.advance();
        // type
        String type;
        // type can be a keyword or an identifier (if it's a class type)
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            type = KEYMAP.get(tokenizer.keyWord());
            tokenizer.advance();
        } else {
            type = tokenizer.identifier();
            tokenizer.advance();
        }
        // name
        String name;
        name = tokenizer.identifier();
        subroutineTable.define(name, type, SymbolTable.KIND.VAR);
        tokenizer.advance();

        while (tokenizer.symbol() != ';') {
            // ,
            tokenizer.advance();
            name = tokenizer.identifier();
            subroutineTable.define(name, type, SymbolTable.KIND.VAR);
            tokenizer.advance();
        }

        // ;
        tokenizer.advance();
    }

    public void compileStatements() throws IOException {
        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            if (tokenizer.keyWord() == Tokenizer.KeyWord.LET) {
                compileLet();
            } else if (tokenizer.keyWord() == Tokenizer.KeyWord.IF) {
                compileIf();
            } else if (tokenizer.keyWord() == Tokenizer.KeyWord.WHILE) {
                compileWhile();
            } else if (tokenizer.keyWord() == Tokenizer.KeyWord.DO) {
                compileDo();
            } else if (tokenizer.keyWord() == Tokenizer.KeyWord.RETURN) {
                compileReturn();
            }
        }
    }

    public void compileLet () throws IOException {
        // let
        tokenizer.advance();

        // var or arg
        // either in class or subroutine table
        // check class table
        VMWriter.SEGMENT assigneeSegment;
        int index;
        if (classTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
            assigneeSegment = VMWriter.KIND_TO_SEGMENT.get(classTable.kindOf(tokenizer.identifier()));
            index = classTable.indexOf(tokenizer.identifier());
        } else {
            assigneeSegment = VMWriter.KIND_TO_SEGMENT.get(subroutineTable.kindOf(tokenizer.identifier()));
            index = subroutineTable.indexOf(tokenizer.identifier());
        }

        tokenizer.advance();
        // TODO: does this need to handle complex identifiers with . or []?
//        handleIdentifier();


        // =
        tokenizer.advance();

        compileExpression();
        vmWriter.writePop(assigneeSegment, index);

        // ;
        tokenizer.advance();
    }

    public void compileIf () throws IOException {
        output.write("<ifStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // if keyword
        tokenizer.advance();

        writeSymbol();  // (
        tokenizer.advance();

        compileExpression();

        writeSymbol();  // )
        tokenizer.advance();

        writeSymbol();  // {
        tokenizer.advance();

        compileStatements();

        writeSymbol();  // }
        tokenizer.advance();

        // 0 or more else statements
        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD & tokenizer.keyWord() == Tokenizer.KeyWord.ELSE) {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // else keyword
            tokenizer.advance();

            writeSymbol();  // {
            tokenizer.advance();

            compileStatements();

            writeSymbol();  // }
            tokenizer.advance();
        }

        output.write("</ifStatement>\n");
    }

    public void compileWhile () throws IOException {
        output.write("<whileStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // while keyword
        tokenizer.advance();

        writeSymbol();  // (
        tokenizer.advance();

        compileExpression();

        writeSymbol();  // )
        tokenizer.advance();

        writeSymbol();  // {
        tokenizer.advance();

        compileStatements();

        writeSymbol();  // }
        tokenizer.advance();

        output.write("</whileStatement>\n");
    }

    public void compileDo() throws IOException {
        tokenizer.advance();  // do
        compileExpression();
        tokenizer.advance();  // ;
        vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);
    }

    private void handleIdentifier() throws IOException {
        // if next char is a . or [
        while (tokenizer.symbol() == '.' | tokenizer.symbol() == '[') {
            if (tokenizer.symbol() == '.') {
                writeSymbol();  // .
                tokenizer.advance();

                output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // class method
                tokenizer.advance();

                if (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL && tokenizer.symbol() == '(') {
                    // handle the list of params
                    writeSymbol();  // (
                    tokenizer.advance();

                    compileExpressionList();

                    writeSymbol();  // )
                    tokenizer.advance();
                }
            } else {
                writeSymbol();  // [
                tokenizer.advance();

                compileExpression();

                writeSymbol();  // ]
                tokenizer.advance();
            }
        }
    }

    public void compileReturn() throws IOException {
        // See if it is returning a value or not
        // "return"
        tokenizer.advance();
        boolean isVoid = tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL & tokenizer.symbol() == ';';
        while (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL & tokenizer.symbol() != ';') {
            compileExpression();
        }

        // ;
        tokenizer.advance();
        if (isVoid) vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, 0);
        vmWriter.writeReturn();
    }
    
    private void writeSymbol() throws  IOException {
        output.write("<symbol>");
        if (tokenizer.symbol() == '<') output.write("&lt;");
        else if (tokenizer.symbol() == '>') output.write("&gt;");
        else if (tokenizer.symbol() == '"') output.write("&quot;");
        else if (tokenizer.symbol() == '&') output.write("&amp;");
        else output.write(tokenizer.symbol());
        output.write("</symbol>\n");
    }

    public void compileExpression() throws IOException {
        // first term
        compileTerm();

        // 0 or more op followed by term
        while (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL &
                (tokenizer.symbol() == '+' | tokenizer.symbol() == '-' | tokenizer.symbol() == '*' |
                        tokenizer.symbol() == '/' | tokenizer.symbol() == '&' | tokenizer.symbol() == '|' |
                        tokenizer.symbol() == '<' | tokenizer.symbol() == '>' | tokenizer.symbol() == '=')) {
            char symbol = tokenizer.symbol();
            tokenizer.advance();
            compileTerm();
            switch (symbol) {
                case '*' -> {
                    vmWriter.writeCall("Math.multiply", 2);
                    return;
                }
                case '/' -> {
                    vmWriter.writeCall("Math.divide", 2);
                    return;
                }
                case '+' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.ADD);
                    return;
                }
                case '-' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.SUB);
                    return;
                }
                case '=' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.EQ);
                    return;
                }
                case '>' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.GT);
                    return;
                }
                case '<' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.LT);
                    return;
                }
                case '&' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.AND);
                    return;
                }
                case '|' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.OR);
                    return;
                }
                case '~' -> {
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NOT);
                    return;
                }
            }
        }
    }

    public void compileTerm() throws IOException {
        switch (tokenizer.tokenType()) {
            case INT_CONST ->  {
                vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, tokenizer.intVal());
                tokenizer.advance();
            }
            case STRING_CONST -> {
                // TODO: HOW?
                output.write("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>\n");
                tokenizer.advance();
            }
            case KEYWORD -> {
                // Keyword Constant true | false | null | this
                if (tokenizer.keyWord() == Tokenizer.KeyWord.TRUE) {
                    vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, 1);
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NEG);
                } else {
                    vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, 0);
                }
                tokenizer.advance();
            }
            case IDENTIFIER -> {
                // in either class/subroutine table or none if subroutine name
                VMWriter.SEGMENT segment;
                int index;
                if (classTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
                    // get kind and index
                    segment = VMWriter.KIND_TO_SEGMENT.get(classTable.kindOf(tokenizer.identifier()));
                    index = classTable.indexOf(tokenizer.identifier());
                    vmWriter.writePush(segment, index);
                    tokenizer.advance();
                } else if (subroutineTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
                    segment = VMWriter.KIND_TO_SEGMENT.get(subroutineTable.kindOf(tokenizer.identifier()));
                    index = subroutineTable.indexOf(tokenizer.identifier());
                    vmWriter.writePush(segment, index);
                    tokenizer.advance();
                }
                else {
                    // TODO: check for more complex, like x = Class then x.method()
                    // need to call the class method/function
                    // It's the name of a class/function
                    String name = tokenizer.identifier();
                    tokenizer.advance();
                    if (tokenizer.symbol() == '.') {
                        tokenizer.advance();  // .
                        name += '.' + tokenizer.stringVal();
                        tokenizer.advance(); // subroutine name

                        // (
                        tokenizer.advance();

                        compileExpressionList();

                        // )
                        tokenizer.advance();
                        vmWriter.writeCall(name, this.length);
                    }

                }
                // TODO: handle complex identifiers subroutine calls or arrays
//                handleIdentifier();
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    // (
                    tokenizer.advance();
                    compileExpression();
                    // )
                    tokenizer.advance();
                } else {
                    // unary op
                    tokenizer.advance();
                    compileTerm();
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NEG);
                }
            }
        }
    }

    public void compileExpressionList() throws IOException {
        this.length = 0;
        if (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL | tokenizer.symbol() == '(') {
            compileExpression();
            this.length ++;
        }
        while (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL & tokenizer.symbol() == ',') {
            // ,
            this.length ++;
            tokenizer.advance();
            compileExpression();
        }
    }
}
