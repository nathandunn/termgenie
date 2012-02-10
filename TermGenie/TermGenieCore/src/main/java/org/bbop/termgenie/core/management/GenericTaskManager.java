package org.bbop.termgenie.core.management;

import java.util.concurrent.Semaphore;

import org.bbop.termgenie.core.management.GenericTaskManager.ManagedTask.Modified;

/**
 * Provide basic runtime management for an instance. Allow limited concurrent
 * usage of the managed instance.
 * 
 * @param <T> type of the managed instance
 */
public abstract class GenericTaskManager<T> {

	private volatile T managed = null;
	private final Semaphore lock;
	final String name;

	/**
	 * Create a new manager, with a binary and fair semaphore.
	 * 
	 * @param name the name of this manager
	 */
	public GenericTaskManager(String name) {
		this(name, 1); // binary and fair
	}

	/**
	 * Create a new manager, allowing n number of concurrent calls. Low Level,
	 * only to be used in this package
	 * 
	 * @param name the name of this manager
	 * @param n number of concurrent users
	 */
	GenericTaskManager(String name, int n) {
		this.lock = new Semaphore(n, true); // fair
		this.name = name;
	}

	/**
	 * Low level method to lock. Only to be used in this package
	 * 
	 * @return managed
	 */
	T getManaged() {
		try {
			lock.acquire();
			if (managed == null) {
				managed = createManaged();
				if (managed == null) {
					throw new GenericTaskManagerException("The managed object in manager " + name + " must never be null!");
				}
			}
			return managed;
		} catch (InterruptedException exception) {
			throw new GenericTaskManagerException("Interrupted during wait.", exception);
		}
	}

	/**
	 * Low level method to unlock. Only to be used in this package
	 * 
	 * @param managed
	 * @param modified
	 */
	void returnManaged(T managed, Modified modified) {
		if (this.managed != managed) {
			throw new GenericTaskManagerException("Trying to return the wrong managed object for manager: " + name);
		}
		try {
			if (modified == Modified.reset) {
				this.managed = resetManaged(managed);
			}
			else if (modified == Modified.update) {
				this.managed = updateManaged(managed);
			}
		} catch (GenericTaskManagerException exception) {
			throw exception;
		}
		finally {
			lock.release();
			if (modified == Modified.reset) {
				setChanged(true);
			}
			else if (modified == Modified.update) {
				setChanged(false);
			}
		}
	}

	/**
	 * Create a managed instance.
	 * 
	 * @return managed instance
	 */
	protected abstract T createManaged();

	/**
	 * Update the current managed instance.
	 * 
	 * @param managed current managed instance
	 * @return updated managed instance
	 */
	protected abstract T updateManaged(T managed);

	/**
	 * Update the current managed instance.
	 * 
	 * @param managed current managed instance
	 * @return updated managed instance
	 */
	protected abstract T resetManaged(T managed);

	/**
	 * Called for disposing the managed instance.
	 * Overwrite to implement custom functionality.
	 * 
	 * @param managed
	 */
	protected void dispose(T managed) {
		// do nothing
	}
	
	/**
	 * Tell the managed object to update. Wait until the other processes are
	 * finished.
	 */
	public void updateManaged() {
		boolean hasLock = false;
		try {
			lock.acquire();
			hasLock = true;
			if (managed == null) {
				managed = createManaged();
			}
			else {
				managed = updateManaged(managed);
				setChanged(false);
			}
		} catch (InterruptedException exception) {
			throw new GenericTaskManagerException("Interrupted during wait.", exception);
		}
		finally {
			if (hasLock) {
				lock.release();
			}
		}
	}
	
	public void dispose() {
		boolean hasLock = false;
		try {
			lock.acquire();
			hasLock = true;
			if (managed != null) {
				dispose(managed);
				managed = null;
			}
		} catch (InterruptedException exception) {
			throw new GenericTaskManagerException("Interrupted during wait.", exception);
		}
		finally {
			if (hasLock) {
				lock.release();
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		// this is a safe guard. If someone forgets to dispose an objects, 
		// which needs to be disposed, try to do it in the finalize.
		if (managed != null) {
			dispose(managed);
		}
		super.finalize();
	}

	protected abstract void setChanged(boolean reset);

	/**
	 * Run a managed task. Encapsulate the wait and return operations for the
	 * managed instance.
	 * 
	 * @param task
	 */
	public void runManagedTask(ManagedTask<T> task) {
		T managed = null;
		Modified modified = Modified.no;
		try {
			managed = getManaged();
			modified = task.run(managed);
		}
		finally {
			if (managed != null) {
				returnManaged(managed, modified);
			}
		}
	}

	/**
	 * A task which requires a managed instance.
	 * 
	 * @param <T>
	 */
	public static interface ManagedTask<T> {

		public enum Modified {
			no, update, reset
		}
		
		/**
		 * Run the task with a managed instance.
		 * 
		 * @param managed
		 * @return true if the instance was modified
		 */
		public Modified run(T managed);
	}

	public static class GenericTaskManagerException extends RuntimeException {

		// generated
		private static final long serialVersionUID = -204418633281300080L;

		public GenericTaskManagerException(String message) {
			super(message);
		}

		public GenericTaskManagerException(String message, Throwable exception) {
			super(message, exception);
		}
	}
}
