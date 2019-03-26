package ru.fullrest.mfr.server.model.entity.mock;

import lombok.Getter;
import ru.fullrest.mfr.server.model.entity.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
public class UpdateData {
    private final int maxCount = 15;
    private final List<Update> updates = new ArrayList<>();

    public UpdateData() {
        for (int i = 0; i < maxCount; i++) {
            Update update = new Update();
            update.setVersion("1." + i);
            update.setPlatform("original");
            update.setUploadDate(new Date());
            update.setAppliedUpdate(i > maxCount / 3);
            update.setSchemaUpdate(i > maxCount / 2);
            update.setPath(String.format("testUpdate%s.zip", i));
            updates.add(update);
        }
    }
}
