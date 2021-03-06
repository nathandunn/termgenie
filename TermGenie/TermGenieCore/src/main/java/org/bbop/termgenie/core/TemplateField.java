package org.bbop.termgenie.core;

import java.util.Collections;
import java.util.List;

import org.bbop.termgenie.core.Ontology.OntologySubset;

/**
 * Specifications for a field in an ontology term generation template.
 */
public class TemplateField {

	private final String name;
	private final String label;
	private final String hint;
	private final boolean required;
	private final Cardinality cardinality;
	
	private final List<String> functionalPrefixes;
	private final List<String> functionalPrefixesIds;
	private final boolean preSelected;
	
	private final OntologySubset subset;
	private final String remoteResource;

	/**
	 * Constant: Fields, which require exactly one input.
	 */
	public static final Cardinality SINGLE_FIELD_CARDINALITY = new Cardinality() {

		@Override
		public int getMinimum() {
			return 1;
		}

		@Override
		public int getMaximum() {
			return 1;
		}
	};

	/**
	 * Constant: Fields, which require at least two inputs of the same type.
	 */
	public static final Cardinality TWO_TO_N_CARDINALITY = new Cardinality() {

		@Override
		public int getMinimum() {
			return 2;
		}

		@Override
		public int getMaximum() {
			return Integer.MAX_VALUE;
		}
	};

	/**
	 * Constant: Fields, which require at least one input.
	 */
	public static final Cardinality ONE_TO_N_CARDINALITY = new Cardinality() {

		@Override
		public int getMinimum() {
			return 1;
		}

		@Override
		public int getMaximum() {
			return Integer.MAX_VALUE;
		}
	};

	/**
	 * Specify the cardinality of a field.
	 */
	public static abstract class Cardinality {

		public abstract int getMinimum();

		public abstract int getMaximum();

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(getMinimum());
			sb.append(getMaximum() == Integer.MAX_VALUE ? "N" : Integer.toString(getMaximum()));
			return sb.toString();
		}
	}

	/**
	 * Standard constructor for specifying all parameters of a field.
	 * 
	 * @param name
	 * @param label
	 * @param hint
	 * @param required
	 * @param cardinality
	 * @param functionalPrefixes
	 * @param functionalPrefixesIds
	 * @param preSelected
	 * @param subset
	 * @param remoteResource 
	 */
	public TemplateField(String name,
			String label,
			String hint,
			boolean required,
			Cardinality cardinality,
			List<String> functionalPrefixes,
			List<String> functionalPrefixesIds,
			boolean preSelected,
			OntologySubset subset,
			String remoteResource)
	{
		super();
		this.name = name;
		this.label = label;
		this.hint = hint;
		this.required = required;
		this.cardinality = cardinality;
		if (functionalPrefixes == null || functionalPrefixes.isEmpty()) {
			this.functionalPrefixes = Collections.emptyList();
		}
		else {
			this.functionalPrefixes = Collections.unmodifiableList(functionalPrefixes);
		}
		if (functionalPrefixesIds == null || functionalPrefixesIds.isEmpty()) {
			this.functionalPrefixesIds = Collections.emptyList();
		}
		else {
			this.functionalPrefixesIds = Collections.unmodifiableList(functionalPrefixesIds);
		}
		this.preSelected = preSelected;
		this.subset = subset;
		this.remoteResource = remoteResource;
	}

	/**
	 * @return name of this field.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * @return the hint
	 */
	public String getHint() {
		return hint;
	}

	/**
	 * @return true if this field is a required parameter, otherwise false
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * @return cardinality
	 */
	public Cardinality getCardinality() {
		return cardinality;
	}

	/**
	 * @return list of functional prefix, to choose from. Never null, but
	 *         returns empty lists.
	 */
	public List<String> getFunctionalPrefixes() {
		return functionalPrefixes;
	}
	
	/**
	 * @return the functionalPrefixesIds
	 */
	public List<String> getFunctionalPrefixesIds() {
		return functionalPrefixesIds;
	}

	/**
	 * @return the preSelected
	 */
	public boolean isPreSelected() {
		return preSelected;
	}

	public OntologySubset getSubset() {
		return subset;
	}

	/**
	 * @return the remoteResource
	 */
	public String getRemoteResource() {
		return remoteResource;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TemplateField [");
		builder.append("name=");
		builder.append(name);
		if (label != null) {
			builder.append(", ");
			builder.append("label=");
			builder.append(label);
		}
		builder.append(", ");
		builder.append("required=");
		builder.append(required);
		if (cardinality != null) {
			builder.append(", ");
			builder.append("cardinality=");
			builder.append(cardinality);
		}
		if (functionalPrefixes != null) {
			builder.append(", ");
			builder.append("functionalPrefixes=");
			builder.append(functionalPrefixes);
		}
		if (subset != null) {
			builder.append(", ");
			builder.append("subset=");
			builder.append(subset.getName());
		}
		if (remoteResource != null) {
			builder.append(", ");
			builder.append("remoteResource=");
			builder.append(remoteResource);
		}
		builder.append("]");
		return builder.toString();
	}

}
