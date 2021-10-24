package com.lezenford.mfr.server.configuration

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.util.FS
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GitConfiguration(
    private val settingProperties: ServerSettingProperties
) {

    @Bean
    fun sshSessionFactory(): SshSessionFactory = object : JschConfigSessionFactory() {
        override fun configure(hc: OpenSshConfig.Host, session: Session) {
            session.setConfig("StrictHostKeyChecking", "no")
        }

        override fun createDefaultJSch(fs: FS): JSch =
            super.createDefaultJSch(fs).also {
                it.removeAllIdentity()
                it.addIdentity(settingProperties.build.key)
            }

    }

    @Bean
    fun transportConfigCallback(sshSessionFactory: SshSessionFactory): TransportConfigCallback =
        TransportConfigCallback { transport -> (transport as SshTransport).sshSessionFactory = sshSessionFactory }
}