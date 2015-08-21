package de.janrufmonitor.repository.search;

public class Operator {

	public final static Operator AND = new Operator("AND");
	public final static Operator OR  = new Operator("OR");
	
	private String _op ;
	
	public Operator(String op) {
		this._op = op;
	}
	
	public int hashCode() {
		return this._op.hashCode();
	}
	
	public String toString() {
		return this._op;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Operator) {
			if (this._op == ((Operator)o)._op)
				return true;	
		}
		return false;
	}
}
