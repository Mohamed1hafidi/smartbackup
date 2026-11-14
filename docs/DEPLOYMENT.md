# SmartBackup — Guide de Déploiement Cloud Complet

> **Durée estimée :** 90 minutes pour un premier déploiement.
> **Budget :** 0 € (tous les services utilisent leur offre gratuite).

## Table des matières
1. [Prérequis](#1-prérequis)
2. [Push du code sur GitHub](#2-push-du-code-sur-github)
3. [Supabase — Base de données PostgreSQL](#3-supabase--base-de-données-postgresql)
4. [Cloudflare R2 — Stockage des fichiers](#4-cloudflare-r2--stockage-des-fichiers)
5. [Render — Déploiement du Backend](#5-render--déploiement-du-backend)
6. [Vercel — Déploiement du Frontend](#6-vercel--déploiement-du-frontend)
7. [Configuration CORS et variables finales](#7-configuration-cors-et-variables-finales)
8. [GitHub Actions — Secrets CI/CD](#8-github-actions--secrets-cicd)
9. [Cloudflare Worker — Keep-alive & Scheduler](#9-cloudflare-worker--keep-alive--scheduler)
10. [Tests post-déploiement](#10-tests-post-déploiement)
11. [Limites des offres gratuites](#11-limites-des-offres-gratuites)
12. [Résolution des problèmes courants](#12-résolution-des-problèmes-courants)

---

## 1. Prérequis

### Comptes à créer (tous gratuits)

| Service | URL | Usage |
|---|---|---|
| GitHub | https://github.com | Code source, CI/CD |
| Supabase | https://supabase.com | PostgreSQL |
| Cloudflare | https://cloudflare.com | R2 Storage + Worker |
| Render | https://render.com | Hébergement Backend Java |
| Vercel | https://vercel.com | Hébergement Frontend React |

### Outils locaux nécessaires
- Git installé et configuré
- Accès au dossier `C:\Users\simoh\Documents\Omor devops\smartbackup`

---

## 2. Push du code sur GitHub

### 2.1 Créer le dépôt GitHub

1. Aller sur https://github.com/new
2. **Repository name :** `smartbackup`
3. **Visibility :** Private (recommandé)
4. **Ne pas** initialiser avec README (votre code existe déjà)
5. Cliquer **Create repository**

### 2.2 Pousser le code

```bash
cd "C:\Users\simoh\Documents\Omor devops\smartbackup"

# Initialiser Git si pas encore fait
git init
git add .
git commit -m "feat: initial SmartBackup project"

# Lier votre dépôt GitHub (remplacer VOTRE_USERNAME)
git remote add origin https://github.com/VOTRE_USERNAME/smartbackup.git

# Pousser sur main
git branch -M main
git push -u origin main
```

> [!WARNING]
> Vérifiez que `.gitignore` exclut bien `.env`, `target/`, `node_modules/` avant de pusher.

> [!CAUTION]
> **NE JAMAIS** committer le fichier `.env` avec de vraies clés. Seul `.env.example` doit être versionné.

---

## 3. Supabase — Base de données PostgreSQL

### 3.1 Créer le projet Supabase

1. Aller sur https://supabase.com → **New Project**
2. **Name :** `smartbackup`
3. **Database Password :** Générer un mot de passe fort et **le noter**
4. **Region :** Choisir la plus proche (ex: `EU West`)
5. Cliquer **Create new project** → attendre ~2 minutes

### 3.2 Récupérer les paramètres de connexion

1. Aller dans **Project Settings → Database**
2. Sous **Connection string**, sélectionner l'onglet **URI**
3. Choisir **Connection pooling** (mode Session, port 5432)

> [!IMPORTANT]
> Utilisez le **Connection Pooler** (port `5432`, mode Session) et non la connexion directe (port `5432` mais différente URL). Cela évite les dépassements de connexions sur le plan gratuit.

**Variables à noter :**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://aws-0-eu-west-1.pooler.supabase.com:5432/postgres?pgpassword=VOTRE_PASSWORD
SPRING_DATASOURCE_USERNAME=postgres.VOTRE_PROJECT_REF
SPRING_DATASOURCE_PASSWORD=VOTRE_DATABASE_PASSWORD
```

La vraie URL se trouve dans **Settings → Database → Connection string → Java (JDBC)**.

### 3.3 Vérifier la connexion (optionnel)

Dans Supabase, aller dans **SQL Editor** et lancer :
```sql
SELECT version();
```
Si ça répond, la DB est opérationnelle.

> [!NOTE]
> Les migrations Flyway (V1 à V10) s'exécuteront automatiquement au premier démarrage de Spring Boot sur Render. Vous n'avez rien à faire manuellement.

---

## 4. Cloudflare R2 — Stockage des fichiers

### 4.1 Accéder à R2

1. Aller sur https://dash.cloudflare.com
2. Dans le menu gauche → **R2 Object Storage**
3. Si première utilisation : entrer une carte bancaire (vérification d'identité, aucun débit sur le plan gratuit)

### 4.2 Créer le bucket

1. Cliquer **Create bucket**
2. **Bucket name :** `smartbackup-files`
3. **Location :** Automatic (ou EU pour conformité RGPD)
4. Cliquer **Create bucket**

> [!IMPORTANT]
> Le bucket doit rester **privé** (pas de public access). Les fichiers seront accessibles via des URL signées générées par le backend.

### 4.3 Créer les identifiants API R2

1. Dans R2 → **Manage R2 API Tokens**
2. Cliquer **Create API token**
3. **Token name :** `smartbackup-backend`
4. **Permissions :** Object Read & Write
5. **Specify bucket :** Sélectionner `smartbackup-files`
6. Cliquer **Create API Token**

**Copier immédiatement (affiché une seule fois) :**
```
R2_ACCESS_KEY_ID=XXXXXXXXXXXXXXXXXXXXXXXX
R2_SECRET_ACCESS_KEY=XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

### 4.4 Récupérer l'endpoint R2

1. Dans R2 → Votre bucket `smartbackup-files`
2. Aller dans **Settings**
3. Sous **S3 API**, noter l'**Endpoint** :

```
R2_ENDPOINT=https://ACCOUNT_ID.r2.cloudflarestorage.com
R2_BUCKET_NAME=smartbackup-files
R2_REGION=auto
```

### 4.5 Configurer CORS sur R2 (obligatoire pour les téléchargements)

Dans le bucket → **Settings** → **CORS Policy** → Ajouter :

```json
[
  {
    "AllowedOrigins": ["https://VOTRE_APP.vercel.app", "http://localhost:5173"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
    "AllowedHeaders": ["*"],
    "MaxAgeSeconds": 3000
  }
]
```

---

## 5. Render — Déploiement du Backend

### 5.1 Créer le Web Service

1. Aller sur https://render.com → **New → Web Service**
2. **Connect a repository** → Autoriser Render à accéder à votre GitHub
3. Sélectionner votre dépôt `smartbackup`
4. Configuration :
   - **Name :** `smartbackup-backend`
   - **Region :** Frankfurt (EU)
   - **Branch :** `main`
   - **Root Directory :** `backend`
   - **Runtime :** Docker
   - **Dockerfile Path :** `./Dockerfile`
5. **Instance Type :** Free

### 5.2 Variables d'environnement sur Render

Dans **Environment → Environment Variables**, ajouter **toutes** ces variables :

| Clé | Valeur |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...supabase.com:5432/postgres` (votre URL JDBC Supabase) |
| `SPRING_DATASOURCE_USERNAME` | `postgres.VOTRE_PROJECT_REF` |
| `SPRING_DATASOURCE_PASSWORD` | `VOTRE_DATABASE_PASSWORD` |
| `JWT_SECRET` | Générer : `openssl rand -base64 64` (min 64 chars) |
| `JWT_EXPIRATION_MS` | `86400000` |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` |
| `R2_ENDPOINT` | `https://ACCOUNT_ID.r2.cloudflarestorage.com` |
| `R2_BUCKET_NAME` | `smartbackup-files` |
| `R2_REGION` | `auto` |
| `R2_ACCESS_KEY_ID` | Votre clé R2 |
| `R2_SECRET_ACCESS_KEY` | Votre secret R2 |
| `R2_PATH_STYLE_ACCESS` | `false` |
| `R2_PRESIGNED_URL_EXPIRY_SECONDS` | `900` |
| `APP_FRONTEND_URL` | `https://VOTRE_APP.vercel.app` (à remplir après Vercel) |
| `CORS_ALLOWED_ORIGINS` | `https://VOTRE_APP.vercel.app` |
| `APP_MAX_FILE_SIZE_MB` | `50` |
| `APP_SCHEDULER_ENABLED` | `true` |
| `SCHEDULER_SECRET` | Générer : `openssl rand -base64 32` |
| `JAVA_OPTS` | `-Xms128m -XX:MaxRAMPercentage=75 -XX:+UseG1GC` |
| `LOG_LEVEL` | `INFO` |

> [!WARNING]
> Render Free Tier = **512 Mo RAM**. Le `JAVA_OPTS` ci-dessus est optimisé pour cette contrainte.

### 5.3 Health Check

Dans **Settings → Health & Alerts** :
- **Health Check Path :** `/actuator/health`

### 5.4 Lancer le premier déploiement

Cliquer **Deploy** → Render va :
1. Cloner votre repo
2. Construire l'image Docker (Maven télécharge les dépendances ~5 min)
3. Lancer le conteneur
4. Exécuter les migrations Flyway (V1→V10 sur Supabase)

**Vérifier les logs :** Dans Render → Logs, vous devriez voir :
```
Flyway migration: 10 migrations applied
Started SmartBackupApplication in X.X seconds
```

### 5.5 Récupérer l'URL Render

Une fois déployé :
```
https://smartbackup-backend-XXXX.onrender.com
```
Notez-la — vous en aurez besoin pour Vercel et pour le Cloudflare Worker.

---

## 6. Vercel — Déploiement du Frontend

### 6.1 Importer le projet

1. Aller sur https://vercel.com → **New Project**
2. **Import Git Repository** → Sélectionner `smartbackup`
3. Configuration :
   - **Framework Preset :** Vite
   - **Root Directory :** `frontend`
   - **Build Command :** `npm run build`
   - **Output Directory :** `dist`
   - **Install Command :** `npm ci`

### 6.2 Variables d'environnement Vercel

Dans **Settings → Environment Variables** :

| Clé | Valeur |
|---|---|
| `VITE_API_BASE_URL` | `https://smartbackup-backend-XXXX.onrender.com` |
| `VITE_APP_NAME` | `SmartBackup` |

> [!IMPORTANT]
> Les variables Vite doivent commencer par `VITE_` pour être exposées au bundle JavaScript. Ne mettez **jamais** de secrets (clés R2, etc.) dans les variables Vercel.

### 6.3 Lancer le déploiement

Cliquer **Deploy** → Vercel va :
1. Installer les dépendances npm
2. Exécuter `npm run build` (TypeScript + Vite)
3. Déployer sur le CDN mondial

**URL générée :** `https://smartbackup-XXXX.vercel.app`

---

## 7. Configuration CORS et variables finales

### 7.1 Mettre à jour CORS sur Render

Maintenant que vous avez l'URL Vercel, retourner dans Render → Environment :

| Clé | Valeur mise à jour |
|---|---|
| `APP_FRONTEND_URL` | `https://smartbackup-XXXX.vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://smartbackup-XXXX.vercel.app` |

Render va redéployer automatiquement.

### 7.2 Mettre à jour CORS sur R2

Retourner dans Cloudflare R2 → bucket → Settings → CORS Policy et remplacer `VOTRE_APP.vercel.app` par votre vraie URL Vercel.

### 7.3 Domaine personnalisé (optionnel)

- **Vercel :** Settings → Domains → Ajouter votre domaine
- **Render :** Settings → Custom Domains → Ajouter votre domaine

---

## 8. GitHub Actions — Secrets CI/CD

Dans votre repo GitHub → **Settings → Secrets and variables → Actions → New repository secret** :

| Secret | Valeur |
|---|---|
| `RENDER_DEPLOY_HOOK_URL` | Render → Settings → Deploy Hook → Copier l'URL |
| `VERCEL_TOKEN` | https://vercel.com/account/tokens → Create |
| `VERCEL_ORG_ID` | Vercel → Settings → General → Team ID |
| `VERCEL_PROJECT_ID` | Vercel → Project → Settings → General → Project ID |
| `RENDER_BACKEND_URL` | `https://smartbackup-backend-XXXX.onrender.com` |

**Trouver le Deploy Hook Render :**
1. Dans Render → votre service → **Settings**
2. Scroll jusqu'à **Deploy Hooks**
3. Créer un hook nommé `github-actions`
4. Copier l'URL générée

---

## 9. Cloudflare Worker — Keep-alive & Scheduler

### 9.1 Pourquoi c'est nécessaire

Render Free Tier **endort** votre backend après 15 minutes d'inactivité. Le `@Scheduled` Spring ne se déclenchera pas si le serveur dort. Le Worker Cloudflare (gratuit : 100 000 requêtes/jour) ping le backend toutes les 10 minutes.

### 9.2 Déployer le Worker

**Option A — Via le Dashboard Cloudflare (recommandée pour débutants) :**

1. Cloudflare Dashboard → **Workers & Pages → Create Application → Create Worker**
2. **Name :** `smartbackup-scheduler`
3. Cliquer **Deploy** (code par défaut)
4. Cliquer **Edit code** → Remplacer par le contenu de `cloudflare-worker/worker.js`
5. Cliquer **Save and Deploy**
6. Dans **Settings → Variables** → Ajouter :
   - `BACKEND_URL` = `https://smartbackup-backend-XXXX.onrender.com`
   - `SCHEDULER_SECRET` = (la même valeur que sur Render)
7. Dans **Triggers → Cron Triggers** → Ajouter `*/10 * * * *`

**Option B — Via Wrangler CLI :**
```bash
npm install -g wrangler
cd cloudflare-worker
wrangler login
wrangler secret put SCHEDULER_SECRET
wrangler deploy
```

---

## 10. Tests post-déploiement

### 10.1 Checklist de validation

Exécuter ces tests dans l'ordre après le déploiement.

#### Backend
```bash
# Health check
curl https://smartbackup-backend-XXXX.onrender.com/actuator/health
# Réponse attendue : {"status":"UP"}

# Swagger disponible
curl -I https://smartbackup-backend-XXXX.onrender.com/swagger-ui.html
# Réponse : HTTP 302 (redirection vers swagger-ui/index.html)
```

#### Authentification
- [ ] Ouvrir https://smartbackup-XXXX.vercel.app
- [ ] Créer un compte → Vérifier l'email reçu (si email activé)
- [ ] Se connecter → Redirection vers le Dashboard

#### Upload & Sauvegarde manuelle
- [ ] Aller dans **My Files** → Uploader un fichier PDF
- [ ] Vérifier que le fichier apparaît dans la liste
- [ ] Cliquer **Backup Now** → Statut SUCCESS
- [ ] Vérifier dans Supabase SQL Editor :
```sql
SELECT id, status, sha256_hash FROM backups ORDER BY created_at DESC LIMIT 1;
```
- [ ] Vérifier dans Cloudflare R2 que l'objet existe dans le bucket

#### Restauration
- [ ] Aller dans **Restore** → Sélectionner le fichier et la version
- [ ] Lancer la restauration → Statut SUCCESS
- [ ] Vérifier que le SHA-256 correspond dans les logs

#### Planification
- [ ] Aller dans **Schedules** → Créer une planification DAILY
- [ ] Vérifier qu'elle apparaît dans la liste avec `is_active = true`
- [ ] Tester le trigger Worker :
```bash
curl -X POST https://smartbackup-backend-XXXX.onrender.com/api/internal/scheduler/trigger \
  -H "X-Scheduler-Secret: VOTRE_SECRET" \
  -H "Content-Type: application/json"
# Réponse : {"triggered":true,"schedulesExecuted":0,...}
```

---

## 11. Limites des offres gratuites

> [!WARNING]
> Ces limites peuvent changer. Toujours vérifier la page pricing officielle.

| Service | Limite Free | Risque |
|---|---|---|
| **Render** | 750h/mois, 512 Mo RAM, mise en veille 15min | Backend peut être lent au réveil (~30s cold start) |
| **Supabase** | 500 Mo DB, 2 Go bandwidth, 2 projets | Supprimer les vieux logs si limite atteinte |
| **Cloudflare R2** | 10 Go stockage, 1M requêtes GET/mois, 10M requêtes Class A/mois | Surveiller l'espace disque |
| **Vercel** | 100 Go bandwidth, 100 déploiements/jour | Très généreux, rarement atteint |
| **Cloudflare Workers** | 100 000 requêtes/jour | `*/10 * * * *` = 144/jour, très en dessous |

### Solutions si limites atteintes
- **Render plein :** Passer à Fly.io (3 VMs gratuites) ou Railway (500h/mois)
- **Supabase plein :** Nettoyer `activity_logs` (supprimer les entrées > 30 jours)
- **R2 plein :** Activer la politique de rétention (Module 9) pour supprimer les vieilles versions

---

## 12. Résolution des problèmes courants

### Le backend ne démarre pas sur Render

**Symptôme :** Logs montrent `Connection refused` à Supabase.

**Solution :** Vérifier que l'URL JDBC utilise le pooler et non la connexion directe. Format correct :
```
jdbc:postgresql://aws-0-XX.pooler.supabase.com:5432/postgres
```

### Flyway échoue au démarrage

**Symptôme :** `FlywayException: Unable to obtain connection`

**Solutions :**
1. Vérifier `SPRING_DATASOURCE_URL`, `USERNAME`, `PASSWORD`
2. Dans Supabase → Settings → Database → vérifier que "Allow connection pooling" est activé
3. Dans Supabase → Authentication → Vérifier que RLS (Row Level Security) n'est pas activé sur vos tables

### Upload échoue en production

**Symptôme :** `SdkClientException: Unable to execute HTTP request`

**Solutions :**
1. Vérifier `R2_ENDPOINT` (format : `https://ACCOUNT_ID.r2.cloudflarestorage.com`)
2. Vérifier que `R2_PATH_STYLE_ACCESS=false` pour R2
3. Vérifier les permissions du token R2 (Object Read & Write sur le bon bucket)

### CORS Error dans le navigateur

**Symptôme :** `Access-Control-Allow-Origin` manquant

**Solutions :**
1. Vérifier `CORS_ALLOWED_ORIGINS` sur Render = URL exacte Vercel (sans slash final)
2. Vérifier que `APP_FRONTEND_URL` est aussi mis à jour
3. Redéployer le backend après chaque changement de variable

### Cold Start Render lent (30s+)

**Symptôme :** Première requête après inactivité prend 30 secondes.

**Solution :** Le Cloudflare Worker pinge toutes les 10 minutes — le serveur ne devrait jamais dormir complètement. Si le problème persiste, réduire l'intervalle à `*/5 * * * *`.
