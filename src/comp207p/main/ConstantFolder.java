package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.*;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.*;

public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void optimize()
	{
		ClassGen classGen = new ClassGen(original);
		ConstantPoolGen constPoolGen = classGen.getConstantPool();
		Method[] methods = classGen.getMethods();
		System.out.println("Working on " + classGen.getClassName());
		for (Method method : methods) {
			MethodGen methodGen = new MethodGen(method, classGen.getClassName(), constPoolGen);
			InstructionList il = methodGen.getInstructionList() ;
			System.out.println("Now optimizing method: " + method );
			displayByteCode(il);
            applySimpleFoldingToAMethod( il , methodGen , constPoolGen );
            applyVariableFoldingToAMethod( methodGen , constPoolGen );
			System.out.println("We optimized method: " + method );
            displayByteCode(il);
            Method newMethod = methodGen.getMethod();
            classGen.replaceMethod(method,newMethod);
		}
		this.optimized = classGen.getJavaClass();
	}
	public void applySimpleFoldingToAMethod( InstructionList il , MethodGen methodGen , ConstantPoolGen constPoolGen)
	{
		boolean finished = false ;
		String pat = "LDC LDC ArithmeticInstruction" ;
		while ( finished == false )
		{
		        InstructionFinder finder = new InstructionFinder(il);
                finished = true ;
		        for(Iterator it = finder.search(pat); it.hasNext();) { // Iterate through instructions to look for arithmetic optimisation
                    finished = false ;
					InstructionHandle[] match = (InstructionHandle[]) it.next();
					InstructionHandle firstPart , secondPart , operation ;
					Number leftValue, rightValue;
					firstPart = match [ 0 ] ;
					secondPart = match [ 1 ] ;
					operation = match [ 2 ] ;
					leftValue = (Number) ((LDC) firstPart.getInstruction()).getValue(constPoolGen) ;
					rightValue = (Number) ((LDC) secondPart.getInstruction()).getValue(constPoolGen) ;
					ArithmeticInstruction op = (ArithmeticInstruction) operation.getInstruction();
					System.out.println("First number is: " + leftValue + " and second :  " + rightValue + " Operation: " + op.getName().toString() ) ;
					computeArithmeticalExpression( leftValue , rightValue , op , constPoolGen , firstPart );
					try {
						il.delete(secondPart);
						il.delete(operation);
						System.out.println("Am obtinut : " + (Number) ((LDC) firstPart.getInstruction()).getValue(constPoolGen) ) ;
					}
					catch (TargetLostException e)
					{
						e.printStackTrace();
					}

		        }
		}
	}
	public void displayByteCode(InstructionList il )
	{
		Instruction[] instruction = il.getInstructions() ;
		for ( Instruction instruction1 : instruction )
		{
			System.out.println(instruction1.toString(false));
		}
	}
	public void computeArithmeticalExpression ( Number a , Number b , ArithmeticInstruction arithmeticalInstruction , ConstantPoolGen constantPoolGen , InstructionHandle instructionHandle ) {
		String code = arithmeticalInstruction.getName().toString();
		if ( code.contains("add") ) {
             if ( code.charAt(0) == 'i' )
			 {
			 	 Integer first = a.intValue();
			 	 Integer second = b.intValue();
			 	 Integer result = first + second ;
			 	 int pos = constantPoolGen.addInteger(result);
				 LDC newInstruction = new LDC(pos);
				 instructionHandle.setInstruction(newInstruction);
			 	 System.out.println("Rezultatul adunarii : " + first + '+' + second + " inserat in constant pool la : " + pos + " este: " + result ) ;
			 }
			 if ( code.charAt(0) == 'd' )
			 {
			 	 Double first = a.doubleValue();
			 	 Double second = b.doubleValue();

			 }
		}
		else if ( code.contains("mul") )
		{

     	}
     	else if ( code.contains("sub") )
		{

		}
		else if ( code.contains("div") )
		{

		}
	}
	public void applyVariableFoldingToAMethod(MethodGen methodGen , ConstantPoolGen constPoolGen )
	{
		String pushInstruction = "ConstantPushInstruction" ;
		InstructionList il = methodGen.getInstructionList() ;
		InstructionFinder finder = new InstructionFinder(il);
		System.out.println("We are now applying variable folding ..." ) ;
		for(Iterator it = finder.search(pushInstruction); it.hasNext();) { // Iterate through instructions to look for arithmetic optimisation
			InstructionHandle[] match = (InstructionHandle[]) it.next();
			ConstantPushInstruction firstPart = (ConstantPushInstruction) match [ 0 ].getInstruction() ;
			System.out.println("The instruction is " + match[0] + " The constant part " + firstPart.getValue() ) ;
			int ind =  constPoolGen.addInteger(firstPart.getValue().intValue());
			LDC newInstruction = new LDC(ind);
			match [ 0 ] .setInstruction(newInstruction);
			System.out.println( " The new instruction is " + match [ 0 ] ) ;
		}
		System.out.println("")
	}
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}