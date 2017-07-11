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
    
	/*@ requires true;
	  @ determines \result \by cb, this; */
public int falsePos(int/*@ nullable @*/ high) {
        int arr[] = new int[5];
        arr[0] = 1;
        cb.putDatumInArr(high, arr);
        return arr[0];
    }
}
