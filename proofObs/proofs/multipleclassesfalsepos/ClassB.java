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
public class ClassB {
    private void func(){}
    
	/*@ requires this != arr;
	  @ determines this \by this, arr; */
public putDatumInArr(int/*@ nullable @*/ h, int[]/*@ nullable @*/ arr) {
        func();
        arr[4] = h;
    }
}
