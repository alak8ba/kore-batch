package fr.kore.batch.core.dto;

public class SyntheseDto implements ISynthese {

    private long nbOK;
    private long nbKO;
    private long nbDoublons;
    private long nbErreursTechniques;
    private long nbErreursFonctionnelles;

    public void incrementOK() {
        nbOK++;
    }

    public void incrementKO() {
        nbKO++;
    }

    public void incrementDoublons() {
        nbDoublons++;
    }

    public void incrementErreurTechnique() {
        nbErreursTechniques++;
    }

    public void incrementErreurFonctionnelle() {
        nbErreursFonctionnelles++;
    }

    public void merge(SyntheseDto other) {
        if (other == null) return;
        this.nbOK += other.nbOK;
        this.nbKO += other.nbKO;
        this.nbDoublons += other.nbDoublons;
        this.nbErreursTechniques += other.nbErreursTechniques;
        this.nbErreursFonctionnelles += other.nbErreursFonctionnelles;
    }

    @Override
    public long getTotal() {
        return nbOK + nbKO;
    }

    @Override
    public long getNbOK() {
        return nbOK;
    }

    @Override
    public long getNbKO() {
        return nbKO;
    }

    @Override
    public long getNbDoublons() {
        return nbDoublons;
    }

    @Override
    public long getNbErreursTechniques() {
        return nbErreursTechniques;
    }

    @Override
    public long getNbErreursFonctionnelles() {
        return nbErreursFonctionnelles;
    }

    @Override
    public boolean hasTechnicalErrors() {
        return nbErreursTechniques > 0;
    }

    @Override
    public boolean hasFunctionalErrors() {
        return nbErreursFonctionnelles > 0;
    }

    @Override
    public String toString() {
        return String.format(
            "SyntheseDto{total=%d, OK=%d, KO=%d, doublons=%d, errTech=%d, errFonc=%d}",
            getTotal(), nbOK, nbKO, nbDoublons, nbErreursTechniques, nbErreursFonctionnelles
        );
    }
}
