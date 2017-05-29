package proofs;
public class sourceFile{

	/*@ requires \nothing;
	  @ determines \result \by \nothing; */
int identity(int /*@nullable*/  l, int /*@nullable*/  h){
		l = l + h;
		l = l - h;
		return l;
	}



}


