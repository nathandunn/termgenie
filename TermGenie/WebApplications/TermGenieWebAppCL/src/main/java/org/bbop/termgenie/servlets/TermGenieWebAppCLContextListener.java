package org.bbop.termgenie.servlets;

import org.bbop.termgenie.core.ioc.IOCModule;
import org.bbop.termgenie.core.rules.ReasonerModule;
import org.bbop.termgenie.ontology.impl.XMLReloadingOntologyModule;
import org.bbop.termgenie.rules.DefaultXMLDynamicRulesModule;

public class TermGenieWebAppCLContextListener extends AbstractTermGenieContextListener {

	@Override
	protected IOCModule getOntologyModule() {
		return new XMLReloadingOntologyModule("ontology-configuration_cl.xml");
	}

	@Override
	protected IOCModule getRulesModule() {
		return new DefaultXMLDynamicRulesModule("termgenie_rules_cl.xml");
	}

	@Override
	protected IOCModule getReasoningModule() {
		return new ReasonerModule("hermit");
	}

}
