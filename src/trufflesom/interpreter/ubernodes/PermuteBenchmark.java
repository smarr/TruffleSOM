package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.vmobjects.SObject;


public abstract class PermuteBenchmark {
  /**
   * <pre>
   * permute: n = (
       count := count + 1.
       n <> 0
          ifTrue: [
              self permute: n - 1.
              n downTo: 1 do: [ :i |
                  self swap: n with: i.
                  self permute: n - 1.
                  self swap: n with: i ] ]
      )
   * </pre>
   */
  /**
   * <pre>
   *
     | count v |
     swap: i with: j = (
         | tmp |
         tmp := v at: i.
         v at: i put: (v at: j).
         v at: j put: tmp
     )
   * </pre>
   */
  public static final class PermuteSwap extends AbstractInvokable {
    @Child private AbstractReadFieldNode readV;

    @Child private AtPrim    atPrim;
    @Child private AtPutPrim atPutPrim;

    public PermuteSwap(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readV = FieldAccessorNode.createRead(1);

      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object i = args[1];
      Object j = args[2];

      Object v = readV.read(rcvr);

      Object tmp = atPrim.executeEvaluated(frame, v, i);
      atPutPrim.executeEvaluated(frame, v, i, atPrim.executeEvaluated(frame, v, j));
      atPutPrim.executeEvaluated(frame, v, j, tmp);
      return rcvr;
    }
  }
}
