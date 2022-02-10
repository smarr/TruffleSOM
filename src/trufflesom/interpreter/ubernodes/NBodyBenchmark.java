package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SBlock;


public abstract class NBodyBenchmark {
  /**
   * <pre>
   * advance: dt
   * [:body |
             body x: body x + (dt * body vx).
             body y: body y + (dt * body vy).
             body z: body z + (dt * body vz).
         ].
   * </pre>
   */
  public static final class NBodyAdvanceBlock extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchX;
    @Child private AbstractDispatchNode dispatchY;
    @Child private AbstractDispatchNode dispatchZ;
    @Child private AbstractDispatchNode dispatchX_;
    @Child private AbstractDispatchNode dispatchY_;
    @Child private AbstractDispatchNode dispatchZ_;
    @Child private AbstractDispatchNode dispatchVX;
    @Child private AbstractDispatchNode dispatchVY;
    @Child private AbstractDispatchNode dispatchVZ;

    public NBodyAdvanceBlock(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchX = new UninitializedDispatchNode(SymbolTable.symbolFor("x"));
      dispatchY = new UninitializedDispatchNode(SymbolTable.symbolFor("y"));
      dispatchZ = new UninitializedDispatchNode(SymbolTable.symbolFor("z"));
      dispatchX_ = new UninitializedDispatchNode(SymbolTable.symbolFor("x:"));
      dispatchY_ = new UninitializedDispatchNode(SymbolTable.symbolFor("y:"));
      dispatchZ_ = new UninitializedDispatchNode(SymbolTable.symbolFor("z:"));
      dispatchVX = new UninitializedDispatchNode(SymbolTable.symbolFor("vx"));
      dispatchVY = new UninitializedDispatchNode(SymbolTable.symbolFor("vy"));
      dispatchVZ = new UninitializedDispatchNode(SymbolTable.symbolFor("vz"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SBlock block = (SBlock) args[0];
      Object body = args[1];
      double dt = (Double) block.getContext().getArguments()[1];

      // common out the args array, because we know it's not captured
      Object[] bodySelf = new Object[] {body};

      double vx = (Double) dispatchVX.executeDispatch(frame, bodySelf);
      double x = (Double) dispatchX.executeDispatch(frame, bodySelf);
      dispatchX_.executeDispatch(frame, new Object[] {body, x + (dt * vx)});

      double vy = (Double) dispatchVY.executeDispatch(frame, bodySelf);
      double y = (Double) dispatchY.executeDispatch(frame, bodySelf);
      dispatchY_.executeDispatch(frame, new Object[] {body, y + (dt * vy)});

      double vz = (Double) dispatchVZ.executeDispatch(frame, bodySelf);
      double z = (Double) dispatchZ.executeDispatch(frame, bodySelf);
      return dispatchZ_.executeDispatch(frame, new Object[] {body, z + (dt * vz)});
    }
  }
}
