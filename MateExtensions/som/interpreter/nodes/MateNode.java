package som.interpreter.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

public class MateNode extends ExpressionNode {
  @CompilationFinal private final SOMNode baseLevel;  
  public MateNode(final SOMNode node) {
    super(node.getSourceSection());
    baseLevel = node;
  }
  
  public Object execute(VirtualFrame frame){return null;};
  
  public static final boolean hasReflectiveBehavior(VirtualFrame frame){
    return false;
  }
  
  public final MateNode createMetadelegation() {
    return this;
  }
  
  @Specialization(guards = "hasReflectiveBehavior(frame)")
  public Object doBaseLevel(final VirtualFrame frame, 
      @Cached("hasReflectiveBehavior(frame)") final boolean cachedReflectiveBehavior) {
    return ((ExpressionNode) baseLevel).executeGeneric(frame);
  }
  
  @Specialization(contains = "doBaseLevel", guards = "!hasReflectiveBehavior(frame)")
  public Object doMeta(final VirtualFrame frame, 
      @Cached("hasReflectiveBehavior(frame)") final boolean cachedReflectiveBehavior, 
      @Cached("createMetadelegation()") final MateNode cachedMetaDelegation) {
    Object[] arguments = null;
    return cachedMetaDelegation.execute(frame);
  }

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    return null;
  }
}