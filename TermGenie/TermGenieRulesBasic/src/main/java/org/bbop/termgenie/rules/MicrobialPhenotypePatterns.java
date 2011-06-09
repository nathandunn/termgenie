package org.bbop.termgenie.rules;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bbop.termgenie.core.OntologyAware.OntologyTerm;
import org.bbop.termgenie.core.rules.DefaultTermTemplates;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationInput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationOutput;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

public class MicrobialPhenotypePatterns extends Patterns {

	private final OWLGraphWrapper go;
	private final OWLGraphWrapper pato;
	private final OWLGraphWrapper omp;

	protected MicrobialPhenotypePatterns(OWLGraphWrapper omp, OWLGraphWrapper go, OWLGraphWrapper pato) {
		super(DefaultTermTemplates.omp_entity_quality);
		this.omp = omp;
		this.go = go;
		this.pato = pato;
	}

	@ToMatch
	protected List<TermGenerationOutput> omp_entity_quality(TermGenerationInput input, Map<String, OntologyTerm> pending) {
		OWLObject e = getSingleTerm(input, "entity", go);
		OWLObject q = getSingleTerm(input, "quality", pato);
		if (e == null ||  q == null) {
			// check branch
			return error("The specified terms do not correspond to the pattern", input);
		}
		String label = createName(name(q, pato) + " of " + name(e, go), input);
		String definition = createDefinition("Any "+name(q, pato)+" of "+name(e, go)+".", input);
		Set<String> synonyms = null;
		String logicalDefinition = "cdef("+id(q, pato)+",['OBO_REL:inheres_in'="+id(e, go)+"]),";
		return createTermList(label, definition, synonyms, logicalDefinition, input, omp);
	}
}