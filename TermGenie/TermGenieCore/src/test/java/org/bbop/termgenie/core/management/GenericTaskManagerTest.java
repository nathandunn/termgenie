package org.bbop.termgenie.core.management;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bbop.termgenie.core.management.GenericTaskManager.InvalidManagedInstanceException;
import org.junit.Test;

/**
 * Test the basic functionality of the {@link GenericTaskManager}.
 */
public class GenericTaskManagerTest {

	/**
	 * Use threads to simulate concurrent users and assert the expected results,
	 * even after updating the managed instance.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testGenericTaskManager() throws InterruptedException {
		TestTaskManager manager = new TestTaskManager();
		List<String> result = Collections.synchronizedList(new ArrayList<String>());

		new TestGenericTask("t1", manager, 100, 100, result).start();
		new TestGenericTask("t2", manager, 150, 100, result).start();
		new TestGenericUpdateTask(manager, 150).start();
		new TestGenericTask("t3", manager, 200, 100, result).start();
		TestGenericTask thread = new TestGenericTask("t4", manager, 300, 100, result);
		thread.start();

		thread.join();
		assertArrayEquals(new String[] { "t1v1", "t2v1", "t3v2", "t4v2" },
				result.toArray(new String[0]));
	}

	/**
	 * Use threads to simulate concurrent users and assert the expected results,
	 * even after updating the managed instance.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testGenericTaskManager2() throws InterruptedException {
		TestTaskManager manager = new TestTaskManager();
		List<String> result = Collections.synchronizedList(new ArrayList<String>());

		new TestGenericTask("t1", manager, 100, 100, result).start();
		new TestGenericModifyingTask("t2", manager, 150, 100, result).start();
		new TestGenericTask("t3", manager, 200, 100, result).start();
		TestGenericTask thread = new TestGenericTask("t4", manager, 300, 100, result);
		thread.start();

		thread.join();
		assertArrayEquals(new String[] { "t1v1", "t2v1", "t3r1", "t4r1" },
				result.toArray(new String[0]));
	}

	private static class TestTaskManager extends GenericTaskManager<String> {

		public TestTaskManager() {
			super("TestTaskManager");
		}

		@Override
		protected String createManaged() {
			return "v1";
		}

		@Override
		protected String updateManaged(String managed) {
			return "v2";
		}

		@Override
		protected String resetManaged(String managed) {
			return "r1";
		}

		@Override
		protected void setChanged(boolean reset) {
			// Do nothing in tests
		}

		@Override
		protected void dispose(String managed) {
			// Do nothing in tests
		}
	}

	private static class TestGenericTask extends Thread {

		private final GenericTaskManager<String> manager;
		private final long startSleep;
		private final long workSleep;
		private final List<String> result;
		private final String name;

		TestGenericTask(String name,
				GenericTaskManager<String> manager,
				long startSleep,
				long workSleep,
				List<String> result)
		{
			super();
			this.name = name;
			this.manager = manager;
			this.startSleep = startSleep;
			this.workSleep = workSleep;
			this.result = result;
		}

		@Override
		public void run() {
			try {
				sleep(startSleep);
				manager.runManagedTask(new GenericTaskManager.ManagedTask<String>() {

					@Override
					public Modified run(String managed) {
						try {
							result.add(name + managed);
							sleep(workSleep);
							return Modified.no;
						} catch (InterruptedException exception) {
							throw new RuntimeException(exception);
						}
					}
				});
			} catch (InterruptedException exception) {
				throw new RuntimeException(exception);
			} catch (InvalidManagedInstanceException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

	private static class TestGenericModifyingTask extends Thread {

		private final GenericTaskManager<String> manager;
		private final long startSleep;
		private final long workSleep;
		private final List<String> result;
		private final String name;

		TestGenericModifyingTask(String name,
				GenericTaskManager<String> manager,
				long startSleep,
				long workSleep,
				List<String> result)
		{
			super();
			this.name = name;
			this.manager = manager;
			this.startSleep = startSleep;
			this.workSleep = workSleep;
			this.result = result;
		}

		@Override
		public void run() {
			try {
				sleep(startSleep);
				manager.runManagedTask(new GenericTaskManager.ManagedTask<String>() {

					@Override
					public Modified run(String managed) {
						try {
							result.add(name + managed);
							sleep(workSleep);
							return Modified.reset;
						} catch (InterruptedException exception) {
							throw new RuntimeException(exception);
						}
					}
				});
			} catch (InterruptedException exception) {
				throw new RuntimeException(exception);
			} catch (InvalidManagedInstanceException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

	private static class TestGenericUpdateTask extends Thread {

		private final GenericTaskManager<String> manager;
		private final long startSleep;

		TestGenericUpdateTask(GenericTaskManager<String> manager, long startSleep) {
			super();
			this.manager = manager;
			this.startSleep = startSleep;
		}

		@Override
		public void run() {
			try {
				sleep(startSleep);
				manager.updateManaged();
			} catch (InterruptedException exception) {
				throw new RuntimeException(exception);
			} catch (InvalidManagedInstanceException exception) {
				throw new RuntimeException(exception);
			}
		}
	}

}
