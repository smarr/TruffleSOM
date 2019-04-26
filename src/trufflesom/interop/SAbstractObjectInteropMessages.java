package trufflesom.interop;

import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;

import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SAbstractObject;


@MessageResolution(receiverType = SAbstractObject.class)
public class SAbstractObjectInteropMessages {
  @Resolve(message = "IS_NULL")
  abstract static class NullCheckNode extends Node {

    public Object access(final SAbstractObject object) {
      return object == Nil.nilObject;
    }
  }
}
