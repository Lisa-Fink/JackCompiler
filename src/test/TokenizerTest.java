import org.junit.Test;

import java.io.IOException;

public class TokenizerTest {
    @Test
    public void tokenizerTest() throws IOException {
        Tokenizer tokenizer = new Tokenizer("src/test/resources/simpleToken.txt");

        tokenizer.advance();
        System.out.println(tokenizer.tokenType());
        System.out.println(tokenizer.identifier());
    }
}
