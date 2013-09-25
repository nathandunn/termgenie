package org.bbop.termgenie.servlets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bbop.termgenie.core.ioc.IOCModule;
import org.bbop.termgenie.mail.MailHandler;
import org.bbop.termgenie.mail.SimpleMailHandler;
import org.bbop.termgenie.mail.review.DefaultReviewMailHandlerModule;
import org.bbop.termgenie.ontology.AdvancedPersistenceModule;
import org.bbop.termgenie.ontology.impl.SvnAwareXMLReloadingOntologyModule;
import org.bbop.termgenie.ontology.svn.CommitSvnUserPasswdModule;
import org.bbop.termgenie.presistence.PersistenceBasicModule;
import org.bbop.termgenie.rules.XMLDynamicRulesModule;
import org.bbop.termgenie.services.DefaultTermCommitServiceImpl;
import org.bbop.termgenie.services.TermCommitService;
import org.bbop.termgenie.services.TermGenieServiceModule;
import org.bbop.termgenie.services.freeform.FreeFormTermServiceModule;
import org.bbop.termgenie.services.permissions.UserPermissionsModule;
import org.bbop.termgenie.services.review.OboTermCommitReviewServiceImpl;
import org.bbop.termgenie.services.review.TermCommitReviewService;
import org.bbop.termgenie.services.review.TermCommitReviewServiceModule;
import org.semanticweb.owlapi.model.IRI;


public class TermGenieWebAppBattoContextListener extends AbstractTermGenieContextListener {

	private static final Logger logger = Logger.getLogger(TermGenieWebAppBattoContextListener.class);
	
	public TermGenieWebAppBattoContextListener() {
		super("TermGenieWebAppBattoConfigFile");
	}
	
	@Override
	protected IOCModule getUserPermissionModule() {
		return new UserPermissionsModule("termgenie-batto", applicationProperties);
	}
	
	@Override
	protected TermGenieServiceModule getServiceModule() {
		return new TermGenieServiceModule(applicationProperties) {

			@Override
			protected void bindTermCommitService() {
				bind(TermCommitService.class, DefaultTermCommitServiceImpl.class);
			}
			
			@Override
			public String getModuleName() {
				return "TermGenieBatto-TermGenieServiceModule";
			}
		};
	}
	
	@Override
	protected IOCModule getOntologyModule() {
		String configFile = "ontology-configuration_batto.xml";
		String repositoryURL = "svn+ssh://ext.geneontology.org/share/go/svn/trunk/ontology";
		String workFolder = null; // no default value
		String svnUserName = null; // no default value
		boolean loadExternal = true;
		
		Map<IRI, String> mappedIRIs = new HashMap<IRI, String>();
		
		mappedIRIs.put(IRI.create("http://purl.obolibrary.org/obo/go/extensions/bio-attributes.obo"), "extensions/bio-attributes.obo");
		
		mappedIRIs.put(IRI.create("http://purl.obolibrary.org/obo/go/extensions/x-attribute.obo"), "extensions/x-attribute.obo");
			
		String catalogXML = "extensions/catalog-v001.xml";
		
		List<String> ignoreIRIs = Arrays.asList(
				"http://purl.obolibrary.org/obo/go/extensions/bio-attributes.owl", 
				"http://purl.obolibrary.org/obo/go/extensions/x-attribute.owl",
				"http://purl.obolibrary.org/obo/go/extensions/x-attribute.obo.owl",
				"http://purl.obolibrary.org/obo/TEMP");
		
		Map<IRI, File> localMappings = getLocalMappings("TermGenieWebappBattoLocalIRIMappings");
		
		return SvnAwareXMLReloadingOntologyModule.createUsernamePasswordSvnModule(configFile, applicationProperties, repositoryURL, mappedIRIs, catalogXML, workFolder, svnUserName, loadExternal, ignoreIRIs, localMappings);
	}
	
