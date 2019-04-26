package trufflesom.vmobjects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import trufflesom.interop.SAbstractObjectInteropMessagesForeign;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.vm.Universe;


public abstract class SAbstractObject implements TruffleObject {

  public abstract SClass getSOMClass(Universe universe);

  @Override
  public final ForeignAccess getForeignAccess() {
    return SAbstractObjectInteropMessagesForeign.ACCESS;
  }

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    SClass clazz = getSOMClass(SomLanguage.getCurrentContext());
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

  /**
   * Used by Truffle interop.
   */
  public static boolean isInstance(final TruffleObject obj) {
    return obj instanceof SAbstractObject;
  }
}
