package trufflesom.bdt.testsetup;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.bdt.inlining.nodes.WithSource;
import trufflesom.bdt.source.SourceCoordinate;


public abstract class ExprNode extends Node implements WithSource {

  private long sourceCoord;

  @Override
  @SuppressWarnings("unchecked")
  public ExprNode initialize(final long coord) {
    this.sourceCoord = coord;
    return this;
  }

  @Override
  public Source getSource() {
    return ((WithSource) getParent()).getSource();
  }

  @Override
  public boolean hasSource() {
    return false;
  }

  @Override
  public SourceSection getSourceSection() {
    return SourceCoordinate.createSourceSection(
        getRootNode().getSourceSection().getSource(), sourceCoord);
  }

  @Override
  public long getSourceCoordinate() {
    return sourceCoord;
  }

  public abstract Object executeGeneric(VirtualFrame frame);

  public int executeInt(final VirtualFrame frame) throws UnexpectedResultException {
    Object result = executeGeneric(frame);
    if (result instanceof Integer) {
      return (int) result;
    } else {
      throw new UnexpectedResultException(result);
    }
  }

  public boolean executeBool(final VirtualFrame frame) throws UnexpectedResultException {
    Object result = executeGeneric(frame);
    if (result instanceof Boolean) {
      return (boolean) result;
    } else {
      throw new UnexpectedResultException(result);
    }
  }
}
