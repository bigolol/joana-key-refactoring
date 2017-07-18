package multipleclassesfalsepos;
public class ClassA{
ClassB b = new ClassB();
	/*@ requires true;
	  @ determines \result \by b, this; */
public int falsePos(int/*@ nullable @*/ high) {
     int arr[] = new int[5];
        arr[0] = 1;
        return b.putDataInArr(high, arr)[0];
    }

}
