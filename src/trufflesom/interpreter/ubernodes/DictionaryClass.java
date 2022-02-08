package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;


public abstract class DictionaryClass {
  /**
   * <pre>
   * #at:
   * [ :p | p key = aKey ifTrue: [ ^p value ] ].
   * </pre>
   */
  public static final class DictAtBlock extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchKey;
    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchEquals;

    private final FrameSlot onStackMarker;

    public DictAtBlock(final Source source, final long sourceCoord,
        final FrameSlot onStackMarker) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchKey = new UninitializedDispatchNode(SymbolTable.symbolFor("key"));
      dispatchValue = new UninitializedDispatchNode(SymbolTable.symbolFor("value"));
      dispatchEquals = new UninitializedDispatchNode(SymbolTable.symbolFor("="));
      this.onStackMarker = onStackMarker;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SBlock block = (SBlock) args[0];

      MaterializedFrame ctx = block.getContext();
      Object p = args[1];
      Object aKey = ctx.getArguments()[1];

      Object pairKey = dispatchKey.executeDispatch(frame, new Object[] {p});
      if ((Boolean) dispatchEquals.executeDispatch(frame, new Object[] {pairKey, aKey})) {
        Object value = dispatchValue.executeDispatch(frame, new Object[] {p});

        FrameOnStackMarker marker =
            (FrameOnStackMarker) FrameUtil.getObjectSafe(ctx, onStackMarker);
        if (marker.isOnStack()) {
          throw new ReturnException(value, marker);
        } else {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }
      }

      return Nil.nilObject;
    }
  }

  /**
   * <pre>
    #containsKey:
    [ :p | p key = aKey ifTrue: [ ^true ] ].
   * </pre>
   */
  public static final class DictContainsKeyBlock extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchKey;
    @Child private AbstractDispatchNode dispatchEquals;

    private final FrameSlot onStackMarker;

    public DictContainsKeyBlock(final Source source, final long sourceCoord,
        final FrameSlot onStackMarker) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchKey = new UninitializedDispatchNode(SymbolTable.symbolFor("key"));
      dispatchEquals = new UninitializedDispatchNode(SymbolTable.symbolFor("="));
      this.onStackMarker = onStackMarker;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SBlock block = (SBlock) args[0];

      MaterializedFrame ctx = block.getContext();
      Object p = args[1];
      Object aKey = ctx.getArguments()[1];

      Object pairKey = dispatchKey.executeDispatch(frame, new Object[] {p});
      if ((Boolean) dispatchEquals.executeDispatch(frame, new Object[] {pairKey, aKey})) {
        FrameOnStackMarker marker =
            (FrameOnStackMarker) FrameUtil.getObjectSafe(ctx, onStackMarker);
        if (marker.isOnStack()) {
          throw new ReturnException(true, marker);
        } else {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          throw new UnsupportedOperationException();
        }
      }

      return Nil.nilObject;
    }
  }
}
