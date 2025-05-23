# gateway-eureka-lb-example
A small project used as a spring boot gateway + netflix eureka + load balancing example for SM Java Guild presentation

## Project Setup and Running Instructions

This guide explains how to build and run the project and how to work with the provided Swagger documentation for the APIs.

## Prerequisites

- **Docker:** Ensure Docker is installed and running.

## Building the Applications

Run below command from the main folder of the repository:
```bash
docker compose up -d
```

## Swagger Documentation

- Customer Service Swagger file: customer-service/src/main/resources/customer_service-openapi.yaml
- Warehouse Service Swagger file: warehouse-service/src/main/resources/warehouse_service-openapi.yaml

### Using Swagger with Postman

1. Import the YAML files into Postman.
2. Set the base URL in the imported collection to: http://localhost:8080
3. Disable the X-Unique-Id header if you want to use X-Unique-Id autogeneration through gateway filter.
4. If request returns HTTP 503 (Service Unavailable) wait a few seconds and perform request again. (Eureka takes 30 seconds to assure that registered services are running correctly)