	protected Map<IRI, File> getLocalMappings(String prefix) {
		Map<IRI, File> localMappings = new HashMap<IRI, File>();
		String localIRIMappings = IOCModule.getProperty(prefix, applicationProperties);
		if (localIRIMappings != null) {
			String[] localIRIMappingComponents = StringUtils.split(localIRIMappings, ','); // treat as comma separated list
			for (String component : localIRIMappingComponents) {
				String iri = IOCModule.getProperty(prefix+"."+component+".IRI", applicationProperties);
				String file = IOCModule.getProperty(prefix+"."+component+".File", applicationProperties);
				if (iri != null && file != null) {
					localMappings.put(IRI.create(iri), new File(file).getAbsoluteFile());
				}
			}
		}
		return localMappings;
	}

	@Override
	protected IOCModule getCommitModule() {
		
		String repositoryURL = "svn+ssh://ext.geneontology.org/share/go/svn/trunk/ontology";
		String remoteTargetFile = "extensions/bio-attributes.obo";
		String svnUserName = null; // no default value
		List<String> additional = Collections.emptyList();
		boolean loadExternal = true;
		
		return new CommitSvnUserPasswdModule(repositoryURL, remoteTargetFile, svnUserName, applicationProperties, "Batto", additional, loadExternal);
	}
	
	@Override
	protected IOCModule getRulesModule() {
		return new XMLDynamicRulesModule("termgenie_rules_batto.xml", false, applicationProperties);
	}
	
	@Override
	protected TermCommitReviewServiceModule getCommitReviewWebModule() {
		return new TermCommitReviewServiceModule(true, applicationProperties) {

			@Override
			public String getModuleName() {
				return "TermGenieBatto-TermCommitReviewServiceModule";
			}
			
			@Override
			protected void bindEnabled() {
				bind(TermCommitReviewService.class, OboTermCommitReviewServiceImpl.class);
			}
		};
	}
	

	@Override
	protected Collection<IOCModule> getAdditionalModules() {
		List<IOCModule> modules = new ArrayList<IOCModule>();
		PersistenceBasicModule basicPersistenceModule = getBasicPersistenceModule();
		if (basicPersistenceModule != null) {
			modules.add(basicPersistenceModule);
		}
		// commit history and ontology id store
		AdvancedPersistenceModule advancedPersistenceModule = getAdvancedPersistenceModule();
		if (advancedPersistenceModule != null) {
			modules.add(advancedPersistenceModule);
		}
		return modules;
	}

	protected AdvancedPersistenceModule getAdvancedPersistenceModule() {
		return new AdvancedPersistenceModule("BATTO-ID-Manager-Primary", 
				"ids/batto-id-manager-primary.conf",
				"BATTO-ID-Manager-Secondary", 
				"ids/batto-id-manager-secondary.conf", 
				applicationProperties);
	}

	protected PersistenceBasicModule getBasicPersistenceModule() {
		try {
			// basic persistence
			String dbFolderString = IOCModule.getProperty("TermGenieWebappBattoDatabaseFolder", applicationProperties);
			File dbFolder;
			if (dbFolderString != null && !dbFolderString.isEmpty()) {
				dbFolder = new File(dbFolderString);
			}
			else {
				dbFolder = new File(FileUtils.getUserDirectory(), "termgenie-batto-db");
			}
			dbFolder = dbFolder.getCanonicalFile();
			logger.info("Using db folder: "+dbFolder);
			FileUtils.forceMkdir(dbFolder);
			return new PersistenceBasicModule(dbFolder, applicationProperties);
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
	}
	
	@Override
	protected IOCModule getReviewMailHandlerModule() {
		
		return new DefaultReviewMailHandlerModule(applicationProperties, "help@go.termgenie.org", "Batto TermGenie") {
			
			@Override
			protected MailHandler provideMailHandler() {
				return new SimpleMailHandler("smtp.lbl.gov");
			}
		};
	}
	
	@Override
	protected IOCModule getFreeFormTermModule() {
		List<String> oboNamespaces = null;
		String defaultOntology = "default_batto";
		boolean addSubsetTag = false;
		String subset = null;
		return new FreeFormTermServiceModule(applicationProperties, addSubsetTag , defaultOntology, oboNamespaces, subset);
	}
}
