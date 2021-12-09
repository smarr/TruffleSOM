package bd.testsetup;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;

import bd.inlining.nodes.WithSource;
import bd.source.SourceCoordinate;


public abstract class ExprNode extends Node implements WithSource {

  private long sourceCoord;

  @Override
  @SuppressWarnings("unchecked")
  public ExprNode initialize(final long coord) {
    this.sourceCoord = coord;
    return this;
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
