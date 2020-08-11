package ru.fullrest.mfr.api;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePlan {
    private List<String> updates;

    private boolean refreshSchema;

    private boolean refreshApplied;
}
