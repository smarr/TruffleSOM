package som.interpreter.objectstorage;

import som.vm.constants.Nil;

import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.LocationFactory;
import com.oracle.truffle.api.object.ObjectLocation;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.object.LayoutImpl;
import com.oracle.truffle.object.Locations;
import com.oracle.truffle.object.LocationImpl.InternalLongLocation;
import com.oracle.truffle.object.basic.BasicAllocator;


public class MateLocationFactory implements LocationFactory {
  @Override
  public Location createLocation(Shape shape, Object value) {
    return new Locations.DeclaredDualLocation(
          (InternalLongLocation) ((BasicAllocator)shape.allocator()).newLongLocation(false), 
          (ObjectLocation) ((BasicAllocator)shape.allocator()).newObjectLocation(false, false), 
          Nil.nilObject, 
          (LayoutImpl)shape.getLayout());
  }
}
