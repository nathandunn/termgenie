package org.bbop.termgenie.services;

import static org.bbop.termgenie.tools.ErrorMessages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bbop.termgenie.core.Ontology;
import org.bbop.termgenie.core.Ontology.IRelation;
import org.bbop.termgenie.core.Ontology.OntologyTerm;
import org.bbop.termgenie.core.Ontology.Relation;
import org.bbop.termgenie.core.TemplateField;
import org.bbop.termgenie.core.TemplateField.Cardinality;
import org.bbop.termgenie.core.TermTemplate;
import org.bbop.termgenie.core.rules.TermGenerationEngine;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationInput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationOutput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationParameters;
import org.bbop.termgenie.data.JsonGenerationResponse;
import org.bbop.termgenie.data.JsonOntologyTerm;
import org.bbop.termgenie.data.JsonOntologyTerm.JsonSynonym;
import org.bbop.termgenie.data.JsonOntologyTerm.JsonTermMetaData;
import org.bbop.termgenie.data.JsonTermGenerationInput;
import org.bbop.termgenie.data.JsonTermGenerationParameter;
import org.bbop.termgenie.data.JsonTermGenerationParameter.JsonOntologyTermIdentifier;
import org.bbop.termgenie.data.JsonTermTemplate;
import org.bbop.termgenie.data.JsonTermTemplate.JsonCardinality;
import org.bbop.termgenie.data.JsonTermTemplate.JsonTemplateField;
import org.bbop.termgenie.data.JsonValidationHint;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager.OntologyTask;
import org.bbop.termgenie.tools.FieldValidatorTool;
import org.bbop.termgenie.tools.OntologyCommitTool;
import org.bbop.termgenie.tools.OntologyTools;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.Synonym;
import owltools.graph.OWLQuantifiedProperty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GenerateTermsServiceImpl implements GenerateTermsService {

	private static final Logger logger = Logger.getLogger(GenerateTermsServiceImpl.class);

	private final TemplateCache TEMPLATE_CACHE = TemplateCache.getInstance();
	private final OntologyTools ontologyTools;
	private final TermGenerationEngine termGeneration;
	private final OntologyCommitTool committer;
	private final JsonTemplateTools jsonTools;

	/**
	 * @param ontologyTools
	 * @param termGeneration
	 * @param committer
	 */
	@Inject
	GenerateTermsServiceImpl(OntologyTools ontologyTools,
			TermGenerationEngine termGeneration,
			OntologyCommitTool committer)
	{
		super();
		this.ontologyTools = ontologyTools;
		this.termGeneration = termGeneration;
		this.committer = committer;
		this.jsonTools = new JsonTemplateTools();
	}

	@Override
	public JsonTermTemplate[] availableTermTemplates(String sessionId, String ontologyName) {
		// sanity check
		if (ontologyName == null) {
			// silently ignore this
			return new JsonTermTemplate[0];
		}
		Collection<TermTemplate> templates = getTermTemplates(ontologyName);
		if (templates.isEmpty()) {
			// short cut for empty results.
			return new JsonTermTemplate[0];
		}

		// encode the templates for JSON
		List<JsonTermTemplate> jsonTemplates = new ArrayList<JsonTermTemplate>();
		for (TermTemplate template : templates) {
			jsonTemplates.add(jsonTools.createJsonTermTemplate(template));
		}
		Collections.sort(jsonTemplates, new Comparator<JsonTermTemplate>() {

			@Override
			public int compare(JsonTermTemplate o1, JsonTermTemplate o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return jsonTemplates.toArray(new JsonTermTemplate[jsonTemplates.size()]);
	}

	/*
	 * Do not trust any input here. Do not assume that this is well formed, as
	 * the request could be generated by a different client!
	 */
	@Override
	public JsonGenerationResponse generateTerms(String sessionId,
			String ontologyName,
			JsonTermGenerationInput[] allParameters)
	{
		// sanity checks
		if (ontologyName == null || ontologyName.isEmpty()) {
			return new JsonGenerationResponse(NO_ONTOLOGY, null, null);
		}
		if (allParameters == null) {
			return new JsonGenerationResponse(NO_TERM_GENERATION_PARAMETERS, null, null);
		}

		// retrieve target ontology
		OntologyTaskManager manager = ontologyTools.getManager(ontologyName);
		if (manager == null) {
			return new JsonGenerationResponse(NO_ONTOLOGY, null, null);
		}

		// term generation parameter validation
		List<JsonValidationHint> allErrors = new ArrayList<JsonValidationHint>();
		for (JsonTermGenerationInput input : allParameters) {
			if (input == null) {
				return new JsonGenerationResponse(UNEXPECTED_NULL_VALUE, null, null);
			}
			JsonTermTemplate one = input.getTermTemplate();
			JsonTermGenerationParameter parameter = input.getTermGenerationParameter();
			if (one == null || parameter == null) {
				return new JsonGenerationResponse(UNEXPECTED_NULL_VALUE, null, null);
			}
			// retrieve the template from the server, do not trust the submitted
			// one.
			TermTemplate template = getTermTemplate(ontologyName, one.getName());
			if (template == null) {
				return new JsonGenerationResponse("Unknow template specified: " + one.getName(), null, null);
			}
			JsonTermTemplate jsonTermTemplate = jsonTools.createJsonTermTemplate(template);

			List<JsonValidationHint> errors = FieldValidatorTool.validateParameters(template,
					jsonTermTemplate,
					parameter);
			if (!errors.isEmpty()) {
				allErrors.addAll(errors);
			}
		}
		// return validation errors
		if (!allErrors.isEmpty()) {
			return new JsonGenerationResponse(null, allErrors, null);
		}

		try {
			// generate term candidates
			List<TermGenerationInput> generationTasks = createGenerationTasks(ontologyName,
					allParameters);
			List<TermGenerationOutput> candidates = generateTermsInternal(manager.getOntology(),
					generationTasks);

			// validate candidates
			if (candidates == null || candidates.isEmpty()) {
				return new JsonGenerationResponse(NO_TERMS_GENERATED, null, null);
			}

			List<JsonOntologyTerm> jsonCandidates = new ArrayList<JsonOntologyTerm>();
			Collection<JsonValidationHint> jsonHints = new ArrayList<JsonValidationHint>();
			for (TermGenerationOutput candidate : candidates) {
				if (candidate.isSuccess()) {
					JsonOntologyTerm jsonCandidate = createJsonCandidate(candidate);
					jsonCandidates.add(jsonCandidate);
				}
				else {
					JsonTermTemplate template = jsonTools.createJsonTermTemplate(candidate.getInput().getTermTemplate());
					jsonHints.add(new JsonValidationHint(template, -1, candidate.getMessage()));
				}
			}

			JsonGenerationResponse generationResponse = new JsonGenerationResponse(null, jsonHints, jsonCandidates);

			// return response
			return generationResponse;
		} catch (Exception exception) {
			logger.warn("An error occured during the term generation for the parameters: {ontologyName: " + ontologyName + ", allParameters: " + Arrays.toString(allParameters) + "}",
					exception);
			return new JsonGenerationResponse("An internal error occured on the server. Please contact the developers if the problem persists.", null, null);
		}
	}

	private JsonOntologyTerm createJsonCandidate(TermGenerationOutput candidate) {

		JsonOntologyTerm term = new JsonOntologyTerm();
		term.setDefinition(candidate.getTerm().getDefinition());
		List<String> defXRef = candidate.getTerm().getDefXRef();
		if (defXRef != null && !defXRef.isEmpty()) {
			term.setDefxRef(defXRef.toArray(new String[defXRef.size()]));
		}
		term.setTempId(candidate.getTerm().getId());
		term.setLabel(candidate.getTerm().getLabel());
		term.setSynonyms(createJsonSynonyms(candidate.getTerm().getSynonyms()));
		term.setMetaData(new JsonTermMetaData(candidate.getTerm().getMetaData()));
		return term;
	}

	private JsonSynonym[] createJsonSynonyms(List<Synonym> synonyms) {
		if (synonyms != null && !synonyms.isEmpty()) {
			List<JsonSynonym> jsonSynonyms = new ArrayList<JsonSynonym>(synonyms.size());
			for (Synonym synonym : synonyms) {
				JsonSynonym jsonSynonym = new JsonSynonym();
				jsonSynonym.setLabel(synonym.getLabel());
				jsonSynonym.setScope(synonym.getScope());
				jsonSynonym.setCategory(synonym.getCategory());
				String[] axrefs = null;
				Set<String> xrefs = synonym.getXrefs();
				if (xrefs != null && !xrefs.isEmpty()) {
					axrefs = xrefs.toArray(new String[xrefs.size()]);
				}
				jsonSynonym.setXrefs(axrefs);
				jsonSynonyms.add(jsonSynonym);
			}
			return jsonSynonyms.toArray(new JsonSynonym[jsonSynonyms.size()]);
		}
		return null;
	}

	private List<TermGenerationInput> createGenerationTasks(String ontologyName,
			JsonTermGenerationInput[] allParameters)
	{
		List<TermGenerationInput> result = new ArrayList<TermGenerationInput>();
		for (JsonTermGenerationInput jsonInput : allParameters) {
			JsonTermTemplate jsonTemplate = jsonInput.getTermTemplate();
			TermTemplate template = getTermTemplate(ontologyName, jsonTemplate.getName());
			TermGenerationParameters parameters = jsonTools.createTermGenerationParameters(jsonInput.getTermGenerationParameter(),
					template);
			TermGenerationInput input = new TermGenerationInput(template, parameters);
			result.add(input);
		}
		return result;
	}

	private Collection<TermTemplate> getTermTemplates(String ontology) {
		Collection<TermTemplate> templates;
		synchronized (TEMPLATE_CACHE) {
			templates = TEMPLATE_CACHE.getTemplates(ontology);
			if (templates == null) {
				templates = requestTemplates(ontology);
				TEMPLATE_CACHE.put(ontology, templates);
			}
		}
		return templates;
	}

	private TermTemplate getTermTemplate(String ontology, String name) {
		TermTemplate template;
		synchronized (TEMPLATE_CACHE) {
			template = TEMPLATE_CACHE.getTemplate(ontology, name);
			if (template == null) {
				Collection<TermTemplate> templates = TEMPLATE_CACHE.getTemplates(ontology);
				if (templates == null) {
					templates = requestTemplates(ontology);
					TEMPLATE_CACHE.put(ontology, templates);
				}
				template = TEMPLATE_CACHE.getTemplate(ontology, name);
			}
		}
		return template;
	}

	/**
	 * Request the templates for a given ontology.
	 * 
	 * @param ontology
	 * @return templates, never null
	 */
	protected Collection<TermTemplate> requestTemplates(String ontology) {
		List<TermTemplate> templates = ontologyTools.getTermTemplates(ontology);
		if (templates == null) {
			templates = Collections.emptyList();
		}
		return templates;
	}

	protected List<TermGenerationOutput> generateTermsInternal(Ontology ontology,
			List<TermGenerationInput> generationTasks)
	{
		return termGeneration.generateTerms(ontology, generationTasks);
	}

	protected boolean executeCommit(Ontology ontology, List<TermGenerationOutput> candidates) {
		return committer.commitCandidates(ontology, candidates);
	}

	/**
	 * Tools for converting term generation details into the JSON enabled
	 * (transfer) objects.
	 */
	class JsonTemplateTools {

		/**
		 * Convert a single template into a JSON specific data structure.
		 * 
		 * @param template
		 * @return internal format
		 */
		JsonTermTemplate createJsonTermTemplate(TermTemplate template) {
			JsonTermTemplate jsonTermTemplate = new JsonTermTemplate();
			jsonTermTemplate.setName(template.getName());
			jsonTermTemplate.setDisplay(template.getDisplayName());
			jsonTermTemplate.setDescription(template.getDescription());
			jsonTermTemplate.setHint(template.getHint());
			List<TemplateField> fields = template.getFields();
			int size = fields.size();
			JsonTemplateField[] jsonFields = new JsonTemplateField[size];
			for (int i = 0; i < size; i++) {
				jsonFields[i] = createJsonTemplateField(fields.get(i));
			}
			jsonTermTemplate.setFields(jsonFields);
			return jsonTermTemplate;
		}

		private JsonTemplateField createJsonTemplateField(TemplateField field) {
			JsonTemplateField jsonField = new JsonTemplateField();
			jsonField.setName(field.getName());
			jsonField.setRequired(field.isRequired());
			Cardinality c = field.getCardinality();
			jsonField.setCardinality(new JsonCardinality(c.getMinimum(), c.getMaximum()));
			jsonField.setFunctionalPrefixes(field.getFunctionalPrefixes().toArray(new String[0]));
			if (field.hasCorrespondingOntologies()) {
				List<Ontology> ontologies = field.getCorrespondingOntologies();
				String[] ontologyNames = new String[ontologies.size()];
				for (int i = 0; i < ontologyNames.length; i++) {
					Ontology ontology = ontologies.get(i);
					ontologyNames[i] = ontologyTools.getOntologyName(ontology);
				}
				jsonField.setOntologies(ontologyNames);
			}
			return jsonField;
		}

		TermGenerationParameters createTermGenerationParameters(JsonTermGenerationParameter json,
				TermTemplate template)
		{
			int fieldCount = template.getFieldCount();
			TermGenerationParameters result = new TermGenerationParameters(fieldCount);
			for (int pos = 0; pos < fieldCount; pos++) {
				result.setStringValues(template, pos, getStrings(json, pos));
				result.setTermValues(template, pos, getTerms(json, pos));
			}
			return result;
		}

		private String[] getStrings(JsonTermGenerationParameter json, int pos) {
			String[][] allStrings = json.getStrings();
			if (allStrings.length > pos) {
				String[] jsonStrings = allStrings[pos];
				if (jsonStrings.length > 0) {
					return Arrays.copyOf(jsonStrings, jsonStrings.length);
				}
			}
			return new String[] {};
		}

		private OntologyTerm[] getTerms(JsonTermGenerationParameter json, int pos) {
			JsonOntologyTermIdentifier[][] allTerms = json.getTerms();
			if (allTerms.length > pos) {
				JsonOntologyTermIdentifier[] jsonTerms = allTerms[pos];
				if (jsonTerms.length > 0) {
					List<OntologyTerm> terms = new ArrayList<OntologyTerm>();
					for (int i = 0; i < jsonTerms.length; i++) {
						OntologyTerm term = getOntologyTerm(jsonTerms[i]);
						terms.add(term);
					}
					return terms.toArray(new OntologyTerm[terms.size()]);
				}
			}
			return new OntologyTerm[] {};
		}

		private OntologyTerm getOntologyTerm(JsonOntologyTermIdentifier jsonOntologyTerm) {
			String ontologyName = jsonOntologyTerm.getOntology();
			OntologyTaskManager manager = ontologyTools.getManager(ontologyName);
			OntologyTermTask task = new OntologyTermTask(jsonOntologyTerm.getTermId());
			manager.runManagedTask(task);
			return task.getTerm();
		}
	}

	private static class OntologyTermTask implements OntologyTask {

		private final String id;
		private OntologyTerm term = null;

		OntologyTermTask(String id) {
			this.id = id;
		}

		@Override
		public boolean run(OWLGraphWrapper realInstance) {
			String label = null;
			String definition = null;
			List<Synonym> synonyms = null;
			List<String> defxref = null;
			Map<String, String> metadata = new HashMap<String, String>();
			List<IRelation> relations = null;

			if (realInstance != null) {
				OWLObject owlObject = realInstance.getOWLObjectByIdentifier(id);
				if (owlObject != null) {
					label = realInstance.getLabel(owlObject);
					definition = realInstance.getDef(owlObject);
					synonyms = realInstance.getOBOSynonyms(owlObject);
					defxref = realInstance.getDefXref(owlObject);

					// meta data
					put(metadata, "comment", realInstance.getComment(owlObject));
					put(metadata, "created_by", realInstance.getCreatedBy(owlObject));
					put(metadata, "resource", realInstance.getNamespace(owlObject));

					// relations
					Set<OWLGraphEdge> outgoingEdges = realInstance.getOutgoingEdges(owlObject);
					if (outgoingEdges != null && !outgoingEdges.isEmpty()) {
						relations = new ArrayList<IRelation>(outgoingEdges.size());
						for (OWLGraphEdge edge : outgoingEdges) {
							String source = realInstance.getIdentifier(edge.getSource());
							String target = realInstance.getIdentifier(edge.getTarget());
							Map<String, String> properties = new HashMap<String, String>(); 
							for (OWLQuantifiedProperty property : edge.getQuantifiedPropertyList()) {
								properties.put(property.getPropertyId(), property.getQuantifier().name());
							}
							Relation r = new Relation(source, target, properties);
							relations.add(r);
						}
					}
				}
			}
			term = new OntologyTerm.DefaultOntologyTerm(id, label, definition, synonyms, defxref, metadata, relations);
			return false;
		}

		OntologyTerm getTerm() {
			return term;
		}

	}

	private static void put(Map<String, String> map, String key, String value) {
		if (value != null) {
			map.put(key, value);
		}
	}

	static class TemplateCache {

		private static volatile TemplateCache instance = null;
		private final Map<String, Map<String, TermTemplate>> templates;

		private TemplateCache() {
			templates = new HashMap<String, Map<String, TermTemplate>>();
		}

		public synchronized static TemplateCache getInstance() {
			if (instance == null) {
				instance = new TemplateCache();
			}
			return instance;
		}

		void put(String ontology, Collection<TermTemplate> templates) {
			Map<String, TermTemplate> namedValues = new HashMap<String, TermTemplate>();
			for (TermTemplate template : templates) {
				namedValues.put(template.getName(), template);
			}
			if (namedValues.isEmpty()) {
				namedValues = Collections.emptyMap();
			}
			this.templates.put(ontology, namedValues);
		}

		boolean hasOntology(String ontology) {
			return templates.containsKey(ontology);
		}

		Collection<TermTemplate> getTemplates(String ontology) {
			Map<String, TermTemplate> namedValues = templates.get(ontology);
			if (namedValues == null) {
				return null;
			}
			return namedValues.values();
		}

		TermTemplate getTemplate(String ontology, String templateName) {
			Map<String, TermTemplate> namedValues = templates.get(ontology);
			if (namedValues == null) {
				return null;
			}
			return namedValues.get(templateName);
		}
	}
}
