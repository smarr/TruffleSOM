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
import trufflesom.vm.Globals;
import trufflesom.vm.Globals.Association;
import trufflesom.vm.SymbolTable;
import trufflesom.vmobjects.SObject;
import trufflesom.vmobjects.SSymbol;


public abstract class CollisionDetectorClass {
  /**
   * <pre>
   *   isInVoxel: voxel motion: motion = (
        | init fin v_s r v_x x0 xv v_y y0 yv low_x high_x low_y high_y |
        (voxel x > Constants MaxX or: [
         voxel x < Constants MinX or: [
         voxel y > Constants MaxY or: [
         voxel y < Constants MinY ]]]) ifTrue: [ ^ false ].
  
        init := motion posOne.
        fin  := motion posTwo.
  
        v_s := Constants GoodVoxelSize.
        r   := Constants ProximityRadius // 2.0.
  
        v_x := voxel x.
        x0  := init x.
        xv  := fin x - init x.
  
        v_y := voxel y.
        y0  := init y.
        yv  := fin y - init y.
  
        low_x  := (v_x - r - x0) // xv.
        high_x := (v_x + v_s + r - x0) // xv.
  
        xv < 0.0 ifTrue: [
          | tmp |
          tmp    := low_x.
          low_x  := high_x.
          high_x := tmp ].
  
        low_y  := (v_y - r - y0) // yv.
        high_y := (v_y + v_s + r - y0) // yv.
  
        yv < 0.0 ifTrue: [
          | tmp |
          tmp    := low_y.
          low_y  := high_y.
          high_y := tmp ].
  
        ^ (((xv = 0.0 and: [v_x <= (x0 + r) and: [(x0 - r) <= (v_x + v_s)]]) or: [ "no motion in x"
            (low_x <= 1.0 and: [1.0 <= high_x]) or: [
            (low_x <= 0.0 and: [0.0 <= high_x]) or: [
            (0.0 <= low_x and: [high_x <= 1.0])]]]) and: [
  
            (yv = 0.0 and: [v_y <= (y0 + r) and: [(y0 - r) <= (v_y + v_s)]]) or: [ "no motion in y"
              (low_y <= 1.0 and: [1.0 <= high_y]) or: [
              (low_y <= 0.0 and: [0.0 <= high_y]) or: [
              (0.0   <= low_y and: [high_y <= 1.0])]]]]) and: [
  
             xv = 0.0 or: [
             yv = 0.0 or: [ "no motion in x or y or both"
             (low_y <= high_x and: [high_x <= high_y]) or: [
             (low_y <= low_x  and: [low_x <= high_y]) or: [
             (low_x <= low_y  and: [high_y <= high_x]) ]]]]]
      )
   * </pre>
   */
  public static final class CDIsInVoxel extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchVoxelX;
    @Child private AbstractDispatchNode dispatchVoxelY;
    @Child private AbstractDispatchNode dispatchConstantsMaxX;
    @Child private AbstractDispatchNode dispatchConstantsMinX;
    @Child private AbstractDispatchNode dispatchConstantsMaxY;
    @Child private AbstractDispatchNode dispatchConstantsMinY;
    @Child private AbstractDispatchNode dispatchConstantsGoodVoxelSize;
    @Child private AbstractDispatchNode dispatchConstantsProximityRadius;

    @Child private AbstractDispatchNode dispatchMotionPosOne;
    @Child private AbstractDispatchNode dispatchMotionPosTwo;

    @CompilationFinal Association globalConstants;

