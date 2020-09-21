package net.fabricmc.loader.transformer.deprecation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public final class EntrypointRemapClassVisitor extends ClassVisitor {
	private final String originalClass;
	private final String newClass;

	public EntrypointRemapClassVisitor(int api, ClassVisitor visitor, String originalClassLdc, String newClassLdc) {
		super(api, visitor);
		this.originalClass = originalClassLdc;
		this.newClass = newClassLdc;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new Methods(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return super.visitField(access, name, descriptor, signature, value); // TODO: Change type value
	}

	class Methods extends MethodVisitor {
		Methods(MethodVisitor method) {
			super(EntrypointRemapClassVisitor.this.api, method);
		}

		@Override
		public void visitLdcInsn(Object value) {
			super.visitLdcInsn(value.equals(EntrypointRemapClassVisitor.this.originalClass) ? EntrypointRemapClassVisitor.this.newClass : value);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			super.visitMethodInsn(opcode, owner.equals(EntrypointRemapClassVisitor.this.originalClass) ? EntrypointRemapClassVisitor.this.newClass : owner, name, descriptor, isInterface);
		}
	}
}
