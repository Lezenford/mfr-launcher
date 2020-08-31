package ru.fullrest.mfr.api;

import lombok.Data;

@Data
public class Version {
    private String version;

    private boolean clientVersionIsDefined;

    private boolean needUpdate;

    private UpdatePlan updatePlan;
}