package proofs;
public class sourceFile{

	/*@ requires true;
	  @ determines \result \by \nothing; */
int testMethod(int /*@nullable*/ high ,  /*@nullable*/ int low) {
		int one = identity(low, high);
		return one;
	}

int identity(int l, int h) {
		l = l + h;
		l = l - h;
		return l;
	}


}


