package proofs;
public class sourceFile{

	/*@ requires true;
	  @ determines \result \by this, l; */
int identity(int /*@ nullable @*/  l, int /*@ nullable @*/  h){
		l = l + h;
		l = l - h;
		return l;
	}



}


