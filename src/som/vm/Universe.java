/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.vm;

import static som.vm.constants.Classes.arrayClass;
import static som.vm.constants.Classes.booleanClass;
import static som.vm.constants.Classes.classClass;
import static som.vm.constants.Classes.doubleClass;
import static som.vm.constants.Classes.integerClass;
import static som.vm.constants.Classes.metaclassClass;
import static som.vm.constants.Classes.methodClass;
import static som.vm.constants.Classes.nilClass;
import static som.vm.constants.Classes.objectClass;
import static som.vm.constants.Classes.primitiveClass;
import static som.vm.constants.Classes.stringClass;
import static som.vm.constants.Classes.symbolClass;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import som.compiler.Disassembler;
import som.interpreter.Invokable;
import som.interpreter.TruffleCompiler;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SInvokable.SPrimitive;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;

public final class Universe {

  /**
   * Associations are handles for globals with a fixed
   * SSymbol and a mutable value.
   */
  public static final class Association {
    private final SSymbol    key;
    @CompilationFinal private Object  value;

    public Association(final SSymbol key, final Object value) {
      this.key   = key;
      this.value = value;
    }

    public SSymbol getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(final Object value) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Changed global");
      this.value = value;
    }
  }

  public static void main(final String[] arguments) {
    Universe u = current();

    try {
      u.interpret(arguments);
      u.exit(0);
    } catch (IllegalStateException e) {
      errorExit(e.getMessage());
    }
  }

  public Object interpret(String[] arguments) {
    // Check for command line switches
    arguments = handleArguments(arguments);

    // Initialize the known universe
    return execute(arguments);
  }

  private Universe() {
    this.truffleRuntime = Truffle.getRuntime();
    this.globals      = new HashMap<SSymbol, Association>();
    this.symbolTable  = new HashMap<>();
    this.avoidExit    = false;
    this.alreadyInitialized = false;
    this.lastExitCode = 0;

    this.blockClasses = new DynamicObject[4];
  }

  public TruffleRuntime getTruffleRuntime() {
    return truffleRuntime;
  }

  public void exit(final int errorCode) {
    TruffleCompiler.transferToInterpreter("exit");
    // Exit from the Java system
    if (!avoidExit) {
      System.exit(errorCode);
    } else {
      lastExitCode = errorCode;
    }
  }

  public int lastExitCode() {
    return lastExitCode;
  }

  public static void errorExit(final String message) {
    TruffleCompiler.transferToInterpreter("errorExit");
    errorPrintln("Runtime Error: " + message);
    current().exit(1);
  }

  @TruffleBoundary
  public String[] handleArguments(String[] arguments) {
    boolean gotClasspath = false;
    String[] remainingArgs = new String[arguments.length];
    int cnt = 0;

    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i].equals("-cp")) {
        if (i + 1 >= arguments.length) {
          printUsageAndExit();
        }
        setupClassPath(arguments[i + 1]);
        // Checkstyle: stop
        ++i; // skip class path
        // Checkstyle: resume
        gotClasspath = true;
      } else if (arguments[i].equals("-d")) {
        printAST = true;
      } else {
        remainingArgs[cnt++] = arguments[i];
      }
    }

    if (!gotClasspath) {
      // Get the default class path of the appropriate size
      classPath = setupDefaultClassPath(0);
    }

    // Copy the remaining elements from the original array into the new
    // array
    arguments = new String[cnt];
    System.arraycopy(remainingArgs, 0, arguments, 0, cnt);

    // check remaining args for class paths, and strip file extension
    for (int i = 0; i < arguments.length; i++) {
      String[] split = getPathClassExt(arguments[i]);

      if (!("".equals(split[0]))) { // there was a path
        String[] tmp = new String[classPath.length + 1];
        System.arraycopy(classPath, 0, tmp, 1, classPath.length);
        tmp[0] = split[0];
        classPath = tmp;
      }
      arguments[i] = split[1];
    }

    return arguments;
  }

  @TruffleBoundary
  // take argument of the form "../foo/Test.som" and return
  // "../foo", "Test", "som"
  private String[] getPathClassExt(final String arg) {
    File file = new File(arg);

    String path = file.getParent();
    StringTokenizer tokenizer = new StringTokenizer(file.getName(), ".");

    if (tokenizer.countTokens() > 2) {
      errorPrintln("Class with . in its name?");
      exit(1);
    }

    String[] result = new String[3];
    result[0] = (path == null) ? "" : path;
    result[1] = tokenizer.nextToken();
    result[2] = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

    return result;
  }

  @TruffleBoundary
  public void setupClassPath(final String cp) {
    // Create a new tokenizer to split up the string of directories
    StringTokenizer tokenizer = new StringTokenizer(cp, File.pathSeparator);

    // Get the default class path of the appropriate size
    classPath = setupDefaultClassPath(tokenizer.countTokens());

    // Get the directories and put them into the class path array
    for (int i = 0; tokenizer.hasMoreTokens(); i++) {
      classPath[i] = tokenizer.nextToken();
    }
  }

  @TruffleBoundary
  private String[] setupDefaultClassPath(final int directories) {
    // Get the default system class path
    String systemClassPath = System.getProperty("system.class.path");

    // Compute the number of defaults
    int defaults = (systemClassPath != null) ? 2 : 1;

    // Allocate an array with room for the directories and the defaults
    String[] result = new String[directories + defaults];

    // Insert the system class path into the defaults section
    if (systemClassPath != null) {
      result[directories] = systemClassPath;
    }

    // Insert the current directory into the defaults section
    result[directories + defaults - 1] = ".";

    // Return the class path
    return result;
  }

  private void printUsageAndExit() {
    // Print the usage
    println("Usage: som [-options] [args...]                          ");
    println("                                                         ");
    println("where options include:                                   ");
    println("    -cp <directories separated by " + File.pathSeparator + ">");
    println("                  set search path for application classes");
    println("    -d            enable disassembling");

    // Exit
    System.exit(0);
  }

  /**
   * Start interpretation by sending the selector to the given class. This is
   * mostly meant for testing currently.
   *
   * @param className
   * @param selector
   * @return
   */
  public Object interpret(final String className, final String selector) {
    initializeObjectSystem();

    DynamicObject clazz = loadClass(symbolFor(className));

    // Lookup the initialize invokable on the system class
    SInvokable initialize = SClass.lookupInvokable(SObject.getSOMClass(clazz),
        symbolFor(selector));
    return initialize.invoke(clazz);
  }

  private Object execute(final String[] arguments) {
    initializeObjectSystem();

    // Start the shell if no filename is given
    if (arguments.length == 0) {
      Shell shell = new Shell(this);
      return shell.start();
    }

    // Lookup the initialize invokable on the system class
    SInvokable initialize = SClass.lookupInvokable(
        systemClass, symbolFor("initialize:"));

    return initialize.invoke(new Object[] {systemObject,
        SArray.create(arguments)});
  }

  protected void initializeObjectSystem() {
    CompilerAsserts.neverPartOfCompilation();
    if (alreadyInitialized) {
      return;
    } else {
      alreadyInitialized = true;
    }

    // Allocate the nil object
    DynamicObject nilObject = Nil.nilObject;

    // Setup the class reference for the nil object
    SObject.internalSetNilClass(nilObject, nilClass);

    // Initialize the system classes.
    initializeSystemClass(objectClass,            null, "Object");
    initializeSystemClass(classClass,      objectClass, "Class");
    initializeSystemClass(metaclassClass,   classClass, "Metaclass");
    initializeSystemClass(nilClass,        objectClass, "Nil");
    initializeSystemClass(arrayClass,      objectClass, "Array");
    initializeSystemClass(methodClass,     objectClass, "Method");
    initializeSystemClass(stringClass,     objectClass, "String");
    initializeSystemClass(symbolClass,     stringClass, "Symbol");
    initializeSystemClass(integerClass,    objectClass, "Integer");
    initializeSystemClass(primitiveClass,  objectClass, "Primitive");
    initializeSystemClass(doubleClass,     objectClass, "Double");
    initializeSystemClass(booleanClass,    objectClass, "Boolean");

    trueClass  = newSystemClass();
    falseClass = newSystemClass();

    initializeSystemClass(trueClass,      booleanClass, "True");
    initializeSystemClass(falseClass,     booleanClass, "False");

    // Load methods and fields into the system classes
    loadSystemClass(objectClass);
    loadSystemClass(classClass);
    loadSystemClass(metaclassClass);
    loadSystemClass(nilClass);
    loadSystemClass(arrayClass);
    loadSystemClass(methodClass);
    loadSystemClass(symbolClass);
    loadSystemClass(integerClass);
    loadSystemClass(primitiveClass);
    loadSystemClass(stringClass);
    loadSystemClass(doubleClass);
    loadSystemClass(booleanClass);
    loadSystemClass(trueClass);
    loadSystemClass(falseClass);

    // Load the generic block class
    blockClasses[0] = loadClass(symbolFor("Block"));

    // Setup the true and false objects
    trueObject  = SObject.create(trueClass);
    falseObject = SObject.create(falseClass);

    // Load the system class and create an instance of it
    systemClass  = loadClass(symbolFor("System"));
    systemObject = SObject.create(systemClass);

    // Put special objects into the dictionary of globals
    setGlobal("nil",    nilObject);
    setGlobal("true",   trueObject);
    setGlobal("false",  falseObject);
    setGlobal("system", systemObject);

    // Load the remaining block classes
    loadBlockClass(1);
    loadBlockClass(2);
    loadBlockClass(3);

    if (Globals.trueObject != trueObject) {
      errorExit("Initialization went wrong for class Globals");
    }

    if (null == blockClasses[1]) {
      errorExit("Initialization went wrong for class Blocks");
    }
    objectSystemInitialized = true;
  }

  @TruffleBoundary
  public SSymbol symbolFor(final String string) {
    String interned = string.intern();
    // Lookup the symbol in the symbol table
    SSymbol result = symbolTable.get(interned);
    if (result != null) { return result; }

    return newSymbol(interned);
  }

  public static SBlock newBlock(final SMethod method,
      final DynamicObject blockClass, final MaterializedFrame context) {
    return new SBlock(method, blockClass, context);
  }

  @TruffleBoundary
  public DynamicObject newClass(final DynamicObject classClass) {
    return SClass.create(classClass);
  }

  @TruffleBoundary
  public static SInvokable newMethod(final SSymbol signature,
      final Invokable truffleInvokable, final boolean isPrimitive,
      final SMethod[] embeddedBlocks) {
    if (isPrimitive) {
      return new SPrimitive(signature, truffleInvokable);
    } else {
      return new SMethod(signature, truffleInvokable, embeddedBlocks);
    }
  }

  @TruffleBoundary
  public static DynamicObject newMetaclassClass() {
    DynamicObject result = SClass.createWithoutClass();
    SClass.internalSetClass(result, SClass.create(result));
    return result;
  }

  private SSymbol newSymbol(final String string) {
    SSymbol result = new SSymbol(string);
    symbolTable.put(string, result);
    return result;
  }

  @TruffleBoundary
  public static DynamicObject newSystemClass() {
    return SClass.create(SClass.create(metaclassClass));
  }

  private void initializeSystemClass(final DynamicObject systemClass,
      final DynamicObject superClass, final String name) {
    // Initialize the superclass hierarchy
    if (superClass != null) {
      SClass.setSuperClass(systemClass, superClass);
      SClass.setSuperClass(SObject.getSOMClass(systemClass), SObject.getSOMClass(superClass));
    } else {
      SClass.setSuperClass(SObject.getSOMClass(systemClass), classClass);
    }

    // Initialize the array of instance fields
    SClass.setInstanceFields(systemClass, SArray.create(new Object[0]));
    SClass.setInstanceFields(SObject.getSOMClass(systemClass), SArray.create(new Object[0]));

    // Initialize the array of instance invokables
    SClass.setInstanceInvokables(systemClass, SArray.create(new Object[0]));
    SClass.setInstanceInvokables(SObject.getSOMClass(systemClass), SArray.create(new Object[0]));

    // Initialize the name of the system class
    SClass.setName(systemClass, symbolFor(name));
    SClass.setName(SObject.getSOMClass(systemClass), symbolFor(name + " class"));

    // Insert the system class into the dictionary of globals
    setGlobal(SClass.getName(systemClass), systemClass);
  }

  @TruffleBoundary
  public boolean hasGlobal(final SSymbol name) {
    return globals.containsKey(name);
  }

  @TruffleBoundary
  public Object getGlobal(final SSymbol name) {
    Association assoc = globals.get(name);
    if (assoc == null) {
      return null;
    }
    return assoc.getValue();
  }

  @TruffleBoundary
  public Association getGlobalsAssociation(final SSymbol name) {
    return globals.get(name);
  }

  public void setGlobal(final String name, final Object value) {
    setGlobal(symbolFor(name), value);
  }

  @TruffleBoundary
  public void setGlobal(final SSymbol name, final Object value) {
    Association assoc = globals.get(name);
    if (assoc == null) {
      assoc = new Association(name, value);
      globals.put(name, assoc);
    } else {
      assoc.setValue(value);
    }
  }

  public DynamicObject getBlockClass(final int numberOfArguments) {
    DynamicObject result = blockClasses[numberOfArguments];
    assert result != null || numberOfArguments == 0;
    return result;
  }

  private void loadBlockClass(final int numberOfArguments) {
    // Compute the name of the block class with the given number of
    // arguments
    SSymbol name = symbolFor("Block" + numberOfArguments);

    assert getGlobal(name) == null;

    // Get the block class for blocks with the given number of arguments
    DynamicObject result = loadClass(name, null);

    // Add the appropriate value primitive to the block class
    SClass.addInstancePrimitive(result, SBlock.getEvaluationPrimitive(
        numberOfArguments, this, result), true);

    // Insert the block class into the dictionary of globals
    setGlobal(name, result);

    blockClasses[numberOfArguments] = result;
  }

  @TruffleBoundary
  public DynamicObject loadClass(final SSymbol name) {
    // Check if the requested class is already in the dictionary of globals
    DynamicObject result = (DynamicObject) getGlobal(name);
    if (result != null) { return result; }

    result = loadClass(name, null);
    loadPrimitives(result, false);

    setGlobal(name, result);

    return result;
  }

  private void loadPrimitives(final DynamicObject result, final boolean isSystemClass) {
    if (result == null) { return; }

    // Load primitives if class defines them, or try to load optional
    // primitives defined for system classes.
    if (SClass.hasPrimitives(result) || isSystemClass) {
      SClass.loadPrimitives(result, !isSystemClass);
    }
  }

  @TruffleBoundary
  private void loadSystemClass(final DynamicObject systemClass) {
    // Load the system class
    DynamicObject result = loadClass(SClass.getName(systemClass), systemClass);

    if (result == null) {
      throw new IllegalStateException(SClass.getName(systemClass).getString()
          + " class could not be loaded. "
          + "It is likely that the class path has not been initialized properly. "
          + "Please set system property 'system.class.path' or "
          + "pass the '-cp' command-line parameter.");
    }

    loadPrimitives(result, true);
  }

  @TruffleBoundary
  private DynamicObject loadClass(final SSymbol name, final DynamicObject systemClass) {
    // Try loading the class from all different paths
    for (String cpEntry : classPath) {
      try {
        // Load the class from a file and return the loaded class
        DynamicObject result = som.compiler.SourcecodeCompiler.compileClass(cpEntry,
            name.getString(), systemClass, this);
        if (printAST) {
          Disassembler.dump(SObject.getSOMClass(result));
          Disassembler.dump(result);
        }
        return result;

      } catch (IOException e) {
        // Continue trying different paths
      }
    }

    // The class could not be found.
    return null;
  }

  @TruffleBoundary
  public DynamicObject loadShellClass(final String stmt) throws IOException {
    // Load the class from a stream and return the loaded class
    DynamicObject result = som.compiler.SourcecodeCompiler.compileClass(stmt,
        null, this);
    if (printAST) { Disassembler.dump(result); }
    return result;
  }

  public void setAvoidExit(final boolean value) {
    avoidExit = value;
  }

  @TruffleBoundary
  public static void errorPrint(final String msg) {
    // Checkstyle: stop
    System.err.print(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void errorPrintln(final String msg) {
    // Checkstyle: stop
    System.err.println(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void errorPrintln() {
    // Checkstyle: stop
    System.err.println();
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void print(final String msg) {
    // Checkstyle: stop
    System.out.print(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void println(final String msg) {
    // Checkstyle: stop
    System.out.println(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void println() {
    // Checkstyle: stop
    System.out.println();
    // Checkstyle: resume
  }

  public DynamicObject getTrueObject()   { return trueObject; }
  public DynamicObject getFalseObject()  { return falseObject; }
  public DynamicObject getSystemObject() { return systemObject; }

  public DynamicObject getTrueClass()   { return trueClass; }
  public DynamicObject getFalseClass()  { return falseClass; }
  public DynamicObject getSystemClass() { return systemClass; }

  @CompilationFinal private DynamicObject trueObject;
  @CompilationFinal private DynamicObject falseObject;
  @CompilationFinal private DynamicObject systemObject;

  @CompilationFinal private DynamicObject trueClass;
  @CompilationFinal private DynamicObject falseClass;
  @CompilationFinal private DynamicObject systemClass;

  private final HashMap<SSymbol, Association>   globals;

  private String[]                              classPath;
  @CompilationFinal private boolean             printAST;

  private final TruffleRuntime                  truffleRuntime;

  private final HashMap<String, SSymbol>        symbolTable;

  // TODO: this is not how it is supposed to be... it is just a hack to cope
  //       with the use of system.exit in SOM to enable testing
  @CompilationFinal private boolean             avoidExit;
  private int                                   lastExitCode;

  // Optimizations
  private final DynamicObject[] blockClasses;

  // Latest instance
  // WARNING: this is problematic with multiple interpreters in the same VM...
  @CompilationFinal private static Universe current;
  @CompilationFinal private boolean alreadyInitialized;

  @CompilationFinal public boolean objectSystemInitialized = false;

  public boolean isObjectSystemInitialized() {
    return objectSystemInitialized;
  }

  public static Universe current() {
    if (current == null) {
      current = new Universe();
    }
    return current;
  }
}
