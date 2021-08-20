package trufflesom.tests;

import static trufflesom.compiler.bc.Disassembler.dumpMethod;
import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Ignore;

import trufflesom.compiler.bc.BytecodeMethodGenContext;
import trufflesom.interpreter.Method;
import trufflesom.interpreter.nodes.bc.BytecodeLoopNode;
import trufflesom.vmobjects.SInvokable.SMethod;


@Ignore("provides just setup")
public class BytecodeTestSetup extends TruffleTestSetup {

  protected BytecodeMethodGenContext mgenc;

  public BytecodeTestSetup() {
    super();
    initMgenc();
  }

  public void initMgenc() {
    mgenc = new BytecodeMethodGenContext(cgenc, probe);
    mgenc.addArgumentIfAbsent(symSelf, sourceForTests.createSection(1, 1));
  }

  protected byte[] getBytecodesOfBlock(final int bytecodeIdx) {
    SMethod blockMethod = (SMethod) mgenc.getConstant(25);
    Method blockIvkbl = (Method) blockMethod.getInvokable();
    return read(blockIvkbl, "expressionOrSequence", BytecodeLoopNode.class).getBytecodeArray();
  }

  public void dump() {
    dumpMethod(mgenc);
  }
}
