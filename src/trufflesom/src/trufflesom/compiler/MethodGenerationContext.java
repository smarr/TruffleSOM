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

import static trufflesom.vm.SymbolTable.symBlockSelf;
import static trufflesom.vm.SymbolTable.symFrameOnStack;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.basic.ProgramDefinitionError;
import trufflesom.bdt.inlining.Scope;
import trufflesom.bdt.inlining.ScopeBuilder;
import trufflesom.bdt.inlining.nodes.Inlinable;
import trufflesom.bdt.source.SourceCoordinate;
import trufflesom.bdt.tools.structure.StructuralProbe;
import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Internal;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.LexicalScope;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.UninitFieldIncNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.nodes.ReturnNonLocalNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode.CatchNonLocalReturnNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.supernodes.IntIncrementNode;
import trufflesom.primitives.Primitives;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


@SuppressWarnings("unchecked")
public class MethodGenerationContext
    implements ScopeBuilder<MethodGenerationContext>, Scope<LexicalScope, Method> {

  protected final ClassGenerationContext  holderGenc;
  protected final MethodGenerationContext outerGenc;
  private final boolean                   blockMethod;

  protected SSymbol signature;
  private boolean   primitive;
  private boolean   needsToCatchNonLocalReturn;

  // does directly or indirectly a non-local return
  protected boolean throwsNonLocalReturn;

  protected boolean accessesVariablesOfOuterScope;
  protected boolean accessesLocalsOfOuterScope;

  protected final LinkedHashMap<SSymbol, Argument> arguments;
  protected final LinkedHashMap<SSymbol, Local>    locals;

  private Internal frameOnStack;

  protected final LexicalScope currentScope;

  private final List<SMethod> embeddedBlockMethods;

  public final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this(holderGenc, null, false, structuralProbe);
  }

  public MethodGenerationContext(
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this(null, null, false, structuralProbe);
  }

  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc) {
    this(holderGenc, outerGenc, true, outerGenc.structuralProbe);
  }

  protected MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc, final boolean isBlockMethod,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this.holderGenc = holderGenc;
    this.outerGenc = outerGenc;
    this.blockMethod = isBlockMethod;
    this.structuralProbe = structuralProbe;

    LexicalScope outer = (outerGenc != null) ? outerGenc.getCurrentLexicalScope() : null;
    this.currentScope = new LexicalScope(outer);

    accessesVariablesOfOuterScope = false;
    throwsNonLocalReturn = false;
    needsToCatchNonLocalReturn = false;
    embeddedBlockMethods = new ArrayList<>();

    arguments = new LinkedHashMap<>();
    locals = new LinkedHashMap<>();
  }

  @Override
  public Source getSource() {
    return holderGenc.getSource();
  }

  /** Includes self, at idx == 0. */
  public Argument getArgument(final int idx) {
    int i = 0;
    for (Argument a : arguments.values()) {
      if (i == idx) {
        return a;
      }
      i += 1;
    }
    throw new IllegalArgumentException(
        "Tried to access argument " + idx + " but there are only " + arguments.size());
  }

  public void markAccessingOuterScopes() {
    MethodGenerationContext context = this;
    while (context != null) {
      context.accessesVariablesOfOuterScope = true;
      context = context.outerGenc;
    }
  }

  public void addEmbeddedBlockMethod(final SMethod method) {
    embeddedBlockMethods.add(method);
    currentScope.addEmbeddedScope(((Method) method.getInvokable()).getScope());
  }

  public LexicalScope getCurrentLexicalScope() {
    return currentScope;
  }

  public Internal getFrameOnStackMarker(final long coord) {
    if (outerGenc != null) {
      return outerGenc.getFrameOnStackMarker(coord);
    }

    if (frameOnStack == null) {
      assert needsToCatchNonLocalReturn;
      assert !locals.containsKey(symFrameOnStack);

      int index = locals.size();
      frameOnStack = new Internal(symFrameOnStack, coord, index);
      locals.put(symFrameOnStack, frameOnStack);
      currentScope.addVariable(frameOnStack);
    }
    return frameOnStack;
  }

  public void makeOuterCatchNonLocalReturn() {
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

  public final SInvokable assemble(final ExpressionNode body, final long coord) {
    currentScope.finalizeVariables(locals.size());

    if (primitive) {
      return Primitives.constructEmptyPrimitive(
          signature, holderGenc.getSource(), coord, structuralProbe);
    }

    return assembleMethod(body, coord);
  }

  protected SMethod assembleMethod(final ExpressionNode methodBody, final long coord) {
    ExpressionNode body = methodBody;
    if (needsToCatchNonLocalReturn()) {
      body = new CatchNonLocalReturnNode(
          body, getFrameOnStackMarker(coord)).initialize(body.getSourceCoordinate());
    }

    Method truffleMethod =
        new Method(getMethodIdentifier(), holderGenc.getSource(), coord,
            body, currentScope, (ExpressionNode) body.deepCopy());

    SMethod meth = new SMethod(signature, truffleMethod,
        embeddedBlockMethods.toArray(new SMethod[0]));

    if (structuralProbe != null) {
      String id = meth.getIdentifier();
      structuralProbe.recordNewMethod(symbolFor(id), meth);
    }

    // return the method - the holder field is to be set later on!
    return meth;
  }

  @Override
  public Variable[] getVariables() {
    int numVars = arguments.size() + locals.size();

    Variable[] vars = new Variable[numVars];
    int i = 0;
    for (Argument a : arguments.values()) {
      vars[i] = a;
      i += 1;
    }

    for (Local l : locals.values()) {
      vars[i] = l;
      i += 1;
    }

    return vars;
  }

  public void setVarsOnMethodScope() {
    currentScope.setVariables(getVariables());
  }

  public void markAsPrimitive() {
    primitive = true;
  }

  public void setSignature(final SSymbol sig) {
    signature = sig;
  }

  private Argument addArgument(final SSymbol arg, final long coord) {
    if ((symSelf == arg || symBlockSelf == arg) && arguments.size() > 0) {
      throw new IllegalStateException(
          "The self argument always has to be the first argument of a method");
    }

    Argument argument = new Argument(arg, arguments.size(), coord);
    arguments.put(arg, argument);

    if (structuralProbe != null) {
      structuralProbe.recordNewVariable(argument);
    }
    return argument;
  }

  public Argument addArgumentIfAbsent(final SSymbol arg, final long coord) {
    if (arguments.containsKey(arg)) {
      return arguments.get(arg);
    }

    return addArgument(arg, coord);
  }

  public boolean hasLocal(final SSymbol local) {
    return locals.containsKey(local);
  }

  public int getNumberOfLocals() {
    return locals.size();
  }

  public Local addLocal(final SSymbol local, final long coord) {
    int index = locals.size();
    Local l = new Local(local, coord, index);
    assert !locals.containsKey(local);
    locals.put(local, l);

    if (structuralProbe != null) {
      structuralProbe.recordNewVariable(l);
    }
    return l;
  }

  private Local addLocalAndUpdateScope(final SSymbol name, final long coord) {
    Local l = addLocal(name, coord);
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

  public int getContextLevel(final SSymbol varName) {
    if (locals.containsKey(varName) || arguments.containsKey(varName)) {
      return 0;
    }

    if (outerGenc != null) {
      return 1 + outerGenc.getContextLevel(varName);
    }

    return 0;
  }

  public int getContextLevel(final Variable var) {
    if (locals.containsValue(var) || arguments.containsValue(var)) {
      return 0;
    }

    if (outerGenc != null) {
      return 1 + outerGenc.getContextLevel(var);
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
        if (outerVar instanceof Local) {
          accessesLocalsOfOuterScope = true;
        }
      }
      return outerVar;
    }
    return null;
  }

  public ExpressionNode getLocalReadNode(final Variable variable, final long coord) {
    return variable.getReadNode(getContextLevel(variable), coord);
  }

  public ExpressionNode getLocalWriteNode(final Variable variable,
      final ExpressionNode valExpr, final long coord) {
    return variable.getWriteNode(getContextLevel(variable), valExpr, coord);
  }

  protected Local getLocal(final SSymbol varName) {
    if (locals.containsKey(varName)) {
      return locals.get(varName);
    }

    if (outerGenc != null) {
      Local outerLocal = outerGenc.getLocal(varName);
      if (outerLocal != null) {
        accessesVariablesOfOuterScope = true;
        accessesLocalsOfOuterScope = true;
      }
      return outerLocal;
    }
    return null;
  }

  public ReturnNonLocalNode getNonLocalReturn(final ExpressionNode expr,
      final long coord) {
    makeOuterCatchNonLocalReturn();
    return new ReturnNonLocalNode(expr, getFrameOnStackMarker(coord),
        getOuterSelfContextLevel()).initialize(coord);
  }

  private ExpressionNode getSelfRead(final long coord) {
    return getVariable(symSelf).getReadNode(getContextLevel(symSelf), coord);
  }

  public FieldReadNode getObjectFieldRead(final SSymbol fieldName,
      final long coord) {
    if (!holderGenc.hasField(fieldName)) {
      return null;
    }

    return new FieldReadNode(getSelfRead(coord),
        holderGenc.getFieldIndex(fieldName)).initialize(coord);
  }

  public FieldNode getObjectFieldWrite(final SSymbol fieldName, final ExpressionNode exp,
      final long coord) {
    if (!holderGenc.hasField(fieldName)) {
      return null;
    }

    int fieldIndex = holderGenc.getFieldIndex(fieldName);
    ExpressionNode self = getSelfRead(coord);
    if (exp instanceof IntIncrementNode
        && ((IntIncrementNode) exp).doesAccessField(fieldIndex)) {
      return new UninitFieldIncNode(self, fieldIndex, coord);
    }

    return FieldWriteNodeGen.create(fieldIndex, self, exp).initialize(coord);
  }

  protected void addLocal(final Local l, final SSymbol name) {
    assert !locals.containsKey(name);
    locals.put(name, l);
    currentScope.addVariable(l);
  }

  public void mergeIntoScope(final LexicalScope scope, final SMethod toBeInlined) {
    for (Variable v : scope.getVariables()) {
      int slotIndex = locals.size();
      Local l = v.splitToMergeIntoOuterScope(slotIndex);
      if (l != null) { // can happen for instance for the block self, which we omit
        SSymbol name = l.getQualifiedName(holderGenc.getSource());
        addLocal(l, name);
      }
    }

    SMethod[] embeddedBlocks = toBeInlined.getEmbeddedBlocks();
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

    boolean removed = embeddedBlockMethods.remove(toBeInlined);
    assert removed;
    currentScope.removeMerged(scope);
  }

  @Override
  public trufflesom.bdt.inlining.Variable<?> introduceTempForInlinedVersion(
      final Inlinable<MethodGenerationContext> blockOrVal, final long coord)
      throws ProgramDefinitionError {
    Local loopIdx;
    if (blockOrVal instanceof BlockNode) {
      Argument[] args = ((BlockNode) blockOrVal).getArguments();
      assert args.length == 2;
      loopIdx = getLocal(args[1].getQualifiedName(holderGenc.getSource()));
    } else {
      // if it is a literal, we still need a memory location for counting, so,
      // add a synthetic local
      loopIdx = addLocalAndUpdateScope(symbolFor(
          "!i" + SourceCoordinate.getLocationQualifier(
              holderGenc.getSource(), coord)),
          coord);
    }
    return loopIdx;
  }

  public boolean isFinished() {
    throw new UnsupportedOperationException(
        "You'll need the BytecodeMethodGenContext. "
            + "This method should only be used when creating bytecodes.");
  }

  public void markFinished() {
    throw new UnsupportedOperationException(
        "You'll need the BytecodeMethodGenContext. "
            + "This method should only be used when creating bytecodes.");
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

  private static String stripColonsAndSourceLocation(final String s) {
    String str = s;
    int startOfSource = str.indexOf('@');
    if (startOfSource > -1) {
      str = str.substring(0, startOfSource);
    }

    // replacing classic colons with triple colons to still indicate them without breaking
    // selector semantics based on colon counting
    return str.replace(":", "⫶");
  }

  public void setBlockSignature(final Source source, final long coord) {
    String outerMethodName =
        stripColonsAndSourceLocation(outerGenc.getSignature().getString());

    int numArgs = getNumberOfArguments();
    int line = SourceCoordinate.getLine(source, coord);
    int column = SourceCoordinate.getColumn(source, coord);
    String blockSig = "λ" + outerMethodName + "@" + line + "@" + column;

    for (int i = 1; i < numArgs; i++) {
      blockSig += ":";
    }

    setSignature(symbolFor(blockSig));
  }

  @Override
  public String toString() {
    String sig = signature == null ? "" : signature.toString();
    return "MethodGenC(" + holderGenc.getName().getString() + ">>" + sig + ")";
  }

  @Override
  public LexicalScope getOuterScopeOrNull() {
    return currentScope.getOuterScopeOrNull();
  }

  @Override
  public LexicalScope getScope(final Method method) {
    return currentScope.getScope(method);
  }

  @Override
  public String getName() {
    return getMethodIdentifier();
  }
}
