package trufflesom.compiler;

import trufflesom.vmobjects.SSymbol;


public class Field {
  private final int     index;
  private final SSymbol name;
  private final long    sourceSection;

  public Field(final int idx, final SSymbol name, final long sourceSection) {
    this.index = idx;
    this.name = name;
    this.sourceSection = sourceSection;
  }

  public int getIndex() {
    return index;
  }

  public SSymbol getName() {
    return name;
  }

  public long getSourceSection() {
    return sourceSection;
  }
}
