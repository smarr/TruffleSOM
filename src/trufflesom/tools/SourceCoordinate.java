package trufflesom.tools;

public class SourceCoordinate {
  public final int startLine;
  public final int startColumn;

  public final transient int charIndex;

  public SourceCoordinate(final int startLine, final int startColumn, final int charIndex) {
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.charIndex = charIndex;
    assert startLine >= 0;
    assert startColumn >= 0;
    assert charIndex >= 0;
  }

  @Override
  public String toString() {
    return "SrcCoord(line: " + startLine + ", col: " + startColumn + ")";
  }
}
