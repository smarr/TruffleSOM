package trufflesom.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.inlining.Scope;
import trufflesom.compiler.Variable.Internal;
import trufflesom.interpreter.nodes.ExpressionNode;


public final class MethodWithCatch extends Method {

  private final Internal onStackMarkerVar;

  private final int onStackMarkerIndex;

  @CompilationFinal private boolean nonLocalReturnHandler;
  @CompilationFinal private boolean doCatch;
  @CompilationFinal private boolean doPropagate;

  public MethodWithCatch(final String name, final Source source, final long sourceCoord,
      final ExpressionNode expressions, final LexicalScope currentLexicalScope,
      final ExpressionNode uninitialized, final Internal onStackMarker) {
    super(name, source, sourceCoord, expressions, currentLexicalScope, uninitialized);
    this.onStackMarkerVar = onStackMarker;
    this.onStackMarkerIndex = onStackMarker.getIndex();
  }

  @Override
  public Object execute(final VirtualFrame frame) {
    FrameOnStackMarker marker = new FrameOnStackMarker();
    frame.setObject(onStackMarkerIndex, marker);

    try {
      return body.executeGeneric(frame);
    } catch (ReturnException e) {
      if (!nonLocalReturnHandler) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        nonLocalReturnHandler = true;
      }
      if (!e.reachedTarget(marker)) {
        if (!doPropagate) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          doPropagate = true;
        }
        throw e;
      } else {
        if (!doCatch) {
          CompilerDirectives.transferToInterpreterAndInvalidate();
          doCatch = true;
        }
        return e.result();
      }
    } finally {
      marker.frameNoLongerOnStack();
    }
  }

  @Override
  protected Method createAdapted(final ExpressionNode adaptedBody,
      final Scope<?, ?> newScope, final LexicalScope adaptedScope,
      final ExpressionNode uninit) {
    Internal splitOnStack = null;
    for (var v : newScope.getVariables()) {
      if (v.equals(onStackMarkerVar)) {
        splitOnStack = (Internal) v;
      }
    }

    return new MethodWithCatch(name, source, sourceCoord, adaptedBody, adaptedScope, uninit,
        onStackMarkerVar);
  }
}
