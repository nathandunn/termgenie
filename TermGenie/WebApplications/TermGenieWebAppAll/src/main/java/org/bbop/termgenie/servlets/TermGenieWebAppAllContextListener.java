package org.bbop.termgenie.servlets;

import org.bbop.termgenie.core.ioc.IOCModule;
import org.bbop.termgenie.core.rules.ReasonerModule;
import org.bbop.termgenie.ontology.impl.XMLReloadingOntologyModule;
import org.bbop.termgenie.rules.XMLDynamicRulesModule;
import org.bbop.termgenie.services.permissions.UserPermissionsModule;

public class TermGenieWebAppAllContextListener extends AbstractTermGenieContextListener {

	@Override
	protected IOCModule getUserPermissionModule() {
		return new UserPermissionsModule("termgenie-all");
	}
	
	@Override
	protected IOCModule getOntologyModule() {
		return new XMLReloadingOntologyModule("ontology-configuration_all.xml");
	}

	@Override
	protected IOCModule getRulesModule() {
		return new XMLDynamicRulesModule("termgenie_rules_all.xml");
	}

	@Override
	protected IOCModule getReasoningModule() {
		return new ReasonerModule("hermit");
	}
}
