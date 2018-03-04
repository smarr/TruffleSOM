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

package som.primitives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.PrimitiveLoader;
import bd.primitives.Specializer;
import som.compiler.MethodGenerationContext;
import som.interpreter.Primitive;
import som.interpreter.SomLanguage;
import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.specialized.AndMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IntDownToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.NotMessageNodeFactory;
import som.interpreter.nodes.specialized.OrMessageNodeFactory;
import som.primitives.arithmetic.AdditionPrimFactory;
import som.primitives.arithmetic.BitXorPrimFactory;
import som.primitives.arithmetic.CosPrimFactory;
import som.primitives.arithmetic.DividePrimFactory;
import som.primitives.arithmetic.DoubleDivPrimFactory;
import som.primitives.arithmetic.GreaterThanPrimFactory;
import som.primitives.arithmetic.LessThanOrEqualPrimFactory;
import som.primitives.arithmetic.LessThanPrimFactory;
import som.primitives.arithmetic.LogicAndPrimFactory;
import som.primitives.arithmetic.ModuloPrimFactory;
import som.primitives.arithmetic.MultiplicationPrimFactory;
import som.primitives.arithmetic.RemainderPrimFactory;
import som.primitives.arithmetic.SinPrimFactory;
import som.primitives.arithmetic.SqrtPrimFactory;
import som.primitives.arithmetic.SubtractionPrimFactory;
import som.primitives.arrays.AtPrimFactory;
import som.primitives.arrays.AtPutPrimFactory;
import som.primitives.arrays.CopyPrimFactory;
import som.primitives.arrays.DoIndexesPrimFactory;
import som.primitives.arrays.DoPrimFactory;
import som.primitives.arrays.NewPrimFactory;
import som.primitives.arrays.PutAllNodeFactory;
import som.primitives.basics.AsStringPrimFactory;
import som.primitives.basics.BlockPrimsFactory;
import som.primitives.basics.DoublePrimsFactory;
import som.primitives.basics.EqualsEqualsPrimFactory;
import som.primitives.basics.EqualsPrimFactory;
import som.primitives.basics.HashPrimFactory;
import som.primitives.basics.IntegerPrimsFactory;
import som.primitives.basics.LengthPrimFactory;
import som.primitives.basics.NewObjectPrimFactory;
import som.primitives.basics.StringPrimsFactory;
import som.primitives.basics.SystemPrimsFactory;
import som.primitives.basics.UnequalsPrimFactory;
import som.primitives.reflection.ClassPrimsFactory;
import som.primitives.reflection.GlobalPrimFactory;
import som.primitives.reflection.HasGlobalPrimFactory;
import som.primitives.reflection.MethodPrimsFactory;
import som.primitives.reflection.ObjectPrimsFactory;
import som.primitives.reflection.ObjectSizePrimFactory;
import som.primitives.reflection.PerformInSuperclassPrimFactory;
import som.primitives.reflection.PerformPrimFactory;
import som.primitives.reflection.PerformWithArgumentsInSuperclassPrimFactory;
import som.primitives.reflection.PerformWithArgumentsPrimFactory;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SSymbol;


public final class Primitives extends PrimitiveLoader<Universe, ExpressionNode, SSymbol> {

  /** Primitives for class and method name. */
  private final HashMap<SSymbol, HashMap<SSymbol, Specializer<Universe, ExpressionNode, SSymbol>>> primitives;

  public static SInvokable constructEmptyPrimitive(final SSymbol signature,
      final SomLanguage lang, final SourceSection sourceSection) {
    CompilerAsserts.neverPartOfCompilation();
    MethodGenerationContext mgen = new MethodGenerationContext(lang.getUniverse());

    ExpressionNode primNode = EmptyPrim.create(new LocalArgumentReadNode(true, 0));
    Primitive primMethodNode =
        new Primitive(signature.getString(), sourceSection, primNode,
            mgen.getCurrentLexicalScope().getFrameDescriptor(),
            (ExpressionNode) primNode.deepCopy(), lang);
    SInvokable prim = Universe.newMethod(signature, primMethodNode, true, new SMethod[0]);
    return prim;
  }

  public Primitives(final Universe universe) {
    super(universe, universe);
    this.primitives = new HashMap<>();
    initialize();
  }

  public void loadPrimitives(final SClass clazz, final boolean displayWarning) {
    HashMap<SSymbol, Specializer<Universe, ExpressionNode, SSymbol>> prims =
        primitives.get(clazz.getName());
    if (prims == null) {
      if (displayWarning) {
        Universe.errorPrintln("No primitives found for " + clazz.getName().getString());
      }
      return;
    }

    for (Entry<SSymbol, Specializer<Universe, ExpressionNode, SSymbol>> e : prims.entrySet()) {
      SClass target;
      if (e.getValue().classSide()) {
        target = clazz.getSOMClass(context);
      } else {
        target = clazz;
      }

      SInvokable ivk = target.lookupInvokable(e.getKey());
      assert ivk != null : "Lookup of " + e.getKey().toString() + " failed in "
          + target.getName().getString() + ". Can't install a primitive for it.";
      SInvokable prim = constructPrimitive(e.getKey(), context.getLanguage(), e.getValue());
      target.addInstanceInvokable(prim);
    }
  }

