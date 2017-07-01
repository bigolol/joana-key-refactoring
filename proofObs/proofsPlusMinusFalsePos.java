public class PlusMinusFalsePos {
	public static void main(String[] args) {
		new PlusMinusFalsePos().testMethod(1, 2);
	}

	public int testMethod(int high, int low) {
		int one = identity(low, high);
		return one;
	}

	/*@ requires true;
	  @ determines \result \by this, l; */
public identity(int/*@ nullable @*/ l, int/*@ nullable @*/ h) {
		l = l + h;
		l = l - h;
		return l;
	}
}
