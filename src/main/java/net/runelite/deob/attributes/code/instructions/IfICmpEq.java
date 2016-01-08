package net.runelite.deob.attributes.code.instructions;

import net.runelite.deob.attributes.code.InstructionType;
import net.runelite.deob.attributes.code.Instructions;
import net.runelite.deob.attributes.code.instruction.types.PushConstantInstruction;
import net.runelite.deob.execution.InstructionContext;
import net.runelite.deob.execution.StackContext;

public class IfICmpEq extends If
{
	public IfICmpEq(Instructions instructions, InstructionType type, int pc)
	{
		super(instructions, type, pc);
	}
	
	private static boolean isZero(StackContext s)
	{
		if (s.getPushed().getInstruction() instanceof PushConstantInstruction)
		{
			PushConstantInstruction pc = (PushConstantInstruction) s.getPushed().getInstruction();
			Object o = pc.getConstant().getObject();
			
			if (o instanceof Integer && (int) o == 0)
				return true;
		}
		
		return false;
	}
	
	@Override
	public boolean isSame(InstructionContext thisIc, InstructionContext otherIc)
	{
		if (super.isSame(thisIc, otherIc))
			return true;
		
		// check for other being ifeq and this has a constant 0
		if (otherIc.getInstruction() instanceof IfEq)
		{
			StackContext s1 = thisIc.getPops().get(0),
				s2 = thisIc.getPops().get(1);
			
			if (isZero(s1) || isZero(s2))
				return true;
		}
		
		return false;
	}
}
