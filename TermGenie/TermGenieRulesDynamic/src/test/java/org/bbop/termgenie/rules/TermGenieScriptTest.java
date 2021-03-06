package org.bbop.termgenie.rules;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bbop.termgenie.core.TemplateField;
import org.bbop.termgenie.core.TermTemplate;
import org.bbop.termgenie.core.ioc.TermGenieGuice;
import org.bbop.termgenie.core.rules.ReasonerModule;
import org.bbop.termgenie.core.rules.TermGenerationEngine;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationInput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationOutput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationParameters;
import org.bbop.termgenie.ontology.obo.OboTools;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;

import com.google.inject.Injector;

public class TermGenieScriptTest {

	private static TermGenerationEngine generationEngine;

	@BeforeClass
	public static void beforeClass() {
		Injector injector = TermGenieGuice.createInjector(new XMLDynamicRulesModule("termgenie_rules_simple.xml", false, true, false, null),
				new OldTestOntologyModule("ontology-configuration_simple.xml"),
				new ReasonerModule(null));

		generationEngine = injector.getInstance(TermGenerationEngine.class);
	}

	@Test
	public void test_lit_xref_warning() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		parameters.setTermValues(field.getName(), Arrays.asList("GO:0043473"));
		parameters.setStringValues(field.getName(),
				Arrays.asList("regulation"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		
		TermGenerationOutput output = list.get(0);
		assertNull(output.getError());
		List<String> warnings = output.getWarnings();
		assertNotNull(warnings);
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("literature reference"));
	}
	
	@Test
	public void test_lit_xref_error() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		parameters.setTermValues(field.getName(), Arrays.asList("GO:0043473"));
		parameters.setStringValues(field.getName(),
				Arrays.asList("regulation"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, true, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		
		TermGenerationOutput output = list.get(0);
		assertNotNull(output.getError());
		assertTrue(output.getError().contains("literature reference"));
	}
	
	@Test
	public void test_regulation_of() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		parameters.setTermValues(field.getName(), Arrays.asList("GO:0043473"));
		parameters.setStringValues(field.getName(),
				Arrays.asList("regulation", "negative_regulation", "positive_regulation"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(3, list.size());

		Frame term1 = list.get(0).getTerm();
		assertEquals("regulation of pigmentation", term1.getTagValue(OboFormatTag.TAG_NAME, String.class));

		Frame term2 = list.get(1).getTerm();
		assertEquals("down regulation of pigmentation", term2.getTagValue(OboFormatTag.TAG_SYNONYM));
		assertEquals("negative regulation of pigmentation", term2.getTagValue(OboFormatTag.TAG_NAME, String.class));
		assertEquals("positive regulation of pigmentation", list.get(2).getTerm().getTagValue(OboFormatTag.TAG_NAME, String.class));
	}
	
	@Test
	public void test_regulation_of_extended_synonyms() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		parameters.setTermValues(field.getName(), Arrays.asList("GO:0051553")); // flavone biosynthetic process
		parameters.setStringValues(field.getName(), Arrays.asList("negative_regulation"));
		
		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());

		final TermGenerationOutput output = list.get(0);
		assertNull(output.getError());
		Frame term = output.getTerm();
		
		Set<String> validSynonyms = new HashSet<String>(Arrays.asList("down regulation of flavone biosynthetic process",
				"down-regulation of flavone biosynthetic process",
				"downregulation of flavone biosynthetic process",
				"inhibition of flavone biosynthetic process",
				"negative regulation of 2-phenyl-4H-1-benzopyran-4-one biosynthetic process",
				"down regulation of 2-phenyl-4H-1-benzopyran-4-one biosynthetic process",
				"down-regulation of 2-phenyl-4H-1-benzopyran-4-one biosynthetic process",
				"downregulation of 2-phenyl-4H-1-benzopyran-4-one biosynthetic process",
				"inhibition of 2-phenyl-4H-1-benzopyran-4-one biosynthetic process",
				"negative regulation of 2-phenylchromone biosynthesis",
				"down regulation of 2-phenylchromone biosynthesis",
				"down-regulation of 2-phenylchromone biosynthesis",
				"downregulation of 2-phenylchromone biosynthesis",
				"inhibition of 2-phenylchromone biosynthesis",
				"negative regulation of 2-phenylchromone biosynthetic process",
				"down regulation of 2-phenylchromone biosynthetic process",
				"down-regulation of 2-phenylchromone biosynthetic process",
				"downregulation of 2-phenylchromone biosynthetic process",
				"inhibition of 2-phenylchromone biosynthetic process",
				"negative regulation of 2-phenyl-4H-1-benzopyran-4-one biosynthesis",
				"down regulation of 2-phenyl-4H-1-benzopyran-4-one biosynthesis",
				"down-regulation of 2-phenyl-4H-1-benzopyran-4-one biosynthesis",
				"downregulation of 2-phenyl-4H-1-benzopyran-4-one biosynthesis",
				"inhibition of 2-phenyl-4H-1-benzopyran-4-one biosynthesis"));
		
		Collection<Clause> synonyms = term.getClauses(OboFormatTag.TAG_SYNONYM);
		for (Clause synonym : synonyms) {
			String label = synonym.getValue(String.class);
			assertTrue("Not a valid synonym label: "+label, validSynonyms.contains(label));
		}
		
	}
	
