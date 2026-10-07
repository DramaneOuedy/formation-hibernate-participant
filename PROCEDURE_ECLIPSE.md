# Formation Hibernate & JPA – Procédure Eclipse

Procédure pour récupérer, lancer et rendre le projet de la formation avec **Eclipse**.
À faire **avant le jour 1** (étapes 1 à 6), puis **à la fin de chaque module** (étape 7).

Dépôt de départ : https://github.com/DramaneOuedy/formation-hibernate-participant

---

## 1. Prérequis

| Outil | Version | Vérification |
| --- | --- | --- |
| JDK | 17 ou plus récent | `java -version` |
| Eclipse | *Eclipse IDE for Enterprise Java and Web Developers* ou *Spring Tools for Eclipse* | contient déjà Git (EGit) et Maven (m2e) |
| MySQL | 8 | service MySQL démarré, port 3306 |
| Compte GitHub | – | https://github.com |

> Spring Tools for Eclipse est recommandé : il ajoute le lanceur **Spring Boot App**, plus pratique pour choisir le profil d'un TP.

---

## 2. Créer la base MySQL (une seule fois)

Dans MySQL Workbench, ou en ligne de commande avec `mysql -u root -p` :

```sql
CREATE DATABASE IF NOT EXISTS formation_hibernate;
CREATE USER IF NOT EXISTS 'formation'@'localhost' IDENTIFIED BY 'formation';
GRANT ALL PRIVILEGES ON formation_hibernate.* TO 'formation'@'localhost';
```

---

## 3. Faire son fork sur GitHub

1. Ouvrir https://github.com/DramaneOuedy/formation-hibernate-participant
2. Cliquer sur **Fork** (en haut à droite), puis sur **Create fork**.
3. Noter l'adresse de **votre** copie : `https://github.com/VOTRE-PSEUDO/formation-hibernate-participant`

---

## 4. Importer le projet dans Eclipse

1. **File > Import… > Git > Projects from Git (with smart import)**, puis **Next**.
2. Choisir **Clone URI**, puis **Next**.
3. Coller l'adresse de **votre fork** dans *URI*. Les autres champs se remplissent seuls. Cliquer sur **Next**.
4. Laisser la branche **main** cochée, puis **Next**.
5. Choisir le dossier de destination, puis **Next** et **Finish**.
6. Attendre la fin du téléchargement des dépendances Maven (barre de progression en bas à droite).

### Vérifier Java 17

- **Window > Preferences > Java > Installed JREs** doit lister un JDK 17. Sinon, cliquer sur **Add…** et indiquer le dossier d'installation du JDK.
- Si le projet affiche des erreurs rouges : clic droit sur le projet, **Maven > Update Project…**, puis **OK**.

---

## 5. Vérifier son poste

### Lancer les tests (sans MySQL)

Clic droit sur le projet, puis **Run As > Maven test**.

Résultat attendu dans la console : `BUILD SUCCESS`.

### Lancer l'application (avec MySQL)

Clic droit sur `src/main/java/com/formation/gestion/GestionApplication.java`, puis **Run As > Spring Boot App** (ou **Java Application**).

Résultat attendu : l'application démarre, se connecte à MySQL (ligne `HikariPool-1 - Start completed`), puis s'arrête sans erreur.

---

## 6. Lancer un TP

Chaque TP est un `CommandLineRunner` activé par un **profil Spring** : `tp1`, `tp2` … `tp8`, `projet`.

### Avec Spring Tools (lanceur *Spring Boot App*)

1. **Run > Run Configurations…**
2. Double-cliquer sur **Spring Boot App** pour créer une configuration.
3. *Name* : `TP1` ; *Project* : votre projet ; *Main type* : `com.formation.gestion.GestionApplication`.
4. Champ **Profile** : `tp1`.
5. Cliquer sur **Apply**, puis **Run**.

### Sans Spring Tools (lanceur *Java Application*)

1. **Run > Run Configurations…**
2. Double-cliquer sur **Java Application**.
3. *Name* : `TP1` ; *Main class* : `com.formation.gestion.GestionApplication`.
4. Onglet **Arguments**, champ *Program arguments* : `--spring.profiles.active=tp1`
5. Cliquer sur **Apply**, puis **Run**.

### Créer les autres TP

Dans *Run Configurations*, clic droit sur `TP1`, puis **Duplicate**. Renommer en `TP2` et remplacer `tp1` par `tp2`. Répéter jusqu'à `projet`.

Ensuite, chaque TP se relance en un clic avec la flèche du bouton ▶ de la barre d'outils.

> La base est recréée à chaque lancement : chaque TP repart de données propres.

---

## 7. Envoyer son travail (fin de chaque module)

1. Clic droit sur le projet, puis **Team > Commit…**. La vue **Git Staging** s'ouvre.
2. Glisser les fichiers modifiés de **Unstaged Changes** vers **Staged Changes** (ou cliquer sur **++** pour tout ajouter).
3. *Commit Message* : par exemple `Module 3 : relations`.
4. Cliquer sur **Commit and Push**, puis **Next** et **Finish**.

### Premier push : identifiants GitHub

GitHub **refuse le mot de passe du compte**. Il faut un **jeton d'accès personnel** :

1. Sur GitHub : **Settings > Developer settings > Personal access tokens > Tokens (classic) > Generate new token (classic)**.
2. *Note* : `Eclipse formation` ; *Expiration* : 30 jours ; cocher **repo**.
3. Cliquer sur **Generate token**, puis **copier le jeton** (il ne s'affiche qu'une fois).
4. Dans la fenêtre de connexion d'Eclipse :
   - *User* : votre pseudo GitHub ;
   - *Password* : le jeton ;
   - cocher **Store in Secure Store**.

Vérification : sur la page GitHub de votre fork, le commit apparaît dans l'historique.

---

## 8. En cas de problème

| Symptôme | Cause probable | Solution |
| --- | --- | --- |
| `Access denied for user 'formation'` | Base ou compte MySQL absent | Refaire l'étape 2 |
| `Communications link failure` | MySQL arrêté | Démarrer le service MySQL (`services.msc`) |
| Erreurs rouges partout après l'import | Dépendances non chargées ou mauvais JDK | **Maven > Update Project…** et vérifier Java 17 |
| `Unsupported class file major version` | Eclipse utilise un JDK trop ancien | Sélectionner le JDK 17 dans *Installed JREs* et dans la configuration de lancement (onglet *JRE*) |
| Le TP ne fait rien | Profil absent ou mal écrit | Vérifier `tp1` (Spring Boot App) ou `--spring.profiles.active=tp1` (Java Application) |
| `Authentication failed` au push | Mot de passe du compte utilisé | Utiliser un jeton d'accès (étape 7) |
| Accents mal affichés dans la console | Encodage | **Window > Preferences > General > Workspace** : *Text file encoding* = **UTF-8** |

---

*Formateur : OUEDRAOGO Dramane – Formation Hibernate & JPA avec Spring Boot 3.4.3*
