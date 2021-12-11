package trufflesom.compiler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.SomLanguage;


public class LexerTests {

  private Source s;

  private Lexer init(final String code) {
    s = SomLanguage.getSyntheticSource(code, "test");
    return new Lexer(code);
  }

  @Test
  public void testStartCoordinate() {
    Lexer l = init("Foo = ()");

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(0, startIndex);
    assertEquals(1, s.getLineNumber(startIndex));
    assertEquals(1, s.getColumnNumber(startIndex));
  }

  @Test
  public void testFirstToken() {
    Lexer l = init("Foo = ()");
    l.getSym();

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(0, startIndex);
    assertEquals(1, s.getLineNumber(startIndex));
    assertEquals(1, s.getColumnNumber(startIndex));
  }

  @Test
  public void testSecondToken() {
    Lexer l = init("Foo = ()");
    l.getSym();
    l.getSym();

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(4, startIndex);
    assertEquals(1, s.getLineNumber(startIndex));
    assertEquals(5, s.getColumnNumber(startIndex));
  }

  @Test
  public void testFirstTokenAfterSpace() {
    Lexer l = init(" Foo = ()");
    l.getSym();

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(1, startIndex);
    assertEquals(1, s.getLineNumber(startIndex));
    assertEquals(2, s.getColumnNumber(startIndex));
  }

  @Test
  public void testSecondLineFirstToken() {
    Lexer l = init("\nFoo = ()");
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("Foo", l.getText());

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(1, startIndex);
    assertEquals(2, s.getLineNumber(startIndex));
    assertEquals(1, s.getColumnNumber(startIndex));
  }

  @Test
  public void testSecondLineSecondToken() {
    Lexer l = init("\nFoo = ()");
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Equal, sym);
    assertEquals("=", l.getText());

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(5, startIndex);
    assertEquals(2, s.getLineNumber(startIndex));
    assertEquals(5, s.getColumnNumber(startIndex));
  }

  @Test
  public void testSecondLineMethodToken() {
    String prefix = "Foo = (\n" + "  ";
    Lexer l = init("Foo = (\n"
        + "  method = ( ) )");
    l.getSym();
    l.getSym();
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("method", l.getText());

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(2, s.getLineNumber(startIndex));
    assertEquals(3, s.getColumnNumber(startIndex));

    assertEquals(prefix.length(), startIndex);
  }

  @Test
  public void testSecondLineMethodNoSpacesToken() {
    String prefix = "Foo = (\n" + "";
    Lexer l = init("Foo = (\n"
        + "method = ( ) )");
    l.getSym();
    l.getSym();
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("method", l.getText());

    int startIndex = l.getNumberOfCharactersRead();
    assertEquals(2, s.getLineNumber(startIndex));
    assertEquals(1, s.getColumnNumber(startIndex));

    assertEquals(prefix.length(), startIndex);
  }
}
