package multipleclassesfalsepos;
public class ClassB{
public int[] arr;
	/*@ requires this != high;
	  @ determines this \by this, this.arr; */
int[] putDataInArr(int/*@ nullable @*/ high) {
     arr[4] = high;
        return arr;
    }

}
