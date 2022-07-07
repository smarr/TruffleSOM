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

import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bdt.source.SourceCoordinate;
import bdt.tools.structure.StructuralProbe;
import trufflesom.compiler.Parser.ParseError;
import trufflesom.vm.Classes;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SInvokable.SPrimitive;
import trufflesom.vmobjects.SSymbol;


public final class ClassGenerationContext {
  private final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe;

  public ClassGenerationContext(final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> structuralProbe) {
    this.source = source;
    this.structuralProbe = structuralProbe;
  }

  private final Source source;

  private SSymbol name;
  private SClass  superClass;
  private long    sourceCoord;

  private boolean           classSide;
  private final List<Field> instanceFields = new ArrayList<>();
  private final List<Field> classFields    = new ArrayList<>();

  private final LinkedHashMap<SSymbol, SInvokable> instanceMethods = new LinkedHashMap<>();
  private final LinkedHashMap<SSymbol, SInvokable> classMethods    = new LinkedHashMap<>();

  private boolean instanceHasPrimitives = false;
  private boolean classHasPrimitives    = false;

  public void setName(final SSymbol name) {
    this.name = name;
  }

  public SSymbol getName() {
    return name;
  }

  public Source getSource() {
    return source;
  }

  public long getSourceCoord() {
    return sourceCoord;
  }

  public void setSourceCoord(final long sourceCoord) {
    this.sourceCoord = sourceCoord;
  }

  /** Return the super class, considering whether we are instance or class side. */
  public SClass getSuperClass() {
    if (classSide) {
      return superClass.getSOMClass();
    }
    return superClass;
  }

  public void setSuperClass(final SClass superClass) {
    this.superClass = superClass;
    setInstanceFieldsOfSuper(superClass.getInstanceFieldDefinitions());
    setClassFieldsOfSuper(
        superClass.getSOMClass().getInstanceFieldDefinitions());
  }

  private void setInstanceFieldsOfSuper(final Field[] fields) {
    for (Field f : fields) {
      instanceFields.add(f);
    }
  }

  private void setClassFieldsOfSuper(final Field[] fields) {
    for (Field f : fields) {
      classFields.add(f);
    }
  }

  public void addInstanceMethod(final SInvokable meth, final Parser<?> parser)
      throws ParseError {
    if (instanceMethods.containsKey(meth.getSignature())) {
      String msg = "A method with name " + meth.getSignature().getString()
          + " is already declared in " + name.getString();
      throw new ParseError(msg, Symbol.NONE, parser);
    }

    instanceMethods.put(meth.getSignature(), meth);
    if (meth instanceof SPrimitive) {
      instanceHasPrimitives = true;
    }
  }

  public void switchToClassSide() {
    classSide = true;
  }

  public void addClassMethod(final SInvokable meth, final Parser<?> parser) throws ParseError {
    if (classMethods.containsKey(meth.getSignature())) {
      String msg = "A method with name " + meth.getSignature().getString()
          + " is already declared in " + name.getString();
      throw new ParseError(msg, Symbol.NONE, parser);
    }

    classMethods.put(meth.getSignature(), meth);
    if (meth instanceof SPrimitive) {
      classHasPrimitives = true;
    }
  }

  public Field addInstanceField(final SSymbol name, final long coord) {
    int length = SourceCoordinate.getLength(coord);
    assert name.getString().length() == length;

    Field f = new Field(instanceFields.size(), name, coord);
    instanceFields.add(f);
    if (structuralProbe != null) {
      structuralProbe.recordNewSlot(f);
    }
    return f;
  }

  public Field addClassField(final SSymbol name, final long coord) {
    int length = SourceCoordinate.getLength(coord);
    assert name.getString().length() == length;

    Field f = new Field(classFields.size(), name, coord);
    classFields.add(f);
    if (structuralProbe != null) {
      structuralProbe.recordNewSlot(f);
    }
    return f;
  }

  public boolean hasField(final SSymbol fieldName) {
    List<Field> fields = (isClassSide() ? classFields : instanceFields);

    for (Field f : fields) {
      if (f.getName() == fieldName) {
        return true;
      }
    }
    return false;
  }

  public byte getFieldIndex(final SSymbol fieldName) {
    List<Field> fields = (isClassSide() ? classFields : instanceFields);

    for (Field f : fields) {
      if (f.getName() == fieldName) {
        return (byte) f.getIndex();
      }
    }
    return -1;
  }

  public boolean isClassSide() {
    return classSide;
  }

  @TruffleBoundary
  public SClass assemble() {
    SourceSection sourceSection = SourceCoordinate.createSourceSection(source, sourceCoord);

    // build class class name
    String ccname = name.getString() + " class";

    // Allocate the class of the resulting class
    SClass resultClass = new SClass(Classes.metaclassClass);

    // Initialize the class of the resulting class
    resultClass.setInstanceFields(classFields);
    resultClass.setInstanceInvokables(classMethods, classHasPrimitives);
    resultClass.setName(symbolFor(ccname));

    SClass superMClass = superClass == null ? null : superClass.getSOMClass();
    resultClass.setSuperClass(superMClass);
    resultClass.setSourceSection(sourceSection);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(resultClass);
    }

    // Allocate the resulting class
    SClass result = new SClass(resultClass);

    // Initialize the resulting class
    result.setName(name);
    result.setSuperClass(superClass);
    result.setInstanceFields(instanceFields);
    result.setInstanceInvokables(instanceMethods, instanceHasPrimitives);
    result.setSourceSection(sourceSection);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(result);
    }
    return result;
  }

  @TruffleBoundary
  public void assembleSystemClass(final SClass systemClass) {
    systemClass.setInstanceInvokables(instanceMethods, instanceHasPrimitives);
    systemClass.setInstanceFields(instanceFields);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(systemClass);
    }

    // class-bound == class-instance-bound
    SClass superMClass = systemClass.getSOMClass();
    superMClass.setInstanceInvokables(classMethods, classHasPrimitives);
    superMClass.setInstanceFields(classFields);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(superMClass);
    }
  }

  @Override
  public String toString() {
    return "ClassGenC(" + name.getString() + ")";
  }
}
