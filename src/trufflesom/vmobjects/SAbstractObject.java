package trufflesom.vmobjects;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;

import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.vm.Universe;


@ExportLibrary(InteropLibrary.class)
public abstract class SAbstractObject implements TruffleObject {

  public abstract DynamicObject getSOMClass(Universe universe);

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    Universe u = SomLanguage.getCurrentContext();
    DynamicObject clazz = getSOMClass(u);
    if (clazz == null) {
      return "an Object(clazz==null)";
    }
    return "a " + SClass.getName(clazz, u).getString();
  }

  public static final Object send(
      final String selectorString,
      final Object[] arguments, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("SAbstractObject.send()");
    SSymbol selector = universe.symbolFor(selectorString);

    // Lookup the invokable
    SInvokable invokable = SClass.lookupInvokable(
        Types.getClassOf(arguments[0], universe), selector, universe);

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

  @ExportMessage
  public final boolean isNull() {
    // can't be null, because our Nil.nilObject is `null`, which is a dynamic object
    return false;
  }
}
