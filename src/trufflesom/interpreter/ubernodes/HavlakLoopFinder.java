package trufflesom.interpreter.ubernodes;

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
import trufflesom.vmobjects.SArray;
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

  /**
   * <pre>
   * | cfg lsg
    nonBackPreds backPreds number (4)
    maxSize header type last (8) nodes (9) |

   *   doDFS: currentNode current: current = (
    | lastId outerBlocks |

    (nodes at: current) initNode: currentNode dfs: current.
    number at: currentNode put: current.

    lastId := current.
    outerBlocks := currentNode outEdges.

    1 to: outerBlocks size do: [:i |
      | target |
      target := outerBlocks at: i.
      (number at: target) = self Unvisited ifTrue: [
        lastId := self doDFS: target current: lastId + 1 ] ].

    last at: current put: lastId.
    ^ lastId
  )
   *
   * </pre>
   */
  public static final class DoDFSCurrent extends AbstractInvokable {

    @Child private AtPrim    atPrim;
    @Child private AtPutPrim atPutPrim;

    @Child private AbstractReadFieldNode readNumber;
    @Child private AbstractReadFieldNode readLast;
    @Child private AbstractReadFieldNode readNodes;

    @Child private AbstractDispatchNode dispatchInitNode;
    @Child private AbstractDispatchNode dispatchAt;
    @Child private AbstractDispatchNode dispatchAtPut;
    @Child private AbstractDispatchNode dispatchOutEdges;
    @Child private AbstractDispatchNode dispatchSize;
    @Child private AbstractDispatchNode dispatchDoDFSCurrent;
    @Child private AbstractDispatchNode dispatchUnvisited;

    public DoDFSCurrent(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      atPrim = AtPrimFactory.create(null, null);
      atPutPrim = AtPutPrimFactory.create(null, null, null);
      readNumber = FieldAccessorNode.createRead(4);
      readLast = FieldAccessorNode.createRead(8);
      readNodes = FieldAccessorNode.createRead(9);

      dispatchInitNode = new UninitializedDispatchNode(SymbolTable.symbolFor("initNode:dfs:"));
      dispatchAt = new UninitializedDispatchNode(SymbolTable.symbolFor("at:"));
      dispatchAtPut = new UninitializedDispatchNode(SymbolTable.symbolFor("at:put:"));
      dispatchOutEdges = new UninitializedDispatchNode(SymbolTable.symbolFor("outEdges"));
      dispatchSize = new UninitializedDispatchNode(SymbolTable.symbolFor("size"));
      dispatchDoDFSCurrent =
          new UninitializedDispatchNode(SymbolTable.symbolFor("doDFS:current:"));
      dispatchUnvisited = new UninitializedDispatchNode(SymbolTable.symbolFor("Unvisited"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object currentNode = args[1];
      Object current = args[2];

      // (nodes at: current)
      SArray nodes = (SArray) readNodes.read(rcvr);
      Object nodesAtCurrent = atPrim.executeEvaluated(frame, nodes, current);

      // nodesAtCurrent initNode: currentNode dfs: current.
      dispatchInitNode.executeDispatch(frame,
          new Object[] {nodesAtCurrent, currentNode, current});

      // number at: currentNode put: current.
      Object number = readNumber.read(rcvr);
      dispatchAtPut.executeDispatch(frame, new Object[] {number, currentNode, current});

      // lastId := current.
      long lastId = (Long) current;
      // outerBlocks := currentNode outEdges.
      Object outerBlocks = dispatchOutEdges.executeDispatch(frame, new Object[] {currentNode});

      // 1 to: outerBlocks size do: [:i |
      long outerBlocksSize =
          (Long) dispatchSize.executeDispatch(frame, new Object[] {outerBlocks});
      for (long i = 1; i <= outerBlocksSize; i += 1) {
        // target := outerBlocks at: i.
        Object target = dispatchAt.executeDispatch(frame, new Object[] {outerBlocks, i});

        // (number at: target)
        long numberAtTarget =
            (Long) dispatchAt.executeDispatch(frame, new Object[] {number, target});

        // self Unvisited
        long unvisited = (Long) dispatchUnvisited.executeDispatch(frame, new Object[] {rcvr});

        // numberAtTarget = self Unvisited ifTrue: [
        if (numberAtTarget == unvisited) {
          // lastId := self doDFS: target current: lastId + 1 ] ].
          lastId = (Long) dispatchDoDFSCurrent.executeDispatch(frame,
              new Object[] {rcvr, target, lastId + 1L});
        }
      }

      // last at: current put: lastId.
      SArray last = (SArray) readLast.read(rcvr);
      atPutPrim.executeEvaluated(frame, last, current, lastId);

      // ^ lastId
      return lastId;

    }
  }
}
