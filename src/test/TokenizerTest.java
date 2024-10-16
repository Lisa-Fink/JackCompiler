import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;

public class TokenizerTest {
    @Test
    public void tokenizerTest() throws IOException {
        Tokenizer tokenizer = new Tokenizer("src/test/resources/simpleToken.txt");
        Assert.assertTrue(tokenizer.hasMoreTokens());
        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.KEYWORD, tokenizer.tokenType());
        Assert.assertEquals("token", Tokenizer.KeyWord.CLASS, tokenizer.keyWord());


        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "Main", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", '{', tokenizer.symbol());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.KEYWORD, tokenizer.tokenType());
        Assert.assertEquals("token", Tokenizer.KeyWord.DO, tokenizer.keyWord());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "func", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", '(', tokenizer.symbol());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "param1", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ',', tokenizer.symbol());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "param2", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ')', tokenizer.symbol());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ';', tokenizer.symbol());

        Assert.assertTrue(tokenizer.hasMoreTokens());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", '}', tokenizer.symbol());

        Assert.assertFalse(tokenizer.hasMoreTokens());
    }

    @Test
    public void testString() throws IOException {
        Tokenizer tokenizer = new Tokenizer("src/test/resources/tokenStringIntConst.txt");

        tokenizer.advance(); // class
        tokenizer.advance(); // Main
        tokenizer.advance(); // {

        // var initialize
        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.KEYWORD, tokenizer.tokenType());
        Assert.assertEquals("token", Tokenizer.KeyWord.STRING, tokenizer.keyWord());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "x", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ';', tokenizer.symbol());

        // string assingment
        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.KEYWORD, tokenizer.tokenType());
        Assert.assertEquals("token", Tokenizer.KeyWord.LET, tokenizer.keyWord());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "x", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", '=', tokenizer.symbol());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.STRING_CONST, tokenizer.tokenType());
        Assert.assertEquals("token", "This is a string", tokenizer.stringVal());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ';', tokenizer.symbol());


        // int init
        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.KEYWORD, tokenizer.tokenType());
        Assert.assertEquals("token", Tokenizer.KeyWord.INT, tokenizer.keyWord());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "y", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ';', tokenizer.symbol());

        // string assingment
        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.KEYWORD, tokenizer.tokenType());
        Assert.assertEquals("token", Tokenizer.KeyWord.LET, tokenizer.keyWord());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.IDENTIFIER, tokenizer.tokenType());
        Assert.assertEquals("token", "y", tokenizer.identifier());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", '=', tokenizer.symbol());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.INT_CONST, tokenizer.tokenType());
        Assert.assertEquals("token", 123, tokenizer.intVal());

        tokenizer.advance();
        Assert.assertEquals("token type", Tokenizer.TokenType.SYMBOL, tokenizer.tokenType());
        Assert.assertEquals("token", ';', tokenizer.symbol());


    }
}
