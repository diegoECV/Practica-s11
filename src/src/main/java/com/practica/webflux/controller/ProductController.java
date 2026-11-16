package com.practica.webflux.controller;

import com.practica.webflux.model.Product;
import com.practica.webflux.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;

    @Value("${DB_HOST:mongo-service}")
    private String dbHost;

    @Value("${DB_PORT:27017}")
    private String dbPort;

    @Value("${DB_NAME:proddb}")
    private String dbName;

    @Value("${APP_PORT:8080}")
    private String appPort;

    @Value("${MONGO_INITDB_ROOT_USERNAME:admin}")
    private String dbUser;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Ruta raíz de diagnóstico y bienvenida para pruebas directas en navegador
    @GetMapping("/")
    public Mono<Map<String, Object>> getRootStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Spring WebFlux Reactive Backend");
        status.put("session", "S11 - Persistencia y Comunicacion Multi-componente");
        status.put("dbHost", dbHost);
        status.put("dbPort", dbPort);
        status.put("dbName", dbName);
        status.put("appPort", appPort);
        status.put("dbUser", dbUser);
        status.put("endpoints", Map.of(
                "health", "/actuator/health",
                "getAllProducts", "GET /api/products",
                "createProduct", "POST /api/products",
                "seedDefaultData", "POST /api/products/seed"
        ));
        return Mono.just(status);
    }

    // Endpoint reactivo para listar todos los productos (Flux no bloqueante)
    @GetMapping("/api/products")
    public Flux<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Endpoint reactivo para obtener un producto por ID
    @GetMapping("/api/products/{id}")
    public Mono<ResponseEntity<Product>> getProductById(@PathVariable String id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Endpoint reactivo para crear un nuevo producto en MongoDB
    @PostMapping("/api/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // Endpoint de conveniencia para poblar datos de prueba rápidamente
    @PostMapping("/api/products/seed")
    @ResponseStatus(HttpStatus.CREATED)
    public Flux<Product> seedData() {
        Flux<Product> sampleProducts = Flux.just(
                new Product(null, "Servidor Cloud AWS t3.medium", "Computo", 0.0416, 100),
                new Product(null, "Almacenamiento EBS gp3 50GB", "Storage", 4.00, 50),
                new Product(null, "Balanceador AWS ALB", "Networking", 16.20, 20)
        );
        return productRepository.deleteAll()
                .thenMany(sampleProducts.flatMap(productRepository::save));
    }

    // Endpoint para eliminar un producto
    @DeleteMapping("/api/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(@PathVariable String id) {
        return productRepository.deleteById(id);
    }
}

