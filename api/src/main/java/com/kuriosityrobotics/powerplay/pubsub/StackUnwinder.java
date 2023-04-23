package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.util.ExceptionFunction;

import java.util.LinkedList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class StackUnwinder {
   static StackTraceElement[] unwind() {
	  return saveCurrent().getStackTrace();
   }

   static ThreadLocal<PublishStackElement> publishStackElementThreadLocal = new ThreadLocal<>();

   static class PublishStackElement {
	  private final PublishStackElement parent;
	  private final StackTraceElement element;

	  PublishStackElement(PublishStackElement parent, StackTraceElement element) {
		 this.parent = parent;
		 this.element = element;
	  }

	  public StackTraceElement[] getStackTrace() {
		 var frames = new LinkedList<StackTraceElement>();
		 var current = this;
		 do {
			if (current.element != null) {
			   frames.addLast(current.element);
			}
			current = current.parent;
		 } while (current != null);
		 return frames.stream().toArray(StackTraceElement[]::new);
	  }
   }

   private static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
	  throw (T)t;
   }

   private static <T> Predicate<T> wrapException(ExceptionFunction<T, Boolean> exceptionRunnable) {
	  return t -> {
		 try {
			return exceptionRunnable.execute(t);
		 } catch (Throwable e) {
			sneakyThrow(e);
			return false;
		 }
	  };
   }

   static PublishStackElement saveCurrent() {
	  var items = Stream.of(Thread.currentThread().getStackTrace())
			  .filter(wrapException(frame ->
					  frame.getClassName().contains("com.kuriosityrobotics") && (
							  !frame.getClassName().startsWith("com.kuriosityrobotics.powerplay.pubsub")
									  || Node.class.isAssignableFrom(Class.forName(frame.getClassName())))))
			  .collect(Collectors.toCollection(LinkedList::new));

	  var current = publishStackElementThreadLocal.get();
	  if (current == null)
		 current = new PublishStackElement(null, null);

	  while (!items.isEmpty()) {
		 var frameToPush = items.removeLast();
		 current = new PublishStackElement(current.element != null ? current : null, frameToPush);
	  }
	  return current;
   }

   public static void restore(PublishStackElement restoree) {
	  publishStackElementThreadLocal.set(restoree);
   }

   public static void main(String[] args) {
	  var current = saveCurrent();
	  do {
		 System.out.println(current.element);
		 current = current.parent;
	  } while (current != null);
   }
}
