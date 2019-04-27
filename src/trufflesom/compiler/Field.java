package trufflesom.compiler;

import com.oracle.truffle.api.source.SourceSection;

import trufflesom.vmobjects.SSymbol;


public class Field {
  private final int           index;
  private final SSymbol       name;
  private final SourceSection sourceSection;

  public Field(final int idx, final SSymbol name, final SourceSection sourceSection) {
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

  public SourceSection getSourceSection() {
    return sourceSection;
  }
}
