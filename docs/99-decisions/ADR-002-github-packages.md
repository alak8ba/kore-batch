# ADR-002 — GitHub Packages à la place de Nexus

## Statut

Accepté

## Contexte

Le socle `kore-batch` doit être publié dans un registry Maven pour être consommé par les projets métier. L'ancien stack utilisait un Nexus interne (inaccessible hors réseau SNCF).

## Décision

Utiliser **GitHub Packages** comme registry Maven.

## Raisons

- Intégré nativement à GitHub Actions — pas de configuration externe
- Authentification via `GITHUB_TOKEN` automatique en CI
- Cohérent avec l'écosystème KORE (tout sur GitHub)
- Gratuit pour les projets publics
- Pas de serveur à maintenir

## Configuration

Publication dans le `pom.xml` parent :
```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/alak8ba/kore-batch</url>
    </repository>
</distributionManagement>
```

Consommation dans `~/.m2/settings.xml` (dev local) ou via `settings.xml` généré en CI.

## Conséquences

- Les projets consommateurs doivent avoir un token GitHub avec accès `read:packages`
- La visibilité du package suit la visibilité du repo GitHub
