package org.bbop.termgenie.freeform;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bbop.termgenie.core.Ontology;
import org.bbop.termgenie.core.management.GenericTaskManager.InvalidManagedInstanceException;
import org.bbop.termgenie.core.management.GenericTaskManager.ManagedTask.Modified;
import org.bbop.termgenie.core.management.MultiResourceTaskManager.MultiResourceManagedTask;
import org.bbop.termgenie.core.process.ProcessState;
import org.bbop.termgenie.core.rules.ReasonerFactory;
import org.bbop.termgenie.core.rules.ReasonerTaskManager;
import org.bbop.termgenie.ontology.MultiOntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.obo.OboTools;
import org.bbop.termgenie.ontology.obo.OwlTranslatorTools;
import org.bbop.termgenie.owl.InferAllRelationshipsTask;
import org.bbop.termgenie.owl.InferredRelations;
import org.bbop.termgenie.owl.OWLChangeTracker;
import org.bbop.termgenie.rules.TermGenieScriptRunner;
import org.bbop.termgenie.tools.Pair;
import org.obolibrary.obo2owl.Obo2OWLConstants;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Frame.FrameType;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class FreeFormTermValidatorImpl implements FreeFormTermValidator {
	
	private static final Logger logger = Logger.getLogger(FreeFormTermValidatorImpl.class);
	
	static final String REQ_LIT_REF_PARAM = "FreeFormTermValidatorRequireLiteratureReference";
	static final String ADD_SUBSET_TAG_PARAM = "FreeFormTermValidatorAddSubsetTag";
	static final String SUBSET_PARAM = "FreeFormTermValidatorSubsetTag";
	
	private static final Pattern def_xref_Pattern = Pattern.compile("\\S+:\\S+");
	
	private final Ontology ontology;
	private final MultiOntologyTaskManager manager;
	private final ReasonerFactory factory;
	private final boolean requireLiteratureReference;
	private final boolean useIsInferred;
	private final boolean addSubsetTag;
	
	private String subset = null;

	@Inject
	public FreeFormTermValidatorImpl(@Named("CommitTargetOntology") OntologyTaskManager ontology,
			MultiOntologyTaskManager manager,
			@Named(REQ_LIT_REF_PARAM) boolean requireLiteratureReference,
			@Named(ADD_SUBSET_TAG_PARAM) boolean addSubsetTag,
			@Named(TermGenieScriptRunner.USE_IS_INFERRED_BOOLEAN_NAME) boolean useIsInferred,
			ReasonerFactory factory)
	{
		super();
		this.addSubsetTag = addSubsetTag;
		this.ontology = ontology.getOntology();
		this.manager = manager;
		this.factory = factory;
		this.requireLiteratureReference = requireLiteratureReference;
		this.useIsInferred = useIsInferred;
	}

	
	/**
	 * @param subset the subset to set
	 * 
	 * only effective if addSubsetTag is also set to true, see constructor.
	 */
	@Inject(optional=true)
	public void setSubset(@Named(SUBSET_PARAM) String subset) {
		this.subset = subset;
	}

	private FreeFormValidationResponse error(String message) {
		FreeFormValidationResponse response = new FreeFormValidationResponse();
		response.setGeneralError(message);
		return response;
	}
	
	private FreeFormValidationResponse error(List<FreeFormHint> errors) {
		FreeFormValidationResponse response = new FreeFormValidationResponse();
		response.setErrors(errors);
		return response;
	}
	
	private FreeFormValidationResponse success(Pair<Frame, Set<OWLAxiom>> term) {
		FreeFormValidationResponse response = new FreeFormValidationResponse();
		response.setGeneratedTerm(term);
		return response;
	}

	@Override
	public FreeFormValidationResponse validate(final FreeFormTermRequest request, ProcessState state) {
		
		String subset = null;
		if (this.subset != null && addSubsetTag) {
			subset = this.subset;
		}
		ValidationTask task = new ValidationTask(request, requireLiteratureReference, useIsInferred, subset, factory, state);
		try {
			manager.runManagedTask(task, ontology);
		} catch (InvalidManagedInstanceException exception) {
			String message = "Error during term validation, due to an inconsistent ontology";
			logger.error(message, exception);
			return error(message);
		}
		if (task.errors == null || task.errors.isEmpty()) {
			if (task.term == null) {
				return error("No term was generated from your request");
			}
			return success(task.term);
		}
		return error(task.errors);
	}


	static class ValidationTask implements MultiResourceManagedTask<OWLGraphWrapper, Ontology>
	{
		private final ReasonerFactory reasonerFactory;
		private final FreeFormTermRequest request;
		private final boolean requireLiteratureReference;
		private final boolean useIsInferred;
		private final ProcessState state;
		
		List<FreeFormHint> errors = null;
		Pair<Frame, Set<OWLAxiom>> term;
		
		List<Modified> modified = null;
		private final String subset;
		
		
	
		ValidationTask(FreeFormTermRequest request,
				boolean requireLiteratureReference,
				boolean useIsInferred,
				String subset,
				ReasonerFactory factory,
				ProcessState state)
		{
			this.request = request;
			this.requireLiteratureReference = requireLiteratureReference;
			this.useIsInferred = useIsInferred;
			this.subset = subset;
			this.reasonerFactory = factory;
			this.state = state;
		}
	
		@Override
		public List<Modified> run(List<OWLGraphWrapper> requested)
				throws InvalidManagedInstanceException
		{
			OWLGraphWrapper graph = requested.get(0);
			runInternal(graph);
			return modified; // no modifications
		}
		
		void runInternal(final OWLGraphWrapper graph) {
			// check label
			String requestedLabel = StringUtils.trimToNull(request.getLabel());
			if (requestedLabel == null) {
				setError("label", "A label is required for a free from request.");
				return;
			}
			requestedLabel = StringUtils.normalizeSpace(requestedLabel);
			
			// TODO discuss/implement a more clever check
			if (requestedLabel.length() < 10) {
				setError("label", "The provided label is too short.");
				return;
			}

			// search for similar labels and synonyms in the ontology 
			CharSequence normalizedLabel = normalizeLabel(requestedLabel);
			
			// check proposed synonyms at the same time 
			List<ISynonym> checkedSynonyms = null;
			Map<CharSequence, String> proposedSynonyms = null;
			
			// each synonym has to have:
			//  * label
			//  * scope (EXACT, NARROW, RELATED, No BROADER)
			// optional: xref
			List<? extends ISynonym> iSynonyms = request.getISynonyms();
			if (iSynonyms != null && !iSynonyms.isEmpty()) {
				checkedSynonyms = new ArrayList<ISynonym>();
				proposedSynonyms = new HashMap<CharSequence, String>();
				Set<String> done = new HashSet<String>();
				for (ISynonym jsonSynonym : iSynonyms) {
					String synLabel = StringUtils.trimToNull(jsonSynonym.getLabel());
					if (synLabel == null) {
						addError("synonyms", "No empty labels as synonym allowed.");
						continue;
					}
					String lowerCase = synLabel.toLowerCase();
					if (done.contains(lowerCase)) {
						addError("synonyms", "Duplicate synonym: "+synLabel);
						continue;
					}
					String scope = StringUtils.trimToNull(jsonSynonym.getScope());
					if (scope == null) {
						scope = OboFormatTag.TAG_RELATED.getTag();
					}
					else {
						if (isOboScope(scope) == false) {
							addError("synonyms", "The synonym '"+synLabel+"' has an unknown scope: "+scope);
						}
					}
					checkedSynonyms.add(jsonSynonym);
					proposedSynonyms.put(normalizeLabel(synLabel), synLabel);
					done.add(lowerCase);
				}
			}
			
			// iterate over all objects
			for(OWLObject current : graph.getAllOWLObjects()) {
				String currentLabel = graph.getLabel(current);
				final CharSequence currentNormalizedLabel = normalizeLabel(currentLabel);
				if (currentLabel != null) {
					if (similar(normalizedLabel, currentNormalizedLabel)) {
						addError("label", "The requested label is similar to the term: "+graph.getIdentifier(current)+" '"+currentLabel+"'");
						return;
					}
					if (proposedSynonyms != null) {
						for (Entry<CharSequence, String> entry : proposedSynonyms.entrySet()) {
							if (similar(normalizedLabel, entry.getKey())) {
								addError("synonym", "The requested synonym '"+entry.getValue()+"' is similar to the term: "+graph.getIdentifier(current)+" '"+currentLabel+"'");
							}
						}
					}
				}
				List<ISynonym> oboSynonyms = graph.getOBOSynonyms(current);
				if (oboSynonyms != null) {
					for (ISynonym synonym : oboSynonyms) {
						String synLabel = synonym.getLabel();
						CharSequence normalizedSynLabel = normalizeLabel(synLabel);
						if (similar(normalizedLabel, normalizedSynLabel)) {
							addError("label",
									"The requested label is similar to the synonym: '" + synLabel + "' of term: " + graph.getIdentifier(current) + " '" + currentLabel + "'");
							return;
						}
						if (proposedSynonyms != null) {
							for (Entry<CharSequence, String> entry : proposedSynonyms.entrySet()) {
								if (similar(normalizedSynLabel, entry.getKey())) {
									addError("synonym", "The requested synonym '"+entry.getValue()+"' is similar to the synonym: '" + synLabel + "' of term: " + graph.getIdentifier(current) + " '" + currentLabel + "'");
								}
							}
						}
					}
				}
			}
			if (errors != null) {
				return;
			}
			
			// TODO check requested terms in the queue
			// TODO check blacklist

			// check namespace
			String namespace = StringUtils.trimToNull(request.getNamespace());
			if (namespace == null || namespace.isEmpty()) {
				setError("namespace", "A namespace is required for request: "+requestedLabel);
				return;
			}

			// check relations

			// at least one is_a parent in correct namespace
			final List<String> parents = request.getIsA();
			if (parents == null || parents.isEmpty()) {
				setError("parents", "At least one is_a parent required for request: "+requestedLabel);
				return;
			}
			Set<OWLClass> superClasses = new HashSet<OWLClass>();
			for (String parentId : parents) {
				OWLClass cls = graph.getOWLClassByIdentifier(parentId);
				if (cls == null) {
					addError("is_a parent", "parent not found in ontology: "+parentId);
					continue;
				}
				String parentNamespace = graph.getNamespace(cls);
				if (namespace.equals(parentNamespace)) {
					superClasses.add(cls);
				}
				else {
					addError("is_a parent", "namespace conflict parent namespace: '"+parentNamespace+"' requested namespace: '"+namespace+"'");
				}
			}
			if (errors != null) {
				return;
			}
			
			// TODO should we test for too high level parents? blacklist?
			
			// optional part_of relations
			Set<OWLClass> partOf = null;
			OWLObjectProperty partOfProperty = null;
			List<String> partOfList = request.getPartOf();			
			if (partOfList != null && !partOfList.isEmpty()) {
				// first: find property
				
				// second: validate given classes 
				partOf = new HashSet<OWLClass>();
				for(String id : partOfList) {
					OWLClass cls = graph.getOWLClassByIdentifier(id);
					if (cls == null) {
						addError("part_of parent", "parent not found in ontology: "+id);
						continue;
					}
					partOf.add(cls);
				}
			}
			if (errors != null) {
				return;
			}

			// check definition
			String def = StringUtils.trimToNull(request.getDefinition());
			if (def == null || def.length() < 20) {
				// check that the definition is at least X amount of chars long
				setError("definition", "Please enter a valid definition");
				return;
			}
			// search for similar definitions?
			
			// require at least on def db xref, ideally includes PMID
			List<String> xrefsList = request.getDbxrefs();
			if (xrefsList == null || xrefsList.isEmpty()) {
				setError("definition db xref", "Please enter at least one valid definition db xref");
				return;
			}
			Set<String> xrefs = new HashSet<String>();
			boolean hasLiteratureReference = false;
			for(String  xref : xrefsList) {
				String dbxref = StringUtils.trimToNull(xref);
				if (dbxref == null) {
					continue;
				}
				// simple db xref check, TODO use a centralized qc check.
				if (xref.length() < 3) {
					addError("definition db xref", "The db xref " + xref + " is too short. A Definition db xref consists of a prefix and suffix with a colon (:) as separator");
					continue;
				}
				if (!def_xref_Pattern.matcher(xref).matches()) {
					addError("definition db xref", "The db xref " + xref + " does not conform to the expected pattern. A db xref consists of a prefix and suffix with a colon (:) as separator and contains no whitespaces.");
					continue;
				}
				if (requireLiteratureReference && !hasLiteratureReference) {
					hasLiteratureReference = isLiteratureReference(xref);
				}
				xrefs.add(dbxref);
			}
			if (requireLiteratureReference && !hasLiteratureReference) {
				addError("definition db xref", "The db xref must contain at least on PMID or doi as literatre reference.");
			}
			
			if (xrefs.isEmpty()) {
				setError("definition db xref", "Please enter at least one valid definition db xref");
				return;
			}
			
			// all checks passed, create term
			
			String owlNewId = getNewId();
			String oboNewId = Owl2Obo.getIdentifier(IRI.create(owlNewId));
			
			// minimal OWL
			Set<OWLAxiom> axioms = new HashSet<OWLAxiom>();
			OWLOntologyManager owlManager = graph.getManager();
			OWLDataFactory factory = owlManager.getOWLDataFactory();
			IRI iri = IRI.create(owlNewId);
			OWLClass owlClass = factory.getOWLClass(iri);
			axioms.add(factory.getOWLDeclarationAxiom(owlClass));
			axioms.add(OwlTranslatorTools.createLabelAxiom(owlNewId, requestedLabel, graph));
			
			Set<OWLAxiom> preliminaryAxioms = new HashSet<OWLAxiom>();
			for(OWLClass superClass : superClasses) {
				preliminaryAxioms.add(factory.getOWLSubClassOfAxiom(owlClass, superClass));
			}
			
			if (partOf != null && !partOf.isEmpty()) {
				for(OWLClass superClass : partOf) {
					preliminaryAxioms.add(factory.getOWLSubClassOfAxiom(owlClass, factory.getOWLObjectSomeValuesFrom(partOfProperty, superClass)));
				}
			}
			preliminaryAxioms.addAll(axioms);
			
			// add relationships to graph and use reasoner to infer relationships (remove redundant ones)
			final OWLOntology owlOntology = graph.getSourceOntology();
			final OWLChangeTracker changeTracker = new OWLChangeTracker(owlOntology);
			try {
				
				// add axioms
				for(OWLAxiom axiom : preliminaryAxioms) {
					changeTracker.apply(new AddAxiom(owlOntology, axiom));
				}
				reasonerFactory.updateBuffered(graph.getOntologyId());
				ReasonerTaskManager reasonerManager = reasonerFactory.getDefaultTaskManager(graph);
				reasonerManager.setProcessState(state);
				
				final InferAllRelationshipsTask task = new InferAllRelationshipsTask(graph, iri, changeTracker, getTempIdPrefix(), state, useIsInferred);
				try {
					reasonerManager.runManagedTask(task);
				} catch (InvalidManagedInstanceException exception) {
					setError("Relations", "Could not create releations due to an invalid reasoner: "+exception.getMessage());
					return;
				}
				finally {
					reasonerManager.dispose();
					reasonerManager.removeProcessState();
				}
				InferredRelations inferredRelations = task.getInferredRelations();
				axioms.addAll(inferredRelations.getClassRelationAxioms());
				
				// OBO
				Frame frame = new Frame(FrameType.TERM);
	
				OboTools.addTermLabel(frame, requestedLabel);
				OboTools.addDefinition(frame, def, xrefs);
				frame.addClause(new Clause(OboFormatTag.TAG_CREATION_DATE, getDate()));
				frame.addClause(new Clause(OboFormatTag.TAG_CREATED_BY, "TermGenie"));
				frame.addClause(new Clause(OboFormatTag.TAG_NAMESPACE, namespace));
				if (checkedSynonyms != null) {
					for(ISynonym synonym : checkedSynonyms) {
						OboTools.addSynonym(frame, synonym.getLabel(), synonym.getScope(), synonym.getXrefs());
					}
				}
				if (subset != null) {
					frame.addClause(new Clause(OboFormatTag.TAG_SUBSET, subset));
				}
	
				OboTools.addTermId(frame, oboNewId);
				
				for (Clause cl : inferredRelations.getClassRelations()) {
					frame.addClause(cl);
				}
				
				term = new Pair<Frame, Set<OWLAxiom>>(frame, axioms);
			}
			finally {
				boolean success = changeTracker.undoChanges();
				if (!success) {
					// only reset the ontology, if the 
					setReset();
				}
			}
			return;
		}
		
		void setError(String field, String messge) {
			errors = Collections.singletonList(new FreeFormHint(field, messge));
		}
		
		void addError(String field, String message) {
			if (errors == null) {
				errors = new ArrayList<FreeFormHint>();
			}
			errors.add(new FreeFormHint(field, message));
		}
		
		void setReset() {
			modified = Collections.singletonList(Modified.reset);
		}
		
		static CharSequence normalizeLabel(CharSequence cs) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < cs.length(); i++) {
				char c = cs.charAt(i);
				c = Character.toLowerCase(c);
				if (Character.isLetterOrDigit(c) == false) {
					c = '*';
				}
				sb.append(c);
			}
			return sb;
		}
		
		static boolean similar(CharSequence cs1, CharSequence cs2) {
			int distance = StringUtils.getLevenshteinDistance(cs1, cs2);
			// TODO make this threshold relative to the string length
			return distance <= 1;
		}
		
		static boolean isOboScope(String s) {
			return (OboFormatTag.TAG_RELATED.getTag().equals(s)) ||
					(OboFormatTag.TAG_NARROW.getTag().equals(s)) ||
					(OboFormatTag.TAG_EXACT.getTag().equals(s)) ||
					(OboFormatTag.TAG_BROAD.getTag().equals(s));
		}
		
		final static Pattern PMID_PATTERN = Pattern.compile("PMID:\\d+", Pattern.CASE_INSENSITIVE);
		final static Pattern ISBN_PATTERN = Pattern.compile("ISBN:\\d+", Pattern.CASE_INSENSITIVE);
		final static Pattern DOI_PATTERN = Pattern.compile("DOI:\\S+", Pattern.CASE_INSENSITIVE);
		
		static boolean isLiteratureReference(String s) {
			Matcher pmidMatcher = PMID_PATTERN.matcher(s);
			if (pmidMatcher.matches()) {
				return true;
			}
			Matcher isbnMatcher = ISBN_PATTERN.matcher(s);
			if (isbnMatcher.matches()) {
				return true;
			}
			Matcher doiMatcher = DOI_PATTERN.matcher(s);
			return doiMatcher.matches();
		}
	}
	
	private final static ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>(){

		@Override
		protected DateFormat initialValue() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			return sdf;
		}
	};

	static String getDate() {
		return df.get().format(new Date());
	}
	
	private static int ID_Counter = 0;
	
	static String getTempIdPrefix() {
		return Obo2OWLConstants.DEFAULT_IRI_PREFIX + "/FreeForm/FreeForm_";
	}
	
	static synchronized String getNewId() {
		String id = getTempIdPrefix() + ID_Counter;
		ID_Counter += 1;
		return id;
	}
}