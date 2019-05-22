package ru.fullrest.mfr.common;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePlan {
    private List<String> updates;

    private boolean refreshSchema;

    private boolean refreshApplied;
}
