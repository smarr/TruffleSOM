package trufflesom.interpreter.ubernodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;

import trufflesom.interpreter.AbstractInvokable;
import trufflesom.interpreter.nodes.GlobalNode;
import trufflesom.interpreter.nodes.dispatch.AbstractDispatchNode;
import trufflesom.interpreter.nodes.dispatch.UninitializedDispatchNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class RandomClass {
  /**
   * <pre>
   *  initialize = (
          seed := 74755.
      )
   * </pre>
   */
  public static final class RandomInitialize extends AbstractInvokable {
    @Child private AbstractWriteFieldNode writeSeed;

    public RandomInitialize(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      writeSeed = FieldAccessorNode.createWrite(0);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];
      writeSeed.write(rcvr, 74755);
      return rcvr;
    }
  }

  /**
   * <pre>
   * next = (
        seed := ((seed * 1309) + 13849) & 65535.
        ^seed
     )
   * </pre>
   */
  public static final class RandomNext extends AbstractInvokable {

    @Child private AbstractReadFieldNode  readSeed;
    @Child private AbstractWriteFieldNode writeSeed;

    public RandomNext(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      readSeed = FieldAccessorNode.createRead(0);
      writeSeed = FieldAccessorNode.createWrite(0);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];

      long seed = ((readSeed.readLongSafe(rcvr) * 1309L) + 13849L) & 65535L;
      writeSeed.write(rcvr, seed);
      return seed;
    }
  }

  /**
   * <pre>
   * next = ( ^self random next. )
   * </pre>
   */
  public static final class RandomClassNext extends AbstractInvokable {

    @Child private AbstractDispatchNode dispatchNext;
    @Child private AbstractDispatchNode dispatchRandom;

    public RandomClassNext(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      dispatchNext = new UninitializedDispatchNode(SymbolTable.symbolFor("next"));
      dispatchRandom = new UninitializedDispatchNode(SymbolTable.symbolFor("random"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];

      Object random = dispatchRandom.executeDispatch(frame, new Object[] {rcvr});
      return dispatchNext.executeDispatch(frame, new Object[] {random});
    }
  }

  /**
   * <pre>
    initialize = ( ^random := Random new. )
   * </pre>
   */
  public static final class RandomClassInitialize extends AbstractInvokable {
    @CompilationFinal Association globalRandom;

    @Child private AbstractDispatchNode   dispatchNew;
    @Child private AbstractWriteFieldNode writeRandom;

    public RandomClassInitialize(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);

      dispatchNew = new UninitializedDispatchNode(SymbolTable.symbolFor("new"));
      writeRandom = FieldAccessorNode.createWrite(0);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      SObject rcvr = (SObject) frame.getArguments()[0];

      if (globalRandom == null) {
        lookupRandom(rcvr);
      }

      Object random =
          dispatchNew.executeDispatch(frame, new Object[] {globalRandom.getValue()});
      writeRandom.write(rcvr, random);
      return random;
    }

    private void lookupRandom(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("Random");
      globalRandom = Globals.getGlobalsAssociation(sym);

      if (globalRandom == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalRandom = Globals.getGlobalsAssociation(sym);
      }
    }
  }
}
