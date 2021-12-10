package trufflesom.compiler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.oracle.truffle.api.source.Source;

import bd.source.SourceCoordinate;
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

    long coord = l.getStartCoordinate();
    assertEquals(0, SourceCoordinate.getStartIndex(coord));
    assertEquals(1, SourceCoordinate.getLine(s, coord));
    assertEquals(1, SourceCoordinate.getColumn(s, coord));
  }

  @Test
  public void testFirstToken() {
    Lexer l = init("Foo = ()");
    l.getSym();

    long coord = l.getStartCoordinate();
    assertEquals(0, SourceCoordinate.getStartIndex(coord));
    assertEquals(1, SourceCoordinate.getLine(s, coord));
    assertEquals(1, SourceCoordinate.getColumn(s, coord));
  }

  @Test
  public void testSecondToken() {
    Lexer l = init("Foo = ()");
    l.getSym();
    l.getSym();

    long coord = l.getStartCoordinate();
    assertEquals(4, SourceCoordinate.getStartIndex(coord));
    assertEquals(1, SourceCoordinate.getLine(s, coord));
    assertEquals(5, SourceCoordinate.getColumn(s, coord));
  }

  @Test
  public void testFirstTokenAfterSpace() {
    Lexer l = init(" Foo = ()");
    l.getSym();

    long coord = l.getStartCoordinate();
    assertEquals(1, SourceCoordinate.getStartIndex(coord));
    assertEquals(1, SourceCoordinate.getLine(s, coord));
    assertEquals(2, SourceCoordinate.getColumn(s, coord));
  }

  @Test
  public void testSecondLineFirstToken() {
    Lexer l = init("\nFoo = ()");
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("Foo", l.getText());

    long coord = l.getStartCoordinate();
    assertEquals(1, SourceCoordinate.getStartIndex(coord));
    assertEquals(2, SourceCoordinate.getLine(s, coord));
    assertEquals(1, SourceCoordinate.getColumn(s, coord));
  }

  @Test
  public void testSecondLineSecondToken() {
    Lexer l = init("\nFoo = ()");
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Equal, sym);
    assertEquals("=", l.getText());

    long coord = l.getStartCoordinate();
    assertEquals(5, SourceCoordinate.getStartIndex(coord));
    assertEquals(2, SourceCoordinate.getLine(s, coord));
    assertEquals(5, SourceCoordinate.getColumn(s, coord));
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

    long coord = l.getStartCoordinate();
    assertEquals(2, SourceCoordinate.getLine(s, coord));
    assertEquals(3, SourceCoordinate.getColumn(s, coord));

    assertEquals(prefix.length(), SourceCoordinate.getStartIndex(coord));
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

    long coord = l.getStartCoordinate();
    assertEquals(2, SourceCoordinate.getLine(s, coord));
    assertEquals(1, SourceCoordinate.getColumn(s, coord));

    assertEquals(prefix.length(), SourceCoordinate.getStartIndex(coord));
  }
}
