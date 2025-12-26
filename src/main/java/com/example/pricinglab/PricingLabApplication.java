package com.example.pricinglab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Pricing Intelligence Lab application.
 *
 * This is an internal simulation tool for store-level pricing experiments.
 * It does NOT perform any live pricing changes or customer-level pricing.
 */
@SpringBootApplication
public class PricingLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingLabApplication.class, args);
    }
}
