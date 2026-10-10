# FISCO-SCF: Blockchain-Enabled Supply Chain Finance Platform

A research-oriented supply-chain finance platform built on FISCO-BCOS, integrating blockchain-based receivables management, financing workflows, enterprise governance, and AI-assisted risk intelligence.

This repository is designed to serve as both a functional software prototype and a research-ready open project for academic review, technical demonstration, collaboration, and scholarly communication.

<p align="center">
  <img src="./images/login.png" alt="Login screen" width="900" />
</p>

## Overview

This project addresses a recurring bottleneck in modern supply chains: the mismatch between financing demand and access to trusted, traceable credit. Small and medium-sized enterprises often struggle to convert receivables into reliable financing because their assets are difficult to verify, assign, transfer, and underwrite in a transparent and defensible way.

The platform demonstrates a solution that combines:

- blockchain-backed receivables tokenization and traceability,
- smart-contract-driven financing lifecycle management,
- enterprise onboarding and credit governance,
- business workflows for split, transfer, and settlement,
- AI-assisted risk assessment and support for decision-making,
- a publication-friendly repository structure for research documentation.

## Why this project matters

This project sits at the intersection of:

- blockchain and distributed ledger technology,
- financial technology and supply chain finance,
- AI-driven risk analysis and decision support,
- interdisciplinary research, engineering, and academic writing.

It is intended to support:

- platform prototyping and system validation,
- reproducible research and technical evaluation,
- academic writing and publication,
- demonstration to faculty, researchers, funders, and collaborators.

## Key capabilities

- Receivables issuance, confirmation, and lifecycle tracking
- On-chain enterprise registration and role-based access
- Financing application, approval, disbursement, and settlement workflows
- Receivables split and transfer processes
- AI-assisted financial risk assessment
- Multi-role dashboard experience for core enterprises, suppliers, and financial institutions
- Research-oriented project structure for future expansion and publication

## Architecture

```text
+---------------------------+
|       Users / Roles       |
| Core Enterprise           |
| Supplier                  |
| Financial Institution     |
| Regulator                 |
+-------------+-------------+
             |
             v
+---------------------------+
| Frontend Layer            |
| Vue 3 + Vite + Element UI|
| Dashboard + Forms         |
+-------------+-------------+
             |
             v
+---------------------------+
| Backend Layer            |
| Spring Boot + REST APIs   |
| JWT Authentication       |
+-------------+-------------+
             |
             v
+---------------------------+
| Blockchain Layer          |
| FISCO-BCOS + Solidity     |
| Smart contracts           |
+-------------+-------------+
             |
             v
+---------------------------+
| AI / Risk Intelligence   |
| Scoring & analytics      |
+---------------------------+
```

## Tech stack

- Frontend: Vue 3, Vite, Element Plus
- Backend: Spring Boot, Spring Security, JWT
- Database: MySQL, JPA
- Blockchain: FISCO-BCOS, Solidity
- Smart contracts: modular access control and receivables logic
- AI: Python-based analytics and scoring workflows
- Build: Maven, Node.js, Java 11

## Repository structure

```text
fisco-scf/
├── README.md
├── LICENSE
├── CITATION.cff
├── CONTRIBUTING.md
├── requirements.txt
├── .gitignore
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
├── frontend/
│   ├── src/
│   ├── package.json
│   └── ...
├── blockchain/
│   └── contracts/
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── screenshots/
│   └── ...
├── research/
├── assets/
├── scripts/
├── examples/
├── images/
│   ├── login.png
│   ├── dashboard.png
│   ├── enterprise-management.png
│   ├── receivables.png
│   └── transfer-records.png
├── .github/
└── ...
```

## Screenshots

### Dashboard overview

<p align="center">
  <img src="./images/dashboard.png" alt="Dashboard overview" width="900" />
</p>

### Enterprise management

<p align="center">
  <img src="./images/enterprise-management.png" alt="Enterprise management" width="900" />
</p>

### Receivables management

<p align="center">
  <img src="./images/receivables.png" alt="Receivables management" width="900" />
</p>

### Transfer records

<p align="center">
  <img src="./images/transfer-records.png" alt="Transfer records" width="900" />
</p>

## Quick start

### Prerequisites

- Java 11+
- Maven
- Node.js 18+
- MySQL 8.0+
- FISCO-BCOS client or local test chain environment

### Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Then open:

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api

## Research and publication readiness

This repository has been organized to support:

- system demonstration for academic audiences,
- discussion of blockchain-based financial infrastructure,
- evidence of business process design and technical implementation,
- future publication materials and research narratives.

The project is explicitly structured for interdisciplinary communication between technical, financial, and academic stakeholders.

## How to cite

If you use this project in research, coursework, or a manuscript, please cite it as follows.

Plain text:

FISCO-SCF: Blockchain-Enabled Supply Chain Finance Platform. GitHub repository, available at: https://github.com/yteicos-ItNa07/fisco-scf

BibTeX:

```bibtex
@misc{fisco_scf_2026,
  title = {FISCO-SCF: Blockchain-Enabled Supply Chain Finance Platform},
  author = {Juliana Tsui},
  year = {2026},
  howpublished = {GitHub Repository},
  url = {https://github.com/yteicos-ItNa07/fisco-scf}
}
```

## License

This project is released under the MIT License, which supports open research, reproducible software development, and unrestricted academic sharing.

## Contributing

Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for contribution guidelines, issue reporting, and pull request expectations.

## Contact

For collaborations, academic inquiries, or research discussions, please contact:

- Email: [juliana.tsui@outlook.com](mailto:juliana.tsui@outlook.com)
- GitHub: [@yteicos-ItNa07](https://github.com/yteicos-ItNa07)

---

This repository is intended to present both the engineering contribution and the research narrative clearly to reviewers, faculty, students, collaborators, and project stakeholders.
