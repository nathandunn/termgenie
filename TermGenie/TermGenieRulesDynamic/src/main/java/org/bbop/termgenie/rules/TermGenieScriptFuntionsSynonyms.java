package org.bbop.termgenie.rules;

import java.util.List;

import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;

public interface TermGenieScriptFuntionsSynonyms {

	/**
	 * Create new synonyms for a given term with a prefix and suffix. The new
	 * label is required as it is used to prevent accidental creation of a
	 * synonym with the same label.
	 * 
	 * @param prefix the prefix, may be null
	 * @param x the term
	 * @param ontology the ontology to retrieve existing synonyms
	 * @param suffix the suffix, may be null
	 * @param label the label of the new term
	 * @return synonyms
	 */
	public List<ISynonym> synonyms(String prefix,
			OWLObject x,
			OWLGraphWrapper ontology,
			String suffix,
			String label);

	/**
	 * Create new synonyms for two terms with a prefix, infix, and suffix. The
	 * new label is required as it is used to prevent accidental creation of a
	 * synonym with the same label.
	 * 
	 * @param prefix
	 * @param x1
	 * @param ontology1
	 * @param infix
	 * @param x2
	 * @param ontology2
	 * @param suffix
	 * @param label
	 * @return synonyms
	 */
	public List<ISynonym> synonyms(String prefix,
			OWLObject x1,
			OWLGraphWrapper ontology1,
			String infix,
			OWLObject x2,
			OWLGraphWrapper ontology2,
			String suffix,
			String label);

	/**
	 * Create new synonyms for a given list of terms with a prefix and suffix.
	 * The new label is required as it is used to prevent accidental creation of
	 * a synonym with the same label.
	 * 
	 * @param prefix the prefix, may be null
	 * @param terms the term list
	 * @param ontology the ontology to retrieve existing synonyms
	 * @param infix the infix between the synonym components
	 * @param suffix the suffix, may be null
	 * @param label the label of the new term
	 * @return synonyms
	 */
	public List<ISynonym> synonyms(String prefix,
			OWLObject[] terms,
			OWLGraphWrapper ontology,
			String infix,
			String suffix,
			String label);

}
