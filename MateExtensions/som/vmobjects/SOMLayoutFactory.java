package som.vmobjects;

import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.LayoutBuilder;
import com.oracle.truffle.api.object.LayoutFactory;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.object.PropertyImpl;
import com.oracle.truffle.object.basic.DefaultStrategy;

public class SOMLayoutFactory implements LayoutFactory {
  @Override
  public Layout createLayout(LayoutBuilder layoutBuilder) {
    return SOMLayout.createLayoutImpl(layoutBuilder.getAllowedImplicitCasts(), new DefaultStrategy());
  }

  @Override
  public Property createProperty(Object id, Location location) {
    return this.createProperty(id,location,0);
  }

  @Override
  public Property createProperty(Object id, Location location, int flags) {
    return new PropertyImpl(id, location, flags);
  }

  @Override
  public int getPriority() {
    return 0;
  }
}
