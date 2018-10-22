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

package trufflesom.primitives;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.compiler.MethodGenerationContext;
import trufflesom.interpreter.Primitive;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.nary.EagerlySpecializableNode;
import trufflesom.interpreter.nodes.specialized.AndMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntDownToDoMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.NotMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.OrMessageNodeFactory;
import trufflesom.primitives.arithmetic.AdditionPrimFactory;
import trufflesom.primitives.arithmetic.BitXorPrimFactory;
import trufflesom.primitives.arithmetic.CosPrimFactory;
import trufflesom.primitives.arithmetic.DividePrimFactory;
import trufflesom.primitives.arithmetic.DoubleDivPrimFactory;
import trufflesom.primitives.arithmetic.GreaterThanPrimFactory;
import trufflesom.primitives.arithmetic.LessThanOrEqualPrimFactory;
import trufflesom.primitives.arithmetic.LessThanPrimFactory;
import trufflesom.primitives.arithmetic.LogicAndPrimFactory;
import trufflesom.primitives.arithmetic.ModuloPrimFactory;
import trufflesom.primitives.arithmetic.MultiplicationPrimFactory;
import trufflesom.primitives.arithmetic.RemainderPrimFactory;
import trufflesom.primitives.arithmetic.SinPrimFactory;
import trufflesom.primitives.arithmetic.SqrtPrimFactory;
import trufflesom.primitives.arithmetic.SubtractionPrimFactory;
import trufflesom.primitives.arrays.AtPrimFactory;
import trufflesom.primitives.arrays.AtPutPrimFactory;
import trufflesom.primitives.arrays.CopyPrimFactory;
import trufflesom.primitives.arrays.DoIndexesPrimFactory;
import trufflesom.primitives.arrays.DoPrimFactory;
import trufflesom.primitives.arrays.NewPrimFactory;
import trufflesom.primitives.arrays.PutAllNodeFactory;
import trufflesom.primitives.basics.AsStringPrimFactory;
import trufflesom.primitives.basics.BlockPrimsFactory;
import trufflesom.primitives.basics.DoublePrimsFactory;
import trufflesom.primitives.basics.EqualsEqualsPrimFactory;
import trufflesom.primitives.basics.EqualsPrimFactory;
import trufflesom.primitives.basics.HashPrimFactory;
import trufflesom.primitives.basics.IntegerPrimsFactory;
import trufflesom.primitives.basics.LengthPrimFactory;
import trufflesom.primitives.basics.NewObjectPrimFactory;
import trufflesom.primitives.basics.StringPrimsFactory;
import trufflesom.primitives.basics.SystemPrimsFactory;
import trufflesom.primitives.basics.UnequalsPrimFactory;
import trufflesom.primitives.reflection.ClassPrimsFactory;
import trufflesom.primitives.reflection.GlobalPrimFactory;
import trufflesom.primitives.reflection.HasGlobalPrimFactory;
import trufflesom.primitives.reflection.MethodPrimsFactory;
import trufflesom.primitives.reflection.ObjectPrimsFactory;
import trufflesom.primitives.reflection.ObjectSizePrimFactory;
import trufflesom.primitives.reflection.PerformInSuperclassPrimFactory;
import trufflesom.primitives.reflection.PerformPrimFactory;
import trufflesom.primitives.reflection.PerformWithArgumentsInSuperclassPrimFactory;
import trufflesom.primitives.reflection.PerformWithArgumentsPrimFactory;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SMethod;
import trufflesom.vmobjects.SSymbol;


public final class Primitives {

  public static SInvokable constructEmptyPrimitive(final SSymbol signature,
      final SomLanguage lang, final SourceSection sourceSection) {
    CompilerAsserts.neverPartOfCompilation();
    MethodGenerationContext mgen = new MethodGenerationContext(null);

    ExpressionNode primNode = EmptyPrim.create(new LocalArgumentReadNode(0, null));
    Primitive primMethodNode =
        new Primitive(signature.getString(), sourceSection, primNode,
            mgen.getCurrentLexicalScope().getFrameDescriptor(),
            (ExpressionNode) primNode.deepCopy(), lang);
    SInvokable prim = Universe.newMethod(signature, primMethodNode, true, new SMethod[0]);
    return prim;
  }

  protected final Universe universe;

  /** Primitives for class and method name. */
  private final HashMap<SSymbol, HashMap<SSymbol, Specializer<? extends ExpressionNode>>> primitives;

  /** Primitives for selector. */
  private final HashMap<SSymbol, Specializer<? extends ExpressionNode>> eagerPrimitives;

  public Primitives(final Universe universe) {
    this.universe = universe;
    this.primitives = new HashMap<>();
    this.eagerPrimitives = new HashMap<>();
    initialize(universe.getLanguage());
  }

