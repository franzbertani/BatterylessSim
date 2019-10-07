package it.polimi.neslab.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Scanner;

import se.sics.mspsim.util.Tuple;

public class SimpleFairy {
    private ArrayList<Tuple> traceValues;
    private ListIterator<Tuple> iter;
    private Tuple current;
    private Tuple next;
    private double maxTime;

    public SimpleFairy(String tracePath){
        this.traceValues = new ArrayList<Tuple>();
        String [] tupleValues = new String[2];
        double offset = 0;
        boolean firstLine = true;
        
        Scanner lineScanner = null;
        try {
            lineScanner = new Scanner(new File(tracePath));
        } catch (FileNotFoundException e) { 
            e.printStackTrace();
        }
        lineScanner.useDelimiter(System.getProperty("line.separator"));
        while (lineScanner.hasNext()){
            tupleValues = lineScanner.next().split("\t");
            if(firstLine) {
                offset = Double.parseDouble(tupleValues[0]);
                firstLine = false;
            }
            traceValues.add(new Tuple((Double.parseDouble(tupleValues[0])-offset),(Double.parseDouble(tupleValues[1]))));
        }
        iter = traceValues.listIterator();
        current = iter.next();
        next = iter.next();
        this.maxTime = traceValues.get(traceValues.size() - 1).getX();
    }

    public double peekNextVoltage() {
        return next.getY();
    }
    
    public double peekVoltage() {
    	return current.getY();
    }
    
    public boolean hasEnded() {
    	return iter.hasNext();
    }

    public void stepTrace() throws NoSuchElementException {
        current = next;
        if(iter.hasNext())
            next = iter.next();
        else
        	throw new NoSuchElementException();
        	
    }

    public void fastForwardTo(double time){
        time %= maxTime;
        iter = traceValues.listIterator();
        current = iter.next();
        while (current.getX() < time){
            current = iter.next();
        }
        current = iter.previous();
        next = iter.next();
    }

    public double getMaxTime() {
        return maxTime;
    }

}
