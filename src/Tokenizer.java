import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Tokenizer {
    private final FileReader scanner;
    private String token;
    private TokenType tokenType;
    private boolean isEnd;
    private boolean foundSlash;
    private char curChar;

    public enum TokenType {KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST}

    public enum KeyWord {CLASS, METHOD, FUNCTION, CONSTRUCTOR, INT, BOOLEAN, CHAR, VOID,
    VAR, STATIC, FIELD, LET, DO, IF, ELSE, WHILE, RETURN, TRUE, FALSE, NULL, THIS, STRING}

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
        // get first char that isn't white space or comment
        int charRead = scanner.read();
        curChar = (char) charRead;
        while (curChar == '/') {
            advanceComment();
        }
        // skip leading white space and new lines
        while (charRead != -1 && (curChar == ' ' | curChar == '\n' | curChar == '\t' | curChar == '\r')) {
            charRead = scanner.read();
            curChar = (char) charRead;
            while (curChar == '/') {
                advanceComment();
            }
        }
        if (charRead == -1 & !foundSlash) isEnd = true;
    }

    public boolean hasMoreTokens() {
        return !isEnd;
    }

    public void advance() throws IOException {
        // Always starts at curChar
        int charRead;
        // When removing comments at end of last processing, a slash could have been found instead
        // check this first. If a slash was found curChar would already be at next
        if (foundSlash) {
            token = "/";
            tokenType = TokenType.SYMBOL;
            foundSlash = false;
            if (curChar == (char) -1) isEnd = true;
            return;
        }
        // if first character is a symbol process symbol
        else if (SYMBOLS.contains(curChar)) {
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
        // skip trailing white space and new lines
        while (curChar == '/') {
            advanceComment();
        }
        while (charRead != -1 & (curChar == ' ' | curChar == '\n' | curChar == '\t' | curChar == '\r')) {
            charRead = scanner.read();
            curChar = (char) charRead;
            while (curChar == '/') {
                advanceComment();
            }
        }
        if (charRead == -1 & !foundSlash) isEnd = true;

    }

    private void advanceComment() throws IOException {
        int charRead = scanner.read();
        curChar = (char) charRead;
        if (curChar == '*') {
            charRead = scanner.read();
            curChar = (char) charRead;
            char prev;
            // advance until */
            while (charRead != -1) {
                prev = curChar;
                charRead = scanner.read();
                curChar = (char) charRead;
                if (prev == '*' & curChar == '/') {
                    charRead = scanner.read();
                    curChar = (char) charRead;
                    break;
                }
            }
        } else if (curChar == '/') {
            charRead = scanner.read();
            curChar = (char) charRead;
            // advance until next line
            while (charRead != -1 & curChar != '\n') {
                charRead = scanner.read();
                curChar = (char) charRead;
            }
            charRead = scanner.read();
            curChar = (char) charRead;
            if (curChar == '\r') {
                charRead = scanner.read();
                curChar = (char) charRead;
            }
        } else {
            foundSlash = true;
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
