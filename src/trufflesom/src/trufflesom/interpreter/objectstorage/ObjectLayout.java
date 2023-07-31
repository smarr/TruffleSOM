package trufflesom.interpreter.objectstorage;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import trufflesom.interpreter.objectstorage.StorageLocation.UnwrittenStorageLocation;
import trufflesom.vmobjects.SClass;
import trufflesom.vmobjects.SObject;


public final class ObjectLayout {
  private final SClass     forClass;
  private final Assumption latestLayoutForClass;

  private final int primitiveStorageLocationsUsed;
  private final int objectStorageLocationsUsed;
  private final int totalNumberOfStorageLocations;

  private final StorageLocation[] storageLocations;
  private final Class<?>[]        storageTypes;

  public ObjectLayout(final int numberOfFields, final SClass forClass) {
    this(new Class<?>[numberOfFields], forClass);
  }

  public ObjectLayout(final Class<?>[] knownFieldTypes, final SClass forClass) {
    CompilerAsserts.neverPartOfCompilation("Layouts should not be created in compiled code");

    this.forClass = forClass;
    this.latestLayoutForClass = Truffle.getRuntime().createAssumption();

    storageTypes = knownFieldTypes;
    totalNumberOfStorageLocations = knownFieldTypes.length;
    storageLocations = new StorageLocation[knownFieldTypes.length];

    int nextFreePrimIdx = 0;
    int nextFreeObjIdx = 0;

    for (int i = 0; i < totalNumberOfStorageLocations; i++) {
      Class<?> type = knownFieldTypes[i];

      StorageLocation storage;
      if (type == Long.class) {
        storage = StorageLocation.createForLong(i, nextFreePrimIdx);
        nextFreePrimIdx++;
      } else if (type == Double.class) {
        storage = StorageLocation.createForDouble(i, nextFreePrimIdx);
        nextFreePrimIdx++;
      } else if (type == Object.class) {
        storage = StorageLocation.createForObject(nextFreeObjIdx);
        nextFreeObjIdx++;
      } else {
        assert type == null;
        storage = new UnwrittenStorageLocation(i);
      }

      storageLocations[i] = storage;
    }

    primitiveStorageLocationsUsed = nextFreePrimIdx;
    objectStorageLocationsUsed = nextFreeObjIdx;
  }

  public boolean isValid() {
    return latestLayoutForClass.isValid();
  }

  public void checkIsLatest() throws InvalidAssumptionException {
    latestLayoutForClass.check();
  }

  public Assumption getAssumption() {
    return latestLayoutForClass;
  }

  public boolean layoutForSameClass(final ObjectLayout other) {
    // TODO: think we don't need this with new guard logic
    return forClass == other.forClass;
  }

  public boolean layoutForSameClass(final SClass clazz) {
    return forClass == clazz;
  }

  public int getNumberOfFields() {
    return storageTypes.length;
  }

  public ObjectLayout withGeneralizedField(final long fieldIndex) {
    return withGeneralizedField((int) fieldIndex);
  }

  public void invalidate() {
    latestLayoutForClass.invalidate();
  }

  public ObjectLayout withGeneralizedField(final int fieldIndex) {
    if (storageTypes[fieldIndex] == Object.class) {
      return this;
    } else {
      assert storageTypes[fieldIndex] != Object.class;
      Class<?>[] withGeneralizedField = storageTypes.clone();
      withGeneralizedField[fieldIndex] = Object.class;

      latestLayoutForClass.invalidate();
      return new ObjectLayout(withGeneralizedField, forClass);
    }
  }

  public ObjectLayout withInitializedField(final long fieldIndex, final Class<?> type) {
    Class<?> specType;
    if (type == Long.class || type == Double.class) {
      specType = type;
    } else {
      specType = Object.class;
    }
    return withInitializedField((int) fieldIndex, specType);
  }

  private ObjectLayout withInitializedField(final int fieldIndex, final Class<?> type) {
    if (storageTypes[fieldIndex] == type) {
      return this;
    } else {
      assert storageTypes[fieldIndex] == null;
      Class<?>[] withInitializedField = storageTypes.clone();
      withInitializedField[fieldIndex] = type;

      latestLayoutForClass.invalidate();
      return new ObjectLayout(withInitializedField, forClass);
    }
  }

  public StorageLocation getStorageLocation(final long fieldIndex) {
    return getStorageLocation((int) fieldIndex);
  }

  public StorageLocation getStorageLocation(final int fieldIndex) {
    return storageLocations[fieldIndex];
  }

  public int getNumberOfUsedExtendedObjectStorageLocations() {
    int requiredExtensionFields = objectStorageLocationsUsed - SObject.NUM_OBJECT_FIELDS;
    if (requiredExtensionFields < 0) {
      return 0;
    }
    return requiredExtensionFields;
  }

  public int getNumberOfUsedExtendedPrimStorageLocations() {
    int requiredExtensionFields = primitiveStorageLocationsUsed - SObject.NUM_PRIMITIVE_FIELDS;
    if (requiredExtensionFields < 0) {
      return 0;
    }
    return requiredExtensionFields;
  }
}
