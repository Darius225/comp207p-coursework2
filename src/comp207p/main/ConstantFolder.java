package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import comp207p.main.util.ValueLoader;
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
	private static final String negation = " (INEG|FNEG|LNEG|DNEG)";

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
			optimizeCont += optimizeNegations(instructionList, constPoolGen);
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

	private int optimizeNegations(InstructionList instructionList, ConstantPoolGen constantPoolGen)
	{
		int cont = 0;
		String regex = loadInstructionRegex + negation;

		InstructionFinder finder = new InstructionFinder(instructionList);

		//Iterate through instructions matching our pattern (negations) to optimize them
		for(Iterator it = finder.search(regex); it.hasNext();)
		{
			InstructionHandle[] match = (InstructionHandle[]) it.next();

			InstructionHandle loadInstruction = match[0];
			InstructionHandle negationInstruction = match[1];

			String type = getSignature(negationInstruction, constantPoolGen);

			Number value = getValue(loadInstruction, constantPoolGen, instructionList, type);

			//Dmul works with all data types, since all of them can be converted to a double value without loss of accuracy
			//Due to double being bigger
			Number negativeValue = foldOperation(new DMUL(), -1, value);

			int poolIndex = insert(negativeValue, type, constantPoolGen);

			replaceInstructionHandleWithLDC(loadInstruction, type, poolIndex);

			try {
				instructionList.delete(match[1]);
			} catch (TargetLostException e) {
				e.printStackTrace();
			}
			cont++;
		}

		return cont;
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

	private static Number foldOperation(ArithmeticInstruction instruction, Number left, Number right)
	{
		if(instruction instanceof IADD || instruction instanceof LADD)
		{
			return left.longValue() + right.longValue();
		}
		else if(instruction instanceof FADD || instruction instanceof  DADD)
		{
			return left.doubleValue() + right.doubleValue();
		}
		else if(instruction instanceof ISUB || instruction instanceof LSUB)
		{
			return left.longValue() - right.longValue();
		}
		else if(instruction instanceof FSUB || instruction instanceof  DSUB)
		{
			return left.doubleValue() - right.doubleValue();
		}
		else if(instruction instanceof IMUL || instruction instanceof  LMUL)
		{
			return left.longValue() * right.longValue();
		}
		else if(instruction instanceof FMUL || instruction instanceof  DMUL)
		{
			return left.doubleValue() * right.doubleValue();
		}
		else if(instruction instanceof IDIV || instruction instanceof  LDIV)
		{
			return left.longValue() / right.longValue();
		}
		else if(instruction instanceof FDIV || instruction instanceof  DDIV)
		{
			return left.doubleValue() / right.doubleValue();
		}
		else if(instruction instanceof IREM || instruction instanceof  LREM)
		{
			return left.longValue() % right.longValue();
		}
		else if(instruction instanceof FREM || instruction instanceof  DREM)
		{
			return left.doubleValue() % right.doubleValue();
		}
		else if(instruction instanceof IAND || instruction instanceof  LAND)
		{
			return left.longValue() & right.longValue();
		}
		else if(instruction instanceof IOR || instruction instanceof  LOR)
		{
			return left.longValue() | right.longValue();
		}
		else if(instruction instanceof IXOR || instruction instanceof  LXOR)
		{
			return left.longValue() ^ right.longValue();
		}
		else if(instruction instanceof ISHL || instruction instanceof  LSHL)
		{
			return left.longValue() << right.longValue();
		}
		else if(instruction instanceof ISHR || instruction instanceof  LSHR)
		{
			return left.longValue() >> right.longValue();
		}
		else
		{
			throw new RuntimeException("Instruction not supported");
		}
	}

	private String getSignature(InstructionHandle handle, ConstantPoolGen constantPoolGen)
	{
		Instruction instruction = handle.getInstruction();
		if(instruction instanceof TypedInstruction)
		{
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

			Type type = ((TypedInstruction) instruction).getType(constantPoolGen);

			return type.getSignature();
		}
		else
		{
			throw new RuntimeException("InstructioHandle is not a TypedInstruction, but : " + instruction.getClass());
		}
	}

	private static int insert(Number value, String type, ConstantPoolGen constantPoolGen)
	{
		switch(type) {
			case "J": return constantPoolGen.addLong(value.longValue());
			case "I": return constantPoolGen.addInteger(value.intValue());
			case "D": return constantPoolGen.addDouble(value.doubleValue());
			case "F": return constantPoolGen.addFloat(value.floatValue());
			case "S": return constantPoolGen.addInteger(value.intValue());
			case "B": return constantPoolGen.addInteger(value.intValue());
			default: throw new RuntimeException("Type is not defined");
		}
	}

	private static void replaceInstructionHandleWithLDC(InstructionHandle handle, String type, int poolIndex)
	{
		boolean supportedByLDC = type.equals("F") || type.equals("I") || type.equals("S");
		//bigger types are only suppoerted by LDC2_W
		Instruction newInstruction;
		if(supportedByLDC)
		{
			newInstruction = new LDC(poolIndex);
		}
		else
		{
			newInstruction = new LDC2_W(poolIndex);
		}
		handle.setInstruction(newInstruction);
	}

	public static Number getValue(InstructionHandle handle, ConstantPoolGen constantPoolGen, InstructionList instructionList, String type)
	{
			Instruction instruction = handle.getInstruction();

			if(instruction instanceof LoadInstruction)
				return ValueLoader.getLoadInstrValue(handle, constantPoolGen, instructionList, type);
			else
				return ValueLoader.getConstantValue(handle, constantPoolGen);
	}
}