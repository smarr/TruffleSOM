/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package trufflesom.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;

import bd.source.SourceCoordinate;


public final class Lexer {

  private static final String SEPARATOR = "----";
  private static final String PRIMITIVE = "primitive";

  public static final class Peek {
    public final Symbol nextSym;
    public final String nextText;

    private Peek(final Symbol sym, final String text) {
      nextSym = sym;
      nextText = text;
    }
  }

  private static class LexerState {
    LexerState() {
      startCoord = SourceCoordinate.createEmpty();
    }

    LexerState(final LexerState old) {
      lineNumber = old.lineNumber;
      charsRead = old.charsRead;
      lastNonWhiteCharIdx = old.lastNonWhiteCharIdx;
      buf = old.buf;
      bufPointer = old.bufPointer;
      sym = old.sym;
      symc = old.symc;
      text = new StringBuffer(old.text);
      startCoord = old.startCoord;
    }

    public void set(final Symbol sym, final char symChar, final String text) {
      this.sym = sym;
      this.symc = symChar;
      this.text = new StringBuffer(text);
    }

    public void set(final Symbol sym) {
      this.sym = sym;
      this.symc = 0;
      this.text = new StringBuffer();
    }

    private int lineNumber;
    private int charsRead;          // all characters read, excluding the current line
    private int lastNonWhiteCharIdx;

    private String buf;
    private int    bufPointer;

    private Symbol       sym;
    private char         symc;
    private StringBuffer text;

    private SourceCoordinate startCoord;

    int incPtr() {
      return incPtr(1);
    }

    int incPtr(final int val) {
      int cur = bufPointer;
      bufPointer += val;
      lastNonWhiteCharIdx = charsRead + bufPointer;
      return cur;
    }
  }

  private final BufferedReader infile;

  private boolean    peekDone;
  private LexerState state;
  private LexerState stateAfterPeek;

  private final Field nextCharField;

  protected Lexer(final Reader reader, final long fileSize) {
    /**
     * TODO: we rely on the internal implementation to get the position in the
     * file. This should be fixed and reimplemented to avoid such hacks.
     * We need to know for Truffle the character index of a token,
     * but we do not get it because line-based reading does split the
     * type of end-of-line indicator and its length.
     */
    infile = new BufferedReader(reader, (int) fileSize);
    peekDone = false;
    state = new LexerState();
    state.buf = "";
    state.text = new StringBuffer();
    state.bufPointer = 0;
    state.lineNumber = 0;

    Field f = null;
    try {
      f = infile.getClass().getDeclaredField("nextChar");
      f.setAccessible(true);
    } catch (NoSuchFieldException | SecurityException e) {
      e.printStackTrace();
    }
    nextCharField = f;
  }

  public static SourceCoordinate createSourceCoordinate(final LexerState state) {
    int charIdx = state.charsRead + state.bufPointer;
    return SourceCoordinate.create(state.lineNumber, state.bufPointer + 1, charIdx);
  }

  public SourceCoordinate getStartCoordinate() {
    return state.startCoord;
  }

