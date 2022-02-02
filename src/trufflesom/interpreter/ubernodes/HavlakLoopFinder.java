package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.vmobjects.SObject;


public abstract class HavlakLoopFinder {

  /**
   * <pre>
   * isAncestor: w v: v = (
        ^ (w <= v) && (v <= (last at: w)).
     )
   * </pre>
   */
  public static final class IsAncestor extends AbstractInvokable {

    @Child private AtPrim                atPrim;
    @Child private AbstractReadFieldNode readLast;

    public IsAncestor(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      atPrim = AtPrimFactory.create(null, null);
      readLast = FieldAccessorNode.createRead(8);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      long w = (Long) args[1];
      long v = (Long) args[2];

      return w <= v && (v <= (Long) (atPrim.executeEvaluated(frame, readLast.read(rcvr), w)));

    }
  }
}
