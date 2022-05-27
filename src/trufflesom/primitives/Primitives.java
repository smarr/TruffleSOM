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

import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import bdt.inlining.InlinableNodes;
import bdt.primitives.PrimitiveLoader;
import bdt.primitives.Specializer;
import bdt.tools.structure.StructuralProbe;
import trufflesom.compiler.Field;
import trufflesom.compiler.Variable;
import trufflesom.interpreter.Primitive;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.specialized.AndBoolMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.AndMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.BooleanInlinedLiteralNode.AndInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.BooleanInlinedLiteralNode.OrInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfMessageNodeGen.IfFalseMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IfMessageNodeGen.IfTrueMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IfNilInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfNotNilInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseInlinedLiteralsNode.FalseIfElseLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseInlinedLiteralsNode.TrueIfElseLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntDownToDoInlinedLiteralsNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntDownToDoMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntToDoInlinedLiteralsNodeFactory;
import trufflesom.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.NotMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.OrBoolMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.OrMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.whileloops.WhileFalsePrimitiveNodeFactory;
import trufflesom.interpreter.nodes.specialized.whileloops.WhileInlinedLiteralsNode;
import trufflesom.interpreter.nodes.specialized.whileloops.WhileTruePrimitiveNodeFactory;
import trufflesom.primitives.arithmetic.AdditionPrimFactory;
import trufflesom.primitives.arithmetic.BitXorPrimFactory;
import trufflesom.primitives.arithmetic.CosPrimFactory;
import trufflesom.primitives.arithmetic.DividePrimFactory;
import trufflesom.primitives.arithmetic.DoubleDivPrimFactory;
import trufflesom.primitives.arithmetic.GreaterThanOrEqualPrimFactory;
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
import trufflesom.primitives.basics.UnequalUnequalPrimFactory;
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
import trufflesom.vm.SymbolTable;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SPrimitive;
import trufflesom.vmobjects.SSymbol;


public final class Primitives extends PrimitiveLoader<ExpressionNode, SSymbol> {

  public static final Primitives Current;

  public static final InlinableNodes<SSymbol> inlinableNodes;

  private static List<Specializer<ExpressionNode, SSymbol>> specializer;

  static {
    specializer = initSpecializers();
    inlinableNodes = new InlinableNodes<>(
        SymbolTable.SymbolProvider, getInlinableNodes(), getInlinableFactories());
    Current = new Primitives();
  }

  /** Primitives for class and method name. */
  private final HashMap<SSymbol, HashMap<SSymbol, Specializer<ExpressionNode, SSymbol>>> primitives;

  public static SPrimitive constructEmptyPrimitive(final SSymbol signature,
      final Source source, final long coord,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    CompilerAsserts.neverPartOfCompilation();

    ExpressionNode primNode = EmptyPrim.create(new LocalArgumentReadNode(true, 0), signature)
                                       .initialize(coord);
    Primitive primMethodNode =
        new Primitive(signature.getString(), source, coord, primNode,
            (ExpressionNode) primNode.deepCopy());
    SPrimitive prim = new SPrimitive(signature, primMethodNode);

    if (probe != null) {
      String id = prim.getIdentifier();
      probe.recordNewMethod(symbolFor(id), prim);
    }
    return prim;
  }

  private Primitives() {
    super(SymbolTable.SymbolProvider);
    this.primitives = new HashMap<>();
    initialize();
  }

  public void loadPrimitives(final SClass clazz, final boolean displayWarning,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    HashMap<SSymbol, Specializer<ExpressionNode, SSymbol>> prims =
        primitives.get(clazz.getName());
    if (prims == null) {
      if (displayWarning) {
        Universe.errorPrintln("No primitives found for " + clazz.getName().getString());
      }
      return;
    }

    for (Entry<SSymbol, Specializer<ExpressionNode, SSymbol>> e : prims.entrySet()) {
      SClass target;
      if (e.getValue().classSide()) {
        target = clazz.getSOMClass();
      } else {
        target = clazz;
      }

      SInvokable ivk = target.lookupInvokable(e.getKey());
      assert ivk != null : "Lookup of " + e.getKey().toString() + " failed in "
          + target.getName().getString() + ". Can't install a primitive for it.";
      SInvokable prim = constructPrimitive(
          e.getKey(), ivk.getSource(), ivk.getSourceCoordinate(), e.getValue(), probe);
      target.addPrimitive(prim);
    }
  }

