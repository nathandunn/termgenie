package org.bbop.termgenie.servlets;

import org.bbop.termgenie.core.ioc.IOCModule;
import org.bbop.termgenie.core.rules.ReasonerModule;
import org.bbop.termgenie.ontology.impl.XMLReloadingOntologyModule;
import org.bbop.termgenie.rules.DefaultXMLDynamicRulesModule;

public class JsonRPCServlet extends AbstractJsonRPCServlet {

	// generated
	private static final long serialVersionUID = -3052651034871303985L;

	@Override
	protected ServiceExecutor createServiceExecutor() {
		return new ServiceExecutor() {

			@Override
			protected IOCModule getOntologyModule() {
				return new XMLReloadingOntologyModule("ontology-configuration_simple.xml");
			}

			@Override
			protected IOCModule getRulesModule() {
				return new DefaultXMLDynamicRulesModule("termgenie_rules_simple.xml");
			}

			@Override
			protected IOCModule getReasoningModule() {
				return new ReasonerModule("hermit");
			}
		};
	}
}