// @requires rules/common.js

function biosynthesis_from() {
	var ont = GeneOntology; // the graph wrapper contains all info, including CHEBI
	var t = getSingleTerm("target", ont);
	var f = getSingleTerm("from", ont);

	var tname = termname(t, ont);
	var fname = termname(f, ont);

	var label = tname + " biosynthetic process from "+fname;
	var definition = "The chemical reactions and pathways resulting in the formation of "
					+ tname + "from "+ fname + ".";

	var synonyms = null;
//	synonyms = termgenie.addSynonym(label, null, null, tname, ' biosynthesis', 'EXACT');
//	synonyms = termgenie.addSynonym(label, synonyms, null, tname, ' biosynthetic process', 'EXACT');
//	synonyms = termgenie.addSynonym(label, synonyms, null, tname, ' anabolism', 'EXACT');
//	synonyms = termgenie.addSynonym(label, synonyms, null, tname, ' formation', 'EXACT');
//	synonyms = termgenie.addSynonym(label, synonyms, null, tname, ' synthesis', 'EXACT');

	var mdef = createMDef("GO_0009058 and 'has output' some ?T and 'has input' some ?F");
	mdef.addParameter('T', t, ont);
	mdef.addParameter('F', f, ont);
	return createTerm(label, definition, synonyms, mdef);
}
