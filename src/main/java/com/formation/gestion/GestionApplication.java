package com.formation.gestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree. Chaque TP est un CommandLineRunner active par un profil :
 * mvn spring-boot:run -Dspring-boot.run.profiles=tp1
 */
@SpringBootApplication
public class GestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionApplication.class, args);
    }
}
