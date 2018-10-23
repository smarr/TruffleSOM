/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package trufflesom.compiler;

import static trufflesom.interpreter.SNodeFactory.createCatchNonLocalReturn;
import static trufflesom.interpreter.SNodeFactory.createFieldRead;
import static trufflesom.interpreter.SNodeFactory.createFieldWrite;
import static trufflesom.interpreter.SNodeFactory.createGlobalRead;
import static trufflesom.interpreter.SNodeFactory.createNonLocalReturn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.SourceSection;

import bd.basic.ProgramDefinitionError;
import bd.inlining.ScopeBuilder;
import bd.inlining.nodes.Inlinable;
import trufflesom.compiler.Variable.AccessNodeState;
import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Internal;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.LexicalScope;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.FieldWriteNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.Universe;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public final class MethodGenerationContext implements ScopeBuilder<MethodGenerationContext> {

  private final ClassGenerationContext  holderGenc;
  private final MethodGenerationContext outerGenc;
  private final boolean                 blockMethod;

  private SSymbol signature;
  private boolean primitive;
  private boolean needsToCatchNonLocalReturn;
  private boolean throwsNonLocalReturn;      // does directly or indirectly a non-local return

  private boolean accessesVariablesOfOuterScope;

  private final LinkedHashMap<SSymbol, Argument> arguments;
  private final LinkedHashMap<SSymbol, Local>    locals;

  private Internal           frameOnStack;
  private final LexicalScope currentScope;

  private final List<SMethod> embeddedBlockMethods;

  private final Universe universe;

  public MethodGenerationContext(final ClassGenerationContext holderGenc) {
    this(holderGenc, null, holderGenc.getUniverse(), false);
  }

  public MethodGenerationContext(final Universe universe) {
    this(null, null, universe, false);
  }

  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc) {
    this(holderGenc, outerGenc, holderGenc.getUniverse(), true);
  }

  private MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc, final Universe universe,
      final boolean isBlockMethod) {
    this.holderGenc = holderGenc;
    this.outerGenc = outerGenc;
    this.blockMethod = isBlockMethod;

    LexicalScope outer = (outerGenc != null) ? outerGenc.getCurrentLexicalScope() : null;
    this.currentScope = new LexicalScope(new FrameDescriptor(Nil.nilObject), outer);

    accessesVariablesOfOuterScope = false;
    throwsNonLocalReturn = false;
    needsToCatchNonLocalReturn = false;
    embeddedBlockMethods = new ArrayList<SMethod>();

    arguments = new LinkedHashMap<SSymbol, Argument>();
    locals = new LinkedHashMap<SSymbol, Local>();

    this.universe = universe;
  }

  public void addEmbeddedBlockMethod(final SMethod blockMethod) {
    embeddedBlockMethods.add(blockMethod);
    currentScope.addEmbeddedScope(((Method) blockMethod.getInvokable()).getScope());
  }

  public LexicalScope getCurrentLexicalScope() {
    return currentScope;
  }

  public Internal getFrameOnStackMarker() {
    if (outerGenc != null) {
      return outerGenc.getFrameOnStackMarker();
    }

    if (frameOnStack == null) {
      assert needsToCatchNonLocalReturn;

      frameOnStack = new Internal(universe.symFrameOnStack);
      frameOnStack.init(
          currentScope.getFrameDescriptor().addFrameSlot(frameOnStack, FrameSlotKind.Object));
      currentScope.addVariable(frameOnStack);
    }
    return frameOnStack;
  }

  public void makeCatchNonLocalReturn() {
    throwsNonLocalReturn = true;

    MethodGenerationContext ctx = markOuterContextsToRequireContextAndGetRootContext();
    assert ctx != null;
    ctx.needsToCatchNonLocalReturn = true;
  }

  public boolean requiresContext() {
    return throwsNonLocalReturn || accessesVariablesOfOuterScope;
  }

  private MethodGenerationContext markOuterContextsToRequireContextAndGetRootContext() {
    MethodGenerationContext ctx = outerGenc;
    while (ctx.outerGenc != null) {
      ctx.throwsNonLocalReturn = true;
      ctx = ctx.outerGenc;
    }
    return ctx;
  }

  public boolean needsToCatchNonLocalReturn() {
    // only the most outer method needs to catch
    return needsToCatchNonLocalReturn && outerGenc == null;
  }

  private String getMethodIdentifier() {
    String cls = holderGenc.getName().getString();
    if (holderGenc.isClassSide()) {
      cls += "_class";
    }
    return cls + ">>" + signature.toString();
  }

  public SInvokable assemble(ExpressionNode body, final SourceSection sourceSection) {
    if (primitive) {
      return Primitives.constructEmptyPrimitive(signature, holderGenc.getLanguage(),
          sourceSection);
    }

    if (needsToCatchNonLocalReturn()) {
      body = createCatchNonLocalReturn(body, getFrameOnStackMarker());
    }

    Method truffleMethod =
        new Method(getMethodIdentifier(), getSourceSectionForMethod(sourceSection),
            body, currentScope, (ExpressionNode) body.deepCopy(), holderGenc.getLanguage());

    SInvokable meth = Universe.newMethod(signature, truffleMethod, false,
        embeddedBlockMethods.toArray(new SMethod[0]));

    // return the method - the holder field is to be set later on!
    return meth;
  }

  public void setVarsOnMethodScope() {
    Variable[] vars = new Variable[arguments.size() + locals.size()];
    int i = 0;
    for (Argument a : arguments.values()) {
      vars[i] = a;
      i += 1;
    }

    for (Local l : locals.values()) {
      vars[i] = l;
      i += 1;
    }
    currentScope.setVariables(vars);
  }

  private SourceSection getSourceSectionForMethod(final SourceSection ssBody) {
    SourceSection ssMethod = ssBody.getSource().createSection(ssBody.getStartLine(),
        ssBody.getStartColumn(), ssBody.getCharLength());
    return ssMethod;
  }

  public void markAsPrimitive() {
    primitive = true;
  }

  public void setSignature(final SSymbol sig) {
    signature = sig;
  }

  private void addArgument(final SSymbol arg, final SourceSection source) {
    if ((universe.symSelf == arg || universe.symBlockSelf == arg) && arguments.size() > 0) {
      throw new IllegalStateException(
          "The self argument always has to be the first argument of a method");
    }

    Argument argument = new Argument(arg, arguments.size(), source);
    arguments.put(arg, argument);
  }

  public void addArgumentIfAbsent(final SSymbol arg, final SourceSection source) {
    if (arguments.containsKey(arg)) {
      return;
    }

    addArgument(arg, source);
  }

  public void addLocalIfAbsent(final SSymbol local, final SourceSection source) {
    if (locals.containsKey(local)) {
      return;
    }

    addLocal(local, source);
  }

  public Local addLocal(final SSymbol local, final SourceSection source) {
    Local l = new Local(local, source);
    l.init(currentScope.getFrameDescriptor().addFrameSlot(l));
    assert !locals.containsKey(local);
    locals.put(local, l);
    return l;
  }

  private Local addLocalAndUpdateScope(final SSymbol name, final SourceSection source)
      throws ProgramDefinitionError {
    Local l = addLocal(name, source);
    currentScope.addVariable(l);
    return l;
  }

  public boolean isBlockMethod() {
    return blockMethod;
  }

  public ClassGenerationContext getHolder() {
    return holderGenc;
  }

  private int getOuterSelfContextLevel() {
    int level = 0;
    MethodGenerationContext ctx = outerGenc;
    while (ctx != null) {
      ctx = ctx.outerGenc;
      level++;
    }
    return level;
  }

  private int getContextLevel(final SSymbol varName) {
    if (locals.containsKey(varName) || arguments.containsKey(varName)) {
      return 0;
    }

    if (outerGenc != null) {
      return 1 + outerGenc.getContextLevel(varName);
    }

    return 0;
  }

  public Local getEmbeddedLocal(final SSymbol embeddedName) {
    return locals.get(embeddedName);
  }

  protected Variable getVariable(final SSymbol varName) {
    if (locals.containsKey(varName)) {
      return locals.get(varName);
    }

    if (arguments.containsKey(varName)) {
      return arguments.get(varName);
    }

    if (outerGenc != null) {
      Variable outerVar = outerGenc.getVariable(varName);
      if (outerVar != null) {
        accessesVariablesOfOuterScope = true;
      }
      return outerVar;
    }
    return null;
  }

  public ExpressionNode getSuperReadNode(final SourceSection source) {
    Variable self = getVariable(universe.symSelf);
    return self.getSuperReadNode(getOuterSelfContextLevel(),
        new AccessNodeState(holderGenc.getName(), holderGenc.isClassSide()), source);
  }

  public ExpressionNode getLocalReadNode(final SSymbol variableName,
      final SourceSection source) {
    Variable variable = getVariable(variableName);
    return variable.getReadNode(getContextLevel(variableName), source);
  }

  public ExpressionNode getLocalWriteNode(final SSymbol variableName,
      final ExpressionNode valExpr, final SourceSection source) {
    Local variable = getLocal(variableName);
    return variable.getWriteNode(getContextLevel(variableName), valExpr, source);
  }

  protected Local getLocal(final SSymbol varName) {
    if (locals.containsKey(varName)) {
      return locals.get(varName);
    }

    if (outerGenc != null) {
      Local outerLocal = outerGenc.getLocal(varName);
      if (outerLocal != null) {
        accessesVariablesOfOuterScope = true;
      }
      return outerLocal;
    }
    return null;
  }

  public ReturnNonLocalNode getNonLocalReturn(final ExpressionNode expr,
      final SourceSection source) {
    makeCatchNonLocalReturn();
    return createNonLocalReturn(expr, getFrameOnStackMarker(),
        getOuterSelfContextLevel(), source, holderGenc.getUniverse());
  }

  private ExpressionNode getSelfRead(final SourceSection source) {
    return getVariable(universe.symSelf).getReadNode(getContextLevel(universe.symSelf),
        source);
  }

  public FieldReadNode getObjectFieldRead(final SSymbol fieldName,
      final SourceSection source) {
    if (!holderGenc.hasField(fieldName)) {
      return null;
    }
    return createFieldRead(getSelfRead(source),
        holderGenc.getFieldIndex(fieldName), source);
  }

  public GlobalNode getGlobalRead(final SSymbol varName,
      final Universe universe, final SourceSection source) {
    return createGlobalRead(varName, universe, source);
  }

  public FieldWriteNode getObjectFieldWrite(final SSymbol fieldName,
      final ExpressionNode exp, final Universe universe,
      final SourceSection source) {
    if (!holderGenc.hasField(fieldName)) {
      return null;
    }

    return createFieldWrite(getSelfRead(source), exp,
        holderGenc.getFieldIndex(fieldName), source);
  }

  public void mergeIntoScope(final LexicalScope scope, final SMethod outer) {
    for (Variable v : scope.getVariables()) {
      Local l = v.splitToMergeIntoOuterScope(universe, currentScope.getFrameDescriptor());
      if (l != null) { // can happen for instance for the block self, which we omit
        SSymbol name = l.getQualifiedName(universe);
        assert !locals.containsKey(name);
        locals.put(name, l);
        currentScope.addVariable(l);
      }
    }

    SMethod[] embeddedBlocks = outer.getEmbeddedBlocks();
    LexicalScope[] embeddedScopes = scope.getEmbeddedScopes();

    assert ((embeddedBlocks == null || embeddedBlocks.length == 0) &&
        (embeddedScopes == null || embeddedScopes.length == 0)) ||
        embeddedBlocks.length == embeddedScopes.length;

    if (embeddedScopes != null) {
      for (LexicalScope e : embeddedScopes) {
        currentScope.addEmbeddedScope(e.split(currentScope));
      }

      for (SMethod m : embeddedBlocks) {
        embeddedBlockMethods.add(m);
      }
    }

    boolean removed = embeddedBlockMethods.remove(outer);
    assert removed;
    currentScope.removeMerged(scope);
  }

  @Override
  public bd.inlining.Variable<?> introduceTempForInlinedVersion(
      final Inlinable<MethodGenerationContext> blockOrVal, final SourceSection source)
      throws ProgramDefinitionError {
    Local loopIdx;
    if (blockOrVal instanceof BlockNode) {
      Argument[] args = ((BlockNode) blockOrVal).getArguments();
      assert args.length == 2;
      loopIdx = getLocal(args[1].getQualifiedName(universe));
    } else {
      // if it is a literal, we still need a memory location for counting, so,
      // add a synthetic local
      loopIdx = addLocalAndUpdateScope(
          universe.symbolFor("!i" + Universe.getLocationQualifier(source)), source);
    }
    return loopIdx;
  }

  /**
   * @return number of explicit arguments,
   *         i.e., excluding the implicit 'self' argument
   */
  public int getNumberOfArguments() {
    return arguments.size();
  }

  public SSymbol getSignature() {
    return signature;
  }

  @Override
  public String toString() {
    return "MethodGenC(" + holderGenc.getName().getString() + ">>" + signature.toString()
        + ")";
  }
}
