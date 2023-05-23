package com.lezenford.mfr.launcher.netty

import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import io.netty.resolver.AddressResolver
import io.netty.resolver.AddressResolverGroup
import io.netty.resolver.DefaultNameResolver
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.Promise
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

@Component
class CustomAddressResolverGroup(
    private val properties: ApplicationProperties
) : AddressResolverGroup<InetSocketAddress>() {
    override fun newResolver(executor: EventExecutor): AddressResolver<InetSocketAddress> {
        return CustomNameResolver(executor).asAddressResolver()
    }

    inner class CustomNameResolver(executor: EventExecutor) : DefaultNameResolver(executor) {
        override fun doResolveAll(inetHost: String, promise: Promise<MutableList<InetAddress>>) {
            try {
                promise.setSuccess(mutableListOf(inetHost.resole()))
            } catch (e: Exception) {
                promise.setFailure(e)
            }
        }

        override fun doResolve(inetHost: String, promise: Promise<InetAddress>) {
            try {
                promise.setSuccess(inetHost.resole())
            } catch (e: Exception) {
                promise.setFailure(e)
            }
        }

        private fun String.resole(): InetAddress {
            return try {
                InetAddress.getByName(this)
            } catch (e: UnknownHostException) {
                if (this == properties.server.http.dnsName) {
                    return InetAddress.getByName(properties.server.http.ip)
                }
                if (this == properties.server.tcp.dnsName){
                    return InetAddress.getByName(properties.server.tcp.ip)
                }
                throw e
            }
        }
    }
}