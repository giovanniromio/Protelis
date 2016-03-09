package org.protelis.lang.datatype;


import java.util.List;
import org.protelis.lang.datatype.impl.ArrayTupleImpl;



public class TupleHelper {
	
	public static Tuple create(final List<?> l) {
        return create(l.toArray());
    }
	
	public static Tuple create(Object... l) {
		return new ArrayTupleImpl(l); 
    }
	
	public static Tuple fill(Object value, int length) {
        return new ArrayTupleImpl(value, length);
    }
	
	public static Tuple union(Tuple t1, Tuple t2) {
        return t1.union(t2);
    }
	
	public static Tuple intersection(Tuple t1, Tuple t2) {
        return t1.intersection(t2);
    }
	
	public static Tuple subtract(Tuple t1, Tuple t2) {
        return t1.subtract(t2);
    }
	
	
}
