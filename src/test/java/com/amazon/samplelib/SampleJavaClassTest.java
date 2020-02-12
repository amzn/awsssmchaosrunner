package com.amazon.samplelib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 *  SampleJavaClassTest.
 */
public class SampleJavaClassTest {
    @Test
    public void sampleMethodTest() {
        SampleJavaClass sampleClass = new SampleJavaClass();
        assertEquals(sampleClass.sampleMethod(), "sampleMethod() called!");
    }
}
