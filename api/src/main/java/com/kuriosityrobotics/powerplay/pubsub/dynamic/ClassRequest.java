package com.kuriosityrobotics.powerplay.pubsub.dynamic;

import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;

import java.io.Serializable;

@Hidden
public class ClassRequest implements Serializable {
	private final String className;

	public ClassRequest(String className) {
		this.className = className;
	}

	public String className() {
		return className;
	}

	@Override
	public String toString() {
		return "ClassRequest(" + className + ')';
	}

   @Override
   public boolean equals(Object obj) {
	  if (obj == this) return true;
	  if (!(obj instanceof ClassRequest)) return false;
	  return className.equals(((ClassRequest) obj).className);
   }

   @Override
   public int hashCode() {
	  return className.hashCode();
   }
}
