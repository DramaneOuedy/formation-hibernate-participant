package com.formation.gestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifie que l'application demarre (contexte Spring complet, base H2 en memoire). */
@SpringBootTest
class ContextLoadsTest {

    @Test
    void contextLoads() {
        // rien a verifier : le test echoue si le contexte Spring ne demarre pas
    }
}
