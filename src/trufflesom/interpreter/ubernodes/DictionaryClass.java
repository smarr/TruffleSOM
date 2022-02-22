package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameDescriptor.Builder;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.FrameOnStackMarker;
import trufflesom.interpreter.ReturnException;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.vm.Classes;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SBlock;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SObject;


public abstract class DictionaryClass {
  /**
   * <pre>
   * #at:
   * [ :p | p key = aKey ifTrue: [ ^p value ] ].
   * </pre>
   */
  private static final class DictAtBlock extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchKey;
    @Child private AbstractDispatchNode dispatchValue;
    @Child private AbstractDispatchNode dispatchEquals;

    private final int onStackMarker;

    private DictAtBlock(final Source source, final long sourceCoord,
        final int onStackMarker) {
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

        FrameOnStackMarker marker = (FrameOnStackMarker) ctx.getObject(onStackMarker);
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
  private static final class DictContainsKeyBlock extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchKey;
    @Child private AbstractDispatchNode dispatchEquals;

    private static final int onStackMarker = 0;

    private DictContainsKeyBlock(final Source source, final long sourceCoord,
        final int onStackMarker) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchKey = new UninitializedDispatchNode(SymbolTable.symbolFor("key"));
      dispatchEquals = new UninitializedDispatchNode(SymbolTable.symbolFor("="));
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
        FrameOnStackMarker marker = (FrameOnStackMarker) ctx.getObject(onStackMarker);
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

  /**
   * <pre>
   * at: aKey = (
   * pairs do: [ :p | p key = aKey ifTrue: [ ^p value ] ].
   * ^nil
   * )
   */
  public static final class DictAt extends AbstractInvokable {
    @Child private AbstractReadFieldNode readPairs;

    @Child private AbstractDispatchNode dispatchDo;

    private static final int onStackMarker = 0;
    private final SMethod    doBlock;

    @CompilationFinal private boolean rethrows;

    public static DictAt create(final Source source, final long sourceCoord) {
      Builder b = FrameDescriptor.newBuilder(1);
      b.addSlot(FrameSlotKind.Object, "#onStackMarker", null);
      return new DictAt(source, sourceCoord, b.build());
    }

    private DictAt(final Source source, final long sourceCoord, final FrameDescriptor fd) {
      super(fd, source, sourceCoord);
      dispatchDo = new UninitializedDispatchNode(SymbolTable.symbolFor("do:"));
      readPairs = FieldAccessorNode.createRead(0);

      doBlock = new SMethod(SymbolTable.symbolFor("value:"),
          new DictAtBlock(source, sourceCoord, onStackMarker),
          new SMethod[0]);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object pairs = readPairs.read(rcvr);

      FrameOnStackMarker marker = new FrameOnStackMarker();
      frame.setObject(onStackMarker, marker);
      SBlock block = new SBlock(doBlock, Classes.blockClasses[1], frame.materialize());

      try {
        dispatchDo.executeDispatch(frame, new Object[] {pairs, block});
      } catch (ReturnException e) {
        if (e.reachedTarget(marker)) {
          return e.result();
        } else {
          if (!rethrows) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rethrows = true;
          }
          throw e;
        }
      } finally {
        marker.frameNoLongerOnStack();
      }

      return Nil.nilObject;
    }
  }

  /**
   * <pre>
    containsKey: aKey = (
        pairs do: [ :p | p key = aKey ifTrue: [ ^true ] ].
        ^false
    )
   * </pre>
   */
  public static final class DictContainsKey extends AbstractInvokable {
    @Child private AbstractReadFieldNode readPairs;

    @Child private AbstractDispatchNode dispatchDo;

    private final int     onStackMarker;
    private final SMethod doBlock;

    @CompilationFinal private boolean rethrows;

    public static DictContainsKey create(final Source source, final long sourceCoord) {
      Builder b = FrameDescriptor.newBuilder(1);
      b.addSlot(FrameSlotKind.Object, "#onStackMarker", null);
      return new DictContainsKey(source, sourceCoord, b.build());
    }

    private DictContainsKey(final Source source, final long sourceCoord,
        final FrameDescriptor fd) {
      super(fd, source, sourceCoord);
      dispatchDo = new UninitializedDispatchNode(SymbolTable.symbolFor("do:"));
      readPairs = FieldAccessorNode.createRead(0);
      this.onStackMarker = 0;

      doBlock = new SMethod(SymbolTable.symbolFor("value:"),
          new DictContainsKeyBlock(source, sourceCoord, onStackMarker),
          new SMethod[0]);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      Object pairs = readPairs.read(rcvr);

      FrameOnStackMarker marker = new FrameOnStackMarker();
      frame.setObject(onStackMarker, marker);
      SBlock block = new SBlock(doBlock, Classes.blockClasses[1], frame.materialize());

      try {
        dispatchDo.executeDispatch(frame, new Object[] {pairs, block});
      } catch (ReturnException e) {
        if (e.reachedTarget(marker)) {
          return e.result();
        } else {
          if (!rethrows) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            rethrows = true;
          }
          throw e;
        }
      } finally {
        marker.frameNoLongerOnStack();
      }

      return false;
    }
  }
}
