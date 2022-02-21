package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrim;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


public abstract class TowersBenchmark {
  /**
   * <pre>
   * | piles movesdone |
   * pushDisk: disk onPile: pile = (
          | top |

          top := piles at: pile.
          (top isNil not) && [ disk size >= top size ]
              ifTrue: [ self error: 'Cannot put a big disk on a smaller one' ].

          disk next: top.
          piles at: pile put: disk.
      )
   * </pre>
   */
  public static final class TowersPushDisk extends AbstractInvokable {
    @Child private AbstractReadFieldNode readPiles;
    @Child private AtPrim                atPrim;
    @Child private AtPutPrim             atPutPrim;

    @Child private AbstractDispatchNode dispatchDiskSize;
    @Child private AbstractDispatchNode dispatchDiskNext;
    @Child private AbstractDispatchNode dispatchError;

    @CompilationFinal private boolean errorPath;

    public TowersPushDisk(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readPiles = FieldAccessorNode.createRead(0);
      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);

      dispatchDiskSize = new UninitializedDispatchNode(SymbolTable.symbolFor("size"));
      dispatchDiskNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next:"));
      dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object disk = args[1];
      Object pile = args[2];

      Object top = atPrim.executeEvaluated(frame, readPiles.read(rcvr), pile);
      if (top != Nil.nilObject &&
          ((Long) dispatchDiskSize.executeDispatch(frame,
              new Object[] {disk}) >= ((Long) dispatchDiskSize.executeDispatch(frame,
                  new Object[] {top})))) {
        if (!errorPath) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          errorPath = true;
        }

        dispatchError.executeDispatch(frame,
            new Object[] {rcvr, "Cannot put a big disk on a smaller one"});
      }

      dispatchDiskNext.executeDispatch(frame, new Object[] {disk, top});

      atPutPrim.executeEvaluated(frame, readPiles.read(rcvr), pile, disk);
      return rcvr;
    }
  }

  /**
   * <pre>
   popDiskFrom: pile = (
       | top |

       top := piles at: pile.
       top isNil
           ifTrue: [
               self error: 'Attempting to remove a disk from an empty pile' ].

       piles at: pile put: top next.
       top next: nil.
       ^top
   )
   * </pre>
   */
  public static final class TowersPopDisk extends AbstractInvokable {
    @Child private AbstractReadFieldNode readPiles;
    @Child private AtPrim                atPrim;
    @Child private AtPutPrim             atPutPrim;

    @Child private AbstractDispatchNode dispatchDiskNext;
    @Child private AbstractDispatchNode dispatchDiskNext_;
    @Child private AbstractDispatchNode dispatchError;

    @CompilationFinal private boolean errorPath;

    public TowersPopDisk(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readPiles = FieldAccessorNode.createRead(0);
      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);

      dispatchDiskNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
      dispatchDiskNext_ = new UninitializedDispatchNode(SymbolTable.symbolFor("next:"));
      dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object pile = args[1];

      Object top = atPrim.executeEvaluated(frame, readPiles.read(rcvr), pile);

      if (top == Nil.nilObject) {
        if (!errorPath) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          errorPath = true;
        }

        dispatchError.executeDispatch(frame,
            new Object[] {rcvr, "Attempting to remove a disk from an empty pile"});
      }

      atPutPrim.executeEvaluated(frame, readPiles.read(rcvr), pile,
          dispatchDiskNext.executeDispatch(frame, new Object[] {top}));

      dispatchDiskNext_.executeDispatch(frame, new Object[] {top, Nil.nilObject});

      return top;
    }
  }
}
