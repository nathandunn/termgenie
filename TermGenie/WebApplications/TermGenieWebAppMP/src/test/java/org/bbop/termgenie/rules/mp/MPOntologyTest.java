package org.bbop.termgenie.rules.mp;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bbop.termgenie.core.TemplateField;
import org.bbop.termgenie.core.TermTemplate;
import org.bbop.termgenie.core.ioc.TermGenieGuice;
import org.bbop.termgenie.core.management.GenericTaskManager.InvalidManagedInstanceException;
import org.bbop.termgenie.core.rules.ReasonerModule;
import org.bbop.termgenie.core.rules.TermGenerationEngine;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationInput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationOutput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationParameters;
import org.bbop.termgenie.ontology.OntologyLoader;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager.OntologyTask;
import org.bbop.termgenie.ontology.impl.OntologyModule;
import org.bbop.termgenie.ontology.obo.OboWriterTools;
import org.bbop.termgenie.ontology.obo.OwlGraphWrapperNameProvider;
import org.bbop.termgenie.rules.XMLDynamicRulesModule;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.writer.OBOFormatWriter.NameProvider;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;

import owltools.graph.OWLGraphWrapper;

import com.google.inject.Injector;


public class MPOntologyTest {

	private static TermGenerationEngine generationEngine;
	private static OntologyLoader loader;
	
	@BeforeClass
	public static void beforeClass() {
		Injector injector = TermGenieGuice.createInjector(new XMLDynamicRulesModule("termgenie_rules_mp.xml", false, false, null),
				new OntologyModule("ontology-configuration_mp_test.xml"),
				new ReasonerModule(null));

		generationEngine = injector.getInstance(TermGenerationEngine.class);
		loader = injector.getInstance(OntologyLoader.class);
	}
	
	
	@Test
	public void testSyntax() throws Exception {
		OntologyTaskManager ontologyManager = loader.getOntologyManager();
		OntologyTask task = new OntologyTask(){

			@Override
			protected void runCatching(OWLGraphWrapper managed) throws TaskException, Exception {
				ManchesterSyntaxTool tool = new ManchesterSyntaxTool(managed.getSourceOntology(), null);
				OWLClassExpression expression = tool.parseManchesterExpression("('has part' some (PATO_0000051 and 'inheres in' some UBERON_0002028 and 'has component' some PATO_0000460))");
				assertNotNull(expression);
			}
		};
		ontologyManager.runManagedTask(task);
		if (task.getException() != null) {
			String message  = task.getMessage() != null ? task.getMessage() : task.getException().getMessage();
			fail(message);	
		}
	}
	
	@Test
	public void test_abnormal_morphology() throws Exception {
//		String id = "UBERON:0002028"; // hindbrain, exists already
		String id = "GO:0005791"; // rough endoplasmic reticulum
		TermGenerationOutput output = generateSingle(getTemplate("abnormal_morphology"), id);
		render(output);
		
	}
	
	@Test
	public void test_abnormal_process() throws Exception {
		// String id = "GO:0001783"; // B cell apoptosis, exists already
		// String id = "GO:0004031"; // aldehyde oxidase activity, exists already MP:0020026
		// String id = "GO:0018488"; // aryl-aldehyde oxidase activity
		String id = "GO:0002516"; // B cell deletion
		// String id = "GO:0044691"; // tooth eruption, exists already MP:0000119
		TermGenerationOutput output = generateSingle(getTemplate("abnormal_process"), id);
		render(output);
	}
	
	private TermGenerationOutput generateSingle(TermTemplate template, String id) {
		List<TermGenerationOutput> list = generate(template, id);
		assertNotNull(list);
		assertEquals(1, list.size());
		TermGenerationOutput output = list.get(0);
		assertNull(output.getError(), output.getError());
		return output;
	}
	
	private List<TermGenerationOutput> generate(TermTemplate template, String id) {
		TermGenerationParameters parameters = new TermGenerationParameters();
		List<TemplateField> fields = template.getFields();
		TemplateField field = fields.get(0);
		parameters.setTermValues(field.getName(), Arrays.asList(id)); 
		TermGenerationInput input = new TermGenerationInput(template, parameters);
		List<TermGenerationInput> generationTasks = Collections.singletonList(input);
		
		List<TermGenerationOutput> list = generationEngine.generateTerms(generationTasks, false, null);
		
		return list;
	}
	
	private TermTemplate getTemplate(String name) {
		List<TermTemplate> templates = generationEngine.getAvailableTemplates();
		for (TermTemplate template : templates) {
			String currentName = template.getName();
			if (name.equals(currentName)) {
				return template;
			}
		}
		throw new RuntimeException("No template found with name: "+name);
	}
	
	private void render(TermGenerationOutput output) throws InvalidManagedInstanceException  {
		System.out.println("-----------");
		Set<OWLAxiom> axioms = output.getOwlAxioms();
		for (OWLAxiom owlAxiom : axioms) {
			System.out.println(owlAxiom);
		}
		final Frame frame = output.getTerm();
		OntologyTaskManager ontologyManager = loader.getOntologyManager();
		OntologyTask task = new OntologyTask(){

			@Override
			protected void runCatching(OWLGraphWrapper managed) throws TaskException, Exception {
				NameProvider provider = new  OwlGraphWrapperNameProvider(managed);
				String obo = OboWriterTools.writeFrame(frame, provider);
				System.out.println("-----------");
				System.out.println(obo);
				System.out.println("-----------");
			}
		};
		ontologyManager.runManagedTask(task);
		if (task.getException() != null) {
			String message  = task.getMessage() != null ? task.getMessage() : task.getException().getMessage();
			fail(message);	
		}
	}
}
