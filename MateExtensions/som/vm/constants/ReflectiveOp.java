package som.vm.constants;

public enum ReflectiveOp {
  ExecutorReadField, ExecutorWriteField, ExecutorLocalArg, ExecutorNonLocalArg, 
    ExecutorLocalSuperArg, ExecutorNonLocalSuperArg,
  MessageLookup, MessageActivation, 
  LayoutReadField, LayoutWriteField, 
  None
}