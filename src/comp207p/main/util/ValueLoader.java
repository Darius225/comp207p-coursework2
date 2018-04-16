package comp207p.main.util;

import comp207p.main.ConstantFolder;
import org.apache.bcel.generic.*;

/**
 * Created by cosmi_owugxv5 on 4/15/2018.
 */
public class ValueLoader {

    public static Number getLoadInstrValue(InstructionHandle handle, ConstantPoolGen constantPoolGen) {
        Instruction instruction = handle.getInstruction();

        //index to identify the right store instruction
        int localIndex = ((LocalVariableInstruction) instruction).getIndex();
        //Iterate until the instruction is a store with the same variable index
        InstructionHandle iterator = handle;
        while(!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localIndex)
        {
            iterator = iterator.getPrev();
            instruction = iterator.getInstruction();
        }
        //At the end of loop, iterator will point to the latest store instruction relevant to current load instruction
        //This means the instruction before that is a push instruction

        iterator = iterator.getPrev();
        instruction = iterator.getInstruction();

        //code until this point is identical to the signature fetcher

        if(instruction instanceof ConstantPushInstruction)
        {
            return (((ConstantPushInstruction) instruction).getValue());
        }
        else if(instruction instanceof LDC)
        {
            //This needs to be casted to Number, as opposed to the other 2 (for some reason)
            return (Number) ((LDC) instruction).getValue(constantPoolGen);
        }
        else if(instruction instanceof LDC2_W)
        {
            return (((LDC2_W) instruction).getValue(constantPoolGen));
        }
        else
            throw new RuntimeException("Cannot get value for this kind of object:" + instruction.getClass());
    }

    public static Number getConstantValue(InstructionHandle handle, ConstantPoolGen constantPoolGen)
    {
        Instruction instruction = handle.getInstruction();
        if(instruction instanceof ConstantPushInstruction)
        {
            return  (((ConstantPushInstruction) instruction).getValue());
        }
        else if(instruction instanceof LDC)
        {
            //This needs to be casted to Number, as opposed to the other 2 (for some reason)
            return (Number) ((LDC) instruction).getValue(constantPoolGen);
        }
        else if(instruction instanceof LDC2_W)
        {
            return (((LDC2_W) instruction).getValue(constantPoolGen));
        }
        else
            throw new RuntimeException("Constant instruction not recognised: " + instruction.getClass());
    }
}
