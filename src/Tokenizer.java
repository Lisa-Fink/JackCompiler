import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Tokenizer {
    private FileReader scanner;
    private String token;
    private TokenType tokenType;
    private boolean isEnd;
    private char curChar;

    public enum TokenType {KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST}

    public enum KeyWord {CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID,
    VAR, STATIC, FIELD, LET, DO, IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS}

    private static final Set<Character> SYMBOLS = new HashSet<>();

    static {
        String symbols = "{}()[].,;+-*/&|<>=~";
        for (char symbol : symbols.toCharArray()) {
            SYMBOLS.add(symbol);
        }
    }

    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Map<String, KeyWord> KEYMAP = new HashMap<>();

    static {
        KEYWORDS.add("class");
        KEYWORDS.add("constructor");
        KEYWORDS.add("function");
        KEYWORDS.add("method");
        KEYWORDS.add("field");
        KEYWORDS.add("static");
        KEYWORDS.add("var");
        KEYWORDS.add("int");
        KEYWORDS.add("char");
        KEYWORDS.add("boolean");
        KEYWORDS.add("void");
        KEYWORDS.add("true");
        KEYWORDS.add("false");
        KEYWORDS.add("null");
        KEYWORDS.add("this");
        KEYWORDS.add("let");
        KEYWORDS.add("do");
        KEYWORDS.add("if");
        KEYWORDS.add("else");
        KEYWORDS.add("while");
        KEYWORDS.add("return");

        KEYMAP.put("class", KeyWord.CLASS);
        KEYMAP.put("constructor", KeyWord.CONSTRUCTOR);
        KEYMAP.put("function", KeyWord.FUNCTION);
        KEYMAP.put("method", KeyWord.METHOD);
        KEYMAP.put("field", KeyWord.FIELD);
        KEYMAP.put("static", KeyWord.STATIC);
        KEYMAP.put("var", KeyWord.VAR);
        KEYMAP.put("int", KeyWord.INT);
        KEYMAP.put("char", KeyWord.CHAR);
        KEYMAP.put("boolean", KeyWord.BOOLEAN);
        KEYMAP.put("void", KeyWord.VOID);
        KEYMAP.put("true", KeyWord.TRUE);
        KEYMAP.put("false", KeyWord.FALSE);
        KEYMAP.put("null", KeyWord.NULL);
        KEYMAP.put("this", KeyWord.THIS);
        KEYMAP.put("let", KeyWord.LET);
        KEYMAP.put("do", KeyWord.DO);
        KEYMAP.put("if", KeyWord.IF);
        KEYMAP.put("else", KeyWord.ELSE);
        KEYMAP.put("while", KeyWord.WHILE);
        KEYMAP.put("return", KeyWord.RETURN);
    }

    public Tokenizer(String filePath) throws IOException {
        // Opens the file
        File file = new File(filePath);
        scanner = new FileReader(file);
        isEnd = false;
        // get first char that isn't white space
        int charRead = scanner.read();

        while (charRead != -1) {
            curChar = (char) charRead;
            if (curChar != ' ') break;
            charRead = scanner.read();
        }
        if (charRead == -1) isEnd = true;
    }

    public boolean hasMoreTokens() {
        return isEnd == false;
    }

    public void advance() throws IOException {
        // Always starts at curChar
        int charRead;
        // if first character is a symbol process symbol
        if (SYMBOLS.contains(curChar)) {
            tokenType = TokenType.SYMBOL;
            token = String.valueOf(curChar);

            charRead = scanner.read(); // advance to next char for next time
            curChar = (char) charRead;
        }
        else if (curChar == '"') {
            // if first character is a " process string
            StringBuilder sb = new StringBuilder();
            charRead = scanner.read();
            curChar = (char) charRead; // skip opening "
            while (curChar != '"') {
                sb.append(curChar);
                charRead = scanner.read();
                curChar = (char) charRead;
            }

            charRead = scanner.read();
            curChar = (char) charRead; // advance to skip closing "

            token = sb.toString();
            tokenType = TokenType.STRING_CONST;
        }
        else if (Character.isDigit(curChar)) {
            // if first character is a digit process digit
            StringBuilder sb = new StringBuilder();
            sb.append(curChar);
            charRead = scanner.read();
            curChar = (char) charRead;
            while (charRead != -1) {
                if (!Character.isDigit(curChar)) break;
                sb.append(curChar);
                charRead = scanner.read();
                curChar = (char) charRead;
            }
            token = sb.toString();
            tokenType = TokenType.INT_CONST;

            // Don't advance since there can be a symbol here needed for next time
        } else {
            // it's either a keyword or an identifier
            // build string until space or symbol or end
            StringBuilder sb = new StringBuilder();
            sb.append(curChar);
            charRead = scanner.read();
            curChar = (char) charRead;
            while (charRead != -1 &&
                    curChar != ' ' && !SYMBOLS.contains(curChar)) {

                sb.append(curChar);
                charRead = scanner.read();
                curChar = (char) charRead;
            }
            token = sb.toString();
            if (token.charAt(0) != '_' && KEYWORDS.contains(token)) {
                // Token is a keyword
                tokenType = TokenType.KEYWORD;
            } else {
                tokenType = TokenType.IDENTIFIER;
            }
            // Don't advance since there can be a symbol here needed for next time
        }
        // skip trailing white space
        charRead = scanner.read();
        curChar = (char) charRead;
        while (charRead != -1 && curChar == ' ') {
            charRead = scanner.read();
            curChar = (char) charRead;
        }
        if (charRead == -1) isEnd = true;
    }

    public TokenType tokenType() {
        return tokenType;
    }

    public KeyWord keyWord() {
        return KEYMAP.get(token);
    }

    public char symbol() {
        return token.charAt(0);
    }

    public String identifier() {
        return token;
    }

    public int intVal() {
        return Integer.parseInt(token);
    }

    public String stringVal() {
        return token;
    }

}