package ru.fullrest.mfr.server.service.updater;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.api.GameUpdate;
import ru.fullrest.mfr.api.MoveFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
class GitService {
    private final Git git;
    private final Repository repository;
    private final TransportConfigCallback transportConfigCallback;

    @Value("${git.repository.remote}")
    private String RepositoryRemotePath;

    @Value("${git.repository.local}")
    private String repositoryLocalPath;

    @Value("${git.repository.branch}")
    private String branchName;

    @Value("${git.repository.tag-suffix}")
    private String tagSuffix;

    public boolean repositoryExist() {
        return repository.getDirectory().exists();
    }

    public void cloneRepository() throws GitAPIException {
        Git.cloneRepository()
                .setURI(RepositoryRemotePath)
                .setDirectory(new File(repositoryLocalPath))
                .setBranchesToClone(Collections.singletonList("refs/heads/" + branchName))
                .setBranch("refs/heads/" + branchName)
                .setTransportConfigCallback(transportConfigCallback)
                .call();
    }

    public void updateRepository() throws GitAPIException {
        git.checkout()
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setName(branchName)
                .call();
        git.tagList().call().forEach(tag -> {
            try {
                git.tagDelete().setTags(tag.getName()).call();
            } catch (GitAPIException e) {
                log.error("Can't delete Tag");
            }
        });
        git.pull()
                .setTransportConfigCallback(transportConfigCallback)
                .call();
    }

    public GameUpdate getDiffForPatch() throws GitAPIException, IOException {
        String fullBranchName = "refs/heads/" + branchName;
        Ref branch = git.branchList()
                .setListMode(ListBranchCommand.ListMode.ALL)
                .call()
                .stream()
                .filter(it -> it.getName().equals(fullBranchName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find branch " + fullBranchName));
        RevWalk revCommits = new RevWalk(repository);
        RevCommit lastCommit = revCommits.parseCommit(branch.getObjectId());

        RevCommit prevCommit = git.tagList()
                .call()
                .stream()
                .filter(tag -> tag.getName().endsWith(tagSuffix))
                .map(it -> {
                    try {
                        return revCommits.parseCommit(it.getObjectId());
                    } catch (IOException e) {
                        log.error(e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(RevCommit::getCommitTime))
                .orElseThrow(() -> new IllegalStateException("Can't find commit with tag for create patch"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DiffFormatter diffFormatter = new DiffFormatter(outputStream);
        diffFormatter.setRepository(repository);

        List<DiffEntry> scan = diffFormatter.scan(prevCommit, lastCommit);

        GameUpdate gameUpdate = new GameUpdate("", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "");

        scan.forEach(it -> {
            if (!it.getNewPath().equals("mge3/MGE.ini") && !it.getNewPath().equals("Morrowind.ini")
                    && !it.getNewPath().equals(".gitignore")) {
                switch (it.getChangeType()) {
                    case ADD:
                    case MODIFY:
                    case COPY: {
                        gameUpdate.getAddFiles().add(it.getNewPath());
                        break;
                    }
                    case DELETE: {
                        gameUpdate.getRemoveFiles().add(it.getOldPath());
                        break;
                    }
                    case RENAME: {
                        gameUpdate.getMoveFiles().add(new MoveFile(it.getOldPath(), it.getNewPath()));
                        break;
                    }
                }
            }
        });

        return gameUpdate;
    }

    public void setTag(String version) throws GitAPIException {
        git.tag().setName(version + tagSuffix).call();
        git.push().setTransportConfigCallback(transportConfigCallback).setPushTags().call();
    }
}
