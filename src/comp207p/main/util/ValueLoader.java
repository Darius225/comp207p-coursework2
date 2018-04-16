package comp207p.main.util;

import org.apache.bcel.generic.*;

/**
 * Created by cosmi_owugxv5 on 4/15/2018.
 */
public class ValueLoader {

    public static Number getLoadInstrValue(InstructionHandle handle, ConstantPoolGen constantPoolGen, InstructionList instructionList, String type) {


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
