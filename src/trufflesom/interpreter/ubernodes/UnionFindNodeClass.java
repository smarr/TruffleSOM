package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vmobjects.SObject;


public abstract class UnionFindNodeClass {
  /**
   * <pre>
   * | parent_ bb_ dfsNumber_ (2) loop |
  
  initialize = (
    dfsNumber_ := 0.
  )
   * </pre>
   */
  public static final class UFNInitialize extends AbstractInvokable {

    @Child private AbstractWriteFieldNode writeDfsNumber;

    public UFNInitialize(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeDfsNumber = FieldAccessorNode.createWrite(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      writeDfsNumber.write(rcvr, 0L);
      return rcvr;
    }
  }

  /**
   * <pre>
   *
  initNode: bb dfs: dfsNumber = (
    parent_ := self.
    bb_ := bb.
    dfsNumber_ := dfsNumber.
  )
   * </pre>
   */
  public static final class UFNInitNode extends AbstractInvokable {

    @Child private AbstractWriteFieldNode writeParent;
    @Child private AbstractWriteFieldNode writeBB;
    @Child private AbstractWriteFieldNode writeDfsNumber;

    public UFNInitNode(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeParent = FieldAccessorNode.createWrite(0);
      writeBB = FieldAccessorNode.createWrite(1);
      writeDfsNumber = FieldAccessorNode.createWrite(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object bb = args[1];
      long dfsNumber = (Long) args[2];

      writeParent.write(rcvr, rcvr);
      writeBB.write(rcvr, bb);
      writeDfsNumber.write(rcvr, dfsNumber);
      return rcvr;
    }
  }
}
