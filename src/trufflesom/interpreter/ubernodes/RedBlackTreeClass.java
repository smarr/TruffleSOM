package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class RedBlackTreeClass {
  /**
   * <pre>
   * RedBlackTree = (
    | root |
  
    at: key put: value = (
      | insertionResult x |
      insertionResult := self treeAt: key insert: value.
      insertionResult isNewEntry ifFalse: [
        ^ insertionResult oldValue ].
  
      x := insertionResult newNode.
  
      [ x ~= root and: [ x parent color = #red ]] whileTrue: [
        x parent == x parent parent left
          ifTrue: [
            | y |
            y := x parent parent right.
            (y notNil and: [y color = #red])
              ifTrue: [
                "Case 1"
                x parent color: #black.
                y color: #black.
                x parent parent color: #red.
                x := x parent parent ]
              ifFalse: [
                x == x parent right ifTrue: [
                  "Case 2"
                  x := x parent.
                  self leftRotate: x ].
  
                "Case 3"
                x parent color: #black.
                x parent parent color: #red.
                self rightRotate: x parent parent ] ]
          ifFalse: [
            "Same as 'then' clause with 'right' and 'left' exchanged"
            | y |
            y := x parent parent left.
            (y notNil and: [ y color = #red ])
              ifTrue: [
                "Case 1"
                x parent color: #black.
                y color: #black.
                x parent parent color: #red.
                x := x parent parent ]
              ifFalse: [
                x == x parent left ifTrue: [
                  "Case 2"
                  x := x parent.
                  self rightRotate: x ].
  
                "Case 3"
                x parent color: #black.
                x parent parent color: #red.
                self leftRotate: x parent parent ] ] ].
  
      root color: #black.
      ^ nil
    )
   * </pre>
   */
  public static final class RBTAtPut extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchTreeAtInsert;
    @Child private AbstractDispatchNode dispatchIsNewEntry;
    @Child private AbstractDispatchNode dispatchOldValue;
    @Child private AbstractDispatchNode dispatchNewNode;

    @Child private AbstractDispatchNode dispatchNodeParent;
    @Child private AbstractDispatchNode dispatchNodeColor;
    @Child private AbstractDispatchNode dispatchNodeColor_;
    @Child private AbstractDispatchNode dispatchNodeLeft;
    @Child private AbstractDispatchNode dispatchNodeRight;

    @Child private AbstractDispatchNode dispatchLeftRotate;
    @Child private AbstractDispatchNode dispatchRightRotate;

    @Child private AbstractReadFieldNode readRoot;

    private static final SSymbol symRed   = SymbolTable.symbolFor("red");
    private static final SSymbol symBlack = SymbolTable.symbolFor("black");

    public RBTAtPut(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchTreeAtInsert =
          new UninitializedDispatchNode(SymbolTable.symbolFor("treeAt:insert:"));
      dispatchIsNewEntry = new UninitializedDispatchNode(SymbolTable.symbolFor("isNewEntry"));
      dispatchOldValue = new UninitializedDispatchNode(SymbolTable.symbolFor("oldValue"));
      dispatchNewNode = new UninitializedDispatchNode(SymbolTable.symbolFor("newNode"));

      dispatchNodeParent = new UninitializedDispatchNode(SymbolTable.symbolFor("parent"));
      dispatchNodeColor = new UninitializedDispatchNode(SymbolTable.symbolFor("color"));
      dispatchNodeColor_ = new UninitializedDispatchNode(SymbolTable.symbolFor("color:"));
      dispatchNodeLeft = new UninitializedDispatchNode(SymbolTable.symbolFor("left"));
      dispatchNodeRight = new UninitializedDispatchNode(SymbolTable.symbolFor("right"));

      dispatchLeftRotate = new UninitializedDispatchNode(SymbolTable.symbolFor("leftRotate:"));
      dispatchRightRotate =
          new UninitializedDispatchNode(SymbolTable.symbolFor("rightRotate:"));

      readRoot = FieldAccessorNode.createRead(0);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object key = args[1];
      Object value = args[2];

      // insertionResult := self treeAt: key insert: value.
      Object insertionResult =
          dispatchTreeAtInsert.executeDispatch(frame, new Object[] {rcvr, key, value});

      // insertionResult isNewEntry ifFalse: [
      if (dispatchIsNewEntry.executeDispatch(frame,
          new Object[] {insertionResult}) == Boolean.FALSE) {
        // ^ insertionResult oldValue ].
        return dispatchOldValue.executeDispatch(frame, new Object[] {insertionResult});
      }

      // x := insertionResult newNode.
      Object x = dispatchNewNode.executeDispatch(frame, new Object[] {insertionResult});

      // [ x ~= root and: [ x parent color = #red ]] whileTrue: [
      while (x != readRoot.read(rcvr) && (dispatchNodeColor.executeDispatch(frame,
          new Object[] {
              dispatchNodeParent.executeDispatch(frame, new Object[] {x})
          }) == symRed)) {

        // x parent == x parent parent left ifTrue: [
        if (dispatchNodeParent.executeDispatch(
            frame, new Object[] {x}) == dispatchNodeLeft.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})})})) {
          // y := x parent parent right.
          Object y = dispatchNodeRight.executeDispatch(
              frame, new Object[] {dispatchNodeParent.executeDispatch(
                  frame, new Object[] {dispatchNodeParent.executeDispatch(
                      frame, new Object[] {x})})});

          // (y notNil and: [y color = #red]) ifTrue: [
          if (y != Nil.nilObject
              && dispatchNodeColor.executeDispatch(frame, new Object[] {y}) == symRed) {
            // Case 1
            // x parent color: #black.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x}), symBlack});

            // y color: #black.
            dispatchNodeColor_.executeDispatch(frame, new Object[] {y, symBlack});

            // x parent parent color: #red.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})}),
                    symRed});

            // x := x parent parent
            x = dispatchNodeParent.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x})});
          } else {
            // x == x parent right ifTrue: [
            if (x == dispatchNodeRight.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x})})) {

              // Case 2
              // x := x parent.
              x = dispatchNodeParent.executeDispatch(frame, new Object[] {x});

              // self leftRotate: x
              dispatchLeftRotate.executeDispatch(frame, new Object[] {rcvr, x});
            }

            // Case 3
            // x parent color: #black.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x}), symBlack});

            // x parent parent color: #red.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})}),
                    symRed});

            // self rightRotate: x parent parent
            dispatchRightRotate.executeDispatch(frame, new Object[] {
                rcvr, dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})})});
          }

        } else {
          // Same as 'then' clause with 'right' and 'left' exchanged
          // y := x parent parent left.
          Object y = dispatchNodeLeft.executeDispatch(
              frame, new Object[] {dispatchNodeParent.executeDispatch(
                  frame, new Object[] {dispatchNodeParent.executeDispatch(
                      frame, new Object[] {x})})});

          // (y notNil and: [ y color = #red ]) ifTrue: [
          if (y != Nil.nilObject
              && dispatchNodeColor.executeDispatch(frame, new Object[] {y}) == symRed) {
            // Case 1
            // x parent color: #black.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x}), symBlack});

            // y color: #black.
            dispatchNodeColor_.executeDispatch(frame, new Object[] {y, symBlack});

            // x parent parent color: #red.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})}),
                    symRed});

            // x := x parent parent
            x = dispatchNodeParent.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x})});

          } else {
            // x == x parent left ifTrue: [
            if (x == dispatchNodeLeft.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x})})) {

              // Case 2
              // x := x parent.
              x = dispatchNodeParent.executeDispatch(frame, new Object[] {x});

              // self rightRotate: x
              dispatchRightRotate.executeDispatch(frame, new Object[] {rcvr, x});
            }

            // Case 3
            // x parent color: #black.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {x}), symBlack});

            // x parent parent color: #red.
            dispatchNodeColor_.executeDispatch(
                frame, new Object[] {dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})}),
                    symRed});

            // self leftRotate: x parent parent
            dispatchLeftRotate.executeDispatch(frame, new Object[] {
                rcvr, dispatchNodeParent.executeDispatch(
                    frame, new Object[] {dispatchNodeParent.executeDispatch(
                        frame, new Object[] {x})})});
          }
        }
      }

      // root color: #black.
      dispatchNodeColor_.executeDispatch(frame, new Object[] {readRoot.read(rcvr), symBlack});

      // ^ nil
      return Nil.nilObject;
    }
  }

  /**
   * <pre>
   * InsertResult
   * | isNewEntry newNode oldValue |
   * init: aBool node: aNode value: val = (
      isNewEntry := aBool.
      newNode    := aNode.
      oldValue   := val.
    )
   * </pre>
   */
  public static final class IRInit extends AbstractInvokable {

    @Child private AbstractWriteFieldNode writeIsNewEntry;
    @Child private AbstractWriteFieldNode writeNewNode;
    @Child private AbstractWriteFieldNode writeOldValue;

    public IRInit(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeIsNewEntry = FieldAccessorNode.createWrite(0);
      writeNewNode = FieldAccessorNode.createWrite(1);
      writeOldValue = FieldAccessorNode.createWrite(2);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aBool = args[1];
      Object aNode = args[2];
      Object val = args[3];

      writeIsNewEntry.write(rcvr, aBool);
      writeNewNode.write(rcvr, aNode);
      writeOldValue.write(rcvr, val);
      return rcvr;
    }
  }

  /**
   * <pre>
   *   | key (0) value (1) left right parent color (5) |
   * init: aKey value: aValue = (
      key   := aKey.
      value := aValue.
      color := #red.
    )
   * </pre>
   */
  public static final class NodeInit extends AbstractInvokable {
    private static final SSymbol symRed = SymbolTable.symbolFor("red");

    @Child private AbstractWriteFieldNode writeKey;
    @Child private AbstractWriteFieldNode writeValue;
    @Child private AbstractWriteFieldNode writeColor;

    public NodeInit(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      writeKey = FieldAccessorNode.createWrite(0);
      writeValue = FieldAccessorNode.createWrite(1);
      writeColor = FieldAccessorNode.createWrite(5);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object aKey = args[1];
      Object aValue = args[2];

      writeKey.write(rcvr, aKey);
      writeValue.write(rcvr, aValue);
      writeColor.write(rcvr, symRed);
      return rcvr;
    }
  }
}
