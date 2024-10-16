import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CompilationEngine {
    FileWriter output;
    Tokenizer tokenizer;

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

    public CompilationEngine(Tokenizer tokenizer, FileWriter output) throws IOException {
        this.output = output;
        this.tokenizer = tokenizer;
    }

    public void compileClass() throws IOException {
        output.write("<class>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");
        tokenizer.advance();

        output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");
        tokenizer.advance();

        output.write("<symbol> ");  // opening  {
        output.write(tokenizer.symbol());
        output.write(" </symbol>\n");
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

        output.write("<symbol> ");  // closing }
        output.write(tokenizer.symbol());
        output.write(" </symbol>\n");
        tokenizer.advance();

        output.write("</class>\n");
    }

    public void compileClassVarDec() throws IOException {
        output.write("<classVarDec>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // field/static
        tokenizer.advance();

        // can be a keyword or an identifier (if it's a class type)
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // type
            tokenizer.advance();
        } else {
            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // class type
            tokenizer.advance();
        }
        char last;
        do {
            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");
            tokenizer.advance();


            output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ending ; or ,
            last = tokenizer.symbol();
            tokenizer.advance();
        } while (last == ',');

        output.write("</classVarDec>\n");
    }

    public void compileSubroutine() throws IOException {
        output.write("<subroutineDec>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // function/method
        tokenizer.advance();

        if (tokenizer.tokenType() == Tokenizer.TokenType.IDENTIFIER) {
            // if constructor
            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // name
            tokenizer.advance();

            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n"); // new
            tokenizer.advance();
        } else {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // return type
            tokenizer.advance();

            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n"); // name
            tokenizer.advance();
        }

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // opening (
        tokenizer.advance();

        compileParameterList();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // closing )
        tokenizer.advance();

        compileSubroutineBody();

        output.write("</subroutineDec>\n");
    }

    public void compileParameterList() throws IOException {
        output.write("<parameterList>\n");

        // process 0 or more parameters
        while (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL & tokenizer.symbol() != ')') {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // type
            tokenizer.advance();

            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");
            tokenizer.advance();

            // only if there is a ','
            if (tokenizer.symbol() != ')') {
                output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ending ; or ,
                tokenizer.advance();
            }
        }

        output.write("</parameterList>\n");
    }

    public void compileSubroutineBody() throws IOException {
        output.write("<subroutineBody>\n");

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // opening {
        tokenizer.advance();

        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD &
                tokenizer.keyWord() == Tokenizer.KeyWord.VAR) {
            compileVarDec();
        }

        compileStatements();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // closing }
        tokenizer.advance();

        output.write("</subroutineBody>\n");
    }

    public void compileVarDec() throws IOException {
        output.write("<varDec>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // var keyword
        tokenizer.advance();

        // can be a keyword or an identifier (if it's a class type)
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // type
            tokenizer.advance();
        } else {
            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // class type
            tokenizer.advance();
        }

        output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // varName
        tokenizer.advance();

        while (tokenizer.symbol() != ';') {
            output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ,
            tokenizer.advance();

            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // type
            tokenizer.advance();

            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // varName
            tokenizer.advance();
        }

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ;
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

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // let keyword
        tokenizer.advance();

        output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // varName
        tokenizer.advance();

        // TODO: there can be an array here

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // =
        tokenizer.advance();

        compileExpression();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ;
        tokenizer.advance();

        output.write("</letStatement>\n");
    }

    public void compileIf () throws IOException {
        output.write("<ifStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // if keyword
        tokenizer.advance();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // (
        tokenizer.advance();

        compileExpression();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // )
        tokenizer.advance();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // {
        tokenizer.advance();

        compileStatements();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // }
        tokenizer.advance();

        // 0 or more else statements
        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD & tokenizer.keyWord() == Tokenizer.KeyWord.ELSE) {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // else keyword
            tokenizer.advance();

            output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // {
            tokenizer.advance();

            compileStatements();

            output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // }
            tokenizer.advance();
        }

        output.write("</ifStatement>\n");
    }

    public void compileWhile () throws IOException {
        output.write("<whileStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // while keyword
        tokenizer.advance();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // (
        tokenizer.advance();

        compileExpression();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // )
        tokenizer.advance();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // {
        tokenizer.advance();

        compileStatements();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // }
        tokenizer.advance();

        output.write("</whileStatement>\n");
    }

    public void compileDo() throws IOException {
        output.write("<doStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // do keyword
        tokenizer.advance();

        output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // subroutineName
        tokenizer.advance();

        // if next char is a . or [
        while (tokenizer.symbol() == '.' | tokenizer.symbol() == '[') {
            if (tokenizer.symbol() == '.') {
                output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // .
                tokenizer.advance();

                output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");  // class method
                tokenizer.advance();
            } else {
                output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // [
                tokenizer.advance();

                compileExpression();

                output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ]
                tokenizer.advance();
            }
        }

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // (
        tokenizer.advance();

        compileExpressionList();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // )
        tokenizer.advance();

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ;
        tokenizer.advance();


        output.write("</doStatement>\n");
    }

    public void compileReturn() throws IOException {
        output.write("<returnStatement>\n");

        output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // return keyword
        tokenizer.advance();

        while (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL & tokenizer.symbol() != ';') {
            compileExpression();
        }

        output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ;
        tokenizer.advance();

        output.write("</returnStatement>\n");
    }

    public void compileExpression() throws IOException {
        output.write("<expression>\n");
        compileTerm();
        output.write("</expression>\n");
    }

    public void compileTerm() throws IOException {
        output.write("<term>\n");
        if (tokenizer.tokenType() == Tokenizer.TokenType.IDENTIFIER) {
            // TODO: Need to also look ahead for array or subroutine call
            output.write("<identifier> " + tokenizer.identifier() + " </identifier>\n");
            tokenizer.advance();
        } else if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            output.write("<keyword> " + KEYMAP.get(tokenizer.keyWord()) + " </keyword>\n");  // do keyword
            tokenizer.advance();
        }
        output.write("</term>\n");
    }

    public void compileExpressionList() throws IOException {
        output.write("<expressionList>\n");
        if (tokenizer.tokenType() != Tokenizer.TokenType.SYMBOL) {
            compileExpression();
        }
        while (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL & tokenizer.symbol() == ',') {
            output.write("<symbol> " + tokenizer.symbol() + " </symbol>\n");  // ,
            tokenizer.advance();

            compileExpression();
        }
        output.write("</expressionList>\n");
    }
}
