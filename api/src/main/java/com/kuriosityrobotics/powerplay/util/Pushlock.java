package com.kuriosityrobotics.powerplay.util;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Pushlock {
   private final ReadWriteLock backingLock;

   public Pushlock() {
	  this.backingLock = new ReentrantReadWriteLock(false);
   }

   /**
	* Attempts to acquire the lock in shared mode.  Blocks while the lock is held in exclusive mode.
	* The lock can have multiple shared-mode holders, or a single exclusive-mode holder.
	* @return a guard that should be closed to release the lock
	*/
   public ShareableGuard acquireShared() {
	  return new ShareableGuard();
   }

   /**
	* Attempts to acquire the lock in exclusive mode.  Blocks while the lock is still held by either exclusive-mode or shared-mode holders.
	* The lock can have multiple 'shared' holders, or a single exclusive holder.
	* @return a guard that should be closed to release the lock
	*/
   public ExclusiveGuard acquireExclusive() {
	  return new ExclusiveGuard();
   }


   public final class ShareableGuard implements AutoCloseable {
	  ShareableGuard() {
         backingLock.readLock().lock();
      }

	  @Override
	  public void close() {
		 backingLock.readLock().unlock();
	  }
   }

   public final class ExclusiveGuard implements AutoCloseable {
	  ExclusiveGuard() {
		 backingLock.writeLock().lock();
	  }

	  @Override
	  public void close() {
		 backingLock.writeLock().unlock();
	  }
   }
}