  @Override
  protected void registerPrimitive(final bd.primitives.Primitive prim,
      final Specializer<Universe, ExpressionNode, SSymbol> specializer) {
    String className = prim.className();
    String primName = prim.primitive();

    if (!("".equals(primName)) && !("".equals(className))) {
      SSymbol clazz = context.symbolFor(className);
      SSymbol signature = context.symbolFor(primName);
      HashMap<SSymbol, Specializer<Universe, ExpressionNode, SSymbol>> primsForClass =
          primitives.computeIfAbsent(clazz, s -> new HashMap<>());
      assert !primsForClass.containsKey(signature) : className
          + " already has a primitive " + primName + " registered.";
      primsForClass.put(signature, specializer);
    } else {
      assert "".equals(primName) && "".equals(
          className) : "If either primitive() or className() is set on @Primitive, both should be set";
    }
  }

  private static SInvokable constructPrimitive(final SSymbol signature,
      final SomLanguage lang,
      final Specializer<Universe, ExpressionNode, SSymbol> specializer) {
    CompilerAsserts.neverPartOfCompilation("This is only executed during bootstrapping.");
    final int numArgs = signature.getNumberOfSignatureArguments();

    Source s = SomLanguage.getSyntheticSource("primitive", specializer.getName());
    SourceSection source = s.createSection(1);

    MethodGenerationContext mgen = new MethodGenerationContext(lang.getUniverse());
    ExpressionNode[] args = new ExpressionNode[numArgs];
    for (int i = 0; i < numArgs; i++) {
      args[i] = new LocalArgumentReadNode(true, i).initialize(source);
    }

    ExpressionNode primNode = specializer.create(null, args, source, false);

    Primitive primMethodNode = new Primitive(signature.getString(), source, primNode,
        mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionNode) primNode.deepCopy(), lang);
    return Universe.newMethod(signature, primMethodNode, true, new SMethod[0]);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected List<NodeFactory<? extends ExpressionNode>> getFactories() {
    List<NodeFactory<? extends ExpressionNode>> allFactories = new ArrayList<>();

    allFactories.addAll(BlockPrimsFactory.getFactories());
    allFactories.addAll(DoublePrimsFactory.getFactories());
    allFactories.addAll(IntegerPrimsFactory.getFactories());
    allFactories.addAll(StringPrimsFactory.getFactories());
    allFactories.addAll((List) SystemPrimsFactory.getFactories());
    allFactories.addAll(ClassPrimsFactory.getFactories());
    allFactories.addAll(MethodPrimsFactory.getFactories());
    allFactories.addAll((List) ObjectPrimsFactory.getFactories());

    allFactories.add(AdditionPrimFactory.getInstance());
    allFactories.add(BitXorPrimFactory.getInstance());
    allFactories.add(CosPrimFactory.getInstance());
    allFactories.add(DividePrimFactory.getInstance());
    allFactories.add(DoubleDivPrimFactory.getInstance());
    allFactories.add(GreaterThanPrimFactory.getInstance());
    allFactories.add(LessThanOrEqualPrimFactory.getInstance());
    allFactories.add(LessThanPrimFactory.getInstance());
    allFactories.add(LogicAndPrimFactory.getInstance());
    allFactories.add(ModuloPrimFactory.getInstance());
    allFactories.add(MultiplicationPrimFactory.getInstance());
    allFactories.add(RemainderPrimFactory.getInstance());
    allFactories.add(SinPrimFactory.getInstance());
    allFactories.add(SqrtPrimFactory.getInstance());
    allFactories.add(SubtractionPrimFactory.getInstance());

    allFactories.add(AtPrimFactory.getInstance());
    allFactories.add(AtPutPrimFactory.getInstance());
    allFactories.add(CopyPrimFactory.getInstance());
    allFactories.add(DoIndexesPrimFactory.getInstance());
    allFactories.add(DoPrimFactory.getInstance());
    allFactories.add(NewPrimFactory.getInstance());
    allFactories.add(PutAllNodeFactory.getInstance());

    allFactories.add(AsStringPrimFactory.getInstance());
    allFactories.add(EqualsEqualsPrimFactory.getInstance());
    allFactories.add(EqualsPrimFactory.getInstance());
    allFactories.add(HashPrimFactory.getInstance());
    allFactories.add(LengthPrimFactory.getInstance());
    allFactories.add(NewObjectPrimFactory.getInstance());
    allFactories.add(UnequalsPrimFactory.getInstance());

    allFactories.add(AndMessageNodeFactory.getInstance());
    allFactories.add(IntToDoMessageNodeFactory.getInstance());
    allFactories.add(IntToByDoMessageNodeFactory.getInstance());
    allFactories.add(IntDownToDoMessageNodeFactory.getInstance());
    allFactories.add(OrMessageNodeFactory.getInstance());
    allFactories.add(IfTrueIfFalseMessageNodeFactory.getInstance());
    allFactories.add(NotMessageNodeFactory.getInstance());

    allFactories.add(GlobalPrimFactory.getInstance());
    allFactories.add(HasGlobalPrimFactory.getInstance());
    allFactories.add(ObjectSizePrimFactory.getInstance());
    allFactories.add(PerformInSuperclassPrimFactory.getInstance());
    allFactories.add(PerformPrimFactory.getInstance());
    allFactories.add(PerformWithArgumentsInSuperclassPrimFactory.getInstance());
    allFactories.add(PerformWithArgumentsPrimFactory.getInstance());

    return allFactories;
  }
}
