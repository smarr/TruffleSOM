package som.vmobjects;

import som.interpreter.Types;
import som.vm.Universe;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;


public abstract class SAbstractObject {

  public abstract DynamicObject getSOMClass();

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    DynamicObject clazz = getSOMClass();
    if (clazz == null) {
      return "an Object(clazz==null)";
    }
    return "a " + SClass.getName(clazz).getString();
  }

  public static final Object send(
      final String selectorString,
      final Object[] arguments) {
    CompilerAsserts.neverPartOfCompilation("SAbstractObject.send()");
    SSymbol selector = Universe.current().symbolFor(selectorString);

    // Lookup the invokable
    SInvokable invokable = SClass.lookupInvokable(Types.getClassOf(arguments[0]), selector);

    return invokable.invoke(arguments);
  }

  public static final Object sendUnknownGlobal(final Object receiver,
      final SSymbol globalName) {
    Object[] arguments = {receiver, globalName};
    return send("unknownGlobal:", arguments);
  }

  public static final Object sendEscapedBlock(final Object receiver,
      final SBlock block) {
    Object[] arguments = {receiver, block};
    return send("escapedBlock:", arguments);
  }

}
