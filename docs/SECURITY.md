# 🔒 SmartBackup — Guide de Sécurité

> [!IMPORTANT]
> Ce document décrit le modèle de sécurité de SmartBackup. Toute modification des mécanismes de sécurité doit être revue par l'équipe et reflétée ici.

---

## Table des matières

1. [Modèle de menaces](#1-modèle-de-menaces)
2. [Authentification](#2-authentification)
3. [Autorisation](#3-autorisation)
4. [Sécurité des fichiers](#4-sécurité-des-fichiers)
5. [Cloudflare R2 — Sécurité du stockage](#5-cloudflare-r2--sécurité-du-stockage)
6. [CORS — Politique d'origines](#6-cors--politique-dorigines)
7. [Variables d'environnement et secrets](#7-variables-denvironnement-et-secrets)
8. [Logs sécurisés](#8-logs-sécurisés)
9. [Protection brute force et rate limiting](#9-protection-brute-force-et-rate-limiting)
10. [HTTPS et transport sécurisé](#10-https-et-transport-sécurisé)
11. [Checklist sécurité pré-déploiement](#11-checklist-sécurité-pré-déploiement)

---

## 1. Modèle de menaces

| # | Menace | Vecteur d'attaque | Mitigation implémentée | Statut |
|---|--------|-------------------|------------------------|--------|
| T-01 | Vol de credentials utilisateur | Brute force sur `/api/auth/login` | Rate limiting (5 req/min/IP), BCrypt coût 12 | ✅ Implémenté |
| T-02 | Accès non autorisé aux fichiers d'autrui | Manipulation de l'ID de ressource dans l'URL | Vérification propriété côté backend (`userId == file.userId`) | ✅ Implémenté |
| T-03 | Injection SQL | Paramètres malveillants dans les requêtes | JPA / Prepared Statements uniquement, pas de SQL natif dynamique | ✅ Implémenté |
| T-04 | Upload de fichier malveillant | Upload de `.exe`, scripts, malwares | Validation MIME type + extension + taille max | ✅ Implémenté |
| T-05 | Exposition des clés R2 | Clés dans le code frontend ou les logs | Clés uniquement dans les variables d'env backend, jamais exposées | ✅ Implémenté |
| T-06 | Interception des communications | MITM sur HTTP | HTTPS obligatoire en production (Render + Vercel forcent TLS) | ✅ Implémenté |
| T-07 | Cross-Site Request Forgery (CSRF) | Requêtes forgées depuis un autre domaine | CORS strict, JWT Bearer (pas de cookies de session) | ✅ Implémenté |
| T-08 | JWT forgé ou tampered | Modification du payload JWT | Signature HMAC-SHA256, vérification systématique | ✅ Implémenté |
| T-09 | Fuite de secrets dans Git | Commit accidentel de `.env` | `.gitignore` + pre-commit hook + `.env.example` sans valeurs | ✅ Implémenté |
| T-10 | Path traversal | `../../../etc/passwd` dans le nom de fichier | Sanitisation du nom de fichier, stockage par UUID côté R2 | ✅ Implémenté |
| T-11 | Énumération des utilisateurs | Réponses différentes selon si l'email existe | Message d'erreur générique pour login/register | ✅ Implémenté |
| T-12 | Déni de service (DoS) | Upload massif de fichiers volumineux | Limite de taille (50 MB), rate limiting, quota par utilisateur | ⚠️ Partiel |
| T-13 | Élévation de privilèges | Manipulation des rôles dans le JWT | Rôles chargés depuis la DB à chaque requête, pas depuis le JWT | ✅ Implémenté |
| T-14 | Session hijacking | Vol du JWT (XSS) | Token dans memory/httpOnly cookie, durée courte (24h), refresh | ⚠️ En cours |

---

## 2. Authentification

### 2.1 JWT Lifecycle

```
┌─────────────┐     POST /api/auth/login      ┌───────────────────┐
│   Frontend  │ ─────────────────────────────► │  Backend Spring   │
│             │  { email, password }           │                   │
│             │ ◄────────────────────────────── │  1. Vérifie email │
│             │  { accessToken, refreshToken } │  2. BCrypt compare│
│             │                                │  3. Génère tokens │
└─────────────┘                                └───────────────────┘
       │
       │  Chaque requête protégée :
       │  Authorization: Bearer <accessToken>
       ▼
┌─────────────────────────────────────────────────────────────────┐
│  JwtAuthFilter (OncePerRequestFilter)                           │
│  1. Extraire le token du header Authorization                   │
│  2. Valider la signature HMAC-SHA256                            │
│  3. Vérifier l'expiration                                       │
│  4. Charger UserDetails depuis la DB (pas depuis le JWT)        │
│  5. Vérifier que l'utilisateur est actif (is_active = true)     │
│  6. Alimenter le SecurityContext                                │
└─────────────────────────────────────────────────────────────────┘
```

**Paramètres JWT :**
- Algorithme : `HMAC-SHA256` (HS256)
- Durée access token : **24 heures** (`JWT_EXPIRATION_MS=86400000`)
- Durée refresh token : **7 jours** (`JWT_REFRESH_EXPIRATION_MS=604800000`)
- Secret : minimum 32 caractères, généré avec `openssl rand -base64 48`

### 2.2 BCrypt

- Facteur de coût : **12** (recommandé OWASP 2024)
- Hashage à l'inscription, comparaison lors du login
- Le mot de passe en clair n'est jamais persisté ni loggé

```java
// Configuration Spring Security
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

### 2.3 Rotation des tokens

- **Refresh token rotation** : chaque utilisation du refresh token invalide l'ancien et génère une nouvelle paire
- **Révocation** : table `refresh_tokens` en base avec flag `is_revoked`
- **Logout** : révocation explicite du refresh token côté backend

---

## 3. Autorisation

### 3.1 Principe fondamental

> [!CAUTION]
> L'autorisation est **toujours** vérifiée côté backend. Le frontend peut masquer des éléments UI, mais il ne constitue **jamais** une barrière de sécurité.

### 3.2 Vérification de propriété des ressources

Pour chaque opération sur une ressource (fichier, backup, planification), le backend vérifie :

```java
// Exemple dans FileService
public FileDto getFile(UUID fileId, UUID authenticatedUserId) {
    File file = fileRepository.findById(fileId)
        .orElseThrow(() -> new ResourceNotFoundException("File not found"));

    // Vérification propriété — OBLIGATOIRE
    if (!file.getUserId().equals(authenticatedUserId)) {
        throw new AccessDeniedException("Access denied");
    }

    return fileMapper.toDto(file);
}
```

**Principe :** un utilisateur ne peut accéder qu'à **ses propres ressources**, jamais à celles des autres, même en connaissant leur UUID.

### 3.3 Endpoints publics vs protégés

| Endpoint | Accès |
|----------|-------|
| `POST /api/auth/register` | Public |
| `POST /api/auth/login` | Public |
| `POST /api/auth/refresh` | Public (avec refresh token valide) |
| `GET /actuator/health` | Public |
| `GET /actuator/prometheus` | Restreint (IP whitelist ou Basic Auth) |
| Tous les autres `/api/**` | JWT obligatoire |

---

## 4. Sécurité des fichiers

### 4.1 Validation MIME type

La validation est effectuée en **deux étapes** côté backend :

1. **Vérification du Content-Type HTTP** (facilement falsifiable — première barrière seulement)
2. **Inspection magique des bytes** (Apache Tika) — vérification réelle du contenu du fichier

```java
// Validation avec Apache Tika
Tika tika = new Tika();
String detectedType = tika.detect(inputStream);
if (!allowedMimeTypes.contains(detectedType)) {
    throw new InvalidFileException("Type de fichier non autorisé : " + detectedType);
}
```

**Types autorisés (défaut) :**
```
image/jpeg, image/png, image/gif, image/webp,
application/pdf, text/plain,
application/zip, application/x-zip-compressed,
application/json
```

### 4.2 Limite de taille

- Configurée via `APP_MAX_FILE_SIZE_MB` (défaut : 50 MB)
- Appliquée au niveau Spring (`spring.servlet.multipart.max-file-size`)
- Et au niveau Nginx/reverse proxy si applicable

```yaml
# application.yml
spring:
  servlet:
    multipart:
      max-file-size: ${APP_MAX_FILE_SIZE_MB:50}MB
      max-request-size: ${APP_MAX_FILE_SIZE_MB:50}MB
```

### 4.3 Sanitisation des noms de fichiers

Le nom original du fichier est **stocké en base uniquement pour l'affichage**. La clé de stockage R2 est toujours un UUID généré côté serveur :

```java
// Clé R2 : jamais le nom original
String r2Key = "files/" + userId + "/" + UUID.randomUUID() + "/" + sanitizedFilename;

// Sanitisation du nom original (affichage uniquement)
String sanitizedFilename = originalFilename
    .replaceAll("[^a-zA-Z0-9._-]", "_")  // remplace les caractères spéciaux
    .replaceAll("\\.{2,}", ".")           // élimine le path traversal (..)
    .substring(0, Math.min(255, originalFilename.length())); // limite longueur
```

---

## 5. Cloudflare R2 — Sécurité du stockage

### 5.1 Clés d'accès — Côté serveur uniquement

> [!CAUTION]
> Les clés R2 (`R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`) ne doivent **jamais** apparaître dans le code frontend, les logs, les réponses API ou les variables Vite (`VITE_*`).

- Les clés sont injectées uniquement dans les variables d'environnement du backend (Render)
- Le client S3 (AWS SDK v2) est initialisé côté Spring Boot uniquement

### 5.2 Bucket privé

- Le bucket R2 est configuré en **accès privé** (aucun objet n'est public par défaut)
- Aucun accès direct possible depuis le navigateur sans URL signée

### 5.3 URLs signées temporaires (Presigned URLs)

Pour le téléchargement, le backend génère une URL signée à durée limitée :

```java
// Génération d'une URL signée valable 1 heure
GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
    .signatureDuration(Duration.ofSeconds(r2PresignedUrlExpiry))
    .getObjectRequest(req -> req.bucket(bucketName).key(r2ObjectKey))
    .build();

PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
return presignedRequest.url().toString();
```

**Durée recommandée :** 1 heure (`R2_PRESIGNED_URL_EXPIRY_SECONDS=3600`)

### 5.4 Upload direct via backend

Les fichiers transitent par le backend Spring Boot avant d'être envoyés sur R2 — le frontend ne contacte jamais R2 directement.

---

## 6. CORS — Politique d'origines

### 6.1 Configuration stricte

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(frontendUrl) // Origine unique whitelistée
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "Accept")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

### 6.2 Origines whitelistées

| Environnement | Origine autorisée |
|---------------|------------------|
| Développement | `http://localhost:5173` |
| Production | `https://smartbackup.vercel.app` (ou domaine custom) |

> [!WARNING]
> N'utilisez **jamais** `allowedOrigins("*")` en production. Cela annule toute protection CORS.

---

## 7. Variables d'environnement et secrets

### 7.1 Règles absolues

1. **Ne jamais committer** `.env` — vérifié par `.gitignore`
2. **Ne jamais hardcoder** de secrets dans le code source
3. **Utiliser `.env.example`** avec des placeholders, jamais de valeurs réelles
4. **Injecter en production** via les dashboards Render (backend) et Vercel (frontend)
5. **Préfixer `VITE_`** uniquement pour les variables destinées au navigateur (non sensibles)

### 7.2 `.gitignore` — Entrées obligatoires

```gitignore
# Secrets
.env
.env.local
.env.production

# Java
*.class
target/

# Node
node_modules/
dist/

# IDE
.idea/
*.iml
.vscode/
```

### 7.3 Rotation des secrets

En cas de compromission suspectée :
1. Révoquer immédiatement la clé dans le dashboard Cloudflare / Supabase
2. Générer une nouvelle clé
3. Mettre à jour les variables d'env sur Render et Vercel
4. Redéployer les services
5. Invalider tous les JWT existants (changer `JWT_SECRET`)

---

## 8. Logs sécurisés

### 8.1 Ce qui ne doit JAMAIS apparaître dans les logs

| Donnée sensible | Exemple à bannir |
|-----------------|-----------------|
| Mots de passe | `"password": "monmotdepasse"` |
| Tokens JWT | `"Bearer eyJhbGci..."` |
| Clés API (R2, etc.) | `"accessKey": "abc123..."` |
| Données personnelles | Numéros de carte, SSN |
| Hashes de mot de passe BCrypt | `"$2a$12$..."` |

### 8.2 Configuration Logback

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Masquer les champs sensibles via un pattern custom -->
    <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>

    <!-- Ne pas logger le body des requêtes /api/auth/** -->
    <filter class="ch.qos.logback.core.filter.EvaluatorFilter">
        <evaluator>
            <expression>logger.contains("AuthController") and level &lt;= INFO</expression>
        </evaluator>
        <onMatch>DENY</onMatch>
    </filter>
</configuration>
```

### 8.3 Bonnes pratiques

```java
// ✅ BON — Logger une action sans données sensibles
log.info("User {} logged in successfully from IP {}", userId, ipAddress);

// ❌ MAUVAIS — Ne jamais logger le mot de passe
log.debug("Login attempt for user {} with password {}", email, password);

// ✅ BON — Logger l'ID de l'entité, pas son contenu sensible
log.info("File {} uploaded by user {}", fileId, userId);
```

---

## 9. Protection brute force et rate limiting

### 9.1 Rate Limiting avec Bucket4j (Spring Boot)

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    private Bucket createBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
            .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        if (request.getRequestURI().startsWith("/api/auth/")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = bucketCache.get(ip, k -> createBucket());

            if (!bucket.tryConsume(1)) {
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("{\"error\": \"Too many requests. Try again in 1 minute.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### 9.2 Limites configurées

| Endpoint | Limite | Fenêtre |
|----------|--------|---------|
| `POST /api/auth/login` | 5 tentatives | 1 minute par IP |
| `POST /api/auth/register` | 3 inscriptions | 1 heure par IP |
| `POST /api/files/upload` | 10 uploads | 1 minute par utilisateur |
| Autres `/api/**` | 100 requêtes | 1 minute par utilisateur |

---

## 10. HTTPS et transport sécurisé

### 10.1 Obligation HTTPS en production

> [!CAUTION]
> SmartBackup ne doit **jamais** être accessible en HTTP en production. Toutes les communications doivent être chiffrées avec TLS 1.2 minimum (TLS 1.3 recommandé).

- **Vercel** : HTTPS automatique avec certificat Let's Encrypt
- **Render** : HTTPS automatique sur les domaines `.onrender.com` et domaines custom
- **Supabase** : connexions PostgreSQL avec `sslmode=require` obligatoire

### 10.2 Headers de sécurité HTTP

Configurer les headers suivants dans Spring Boot :

```java
@Configuration
public class SecurityHeadersConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
            .contentSecurityPolicy("default-src 'self'; img-src 'self' data:; script-src 'self'")
            .frameOptions().deny()
            .contentTypeOptions().and()
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubdomains(true)
            )
        );
    }
}
```

| Header | Valeur recommandée |
|--------|-------------------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Content-Security-Policy` | `default-src 'self'` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |

### 10.3 Connexion Supabase sécurisée

```properties
# application.yml — SSL obligatoire
spring.datasource.url=${SPRING_DATASOURCE_URL}?sslmode=require
```

---

## 11. Checklist sécurité pré-déploiement

Cochez chaque point avant tout déploiement en production :

### 🔑 Secrets et configuration

- [ ] Le fichier `.env` est dans `.gitignore` et n'est pas tracké par Git
- [ ] Aucune valeur secrète dans `.env.example` (uniquement des placeholders)
- [ ] `JWT_SECRET` est un secret fort (≥ 32 chars, généré aléatoirement)
- [ ] Les clés R2 sont uniquement dans les variables Render (backend), jamais dans Vercel
- [ ] `SPRING_PROFILES_ACTIVE=prod` est configuré sur Render
- [ ] `VITE_DEBUG=false` est configuré sur Vercel

### 🔐 Authentification et autorisation

- [ ] BCrypt coût ≥ 12 configuré
- [ ] Durée JWT access token ≤ 24h
- [ ] Refresh token rotation activée
- [ ] Logout révoque le refresh token en base
- [ ] Toutes les routes `/api/**` (sauf auth) nécessitent un JWT valide
- [ ] Vérification de propriété (`userId == resource.userId`) dans chaque service

### 🛡️ Sécurité applicative

- [ ] Rate limiting activé sur `/api/auth/login` et `/api/auth/register`
- [ ] Validation MIME type (Apache Tika) activée pour les uploads
- [ ] Taille maximale de fichier configurée (`APP_MAX_FILE_SIZE_MB`)
- [ ] Noms de fichiers sanitisés avant stockage
- [ ] CORS configuré avec l'URL de production uniquement (pas de `*`)
- [ ] Headers de sécurité HTTP configurés (HSTS, CSP, X-Frame-Options)

### ☁️ Infrastructure

- [ ] Connexion Supabase avec `sslmode=require`
- [ ] Bucket R2 configuré en **privé** (aucun accès public)
- [ ] URLs signées R2 avec durée ≤ 1 heure
- [ ] HTTPS activé et vérifié sur Vercel et Render
- [ ] Redirection HTTP → HTTPS activée

### 📋 Logs et monitoring

- [ ] Aucun mot de passe / token / clé dans les logs (revue du code)
- [ ] Niveau de log en production : `INFO` (pas `DEBUG` ni `TRACE`)
- [ ] Les erreurs 500 ne retournent pas de stack trace à l'utilisateur
- [ ] Les messages d'erreur d'authentification sont génériques (pas "email inconnu")

### 🔄 Avant chaque déploiement

- [ ] Revue du diff Git — aucun secret commité
- [ ] Tests de sécurité passés (OWASP ZAP ou équivalent)
- [ ] Dépendances à jour (`./mvnw versions:display-dependency-updates`)
- [ ] Scan de vulnérabilités (`./mvnw dependency-check:check`)

---

*Document maintenu par l'équipe SmartBackup — Mise à jour : Phase 1*
