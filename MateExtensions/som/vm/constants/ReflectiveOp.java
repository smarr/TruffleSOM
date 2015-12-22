package som.vm.constants;

public enum ReflectiveOp {
  ExecutorReadField, ExecutorWriteField, ExecutorLocalArg, ExecutorNonLocalArg, 
    ExecutorLocalSuperArg, ExecutorNonLocalSuperArg, ExecutorReadLocal, ExecutorWriteLocal,  
  MessageLookup, MessageActivation, 
  LayoutReadField, LayoutWriteField, 
  None
}