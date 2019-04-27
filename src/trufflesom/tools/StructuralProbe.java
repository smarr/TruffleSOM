package trufflesom.tools;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.graalvm.collections.EconomicMap;

import trufflesom.compiler.Field;
import trufflesom.compiler.Variable;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public class StructuralProbe {

  protected final Set<SClass>                      classes;
  protected final EconomicMap<SSymbol, SInvokable> instanceMethods;
  protected final EconomicMap<SSymbol, SInvokable> classMethods;
  protected final Set<Field>                       instanceFields;
  protected final Set<Field>                       classFields;
  protected final Set<Variable>                    variables;

  public StructuralProbe() {
    classes = new LinkedHashSet<>();

    instanceMethods = EconomicMap.create();
    classMethods = EconomicMap.create();

    instanceFields = new LinkedHashSet<>();
    classFields = new LinkedHashSet<>();

    variables = new LinkedHashSet<>();
  }

  public synchronized void recordNewClass(final SClass clazz) {
    classes.add(clazz);
  }

  public synchronized void recordNewInstanceMethod(final SInvokable method) {
    // make sure we don't lose an invokable
    assert instanceMethods.get(method.getSignature(), method) == method;
    instanceMethods.put(method.getSignature(), method);
  }

  public synchronized void recordNewClassMethod(final SInvokable method) {
    // make sure we don't lose an invokable
    assert classMethods.get(method.getSignature(), method) == method;
    classMethods.put(method.getSignature(), method);
  }

  public synchronized void recordNewInstanceField(final Field field) {
    instanceFields.add(field);
  }

  public synchronized void recordNewClassField(final Field field) {
    classFields.add(field);
  }

  public synchronized void recordNewVariable(final Variable var) {
    variables.add(var);
  }

  public Set<SClass> getClasses() {
    return classes;
  }

  public Set<SInvokable> getInstanceMethods() {
    HashSet<SInvokable> res = new HashSet<>();
    for (SInvokable si : instanceMethods.getValues()) {
      res.add(si);
    }
    return res;
  }

  public Set<SInvokable> getClassMethods() {
    HashSet<SInvokable> res = new HashSet<>();
    for (SInvokable si : classMethods.getValues()) {
      res.add(si);
    }
    return res;
  }

  public Set<Field> getInstanceFields() {
    return instanceFields;
  }

  public Set<Field> getClassFields() {
    return classFields;
  }

  public Set<Variable> getVariables() {
    return variables;
  }

  public SInvokable lookupInstanceMethod(final SSymbol sym) {
    return instanceMethods.get(sym);
  }

  public SInvokable lookupClassMethod(final SSymbol sym) {
    return classMethods.get(sym);
  }

  public Field lookupInstanceField(final String name) {
    return lookupField(name, instanceFields);
  }

  public Field lookupClassField(final String name) {
    return lookupField(name, classFields);
  }

  private static Field lookupField(final String name, final Set<Field> fields) {
    return fields.stream()
                 .filter(f -> f.getName().getString().equals(name))
                 .findFirst()
                 .orElse(null);
  }
}
