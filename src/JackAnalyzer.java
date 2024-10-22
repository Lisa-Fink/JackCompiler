import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JackAnalyzer {
    private static void handleFile(String filename) throws IOException {
        Tokenizer tokenizer = new Tokenizer(filename);
        tokenizer.advance();
        FileWriter outputFile = new FileWriter(filename.substring(0, filename.indexOf(".jack")) + "test11" + ".xml", false);

        CompilationEngine compilationEngine = new CompilationEngine(tokenizer, outputFile);
        compilationEngine.compileClass();
        outputFile.close();
    }

        public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java JackAnalyzer <filename>");
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
            handleFile(filename);
        }
    }
}
