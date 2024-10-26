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
    int label;
    String functionName;

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
        // field or static
        SymbolTable.KIND kind;
        if (Objects.equals(KEYMAP.get(tokenizer.keyWord()), "field")) {
            kind = SymbolTable.KIND.FIELD;
        } else {
            kind = SymbolTable.KIND.STATIC;
        }
        tokenizer.advance();  // field or static

        // type
        String type;
        // can be a keyword or an identifier (if it's a class type)
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
            type = KEYMAP.get(tokenizer.keyWord());
            tokenizer.advance();
        } else {
            // class type
            type = tokenizer.identifier();
            tokenizer.advance();
        }
        char last;
        String name;
        do {
            name = tokenizer.identifier();
            classTable.define(name, type, kind);
            tokenizer.advance();


            // ending ; or ,
            last = tokenizer.symbol();
            tokenizer.advance();
        } while (last == ',');
    }

    public void compileSubroutine() throws IOException {
        String subroutineName;
        subroutineTable.reset();

        // constructor/method/function
        // Add this to symbol table if method, don't do this for function or constructor
        if (tokenizer.keyWord() == Tokenizer.KeyWord.METHOD)  {
            subroutineTable.define("this", this.className, SymbolTable.KIND.ARG);
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
            tokenizer.advance();

            // method/function name
            subroutineName = tokenizer.identifier();
            tokenizer.advance();
        }

        // opening (
        tokenizer.advance();

        compileParameterList();

        // save this to call after getting num vars for local
        this.functionName = className + "." + subroutineName;

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
        int curLength = 0;
        while (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD &
                tokenizer.keyWord() == Tokenizer.KeyWord.VAR) {
            compileVarDec();
            curLength += this.length;
        }
        vmWriter.writeFunction(this.functionName, curLength);

        // handle constructor or method
        // if method, this is in symbol table
        if (subroutineTable.kindOf("this") != SymbolTable.KIND.NONE) {
            // need to set THIS, which was passed as the first arg
            vmWriter.writePush(VMWriter.SEGMENT.ARGUMENT, 0);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER, 0);
        } else if (this.functionName.endsWith(".new")) {
            // constructor
            // will need to allocate memory for this object
            // need a word for each field
            // and set THIS to the new base address
            int numFields = classTable.varCount(SymbolTable.KIND.FIELD);
            vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, numFields);
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER, 0);
        }

        compileStatements();

        // closing }
        tokenizer.advance();
    }

    public void compileVarDec() throws IOException {
        this.length = 0;
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
        this.length++;
        subroutineTable.define(name, type, SymbolTable.KIND.VAR);
        tokenizer.advance();

        while (tokenizer.symbol() != ';') {
            // ,
            tokenizer.advance();
            name = tokenizer.identifier();
            this.length++;
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
        String type;
        String varName = tokenizer.identifier();
        if (classTable.kindOf(tokenizer.identifier()) != SymbolTable.KIND.NONE) {
            assigneeSegment = VMWriter.KIND_TO_SEGMENT.get(classTable.kindOf(varName));
            index = classTable.indexOf(varName);
            type = classTable.typeOf(varName);
        } else {
            assigneeSegment = VMWriter.KIND_TO_SEGMENT.get(subroutineTable.kindOf(varName));
            index = subroutineTable.indexOf(varName);
            type = subroutineTable.typeOf(varName);
        }
        tokenizer.advance(); // varName
        // Check if it's an Array and it is indexing. Skips this if just reassigning the variable to a different array
        // arr = Array.new()
        if (Objects.equals(type, "Array") & tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL &
                tokenizer.symbol() == '[') {

            // push address of a[i] to stack
            // don't use handleIdentifier, because it will push the value at a[i] not the address
            vmWriter.writePush(assigneeSegment, index);     // base addr a
            // now we need to i
            tokenizer.advance();    // [
            compileExpression();
            vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.ADD);  // top of stack has addr arr[i]
            tokenizer.advance();    // ]

            tokenizer.advance();    // =

            compileExpression();    // handles right side

            // the top of the stack has the right side arr[i] = rightside
            // we need the next element on the stack which is the address for arr[i]
            // store right side in temp
            vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);    // temp[0] = right side, top of stack is addr arr[i]
            vmWriter.writePop(VMWriter.SEGMENT.POINTER, 1); // THAT points to addr arr[i]
            vmWriter.writePush(VMWriter.SEGMENT.TEMP, 0);   // place right side back to top of stack
            vmWriter.writePop(VMWriter.SEGMENT.THAT, 0);    // arr[i] = right side

            tokenizer.advance();    // ;
            return;
        }
        // Only gets here if not assigning a value to an array index
        tokenizer.advance();    // =
        compileExpression();
        vmWriter.writePop(assigneeSegment, index);
        tokenizer.advance();    // ;
    }

    public void compileIf () throws IOException {
        String l1 = "L" + (this.label++);
        String l2 = "L" + (this.label++);

        tokenizer.advance();

        // (
        tokenizer.advance();

        compileExpression();
        vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NOT);
        vmWriter.writeIf(l1);
        // )
        tokenizer.advance();

        // {
        tokenizer.advance();

        compileStatements();

        // }
        tokenizer.advance();
        vmWriter.writeGoto(l2);
        vmWriter.writeLabel(l1);
        // 0 or 1 else statements
        if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD & tokenizer.keyWord() == Tokenizer.KeyWord.ELSE) {
            tokenizer.advance();

            // {
            tokenizer.advance();

            compileStatements();

            // }
            tokenizer.advance();
        }
        vmWriter.writeLabel(l2);
    }

    public void compileWhile () throws IOException {
        String l1 = "L" + this.label++;
        String l2 = "L" + this.label++;

        tokenizer.advance();    // while

        // (
        tokenizer.advance();
        vmWriter.writeLabel(l1);
        compileExpression();
        vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NOT);
        vmWriter.writeIf(l2);

        // )
        tokenizer.advance();

        // {
        tokenizer.advance();

        compileStatements();
        vmWriter.writeGoto(l1);
        // }
        tokenizer.advance();
        vmWriter.writeLabel(l2);
    }

    public void compileDo() throws IOException {
        tokenizer.advance();  // do
        compileExpression();
        tokenizer.advance();  // ;
        vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);
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
        Tokenizer.TokenType tokenType = tokenizer.tokenType();
        switch (tokenType) {
            case INT_CONST ->  {
                vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, tokenizer.intVal());
                tokenizer.advance();
            }
            case STRING_CONST -> {
                String stringConst = tokenizer.stringVal();
                int length = stringConst.length();

                // create a new String
                vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, length);
                vmWriter.writeCall("String.new", 1);
                vmWriter.writePop(VMWriter.SEGMENT.TEMP, 1);  // store addr in temp 1

                for (int i = 0; i < length; i++) {
                    vmWriter.writePush(VMWriter.SEGMENT.TEMP, 1);  // String addr
                    vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, stringConst.charAt(i));
                    vmWriter.writeCall("String.appendChar", 2);
                    vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);    // disregard void return
                }
                vmWriter.writePush(VMWriter.SEGMENT.TEMP, 1); // leave str addr on stack
                tokenizer.advance();
            }
            case KEYWORD -> {
                // Keyword Constant true | false | null | this
                if (tokenizer.keyWord() == Tokenizer.KeyWord.TRUE) {
                    vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, 1);
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NEG);
                } else if (tokenizer.keyWord() == Tokenizer.KeyWord.THIS) {
                    vmWriter.writePush(VMWriter.SEGMENT.POINTER, 0);
                } else {
                    vmWriter.writePush(VMWriter.SEGMENT.CONSTANT, 0);
                }
                tokenizer.advance();
            }
            case IDENTIFIER -> {
                // either a normal variable, a variable.method() call, a method() call
                // from in the class instance, a Classname.function() call

                // get current identifier
                // this can be the variable name, method() name or Classname
                String name = tokenizer.identifier();

                // check for '.'
                tokenizer.advance();
                if (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL &
                        tokenizer.symbol() == '.') {
                    // If there is a dot, it is either variable.method() or
                    // Classname.function()
                    String first = String.valueOf(name.charAt(0));
                    if (first.equals(first.toUpperCase())) {
                        // It has a capital letter, so it's a Classname.function()
                        tokenizer.advance(); // .
                        String functionName = tokenizer.identifier();
                        tokenizer.advance(); // funcName
                        tokenizer.advance(); // (
                        compileExpressionList();
                        tokenizer.advance(); // )
                        vmWriter.writeCall(name + "." + functionName, this.length);
                    } else {
                        // It's variable.method()
                        // need to replace variable with the Classname, and push address for this
                        String curClassName;
                        SymbolTable.KIND kind;
                        int index;
                        if (classTable.kindOf(name) != SymbolTable.KIND.NONE) {
                            // It's a class variable
                            kind = classTable.kindOf(name);
                            curClassName = classTable.typeOf(name);
                            index = classTable.indexOf(name);
                        } else {
                            // subroutine variable
                            kind = subroutineTable.kindOf(name);
                            curClassName = subroutineTable.typeOf(name);
                            index = subroutineTable.indexOf(name);
                        }
                        tokenizer.advance(); // .
                        String methodName = tokenizer.identifier();
                        tokenizer.advance(); // methodName
                        tokenizer.advance(); // (

                        // before pushing args and calling method, which changes pointer,
                        // save current pointer to restore later
                        // save current pointer before calling a method
                        vmWriter.writePush(VMWriter.SEGMENT.POINTER, 0);

                        // before getting args, push this, so it becomes arg 0 for the method
                        vmWriter.writePush(VMWriter.KIND_TO_SEGMENT.get(kind), index);
                        compileExpressionList();
                        tokenizer.advance(); // )

                        vmWriter.writeCall(curClassName + "." + methodName, this.length + 1);
                        // return val is first on stack, but need to restore pointer
                        vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);
                        vmWriter.writePop(VMWriter.SEGMENT.POINTER, 0);  // restore pointer
                        vmWriter.writePush(VMWriter.SEGMENT.TEMP, 0);
                    }

                } else {
                    // There is no dot, so it is either normal variable,
                    // subroutine method() from same class instance, or array index access arr[i]

                    // We know there's no dot, but there could be a ( if it's a method
                    if (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL &
                            tokenizer.symbol() == '(') {
                        // It's a method call from this same instance
                        // need to get Classname and push this
                        tokenizer.advance(); // (

                        // before getting args, push this, so it becomes arg 0 for the method
                        vmWriter.writePush(VMWriter.SEGMENT.POINTER, 0);   // this is arg 0
                        compileExpressionList();
                        tokenizer.advance(); // )

                        vmWriter.writeCall(className + "." + name, this.length + 1);

                    } else if (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL &
                            tokenizer.symbol() == '[') {
                        // It's accessing an array index
                        // varName is either in the classTable or subroutineTable
                        // push it onto the stack
                        SymbolTable.KIND kind;
                        int index;
                        if (classTable.kindOf(name) != SymbolTable.KIND.NONE) {
                            // It's a class variable
                            kind = classTable.kindOf(name);
                            index = classTable.indexOf(name);
                        } else {
                            // subroutine variable
                            kind = subroutineTable.kindOf(name);
                            index = subroutineTable.indexOf(name);
                        }
                        vmWriter.writePush(VMWriter.KIND_TO_SEGMENT.get(kind), index);

                        // process the express between the brackets
                        tokenizer.advance(); // [
                        compileExpression();
                        tokenizer.advance(); // ]

                        // add, so the top of the stack has the exact address calculated for the index
                        vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.ADD);

                        // get value at the address using THAT/pointer 1
                        vmWriter.writePop(VMWriter.SEGMENT.POINTER, 1);
                        vmWriter.writePush(VMWriter.SEGMENT.THAT, 0);
                        // top of the stack now has the value at the array index
                    }
                    else {
                        // This is just a variable
                        // push it
                        SymbolTable.KIND kind;
                        int index;
                        if (classTable.kindOf(name) != SymbolTable.KIND.NONE) {
                            // It's a class variable
                            kind = classTable.kindOf(name);
                            index = classTable.indexOf(name);
                        } else {
                            // subroutine variable
                            kind = subroutineTable.kindOf(name);
                            index = subroutineTable.indexOf(name);
                        }
                        vmWriter.writePush(VMWriter.KIND_TO_SEGMENT.get(kind), index);
                    }
                }
            }
            case SYMBOL -> {
                if (tokenizer.symbol() == '(') {
                    // (
                    tokenizer.advance();
                    compileExpression();
                    // )
                    tokenizer.advance();
                } else if (tokenizer.symbol() == '-') {
                    // unary op
                    tokenizer.advance();
                    compileTerm();
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NEG);
                } else {
                    tokenizer.advance();
                    compileTerm();
                    vmWriter.writeArithmetic(VMWriter.ARITHMETIC_COMMAND.NOT);
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
