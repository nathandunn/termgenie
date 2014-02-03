package org.bbop.termgenie.ontology.svn;

import java.util.List;
import java.util.Properties;

import org.bbop.termgenie.mail.review.ReviewMailHandler;
import org.bbop.termgenie.ontology.CommitHistoryStore;
import org.bbop.termgenie.ontology.OntologyCommitReviewPipelineStages;
import org.bbop.termgenie.ontology.OntologyLoader;
import org.bbop.termgenie.ontology.ScmHelper;
import org.bbop.termgenie.ontology.TermFilter;
import org.bbop.termgenie.ontology.obo.DefaultOboTermFilter;
import org.bbop.termgenie.ontology.obo.OboCommitReviewPipeline;
import org.obolibrary.oboformat.model.OBODoc;

import com.google.inject.Provides;
import com.google.inject.Singleton;

abstract class AbstractOboCommitSvnModule extends AbstractCommitSvnModule {

	private final List<String> additionalOntologyFileNames;

	/**
	 * @param svnRepository
	 * @param svnOntologyFileName
	 * @param applicationProperties
	 * @param additionalOntologyFileNames
	 * @param svnLoadExternals
	 */
	protected AbstractOboCommitSvnModule(String svnRepository,
			String svnOntologyFileName,
			Properties applicationProperties,
			List<String> additionalOntologyFileNames,
			boolean svnLoadExternals)
	{
		super(svnRepository, svnOntologyFileName, applicationProperties, svnLoadExternals);
		this.additionalOntologyFileNames = additionalOntologyFileNames;
	}

	@Override
	protected void configure() {
		super.configure();
		bindList("CommitAdapterSVNAdditionalOntologyFileNames", additionalOntologyFileNames, true);
	}

	@Singleton
	@Provides
	protected OntologyCommitReviewPipelineStages provideReviewStages(OntologyLoader loader,
			CommitHistoryStore store,
			TermFilter<OBODoc> filter,
			ReviewMailHandler handler,
			ScmHelper<OBODoc> helper)
	{
		return new OboCommitReviewPipeline(loader.getOntologyManager(), store, filter, handler, helper);
	}

	@Singleton
	@Provides
	protected TermFilter<OBODoc> provideTermFilter() {
		return new DefaultOboTermFilter();
	}
}