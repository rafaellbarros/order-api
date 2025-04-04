# **Order API**

## **Tecnologias Utilizadas**
- **Java 21**: Linguagem de programação principal.
- **Spring Boot 3.4.4**: Framework para construção de aplicações robustas.
- **MongoDB**: Banco de dados não relacional.
- **SpringDoc OpenAPI**: Documentação da API REST.
- **Docker & Docker Compose**: Containerização de serviços.

---

## **Como Executar a Aplicação**

### **Pré-requisitos**
- **JDK 21** instalado para desenvolvimento local.
- **Docker & Docker Compose** instalados.

### **1️⃣ Ambiente Local (Desenvolvimento)**

#### **Subindo os serviços auxiliares**
```sh
  docker-compose up -d
```

A aplicação precisa ser iniciada **manualmente** na IDE ou pelo JAR, enquanto os serviços auxiliares (Banco, etc.) são iniciados via Docker Compose.

#### **Rodando a aplicação**
1. **Na IDE**: Rode a classe principal `OrderApiApplication.java`.
2. **Via terminal (caso já tenha o JAR gerado)**:
```bash
  java -jar build/libs/order-api.jar
```

#### **Acessando a aplicação**
- API: [http://localhost:8082/order-api/swagger-ui/index.html](http://localhost:8082/order-api/swagger-ui/index.html)
