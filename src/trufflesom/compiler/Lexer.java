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
      lastLineEnd = old.lastLineEnd;
      lastNonWhiteCharIdx = old.lastNonWhiteCharIdx;
      ptr = old.ptr;
      sym = old.sym;
      symc = old.symc;
      text = new StringBuilder(old.text);
      startCoord = old.startCoord;
    }

    public void set(final Symbol sym, final char symChar, final String text) {
      this.sym = sym;
      this.symc = symChar;
      this.text = new StringBuilder(text);
    }

    public void set(final Symbol sym) {
      this.sym = sym;
      this.symc = 0;
      this.text = new StringBuilder();
    }

    private int lineNumber;

    /** All characters read, excluding the current line, incl. line break. */
    private int lastLineEnd;

    private int lastNonWhiteCharIdx;

    private int ptr;

    private Symbol        sym;
    private char          symc;
    private StringBuilder text;

    private SourceCoordinate startCoord;

    int incPtr() {
      return incPtr(1);
    }

    int incPtr(final int val) {
      int cur = ptr;
      ptr += val;
      lastNonWhiteCharIdx = ptr;
      return cur;
    }
  }

  private final String content;

  private boolean    peekDone;
  private LexerState state;
  private LexerState stateAfterPeek;

  protected Lexer(final String content) {
    this.content = content;
    peekDone = false;
    state = new LexerState();
    state.text = new StringBuilder();
    state.ptr = 0;
    state.lineNumber = 1;
    state.lastLineEnd = -1;
    state.lastNonWhiteCharIdx = 0;
  }

  public static SourceCoordinate createSourceCoordinate(final LexerState state) {
    // We use the coord.length to indicate the lastNonWhiteCharIdx
    // TODO: fix this terrible hack, and make this explicit
    return SourceCoordinate.create(state.lineNumber, state.ptr - state.lastLineEnd,
        state.ptr, state.lastNonWhiteCharIdx);
  }

  public SourceCoordinate getStartCoordinate() {
    return state.startCoord;
  }

  protected Symbol getSym() {
    try {
      return doSym();
    } catch (StringIndexOutOfBoundsException e) {
      state.set(Symbol.NONE);
      return state.sym;
    }
  }

  public String getCurrentLine() {
    int endLine = content.indexOf("\n", state.lastLineEnd + 1);
    if (endLine == -1) {
      endLine = content.length() - 1;
    }
    return content.substring(state.lastLineEnd + 1, endLine);
  }

  private Symbol doSym() {
    if (peekDone) {
      peekDone = false;
      state = stateAfterPeek;
      stateAfterPeek = null;
      state.text = new StringBuilder(state.text);
      return state.sym;
    }

    do {
      if (endOfContent()) {
        state.set(Symbol.NONE);
        return state.sym;
      }
      skipWhiteSpace();
      skipComment();
    } while (endOfContent() || Character.isWhitespace(currentChar())
        || currentChar() == '"');

    state.startCoord = createSourceCoordinate(state);

    if (currentChar() == '\'') {
      lexString();
    } else if (currentChar() == '[') {
      match(Symbol.NewBlock);
    } else if (currentChar() == ']') {
      match(Symbol.EndBlock);
    } else if (currentChar() == ':') {
      if (nextChar() == '=') {
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
      if (nextWordInBufferIs(SEPARATOR)) {
        state.text = new StringBuilder();
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
      if (currentChar() == ':') {
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
          Character.isDigit(nextChar())) {
        state.sym = Symbol.Double;
        state.text.append(bufchar(state.incPtr()));
      }
    } while (Character.isDigit(currentChar()));
  }

  private void lexEscapeChar() {
    assert !endOfContent();

    char current = currentChar();
    switch (current) {
      // @formatter:off
      case 't': state.text.append("\t"); break;
      case 'b': state.text.append("\b"); break;
      case 'n': state.text.append("\n"); break;
      case 'r': state.text.append("\r"); break;
      case 'f': state.text.append("\f"); break;
      case '0': state.text.append("\0"); break;
      case '\'': state.text.append("'"); break;
      case '\\': state.text.append("\\"); break;
     // @formatter:on
    }
    state.incPtr();
  }

  private void lexStringChar() {
    char cur = currentChar();
    if (cur == '\'' && nextChar() == '\'') {
      state.text.append('\'');
      state.incPtr(2);
    } else if (cur == '\\') {
      state.incPtr();
      lexEscapeChar();
    } else {
      state.text.append(cur);
      state.incPtr();
    }

    if (cur == '\n') {
      state.lineNumber += 1;
      state.lastLineEnd = state.ptr - 1;
    }
  }

  private void lexString() {
    state.set(Symbol.STString);
    state.incPtr();

    while (currentChar() != '\'' || nextChar() == '\'') {
      lexStringChar();
    }

    state.incPtr();
  }

  private void lexOperator() {
    if (isOperator(nextChar())) {
      state.set(Symbol.OperatorSequence);
      while (isOperator(currentChar())) {
        state.text.append(bufchar(state.incPtr()));
      }
    }

    if (currentChar() == '~') {
      match(Symbol.Not);
      return;
    }

    if (currentChar() == '&') {
      match(Symbol.And);
      return;
    }

    if (currentChar() == '|') {
      match(Symbol.Or);
      return;
    }

    if (currentChar() == '*') {
      match(Symbol.Star);
      return;
    }

    if (currentChar() == '/') {
      match(Symbol.Div);
      return;
    }

    if (currentChar() == '\\') {
      match(Symbol.Mod);
      return;
    }

    if (currentChar() == '+') {
      match(Symbol.Plus);
      return;
    }

    if (currentChar() == '=') {
      match(Symbol.Equal);
      return;
    }

    if (currentChar() == '>') {
      match(Symbol.More);
      return;
    }

    if (currentChar() == '<') {
      match(Symbol.Less);
      return;
    }

    if (currentChar() == ',') {
      match(Symbol.Comma);
      return;
    }

    if (currentChar() == '@') {
      match(Symbol.At);
      return;
    }

    if (currentChar() == '%') {
      match(Symbol.Per);
      return;
    }

    if (currentChar() == '-') {
      match(Symbol.Minus);
      return;
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

  protected int getCurrentLineNumber() {
    return state.lineNumber;
  }

  protected int getCurrentColumn() {
    return state.ptr + 1 - state.lastLineEnd;
  }

  protected int getNumberOfNonWhiteCharsRead() {
    return state.startCoord.charLength;
  }

  // All characters read and processed, including current line
  protected int getNumberOfCharactersRead() {
    return state.startCoord.charIndex;
  }

  private void skipWhiteSpace() {
    char curr;
    while (!endOfContent() && Character.isWhitespace(curr = currentChar())) {
      if (curr == '\n') {
        state.lineNumber += 1;
        state.lastLineEnd = state.ptr;
      }
      state.ptr++;
    }
  }

  private void skipComment() {
    if (currentChar() == '"') {
      do {
        if (currentChar() == '\n') {
          state.lineNumber += 1;
          state.lastLineEnd = state.ptr;
        }
        state.incPtr();
      } while (currentChar() != '"');
      state.incPtr();
    }
  }

  private char currentChar() {
    return bufchar(state.ptr);
  }

  protected char nextChar() {
    return bufchar(state.ptr + 1);
  }

  protected char nextChar(final int offset) {
    return bufchar(state.ptr + offset);
  }

  private boolean endOfContent() {
    return state.ptr >= content.length();
  }

  private static boolean isOperator(final char c) {
    return c == '~' || c == '&' || c == '|' || c == '*' || c == '/'
        || c == '\\' || c == '+' || c == '=' || c == '>' || c == '<'
        || c == ',' || c == '@' || c == '%' || c == '-';
  }

  private void match(final Symbol s) {
    state.set(s, currentChar(), "" + currentChar());
    state.incPtr();
  }

  private char bufchar(final int p) {
    return p >= content.length() ? '\0' : content.charAt(p);
  }

  private boolean isIdentifierChar(final char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  private boolean nextWordInBufferIs(final String text) {
    if (!content.startsWith(text, state.ptr)) {
      return false;
    }
    return !isIdentifierChar(nextChar(text.length()));
  }

}
