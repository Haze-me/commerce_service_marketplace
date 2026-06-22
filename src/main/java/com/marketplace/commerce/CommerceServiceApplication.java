package com.marketplace.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Commerce Service — customer auth, cart, orders, checkout,
 * Paystack payments, reviews, notifications.
 * Owns commerce_db exclusively.
 */

@SpringBootApplication
public class CommerceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommerceServiceApplication.class, args);
	}

}
