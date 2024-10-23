import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VMWriter {
    public enum SEGMENT {CONSTANT, ARGUMENT, LOCAL, STATIC, THIS, THAT, POINTER, TEMP}
    public static final Map<SEGMENT, String> SEGMENT_MAP = new HashMap<>();
    static {
        SEGMENT_MAP.put(SEGMENT.CONSTANT, "constant");
        SEGMENT_MAP.put(SEGMENT.ARGUMENT, "argument");
        SEGMENT_MAP.put(SEGMENT.LOCAL, "local");
        SEGMENT_MAP.put(SEGMENT.STATIC, "static");
        SEGMENT_MAP.put(SEGMENT.THIS, "this");
        SEGMENT_MAP.put(SEGMENT.THAT, "that");
        SEGMENT_MAP.put(SEGMENT.POINTER, "pointer");
        SEGMENT_MAP.put(SEGMENT.TEMP, "temp");
    }

    public enum ARITHMETIC_COMMAND {ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT};

    public static final Map<ARITHMETIC_COMMAND, String> ARITHMETIC_COMMAND_MAP = new HashMap<>();
    static {
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.ADD, "add");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.SUB, "sub");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.NEG, "new");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.EQ, "eq");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.GT, "gt");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.LT, "lt");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.AND, "and");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.OR, "or");
        ARITHMETIC_COMMAND_MAP.put(ARITHMETIC_COMMAND.NOT, "not");
    }
    private final FileWriter output;

    public VMWriter(FileWriter output) {
        this.output = output;
    }

    void writePush(SEGMENT segment, int index) throws IOException {
        output.write("\tpush " + SEGMENT_MAP.get(segment) + " " + index + "\n");
    }

    void writePop(SEGMENT segment, int index) throws IOException {
        output.write("\tpop " + SEGMENT_MAP.get(segment) + " " + index + "\n");
    }

    void writeArithmetic(ARITHMETIC_COMMAND command) throws IOException {
        output.write("\t" + ARITHMETIC_COMMAND_MAP.get(command) + "\n");
    }

    void writeLabel(String label) throws IOException {
        output.write("label " + label + "\n");
    }

    void writeGoto(String label) throws IOException {
        output.write("\tgoto " + label + "\n");
    }
    void writeIf(String label) throws IOException {
        output.write("\tif-goto " + label + "\n");
    }
    void writeCall(String label, int nArgs) throws IOException {
        output.write("\tcall " + label + " " + nArgs + "\n");
    }
    void writeFunction(String label, int nArgs) throws IOException {
        output.write("function " + label + " " + nArgs + "\n");
    }
    void writeReturn() throws IOException {
        output.write("\treturn\n");
    }

    void close() throws IOException {
        this.output.close();
    }
}