	@Test
	public void test_regulation_of_weaker_check_of_ancestor() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		// intracellular vesicle pattern recognition receptor signaling pathway
		parameters.setTermValues(field.getName(), Arrays.asList("GO:0002754"));
		parameters.setStringValues(field.getName(),
				Arrays.asList("regulation", "negative_regulation", "positive_regulation"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(3, list.size());

		Frame term1 = list.get(0).getTerm();
		assertEquals("regulation of intracellular vesicle pattern recognition receptor signaling pathway", term1.getTagValue(OboFormatTag.TAG_NAME, String.class));

		Frame term2 = list.get(1).getTerm();
		assertEquals("down regulation of intracellular vesicle pattern recognition receptor signaling pathway", term2.getTagValue(OboFormatTag.TAG_SYNONYM));
		assertEquals("negative regulation of intracellular vesicle pattern recognition receptor signaling pathway", term2.getTagValue(OboFormatTag.TAG_NAME, String.class));
		
		Frame term3 = list.get(2).getTerm();
		assertEquals("positive regulation of intracellular vesicle pattern recognition receptor signaling pathway", term3.getTagValue(OboFormatTag.TAG_NAME, String.class));
		assertEquals("up regulation of intracellular vesicle pattern recognition receptor signaling pathway", term3.getTagValue(OboFormatTag.TAG_SYNONYM));
	}
	
	@Test
	public void test_regulation_of_fail() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		parameters.setTermValues(field.getName(), Arrays.asList("GO:0051782")); // negative regulation of cell division
		parameters.setStringValues(field.getName(),
				Arrays.asList("regulation", "negative_regulation", "positive_regulation"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		
		TermGenerationOutput output = list.get(0);
		assertNotNull(output.getError());
		assertTrue(output.getError().contains("biological regulation"));
	}
	
	@Test
	public void test_regulation_of_specific_relation() {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(0);
		TermGenerationParameters parameters = new TermGenerationParameters();

		TemplateField field = termTemplate.getFields().get(0);

		parameters.setTermValues(field.getName(), Arrays.asList("GO:0072225"));
		parameters.setStringValues(field.getName(), Collections.singletonList("regulation"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());

		Frame term = list.get(0).getTerm();
		assertEquals("regulation of metanephric late distal convoluted tubule development", term.getTagValue(OboFormatTag.TAG_NAME, String.class));
		assertEquals("GO:0072215", term.getTagValue(OboFormatTag.TAG_IS_A)); // regulation of metanephros development
	}

	@Test
	public void test_involved_in_relations() throws Exception {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(1);
		TermGenerationParameters parameters = new TermGenerationParameters();

		String field0 = termTemplate.getFields().get(0).getName();
		String field1 = termTemplate.getFields().get(1).getName();

		parameters.setTermValues(field0,
				Arrays.asList("GO:0046836")); // glycolipid transport
		parameters.setTermValues(field1, 
				Arrays.asList("GO:0006915")); // apoptosis

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		TermGenerationOutput output = list.get(0);
		assertNull(output.getError());
		Frame term = output.getTerm();

		List<Clause> clauses = OboTools.getRelations(term);
		assertEquals(4, clauses.size());
		Clause clause0 = clauses.get(0);
		assertEquals(OboFormatTag.TAG_IS_A.getTag(), clause0.getTag());
		assertEquals("GO:0046836", clause0.getValue());

		Clause clause1 = clauses.get(1);
		assertEquals(OboFormatTag.TAG_INTERSECTION_OF.getTag(), clause1.getTag());
		assertEquals("GO:0046836", clause1.getValue());

		Clause clause2 = clauses.get(2);
		assertEquals(OboFormatTag.TAG_INTERSECTION_OF.getTag(), clause2.getTag());
		assertEquals("part_of", clause2.getValue());
		assertEquals("GO:0006915", clause2.getValue2());

		Clause clause3 = clauses.get(3);
		assertEquals(OboFormatTag.TAG_RELATIONSHIP.getTag(), clause3.getTag());
		assertEquals("part_of", clause3.getValue());
		assertEquals("GO:0006915", clause3.getValue2());
	}
	
	@Test
	public void test_involved_in_mf_bp() throws Exception {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(2);
		TermGenerationParameters parameters = new TermGenerationParameters();
		
		String field0 = termTemplate.getFields().get(0).getName();
		String field1 = termTemplate.getFields().get(1).getName();

		parameters.setTermValues(field0,
				Arrays.asList("GO:0017089")); // glycolipid transporter activity
		parameters.setTermValues(field1, 
				Arrays.asList("GO:0006915")); // apoptosis
		
		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		TermGenerationOutput output = list.get(0);
		assertNull(output.getError());
		Frame term = output.getTerm();
		
		assertEquals("molecular_function", term.getTagValue(OboFormatTag.TAG_NAMESPACE));
		
		List<Clause> clauses = OboTools.getRelations(term);
		assertEquals(4, clauses.size());
		Clause clause0 = clauses.get(0);
		assertEquals(OboFormatTag.TAG_IS_A.getTag(), clause0.getTag());
		assertEquals("GO:0017089", clause0.getValue());

		Clause clause1 = clauses.get(1);
		assertEquals(OboFormatTag.TAG_INTERSECTION_OF.getTag(), clause1.getTag());
		assertEquals("GO:0017089", clause1.getValue());

		Clause clause2 = clauses.get(2);
		assertEquals(OboFormatTag.TAG_INTERSECTION_OF.getTag(), clause2.getTag());
		assertEquals("part_of", clause2.getValue());
		assertEquals("GO:0006915", clause2.getValue2());

		Clause clause3 = clauses.get(3);
		assertEquals(OboFormatTag.TAG_RELATIONSHIP.getTag(), clause3.getTag());
		assertEquals("part_of", clause3.getValue());
		assertEquals("GO:0006915", clause3.getValue2());
	}
	
	@Test
	@Ignore("XP has changed for GO:0086088")
	public void test_involved_in_mf_bp_existing() throws Exception {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(2);
		TermGenerationParameters parameters = new TermGenerationParameters();
		
		String field0 = termTemplate.getFields().get(0).getName();
		String field1 = termTemplate.getFields().get(1).getName();

		parameters.setTermValues(field0,
				Arrays.asList("GO:0005249")); // voltage-gated potassium channel activity
		parameters.setTermValues(field1, 
				Arrays.asList("GO:0086017")); // regulation of Purkinje myocyte action potential
		
		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		TermGenerationOutput output = list.get(0);
		assertNotNull(output.getError());
		assertTrue(output.getError().contains("GO:0086088")); // voltage-gated potassium channel activity involved in Purkinje myocyte action potential repolarization
	}
	
	@Test
	public void test_occurs_in_relations() throws Exception {
		TermTemplate termTemplate = generationEngine.getAvailableTemplates().get(3);
		TermGenerationParameters parameters = new TermGenerationParameters();

		String field0 = termTemplate.getFields().get(0).getName();
		String field1 = termTemplate.getFields().get(1).getName();

		parameters.setTermValues(field0, Arrays.asList("GO:0019660"));
		parameters.setTermValues(field1, Arrays.asList("GO:0005777"));

		TermGenerationInput input = new TermGenerationInput(termTemplate, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);

		assertNotNull(list);
		assertEquals(1, list.size());
		TermGenerationOutput output = list.get(0);
		assertNull(output.getError());
		Frame term = output.getTerm();

		List<Clause> clauses = OboTools.getRelations(term);
		assertEquals(4, clauses.size());
		Clause clause0 = clauses.get(0);
		assertEquals(OboFormatTag.TAG_IS_A.getTag(), clause0.getTag());
		assertEquals("GO:0019660", clause0.getValue());

		Clause clause1 = clauses.get(1);
		assertEquals(OboFormatTag.TAG_INTERSECTION_OF.getTag(), clause1.getTag());
		assertEquals("GO:0019660", clause1.getValue());

		Clause clause2 = clauses.get(2);
		assertEquals(OboFormatTag.TAG_INTERSECTION_OF.getTag(), clause2.getTag());
		assertEquals("occurs_in", clause2.getValue());
		assertEquals("GO:0005777", clause2.getValue2());

		Clause clause3 = clauses.get(3);
		assertEquals(OboFormatTag.TAG_RELATIONSHIP.getTag(), clause3.getTag());
		assertEquals("occurs_in", clause3.getValue());
		assertEquals("GO:0005777", clause3.getValue2());
	}

}
