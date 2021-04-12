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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import bd.basic.IdProvider;
import bd.inlining.InlinableNodes;
import bd.primitives.PrimitiveLoader;
import bd.primitives.Specializer;
import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.Field;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.Variable;
import trufflesom.interpreter.Primitive;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.specialized.AndBoolMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.AndMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.BooleanInlinedLiteralNode.AndInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.BooleanInlinedLiteralNode.OrInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfInlinedLiteralNode;
import trufflesom.interpreter.nodes.specialized.IfMessageNodeGen.IfFalseMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IfMessageNodeGen.IfTrueMessageNodeFactory;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseInlinedLiteralsNode;
import trufflesom.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
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
import trufflesom.vmobjects.SInvokable.SPrimitive;
import trufflesom.vmobjects.SSymbol;


public final class Primitives extends PrimitiveLoader<Universe, ExpressionNode, SSymbol> {

  public static final InlinableNodes<SSymbol> inlinableNodes;
  private static final StaticSymbolProvider   idProvider;

  private static List<Specializer<Universe, ExpressionNode, SSymbol>> specializer =
      initSpecializers();

  private final Universe universe;

  /** Primitives for class and method name. */
  private final HashMap<SSymbol, HashMap<SSymbol, Specializer<Universe, ExpressionNode, SSymbol>>> primitives;

  public static SPrimitive constructEmptyPrimitive(final SSymbol signature,
      final SomLanguage lang, final SourceSection sourceSection,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    CompilerAsserts.neverPartOfCompilation();
    MethodGenerationContext mgen = new MethodGenerationContext(lang.getUniverse(), probe);

    ExpressionNode primNode = EmptyPrim.create(new LocalArgumentReadNode(true, 0), signature)
                                       .initialize(sourceSection, false);
    Primitive primMethodNode =
        new Primitive(signature.getString(), sourceSection, primNode,
            mgen.getCurrentLexicalScope().getFrameDescriptor(),
            (ExpressionNode) primNode.deepCopy(), lang);
    SPrimitive prim = new SPrimitive(signature, primMethodNode, sourceSection);

    if (probe != null) {
      String id = prim.getIdentifier();
      probe.recordNewMethod(lang.getUniverse().symbolFor(id), prim);
    }
    return prim;
  }

  public Primitives(final Universe universe) {
    super(universe);
    this.universe = universe;
    this.primitives = new HashMap<>();
    initialize();
  }

  public void loadPrimitives(final SClass clazz, final boolean displayWarning,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
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
        target = clazz.getSOMClass(universe);
      } else {
        target = clazz;
      }

      SInvokable ivk = target.lookupInvokable(e.getKey());
      assert ivk != null : "Lookup of " + e.getKey().toString() + " failed in "
          + target.getName().getString() + ". Can't install a primitive for it.";
      SInvokable prim = constructPrimitive(
          e.getKey(), ivk.getSourceSection(), universe.getLanguage(), e.getValue(), probe);
      target.addInstanceInvokable(prim);
    }
  }

  @Override
  protected void registerPrimitive(
      final Specializer<Universe, ExpressionNode, SSymbol> specializer) {
    String className = specializer.getPrimitive().className();
    String primName = specializer.getPrimitive().primitive();

    if (!("".equals(primName)) && !("".equals(className))) {
      SSymbol clazz = ids.getId(className);
      SSymbol signature = ids.getId(primName);
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
      final SourceSection source, final SomLanguage lang,
      final Specializer<Universe, ExpressionNode, SSymbol> specializer,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    CompilerAsserts.neverPartOfCompilation("This is only executed during bootstrapping.");
    final int numArgs = signature.getNumberOfSignatureArguments();

    MethodGenerationContext mgen = new MethodGenerationContext(lang.getUniverse(), probe);
    ExpressionNode[] args = new ExpressionNode[numArgs];
    for (int i = 0; i < numArgs; i++) {
      args[i] = new LocalArgumentReadNode(true, i).initialize(source);
    }

    ExpressionNode primNode =
        specializer.create(null, args, source, false, lang.getUniverse());

    Primitive primMethodNode = new Primitive(signature.getString(), source, primNode,
        mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionNode) primNode.deepCopy(), lang);
    return new SPrimitive(signature, primMethodNode, source);
  }

  @Override
  protected List<Specializer<Universe, ExpressionNode, SSymbol>> getSpecializers() {
    return specializer;
  }

  private static List<Specializer<Universe, ExpressionNode, SSymbol>> initSpecializers() {
    List<Specializer<Universe, ExpressionNode, SSymbol>> allFactories = new ArrayList<>();

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
    nodes.add(IfTrueIfFalseInlinedLiteralsNode.class);
    nodes.add(AndInlinedLiteralNode.class);
    nodes.add(OrInlinedLiteralNode.class);
    nodes.add(WhileInlinedLiteralsNode.class);

    return nodes;
  }

  public static List<NodeFactory<? extends Node>> getInlinableFactories() {
    List<NodeFactory<? extends Node>> factories = new ArrayList<>();

    factories.add(IntToDoInlinedLiteralsNodeFactory.getInstance());

    return factories;
  }

  private static class StaticSymbolProvider implements IdProvider<SSymbol> {
    private final HashMap<String, SSymbol> symbolTable = new HashMap<>();

    @Override
    public SSymbol getId(final String id) {
      String interned = id.intern();
      // Lookup the symbol in the symbol table
      SSymbol result = symbolTable.get(interned);
      if (result != null) {
        return result;
      }

      result = new SSymbol(interned);
      symbolTable.put(interned, result);
      return result;
    }

    public void addSymbols(final HashMap<String, SSymbol> symbolTable) {
      symbolTable.putAll(this.symbolTable);
    }
  }

  public static void initializeStaticSymbols(final HashMap<String, SSymbol> symbolTable) {
    idProvider.addSymbols(symbolTable);
  }

  static {
    idProvider = new StaticSymbolProvider();
    inlinableNodes =
        new InlinableNodes<>(idProvider, getInlinableNodes(), getInlinableFactories());
  }
}
