package se.sics.mspsim.util;

import java.util.Arrays;

public class ResetsMemory {

    private int[] resetsArray;
    private int head;
    private int tail;
    private int size;
    private int count;
    private int sum;
    private int average;

    public ResetsMemory(int size){
        this.resetsArray = new int[size];
        this.size = size;
        this.count = 0;
        this.head = 0;
        this.tail = 0;
        this.sum = 0;
        this.average = 0;
    }

    public int getSize(){
        return this.size;
    }

    public int getCount(){
        return this.count;
    }

    public int getNth(int index) throws ArrayIndexOutOfBoundsException{
        if(index>=count)
            throw new ArrayIndexOutOfBoundsException();
        return resetsArray[(tail+index)%size];
    }
    
    public int getNewest() {
    	if(count==0)
    		return 0;
    	int index = (head-1) < 0 ? size + (head-1) : (head-1) % size;
		return resetsArray[index];
    }

    public void put(int resetTime){
        resetsArray[head] = resetTime;
        if(count<size) {
        	count++;
        } else {
        	tail = (tail+1)%size;
        }
        head = (head+1)%size;
        sum+=resetTime;
        average = sum/(count);
    }

    public int getAverage(){
        return average;
    }

    @Override
    public String toString(){
        int[] retArray = new int[count];
        for(int i=0; i<count; i++)
            retArray[i] = getNth(i);
        return Arrays.toString(retArray);
    }

}
