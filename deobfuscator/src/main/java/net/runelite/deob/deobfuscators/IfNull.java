package net.runelite.deob.deobfuscators;

import net.runelite.asm.ClassGroup;
import net.runelite.deob.Deobfuscator;
import net.runelite.asm.attributes.code.Instruction;
import net.runelite.asm.attributes.code.Instructions;
import java.util.ArrayList;
import java.util.List;
import net.runelite.asm.attributes.code.instruction.types.LVTInstruction;
import net.runelite.asm.attributes.code.instructions.Goto;
import net.runelite.asm.attributes.code.instructions.If0;
import net.runelite.asm.attributes.code.instructions.Pop;
import net.runelite.asm.execution.Execution;
import net.runelite.asm.execution.Frame;
import net.runelite.asm.execution.InstructionContext;
import net.runelite.asm.execution.StackContext;
import net.runelite.asm.execution.Value;

public class IfNull implements Deobfuscator
{
	private int count;
	private final List<Instruction> interesting = new ArrayList<>(),
		nonInteresting = new ArrayList<>();

	private void visit(InstructionContext ictx)
	{
		if (ictx.getBranches().isEmpty() == false)
		{
			nonInteresting.addAll(interesting);
			interesting.clear();
		}
		
		if (!(ictx.getInstruction() instanceof net.runelite.asm.attributes.code.instructions.IfNull))
			return;

		StackContext s = ictx.getPops().get(0);
		if (s.getValue() != Value.NULL)
		{
			nonInteresting.add(ictx.getInstruction());
			interesting.remove(ictx.getInstruction());
			return;
		}

		InstructionContext ictx2 = s.getPushed().resolve(s);

		if (ictx2.getFrame() != ictx.getFrame())
		{
			nonInteresting.addAll(interesting);
			interesting.clear();
			return;
		}

		if (ictx2.getInstruction() instanceof LVTInstruction)
		{
			LVTInstruction lvt = (LVTInstruction) ictx2.getInstruction();
			int idx = lvt.getVariableIndex();
			if (ictx2.getVariables().get(idx).isIsParameter())
			{
				nonInteresting.add(ictx.getInstruction());
				interesting.remove(ictx.getInstruction());
				return;
			}
		}

		if (nonInteresting.contains(ictx.getInstruction()))
			return;

		interesting.add(ictx.getInstruction());
	}

	private void visit(Frame frame)
	{
		for (Instruction i : interesting)
		{
			Instructions ins = i.getInstructions();
			If0 if0 = (If0) i;

			int idx = ins.getInstructions().indexOf(i);
			assert idx != -1;

			ins.replace(i, new Pop(ins));
			ins.getInstructions().add(idx + 1, new Goto(ins, if0.getJumps().get(0)));
			++count;
		}

		interesting.clear();
		nonInteresting.clear();
	}

	@Override
	public void run(ClassGroup group)
	{
		Execution execution = new Execution(group);
		execution.addExecutionVisitor(i -> visit(i));
		execution.addFrameVisitor(i -> visit(i));
		execution.populateInitialMethods();
		execution.run();

		System.out.println("Removed " + count + " if null");
	}

}
