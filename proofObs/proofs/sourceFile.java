package proofs;
public class sourceFile{
ClassB /*@ nullable @*/ cb = new ClassB(); 

	/*@ requires this != arr;
	  @ determines this \by this, arr; */
void putDatumInArr(int /*@ nullable @*/  h, int[] /*@ nullable @*/  arr){
        func();
        arr[4] = h;
    }



}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multipleclassesfalsepos;

/**
 *
 * @author holgerklein
 */
public class ClassA {
    private ClassB cb = new ClassB();
    
    public int falsePos(int high) {
        int arr[] = new int[5];
        arr[0] = 1;
        cb.putDatumInArr(high, arr);
        return arr[0];
    }
}


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multipleclassesfalsepos;

/**
 *
 * @author holgerklein
 */
public class MultipleClassesFalsePos {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("a low val is " + new ClassA().falsePos(4));
    }
    
}


