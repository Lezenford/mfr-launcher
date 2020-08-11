package ru.fullrest.mfr.server.model.repository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ru.fullrest.mfr.server.ServerApplication;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.entity.mock.UpdateData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@TestPropertySource("classpath:application.properties")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServerApplication.class)
public class UpdateRepositoryTest {

    private final UpdateData updateData = new UpdateData();

    @Autowired
    private UpdateRepository updateRepository;

    @Test
    public void checkIncrementDownloadCount() {
        Update update = updateData.getUpdates().get(0);
        updateRepository.save(update);
        int downloadCount = update.getDownloadCount();
        int step = 100;
        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < step; i++) {
            futures.add(threadPool.submit(() -> updateRepository.incrementDownloadCount(update.getId())));
        }
        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(updateRepository.findById(update.getId()).get().getDownloadCount(), downloadCount + step);
    }

}