package bd.source;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;


/**
 * Represents a potentially empty range of source characters, for a potentially
 * not yet loaded source.
 *
 * <p>
 * The {@code charIndex} may not be set. It can be derived from startLine and startColumn,
 * if the source file is present.
 */
public class SourceCoordinate {
  public final int           startLine;
  public final int           startColumn;
  public final transient int charIndex;
  public final int           charLength;

  protected SourceCoordinate(final int startLine, final int startColumn,
      final int charIndex, final int length) {
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.charIndex = charIndex;
    this.charLength = length;
    assert startLine >= 0;
    assert startColumn >= 0;
    assert charIndex >= 0 || charIndex == -1;
  }

  @Override
  public String toString() {
    return "SrcCoord(line: " + startLine + ", col: " + startColumn + ", charlength:"
        + charLength + ")";
  }

  public static SourceCoordinate createEmpty() {
    return new SourceCoordinate(1, 1, 0, 0);
  }

  public static SourceCoordinate create(final int startLine, final int startColumn,
      final int charIndex) {
    return new SourceCoordinate(startLine, startColumn, charIndex, 0);
  }

  public static SourceCoordinate create(final int startLine, final int startColumn,
      final int charIndex, final int length) {
    return new SourceCoordinate(startLine, startColumn, charIndex, length);
  }

  public static SourceCoordinate create(final SourceSection section) {
    return new SourceCoordinate(section.getStartLine(), section.getStartColumn(),
        section.getCharIndex(), section.getCharLength());
  }

  public static String getLocationQualifier(final SourceSection section) {
    return ":" + section.getStartLine() + ":" + section.getStartColumn();
  }

  public static String getURI(final Source source) {
    return source.getURI().toString();
  }
}
