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

import static som.interpreter.SNodeFactory.createCatchNonLocalReturn;
import static som.interpreter.SNodeFactory.createFieldRead;
import static som.interpreter.SNodeFactory.createFieldWrite;
import static som.interpreter.SNodeFactory.createGlobalRead;
import static som.interpreter.SNodeFactory.createNonLocalReturn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.Variable.Argument;
import trufflesom.compiler.Variable.Local;
import trufflesom.interpreter.LexicalScope;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.FieldNode.FieldReadNode;
import trufflesom.interpreter.nodes.FieldNode.FieldWriteNode;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.ReturnNonLocalNode;
import trufflesom.primitives.Primitives;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public final class MethodGenerationContext {

  private final ClassGenerationContext  holderGenc;
  private final MethodGenerationContext outerGenc;
  private final boolean                 blockMethod;

  private SSymbol signature;
  private boolean primitive;
  private boolean needsToCatchNonLocalReturn;
  private boolean throwsNonLocalReturn;      // does directly or indirectly a non-local return

  private boolean accessesVariablesOfOuterScope;

  private final LinkedHashMap<String, Argument> arguments =
      new LinkedHashMap<String, Argument>();
  private final LinkedHashMap<String, Local>    locals    = new LinkedHashMap<String, Local>();

  private FrameSlot          frameOnStackSlot;
  private final LexicalScope currentScope;

  private final List<SMethod> embeddedBlockMethods;

  public MethodGenerationContext(final ClassGenerationContext holderGenc) {
    this(holderGenc, null, false);
  }

  public MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc) {
    this(holderGenc, outerGenc, true);
  }

  private MethodGenerationContext(final ClassGenerationContext holderGenc,
      final MethodGenerationContext outerGenc, final boolean isBlockMethod) {
    this.holderGenc = holderGenc;
    this.outerGenc = outerGenc;
    this.blockMethod = isBlockMethod;

    LexicalScope outer = (outerGenc != null) ? outerGenc.getCurrentLexicalScope() : null;
    this.currentScope = new LexicalScope(new FrameDescriptor(), outer);

    accessesVariablesOfOuterScope = false;
    throwsNonLocalReturn = false;
    needsToCatchNonLocalReturn = false;
    embeddedBlockMethods = new ArrayList<SMethod>();
  }

  public void addEmbeddedBlockMethod(final SMethod blockMethod) {
    embeddedBlockMethods.add(blockMethod);
  }

  public LexicalScope getCurrentLexicalScope() {
    return currentScope;
  }

  // Name for the frameOnStack slot,
  // starting with ! to make it a name that's not possible in Smalltalk
  private static final String frameOnStackSlotName = "!frameOnStack";

  public FrameSlot getFrameOnStackMarkerSlot() {
    if (outerGenc != null) {
      return outerGenc.getFrameOnStackMarkerSlot();
    }

    if (frameOnStackSlot == null) {
      frameOnStackSlot = currentScope.getFrameDescriptor().addFrameSlot(frameOnStackSlotName);
    }
    return frameOnStackSlot;
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

  private void separateVariables(final Collection<? extends Variable> variables,
      final ArrayList<Variable> onlyLocalAccess,
      final ArrayList<Variable> nonLocalAccess) {
    for (Variable l : variables) {
      if (l.isAccessedOutOfContext()) {
        nonLocalAccess.add(l);
      } else {
        onlyLocalAccess.add(l);
      }
    }
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

    ArrayList<Variable> onlyLocalAccess = new ArrayList<>(arguments.size() + locals.size());
    ArrayList<Variable> nonLocalAccess = new ArrayList<>(arguments.size() + locals.size());
    separateVariables(arguments.values(), onlyLocalAccess, nonLocalAccess);
    separateVariables(locals.values(), onlyLocalAccess, nonLocalAccess);

    if (needsToCatchNonLocalReturn()) {
      body = createCatchNonLocalReturn(body, getFrameOnStackMarkerSlot());
    }

    Method truffleMethod =
        new Method(getMethodIdentifier(), getSourceSectionForMethod(sourceSection),
            body, currentScope, (ExpressionNode) body.deepCopy(), holderGenc.getLanguage());

    SInvokable meth = Universe.newMethod(signature, truffleMethod, false,
        embeddedBlockMethods.toArray(new SMethod[0]));

    // return the method - the holder field is to be set later on!
    return meth;
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

  private void addArgument(final String arg) {
    if (("self".equals(arg) || "$blockSelf".equals(arg)) && arguments.size() > 0) {
      throw new IllegalStateException(
          "The self argument always has to be the first argument of a method");
    }

    Argument argument = new Argument(arg, arguments.size());
    arguments.put(arg, argument);
  }

  public void addArgumentIfAbsent(final String arg) {
    if (arguments.containsKey(arg)) {
      return;
    }

    addArgument(arg);
  }

  public void addLocalIfAbsent(final String local) {
    if (locals.containsKey(local)) {
      return;
    }

    addLocal(local);
  }

  public Local addLocal(final String local) {
    Local l = new Local(local, currentScope.getFrameDescriptor().addFrameSlot(local));
    assert !locals.containsKey(local);
    locals.put(local, l);
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

  private int getContextLevel(final String varName) {
    if (locals.containsKey(varName) || arguments.containsKey(varName)) {
      return 0;
    }

    if (outerGenc != null) {
      return 1 + outerGenc.getContextLevel(varName);
    }

    return 0;
  }

  public Local getEmbeddedLocal(final String embeddedName) {
    return locals.get(embeddedName);
  }

  protected Variable getVariable(final String varName) {
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
    Variable self = getVariable("self");
    return self.getSuperReadNode(getOuterSelfContextLevel(),
        holderGenc.getName(), holderGenc.isClassSide(), source);
  }

  public ExpressionNode getLocalReadNode(final String variableName,
      final SourceSection source) {
    Variable variable = getVariable(variableName);
    return variable.getReadNode(getContextLevel(variableName), source);
  }

  public ExpressionNode getLocalWriteNode(final String variableName,
      final ExpressionNode valExpr, final SourceSection source) {
    Local variable = getLocal(variableName);
    return variable.getWriteNode(getContextLevel(variableName), valExpr, source);
  }

  protected Local getLocal(final String varName) {
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
    return createNonLocalReturn(expr, getFrameOnStackMarkerSlot(),
        getOuterSelfContextLevel(), source, holderGenc.getUniverse());
  }

  private ExpressionNode getSelfRead(final SourceSection source) {
    return getVariable("self").getReadNode(getContextLevel("self"), source);
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
