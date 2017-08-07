package plusminus;

public class PlusMinusFalsePos {
	
	
	
	public int testMethod(int high, int low) {
		
		if(low == 5){
			low = identity(low,high);
			
		}
		else{
			low = identity2(low,high);
		}		
		return low;
	}
    /*@ determines \result \by l;
    @*/
	/*@ requires true;
	  @ determines \result \by this, l; */
public int identity(int/*@ nullable @*/ l, int/*@ nullable @*/ h) {
		l = l + h;
		l = l - h;
		return l;
	}
	
	public int identity2(int l, int h) {	
		return l;
	}
}
