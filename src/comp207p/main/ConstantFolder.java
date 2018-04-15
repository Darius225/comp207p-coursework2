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

	private static final String loadInstructionRegex = "(ConstantPushInstruction|LoadInstruction|LDC|LDC2_W)";

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
			System.out.println("Now optimizing method: " + method );
			optimizeMethod(classGen, constPoolGen, method);
            //applySimpleFoldingToAMethod( methodGen , constPoolGen );
            //Method newMethod = methodGen.getMethod();
            //classGen.replaceMethod(method,newMethod);
		}
		this.optimized = classGen.getJavaClass();
	}

	private void optimizeMethod(ClassGen classGen, ConstantPoolGen constPoolGen, Method method)
	{
		Code methodCode = method.getCode();

		InstructionList instructionList = new InstructionList(methodCode.getCode());

		MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), classGen.getClassName(), instructionList, constPoolGen);

		int optimizeCont = 1;

		//Run in while loop until all possible optimisations have been made

		while(optimizeCont > 0)
		{
			optimizeCont = 0;
		}

		//checks that jump handles are within the current method
		instructionList.setPositions(true);

		//set max locals/stack
		methodGen.setMaxLocals();
		methodGen.setMaxStack();

		//generate new method with optimized instructions
		Method newMethod = methodGen.getMethod();

		//replace the old method with the optimized one
		classGen.replaceMethod(method, newMethod);
	}

	public void applySimpleFoldingToAMethod( MethodGen methodGen , ConstantPoolGen constPoolGen)
	{
		boolean finished = false ;
		InstructionList il = methodGen.getInstructionList() ;
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