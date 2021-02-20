package trufflesom.intepreter.objectstorage;

import java.util.Arrays;

import org.junit.Ignore;

import trufflesom.interpreter.objectstorage.StorageLocation;
import trufflesom.vm.constants.Nil;
import trufflesom.vmobjects.SObject;


@Ignore // Make sure JUnit doesn't fail, but ignores our custom test
public class BasicStorageTester {

  private static boolean someAssertionsFailed = false;

  private static final class STestObject extends SObject {

    STestObject(final int numFields) {
      super(numFields);

      this.extensionObjFields = new Object[numFields - SObject.NUM_OBJECT_FIELDS];
      this.extensionPrimFields = new long[numFields - SObject.NUM_PRIMITIVE_FIELDS];

      Arrays.fill(this.extensionObjFields, Nil.nilObject);
    }
  }

  public static void main(final String[] args) {
    System.out.println("BasicStorageTester start tests.");
    System.out.println();

    SObject obj = new STestObject(100);

    testDirectStorage(obj);

    testDirectDouble(obj);
    testDirectLong(obj);
    testDirectObject(obj);

    testExtDouble(obj);
    testExtLong(obj);
    testExtObject(obj);

    for (int i = 0; i < 100; i++) {
      SObject doubleObj = new STestObject(100);
      testDouble(doubleObj, i, i + 1111.11);
    }

    for (int i = 0; i < 100; i++) {
      SObject longObj = new STestObject(100);
      testLong(longObj, i, i + 222222);
    }

    for (int i = 0; i < 100; i++) {
      SObject objObj = new STestObject(100);
      testObject(objObj, i, i);
    }

    System.out.println("BasicStorageTester completed.");

    if (someAssertionsFailed) {
      System.out.println("Some tests failed.");
      System.exit(1);
    }
  }

  private static void testDirectStorage(final SObject obj) {
    System.out.println("\nStart testDirectStorage");
    System.out.println("\nLong Fields");
    for (int i = 0; i < SObject.NUM_PRIMITIVE_FIELDS; i++) {
      testLong(obj, i, i).debugPrint(obj);
    }

    System.out.println("\nDouble Fields");
    for (int i = 0; i < SObject.NUM_PRIMITIVE_FIELDS; i++) {
      testDouble(obj, i, i).debugPrint(obj);
    }

    System.out.println("\nObject Fields");
    for (int i = 0; i < SObject.NUM_PRIMITIVE_FIELDS; i++) {
      testObject(obj, i, i).debugPrint(obj);
    }

    System.out.println("Done testDirectStorage");
  }

  private static void testDirectDouble(final SObject obj) {
    StorageLocation sl = StorageLocation.createForDouble(null, 0, 0);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, 5.5);

    double value = (double) sl.read(obj);
    assertEquals(5.5, value);

    sl.debugPrint(obj);
  }

  private static void testDirectLong(final SObject obj) {
    StorageLocation sl = StorageLocation.createForLong(null, 1, 1);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, 32L);

    long value = (long) sl.read(obj);

    assertEquals(32L, value);
    sl.debugPrint(obj);
  }

  private static void testDirectObject(final SObject obj) {
    StorageLocation sl = StorageLocation.createForObject(null, 2);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, obj);

    Object value = sl.read(obj);

    assertIs(obj, value);
    sl.debugPrint(obj);
  }

  private static void testExtDouble(final SObject obj) {
    StorageLocation sl = StorageLocation.createForDouble(null, 0, 10);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, 5.5);

    double value = (double) sl.read(obj);

    assertEquals(5.5, value);
  }

  private static void testExtLong(final SObject obj) {
    StorageLocation sl = StorageLocation.createForLong(null, 1, 11);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, 32L);

    long value = (long) sl.read(obj);

    assertEquals(32, value);
  }

  private static void testExtObject(final SObject obj) {
    StorageLocation sl = StorageLocation.createForObject(null, 12);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, obj);

    Object value = sl.read(obj);

    assertIs(obj, value);
  }

  private static StorageLocation testDouble(final SObject obj, final int idx,
      final double value) {
    StorageLocation sl = StorageLocation.createForDouble(null, idx, idx);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, value);

    double readValue = (double) sl.read(obj);

    assertEquals(value, readValue);
    return sl;
  }

  private static StorageLocation testLong(final SObject obj, final int idx, final long value) {
    StorageLocation sl = StorageLocation.createForLong(null, idx, idx);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, value);

    long readValue = (long) sl.read(obj);

    assertEquals(value, readValue);
    return sl;
  }

  private static StorageLocation testObject(final SObject obj, final int idx,
      final Object value) {
    StorageLocation sl = StorageLocation.createForObject(null, idx);
    assertIsInitiallyNil(obj, sl);

    sl.write(obj, value);

    Object readValue = sl.read(obj);

    assertIs(value, readValue);
    return sl;
  }

  private static void assertIsInitiallyNil(final SObject obj, final StorageLocation sl) {
    Object nil = sl.read(obj);
    assertNil(nil);
  }

  public static void assertNil(final Object actual) {
    if (Nil.nilObject != actual) {
      System.out.println("Assert failed. Expected nil, but got actual: " + actual);
      someAssertionsFailed = true;
    }
  }

  public static void assertEquals(final long expected, final long actual) {
    if (expected != actual) {
      System.out.println("Assert failed. Expected: " + expected + " actual: " + actual);
      someAssertionsFailed = true;
    }
  }

  public static void assertEquals(final double expected, final double actual) {
    if (expected != actual) {
      System.out.println("Assert failed. Expected: " + expected + " actual: " + actual);
      someAssertionsFailed = true;
    }
  }

  public static void assertIs(final Object expected, final Object actual) {
    if (expected != actual) {
      System.out.println("Assert failed. Expected: " + expected + " actual: " + actual);
      someAssertionsFailed = true;
    }
  }
}
