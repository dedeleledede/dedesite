# PostgreSQL deployment

Production uses the `prod` Spring profile and PostgreSQL. The local default remains the disposable H2 database.

On the server:

```bash
cd backend
cp .env.example .env
openssl rand -base64 32
openssl rand -base64 32
chmod 600 .env
docker compose -f docker-compose.prod.yml up -d --build
```

Put the first random value in both `SPRING_DATASOURCE_PASSWORD` and `POSTGRES_PASSWORD`.
Put the second random value in `OBSERVATORY_ENCRYPTION_KEY`.
Set `APP_PUBLIC_URL` to the public HTTPS origin used by the Cloudflare Tunnel.

The `.env` file stays only on the server and is ignored by Git. Keep a secure backup of
`OBSERVATORY_ENCRYPTION_KEY`: losing or changing it makes existing Observatory ciphertext unreadable.

Useful operations:

```bash
docker compose -f docker-compose.prod.yml logs -f app
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --build
```