  protected Symbol getSym() {
    if (peekDone) {
      peekDone = false;
      state = stateAfterPeek;
      stateAfterPeek = null;
      state.text = new StringBuffer(state.text);
      return state.sym;
    }

    do {
      if (!hasMoreInput()) {
        state.set(Symbol.NONE);
        return state.sym;
      }
      skipWhiteSpace();
      skipComment();
    } while (endOfBuffer() || Character.isWhitespace(currentChar())
        || currentChar() == '"');

    state.startCoord = createSourceCoordinate(state);

    if (currentChar() == '\'') {
      lexString();
    } else if (currentChar() == '[') {
      match(Symbol.NewBlock);
    } else if (currentChar() == ']') {
      match(Symbol.EndBlock);
    } else if (currentChar() == ':') {
      if (bufchar(state.bufPointer + 1) == '=') {
        state.incPtr(2);
        state.set(Symbol.Assign, '\0', ":=");
      } else {
        state.incPtr();
        state.set(Symbol.Colon, ':', ":");
      }
    } else if (currentChar() == '(') {
      match(Symbol.NewTerm);
    } else if (currentChar() == ')') {
      match(Symbol.EndTerm);
    } else if (currentChar() == '#') {
      match(Symbol.Pound);
    } else if (currentChar() == '^') {
      match(Symbol.Exit);
    } else if (currentChar() == '.') {
      match(Symbol.Period);
    } else if (currentChar() == '-') {
      if (state.buf.startsWith(SEPARATOR, state.bufPointer)) {
        state.text = new StringBuffer();
        while (currentChar() == '-') {
          state.text.append(bufchar(state.incPtr()));
        }
        state.sym = Symbol.Separator;
      } else {
        lexOperator();
      }
    } else if (isOperator(currentChar())) {
      lexOperator();
    } else if (nextWordInBufferIs(PRIMITIVE)) {
      state.incPtr(PRIMITIVE.length());
      state.set(Symbol.Primitive, '\0', PRIMITIVE);
    } else if (Character.isLetter(currentChar())) {
      state.set(Symbol.Identifier);
      while (isIdentifierChar(currentChar())) {
        state.text.append(bufchar(state.incPtr()));
      }
      if (bufchar(state.bufPointer) == ':') {
        state.sym = Symbol.Keyword;
        state.incPtr();
        state.text.append(':');
        if (Character.isLetter(currentChar())) {
          state.sym = Symbol.KeywordSequence;
          while (Character.isLetter(currentChar()) || currentChar() == ':') {
            state.text.append(bufchar(state.incPtr()));
          }
        }
      }
    } else if (Character.isDigit(currentChar())) {
      lexNumber();
    } else {
      state.set(Symbol.NONE, currentChar(), "" + currentChar());
    }

    return state.sym;
  }

  private void lexNumber() {
    state.set(Symbol.Integer);

    boolean sawDecimalMark = false;

    do {
      state.text.append(bufchar(state.incPtr()));

      if (!sawDecimalMark &&
          '.' == currentChar() &&
          Character.isDigit(bufchar(state.bufPointer + 1))) {
        state.sym = Symbol.Double;
        state.text.append(bufchar(state.incPtr()));
      }
    } while (Character.isDigit(currentChar()));
  }

  private void lexEscapeChar() {
    assert !endOfBuffer();

    char current = currentChar();
    switch (current) {
      case 't':
        state.text.append("\t");
        break;
      case 'b':
        state.text.append("\b");
        break;
      case 'n':
        state.text.append("\n");
        break;
      case 'r':
        state.text.append("\r");
        break;
      case 'f':
        state.text.append("\f");
        break;
      case '0':
        state.text.append("\0");
        break;
      case '\'':
        state.text.append("'");
        break;
      case '\\':
        state.text.append("\\");
        break;
    }
    state.incPtr();
  }

  private void lexStringChar() {
    if (currentChar() == '\\') {
      state.incPtr();
      lexEscapeChar();
    } else {
      state.text.append(currentChar());
      state.incPtr();
    }
  }

  private void lexString() {
    state.set(Symbol.STString);
    state.incPtr();

    while (currentChar() != '\'') {
      lexStringChar();
      while (endOfBuffer()) {
        if (fillBuffer() == -1) {
          return;
        }
        state.text.append('\n');
      }
    }

    state.incPtr();
  }

