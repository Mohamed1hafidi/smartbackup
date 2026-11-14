# SmartBackup — Documentation API REST (Phase 1)

> **Version :** 1.0.0  
> **Base URL :** `https://your-render-app.onrender.com`  
> **Authentification :** JWT Bearer Token (sauf endpoints publics)  
> **Content-Type :** `application/json` (sauf upload fichier)

---

## Table des matières

1. [Format des réponses](#format-des-réponses)
2. [Authentification](#authentification)
3. [Utilisateurs](#utilisateurs)
4. [Fichiers](#fichiers)
5. [Sauvegardes](#sauvegardes)
6. [Programmations](#programmations)
7. [Notifications](#notifications)
8. [Dashboard](#dashboard)

---

## Format des réponses

### Réponse succès générique

```json
{
  "data": { ... },
  "message": "Opération réussie"
}
```

### Format d'erreur standard

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Le champ email est invalide",
  "path": "/api/auth/register",
  "details": ["email: format invalide"]
}
```

### Codes HTTP utilisés

| Code | Signification |
|------|--------------|
| `200` | OK — Requête réussie |
| `201` | Created — Ressource créée |
| `204` | No Content — Suppression réussie |
| `400` | Bad Request — Validation échouée |
| `401` | Unauthorized — Token absent ou invalide |
| `403` | Forbidden — Accès refusé |
| `404` | Not Found — Ressource introuvable |
| `409` | Conflict — Ressource déjà existante |
| `413` | Payload Too Large — Fichier trop volumineux |
| `500` | Internal Server Error — Erreur serveur |

---

## Authentification

### `POST /api/auth/register` — Inscription

**Description :** Crée un nouveau compte utilisateur.  
**Authentification requise :** ❌ Non

**Headers :**
```
Content-Type: application/json
```

**Body :**
```json
{
  "email": "alice@example.com",
  "password": "Secure@123",
  "fullName": "Alice Martin"
}
```

**Réponse succès — `201 Created` :**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "alice@example.com",
    "fullName": "Alice Martin",
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "message": "Compte créé avec succès"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Champ requis manquant ou format invalide |
| `409` | Un compte avec cet email existe déjà |

**Règles de validation :**
- `email` : format RFC 5321, requis, unique
- `password` : min 8 caractères, 1 majuscule, 1 chiffre, 1 caractère spécial
- `fullName` : 2–100 caractères, requis

> [!WARNING]
> Le mot de passe est haché avec BCrypt avant stockage. Ne jamais retourner le `password_hash` dans les réponses.

---

### `POST /api/auth/login` — Connexion

**Description :** Authentifie un utilisateur et retourne un JWT.  
**Authentification requise :** ❌ Non

**Headers :**
```
Content-Type: application/json
```

**Body :**
```json
{
  "email": "alice@example.com",
  "password": "Secure@123"
}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "alice@example.com",
      "fullName": "Alice Martin"
    }
  },
  "message": "Connexion réussie"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Email ou mot de passe manquant |
| `401` | Identifiants incorrects |
| `403` | Compte désactivé |

**Règles de validation :**
- `email` : requis, format email valide
- `password` : requis

**Contrôle d'autorisation :**
- Le JWT expire après 24h (`expiresIn: 86400` secondes)
- Payload JWT contient : `sub` (userId), `email`, `iat`, `exp`

---

## Utilisateurs

### `GET /api/users/me` — Profil connecté

**Description :** Retourne le profil de l'utilisateur authentifié.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "alice@example.com",
    "fullName": "Alice Martin",
    "isActive": true,
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-15T09:30:00Z"
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent, expiré ou invalide |

**Contrôle d'autorisation :** L'utilisateur ne peut accéder qu'à son propre profil (extrait du JWT).

---

### `PUT /api/users/me` — Mise à jour du profil

**Description :** Met à jour le nom complet de l'utilisateur authentifié.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body :**
```json
{
  "fullName": "Alice Dupont"
}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "alice@example.com",
    "fullName": "Alice Dupont",
    "updatedAt": "2024-01-20T10:00:00Z"
  },
  "message": "Profil mis à jour"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | fullName invalide (trop court, trop long) |
| `401` | Token absent ou invalide |

**Règles de validation :**
- `fullName` : 2–100 caractères, requis

---

### `PUT /api/users/me/password` — Changement de mot de passe

**Description :** Change le mot de passe de l'utilisateur authentifié.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body :**
```json
{
  "currentPassword": "Secure@123",
  "newPassword": "NewSecure@456",
  "confirmPassword": "NewSecure@456"
}
```

**Réponse succès — `200 OK` :**
```json
{
  "message": "Mot de passe modifié avec succès"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | newPassword ne respecte pas les règles / confirmPassword ne correspond pas |
| `401` | currentPassword incorrect |

**Règles de validation :**
- `currentPassword` : requis, doit correspondre au mot de passe actuel haché en base
- `newPassword` : min 8 caractères, 1 majuscule, 1 chiffre, 1 caractère spécial, différent de currentPassword
- `confirmPassword` : doit être identique à `newPassword`

> [!WARNING]
> Après changement de mot de passe, invalider tous les tokens JWT existants (blacklist ou rotation du secret utilisateur).

---

## Fichiers

### `POST /api/files` — Upload d'un fichier

**Description :** Envoie un fichier vers le stockage Cloudflare R2 et enregistre ses métadonnées.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Body (multipart/form-data) :**
```
file=<binary>          # Champ fichier (requis)
```

**Réponse succès — `201 Created` :**
```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "originalName": "rapport_annuel.pdf",
    "storedKey": "users/550e8400/files/a1b2c3d4-rapport_annuel.pdf",
    "fileSize": 2048576,
    "mimeType": "application/pdf",
    "sha256Hash": "e3b0c44298fc1c149afb...",
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "message": "Fichier uploadé avec succès"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Aucun fichier fourni |
| `401` | Token absent ou invalide |
| `413` | Fichier trop volumineux (limite : 500 Mo) |
| `500` | Erreur lors de l'upload vers R2 |

**Règles de validation :**
- Taille maximale : 500 Mo
- Types MIME autorisés : tous (validation côté serveur du Content-Type réel)
- Le nom du fichier est assaini (suppression des caractères dangereux)
- La clé R2 est générée : `users/{userId}/files/{uuid}-{filename}`
- Le hash SHA-256 est calculé côté serveur pour vérification d'intégrité

**Contrôle d'autorisation :** Le fichier est associé au `userId` extrait du JWT.

---

### `GET /api/files` — Liste des fichiers

**Description :** Retourne la liste paginée des fichiers de l'utilisateur.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Query Params :**

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `page` | int | `0` | Numéro de page (0-indexed) |
| `size` | int | `20` | Éléments par page (max 100) |
| `search` | string | — | Recherche par nom de fichier (ILIKE) |
| `sort` | string | `createdAt` | Champ de tri : `createdAt`, `originalName`, `fileSize` |
| `direction` | string | `desc` | Ordre : `asc` ou `desc` |

**Exemple :** `GET /api/files?page=0&size=10&search=rapport&sort=fileSize&direction=asc`

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "content": [
      {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "originalName": "rapport_annuel.pdf",
        "fileSize": 2048576,
        "mimeType": "application/pdf",
        "createdAt": "2024-01-01T12:00:00Z",
        "lastBackupAt": "2024-01-10T08:00:00Z"
      }
    ],
    "totalElements": 42,
    "totalPages": 5,
    "currentPage": 0,
    "size": 10
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Paramètre de pagination invalide |
| `401` | Token absent ou invalide |

**Contrôle d'autorisation :** Retourne uniquement les fichiers de l'utilisateur authentifié (`is_deleted = false`).

---

### `GET /api/files/{id}` — Détail d'un fichier

**Description :** Retourne les métadonnées complètes d'un fichier.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant du fichier

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "originalName": "rapport_annuel.pdf",
    "storedKey": "users/550e8400/files/a1b2c3d4-rapport_annuel.pdf",
    "fileSize": 2048576,
    "mimeType": "application/pdf",
    "sha256Hash": "e3b0c44298fc1c149afb...",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-15T09:30:00Z",
    "backupCount": 3,
    "lastBackupAt": "2024-01-10T08:00:00Z"
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Ce fichier appartient à un autre utilisateur |
| `404` | Fichier introuvable |

**Contrôle d'autorisation :** Vérifie que `file.userId == jwtUserId`.

---

### `DELETE /api/files/{id}` — Suppression d'un fichier

**Description :** Suppression logique du fichier (soft delete, `is_deleted = true`).  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant du fichier

**Réponse succès — `204 No Content`**

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Ce fichier appartient à un autre utilisateur |
| `404` | Fichier introuvable |

**Contrôle d'autorisation :** Vérifie que `file.userId == jwtUserId`.

> [!WARNING]
> La suppression est logique : l'objet R2 n'est pas immédiatement supprimé. Un job de nettoyage planifié peut purger les fichiers supprimés après une période de rétention.

---

### `GET /api/files/{id}/download` — Téléchargement (URL signée R2)

**Description :** Génère une URL présignée Cloudflare R2 valable 15 minutes pour télécharger le fichier.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant du fichier

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "downloadUrl": "https://pub-xxxx.r2.dev/users/550e8400/files/a1b2c3d4-rapport_annuel.pdf?X-Amz-Signature=...",
    "expiresAt": "2024-01-01T12:15:00Z",
    "fileName": "rapport_annuel.pdf",
    "fileSize": 2048576,
    "mimeType": "application/pdf"
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Ce fichier appartient à un autre utilisateur |
| `404` | Fichier introuvable |
| `500` | Erreur lors de la génération de l'URL signée |

**Contrôle d'autorisation :** Vérifie ownership. L'URL présignée est valable **15 minutes**.

---

## Sauvegardes

### `POST /api/backups/{fileId}` — Déclencher une sauvegarde manuelle

**Description :** Lance une sauvegarde manuelle immédiate pour un fichier donné.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `fileId` (UUID) — Identifiant du fichier à sauvegarder

**Réponse succès — `202 Accepted` :**
```json
{
  "data": {
    "id": "b1c2d3e4-f5a6-7890-bcde-fg2345678901",
    "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "fileName": "rapport_annuel.pdf",
    "backupType": "MANUAL",
    "status": "PENDING",
    "versionNumber": 4,
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "message": "Sauvegarde démarrée"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Ce fichier appartient à un autre utilisateur |
| `404` | Fichier introuvable |
| `409` | Une sauvegarde est déjà en cours pour ce fichier |

**Contrôle d'autorisation :** Vérifie que `file.userId == jwtUserId`. Une notification est créée à la fin de la sauvegarde.

---

### `GET /api/backups` — Historique des sauvegardes

**Description :** Retourne l'historique paginé et filtrable des sauvegardes.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Query Params :**

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `page` | int | `0` | Numéro de page |
| `size` | int | `20` | Éléments par page (max 100) |
| `status` | string | — | Filtre : `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED` |
| `fileId` | UUID | — | Filtre par fichier |
| `dateFrom` | ISO 8601 | — | Filtre date de début (`2024-01-01T00:00:00Z`) |
| `dateTo` | ISO 8601 | — | Filtre date de fin |

**Exemple :** `GET /api/backups?status=SUCCESS&fileId=a1b2c3d4&dateFrom=2024-01-01T00:00:00Z&page=0&size=10`

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "content": [
      {
        "id": "b1c2d3e4-f5a6-7890-bcde-fg2345678901",
        "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "fileName": "rapport_annuel.pdf",
        "backupType": "MANUAL",
        "status": "SUCCESS",
        "fileSize": 2048576,
        "versionNumber": 4,
        "createdAt": "2024-01-01T12:00:00Z",
        "completedAt": "2024-01-01T12:00:45Z"
      }
    ],
    "totalElements": 15,
    "totalPages": 2,
    "currentPage": 0,
    "size": 10
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Paramètre invalide (status inconnu, format date incorrect) |
| `401` | Token absent ou invalide |

**Contrôle d'autorisation :** Retourne uniquement les sauvegardes de l'utilisateur authentifié.

---

### `GET /api/backups/{id}` — Détail d'une sauvegarde

**Description :** Retourne les détails complets d'une sauvegarde.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant de la sauvegarde

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "b1c2d3e4-f5a6-7890-bcde-fg2345678901",
    "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "fileName": "rapport_annuel.pdf",
    "r2ObjectKey": "users/550e8400/backups/b1c2d3e4-v4-rapport_annuel.pdf",
    "r2Bucket": "smartbackup-prod",
    "fileSize": 2048576,
    "sha256Hash": "e3b0c44298fc1c149afb...",
    "backupType": "MANUAL",
    "status": "SUCCESS",
    "errorMessage": null,
    "versionNumber": 4,
    "createdAt": "2024-01-01T12:00:00Z",
    "completedAt": "2024-01-01T12:00:45Z"
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Cette sauvegarde appartient à un autre utilisateur |
| `404` | Sauvegarde introuvable |

---

### `POST /api/backups/{id}/restore` — Restaurer une sauvegarde

**Description :** Restaure la version sauvegardée en remplaçant le fichier courant dans R2.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant de la sauvegarde à restaurer

**Réponse succès — `202 Accepted` :**
```json
{
  "data": {
    "restoreJobId": "c1d2e3f4-a5b6-7890-cdef-gh3456789012",
    "backupId": "b1c2d3e4-f5a6-7890-bcde-fg2345678901",
    "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "status": "PENDING",
    "initiatedAt": "2024-01-01T12:00:00Z"
  },
  "message": "Restauration démarrée"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | La sauvegarde n'est pas dans un état SUCCESS |
| `401` | Token absent ou invalide |
| `403` | Cette sauvegarde appartient à un autre utilisateur |
| `404` | Sauvegarde introuvable |

> [!WARNING]
> La restauration écrase le fichier courant dans R2. Une nouvelle entrée de sauvegarde est créée automatiquement avant la restauration pour préserver l'historique.

---

## Programmations

### `POST /api/schedules` — Créer une programmation

**Description :** Crée une sauvegarde planifiée (quotidienne ou hebdomadaire) pour un fichier.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body :**
```json
{
  "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "frequency": "WEEKLY",
  "scheduledTime": "02:00",
  "dayOfWeek": "MONDAY",
  "timezone": "Europe/Paris"
}
```

| Champ | Type | Requis | Description |
|-------|------|--------|-------------|
| `fileId` | UUID | ✅ | Fichier à sauvegarder |
| `frequency` | enum | ✅ | `DAILY` ou `WEEKLY` |
| `scheduledTime` | HH:mm | ✅ | Heure d'exécution (24h) |
| `dayOfWeek` | string | Si WEEKLY | `MONDAY`…`SUNDAY` |
| `timezone` | string | ✅ | Fuseau horaire IANA (ex: `Europe/Paris`) |

**Réponse succès — `201 Created` :**
```json
{
  "data": {
    "id": "d1e2f3a4-b5c6-7890-defa-hi4567890123",
    "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "fileName": "rapport_annuel.pdf",
    "frequency": "WEEKLY",
    "scheduledTime": "02:00",
    "dayOfWeek": "MONDAY",
    "timezone": "Europe/Paris",
    "isActive": true,
    "nextRunAt": "2024-01-08T01:00:00Z",
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "message": "Programmation créée"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Champ requis manquant ou invalide |
| `401` | Token absent ou invalide |
| `403` | Ce fichier appartient à un autre utilisateur |
| `404` | Fichier introuvable |
| `409` | Une programmation active existe déjà pour ce fichier |

**Règles de validation :**
- `frequency` : uniquement `DAILY` ou `WEEKLY`
- `dayOfWeek` : requis si `frequency = WEEKLY`
- `timezone` : doit être un identifiant IANA valide
- `scheduledTime` : format `HH:mm` strict

---

### `GET /api/schedules` — Lister les programmations

**Description :** Retourne toutes les programmations de l'utilisateur.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": [
    {
      "id": "d1e2f3a4-b5c6-7890-defa-hi4567890123",
      "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "fileName": "rapport_annuel.pdf",
      "frequency": "WEEKLY",
      "scheduledTime": "02:00",
      "dayOfWeek": "MONDAY",
      "timezone": "Europe/Paris",
      "isActive": true,
      "lastRunAt": "2024-01-01T01:00:00Z",
      "nextRunAt": "2024-01-08T01:00:00Z",
      "createdAt": "2024-01-01T12:00:00Z"
    }
  ],
  "totalElements": 1
}
```

---

### `GET /api/schedules/{id}` — Détail d'une programmation

**Description :** Retourne les détails d'une programmation spécifique.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant de la programmation

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "d1e2f3a4-b5c6-7890-defa-hi4567890123",
    "fileId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "fileName": "rapport_annuel.pdf",
    "frequency": "WEEKLY",
    "scheduledTime": "02:00",
    "dayOfWeek": "MONDAY",
    "timezone": "Europe/Paris",
    "isActive": true,
    "lastRunAt": "2024-01-01T01:00:00Z",
    "nextRunAt": "2024-01-08T01:00:00Z",
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-01-01T12:00:00Z"
  }
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Cette programmation appartient à un autre utilisateur |
| `404` | Programmation introuvable |

---

### `PUT /api/schedules/{id}` — Modifier une programmation

**Description :** Met à jour une programmation existante.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Path Params :**
- `id` (UUID)

**Body :**
```json
{
  "frequency": "DAILY",
  "scheduledTime": "03:30",
  "dayOfWeek": null,
  "timezone": "UTC"
}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "d1e2f3a4-b5c6-7890-defa-hi4567890123",
    "frequency": "DAILY",
    "scheduledTime": "03:30",
    "dayOfWeek": null,
    "timezone": "UTC",
    "nextRunAt": "2024-01-02T03:30:00Z",
    "updatedAt": "2024-01-01T13:00:00Z"
  },
  "message": "Programmation mise à jour"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `400` | Validation échouée |
| `401` | Token absent ou invalide |
| `403` | Cette programmation appartient à un autre utilisateur |
| `404` | Programmation introuvable |

---

### `DELETE /api/schedules/{id}` — Supprimer une programmation

**Description :** Supprime définitivement une programmation.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID)

**Réponse succès — `204 No Content`**

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Cette programmation appartient à un autre utilisateur |
| `404` | Programmation introuvable |

---

### `PATCH /api/schedules/{id}/toggle` — Activer / Désactiver

**Description :** Bascule l'état `isActive` d'une programmation.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID)

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "d1e2f3a4-b5c6-7890-defa-hi4567890123",
    "isActive": false,
    "updatedAt": "2024-01-01T14:00:00Z"
  },
  "message": "Programmation désactivée"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Cette programmation appartient à un autre utilisateur |
| `404` | Programmation introuvable |

---

## Notifications

### `GET /api/notifications` — Lister les notifications

**Description :** Retourne la liste paginée des notifications de l'utilisateur.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Query Params :**

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| `page` | int | `0` | Numéro de page |
| `size` | int | `20` | Éléments par page (max 50) |
| `is_read` | boolean | — | Filtre : `true` (lues) ou `false` (non lues) |

**Exemple :** `GET /api/notifications?is_read=false&page=0&size=10`

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "content": [
      {
        "id": "e1f2a3b4-c5d6-7890-efab-ij5678901234",
        "backupId": "b1c2d3e4-f5a6-7890-bcde-fg2345678901",
        "type": "BACKUP_SUCCESS",
        "message": "Sauvegarde du fichier 'rapport_annuel.pdf' réussie (version 4)",
        "isRead": false,
        "createdAt": "2024-01-01T12:01:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "unreadCount": 3
  }
}
```

**Types de notifications :**

| Type | Description |
|------|-------------|
| `BACKUP_SUCCESS` | Sauvegarde terminée avec succès |
| `BACKUP_FAILED` | Sauvegarde échouée |
| `RESTORE_SUCCESS` | Restauration réussie |
| `RESTORE_FAILED` | Restauration échouée |
| `SCHEDULE_ACTIVATED` | Programmation activée |
| `SCHEDULE_DEACTIVATED` | Programmation désactivée |

---

### `PATCH /api/notifications/{id}/read` — Marquer comme lue

**Description :** Marque une notification spécifique comme lue.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Path Params :**
- `id` (UUID) — Identifiant de la notification

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "id": "e1f2a3b4-c5d6-7890-efab-ij5678901234",
    "isRead": true
  },
  "message": "Notification marquée comme lue"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |
| `403` | Cette notification appartient à un autre utilisateur |
| `404` | Notification introuvable |

---

### `PATCH /api/notifications/read-all` — Tout marquer comme lu

**Description :** Marque toutes les notifications de l'utilisateur comme lues.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "updatedCount": 5
  },
  "message": "5 notifications marquées comme lues"
}
```

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |

---

## Dashboard

### `GET /api/dashboard/stats` — Statistiques globales

**Description :** Retourne un résumé des statistiques de l'utilisateur pour le tableau de bord.  
**Authentification requise :** ✅ Oui

**Headers :**
```
Authorization: Bearer {token}
```

**Réponse succès — `200 OK` :**
```json
{
  "data": {
    "totalFiles": 24,
    "totalBackups": 87,
    "usedStorageBytes": 15728640,
    "usedStorageFormatted": "15.0 MB",
    "lastBackupDate": "2024-01-01T12:00:00Z",
    "nextScheduledBackup": "2024-01-08T02:00:00Z",
    "successCount": 83,
    "failedCount": 4,
    "successRate": 95.4,
    "activeSchedules": 3
  }
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `totalFiles` | int | Nombre de fichiers actifs (non supprimés) |
| `totalBackups` | int | Nombre total de sauvegardes |
| `usedStorageBytes` | long | Stockage total utilisé en octets |
| `usedStorageFormatted` | string | Stockage formaté lisible |
| `lastBackupDate` | ISO 8601 | Date de la dernière sauvegarde |
| `nextScheduledBackup` | ISO 8601 | Prochaine sauvegarde planifiée (ou `null`) |
| `successCount` | int | Nombre de sauvegardes réussies |
| `failedCount` | int | Nombre de sauvegardes échouées |
| `successRate` | float | Taux de succès en % |
| `activeSchedules` | int | Nombre de programmations actives |

**Réponses d'erreur :**

| Code | Message |
|------|---------|
| `401` | Token absent ou invalide |

---

## Récapitulatif des endpoints

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| POST | `/api/auth/register` | ❌ | Inscription |
| POST | `/api/auth/login` | ❌ | Connexion → JWT |
| GET | `/api/users/me` | ✅ | Profil connecté |
| PUT | `/api/users/me` | ✅ | Mise à jour profil |
| PUT | `/api/users/me/password` | ✅ | Changement mot de passe |
| POST | `/api/files` | ✅ | Upload fichier |
| GET | `/api/files` | ✅ | Liste fichiers (paginée) |
| GET | `/api/files/{id}` | ✅ | Détail fichier |
| DELETE | `/api/files/{id}` | ✅ | Suppression fichier |
| GET | `/api/files/{id}/download` | ✅ | URL de téléchargement signée |
| POST | `/api/backups/{fileId}` | ✅ | Sauvegarde manuelle |
| GET | `/api/backups` | ✅ | Historique sauvegardes |
| GET | `/api/backups/{id}` | ✅ | Détail sauvegarde |
| POST | `/api/backups/{id}/restore` | ✅ | Restaurer |
| POST | `/api/schedules` | ✅ | Créer programmation |
| GET | `/api/schedules` | ✅ | Lister programmations |
| GET | `/api/schedules/{id}` | ✅ | Détail programmation |
| PUT | `/api/schedules/{id}` | ✅ | Modifier programmation |
| DELETE | `/api/schedules/{id}` | ✅ | Supprimer programmation |
| PATCH | `/api/schedules/{id}/toggle` | ✅ | Activer/désactiver |
| GET | `/api/notifications` | ✅ | Lister notifications |
| PATCH | `/api/notifications/{id}/read` | ✅ | Marquer lue |
| PATCH | `/api/notifications/read-all` | ✅ | Tout marquer lu |
| GET | `/api/dashboard/stats` | ✅ | Statistiques dashboard |
