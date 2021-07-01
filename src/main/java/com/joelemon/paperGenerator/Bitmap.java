package com.joelemon.paperGenerator;

/** 使用bitmap节省内存开销
 *   暂时使用普通 boolean[] 代替
 * @author HJY
 * @date 2020/2/19
 */
public class Bitmap {

    private boolean[] bitData;

    private int total = -1;

    public Bitmap(int byteLength) {
        this.bitData = new boolean[byteLength];
    }

    public void set(int position) {
        bitData[position] = true;
    }

    public int get(int position) {
        if (position >= bitData.length) {
            throw new IndexOutOfBoundsException();
        }
        return bitData[position] ? 1 : 0;
    }

    public boolean isValid(int position) {
        if (position >= bitData.length) {
            throw new IndexOutOfBoundsException();
        }
        return bitData[position];
    }

    public int getTotal(){
        if (total == -1) {
            this.total = 0;
            for (int i = 0; i < bitData.length; i++) {
                if (bitData[i]) {
                    total++;
                }
            }
        }
        return total;
    }
}
