package org.protelis.lang.datatype;

import java.util.List;

import org.protelis.lang.datatype.impl.ArrayTupleImpl;


public class TupleHelper{
	public static Tuple create(final List<?> l) {
        return create(l.toArray());
    }
	
	public static Tuple create(Object... l) {
		return new ArrayTupleImpl(l); 
    }

	
}
