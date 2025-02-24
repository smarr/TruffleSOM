package trufflesom.vm;

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import trufflesom.bdt.basic.IdProvider;
import trufflesom.vmobjects.SSymbol;


public class SymbolTable {
  private static final HashMap<String, SSymbol> symbolTable;

  public static final String strSelf         = "self";
  public static final String strSuper        = "super";
  public static final String strBlockSelf    = "$blockSelf";
  public static final String strFrameOnStack = "!frameOnStack";

  public static final SSymbol symNil;
  public static final SSymbol symTrue;
  public static final SSymbol symFalse;
  public static final SSymbol symSelf;
  public static final SSymbol symBlockSelf;
  public static final SSymbol symFrameOnStack;
  public static final SSymbol symSuper;

  public static final SSymbol symObject;
  public static final SSymbol symArray;
  public static final SSymbol symNewMsg;
  public static final SSymbol symAtPutMsg;
  public static final SSymbol symArraySizePlaceholder;

  public static final SSymbol symPlus;
  public static final SSymbol symMinus;
  public static final SSymbol symEquals;

  public static IdProvider<SSymbol> SymbolProvider = new Provider();

  private static final class Provider implements IdProvider<SSymbol> {
    @Override
    public SSymbol getId(final String id) {
      return symbolFor(id);
    }
  }

  @TruffleBoundary
  public static SSymbol symbolFor(final String string) {
    String interned = string.intern();
    // Lookup the symbol in the symbol table
    SSymbol result = symbolTable.get(interned);
    if (result != null) {
      return result;
    }

    result = new SSymbol(interned);
    symbolTable.put(interned, result);
    return result;
  }

  static {
    symbolTable = new HashMap<>();

    symNil = symbolFor("nil");
    symTrue = symbolFor("true");
    symFalse = symbolFor("false");
    symSelf = symbolFor("self");
    symBlockSelf = symbolFor("$blockSelf");
    symSuper = symbolFor("super");

    symObject = symbolFor("Object");
    symArray = symbolFor("Array");
    symNewMsg = symbolFor("new:");
    symAtPutMsg = symbolFor("at:put:");
    symArraySizePlaceholder = symbolFor("ArraySizeLiteralPlaceholder");

    symPlus = symbolFor("+");
    symMinus = symbolFor("-");
    symEquals = symbolFor("=");

    // Name for the frameOnStack slot,
    // starting with ! to make it a name that's not possible in Smalltalk
    symFrameOnStack = symbolFor("!frameOnStack");
  }
}
