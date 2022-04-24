package bdt.inlining;

import com.oracle.truffle.api.source.Source;

import bdt.basic.ProgramDefinitionError;
import bdt.inlining.ScopeBuilder;
import bdt.inlining.Variable;
import bdt.inlining.nodes.Inlinable;


public class TScopeBuilder implements ScopeBuilder<TScopeBuilder> {

  @Override
  public Variable<?> introduceTempForInlinedVersion(final Inlinable<TScopeBuilder> node,
      final long coord) throws ProgramDefinitionError {
    return null;
  }

  @Override
  public Source getSource() {
    return null;
  }
}