  private void lexOperator() {
    if (isOperator(bufchar(state.bufPointer + 1))) {
      state.set(Symbol.OperatorSequence);
      while (isOperator(currentChar())) {
        state.text.append(bufchar(state.incPtr()));
      }
    } else if (currentChar() == '~') {
      match(Symbol.Not);
    } else if (currentChar() == '&') {
      match(Symbol.And);
    } else if (currentChar() == '|') {
      match(Symbol.Or);
    } else if (currentChar() == '*') {
      match(Symbol.Star);
    } else if (currentChar() == '/') {
      match(Symbol.Div);
    } else if (currentChar() == '\\') {
      match(Symbol.Mod);
    } else if (currentChar() == '+') {
      match(Symbol.Plus);
    } else if (currentChar() == '=') {
      match(Symbol.Equal);
    } else if (currentChar() == '>') {
      match(Symbol.More);
    } else if (currentChar() == '<') {
      match(Symbol.Less);
    } else if (currentChar() == ',') {
      match(Symbol.Comma);
    } else if (currentChar() == '@') {
      match(Symbol.At);
    } else if (currentChar() == '%') {
      match(Symbol.Per);
    } else if (currentChar() == '-') {
      match(Symbol.Minus);
    }
  }

  protected boolean getPeekDone() {
    return peekDone;
  }

  protected Peek peek() {
    LexerState old = new LexerState(state);
    if (peekDone) {
      throw new IllegalStateException("SOM lexer: cannot peek twice!");
    }
    getSym();
    Peek peek = new Peek(state.sym, state.text.toString());
    stateAfterPeek = state;
    state = old;

    peekDone = true;
    return peek;
  }

  protected String getText() {
    return state.text.toString();
  }

  protected String getRawBuffer() {
    return state.buf;
  }

  protected int getCurrentLineNumber() {
    return state.lineNumber;
  }

  protected int getCurrentColumn() {
    return state.bufPointer + 1;
  }

  // All characters read and processed, including current line
  protected int getNumberOfCharactersRead() {
    return state.startCoord.charIndex;
  }

  private int fillBuffer() {
    try {
      if (!infile.ready()) {
        return -1;
      }

      try {
        int charsRead = nextCharField.getInt(infile);
        assert charsRead >= 0 && charsRead >= state.charsRead;
        state.charsRead = charsRead;
      } catch (IllegalArgumentException | IllegalAccessException e) {
        e.printStackTrace();
      }

      state.buf = infile.readLine();
      if (state.buf == null) {
        return -1;
      }
      ++state.lineNumber;
      state.bufPointer = 0;
      return state.buf.length();
    } catch (IOException ioe) {
      throw new IllegalStateException("Error reading from input: "
          + ioe.toString());
    }
  }

  private boolean hasMoreInput() {
    while (endOfBuffer()) {
      if (fillBuffer() == -1) {
        return false;
      }
    }
    return true;
  }

  private void skipWhiteSpace() {
    while (Character.isWhitespace(currentChar())) {
      state.incPtr();
      while (endOfBuffer()) {
        if (fillBuffer() == -1) {
          return;
        }
      }
    }
  }

  private void skipComment() {
    if (currentChar() == '"') {
      do {
        state.incPtr();
        while (endOfBuffer()) {
          if (fillBuffer() == -1) {
            return;
          }
        }
      } while (currentChar() != '"');
      state.incPtr();
    }
  }

  private char currentChar() {
    return bufchar(state.bufPointer);
  }

  private boolean endOfBuffer() {
    return state.bufPointer >= state.buf.length();
  }

  private boolean isOperator(final char c) {
    return c == '~' || c == '&' || c == '|' || c == '*' || c == '/'
        || c == '\\' || c == '+' || c == '=' || c == '>' || c == '<'
        || c == ',' || c == '@' || c == '%' || c == '-';
  }

  private void match(final Symbol s) {
    state.set(s, currentChar(), "" + currentChar());
    state.incPtr();
  }

  private char bufchar(final int p) {
    return p >= state.buf.length() ? '\0' : state.buf.charAt(p);
  }

  private boolean isIdentifierChar(final char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  private boolean nextWordInBufferIs(final String text) {
    if (!state.buf.startsWith(text, state.bufPointer)) {
      return false;
    }
    return !isIdentifierChar(bufchar(state.bufPointer + text.length()));
  }

}
