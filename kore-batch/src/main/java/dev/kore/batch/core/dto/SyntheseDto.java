package dev.kore.batch.core.dto;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SyntheseDto implements ISynthese {

    private long nbOK;
    private long nbKO;
    private long nbDoublons;
    private long nbErreursTechniques;
    private long nbErreursFonctionnelles;

    public void incrementOK()                  { nbOK++; }
    public void incrementKO()                  { nbKO++; }
    public void incrementDoublons()            { nbDoublons++; }
    public void incrementErreurTechnique()     { nbErreursTechniques++; }
    public void incrementErreurFonctionnelle() { nbErreursFonctionnelles++; }

    public void merge(SyntheseDto other) {
        if (other == null) return;
        nbOK                  += other.nbOK;
        nbKO                  += other.nbKO;
        nbDoublons            += other.nbDoublons;
        nbErreursTechniques   += other.nbErreursTechniques;
        nbErreursFonctionnelles += other.nbErreursFonctionnelles;
    }

    @Override public long getTotal()                  { return nbOK + nbKO; }
    @Override public boolean hasTechnicalErrors()     { return nbErreursTechniques > 0; }
    @Override public boolean hasFunctionalErrors()    { return nbErreursFonctionnelles > 0; }
}
