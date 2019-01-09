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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.source.SourceSection;

import trufflesom.interpreter.SomLanguage;
import trufflesom.tools.StructuralProbe;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SArray;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public final class ClassGenerationContext {
  private final Universe        universe;
  private final StructuralProbe structuralProbe;

  public ClassGenerationContext(final Universe universe,
      final StructuralProbe structuralProbe) {
    this.universe = universe;
    this.structuralProbe = structuralProbe;
  }

  private SSymbol                name;
  private SSymbol                superName;
  private SourceSection          sourceSection;
  private boolean                classSide;
  private final List<Field>      instanceFields  = new ArrayList<>();
  private final List<SInvokable> instanceMethods = new ArrayList<SInvokable>();
  private final List<Field>      classFields     = new ArrayList<>();
  private final List<SInvokable> classMethods    = new ArrayList<SInvokable>();

  public SomLanguage getLanguage() {
    return universe.getLanguage();
  }

  public Universe getUniverse() {
    return universe;
  }

  public void setName(final SSymbol name) {
    this.name = name;
  }

  public SSymbol getName() {
    return name;
  }

  public void setSourceSection(final SourceSection source) {
    sourceSection = source;
  }

  public SourceSection getSourceSection() {
    return sourceSection;
  }

  public void setSuperName(final SSymbol superName) {
    this.superName = superName;
  }

  public void setInstanceFieldsOfSuper(final Field[] fields) {
    for (Field f : fields) {
      instanceFields.add(f);
    }
  }

  public void setClassFieldsOfSuper(final Field[] fields) {
    for (Field f : fields) {
      classFields.add(f);
    }
  }

  public void addInstanceMethod(final SInvokable meth) {
    instanceMethods.add(meth);
  }

  public void setClassSide(final boolean b) {
    classSide = b;
  }

  public void addClassMethod(final SInvokable meth) {
    classMethods.add(meth);
  }

  public void addInstanceField(final SSymbol name, final SourceSection source) {
    Field f = new Field(instanceFields.size(), name, source);
    instanceFields.add(f);
  }

  public void addClassField(final SSymbol name, final SourceSection source) {
    Field f = new Field(classFields.size(), name, source);
    classFields.add(f);
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
    // build class class name
    String ccname = name.getString() + " class";

    // Load the super class
    SClass superClass = universe.loadClass(superName);

    // Allocate the class of the resulting class
    SClass resultClass = universe.newClass(universe.metaclassClass);

    // Initialize the class of the resulting class
    resultClass.setInstanceFields(classFields);
    resultClass.setInstanceInvokables(
        SArray.create(classMethods.toArray(new Object[0])));
    resultClass.setName(universe.symbolFor(ccname));

    SClass superMClass = superClass.getSOMClass(universe);
    resultClass.setSuperClass(superMClass);
    resultClass.setSourceSection(sourceSection);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(resultClass);
    }

    // Allocate the resulting class
    SClass result = universe.newClass(resultClass);

    // Initialize the resulting class
    result.setName(name);
    result.setSuperClass(superClass);
    result.setInstanceFields(instanceFields);
    result.setInstanceInvokables(SArray.create(instanceMethods.toArray(new Object[0])));
    result.setSourceSection(sourceSection);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(result);
    }
    return result;
  }

  @TruffleBoundary
  public void assembleSystemClass(final SClass systemClass) {
    systemClass.setInstanceInvokables(SArray.create(instanceMethods.toArray(new Object[0])));
    systemClass.setInstanceFields(instanceFields);

    if (structuralProbe != null) {
      structuralProbe.recordNewClass(systemClass);
    }

    // class-bound == class-instance-bound
    SClass superMClass = systemClass.getSOMClass(universe);
    superMClass.setInstanceInvokables(SArray.create(classMethods.toArray(new Object[0])));
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