    public CDIsInVoxel(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchVoxelX = new UninitializedDispatchNode(SymbolTable.symbolFor("x"));
      dispatchVoxelY = new UninitializedDispatchNode(SymbolTable.symbolFor("y"));
      dispatchConstantsMaxX = new UninitializedDispatchNode(SymbolTable.symbolFor("MaxX"));
      dispatchConstantsMinX = new UninitializedDispatchNode(SymbolTable.symbolFor("MinX"));
      dispatchConstantsMaxY = new UninitializedDispatchNode(SymbolTable.symbolFor("MaxY"));
      dispatchConstantsMinY = new UninitializedDispatchNode(SymbolTable.symbolFor("MinY"));

      dispatchConstantsGoodVoxelSize =
          new UninitializedDispatchNode(SymbolTable.symbolFor("GoodVoxelSize"));
      dispatchConstantsProximityRadius =
          new UninitializedDispatchNode(SymbolTable.symbolFor("ProximityRadius"));

      dispatchMotionPosOne = new UninitializedDispatchNode(SymbolTable.symbolFor("posOne"));
      dispatchMotionPosTwo = new UninitializedDispatchNode(SymbolTable.symbolFor("posTwo"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object voxel = args[1];
      Object motion = args[2];

      // Constants
      if (globalConstants == null) {
        lookupConstants(rcvr);
      }

      /*
       * (voxel x > Constants MaxX or: [
       * voxel x < Constants MinX or: [
       * voxel y > Constants MaxY or: [
       * voxel y < Constants MinY ]]]) ifTrue: [ ^ false ].
       */

      if ((Double) dispatchVoxelX.executeDispatch(frame,
          new Object[] {voxel}) > (Double) dispatchConstantsMaxX.executeDispatch(frame,
              new Object[] {globalConstants.getValue()})) {
        return false;
      }
      if ((Double) dispatchVoxelX.executeDispatch(frame,
          new Object[] {voxel}) < (Double) dispatchConstantsMinX.executeDispatch(frame,
              new Object[] {globalConstants.getValue()})) {
        return false;
      }
      if ((Double) dispatchVoxelY.executeDispatch(frame,
          new Object[] {voxel}) > (Double) dispatchConstantsMaxY.executeDispatch(frame,
              new Object[] {globalConstants.getValue()})) {
        return false;
      }
      if ((Double) dispatchVoxelY.executeDispatch(frame,
          new Object[] {voxel}) < (Double) dispatchConstantsMinY.executeDispatch(frame,
              new Object[] {globalConstants.getValue()})) {
        return false;
      }

      // init := motion posOne.
      Object init = dispatchMotionPosOne.executeDispatch(frame, new Object[] {motion});

      // fin := motion posTwo.
      Object fin = dispatchMotionPosTwo.executeDispatch(frame, new Object[] {motion});

      // v_s := Constants GoodVoxelSize.
      double v_s = (Double) dispatchConstantsGoodVoxelSize.executeDispatch(frame,
          new Object[] {globalConstants.getValue()});

      // r := Constants ProximityRadius // 2.0.
      double r = (Double) dispatchConstantsProximityRadius.executeDispatch(frame,
          new Object[] {globalConstants.getValue()}) / 2.0d;

      // v_x := voxel x.
      double v_x = (Double) dispatchVoxelX.executeDispatch(frame, new Object[] {voxel});
      // x0 := init x.
      double x0 = (Double) dispatchVoxelX.executeDispatch(frame, new Object[] {init});

      // xv := fin x - init x.
      double xv = (Double) dispatchVoxelX.executeDispatch(frame, new Object[] {fin})
          - (Double) dispatchVoxelX.executeDispatch(frame, new Object[] {init});

      // v_y := voxel y.
      double v_y = (Double) dispatchVoxelY.executeDispatch(frame, new Object[] {voxel});

      // y0 := init y.
      double y0 = (Double) dispatchVoxelY.executeDispatch(frame, new Object[] {init});

      // yv := fin y - init y.
      double yv = (Double) dispatchVoxelY.executeDispatch(frame, new Object[] {fin})
          - (Double) dispatchVoxelY.executeDispatch(frame, new Object[] {init});

      // low_x := (v_x - r - x0) // xv.
      double low_x = (v_x - r - x0) / xv;

      // high_x := (v_x + v_s + r - x0) // xv.
      double high_x = (v_x + v_s + r - x0) / xv;

      // xv < 0.0 ifTrue: [
      // | tmp |
      // tmp := low_x.
      // low_x := high_x.
      // high_x := tmp ].

      if (xv < 0.0d) {
        double tmp = low_x;
        low_x = high_x;
        high_x = tmp;
      }

      // low_y := (v_y - r - y0) // yv.
      double low_y = (v_y - r - y0) / yv;

      // high_y := (v_y + v_s + r - y0) // yv.
      double high_y = (v_y + v_s + r - y0) / yv;

      // yv < 0.0 ifTrue: [
      // | tmp |
      // tmp := low_y.
      // low_y := high_y.
      // high_y := tmp ].

      if (yv < 0.0d) {
        double tmp = low_y;
        low_y = high_y;
        high_y = tmp;
      }

      // ^ (((xv = 0.0 and: [v_x <= (x0 + r) and: [(x0 - r) <= (v_x + v_s)]]) or: [
      return (((xv == 0.0d && (v_x <= (x0 + r) && ((x0 - r) <= (v_x + v_s)))) || (
      // no motion in x
      // (low_x <= 1.0 and: [1.0 <= high_x]) or: [
      (low_x <= 1.0d && (1.0 <= high_x)) || (
      // (low_x <= 0.0 and: [0.0 <= high_x]) or: [
      (low_x <= 0.0d && (0.0d <= high_x)) || (
      // (0.0 <= low_x and: [high_x <= 1.0])]]]) and: [
      (0.0d <= low_x && (high_x <= 1.0d)))))) && (
      // (yv = 0.0 and: [v_y <= (y0 + r) and: [(y0 - r) <= (v_y + v_s)]]) or: [
      (yv == 0.0d && (v_y <= (y0 + r) && ((y0 - r) <= (v_y + v_s)))) || (
      // no motion in y
      // (low_y <= 1.0 and: [1.0 <= high_y]) or: [
      (low_y <= 1.0d && (1.0d <= high_y)) || (
      // (low_y <= 0.0 and: [0.0 <= high_y]) or: [
      (low_y <= 0.0d && (0.0d <= high_y)) || (
      // (0.0 <= low_y and: [high_y <= 1.0])]]]]) and: [
      (0.0d <= low_y && (high_y <= 1.0d))))))) && (
      // xv = 0.0 or: [
      xv == 0.0d || (
      // yv = 0.0 or: [ "no motion in x or y or both"
      yv == 0.0d || (// no motion in x or y or both
      // (low_y <= high_x and: [high_x <= high_y]) or: [
      (low_y <= high_x && (high_x <= high_y)) || (
      // (low_y <= low_x and: [low_x <= high_y]) or: [
      (low_y <= low_x && (low_x <= high_y)) || (
      // (low_x <= low_y and: [high_y <= high_x]) ]]]]]
      (low_x <= low_y && (high_y <= high_x)))))));
    }

    private void lookupConstants(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("Constants");
      globalConstants = Globals.getGlobalsAssociation(sym);

      if (globalConstants == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalConstants = Globals.getGlobalsAssociation(sym);
      }
    }
  }

  /**
   * <pre>
   *   recurse: voxelMap seen: seen voxel: nextVoxel motion: motion = (
    (self isInVoxel: nextVoxel motion: motion) ifFalse: [ ^ self ].
    (seen at: nextVoxel put: true) = true ifTrue: [ ^ self ].

    self put: motion and: nextVoxel into: voxelMap.

    self recurse: voxelMap seen: seen voxel: (nextVoxel minus: Constants horizontal) motion: motion.
    self recurse: voxelMap seen: seen voxel: (nextVoxel plus:  Constants horizontal) motion: motion.
    self recurse: voxelMap seen: seen voxel: (nextVoxel minus: Constants vertical)   motion: motion.
    self recurse: voxelMap seen: seen voxel: (nextVoxel plus:  Constants vertical)   motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel minus: Constants horizontal) minus: Constants vertical) motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel minus: Constants horizontal) plus:  Constants vertical) motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel plus:  Constants horizontal) minus: Constants vertical) motion: motion.
    self recurse: voxelMap seen: seen voxel: ((nextVoxel plus:  Constants horizontal) plus:  Constants vertical) motion: motion.
  )
   * </pre>
   */
  public static final class CDRecurse extends AbstractInvokable {
    @Child private AbstractDispatchNode dispatchIsInVoxel;
    @Child private AbstractDispatchNode dispatchAtPut;
    @Child private AbstractDispatchNode dispatchPutAndInto;
    @Child private AbstractDispatchNode dispatchRecurse;
    @Child private AbstractDispatchNode dispatchMinus;
    @Child private AbstractDispatchNode dispatchPlus;
    @Child private AbstractDispatchNode dispatchHorizontal;
    @Child private AbstractDispatchNode dispatchVertical;

    @CompilationFinal Association globalConstants;

    public CDRecurse(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      dispatchIsInVoxel =
          new UninitializedDispatchNode(SymbolTable.symbolFor("isInVoxel:motion:"));
      dispatchAtPut = new UninitializedDispatchNode(SymbolTable.symbolFor("at:put:"));
      dispatchPutAndInto =
          new UninitializedDispatchNode(SymbolTable.symbolFor("put:and:into:"));
      dispatchRecurse =
          new UninitializedDispatchNode(SymbolTable.symbolFor("recurse:seen:voxel:motion:"));
      dispatchMinus =
          new UninitializedDispatchNode(SymbolTable.symbolFor("minus:"));
      dispatchPlus =
          new UninitializedDispatchNode(SymbolTable.symbolFor("plus:"));
      dispatchHorizontal =
          new UninitializedDispatchNode(SymbolTable.symbolFor("horizontal"));
      dispatchVertical =
          new UninitializedDispatchNode(SymbolTable.symbolFor("vertical"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object voxelMap = args[1];
      Object seen = args[2];
      Object nextVoxel = args[3];
      Object motion = args[4];

      if (!(Boolean) dispatchIsInVoxel.executeDispatch(frame,
          new Object[] {rcvr, nextVoxel, motion})) {
        return rcvr;
      }

      if (Boolean.TRUE == (dispatchAtPut.executeDispatch(frame,
          new Object[] {seen, nextVoxel, true}))) {
        return rcvr;
      }

      dispatchPutAndInto.executeDispatch(frame,
          new Object[] {rcvr, motion, nextVoxel, voxelMap});

      // Constants
      if (globalConstants == null) {
        lookupConstants(rcvr);
      }

      // self recurse: voxelMap seen: seen voxel: (nextVoxel minus: Constants horizontal)
      // motion: motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchMinus.executeDispatch(frame, new Object[] {
              nextVoxel,
              dispatchHorizontal.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: (nextVoxel plus: Constants horizontal)
      // motion: motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchPlus.executeDispatch(frame, new Object[] {
              nextVoxel,
              dispatchHorizontal.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: (nextVoxel minus: Constants vertical) motion:
      // motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchMinus.executeDispatch(frame, new Object[] {
              nextVoxel,
              dispatchVertical.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: (nextVoxel plus: Constants vertical) motion:
      // motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchPlus.executeDispatch(frame, new Object[] {
              nextVoxel,
              dispatchVertical.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: ((nextVoxel minus: Constants horizontal)
      // minus: Constants vertical) motion: motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchMinus.executeDispatch(frame, new Object[] {
              dispatchMinus.executeDispatch(frame, new Object[] {
                  nextVoxel,
                  dispatchHorizontal.executeDispatch(frame,
                      new Object[] {globalConstants.getValue()})}),
              dispatchVertical.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: ((nextVoxel minus: Constants horizontal)
      // plus: Constants vertical) motion: motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchPlus.executeDispatch(frame, new Object[] {
              dispatchMinus.executeDispatch(frame, new Object[] {
                  nextVoxel,
                  dispatchHorizontal.executeDispatch(frame,
                      new Object[] {globalConstants.getValue()})}),
              dispatchVertical.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: ((nextVoxel plus: Constants horizontal)
      // minus: Constants vertical) motion: motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchMinus.executeDispatch(frame, new Object[] {
              dispatchPlus.executeDispatch(frame, new Object[] {
                  nextVoxel,
                  dispatchHorizontal.executeDispatch(frame,
                      new Object[] {globalConstants.getValue()})}),
              dispatchVertical.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      // self recurse: voxelMap seen: seen voxel: ((nextVoxel plus: Constants horizontal) plus:
      // Constants vertical) motion: motion.
      dispatchRecurse.executeDispatch(frame, new Object[] {
          rcvr,
          voxelMap,
          seen,
          dispatchPlus.executeDispatch(frame, new Object[] {
              dispatchPlus.executeDispatch(frame, new Object[] {
                  nextVoxel,
                  dispatchHorizontal.executeDispatch(frame,
                      new Object[] {globalConstants.getValue()})}),
              dispatchVertical.executeDispatch(frame,
                  new Object[] {globalConstants.getValue()})}),
          motion
      });

      return rcvr;
    }

    private void lookupConstants(final Object rcvr) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SSymbol sym = SymbolTable.symbolFor("Constants");
      globalConstants = Globals.getGlobalsAssociation(sym);

      if (globalConstants == null) {
        GlobalNode.sendUnknownGlobalToMethodRcvr(rcvr, sym);
        globalConstants = Globals.getGlobalsAssociation(sym);
      }
    }
  }

  /**
   * <pre>
   *   | value |.
   *   compareTo: other = (
        ^ value = other value
            ifTrue:  [ 0 ]
            ifFalse: [
              value < other value ifTrue: [ -1 ] ifFalse: [ 1 ]]
      )
   * </pre>
   */
  public static final class CallSignCompareTo extends AbstractInvokable {
    @Child private AbstractDispatchNode  dispatchValue;
    @Child private AbstractReadFieldNode readValue;

    public CallSignCompareTo(final Source source, final long sourceCoord) {
      super(new FrameDescriptor(), source, sourceCoord);
      readValue = FieldAccessorNode.createRead(0);
      dispatchValue = new UninitializedDispatchNode(SymbolTable.symbolFor("value"));
    }

    @Override
    public Object execute(final VirtualFrame frame) {
      Object[] args = frame.getArguments();
      SObject rcvr = (SObject) args[0];
      Object other = args[1];

      long value = readValue.readLongSafe(rcvr);
      if (value == (Long) dispatchValue.executeDispatch(frame, new Object[] {other})) {
        return 0L;
      }

      value = readValue.readLongSafe(rcvr);
      if (value < (Long) dispatchValue.executeDispatch(frame, new Object[] {other})) {
        return -1L;
      }
      return 1L;
    }
  }
}
