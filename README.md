# ✈️ Flight Reservation — DevOps Deployment Guide

A complete step-by-step guide to deploy the Flight Reservation app using Jenkins, SonarQube, Docker, and AWS EKS.

---

## 📁 Repository Structure

| Repo | Purpose |
|------|---------|
| **Infra Repo** | Creates AWS infrastructure (EC2, RDS, EKS, etc.) |
| **App Repo** | Application code + Jenkins pipeline |

---

## 🗺️ Flow Overview

```
Infra Repo (Terraform)
        ↓
  AWS EC2 Instances (Jenkins + SonarQube)
        ↓
  Jenkins Pipeline (App Repo)
        ↓
  Build → SonarQube Scan → Docker Build → Push → Deploy to EKS
```

---

## PART 1 — Infrastructure Setup

### Step 1 — Clone & Configure Infra Repo

```bash
git clone <infra-repo-url>
cd <repo-name>
```

Configure your AWS provider in Terraform:

**Option A — AWS Profile (Recommended)**
```hcl
provider "aws" {
  region  = "ap-south-1"
  profile = "default"
}
```

**Option B — Access Key / Secret Key**
```hcl
provider "aws" {
  region     = "ap-south-1"
  access_key = "YOUR_ACCESS_KEY"
  secret_key = "YOUR_SECRET_KEY"
}
```

> ✅ Make sure your Terraform creates at least **1 EC2 instance for Jenkins** and **1 for SonarQube**

---

### Step 2 — Deploy Infrastructure

```bash
terraform init
terraform plan
terraform apply
```

This will create:
- EC2 instances (Jenkins + SonarQube)
- Security Groups
- RDS (if configured)

---

## PART 2 — Jenkins Server Setup

### Step 3 — SSH into Jenkins EC2

```bash
ssh ubuntu@<jenkins-ec2-public-ip>
```

---

### Step 4 — Install Jenkins

```bash
sudo apt update -y
sudo apt install openjdk-17-jdk -y

curl -fsSL https://pkg.jenkins.io/debian/jenkins.io-2023.key | sudo tee \
  /usr/share/keyrings/jenkins-keyring.asc > /dev/null

echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian binary/ | sudo tee \
  /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update -y
sudo apt install jenkins -y

sudo systemctl enable jenkins
sudo systemctl start jenkins
```

---

### Step 5 — Install Required Tools

```bash
sudo apt install git -y
sudo apt install maven -y
sudo apt install docker.io -y

sudo usermod -aG docker ubuntu
sudo usermod -aG docker jenkins
sudo systemctl restart docker
```

---

### Step 6 — Open Security Group Ports

| Port | Service |
|------|---------|
| `22` | SSH |
| `8080` | Jenkins UI |
| `9000` | SonarQube UI |

---

### Step 7 — Access & Unlock Jenkins

Open in browser:
```
http://<JENKINS-EC2-IP>:8080
```

Get the initial password:
```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

---

### Step 8 — Install Jenkins Plugins

Go to **Manage Jenkins → Plugins** and install:

- Git Plugin
- Pipeline Plugin
- Maven Integration Plugin
- SonarQube Scanner Plugin
- Docker Pipeline Plugin

---

## PART 3 — SonarQube Setup

### Step 9 — Access SonarQube

Open in browser:
```
http://<SONARQUBE-EC2-IP>:9000
```

Default login:
```
Username: admin
Password: admin
```

> 📄 Full SonarQube installation steps are in: `flight-reservation/jenkinpipeline.md`

---

### Step 10 — Create SonarQube Project & Token

1. Click **Create Project → Local Project**
2. Enter **Project Name** and **Branch Name** → click Next
3. In **Global Settings**, proceed further
4. Click **Generate Token** → copy the token → click Continue
5. Select **Project Type: Maven**
6. Copy the Maven command shown

---

## PART 4 — Application Setup

### Step 11 — Clone App Repo

```bash
git clone <flight-repo-url>
cd <repo>
```

---

### Step 12 — Update Application Properties

Edit the file:
```
flight-reservation/src/main/resources/application.properties
```

Update:
```properties
spring.datasource.url=<YOUR_RDS_URL>
spring.datasource.username=<DB_USERNAME>
spring.datasource.password=<DB_PASSWORD>
server.port=<PORT>
```

> 📄 RDS details are in: `infra-repo/modules/rds/main.tf`

---

### Step 13 — Update Docker Image in Pipeline

Open the pipeline file:
```
flight-reservation/backend/jenkinpipeline.groovy
```

Replace the placeholder:
```groovy
// Before
docker username/repo:image

