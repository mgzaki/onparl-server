# Onparl - Secure Messaging

A secure chat application using the Signal Protocol for End-to-End Encryption (E2EE).
This project is structured as three independent components.

## Project Structure

### 1. Server (`/server`)
A Spring Boot Application (Java) that acts as the Key Distribution Center (KDC) and Message Relay.
**Note:** The server **does not** perform encryption. It only stores public keys and forwards encrypted payloads.

**Prerequisites:** Java 17+

**Running:**
```bash
cd server
./gradlew bootRun
```
Runs on `http://localhost:8080`.

### 2. Web Client (`/web-client`)
A React + TypeScript application that implements the Signal Protocol (X3DH + Double Ratchet) in the browser.

**Prerequisites:** Node.js 18+

**Setup & Running:**
```bash
cd web-client
npm install
npm run dev
```
Runs on `http://localhost:5173`.

### 3. Infrastructure (`/infra`)
OpenTofu/Terraform configuration for deploying the application to AWS.

**Prerequisites:** OpenTofu or Terraform, AWS CLI, Docker

**Deployment:**
```bash
cd infra
./deploy.sh
```

This automated deployment script will:
- Provision AWS infrastructure (VPC, ECS, ALB, DynamoDB, CloudFront, S3)
- Build and push the server Docker image to ECR
- Deploy the server to ECS Fargate
- Build and deploy the web client to S3/CloudFront

## Architecture
- **E2EE:** Messages are encrypted on the client device using the Signal Protocol before being sent.
- **Protocol:** X3DH for key agreement, Double Ratchet for message encryption.
- **Storage:** Keys and offline messages are stored in DynamoDB.
- **Transport:** WebSocket for real-time delivery (`/ws-signal`).
- **Infrastructure:** Fully containerized backend on AWS ECS, static frontend on CloudFront.

## Technology Stack
- **Backend:** Spring Boot, AWS DynamoDB, WebSocket (SockJS/STOMP)
- **Frontend:** React, TypeScript, Vite
- **Infrastructure:** AWS (ECS Fargate, ALB, CloudFront, S3, DynamoDB), OpenTofu
- **Encryption:** Signal Protocol libraries
