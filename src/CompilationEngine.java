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

        output.write("</subroutineDec>\n");
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
        output.write("<subroutineBody>\n");

        writeSymbol();  // opening {
        tokenizer.advance();

        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD &
                tokenizer.keyWord() == Tokenizer.KeyWord.VAR) {
            compileVarDec();
        }

        compileStatements();

        writeSymbol();  // closing }
        tokenizer.advance();

        output.write("</subroutineBody>\n");
    }

    public void compileVarDec() throws IOException {
        output.write("<varDec>\n");
        // var
        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // var keyword
        tokenizer.advance();
        // type
        String type;
        // can be a keyword or an identifier (if it's a class type)
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            type = KEYMAP.get(tokenizer.keyWord());
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // type
            tokenizer.advance();
        } else {
            type = tokenizer.identifier();
            output.write("<classIdentifierDef> " + tokenizer.identifier() + " </classIdentifierDef>\n");  // class type
            tokenizer.advance();
        }
        // name
        String name;
        name = tokenizer.identifier();
        subroutineTable.define(name, type, SymbolTable.KIND.VAR);
        output.write("<varIdentifierDef> " + tokenizer.identifier() + SymbolTable.KIND.VAR + subroutineTable.indexOf(name) + " </varIdentifierDef>\n");  // varName
        tokenizer.advance();

        while (tokenizer.symbol() != ';') {
            writeSymbol();  // ,
            tokenizer.advance();
            name = tokenizer.identifier();
            subroutineTable.define(name, type, SymbolTable.KIND.VAR);
            output.write("<varIdentifierDef> " + tokenizer.identifier() + SymbolTable.KIND.VAR + subroutineTable.indexOf(name) + " </varIdentifierDef>\n");  // varName
            tokenizer.advance();
        }

        writeSymbol();  // ;
        tokenizer.advance();

        output.write("</varDec>\n");
    }

    public void compileStatements() throws IOException {
        output.write("<statements>\n");

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

        output.write("</statements>\n");
    }

    public void compileLet () throws IOException {
        output.write("<letStatement>\n");
        // let
        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // let keyword
        tokenizer.advance();

        // var or arg
        // either in class or subroutine table
        // check class table
        if (classTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
            output.write("<identifierUsed> " + tokenizer.identifier() + classTable.kindOf(tokenizer.identifier()) + classTable.indexOf(tokenizer.identifier()) + " </identifierUsed>\n");  // varName

        } else {
            output.write("<identifierUsed> " + tokenizer.identifier() + subroutineTable.kindOf(tokenizer.identifier()) + subroutineTable.indexOf(tokenizer.identifier()) + " </identifierUsed>\n");  // varName
        }
        tokenizer.advance();
        handleIdentifier();


        writeSymbol();  // =
        tokenizer.advance();

        compileExpression();

        writeSymbol();  // ;
        tokenizer.advance();

        output.write("</letStatement>\n");
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
        output.write("<doStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // do keyword
        tokenizer.advance();

        /// in either class/subroutine table or none if subroutine name
        if (classTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
            output.write("<classIdentifierUsed>" + tokenizer.identifier() + classTable.kindOf(tokenizer.identifier()) + classTable.indexOf(tokenizer.identifier()) + "</classIdentifierUsed>");
        } else if (subroutineTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE)
            output.write("<subroutineIdentifierUsed> " + tokenizer.identifier() + subroutineTable.kindOf(tokenizer.identifier()) + subroutineTable.indexOf(tokenizer.identifier()) + " </subroutineIdentifierUsed>\n");
        else {
            output.write("<identifierUsed> " + tokenizer.identifier() + " </identifierUsed>\n");
        }
        tokenizer.advance();
        if (tokenizer.symbol() == '.' | tokenizer.symbol() == '[') {
            handleIdentifier();
        } else {
            writeSymbol();  // (
            tokenizer.advance();

            compileExpressionList();

            writeSymbol();  // )
            tokenizer.advance();
        }

        writeSymbol();  // ;
        tokenizer.advance();


        output.write("</doStatement>\n");
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
        output.write("<returnStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // return keyword
        tokenizer.advance();

        while (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL & tokenizer.symbol() != ';') {
            compileExpression();
        }

        writeSymbol();  // ;
        tokenizer.advance();

        output.write("</returnStatement>\n");
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
        output.write("<expression>\n");
        // first term
        compileTerm();

        // 0 or more op followed by term
        while (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL &
                (tokenizer.symbol() == '+' | tokenizer.symbol() == '-' | tokenizer.symbol() == '*' |
                        tokenizer.symbol() == '/' | tokenizer.symbol() == '&' | tokenizer.symbol() == '|' |
                        tokenizer.symbol() == '<' | tokenizer.symbol() == '>' | tokenizer.symbol() == '=')) {
            writeSymbol();
            tokenizer.advance();
            compileTerm();
        }

        output.write("</expression>\n");
    }

    public void compileTerm() throws IOException {
        output.write("<term>\n");

        switch (tokenizer.tokenType()) {
            case INT_CONST ->  {
                output.write("<integerConstant> " + tokenizer.intVal() + " </integerConstant>\n");
                tokenizer.advance();
            }
            case STRING_CONST -> {
                output.write("<stringConstant> " + tokenizer.stringVal() + " </stringConstant>\n");
                tokenizer.advance();
            }
            case KEYWORD -> {
                // Keyword Constant true | false | null | this
                output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // do keyword
                tokenizer.advance();
            }
            case IDENTIFIER -> {
                // in either class/subroutine table or none if subroutine name
                if (classTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
                    output.write("<classIdentifierUsed>" + tokenizer.identifier() + classTable.kindOf(tokenizer.identifier()) + classTable.indexOf(tokenizer.identifier()) + "</classIdentifierUsed>");
                } else if (subroutineTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE)
                output.write("<subroutineIdentifierUsed> " + tokenizer.identifier() + subroutineTable.kindOf(tokenizer.identifier()) + subroutineTable.indexOf(tokenizer.identifier()) + " </subroutineIdentifierUsed>\n");
                else {
                    output.write("<identifierUsed> " + tokenizer.identifier() + " </identifierUsed>\n");
                }
                tokenizer.advance();
                handleIdentifier();
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    writeSymbol();  // (
                    tokenizer.advance();
                    compileExpression();
                    writeSymbol();  // )
                    tokenizer.advance();
                } else {
                    writeSymbol();  // unary op
                    tokenizer.advance();

                    compileTerm();
                }
            }
        }
        output.write("</term>\n");
    }

    public void compileExpressionList() throws IOException {
        output.write("<expressionList>\n");

        if (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL | tokenizer.symbol() == '(') {
            compileExpression();
        }
        while (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL & tokenizer.symbol() == ',') {
            writeSymbol();  // ,
            tokenizer.advance();

            compileExpression();
        }
        output.write("</expressionList>\n");
    }
}
