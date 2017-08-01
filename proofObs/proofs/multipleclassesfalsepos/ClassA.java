package multipleclassesfalsepos;
public class ClassA{
ClassB b = new ClassB();
ClassB c = new ClassB();
	/*@ requires true;
	  @ determines \result \by this, this.b, this.b.arr, this.c.arr, this.c; */
public int falsePos(int/*@ nullable @*/ high) {
     b.arr = new int[5];
        b.arr[0] = 1;
        c.arr = new int[3];
        c.arr[0] = 2;
        b.arr[1] = c.arr[0];
        return b.putDataInArr(high)[0];
    }

}
