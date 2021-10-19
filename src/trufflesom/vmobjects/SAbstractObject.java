package trufflesom.vmobjects;

import static trufflesom.vm.SymbolTable.symbolFor;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.Types;
import trufflesom.vm.Universe;


@ExportLibrary(InteropLibrary.class)
public abstract class SAbstractObject implements TruffleObject {

  public abstract SClass getSOMClass(Universe universe);

  @Override
  public String toString() {
    CompilerAsserts.neverPartOfCompilation();
    SClass clazz = getSOMClass(SomLanguage.getCurrentContext());
    if (clazz == null) {
      return "an Object(clazz==null)";
    }
    return "a " + clazz.getName().getString();
  }

  private static Object send2(
      final String selectorString,
      final Object rcvr, final Object arg, final Universe universe) {
    CompilerAsserts.neverPartOfCompilation("SAbstractObject.send()");
    SSymbol selector = symbolFor(selectorString);

    // Lookup the invokable
    SInvokable invokable = Types.getClassOf(rcvr, universe).lookupInvokable(selector);
    return invokable.invoke2(rcvr, arg);
  }

  @TruffleBoundary
  public static final Object sendUnknownGlobal(final Object receiver,
      final SSymbol globalName, final Universe universe) {
    return send2("unknownGlobal:", receiver, globalName, universe);
  }

  @TruffleBoundary
  public static final Object sendEscapedBlock(final Object receiver,
      final SBlock block, final Universe universe) {
    return send2("escapedBlock:", receiver, block, universe);
  }

  @ExportMessage
  public final boolean isNull() {
    // can't be null, because our Nil.nilObject is `null`, which is a dynamic object
    return false;
  }
}