  @SuppressWarnings("unchecked")
  public Specializer<EagerlySpecializableNode> getParserSpecializer(final SSymbol selector,
      final ExpressionNode[] argNodes) {
    Specializer<? extends ExpressionNode> specializer = eagerPrimitives.get(selector);
    if (specializer != null && specializer.inParser() && specializer.matches(null, argNodes)) {
      return (Specializer<EagerlySpecializableNode>) specializer;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Specializer<EagerlySpecializableNode> getEagerSpecializer(final SSymbol selector,
      final Object[] arguments, final ExpressionNode[] argumentNodes) {
    Specializer<? extends ExpressionNode> specializer = eagerPrimitives.get(selector);
    if (specializer != null && specializer.matches(arguments, argumentNodes)) {
      return (Specializer<EagerlySpecializableNode>) specializer;
    }
    return null;
  }

  public void loadPrimitives(final SClass clazz, final boolean displayWarning) {
    HashMap<SSymbol, Specializer<? extends ExpressionNode>> prims =
        primitives.get(clazz.getName());
    if (prims == null) {
      if (displayWarning) {
        Universe.errorPrintln("No primitives found for " + clazz.getName().getString());
      }
      return;
    }

    for (Entry<SSymbol, Specializer<? extends ExpressionNode>> e : prims.entrySet()) {
      SClass target;
      if (e.getValue().classSide()) {
        target = clazz.getSOMClass(universe);
      } else {
        target = clazz;
      }

      SInvokable ivk = target.lookupInvokable(e.getKey());
      assert ivk != null : "Lookup of " + e.getKey().toString() + " failed in "
          + target.getName().getString() + ". Can't install a primitive for it.";
      SInvokable prim = constructPrimitive(e.getKey(), ivk.getInvokable().getSourceSection(),
          universe.getLanguage(), e.getValue());

      target.addInstanceInvokable(prim);
    }
  }

  /**
   * Setup the lookup data structures for primitive registration as well as eager primitive
   * replacement.
   */
  private void initialize(final SomLanguage lang) {
    List<NodeFactory<? extends ExpressionNode>> primFacts = getFactories();
    for (NodeFactory<? extends ExpressionNode> primFact : primFacts) {
      trufflesom.primitives.Primitive[] prims = getPrimitiveAnnotation(primFact);
      if (prims != null) {
        for (trufflesom.primitives.Primitive prim : prims) {
          Specializer<? extends ExpressionNode> specializer = getSpecializer(prim, primFact);
          String className = prim.className();
          String primName = prim.primitive();

          if (!("".equals(primName)) && !("".equals(className))) {
            SSymbol clazz = universe.symbolFor(className);
            SSymbol signature = universe.symbolFor(primName);
            HashMap<SSymbol, Specializer<? extends ExpressionNode>> primsForClass =
                primitives.computeIfAbsent(clazz, s -> new HashMap<>());
            assert !primsForClass.containsKey(signature) : className
                + " already has a primitive " + primName + " registered.";
            primsForClass.put(signature, specializer);
          } else {
            assert "".equals(primName) && "".equals(
                className) : "If either primitive() or className() is set on @Primitive, both should be set";
          }

          if (!("".equals(prim.selector()))) {
            SSymbol msgSel = universe.symbolFor(prim.selector());
            assert !eagerPrimitives.containsKey(
                msgSel) : "There is already an eager primitive registered for selector: "
                    + prim.selector();
            eagerPrimitives.put(msgSel, specializer);
          }
        }
      }
    }
  }

  private static trufflesom.primitives.Primitive[] getPrimitiveAnnotation(
      final NodeFactory<? extends ExpressionNode> primFact) {
    Class<?> nodeClass = primFact.getNodeClass();
    return nodeClass.getAnnotationsByType(trufflesom.primitives.Primitive.class);
  }

  @SuppressWarnings("unchecked")
  private <T> Specializer<T> getSpecializer(final trufflesom.primitives.Primitive prim,
      final NodeFactory<T> factory) {
    try {
      return prim.specializer().getConstructor(trufflesom.primitives.Primitive.class,
          NodeFactory.class, Universe.class).newInstance(prim, factory, universe);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private static SInvokable constructPrimitive(final SSymbol signature,
      final SourceSection section, final SomLanguage lang,
      final Specializer<? extends ExpressionNode> specializer) {
    CompilerAsserts.neverPartOfCompilation("This is only executed during bootstrapping.");
    final int numArgs = signature.getNumberOfSignatureArguments();

    MethodGenerationContext mgen = new MethodGenerationContext(null);
    ExpressionNode[] args = new ExpressionNode[numArgs];
    for (int i = 0; i < numArgs; i++) {
      args[i] = new LocalArgumentReadNode(i, section);
    }

    ExpressionNode primNode = specializer.create(null, args, section, false);

    Primitive primMethodNode = new Primitive(signature.getString(), section, primNode,
        mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionNode) primNode.deepCopy(), lang);
    return Universe.newMethod(signature, primMethodNode, true, new SMethod[0]);
  }

  private static List<NodeFactory<? extends ExpressionNode>> getFactories() {
    List<NodeFactory<? extends ExpressionNode>> allFactories = new ArrayList<>();

    allFactories.addAll(BlockPrimsFactory.getFactories());
    allFactories.addAll(DoublePrimsFactory.getFactories());
    allFactories.addAll(IntegerPrimsFactory.getFactories());
    allFactories.addAll(StringPrimsFactory.getFactories());
    allFactories.addAll(SystemPrimsFactory.getFactories());
    allFactories.addAll(ClassPrimsFactory.getFactories());
    allFactories.addAll(MethodPrimsFactory.getFactories());
    allFactories.addAll(ObjectPrimsFactory.getFactories());

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
