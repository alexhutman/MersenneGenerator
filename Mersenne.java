/* Mersenne.java
/  By Alexander Hutman
/  Finds the Mersenne Primes from 1 to 2^p - 1,
/     where p is input from the user (default is 10,000)
/  Usage: java Mersenne <p> */

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.Random;

class PrimeCount extends Thread {
  List<Integer> section;      // Initialize the List of primes this thread will be checking to see if 2^[those primes] - 1 are Mersenne Primes
  ArrayList<Integer> threadPrimes = new ArrayList<Integer>();     // Initialize ArrayList of verified Mersenne Primes for this thread

  PrimeCount(List<Integer> section_) {
    section = section_;     // Store passed-in List to one the thread can use
  }

  public void run() {
    for (int p=0; p<section.size(); p++) {      // Iterate through each prime in the passed in List
      int curPrime = section.get(p);
      if (isMersennePrime(curPrime))  threadPrimes.add(curPrime);     // If 2^[said prime] - 1 is a Mersenne Prime, add it to threadPrimes
    }
    System.out.println("~"+ (Mersenne.totPercentage.doubleValue()+Mersenne.indivPercentage) + "% finished.");     // When thread is finished, update percentage done then display it
    Mersenne.totPercentage.add(Mersenne.indivPercentage);
  }

  public static boolean isMersennePrime(int prime) {      //I didn't write this method! Taken from https://rosettacode.org/wiki/Lucas-Lehmer_test#Java
    BigInteger m_p = BigInteger.ONE.shiftLeft(prime).subtract(BigInteger.ONE);
    BigInteger s = BigInteger.valueOf(4);
    for (int i=3; i <= prime; i++) {
      s = s.multiply(s).subtract(BigInteger.valueOf(2)).mod(m_p);
    }
    return s.equals(BigInteger.ZERO);
  }
}

public class Mersenne {
  static int numThreads = 4*Runtime.getRuntime().availableProcessors();     // Number of threads to run the program on. Is 4 times the number of cores on your machine
  static DoubleAdder totPercentage = new DoubleAdder();     // Atomic doubleAdder to keep track of the current percentage done
  static double indivPercentage = 100/(double)numThreads;     // Percentage each thread should contribute when finished

  public static void main(String[] args) {
    int upTo = 10000;     // Default # of primes to go up to. Can be changed with specifying an int for arg[0]
    int divSize;      // Amount of chunks to break primeList into
    ArrayList<Integer> mersennePrimes = new ArrayList<Integer>();     // Initialize ArrayList for final numbers to return
    ArrayList<Integer> primeList = new ArrayList<Integer>();      // Initialize ArrayList of prime numbers from 3 to upTo
    PrimeCount[] threads;     // Initialize array of threads to be utilized
    long startTime;     // Store time when program starts so that the elapsed time can be calculated
    long endTime;     // Store time when program ends so that the elapsed time can be calculated


    if (args.length == 0) {
      System.out.println("No argument for p was detected. Using p = 10,000. \nUsage: java Mersenne <p> \t to find the Mersenne Primes from 1 to 2^p - 1");      // Use default upTo = 10,000 if arg[0] isn't entered
    }

    else {
      try {
        upTo = Integer.parseInt(args[0]);     // Otherwise, use arg[0]
        if (upTo < 2) {
          System.out.println("Please enter a number, p, greater than 1 to find the Mersenne Primes from 1 to 2^p - 1");     // ...but if it's less than 2 then get out of here
          System.exit(0);
        }
        else {
          System.out.println("Calculating Mersenne Primes (2^p - 1) up to p = " + upTo + ":");      // If you play nice, then upTo = arg[0]
        }
      }
      catch (NumberFormatException e){
        System.out.println("Please enter a number, p, greater than 1 to find the Mersenne Primes from 1 to 2^p - 1");     // If you enter $#!% or something as arg[0], get out of here
        System.exit(0);
      }
    }

    for (int i=3; i<=upTo; i+=2) {      // Calculate all of the primes from 3 to upTo, store them in primeList
      if (isPrime(i)) {
        primeList.add(i);
      }
    }

    Collections.shuffle(primeList, new Random());     // Pseudorandomly shuffles the ArrayList of primes. This will, on paper, always be faster than keeping it sorted because doing so would
                                                      // mean that one single thread would be responsible for the last divSize number of threads, which would be the largest bottleneck possible
    System.out.println("Using "+numThreads+" threads");

    divSize = primeList.size()/numThreads;
    threads = new PrimeCount[numThreads];
    startTime = System.currentTimeMillis();

    for (int i = 0; i<numThreads; i++) {      // Start all of the threads
      List<Integer> tempSubList = primeList.subList(i*divSize, (i==numThreads-1) ? primeList.size() : (i+1)*divSize);     // Pass chunk of primeList of size divSize to each thread and start each one
      threads[i] = new PrimeCount(tempSubList);
      threads[i].start();
    }
    for (int i = 0; i<numThreads; i++) {      // Join all of the threads
      try {
        threads[i].join();
      }
      catch (InterruptedException e){
        System.out.println(e);
      }
    }

    endTime = System.currentTimeMillis();

    System.out.println("\n It took " + (endTime-startTime)/1000.0 + " seconds to calculate the Mersenne Primes up to 2^" + upTo + " - 1. Printing them in 2.5 seconds: \n");

    try {
      Thread.sleep(2500);
    }
    catch (InterruptedException e){
      System.out.println(e);
    }

    for (int i = 0; i<threads.length; i++) {
      mersennePrimes.addAll(threads[i].threadPrimes);     // Concatenate every thread's ArrayList of Mersenne Primes to a single ArrayList
    }
    Collections.sort(mersennePrimes);     // Sort the ArrayList so that the Mersenne Primes can be displayed in ascending order

    System.out.println("2^2 - 1");      // Hardcoded 2^2  - 1 because 2 is (obviously) the only even prime and a lot of optimization was gained by assuming that every value to check was odd
    for(int p=0; p< mersennePrimes.size(); p++){
      System.out.println("2^"+mersennePrimes.get(p)+" - 1");
    }
  }
  private static boolean isPrime(int p) {     // Primality test taken from https://rosettacode.org/wiki/Lucas-Lehmer_test#Java
    if (p == 2)
    return true;
    else if (p <= 1 || p % 2 == 0)
    return false;
    else {
      int to = (int)Math.sqrt(p);
      for (int i = 3; i <= to; i += 2)
      if (p % i == 0)
      return false;
      return true;
    }
  }
}
