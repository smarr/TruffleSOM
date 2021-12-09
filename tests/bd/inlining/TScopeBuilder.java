package bd.inlining;

import com.oracle.truffle.api.source.Source;

import bd.basic.ProgramDefinitionError;
import bd.inlining.nodes.Inlinable;


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
