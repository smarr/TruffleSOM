package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.vmobjects.SBlock;


public abstract class SomIdentitySet {
  /**
   * <pre>
   * [ :it |  it == anObject ].
   * </pre>
   */
  public static final class IsObject extends AbstractInvokable {

    public IsObject(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SBlock block = (SBlock) args[0];
      Object it = args[1];

      Object anObject = block.getContext().getArguments()[1];
      return anObject == it;
    }
  }
}
