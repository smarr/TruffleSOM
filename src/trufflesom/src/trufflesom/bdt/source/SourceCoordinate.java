package trufflesom.bdt.source;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.bdt.inlining.nodes.WithSource;


/**
 * Represents a potentially empty range of source characters, for a potentially
 * not yet loaded source.
 */
public class SourceCoordinate {

  public static String toString(final long coord) {
    long startIndex = getStartIndex(coord);
    long length = getLength(coord);

    return "SrcCoord(start:" + startIndex + " length: " + length + ")";
  }

  public static int getStartIndex(final long coord) {
    return (int) (coord & 0xFFFFFFFFL);
  }

  public static int getLength(final long coord) {
    return (int) ((coord >>> 32) & 0xFFFFFFFFL);
  }

  public static long createEmpty() {
    return 0;
  }

  public static long create(final SourceSection section) {
    return create(section.getCharIndex(), section.getCharLength());
  }

  public static long create(final int startIndex, final int length) {
    return (((long) length) << 32) | (startIndex & 0xFFFFFFFFL);
  }

  public static long withZeroLength(final long coord) {
    return coord & 0xFFFFFFFFL;
  }

  public static String getLocationQualifier(final SourceSection section) {
    return ":" + section.getStartLine() + ":" + section.getStartColumn();
  }

  public static String getLocationQualifier(final int startIndex, final Source source) {
    int lineNumber = source.getLineNumber(startIndex);
    int column = source.getColumnNumber(startIndex);
    return ":" + lineNumber + ":" + column;
  }

  public static String getLocationQualifier(final Source source, final long coord) {
    int startIndex = getStartIndex(coord);
    int lineNumber = source.getLineNumber(startIndex);
    int column = source.getColumnNumber(startIndex);
    return ":" + lineNumber + ":" + column;
  }

  public static int getLine(final Source source, final long coord) {
    int startIndex = getStartIndex(coord);
    int lineNumber = source.getLineNumber(startIndex);
    return lineNumber;
  }

  public static int getColumn(final Source source, final long coord) {
    int startIndex = getStartIndex(coord);
    int column = source.getColumnNumber(startIndex);
    return column;
  }

  public static SourceSection createSourceSection(final Source source, final long coord) {
    int startIndex = getStartIndex(coord);
    int length = getLength(coord);
    return source.createSection(startIndex, length);
  }

  public static SourceSection createSourceSection(final WithSource node, final long coord) {
    Source source = node.getSource();
    if (source == null) {
      return null;
    }

    int startIndex = getStartIndex(coord);
    int length = getLength(coord);

    // HACK: make sure source sections are valid
    if (startIndex + length > source.getCharacters().length()) {
      startIndex = 0;
      length = 1;
    }

    return source.createSection(startIndex, length);
  }
}
