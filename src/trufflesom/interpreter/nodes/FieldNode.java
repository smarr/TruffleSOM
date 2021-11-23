/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
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
package trufflesom.interpreter.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.nodes.PreevaluatedExpression;
import trufflesom.compiler.Variable.Argument;
import trufflesom.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import trufflesom.interpreter.nodes.FieldNodeFactory.FieldWriteNodeGen;
import trufflesom.interpreter.objectstorage.FieldAccessorNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractReadFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.AbstractWriteFieldNode;
import trufflesom.interpreter.objectstorage.FieldAccessorNode.IncrementLongFieldNode;
import trufflesom.vm.NotYetImplementedException;
import trufflesom.vmobjects.SObject;


public abstract class FieldNode extends ExpressionNode {

  public abstract ExpressionNode getSelf();

  public static final class FieldReadNode extends FieldNode
      implements PreevaluatedExpression {
    @Child private ExpressionNode        self;
    @Child private AbstractReadFieldNode read;

    public FieldReadNode(final ExpressionNode self, final int fieldIndex) {
      this.self = self;
      read = FieldAccessorNode.createRead(fieldIndex);
    }

    public int getFieldIndex() {
      return read.getFieldIndex();
    }

    @Override
    public ExpressionNode getSelf() {
      return self;
    }

    public Object executeEvaluated(final SObject obj) {
      return read.read(obj);
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated((SObject) arguments[0]);
    }

    @Override
    public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
      SObject obj = (SObject) self.executeGeneric(frame);
      return read.readLong(obj);
    }

    @Override
    public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
      SObject obj = (SObject) self.executeGeneric(frame);
      return read.readDouble(obj);
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      SObject obj = (SObject) self.executeGeneric(frame);
      return executeEvaluated(obj);
    }

    @Override
    public boolean isTrivial() {
      return true;
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      FieldReadNode node = (FieldReadNode) copy();
      node.self = null;
      node.read = (AbstractReadFieldNode) node.read.deepCopy();
      return node;
    }
  }

  @NodeChild(value = "self", type = ExpressionNode.class)
  @NodeChild(value = "value", type = ExpressionNode.class)
  public abstract static class FieldWriteNode extends FieldNode
      implements PreevaluatedExpression {
    @Child private AbstractWriteFieldNode write;

    public FieldWriteNode(final int fieldIndex) {
      write = FieldAccessorNode.createWrite(fieldIndex);
    }

    public int getFieldIndex() {
      return write.getFieldIndex();
    }

    public abstract ExpressionNode getValue();

    @Override
    public boolean isTrivial() {
      ExpressionNode val = getValue();
      // can't be a NonLocalArgumentReadNode, then it wouldn't be a setter
      // can't be a super access either. So that's why we have the == compare here
      return val.getClass() == LocalArgumentReadNode.class;
    }

    @Override
    public boolean isTrivialInSequence() {
      return isTrivial();
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      return new WriteAndReturnSelf(
          FieldWriteNodeGen.create(write.getFieldIndex(), null, null));
    }

    public final Object executeEvaluated(final VirtualFrame frame,
        final SObject self, final Object value) {
      return write.write(self, value);
    }

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated(frame, (SObject) arguments[0], arguments[1]);
    }

    @Specialization
    public long doLong(final VirtualFrame frame, final SObject self,
        final long value) {
      return write.write(self, value);
    }

    @Specialization
    public double doDouble(final VirtualFrame frame, final SObject self,
        final double value) {
      return write.write(self, value);
    }

    @Specialization
    public Object doObject(final VirtualFrame frame, final SObject self,
        final Object value) {
      return executeEvaluated(frame, self, value);
    }

    public static ExpressionNode createForMethod(final int fieldIdx, final Argument self,
        final Argument val) {
      FieldWriteNode node = FieldWriteNodeGen.create(
          fieldIdx,
          new LocalArgumentReadNode(self),
          new LocalArgumentReadNode(val));
      return new WriteAndReturnSelf(node);
    }
  }

  public static final class UninitFieldIncNode extends FieldNode {

    @Child private ExpressionNode self;
    private final int             fieldIndex;

    public UninitFieldIncNode(final ExpressionNode self, final int fieldIndex,
        final SourceSection source) {
      this.self = self;
      this.fieldIndex = fieldIndex;
      this.sourceSection = source;
    }

    @Override
    public ExpressionNode getSelf() {
      return self;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      SObject obj = (SObject) self.executeGeneric(frame);

      Object val = obj.getField(fieldIndex);
      if (!(val instanceof Long)) {
        throw new NotYetImplementedException();
      }

      long longVal = 0;
      try {
        longVal = Math.addExact((Long) val, 1);
        obj.setField(fieldIndex, longVal);
      } catch (ArithmeticException e) {
        throw new NotYetImplementedException();
      }

      IncrementLongFieldNode node = FieldAccessorNode.createIncrement(fieldIndex, obj);
      replace(new IncFieldNode(self, node, sourceSection));
      return longVal;
    }
  }

  private static final class IncFieldNode extends FieldNode {
    @Child private ExpressionNode         self;
    @Child private IncrementLongFieldNode inc;

    IncFieldNode(final ExpressionNode self, final IncrementLongFieldNode inc,
        final SourceSection source) {
      this.self = self;
      this.inc = inc;
      this.sourceSection = source;
    }

    @Override
    public ExpressionNode getSelf() {
      return self;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return executeLong(frame);
    }

    @Override
    public long executeLong(final VirtualFrame frame) {
      SObject obj = (SObject) self.executeGeneric(frame);
      return inc.increment(obj);
    }
  }

  public static final class WriteAndReturnSelf extends ExpressionNode
      implements PreevaluatedExpression {
    @Child FieldWriteNode write;

    WriteAndReturnSelf(final FieldWriteNode write) {
      this.write = write;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
      write.doPreEvaluated(frame, args);
      return args[0];
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return doPreEvaluated(frame, frame.getArguments());
    }

    @Override
    public boolean isTrivial() {
      return true;
    }

    @Override
    public PreevaluatedExpression copyTrivialNode() {
      return (PreevaluatedExpression) deepCopy();
    }
  }
}
