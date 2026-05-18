
<h1>⚙️ HireConnect Backend</h1>
<h2>Backend for HireConnect – Job Portal with Recruiter & Candidate Dashboard</h2>
<h3>This backend is built using Microservices Architecture with Spring Boot, enabling scalability, modularity, and independent deployment of services.</h3>

<h1>🚀 Overview</h1>
<p>HireConnect backend powers a complete hiring ecosystem where:
Candidates can search and apply for jobs
Recruiters can post jobs and manage applicants
System handles interviews, notifications, analytics, and subscriptions
The system is divided into independent microservices, each responsible for a specific domain.
</p>

<h1>🏗️ Architecture</h1>
<ul>
<li>Architecture Style: Microservices</li>
<li>Communication:</li>
<li>REST (synchronous)</li>
<li>Authentication: JWT + OAuth2 (GitHub)</li>
<li>API Gateway</li>
<li>Service-to-service communication: RestTemplate / FeignClient</li>
</ul>

<h1>🧑‍💻 Tech Stack</h1>
<ol>
<li>Backend Framework: Spring Boot</li>
<li>Security: Spring Security + JWT</li>
<li>Database: MySQL</li>
<li>Search: Elasticsearch</li>
<li>Build Tool: Maven</li>
</ol>

<h1>📁 Backend Services</h1>
hireconnect-backend/
│
├── auth-service
├── profile-service
├── job-service
├── application-service
├── interview-service
├── notification-service
├── subscription-service
├── analytics-service
└── api-gateway

<h1>🔐 Authentication Flow</h1>
<ol>
<li>User logs in via /auth/login</li>
<li>JWT token is generated</li>
<li>Token is passed in headers for secured APIs</li>
<li>Services validate token before processing requests</li>
</ol>

<h1>🔧 Microservices</h1>
<h2>🔑 1. Auth Service</h2>
📌 Responsibilities: User Registration & Login, JWT Token Generation & Validation, OAuth2 Login (GitHub)
📍 Base URL:/auth
🔗 Key Endpoints:
POST /register
POST /login
POST /logout
GET /validate

<h2>👤 2. Profile Service</h2>
📌 Responsibilities: Manage Candidate & Recruiter Profiles, Store personal & company details, Resume handling
📍 Base URL: /profiles
🔗 Key Endpoints:
POST /candidate
POST /recruiter
GET /{id}
PUT /update
DELETE /delete

<h2>💼 3. Job Service</h2>
📌 Responsibilities: Job posting & management,Job search & filtering
📍 Base URL: /jobs
🔗 Key Endpoints:
POST /add
GET /all
GET /{id}
GET /search
PUT /update
DELETE /delete

<h2>📄 4. Application Service</h2>
📌 Responsibilities:Job application submission, Track application status, Manage hiring pipeline
📍 Base URL: /applications
🔗 Key Endpoints:
POST /apply
GET /candidate/{id}
GET /job/{id}
PUT /status
DELETE /withdraw

<h2>📅 5. Interview Service</h2>
📌 Responsibilities: Schedule interviews,Manage interview lifecycle
📍 Base URL:/interviews
🔗 Key Endpoints:
POST /schedule
PUT /confirm
PUT /reschedule
DELETE /cancel

<h2>🔔 6. Notification Service</h2>
📌 Responsibilities:Send in-app notifications, Email notifications (SMTP),Track read/unread status
📍 Base URL:/notifications
🔗 Key Endpoints:
GET /user/{id}
PUT /mark-read
PUT /mark-all-read
DELETE /delete

<h2>💳 7. Subscription Service</h2>
📌 Responsibilities:Manage recruiter subscription plans, Payment handling, Invoice generation
📍 Base URL:/subscriptions
🔗 Key Endpoints:
POST /subscribe
PUT /cancel
PUT /renew
GET /invoices

<h2>📊 8. Analytics Service</h2>
📌 Responsibilities:Job analytics, Platform insights, Hiring metrics
📍 Base URL:/analytics
🔗 Key Endpoints:
GET /recruiter/{id}
GET /admin

🔄 Inter-Service Communication:REST APIs for synchronous calls
RabbitMQ for:Notifications,Email triggers,Event-driven updates

