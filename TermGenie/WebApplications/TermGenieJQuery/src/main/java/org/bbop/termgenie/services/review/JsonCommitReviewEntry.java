package org.bbop.termgenie.services.review;

import org.bbop.termgenie.ontology.CommitObject.Modification;

public class JsonCommitReviewEntry {

	private int historyId;

	private String user;
	private String date;

	private JsonDiff[] diffs;

	/**
	 * @return the historyId
	 */
	public int getHistoryId() {
		return historyId;
	}

	/**
	 * @param historyId the historyId to set
	 */
	public void setHistoryId(int historyId) {
		this.historyId = historyId;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(String date) {
		this.date = date;
	}

	/**
	 * @return the diffs
	 */
	public JsonDiff[] getDiffs() {
		return diffs;
	}

	/**
	 * @param diffs the diffs to set
	 */
	public void setDiffs(JsonDiff[] diffs) {
		this.diffs = diffs;
	}

	public static class JsonDiff {

		private String id;

		private int operation;

		private String diff;

		private boolean modified = false;

		/**
		 * @return the id
		 */
		public String getId() {
			return id;
		}

		/**
		 * @param id the id to set
		 */
		public void setId(String id) {
			this.id = id;
		}

		/**
		 * @return the operation
		 */
		public int getOperation() {
			return operation;
		}

		/**
		 * @param operation the operation to set
		 */
		public void setOperation(int operation) {
			this.operation = operation;
		}
		
		/**
		 * @param operation the {@link Modification} to set
		 */
		public void setOperation(Modification operation) {
			this.operation = operation.ordinal();
		}
		
		public static Modification getModification(JsonDiff diff) {
			int operation = diff.getOperation();
			if (operation >= 0 && operation < Modification.values().length) {
				return Modification.values()[operation];
			}
			return null;
		}

		/**
		 * @return the diff
		 */
		public String getDiff() {
			return diff;
		}

		/**
		 * @param diff the diff to set
		 */
		public void setDiff(String diff) {
			this.diff = diff;
		}

		/**
		 * @return the modified
		 */
		public boolean isModified() {
			return modified;
		}

		/**
		 * @param modified the modified to set
		 */
		public void setModified(boolean modified) {
			this.modified = modified;
		}
	}
}
