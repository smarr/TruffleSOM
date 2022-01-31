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
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SSymbol;


public abstract class ListBenchmark {
  /**
   * <pre>
   * benchmark = ( | result |
        result := self
            tailWithX: (self makeList: 15)
            withY: (self makeList: 10)
            withZ: (self makeList: 6).
        ^ result length
    )
   * </pre>
   */
  public static final class ListBenchmarkMethod extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchMakeList;
    @Child private AbstractDispatchNode dispatchTail;
    @Child private AbstractDispatchNode dispatchLength;

    public ListBenchmarkMethod(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchMakeList = new UninitializedDispatchNode(SymbolTable.symbolFor("makeList:"));
      dispatchTail =
          new UninitializedDispatchNode(SymbolTable.symbolFor("tailWithX:withY:withZ:"));
      dispatchLength = new UninitializedDispatchNode(SymbolTable.symbolFor("length"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object rcvr = frame.getArguments()[0];

      Object result = dispatchTail.executeDispatch(frame, new Object[] {
          rcvr,
          dispatchMakeList.executeDispatch(frame, new Object[] {rcvr, 15L}),
          dispatchMakeList.executeDispatch(frame, new Object[] {rcvr, 10L}),
          dispatchMakeList.executeDispatch(frame, new Object[] {rcvr, 6L}),
      });

      return dispatchLength.executeDispatch(frame, new Object[] {result});
    }
  }

  /**
   * <pre>
   * verifyResult: result = (
      ^ self assert: 10 equals: result.
    )
   * </pre>
   */
  public static final class ListVerifyResult extends AbstractInvokable {
    public ListVerifyResult(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      long result = (Long) frame.getArguments()[1];
      return result == 10L;
    }
  }

  /**
   * <pre>
   * makeList: length = (
        (length = 0)
            ifTrue: [ ^nil ]
            ifFalse: [
                ^(ListElement new: length)
                    next: (self makeList: (length - 1)) ].
    )
   * </pre>
   */
  public static final class ListMakeList extends AbstractInvokable {
    @CompilationFinal Association globalListElement;

    @Child private AbstractDispatchNode dispatchNew;
    @Child private AbstractDispatchNode dispatchNext;
    @Child private AbstractDispatchNode dispatchMakeList;

    public ListMakeList(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      dispatchNew = new UninitializedDispatchNode(SymbolTable.symbolFor("new:"));
      dispatchNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next:"));
      dispatchMakeList = new UninitializedDispatchNode(SymbolTable.symbolFor("makeList:"));
    }

    private void lookupListElement(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("ListElement");
      globalListElement = Globals.getGlobalsAssociation(sym);

      if (globalListElement == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalListElement = Globals.getGlobalsAssociation(sym);
      }
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      Object rcvr = args[0];

      if (globalListElement == null) {
        lookupListElement(rcvr);
      }

      long length = (Long) args[1];

      if (0 == length) {
        return Nil.nilObject;
      }

      Object listElement = dispatchNew.executeDispatch(frame, new Object[] {
          globalListElement.getValue(), length});
      Object nextArg =
          dispatchMakeList.executeDispatch(frame, new Object[] {rcvr, length - 1L});
      return dispatchNext.executeDispatch(frame, new Object[] {listElement, nextArg});
    }
  }

  /**
   * <pre>
   * isShorter: x than: y = (
        | xTail yTail |

        xTail := x. yTail := y.
        [ yTail isNil ]
            whileFalse: [
                xTail isNil ifTrue: [ ^true ].
                xTail := xTail next.
                yTail := yTail next ].

        ^false
    )
   * </pre>
   */
  public static final class ListIsShorter extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchNext;

    public ListIsShorter(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      Object xTail = args[1];
      Object yTail = args[2];

      while (!(yTail == Nil.nilObject)) {
        if (xTail == Nil.nilObject) {
          return true;
        }

        xTail = dispatchNext.executeDispatch(frame, new Object[] {xTail});
        yTail = dispatchNext.executeDispatch(frame, new Object[] {yTail});
      }

      return false;
    }
  }

  /**
   * <pre>
   * tailWithX: x withY: y withZ: z = (
        (self isShorter: y than: x)
            ifTrue: [
                ^(self
                    tailWithX: (self tailWithX: x next withY: y withZ: z)
                    withY: (self tailWithX: y next withY: z withZ: x)
                    withZ: (self tailWithX: z next withY: x withZ: y)) ]
            ifFalse: [ ^z ].
    )
   * </pre>
   */
  public static final class ListTail extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchIsShorter;
    @Child private AbstractDispatchNode dispatchTail;
    @Child private AbstractDispatchNode dispatchNext;

    public ListTail(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      dispatchIsShorter =
          new UninitializedDispatchNode(SymbolTable.symbolFor("isShorter:than:"));
      dispatchTail =
          new UninitializedDispatchNode(SymbolTable.symbolFor("tailWithX:withY:withZ:"));
      dispatchNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      Object rcvr = args[0];
      Object x = args[1];
      Object y = args[2];
      Object z = args[3];

      boolean isShorter =
          (Boolean) dispatchIsShorter.executeDispatch(frame, new Object[] {rcvr, y, x});
      if (isShorter) {
        return dispatchTail.executeDispatch(frame, new Object[] {
            rcvr,
            dispatchTail.executeDispatch(
                frame, new Object[] {
                    rcvr, dispatchNext.executeDispatch(frame, new Object[] {x}), y, z}),
            dispatchTail.executeDispatch(
                frame, new Object[] {
                    rcvr, dispatchNext.executeDispatch(frame, new Object[] {y}), z, x}),
            dispatchTail.executeDispatch(
                frame, new Object[] {
                    rcvr, dispatchNext.executeDispatch(frame, new Object[] {z}), x, y}),
        });
      }

      return z;
    }
  }
}
