package ru.fullrest.mfr.common;

import lombok.Data;

@Data
public class Version {
    private String version;

    private boolean clientVersionIsDefined;

    private boolean needUpdate;

    private UpdatePlan updatePlan;
}