// After
<your-docker-username>/<your-repo>:<tag>
```

---

### Step 14 — Push Code to GitHub

```bash
git add .
git commit -m "updated configs"
git push origin main
```

---

## PART 5 — Jenkins Pipeline

### Step 15 — Create Jenkins Pipeline Job

1. Go to **Jenkins Dashboard**
2. Click **New Item**
3. Select **Pipeline**
4. Name it: `flight-backend-pipeline`

Configure:
| Field | Value |
|-------|-------|
| Definition | Pipeline script from SCM |
| SCM | Git |
| Repository URL | `<github-repo-url>` |
| Branch | `main` |
| Script Path | `flight-reservation/backend/jenkinpipeline.groovy` |

---

## PART 6 — Jenkins User & Cluster Configuration

### Step 16 — Docker Login as Jenkins User

```bash
su - jenkins
docker login
chown -R jenkins:jenkins ~/.docker/
```

---

### Step 17 — Install kubectl

Follow the [official kubectl installation guide](https://kubernetes.io/docs/tasks/tools/) for your OS.

---

### Step 18 — Install & Configure AWS CLI

```bash
aws configure
```

Enter your AWS Access Key, Secret Key, Region, and output format.

---

### Step 19 — Connect to EKS Cluster

```bash
aws eks update-kubeconfig --name cbz-cluster --region eu-north-1
```

> ⚠️ Replace region with yours (e.g., `ap-osaka-1`) if different

---

### Step 20 — Copy AWS & Kube Config to Jenkins

```bash
cp -rf ~/.aws /var/lib/jenkins
cp -rf ~/.kube /var/lib/jenkins

chown -R jenkins:jenkins /var/lib/jenkins/.aws
chown -R jenkins:jenkins /var/lib/jenkins/.kube
```

---

### Step 21 — Restart Jenkins

```bash
systemctl restart jenkins
```

---

### Step 22 — Verify Cluster Access

```bash
kubectl get nodes
```

---

## PART 7 — Deploy

### Step 23 — Update Kubernetes Deployment

Edit:
```
k8s/deployment.yaml
```

Update the Docker image tag to the latest pushed image.

---

### Step 24 — Run Jenkins Pipeline

1. Go to Jenkins Dashboard
2. Open `flight-backend-pipeline`
3. Click **Build Now**

---

### Step 25 — Handle Namespace Error (If Any)

> ℹ️ If you see a namespace error, **don't worry** — it just needs time to create.

1. Wait a moment
2. Go to the pipeline
3. Click **Restart from Stage → Select "Deploy"**
4. Run again

---

## ✅ Done!

Your Flight Reservation app is now deployed on AWS EKS via Jenkins CI/CD. 🎉

---

## 📌 Quick Reference

| Component | URL / Path |
|-----------|-----------|
| Jenkins | `http://<jenkins-ip>:8080` |
| SonarQube | `http://<sonarqube-ip>:9000` |
| Pipeline file | `flight-reservation/backend/jenkinpipeline.groovy` |
| App properties | `flight-reservation/src/main/resources/application.properties` |
| K8s deployment | `k8s/deployment.yaml` |
| SonarQube docs | `flight-reservation/jenkinpipeline.md` |
| RDS config | `infra-repo/modules/rds/main.tf` |
