/*
 * Copyright (c) 2022 Leon Linhart
 * All rights reserved.
 */
package com.example.producer.alpha;

public class UnmarkedClass {

    @AlphaMarker
    public class MarkedInnerClass {

    }

    @AlphaMarker
    public static class MarkedNestedClass {

    }

}