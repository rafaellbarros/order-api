# **Order API**

## ✅ Tecnologias Utilizadas
- **Java 21** – Linguagem principal  
- **Spring Boot 3.4.4** – Framework principal  
- **MongoDB** – Banco de dados NoSQL  
- **SpringDoc OpenAPI** – Documentação da API  
- **Docker & Docker Compose** – Containerização de serviços  

---

## 🚀 Como Executar a Aplicação

### 📌 Pré-requisitos
- JDK 21
- Docker & Docker Compose

---

### 🔧 Ambiente Local (Desenvolvimento)

#### 1. Subir os serviços auxiliares
```bash
docker-compose up -d
```

#### 2. Rodar a aplicação

- **Na IDE**: Rode a classe `OrderApiApplication.java`
- **Via terminal** (com JAR gerado):
```bash
java -jar build/libs/order-api.jar
```

#### 3. Acessar a documentação da API
- Swagger UI: [http://localhost:8082/order-api/swagger-ui/index.html](http://localhost:8082/order-api/swagger-ui/index.html)

---

## 🔄 Integrações Externas A – Criação de Pedidos

### ✅ Criar Pedido Único

#### 📄 JSON
```json
{
  "externalId": "ext-123406",
  "items": [
    {
      "name": "Item 1",
      "price": 100.00,
      "quantity": 2
    },
    {
      "name": "Item 2",
      "price": 250.00,
      "quantity": 1
    }
  ]
}
```

#### 💻 cURL
```bash
curl -X POST \
  'http://localhost:8082/order-api/orders/receive' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{
    "externalId": "ext-123406",
    "items": [
      { "name": "Item 1", "price": 100.00, "quantity": 2 },
      { "name": "Item 2", "price": 250.00, "quantity": 1 }
    ]
  }'
```

---

### ✅ Criar Vários Pedidos (Lote)

#### 📄 JSON
```json
[
  {
    "externalId": "ext-123407",
    "items": [
      {
        "name": "Item 1",
        "price": 100.00,
        "quantity": 2
      },
      {
        "name": "Item 2",
        "price": 250.00,
        "quantity": 1
      }
    ]
  },
  {
    "externalId": "ext-123408",
    "items": [
      {
        "name": "Item 3",
        "price": 60.25,
        "quantity": 2
      },
      {
        "name": "Item 4",
        "price": 200.00,
        "quantity": 3
      }
    ]
  }
]
```

#### 💻 cURL
```bash
curl -X POST \
  'http://localhost:8082/order-api/orders/receive/all' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "externalId": "ext-123407",
      "items": [
        { "name": "Item 1", "price": 100.00, "quantity": 2 },
        { "name": "Item 2", "price": 250.00, "quantity": 1 }
      ]
    },
    {
      "externalId": "ext-123408",
      "items": [
        { "name": "Item 3", "price": 60.25, "quantity": 2 },
        { "name": "Item 4", "price": 200.00, "quantity": 3 }
      ]
    }
  ]'
```

---

## 🔎 Integrações Externas B – Consulta de Pedidos

### 🔍 Consultar pedido por `externalId`

#### 💻 cURL
```bash
curl -X GET \
  'http://localhost:8082/order-api/orders/externalId/ext-123406' \
  -H 'accept: */*'
```

---

### 🔍 Consultar pedidos por `status`

#### 💻 cURL
```bash
curl -X GET \
  'http://localhost:8082/order-api/orders/status/CALCULATED' \
  -H 'accept: */*'
```
