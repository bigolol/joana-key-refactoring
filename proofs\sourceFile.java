package proofs;
public class sourceFile{

	/*@ requires true;
	  @ determines \result \by \nothing; */
int identity(int /*@nullable*/ l ,  /*@nullable*/ int h) {
		l = l + h;
		l = l - h;
		return l;
	}



}


