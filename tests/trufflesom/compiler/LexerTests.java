package trufflesom.compiler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import bd.source.SourceCoordinate;


public class LexerTests {

  @Test
  public void testStartCoordinate() {
    Lexer l = new Lexer("Foo = ()");
    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(0, coord.charIndex);
    assertEquals(1, coord.startLine);
    assertEquals(1, coord.startColumn);
  }

  @Test
  public void testFirstToken() {
    Lexer l = new Lexer("Foo = ()");
    l.getSym();

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(0, coord.charIndex);
    assertEquals(1, coord.startLine);
    assertEquals(1, coord.startColumn);
  }

  @Test
  public void testSecondToken() {
    Lexer l = new Lexer("Foo = ()");
    l.getSym();
    l.getSym();

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(4, coord.charIndex);
    assertEquals(1, coord.startLine);
    assertEquals(5, coord.startColumn);
  }

  @Test
  public void testFirstTokenAfterSpace() {
    Lexer l = new Lexer(" Foo = ()");
    l.getSym();

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(1, coord.charIndex);
    assertEquals(1, coord.startLine);
    assertEquals(2, coord.startColumn);
  }

  @Test
  public void testSecondLineFirstToken() {
    Lexer l = new Lexer("\nFoo = ()");
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("Foo", l.getText());

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(1, coord.charIndex);
    assertEquals(2, coord.startLine);
    assertEquals(1, coord.startColumn);
  }

  @Test
  public void testSecondLineSecondToken() {
    Lexer l = new Lexer("\nFoo = ()");
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Equal, sym);
    assertEquals("=", l.getText());

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(5, coord.charIndex);
    assertEquals(2, coord.startLine);
    assertEquals(5, coord.startColumn);
  }

  @Test
  public void testSecondLineMethodToken() {
    String prefix = "Foo = (\n" + "  ";
    Lexer l = new Lexer("Foo = (\n"
        + "  method = ( ) )");
    l.getSym();
    l.getSym();
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("method", l.getText());

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(2, coord.startLine);
    assertEquals(3, coord.startColumn);

    assertEquals(prefix.length(), coord.charIndex);
  }

  @Test
  public void testSecondLineMethodNoSpacesToken() {
    String prefix = "Foo = (\n" + "";
    Lexer l = new Lexer("Foo = (\n"
        + "method = ( ) )");
    l.getSym();
    l.getSym();
    l.getSym();
    Symbol sym = l.getSym();

    assertEquals(Symbol.Identifier, sym);
    assertEquals("method", l.getText());

    SourceCoordinate coord = l.getStartCoordinate();
    assertEquals(2, coord.startLine);
    assertEquals(1, coord.startColumn);

    assertEquals(prefix.length(), coord.charIndex);
  }
}
