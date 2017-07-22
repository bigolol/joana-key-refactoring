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
    public int[] arr;   
    ClassB() {}
    
    int[] putDataInArr(int high) {
        arr[4] = high;
        return arr;
    }
    
}
