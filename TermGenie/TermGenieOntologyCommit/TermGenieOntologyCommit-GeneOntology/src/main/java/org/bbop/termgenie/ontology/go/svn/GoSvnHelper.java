package org.bbop.termgenie.ontology.go.svn;

import java.io.File;

import org.bbop.termgenie.ontology.CommitException;
import org.bbop.termgenie.ontology.CommitInfo.CommitMode;
import org.bbop.termgenie.ontology.obo.OboScmHelper;
import org.bbop.termgenie.ontology.IRIMapper;
import org.bbop.termgenie.ontology.OntologyCleaner;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.scm.VersionControlAdapter;
import org.bbop.termgenie.svn.SvnTool;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Main steps for committing ontology changes to an OBO file in an CVS
 * repository.
 */
public class GoSvnHelper {

	@Singleton
	public static final class GoSvnHelperPassword extends OboScmHelper {

		private final String svnRepository;
		private final String svnUsername;
		private final String svnPassword;

		@Inject
		GoSvnHelperPassword(@Named("GeneOntology") OntologyTaskManager source,
				IRIMapper iriMapper,
				OntologyCleaner cleaner,
				@Named("GeneOntologyCommitAdapterSVNRepositoryUrl") String svnRepository,
				@Named("GeneOntologyCommitAdapterSVNOntologyFileName") String svnOntologyFileName,
				@Named("GeneOntologyCommitAdapterSVNUsername") String svnUsername,
				@Named("GeneOntologyCommitAdapterSVNPassword") String svnPassword)
		{
			super(source, iriMapper, cleaner, svnOntologyFileName);
			this.svnRepository = svnRepository;
			this.svnUsername = svnUsername;
			this.svnPassword = svnPassword;
		}

		@Override
		public VersionControlAdapter createSCM(CommitMode commitMode,
				String username,
				String password,
				File svnFolder)
		{
			String realUsername;
			String realPassword;
			if (commitMode == CommitMode.internal) {
				realUsername = svnUsername;
				realPassword = svnPassword;
			}
			else {
				realUsername = username;
				realPassword = password;
			}
			SvnTool svn = SvnTool.createUsernamePasswordSVN(svnFolder, svnRepository, realUsername, realPassword);
			return svn;
		}

		@Override
		public boolean isSupportAnonymus() {
			return false;
		}

		@Override
		public CommitMode getCommitMode() {
			return CommitMode.explicit;
		}

		@Override
		public String getCommitUserName() {
			return svnUsername;
		}

		@Override
		public String getCommitPassword() {
			return svnPassword;
		}
	}

	@Singleton
	public static final class GoSvnHelperAnonymous extends OboScmHelper {

		private final String svnRepository;

		@Inject
		GoSvnHelperAnonymous(@Named("GeneOntology") OntologyTaskManager source,
				IRIMapper iriMapper,
				OntologyCleaner cleaner,@Named("GeneOntologyCommitAdapterSVNRepositoryUrl") String svnRepository,
				@Named("GeneOntologyCommitAdapterSVNOntologyFileName") String svnOntologyFileName)
		{
			super(source, iriMapper, cleaner, svnOntologyFileName);
			this.svnRepository = svnRepository;
		}

		@Override
		public VersionControlAdapter createSCM(CommitMode commitMode,
				String username,
				String password,
				File svnFolder) throws CommitException
		{
			return SvnTool.createAnonymousSVN(svnFolder, svnRepository);
		}

		@Override
		public boolean isSupportAnonymus() {
			return true;
		}

		@Override
		public CommitMode getCommitMode() {
			return CommitMode.anonymus;
		}

		@Override
		public String getCommitUserName() {
			return null; // no username
		}

		@Override
		public String getCommitPassword() {
			return null; // no password
		}
	}

	@Singleton
	public static final class GoSvnHelperKeyFile extends OboScmHelper {
	
		private final String svnRepository;
		private final String svnUsername;
		private final File svnKeyFile;
		private final String svnPassword;
	
		@Inject
		GoSvnHelperKeyFile(@Named("GeneOntology") OntologyTaskManager source,
				IRIMapper iriMapper,
				OntologyCleaner cleaner,
				@Named("GeneOntologyCommitAdapterSVNRepositoryUrl") String svnRepository,
				@Named("GeneOntologyCommitAdapterSVNOntologyFileName") String svnOntologyFileName,
				@Named("GeneOntologyCommitAdapterSVNUsername") String svnUsername,
				@Named("GeneOntologyCommitAdapterSVNKeyFile") File svnKeyFile,
				@Named("GeneOntologyCommitAdapterSVNPassword") String svnPassword)
		{
			super(source, iriMapper, cleaner, svnOntologyFileName);
			this.svnRepository = svnRepository;
			this.svnUsername = svnUsername;
			this.svnKeyFile = svnKeyFile;
			this.svnPassword = svnPassword;
		}
	
		@Override
		public VersionControlAdapter createSCM(CommitMode commitMode,
				String username,
				String password,
				File svnFolder)
		{
			String realUsername;
			String realPassword;
			if (commitMode == CommitMode.internal) {
				realUsername = svnUsername;
				realPassword = svnPassword;
			}
			else {
				realUsername = username;
				realPassword = password;
			}
			SvnTool svn = SvnTool.createSSHKeySVN(svnFolder, svnRepository, realUsername, svnKeyFile, realPassword);
			return svn;
		}
	
		@Override
		public boolean isSupportAnonymus() {
			return false;
		}
	
		@Override
		public CommitMode getCommitMode() {
			return CommitMode.explicit;
		}
	
		@Override
		public String getCommitUserName() {
			return svnUsername;
		}
	
		@Override
		public String getCommitPassword() {
			return svnPassword;
		}
	}

	private GoSvnHelper() {
		// no instances
	}
}
