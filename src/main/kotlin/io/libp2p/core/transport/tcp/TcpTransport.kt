package io.libp2p.core.transport.tcp

import io.libp2p.core.Connection
import io.libp2p.core.Libp2pException
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.core.multiformats.Protocol.DNSADDR
import io.libp2p.core.multiformats.Protocol.IP4
import io.libp2p.core.multiformats.Protocol.IP6
import io.libp2p.core.multiformats.Protocol.TCP
import io.libp2p.core.transport.ConnectionUpgrader
import io.libp2p.core.transport.Transport
import io.libp2p.core.types.toCompletableFuture
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

/**
 * The TCP transport can establish libp2p connections via TCP endpoints.
 *
 * Given that TCP by itself is not authenticated, encrypted, nor multiplexed, this transport uses the upgrader to
 * shim those capabilities via dynamic negotiation.
 */
class TcpTransport(val upgrader: ConnectionUpgrader) : Transport {
    private var server: ServerBootstrap? = ServerBootstrap()
    private var client: Bootstrap = Bootstrap()

    // Initializes the server and client fields, preparing them to establish outbound connections (client)
    // and to accept inbound connections (server).
    override fun initialize() {
    }

    // Checks if this transport can handle this multiaddr. It should return true for multiaddrs containing `tcp` atoms.
    override fun handles(addr: Multiaddr): Boolean {
        return addr.components
            .any { pair -> pair.first == TCP }
    }

    // Closes this transport entirely, aborting all ongoing connections and shutting down any listeners.
    override fun close(): CompletableFuture<Void> {
        TODO("not implemented")
    }

    override fun listen(addr: Multiaddr): CompletableFuture<Void> {
        TODO("not implemented")
    }

    override fun unlisten(addr: Multiaddr): CompletableFuture<Void> {
        TODO("not implemented")
    }

    override fun dial(addr: Multiaddr): CompletableFuture<Connection> =
        client.connect(fromMultiaddr(addr)).toCompletableFuture()
            .thenCompose { upgrader.establishSecureChannel(it).toCompletableFuture() }
            .thenCompose { upgrader.establishMuxer(it).toCompletableFuture() }
            .thenApply { Connection(it) }

    private fun fromMultiaddr(addr: Multiaddr): InetSocketAddress {
        val host = addr.getStringComponents().find { p -> p.first in arrayOf(IP4, IP6, DNSADDR) }
            ?.second ?: throw Libp2pException("Missing IP4/IP6/DNSADDR in multiaddress $addr")
        val port = addr.getStringComponents().find { p -> p.first == TCP }
            ?.second ?: throw Libp2pException("Missing TCP in multiaddress $addr")
        return InetSocketAddress.createUnresolved(host, port.toInt())
    }
}