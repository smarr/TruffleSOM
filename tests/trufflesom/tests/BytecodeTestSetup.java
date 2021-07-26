package trufflesom.tests;

import static trufflesom.compiler.bc.Disassembler.dumpMethod;

import org.junit.Ignore;

import com.oracle.truffle.api.source.Source;

import bd.tools.structure.StructuralProbe;
import trufflesom.compiler.ClassGenerationContext;
import trufflesom.compiler.Field;
import trufflesom.compiler.Variable;
import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.SomLanguage;
import trufflesom.vm.Universe;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


@Ignore("provides just setup")
public class BytecodeTestSetup {

  protected final ClassGenerationContext   cgenc;
  protected final BytecodeMethodGenContext mgenc;

  protected final Universe                                                      universe;
  protected final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe;

  private static Universe createUniverse() {
    Universe u = new Universe(new SomLanguage());
    Source s = SomLanguage.getSyntheticSource("self", "self");
    u.selfSource = s.createSection(1);
    return u;
  }

  public BytecodeTestSetup() {
    universe = createUniverse();
    probe = null;

    cgenc = new ClassGenerationContext(universe, null);
    cgenc.setName(universe.symbolFor("Test"));

    mgenc = new BytecodeMethodGenContext(cgenc, probe);
    mgenc.addArgumentIfAbsent(universe.symSelf, null);
  }

  public void dump() {
    dumpMethod(mgenc);
  }

  protected void addField(final String name) {
    cgenc.addInstanceField(universe.symbolFor(name), null);
  }
}
