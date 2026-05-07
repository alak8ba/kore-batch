package dev.kore.batch.core.dto;

import java.io.Serializable;

public interface ISynthese extends Serializable {

    String SYNTHESE_KEY = "synthese";

    long getTotal();
    long getNbOK();
    long getNbKO();
    long getNbDoublons();
    long getNbErreursTechniques();
    long getNbErreursFonctionnelles();

    boolean hasTechnicalErrors();
    boolean hasFunctionalErrors();
}
