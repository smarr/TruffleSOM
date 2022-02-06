package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.primitives.basics.NewObjectPrim;
import trufflesom.primitives.basics.NewObjectPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SClass;


public abstract class JsonValues {
  /**
   * <pre>
   * new: str = ( ^ self new initializeWith: str. )
   * </pre>
   */
  public static final class JStringNew extends AbstractInvokable {

    @Child private NewObjectPrim        newPrim;
    @Child private AbstractDispatchNode dispatchInit;

    public JStringNew(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      newPrim = NewObjectPrimFactory.create(null);
      dispatchInit = new UninitializedDispatchNode(SymbolTable.symbolFor("initializeWith:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SClass clazz = (SClass) args[0];
      Object str = args[1];

      Object newObj = newPrim.executeEvaluated(frame, clazz);
      return dispatchInit.executeDispatch(frame, new Object[] {newObj, str});
    }
  }

  /**
   * <pre>
   * new: string = (
        string ifNil: [ self error: 'string is null' ].
        ^ self new initializeWith: string
      )
   * </pre>
   */
  public static final class JNumberNew extends AbstractInvokable {
    @CompilationFinal boolean error;

    @Child private NewObjectPrim        newPrim;
    @Child private AbstractDispatchNode dispatchError;
    @Child private AbstractDispatchNode dispatchInit;

    public JNumberNew(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      newPrim = NewObjectPrimFactory.create(null);
      dispatchError = new UninitializedDispatchNode(SymbolTable.symbolFor("error:"));
      dispatchInit = new UninitializedDispatchNode(SymbolTable.symbolFor("initializeWith:"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SClass clazz = (SClass) args[0];
      Object string = args[1];

      if (string == Nil.nilObject) {
        if (!error) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          error = true;
        }
        dispatchError.executeDispatch(frame, new Object[] {clazz, "string is null"});
      }

      Object newObj = newPrim.executeEvaluated(frame, clazz);
      return dispatchInit.executeDispatch(frame, new Object[] {newObj, string});
    }
  }
}
