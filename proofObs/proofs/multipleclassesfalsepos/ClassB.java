/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multipleclassesfalsepos;

/**
 *
 * @author holger
 */
public class ClassB {

	/*@ requires this != arr;
	  @ determines this \by this, arr; */
putDataInArr(int/*@ nullable @*/ high, int[]/*@ nullable @*/ arr) {
        arr[4] = high;
        return arr;
    }
    
}
