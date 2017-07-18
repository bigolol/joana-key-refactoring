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

    ClassB b = new ClassB();

    ClassA() {
    }

    public int falsePos(int high) {
        int arr[] = new int[5];
        arr[0] = 1;
        return b.putDataInArr(high, arr)[0];
    }
}
