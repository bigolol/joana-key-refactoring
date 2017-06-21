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
    
    public void putDatumInArr(int h, int[] arr) {
        func();
        arr[4] = h;
    }
}
