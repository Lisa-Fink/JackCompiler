import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TokenizerXMLTest {
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
    private static void handleFile(String filename) throws IOException {
        Tokenizer tokenizer = new Tokenizer(filename);
        FileWriter outputFile = new FileWriter(filename.substring(0, filename.indexOf(".jack")) + "Ttest" + ".xml", false);
        outputFile.write("<tokens>\n");

        while (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
            if (tokenizer.tokenType() == Tokenizer.TokenType.KEYWORD) {
                outputFile.write("<keyword>");
                outputFile.write(KEYMAP.get(tokenizer.keyWord()));
                outputFile.write("</keyword>\n");
            } else if (tokenizer.tokenType() == Tokenizer.TokenType.SYMBOL) {
                outputFile.write("<symbol>");
                if (tokenizer.symbol() == '<') outputFile.write("&lt;");
                else if (tokenizer.symbol() == '>') outputFile.write("&gt;");
                else if (tokenizer.symbol() == '"') outputFile.write("&quot;");
                else if (tokenizer.symbol() == '&') outputFile.write("&amp;");
                else outputFile.write(tokenizer.symbol());
                outputFile.write("</symbol>\n");
            } else if (tokenizer.tokenType() == Tokenizer.TokenType.IDENTIFIER) {
                outputFile.write("<identifier>");
                outputFile.write(tokenizer.identifier());
                outputFile.write("</identifier>\n");
            } else if (tokenizer.tokenType() == Tokenizer.TokenType.INT_CONST) {
                outputFile.write("<integerConstant>");
                outputFile.write(String.valueOf(tokenizer.intVal()));
                outputFile.write("</integerConstant>\n");
            } else if (tokenizer.tokenType() == Tokenizer.TokenType.STRING_CONST) {
                outputFile.write("<stringConstant>");
                outputFile.write(tokenizer.stringVal());
                outputFile.write("</stringConstant>\n");
            }
        }

        outputFile.write("</tokens>");
        outputFile.close();
    }
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java TokenizerXMLTest <filename>");
            return;
        }

        String filename = args[0];
        // find the last slash
        int slashIndex = filename.lastIndexOf('\\');
        if (slashIndex == -1) slashIndex = filename.lastIndexOf('/');

        int dotIndex = filename.lastIndexOf('.');
        // Arg is a directory
        if (dotIndex == -1 || dotIndex < slashIndex) {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(filename));
            for (Path file: directoryStream) {
                String curFileName = file.toString();
                if (Files.isRegularFile(file) && curFileName.endsWith(".jack")) {
                    handleFile(curFileName);
                }
            }

        } else {
            // Arg is a file

        }
    }
}
