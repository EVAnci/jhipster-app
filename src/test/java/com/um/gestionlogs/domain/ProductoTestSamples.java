package com.um.gestionlogs.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ProductoTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Producto getProductoSample1() {
        return new Producto().id(1L).nombre("nombre1");
    }

    public static Producto getProductoSample2() {
        return new Producto().id(2L).nombre("nombre2");
    }

    public static Producto getProductoRandomSampleGenerator() {
        return new Producto().id(longCount.incrementAndGet()).nombre(UUID.randomUUID().toString());
    }
}
