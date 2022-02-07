package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.primitives.arrays.AtPrim;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class RichardsBenchmark {
  /**
   * <pre>
   * |taskList (0) currentTask (1) currentTaskIdentity (2) taskTable (3) tracing layout queuePacketCount holdCount|
   * findTask: identity = (
        | t |
        t := taskTable at: identity.
        RBObject NoTask == t ifTrue: [self error: 'findTask failed'].
        ^ t
      )
   * </pre>
   */
  public static final class SchedulerFindTask extends AbstractInvokable {
    @Child private AtPrim atPrim;

    @Child private AbstractReadFieldNode readTaskTable;

    @Child private AbstractDispatchNode dispatchNoTask;
    @Child private AbstractDispatchNode dispatchError;

    @CompilationFinal Association globalRBObject;

    public SchedulerFindTask(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      atPrim = AtPrimFactory.create(null, null);
      readTaskTable = FieldAccessorNode.createRead(3);
      dispatchNoTask = new UninitializedDispatchNode(SymbolTable.symbolFor("NoTask"));
      dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object identity = args[1];

      if (globalRBObject == null) {
        lookupRBObject(rcvr);
      }

      Object t = atPrim.executeEvaluated(frame, readTaskTable.read(rcvr), identity);

      if (dispatchNoTask.executeDispatch(frame,
          new Object[] {globalRBObject.getValue()}) == t) {
        dispatchError.executeDispatch(frame, new Object[] {"findTask failed"});
      }
      return t;
    }

    private void lookupRBObject(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("RBObject");
      globalRBObject = Globals.getGlobalsAssociation(sym);

      if (globalRBObject == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalRBObject = Globals.getGlobalsAssociation(sym);
      }
    }
  }

  /**
   * <pre>
   * | packetPending taskWaiting taskHolding |
   * isTaskHoldingOrWaiting = ( ^ taskHolding or: [packetPending not and: [taskWaiting]] ).
   * </pre>
   */

  public static final class TSIsTask extends AbstractInvokable {
    @Child private AbstractReadFieldNode readTaskHolding;
    @Child private AbstractReadFieldNode readPacketPending;
    @Child private AbstractReadFieldNode readTaskWaiting;

    public TSIsTask(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readPacketPending = FieldAccessorNode.createRead(0);
      readTaskWaiting = FieldAccessorNode.createRead(1);
      readTaskHolding = FieldAccessorNode.createRead(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      return readTaskHolding.read(rcvr) == Boolean.TRUE
          || (readPacketPending.read(rcvr) == Boolean.FALSE
              && readTaskWaiting.read(rcvr) == Boolean.TRUE);
    }
  }

  /**
   * <pre>
   * isWaitingWithPacket = ( ^ packetPending and: [taskWaiting and: [taskHolding not]] ).
   * </pre>
   */
  public static final class TSIsWaitingWithPacket extends AbstractInvokable {
    @Child private AbstractReadFieldNode readTaskHolding;
    @Child private AbstractReadFieldNode readPacketPending;
    @Child private AbstractReadFieldNode readTaskWaiting;

    public TSIsWaitingWithPacket(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readPacketPending = FieldAccessorNode.createRead(0);
      readTaskWaiting = FieldAccessorNode.createRead(1);
      readTaskHolding = FieldAccessorNode.createRead(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];

      return readPacketPending.read(rcvr) == Boolean.TRUE
          && readTaskWaiting.read(rcvr) == Boolean.TRUE
          && readTaskHolding.read(rcvr) == Boolean.FALSE;
    }
  }

  /**
   * <pre>
   * append: packet head: queueHead = (
      | mouse link |
      packet link: RBObject NoWork.
      RBObject NoWork == queueHead ifTrue: [ ^ packet ].
      mouse := queueHead.
      [RBObject NoWork == (link := mouse link)]
              whileFalse: [mouse := link].
      mouse link: packet.
      ^ queueHead
    )
   * </pre>
   */
  public static final class RBOAppendHead extends AbstractInvokable {
    @Child private AbstractReadFieldNode readTaskHolding;
    @Child private AbstractReadFieldNode readPacketPending;
    @Child private AbstractReadFieldNode readTaskWaiting;

    @Child private AbstractDispatchNode dispatchNoWork;
    @Child private AbstractDispatchNode dispatchLink;
    @Child private AbstractDispatchNode dispatchLink_;

    @CompilationFinal Association globalRBObject;

    public RBOAppendHead(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readPacketPending = FieldAccessorNode.createRead(0);
      readTaskWaiting = FieldAccessorNode.createRead(1);
      readTaskHolding = FieldAccessorNode.createRead(2);

      dispatchNoWork = new UninitializedDispatchNode(SymbolTable.symbolFor("NoWork"));
      dispatchLink = new UninitializedDispatchNode(SymbolTable.symbolFor("link"));
      dispatchLink_ = new UninitializedDispatchNode(SymbolTable.symbolFor("link:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object packet = args[1];
      Object queueHead = args[2];

      if (globalRBObject == null) {
        lookupRBObject(rcvr);
      }

      dispatchLink_.executeDispatch(frame, new Object[] {
          packet,
          dispatchNoWork.executeDispatch(frame, new Object[] {globalRBObject.getValue()})
      });

      if (dispatchNoWork.executeDispatch(frame,
          new Object[] {globalRBObject.getValue()}) == queueHead) {
        return packet;
      }

      Object mouse = queueHead;
      Object link;

      while (dispatchNoWork.executeDispatch(frame,
          new Object[] {globalRBObject.getValue()}) != (link =
              dispatchLink.executeDispatch(frame, new Object[] {mouse}))) {
        mouse = link;
      }

      dispatchLink_.executeDispatch(frame, new Object[] {mouse, packet});
      return queueHead;
    }

    private void lookupRBObject(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("RBObject");
      globalRBObject = Globals.getGlobalsAssociation(sym);

      if (globalRBObject == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalRBObject = Globals.getGlobalsAssociation(sym);
      }
    }
  }

  /**
   * <pre>
   * TaskState = RBObject (
    | packetPending taskWaiting taskHolding (2) |
  
    TaskControlBlock = TaskState (
    | link identity priority input (6) function handle |
  
   * addInput: packet checkPriority: oldTask = (
    RBObject NoWork == input
      ifTrue: [
        input := packet.
        self packetPending: true.
        priority > oldTask priority ifTrue: [ ^ self ] ]
      ifFalse: [
        input := self append: packet head: input ].
    ^ oldTask
  )
   *
   * </pre>
   */

  public static final class TCBAddInputCheckPriority extends AbstractInvokable {
    @Child private AbstractReadFieldNode  readInput;
    @Child private AbstractWriteFieldNode writeInput;
    @Child private AbstractReadFieldNode  readPriority;

    @Child private AbstractDispatchNode dispatchNoWork;
    @Child private AbstractDispatchNode dispatchPacketPending;
    @Child private AbstractDispatchNode dispatchPriority;
    @Child private AbstractDispatchNode dispatchAppendHead;

    @CompilationFinal Association globalRBObject;

    public TCBAddInputCheckPriority(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readInput = FieldAccessorNode.createRead(6);
      writeInput = FieldAccessorNode.createWrite(6);

      readPriority = FieldAccessorNode.createRead(5);

      dispatchNoWork = new UninitializedDispatchNode(SymbolTable.symbolFor("NoWork"));
      dispatchPacketPending =
          new UninitializedDispatchNode(SymbolTable.symbolFor("packetPending:"));
      dispatchPriority = new UninitializedDispatchNode(SymbolTable.symbolFor("priority"));
      dispatchAppendHead =
          new UninitializedDispatchNode(SymbolTable.symbolFor("append:head:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object packet = args[1];
      Object oldTask = args[2];

      if (globalRBObject == null) {
        lookupRBObject(rcvr);
      }

      if (dispatchNoWork.executeDispatch(frame,
          new Object[] {globalRBObject.getValue()}) == readInput.read(rcvr)) {
        writeInput.write(rcvr, packet);
        dispatchPacketPending.executeDispatch(frame, new Object[] {rcvr, true});
        if (readPriority.readLongSafe(
            rcvr) > (Long) dispatchPriority.executeDispatch(frame, new Object[] {oldTask})) {
          return rcvr;
        }
      } else {
        writeInput.write(rcvr, dispatchAppendHead.executeDispatch(frame,
            new Object[] {rcvr, packet, readInput.read(rcvr)}));
      }

      return oldTask;
    }

    private void lookupRBObject(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("RBObject");
      globalRBObject = Globals.getGlobalsAssociation(sym);

      if (globalRBObject == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalRBObject = Globals.getGlobalsAssociation(sym);
      }
    }
  }

}
