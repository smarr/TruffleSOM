package trufflesom.tests;

import static trufflesom.vm.SymbolTable.symbolFor;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.junit.Ignore;

import com.oracle.truffle.api.nodes.Node;

import bd.source.SourceCoordinate;
import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.ClassGenerationContext;
import trufflesom.compiler.Field;
import trufflesom.compiler.Variable;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.interpreter.nodes.literals.BlockNode;
import trufflesom.interpreter.objectstorage.StorageAnalyzer;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@Ignore("provides just setup")
public class TruffleTestSetup {
  protected ClassGenerationContext cgenc;

  protected final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe;

  protected List<String> fieldNames;
  protected List<String> argNames;

  protected TruffleTestSetup() {
    probe = null;
    fieldNames = new ArrayList<>();
    argNames = new ArrayList<>();
  }

  private static void initTruffle() {
    StorageAnalyzer.initAccessors();

    Builder builder = Universe.createContextBuilder();
    builder.logHandler(System.err);

    Context context = builder.build();
    context.eval(SomLanguage.INIT);

    Universe.selfSource = SomLanguage.getSyntheticSource("self", "self");
    Universe.selfCoord = SourceCoordinate.createEmpty();
  }

  protected void addField(final String name) {
    fieldNames.add(name);
  }

  protected void addArgument(final String name) {
    argNames.add(name);
  }

  protected void addAllFields() {
    int i = 2;
    for (String fieldName : fieldNames) {
      cgenc.addInstanceField(symbolFor(fieldName),
          SourceCoordinate.create(i, 1));
      i += 1;
    }
    fieldNames.clear();
  }

  private java.lang.reflect.Field lookup(final Class<?> cls, final String fieldName) {
    try {
      return cls.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      if (cls.getSuperclass() != null) {
        return lookup(cls.getSuperclass(), fieldName);
      }
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException("Didn't find field: " + fieldName);
  }

  protected Node read(final Object obj, final String fieldName) {
    return read(obj, fieldName, Node.class);
  }

  protected ExpressionNode read(final Object obj, final String fieldName, final int idx) {
    return read(obj, fieldName, ExpressionNode[].class)[idx];
  }

  protected ExpressionNode[] getBlockExprs(final BlockNode blockNode) {
    return read(read(blockNode.getMethod().getInvokable(), "body"),
        "expressions", ExpressionNode[].class);
  }

  protected <T> T read(final Object obj, final String fieldName, final Class<T> c) {
    java.lang.reflect.Field field = lookup(obj.getClass(), fieldName);
    field.setAccessible(true);
    try {
      return c.cast(field.get(obj));
    } catch (IllegalAccessException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }

  static {
    initTruffle();
  }
}
