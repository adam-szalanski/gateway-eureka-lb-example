# gateway-eureka-lb-example
A small project used as a spring boot gateway + netflix eureka + load balancing example for SM Java Guild presentation

## Project Setup and Running Instructions

This guide explains how to build and run the project and how to work with the provided Swagger documentation for the APIs.

## Prerequisites

- **Docker:** Ensure Docker is installed and running.
- **Docker Compose:** Ensure Docker Compose is installed.

## Building the Applications

Create the Docker images for each service:

Customer Service
```bash
cd customer-service
docker image build -t customer-service .
```
Warehouse Service
```bash
cd warehouse-service
docker image build -t warehouse-service .
```
Eureka Service
```bash
cd eureka
docker image build -t eureka-service .
```
Gateway Service
```bash
cd gateway
docker image build -t gateway-service .
```

Once all images are built, from the main folder of the repository run:
```bash
docker compose up -d
```

## Swagger Documentation

- Customer Service Swagger file: customer-service/src/main/resources/customer_service-openapi.yaml
- Warehouse Service Swagger file: warehouse-service/src/main/resources/warehouse_service-openapi.yaml

### Using Swagger with Postman

1. Import the YAML files into Postman.
2. Set the base URL in the imported collection to: http://localhost:8080
3. Disable the X-Unique-Id header if it's automatically included in requests.
4. If request returns HTTP 503 (Service Unavailable) wait a few seconds and perform request again.