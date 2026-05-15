# Vision

## Origine

KORE BATCH est issu de plusieurs années de développement batch en production (2018-2021), sur des projets à fort volume de données : traitements de flux entrants, notifications, rapports fonctionnels, valorisation de données métier.

Ces projets partageaient tous la même infrastructure : lancement CLI, gestion des codes retour, partitionnement parallèle, agrégation des résultats, distinction erreurs fonctionnelles / techniques. Ce socle extrait ces patterns communs et les met à disposition de tout projet batch Java.

## Philosophie

**Un seul socle, N projets.** Le module `kore-batch` est publié comme librairie. Chaque projet métier en hérite et n'implémente que sa logique propre.

**Le métier ne gère pas l'infrastructure.** Le projet consommateur ne se préoccupe pas du lancement du job, de la gestion des codes retour, du thread pool ou de l'agrégation : tout est dans le socle.

**Les erreurs fonctionnelles ne tuent pas le batch.** Un item invalide est comptabilisé et loggué, le batch continue. Seules les erreurs techniques font échouer l'exécution.

## Acteurs

| Acteur | Rôle |
|---|---|
| Développeur socle | Maintient et fait évoluer `kore-batch` |
| Développeur métier | Crée un projet qui dépend de `kore-batch` |
| Orchestrateur | Lance le batch (Control-M, cron, CI/CD) et interprète le code retour |
| Ops | Supervise les logs JSON et les métriques Prometheus |

## Glossaire

| Terme | Définition |
|---|---|
| **Socle** | Module `kore-batch` - librairie réutilisable |
| **Sample** | Module `kore-batch-sample` - exemple d'utilisation concret |
| **Job** | Unité d'exécution Spring Batch |
| **Step** | Étape d'un job (partitionné en N workers) |
| **Synthèse** | Rapport d'exécution agrégé (OK, KO, erreurs) |
| **Erreur fonctionnelle** | Erreur métier attendue, ne fait pas échouer le batch |
| **Erreur technique** | Erreur infrastructure, fait échouer le batch |
| **Partition** | Thread de traitement parallèle |
