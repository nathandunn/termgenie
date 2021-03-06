package org.bbop.termgenie.freeform;

import java.util.List;
import java.util.Map;

import owltools.graph.OWLGraphWrapper.ISynonym;

/**
 * Fields expected for a free form request.
 */
public interface FreeFormTermRequest {

	/**
	 * @return the label
	 */
	public String getLabel();

	/**
	 * @return the namespace
	 */
	public String getNamespace();

	/**
	 * @return the definition
	 */
	public String getDefinition();

	/**
	 * @return the comment
	 */
	public String getComment();
	
	/**
	 * @return the dbxrefs
	 */
	public List<String> getDbxrefs();

	/**
	 * @return the isA
	 */
	public List<String> getIsA();

	/**
	 * @return all additional relations
	 */
	public Map<String, List<String>> getAdditionalRelations();
	
	/**
	 * @return the synonyms
	 */
	public List<? extends ISynonym> getISynonyms();
	
	/**
	 * @return the xrefs
	 */
	public List<Xref> getXrefs();

	
	public static class Xref {
		
		private String idRef;
		private String annotation;
		
		public String getIdRef() {
			return idRef;
		}
		
		public void setIdRef(String idRef) {
			this.idRef = idRef;
		}
		
		public String getAnnotation() {
			return annotation;
		}
		
		public void setAnnotation(String annotation) {
			this.annotation = annotation;
		}
	}
}