  @Override
  protected void registerPrimitive(
      final Specializer<ExpressionNode, SSymbol> specializer) {
    String className = specializer.getPrimitive().className();
    String primName = specializer.getPrimitive().primitive();

    if (!("".equals(primName)) && !("".equals(className))) {
      SSymbol clazz = ids.getId(className);
      SSymbol signature = ids.getId(primName);
      HashMap<SSymbol, Specializer<ExpressionNode, SSymbol>> primsForClass =
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
      final Source source, final long coord,
      final Specializer<ExpressionNode, SSymbol> specializer,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    CompilerAsserts.neverPartOfCompilation("This is only executed during bootstrapping.");

    final int numArgs = signature.getNumberOfSignatureArguments();

    // args is needed to communicate the number of arguments to the constructor
    ExpressionNode[] args = new ExpressionNode[numArgs];

    ExpressionNode primNode = specializer.create(null, args, coord);

    Primitive primMethodNode = new Primitive(signature.getString(), source, coord, primNode,
        (ExpressionNode) primNode.deepCopy());
    return new SPrimitive(signature, primMethodNode);
  }

  @Override
  protected List<Specializer<ExpressionNode, SSymbol>> getSpecializers() {
    return specializer;
  }

  private static List<Specializer<ExpressionNode, SSymbol>> initSpecializers() {
    List<Specializer<ExpressionNode, SSymbol>> allFactories = new ArrayList<>();

    addAll(allFactories, BlockPrimsFactory.getFactories());
    addAll(allFactories, DoublePrimsFactory.getFactories());
    addAll(allFactories, IntegerPrimsFactory.getFactories());
    addAll(allFactories, StringPrimsFactory.getFactories());
    addAll(allFactories, SystemPrimsFactory.getFactories());
    addAll(allFactories, ClassPrimsFactory.getFactories());
    addAll(allFactories, MethodPrimsFactory.getFactories());
    addAll(allFactories, ObjectPrimsFactory.getFactories());

    add(allFactories, AdditionPrimFactory.getInstance());
    add(allFactories, BitXorPrimFactory.getInstance());
    add(allFactories, CosPrimFactory.getInstance());
    add(allFactories, DividePrimFactory.getInstance());
    add(allFactories, DoubleDivPrimFactory.getInstance());
    add(allFactories, GreaterThanPrimFactory.getInstance());
    add(allFactories, GreaterThanOrEqualPrimFactory.getInstance());
    add(allFactories, LessThanOrEqualPrimFactory.getInstance());
    add(allFactories, LessThanPrimFactory.getInstance());
    add(allFactories, LogicAndPrimFactory.getInstance());
    add(allFactories, ModuloPrimFactory.getInstance());
    add(allFactories, MultiplicationPrimFactory.getInstance());
    add(allFactories, RemainderPrimFactory.getInstance());
    add(allFactories, SinPrimFactory.getInstance());
    add(allFactories, SqrtPrimFactory.getInstance());
    add(allFactories, SubtractionPrimFactory.getInstance());

    add(allFactories, AtPrimFactory.getInstance());
    add(allFactories, AtPutPrimFactory.getInstance());
    add(allFactories, CopyPrimFactory.getInstance());
    add(allFactories, DoIndexesPrimFactory.getInstance());
    add(allFactories, DoPrimFactory.getInstance());
    add(allFactories, NewPrimFactory.getInstance());
    add(allFactories, PutAllNodeFactory.getInstance());

    add(allFactories, AsStringPrimFactory.getInstance());
    add(allFactories, EqualsEqualsPrimFactory.getInstance());
    add(allFactories, UnequalUnequalPrimFactory.getInstance());
    add(allFactories, EqualsPrimFactory.getInstance());
    add(allFactories, HashPrimFactory.getInstance());
    add(allFactories, LengthPrimFactory.getInstance());
    add(allFactories, NewObjectPrimFactory.getInstance());
    add(allFactories, UnequalsPrimFactory.getInstance());

    add(allFactories, AndMessageNodeFactory.getInstance());
    add(allFactories, AndBoolMessageNodeFactory.getInstance());
    add(allFactories, IntToDoMessageNodeFactory.getInstance());
    add(allFactories, IntToByDoMessageNodeFactory.getInstance());
    add(allFactories, IntDownToDoMessageNodeFactory.getInstance());
    add(allFactories, OrMessageNodeFactory.getInstance());
    add(allFactories, OrBoolMessageNodeFactory.getInstance());
    add(allFactories, IfTrueIfFalseMessageNodeFactory.getInstance());
    add(allFactories, NotMessageNodeFactory.getInstance());

    add(allFactories, GlobalPrimFactory.getInstance());
    add(allFactories, HasGlobalPrimFactory.getInstance());
    add(allFactories, ObjectSizePrimFactory.getInstance());
    add(allFactories, PerformInSuperclassPrimFactory.getInstance());
    add(allFactories, PerformPrimFactory.getInstance());
    add(allFactories, PerformWithArgumentsInSuperclassPrimFactory.getInstance());
    add(allFactories, PerformWithArgumentsPrimFactory.getInstance());

    add(allFactories, WhileTruePrimitiveNodeFactory.getInstance());
    add(allFactories, WhileFalsePrimitiveNodeFactory.getInstance());
    add(allFactories, IfTrueMessageNodeFactory.getInstance());
    add(allFactories, IfFalseMessageNodeFactory.getInstance());

    return allFactories;
  }

  public static List<Class<? extends Node>> getInlinableNodes() {
    List<Class<? extends Node>> nodes = new ArrayList<>();

    nodes.add(IfInlinedLiteralNode.class);
    nodes.add(IfNilInlinedLiteralNode.class);
    nodes.add(IfNotNilInlinedLiteralNode.class);
    nodes.add(TrueIfElseLiteralNode.class);
    nodes.add(FalseIfElseLiteralNode.class);
    nodes.add(AndInlinedLiteralNode.class);
    nodes.add(OrInlinedLiteralNode.class);
    nodes.add(WhileInlinedLiteralsNode.class);

    return nodes;
  }

  public static List<NodeFactory<? extends Node>> getInlinableFactories() {
    List<NodeFactory<? extends Node>> factories = new ArrayList<>();

    factories.add(IntToDoInlinedLiteralsNodeFactory.getInstance());
    factories.add(IntDownToDoInlinedLiteralsNodeFactory.getInstance());

    return factories;
  }
}
