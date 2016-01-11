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
package som.interpreter.nodes;

import som.interpreter.nodes.MateFieldNodesFactory.MateFieldReadNodeGen;
import som.interpreter.nodes.MateFieldNodesFactory.MateFieldWriteNodeGen;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.WriteFieldNode;
import som.vm.constants.ReflectiveOp;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public abstract class FieldNode extends ExpressionNode {
  protected FieldNode(final SourceSection source) {
    super(source);
  }
  
  public abstract ExpressionNode getSelf();
  
  @NodeChild(value = "self", type = ExpressionNode.class)
  public abstract static class FieldReadNode extends FieldNode {

    @Child protected ReadFieldNode read;

    protected FieldReadNode(final int fieldIndex, final SourceSection source) {
//    implements PreevaluatedExpression {       
      super(source);
      read = FieldAccessorNode.createRead(fieldIndex);
    }

    @Specialization
    public final Object executeEvaluated(final DynamicObject obj) {
      return read.executeRead(obj);
    }

    /*@Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated((DynamicObject) arguments[0]);
    }*/
    
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ExecutorReadField;
    }

    @Override
    public void wrapIntoMateNode() {
      replace(MateFieldReadNodeGen.create(this, this.getSelf()));
    }
  }

  @NodeChildren({
    @NodeChild(value = "self", type = ExpressionNode.class),
    @NodeChild(value = "value", type = ExpressionNode.class)})
  public abstract static class FieldWriteNode extends FieldNode {
//      implements PreevaluatedExpression {
    @Child protected WriteFieldNode write;

    public abstract ExpressionNode getValue();
    
    public FieldWriteNode(final int fieldIndex, final SourceSection source) {
      super(source);
      write = FieldAccessorNode.createWrite(fieldIndex);
    }

    @Specialization
    public final Object executeEvaluated(final DynamicObject self, final Object value) {
      return write.executeWrite(self, value);
    }

    /*@Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated((DynamicObject) arguments[0], arguments[1]);
    }*/
    
    @Override
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ExecutorWriteField;
    }
    
    @Override
    public void wrapIntoMateNode() {
      replace(MateFieldWriteNodeGen.create(this, this.getSelf(), this.getValue()));
    }
  }
}
