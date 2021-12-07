package trufflesom.vm;

import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.HashMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import trufflesom.vmobjects.SSymbol;


public class Globals {
  private static final HashMap<SSymbol, Association> globals =
      new HashMap<SSymbol, Association>();

  @TruffleBoundary
  public static boolean hasGlobal(final SSymbol name) {
    return globals.containsKey(name);
  }

  @TruffleBoundary
  public static Object getGlobal(final SSymbol name) {
    Association assoc = globals.get(name);
    if (assoc == null) {
      return null;
    }
    return assoc.getValue();
  }

  @TruffleBoundary
  public static Association getGlobalsAssociation(final SSymbol name) {
    return globals.get(name);
  }

  public static void setGlobal(final String name, final Object value) {
    setGlobal(symbolFor(name), value);
  }

  @TruffleBoundary
  public static void setGlobal(final SSymbol name, final Object value) {
    Association assoc = globals.get(name);
    if (assoc == null) {
      assoc = new Association(name, value);
      globals.put(name, assoc);
    } else {
      assoc.setValue(value);
    }
  }

  public static void reset() {
    globals.clear();
  }

  /**
   * Associations are handles for globals with a fixed
   * SSymbol and a mutable value.
   */
  public static final class Association {
    private final SSymbol          key;
    private final CyclicAssumption assumption;

    @CompilationFinal private Object value;

    public Association(final SSymbol key, final Object value) {
      this.key = key;
      this.value = value;
      this.assumption = new CyclicAssumption("Global: " + key.getString());
    }

    public SSymbol getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(final Object value) {
      this.assumption.invalidate("updated global");
      this.value = value;
    }

    public Assumption getAssumption() {
      return assumption.getAssumption();
    }
  }
}
