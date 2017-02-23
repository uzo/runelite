/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.runelite.deob.deobfuscators.mapping;

import net.runelite.asm.ClassFile;
import net.runelite.asm.ClassGroup;
import net.runelite.asm.Method;
import net.runelite.asm.signature.Signature;
import net.runelite.asm.signature.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstructorMapper
{
	private static final Logger logger = LoggerFactory.getLogger(ConstructorMapper.class);
	
	private final ClassGroup source, target;
	private final ParallelExecutorMapping mapping;

	public ConstructorMapper(ClassGroup source, ClassGroup target, ParallelExecutorMapping mapping)
	{
		this.source = source;
		this.target = target;
		this.mapping = mapping;
	}
	
	private Type toOtherType(Type type)
	{
		if (type.isPrimitive())
		{
			return type;
		}

		String className = type.getType();
		className = className.substring(1, className.length() - 1); // remove L ;
		ClassFile cf = source.findClass(className);
		if (cf == null)
		{
			return type;
		}

		ClassFile other = (ClassFile) mapping.get(cf);
		return new Type("L" + other.getName() + ";");
	}
	
	private Signature toOtherSignature(Signature s)
	{
		Signature sig = new Signature();
		sig.setTypeOfReturnValue(toOtherType(s.getReturnValue()));
		for (int i = 0; i < s.size(); ++i)
			sig.addArg(toOtherType(s.getTypeOfArg(i)));
		return sig;
	}
	
	/**
	 * Map constructors based on the class mappings of the given mapping
	 */
	public void mapConstructors()
	{
		for (ClassFile cf : source.getClasses())
		{
			ClassFile other = (ClassFile) mapping.get(cf);

			if (other == null)
				continue;

			for (Method m : cf.getMethods().getMethods())
			{
				if (!m.getName().equals("<init>"))
					continue;
				
				Signature otherSig = toOtherSignature(m.getDescriptor());
				
				logger.debug("Converted signature {} -> {}", m.getDescriptor(), otherSig);

				Method m2 = other.findMethod(m.getName(), otherSig);
				if (m2 == null)
				{
					logger.warn("Unable to find other constructor for {}, looking for signature {} on class {}", m, otherSig, other);
					continue;
				}

				ParallelExecutorMapping p = MappingExecutorUtil.map(m, m2);
				p.map(null, m, m2);

				mapping.merge(p);
			}
		}
	}
}
