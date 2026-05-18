# Blog App

A full-stack blog application with a Spring Boot REST API backend and React + TypeScript frontend.

## Tech Stack

**Backend**
- Java 21 / Spring Boot 4
- Spring Security 7 + JWT authentication
- Spring Data JPA + Hibernate
- PostgreSQL
- MapStruct
- Lombok

**Frontend**
- React 18 + TypeScript
- Vite
- Axios
- Tailwind CSS

## Project Structure

```
blog/
├── backend/     # Spring Boot REST API
└── frontend/    # React + TypeScript SPA
```

## Features

- JWT-based authentication (login, token validation)
- Create, read, update posts
- Draft and published post states
- Filter posts by category and tags
- Category and tag management

## Getting Started

### Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL running locally

### Backend

1. Create a PostgreSQL database
2. Update `backend/src/main/resources/application.properties` with your database credentials
3. Run the backend:

```bash
cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. A test user is seeded automatically:
- Email: `user@test.com`
- Password: `password`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app starts on `http://localhost:5173`. Requests to `/api` are proxied to the backend.

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/login` | No | Login and receive JWT |
| GET | `/api/v1/posts` | No | Get all published posts |
| GET | `/api/v1/posts/{id}` | No | Get a single post |
| POST | `/api/v1/posts` | Yes | Create a post |
| PUT | `/api/v1/posts/{id}` | Yes | Update a post |
| DELETE | `/api/v1/posts/{id}` | Yes | Delete a post |
| GET | `/api/v1/posts/drafts` | Yes | Get current user's drafts |
| GET | `/api/v1/categories` | No | List categories |
| POST | `/api/v1/categories` | Yes | Create a category |
| GET | `/api/v1/tags` | No | List tags |
| POST | `/api/v1/tags` | Yes | Create tags |

## Environment Variables

### Backend

| Variable | Description |
|----------|-------------|
| `JWT_SECRET_KEY` | Secret key for signing JWTs |
| `DATABASE_URL` | JDBC connection string |
| `DATABASE_USERNAME` | Database username |
| `DATABASE_PASSWORD` | Database password |

### Frontend

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | Backend base URL (leave empty for local dev) |

## Deployment

**Backend + Database** — Railway, Render, or Fly.io

**Frontend** — Vercel, Netlify, or Cloudflare Pages

Set `VITE_API_BASE_URL` in your frontend hosting dashboard to the deployed backend URL.
