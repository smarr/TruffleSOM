package trufflesom.compiler;

import com.oracle.truffle.api.bytecode.BytecodeConfig;
import com.oracle.truffle.api.source.Source;

import trufflesom.bdt.basic.ProgramDefinitionError;
import trufflesom.bdt.tools.structure.StructuralProbe;
import trufflesom.interpreter.operations.SomOperationsGen;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SInvokable;
import trufflesom.vmobjects.SSymbol;


public class ParserOp extends ParserAst {

  public ParserOp(final String code, final Source source,
      final StructuralProbe<SSymbol, SClass, SInvokable, Field, Variable> probe) {
    super(code, source, probe);
  }

  @Override
  public void classdef(final ClassGenerationContext cgenc) throws ProgramDefinitionError {
    ProgramDefinitionError[] error = new ProgramDefinitionError[1];

    SomOperationsGen.create(BytecodeConfig.WITH_SOURCE, builder -> {
      try {
        super.classdef(cgenc);
        cgenc.convertMethods(builder);
      } catch (ProgramDefinitionError e) {
        error[0] = e;
      }
    });

    if (error[0] != null) {
      throw error[0];
    }
  }

}
