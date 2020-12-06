package ru.fullrest.mfr.server.configuration;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class GitConfiguration {
    private final static String GIT_FOLDER = ".git";

    @Value("${local.ssh-key}")
    private String sshKeyPath;

    @Value("${git.repository.local}")
    private String repositoryLink;

    @Bean
    public SshSessionFactory sshSessionFactory() {
        return new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.removeAllIdentity();
                defaultJSch.addIdentity(sshKeyPath);
                return defaultJSch;
            }
        };
    }

    @Bean
    public TransportConfigCallback transportConfigCallback(SshSessionFactory sshSessionFactory) {
        return transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        };
    }

    @Bean
    public Repository repository() throws IOException {
        return FileRepositoryBuilder.create(new File(repositoryLink + File.separator + GIT_FOLDER));
    }

    @Bean
    public Git git(Repository repository) {
        return new Git(repository);
    }
}
