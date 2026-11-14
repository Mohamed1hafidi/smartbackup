# SmartBackup — Guide de Tests

## Table des matières
1. [Vue d'ensemble](#vue-densemble)
2. [Tests Backend](#tests-backend)
3. [Tests Frontend](#tests-frontend)
4. [Tests d'intégration](#tests-dintégration)
5. [Scénarios d'erreur obligatoires](#scénarios-derreur)
6. [Commandes de test](#commandes-de-test)

---

## 1. Vue d'ensemble

| Couche | Framework | Couverture cible |
|---|---|---|
| Backend (services) | JUnit 5 + Mockito | ≥ 70 % |
| Backend (contrôleurs) | Spring Boot Test + MockMvc | Endpoints critiques |
| Backend (intégration) | Spring Boot Test + H2 | Flux complets |
| Frontend (composants) | Vitest + Testing Library | Composants clés |
| E2E | Manuel (checklist) | Flux upload→backup→restore |

> [!NOTE]
> La base de données utilisée en test est **H2 en mémoire** (jamais Supabase). Le profil `test` désactive Flyway ou utilise un schéma H2 compatible.

---

## 2. Tests Backend

### 2.1 Structure des tests

```
backend/src/test/java/com/smartbackup/
├── AuthServiceTest.java                  # Tests unitaires AuthService
├── JwtTokenProviderTest.java             # Tests JWT
├── AuthFlowIntegrationTest.java          # Flux inscription → connexion
├── FileFlowIntegrationTest.java          # Upload → liste → téléchargement → suppression
├── BackupFlowIntegrationTest.java        # Upload → backup → historique
├── RestoreFlowIntegrationTest.java       # Backup → restore + vérification SHA-256
├── ScheduleFlowIntegrationTest.java      # Création planification → exécution → désactivation
├── DashboardNotificationIntegrationTest.java  # Stats dashboard + notifications
└── H2TestDialect.java                    # Dialecte H2 compatible PostgreSQL
```

### 2.2 Configuration test (`application-test.yml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: false   # Flyway désactivé en test, DDL auto géré par Hibernate

app:
  storage:
    max-file-size-mb: 10

jwt:
  secret: test-secret-key-for-junit-minimum-32-characters
  expiration-ms: 3600000
```

### 2.3 Tests unitaires critiques

#### `AuthServiceTest` — Inscription & Connexion
```java
// Cas couverts :
// ✅ Inscription réussie avec BCrypt
// ✅ Refus si email déjà utilisé → EmailAlreadyUsedException
// ✅ Connexion avec bon mot de passe → JWT retourné
// ✅ Connexion avec mauvais mot de passe → BadCredentialsException
// ✅ Changement de mot de passe avec confirmation correcte
// ✅ Refus si ancien mot de passe incorrect
```

#### `JwtTokenProviderTest` — JWT
```java
// Cas couverts :
// ✅ Token généré avec claims corrects (userId, email)
// ✅ Token valide accepté
// ✅ Token expiré rejeté → InvalidTokenException
// ✅ Token falsifié rejeté (signature invalide)
// ✅ Token avec mauvais userId rejeté
```

### 2.4 Tests d'intégration critiques

#### `BackupFlowIntegrationTest`
```
Flux 1 : Upload → Backup Manuel → Vérification métadonnées
Flux 2 : Backup en double → BlockingException (pas de doublons)
Flux 3 : Backup d'un fichier appartenant à un autre user → 403 Forbidden
Flux 4 : Simulation échec R2 → statut FAILED enregistré
Flux 5 : SHA-256 calculé et stocké correctement
```

#### `RestoreFlowIntegrationTest`
```
Flux 1 : Backup → Restore → Comparaison SHA-256 OK
Flux 2 : Restore d'un objet inexistant → ResourceNotFoundException
Flux 3 : Restore avec SHA-256 corrompu → BackupIntegrityException
Flux 4 : Restore d'une sauvegarde appartenant à un autre user → 403
Flux 5 : Restore concurrent → protection contre doublons
```

#### `ScheduleFlowIntegrationTest`
```
Flux 1 : Créer planification DAILY → vérifier next_run_at calculé
Flux 2 : Activer → Désactiver → Vérifier statut
Flux 3 : Exécution planifiée → backup créé → notification générée
Flux 4 : Deux exécutions simultanées → protection par verrou
Flux 5 : Planification avec fichier supprimé → gestion correcte
```

---

## 3. Tests Frontend

### 3.1 Lancer les tests

```bash
cd frontend
npm run test          # Vitest en mode watch
npm run test -- --run # Une seule passe (CI)
```

### 3.2 Composants à tester en priorité

| Composant | Ce qu'on teste |
|---|---|
| `LoginForm` | Validation email/mot de passe, soumission, erreur API |
| `RegisterForm` | Validation, confirmation mot de passe, soumission |
| `FileUploader` | Rejet fichier trop volumineux, type non autorisé, upload réussi |
| `ConfirmDialog` | Ouverture, confirmation, annulation |
| `NotificationList` | Affichage, marquage comme lu |

---

## 4. Tests d'intégration manuels (cycle complet)

### Checklist MVP — À exécuter après chaque déploiement

#### Authentification
- [ ] Inscription avec email valide → JWT reçu
- [ ] Connexion → redirection Dashboard
- [ ] Accès à `/files` sans token → redirection `/login`
- [ ] Token expiré → déconnexion automatique

#### Upload & Fichiers
- [ ] Upload d'un PDF ≤ 50 Mo → fichier visible dans la liste
- [ ] Upload d'un `.exe` → rejeté (type non autorisé)
- [ ] Upload d'un fichier > 50 Mo → rejeté (taille)
- [ ] Téléchargement d'un fichier uploadé → contenu intact

#### Sauvegarde manuelle
- [ ] Clic "Backup Now" → statut SUCCESS dans l'historique
- [ ] SHA-256 visible dans les détails de la sauvegarde
- [ ] Notification "Sauvegarde réussie" apparaît

#### Restauration
- [ ] Sélectionner une version → restaurer → fichier récupéré
- [ ] SHA-256 après restauration = SHA-256 original ✅

#### Planification
- [ ] Créer planification DAILY → visible dans Schedules
- [ ] Désactiver → statut mis à jour
- [ ] Exécution planifiée → nouvelle entrée dans l'historique

#### Sécurité
- [ ] Accéder au fichier d'un autre utilisateur → 403
- [ ] Modifier le profil → vérification mot de passe actuel obligatoire

---

## 5. Scénarios d'erreur obligatoires

| # | Scénario | Comportement attendu |
|---|---|---|
| E-01 | Fichier trop volumineux (> MAX) | 400 Bad Request + message explicite |
| E-02 | Type MIME non autorisé | 400 Bad Request + liste des types autorisés |
| E-03 | Utilisateur non authentifié | 401 Unauthorized |
| E-04 | Accès à la ressource d'un autre user | 403 Forbidden |
| E-05 | Stockage R2 indisponible | 503 + statut FAILED enregistré en DB |
| E-06 | Base de données indisponible | 503 Service Unavailable |
| E-07 | Sauvegarde interrompue | Rollback + statut FAILED cohérent |
| E-08 | Restauration d'objet inexistant en R2 | 404 Resource Not Found |
| E-09 | SHA-256 incorrect lors de la restauration | 422 Unprocessable Entity + log |
| E-10 | Deux sauvegardes simultanées du même fichier | 409 Conflict (protection verrou) |
| E-11 | Scheduler sur serveur en veille (Render) | Cloudflare Worker relance le backend |
| E-12 | Planification avec fichier supprimé | Planification désactivée + notification |

---

## 6. Commandes de test

```bash
# ── Backend ──────────────────────────────────────────────
cd backend

# Tous les tests
./mvnw test

# Un seul test
./mvnw test -Dtest=AuthServiceTest

# Tests avec rapport Surefire
./mvnw test surefire-report:report
# Rapport : target/site/surefire-report.html

# ── Frontend ─────────────────────────────────────────────
cd frontend

# Tous les tests (watch)
npm run test

# Tests une seule fois (CI)
npm run test -- --run

# Couverture
npm run test -- --coverage
```

> [!WARNING]
> Ne jamais exécuter les tests d'intégration contre la base Supabase de production. Utilisez toujours H2 (profil `test`) ou une instance PostgreSQL locale Docker.
