package som.vmobjects;

import com.oracle.truffle.api.CompilerAsserts;

import som.interpreter.Types;
import som.vm.Universe;


public abstract class SAbstractObject {

  public abstract SClass getSOMClass();

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    SClass clazz = getSOMClass();
    if (clazz == null) {
      return "an Object(clazz==null)";
    }
    return "a " + clazz.getName().getString();
  }

  public static final Object send(
      final String selectorString,
      final Object[] arguments, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("SAbstractObject.send()");
    SSymbol selector = universe.symbolFor(selectorString);

    // Lookup the invokable
    SInvokable invokable = Types.getClassOf(arguments[0], universe).lookupInvokable(selector);

    return invokable.invoke(arguments);
  }

  public static final Object sendUnknownGlobal(final Object receiver,
      final SSymbol globalName, final Universe universe) {
    Object[] arguments = {receiver, globalName};
    return send("unknownGlobal:", arguments, universe);
  }

  public static final Object sendEscapedBlock(final Object receiver,
      final SBlock block, final Universe universe) {
    Object[] arguments = {receiver, block};
    return send("escapedBlock:", arguments, universe);
  }

}
