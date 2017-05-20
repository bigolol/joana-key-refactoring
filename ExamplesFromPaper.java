
public class ExampleFalsePositives {
	public static void main(String[] args) {
		new ExampleFalsePositives().testMethod(1, 2);
	}

	public int testMethod(int high, int low) {
		int one = identity1(low, high);
		return one;
	}

	public int identity(int l, int h) {
		l = l + h;
		l = l - h;
		return l;
	}

	public int precondition(int l, int h) {
		int x = 0;
		if (x > 0) {
			l = h;
		}
		return l;
	}
	
	public int excludingStatements(int x, int l, int h) {
		int z = 0;
		if (x == 3) {
			z = h;
		}
		if (x != 3) {
			l = z;
		}
		z = 0;
		return l;
	}
	
	public int loopOverride(int l, int h) {
		int y = l;
		for (int i = 0; i < 5; i++) {
			if (i < 4) {
				l = l + h;
			} else {
				l = y;
			}
		}
		return l;
	}
	
	
	private int arrayAccess(int l, int h) {
		int[] array = new int[3];
		array[0] = l;
		array[1] = h;
		array[2] = array[1];
		return array[2];
	}
	
	public int keYExample(int high, int low) {
		if (high > 0) {
			low = n5(high, high);
		} else {
			high = -high;
			low = n5(high + low, high);
		}
		return low;
	}

	public int n5(int x, int high) {
		high = 2 * x;
		return 15;
	}
	
	
	
	public int singleFlow(int high, int low) {
		low = plus(low, high);
		low = minus(low, high);
		return low;
	}

	private int minus(int low, int high) {
		low = low - high;
		return low;
	}

	private int plus(int low, int high) {
		low = low + high;
		return low;
	}
	
	public int branching(int high, int low) {
		int i = plus(low, high);
		int j = minus(low, high);
		return i + j;
	}

	private int plus(int low, int high) {
		low = low + high;
		return low;
	}

	private int minus(int low, int high) {
		low = low - high;
		return low;
	}

	
	public int nested(int high, int low) {
		low = plusMinus(low, high);
		return low;
	}

	private int plusMinus(int low, int high) {
		low = low + high;
		low = minus(low, high);
		return low;
	}

	private int minus(int low, int high) {
		low = low - high;
		return low;
	}
	
	
	public int Mixture(int high, int low) {
		int i = identity(low, high);
		int j = precondition(low, high);
		int k = secure(low);
		return i + j + k;
	}
	private int secure(int low) {
		return low;
	}
	
	private int MixtureWithLoop(int l, int h) {
		int x = arrayInsecure(l, h);
		x = loopOverride(l, x);
		x += arrayAccess(x, h);
		x = justSet(l, x);
		int y = loopOverride(l, h);
		return x + y;
	}
	private int justSet(int l, int h) {
		int[] array = new int[5];
		array[0] = 5;
		array[1] = h;
		l = array[0];
		return l;
	}
}