<h2>📈 Non-Functional Features</h2>
<ul>
<li>Scalable microservices</li>
<li>High availability</li>
<li>Secure APIs with JWT</li>
<li>Fast job search (Elasticsearch)</li>
</ul>
=======
<h1>⚙️ HireConnect Backend</h1>
<h2>Backend for HireConnect – Job Portal with Recruiter & Candidate Dashboard</h2>
<h3>This backend is built using Microservices Architecture with Spring Boot, enabling scalability, modularity, and independent deployment of services.</h3>

<h1>🚀 Overview</h1>
<p>HireConnect backend powers a complete hiring ecosystem where:
Candidates can search and apply for jobs
Recruiters can post jobs and manage applicants
System handles interviews, notifications, analytics, and subscriptions
The system is divided into independent microservices, each responsible for a specific domain.
</p>

<h1>🏗️ Architecture</h1>
<ul>
<li>Architecture Style: Microservices</li>
<li>Communication:</li>
<li>REST (synchronous)</li>
<li>Authentication: JWT + OAuth2 (GitHub)</li>
<li>API Gateway</li>
<li>Service-to-service communication: RestTemplate / FeignClient</li>
</ul>

<h1>🧑‍💻 Tech Stack</h1>
<ol>
<li>Backend Framework: Spring Boot</li>
<li>Security: Spring Security + JWT</li>
<li>Database: MySQL</li>
<li>Search: Elasticsearch</li>
<li>Build Tool: Maven</li>
</ol>

<h1>📁 Backend Services</h1>
hireconnect-backend/
│
├── auth-service
├── profile-service
├── job-service
├── application-service
├── interview-service
├── notification-service
├── subscription-service
├── analytics-service
└── api-gateway

<h1>🔐 Authentication Flow</h1>
<ol>
<li>User logs in via /auth/login</li>
<li>JWT token is generated</li>
<li>Token is passed in headers for secured APIs</li>
<li>Services validate token before processing requests</li>
</ol>

<h1>🔧 Microservices</h1>
<h2>🔑 1. Auth Service</h2>
📌 Responsibilities: User Registration & Login, JWT Token Generation & Validation, OAuth2 Login (GitHub)
📍 Base URL:/auth
🔗 Key Endpoints:
POST /register
POST /login
POST /logout
GET /validate

<h2>👤 2. Profile Service</h2>
📌 Responsibilities: Manage Candidate & Recruiter Profiles, Store personal & company details, Resume handling
📍 Base URL: /profiles
🔗 Key Endpoints:
POST /candidate
POST /recruiter
GET /{id}
PUT /update
DELETE /delete

<h2>💼 3. Job Service</h2>
📌 Responsibilities: Job posting & management,Job search & filtering
📍 Base URL: /jobs
🔗 Key Endpoints:
POST /add
GET /all
GET /{id}
GET /search
PUT /update
DELETE /delete

<h2>📄 4. Application Service</h2>
📌 Responsibilities:Job application submission, Track application status, Manage hiring pipeline
📍 Base URL: /applications
🔗 Key Endpoints:
POST /apply
GET /candidate/{id}
GET /job/{id}
PUT /status
DELETE /withdraw

<h2>📅 5. Interview Service</h2>
📌 Responsibilities: Schedule interviews,Manage interview lifecycle
📍 Base URL:/interviews
🔗 Key Endpoints:
POST /schedule
PUT /confirm
PUT /reschedule
DELETE /cancel

<h2>🔔 6. Notification Service</h2>
📌 Responsibilities:Send in-app notifications, Email notifications (SMTP),Track read/unread status
📍 Base URL:/notifications
🔗 Key Endpoints:
GET /user/{id}
PUT /mark-read
PUT /mark-all-read
DELETE /delete

<h2>💳 7. Subscription Service</h2>
📌 Responsibilities:Manage recruiter subscription plans, Payment handling, Invoice generation
📍 Base URL:/subscriptions
🔗 Key Endpoints:
POST /subscribe
PUT /cancel
PUT /renew
GET /invoices

<h2>📊 8. Analytics Service</h2>
📌 Responsibilities:Job analytics, Platform insights, Hiring metrics
📍 Base URL:/analytics
🔗 Key Endpoints:
GET /recruiter/{id}
GET /admin

🔄 Inter-Service Communication:REST APIs for synchronous calls
RabbitMQ for:Notifications,Email triggers,Event-driven updates

<h2>📈 Non-Functional Features</h2>
<ul>
<li>Scalable microservices</li>
<li>High availability</li>
<li>Secure APIs with JWT</li>
<li>Fast job search (Elasticsearch)</li>
</ul>

