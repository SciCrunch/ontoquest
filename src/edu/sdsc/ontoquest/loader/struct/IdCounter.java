package edu.sdsc.ontoquest.loader.struct;

import edu.sdsc.ontoquest.Context;
import edu.sdsc.ontoquest.OntoquestException;
import edu.sdsc.ontoquest.db.DbUtility;

public class IdCounter {
	int currentVal = -1;
	/** initial sequence value */
	int initialVal = -1;
	/** sequence property for this instance of IdCounter. The properties are listed in loader's configuration file (default: load_pgsql.xml) */
	String seqProp = null;
	/** The app context holding database connection info */
	Context context = null;

	public IdCounter(String seqProp, Context context) {
		this.seqProp = seqProp;
		this.context = context;
	}

	public int getCurrentValue() {
		return currentVal;
	}

	public int getInitialValue() {
		return initialVal;
	}

	public int increment() throws OntoquestException {
		if (currentVal < 0) {
			currentVal = DbUtility.fetchSeqNextVal(seqProp, context);
			initialVal = currentVal;
		} else {
			currentVal++;
		}
		return currentVal;
	}

	public boolean isUsed() {
		return currentVal >= 0;
	}
}
