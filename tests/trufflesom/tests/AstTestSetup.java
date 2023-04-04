package trufflesom.tests;

import static trufflesom.vm.SymbolTable.symSelf;
import static trufflesom.vm.SymbolTable.symbolFor;

import org.junit.Ignore;

import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import trufflesom.compiler.ClassGenerationContext;
import trufflesom.compiler.MethodGenerationContext;
import trufflesom.compiler.ParserAst;
import trufflesom.interpreter.SomLanguage;
import trufflesom.interpreter.nodes.ExpressionNode;
import trufflesom.vmobjects.SInvokable.SMethod;


@Ignore("provides just setup")
public abstract class AstTestSetup extends TruffleTestSetup {
  protected MethodGenerationContext mgenc;

  protected ExpressionNode parseMethod(final String source) {
    Source s = SomLanguage.getSyntheticSource(source, "test");

    cgenc = new ClassGenerationContext(s, null);
    cgenc.setName(symbolFor("Test"));
    addAllFields();

    mgenc = new MethodGenerationContext(cgenc, probe);
    mgenc.addArgumentIfAbsent(symSelf, 0);

    ParserAst parser = new ParserAst(source, s, null);
    try {
      return parser.method(mgenc);
    } catch (ProgramDefinitionError e) {
      throw new RuntimeException(e);
    }
  }

  protected SMethod assembleLastMethod(final ExpressionNode body) {
    return (SMethod) mgenc.assemble(body, 0);
  }
}
