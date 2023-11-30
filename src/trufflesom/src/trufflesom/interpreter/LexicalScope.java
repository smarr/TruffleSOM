package trufflesom.interpreter;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameDescriptor.Builder;
import com.oracle.truffle.api.frame.FrameSlotKind;

import trufflesom.bdt.inlining.Scope;
import trufflesom.compiler.Variable;
import trufflesom.compiler.Variable.Local;
import trufflesom.vm.constants.Nil;


public final class LexicalScope implements Scope<LexicalScope, Method> {

  private final LexicalScope outerScope;

  @CompilationFinal private Method method;

  @CompilationFinal private FrameDescriptor frameDescriptor;

  @CompilationFinal(dimensions = 1) private Variable[]     variables;
  @CompilationFinal(dimensions = 1) private LexicalScope[] embeddedScopes;

  private int numberOfLocals;

  public LexicalScope(final LexicalScope outerScope) {
    this.outerScope = outerScope;
    this.numberOfLocals = -1;
  }

  public FrameDescriptor getFrameDescriptor() {
    assert frameDescriptor != null : "Should not be accessed before initialized";
    return frameDescriptor;
  }

  @Override
  public LexicalScope getOuterScopeOrNull() {
    return outerScope;
  }

  public Local getLocal(final int slotIndex, final int contextIdx) {
    if (contextIdx == 0) {
      return getLocal(slotIndex);
    } else {
      return outerScope.getLocal(slotIndex, contextIdx - 1);
    }
  }

  public Local getLocal(final int slotIndex) {
    for (Variable v : variables) {
      if (v instanceof Local) {
        if (((Local) v).getIndex() == slotIndex) {
          return (Local) v;
        }
      }
    }
    return null;
  }

  public boolean isBlock() {
    return outerScope != null;
  }

  public LexicalScope getOuterScope() {
    assert outerScope != null;
    return outerScope;
  }

  public void finalizeVariables(final int numLocals) {
    Builder builder = FrameDescriptor.newBuilder(numLocals);
    builder.defaultValue(Nil.nilObject);
    builder.addSlots(numLocals, FrameSlotKind.Illegal);
    frameDescriptor = builder.build();

    assert frameDescriptor != null;

    numberOfLocals = numLocals;

    if (variables == null) {
      return;
    }

    for (Variable v : variables) {
      if (v instanceof Local) {
        Local l = (Local) v;
        l.init(frameDescriptor);
      }
    }
  }

  public void setVariables(final Variable[] variables) {
    assert variables != null : "variables are expected to be != null once set";
    assert this.variables == null;
    this.variables = variables;
  }

  public void addVariable(final Variable var) {
    int length = variables.length;
    variables = Arrays.copyOf(variables, length + 1);
    variables[length] = var;
  }

  public LexicalScope[] getEmbeddedScopes() {
    return embeddedScopes;
  }

  public void addEmbeddedScope(final LexicalScope embeddedScope) {
    assert embeddedScope.outerScope == this;
    int length;
    if (embeddedScopes == null) {
      length = 0;
      embeddedScopes = new LexicalScope[length + 1];
    } else {
      length = embeddedScopes.length;
      embeddedScopes = Arrays.copyOf(embeddedScopes, length + 1);
    }
    embeddedScopes[length] = embeddedScope;
  }

  /**
   * The given scope was just merged into this one. Now, we need to
   * remove it from the embedded scopes.
   */
  public void removeMerged(final LexicalScope scope) {
    LexicalScope[] remainingScopes = new LexicalScope[embeddedScopes.length - 1];

    int i = 0;
    for (LexicalScope s : embeddedScopes) {
      if (s != scope) {
        remainingScopes[i] = s;
        i += 1;
      }
    }

    embeddedScopes = remainingScopes;
  }

  public void propagateLoopCountThroughoutLexicalScope(final long count) {
    if (outerScope != null) {
      outerScope.method.propagateLoopCountThroughoutLexicalScope(count);
    }
  }

  public Method getMethod() {
    return method;
  }

  public void setMethod(final Method method) {
    CompilerAsserts.neverPartOfCompilation("LexicalContext.sOM()");
    // might be reset when doing inlining/embedded, but should always
    // refer to the same method
    assert this.method == null ||
        this.method.getSourceSection().equals(method.getSourceSection());
    this.method = method;
  }

  private LexicalScope constructSplitScope(final LexicalScope newOuter) {
    Variable[] newVars = new Variable[variables.length];
    for (int i = 0; i < variables.length; i += 1) {
      newVars[i] = variables[i].split();
    }

    LexicalScope split = new LexicalScope(newOuter);

    if (embeddedScopes != null) {
      for (LexicalScope s : embeddedScopes) {
        split.addEmbeddedScope(s.constructSplitScope(split));
      }
    }
    split.setVariables(newVars);
    split.setMethod(method);
    split.finalizeVariables(numberOfLocals);

    return split;
  }

  /** Split lexical scope. */
  public LexicalScope split() {
    return constructSplitScope(outerScope);
  }

  /**
   * Split lexical scope to adapt to new outer lexical scope.
   * One of the outer scopes was inlined into its parent,
   * or simply split itself.
   */
  public LexicalScope split(final LexicalScope newOuter) {
    return constructSplitScope(newOuter);
  }

  @Override
  public String getName() {
    return method.getName();
  }

  @Override
  public String toString() {
    return "LexScp[" + frameDescriptor.toString() + "]";
  }

  @Override
  @SuppressWarnings("unchecked")
  public Variable[] getVariables() {
    return variables;
  }

  @Override
  public LexicalScope getScope(final Method m) {
    if (m.equals(this.method)) {
      return this;
    }

    if (embeddedScopes == null) {
      return null;
    }

    for (LexicalScope scope : embeddedScopes) {
      LexicalScope result = scope.getScope(m);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
