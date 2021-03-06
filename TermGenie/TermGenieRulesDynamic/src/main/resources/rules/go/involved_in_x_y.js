// @requires rules/common.js

function involved_in_x_y(x, y){
  var go = GeneOntology;
  var p = getSingleTerm("part", go);
  var w = getSingleTerm("whole", go);
  var check = checkGenus(p, x, go);
  if (check.isGenus() !== true) {
    error(check.error());
    return;
  }
  var check = checkGenus(w, y, go);
  if (check.isGenus() !== true) {
    error(check.error());
    return;
  }
  var label = termname(p, go) + " involved in " + termname(w, go);
  var definition = "Any "+termname(p, go)+" that is involved in "+termname(w, go)+".";
  var synonyms = termgenie.synonyms(null, p, go, " involved in ", w, go, null, null, label);
  var mdef = createMDef("?P and 'part of' some ?W");
  mdef.addParameter('P', p, go);
  mdef.addParameter('W', w, go);
  createTerm(label, definition, synonyms, mdef);
}
