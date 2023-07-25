package trufflesom.compiler;

import trufflesom.vmobjects.SSymbol;


public class Field {
  private final int     index;
  private final SSymbol name;
  private final long    sourceCoord;

  public Field(final int idx, final SSymbol name, final long sourceCoord) {
    this.index = idx;
    this.name = name;
    this.sourceCoord = sourceCoord;
  }

  public int getIndex() {
    return index;
  }

  public SSymbol getName() {
    return name;
  }

  public long getSourceCoordinate() {
    return sourceCoord;
  }
}
