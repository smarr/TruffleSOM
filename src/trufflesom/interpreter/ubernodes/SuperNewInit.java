package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.primitives.basics.NewObjectPrim;
import trufflesom.primitives.basics.NewObjectPrimFactory;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SClass;


/**
 * <pre>
  new = ( ^super new initialize ).
 * </pre>
 */
public final class SuperNewInit extends AbstractInvokable {
  @Child private NewObjectPrim        newPrim;
  @Child private AbstractDispatchNode dispatchInit;

  public SuperNewInit(final Source source, final long sourceCoord) {
    super(new FrameDescriptor(), source, sourceCoord);
    newPrim = NewObjectPrimFactory.create(null);
    dispatchInit = new UninitializedDispatchNode(SymbolTable.symbolFor("initialize"));
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    Object[] args = frame.getArguments();
    SClass clazz = (SClass) args[0];
    Object newObj = newPrim.executeEvaluated(frame, clazz);
    return dispatchInit.executeDispatch(frame, new Object[] {newObj});
  }
}
