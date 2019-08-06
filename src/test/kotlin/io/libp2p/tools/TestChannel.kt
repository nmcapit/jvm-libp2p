package io.libp2p.tools

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.libp2p.core.types.lazyVar
import io.netty.channel.ChannelHandler
import io.netty.channel.embedded.EmbeddedChannel
import org.apache.logging.log4j.LogManager
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private val threadFactory = ThreadFactoryBuilder().setDaemon(true).setNameFormat("TestChannel-interconnect-executor-%d").build()

class TestChannel(vararg handlers: ChannelHandler?) : EmbeddedChannel(*handlers) {
    var link: TestChannel? = null
    var executor: Executor by lazyVar {
        Executors.newSingleThreadExecutor(threadFactory)
    }

    @Synchronized
    fun connect(other: TestChannel) {
        link = other
        outboundMessages().forEach(this::send)
    }

    @Synchronized
    override fun handleOutboundMessage(msg: Any?) {
        super.handleOutboundMessage(msg)
        if (link != null) {
            send(msg!!)
        }
    }

    fun send(msg: Any) {
        link!!.executor.execute {
            logger.debug("---- link!!.writeInbound")
            link!!.writeInbound(msg)
        }
    }

    companion object {
        fun interConnect(ch1: TestChannel, ch2: TestChannel) : TestConnection {
            ch1.connect(ch2)
            ch2.connect(ch1)
            return TestConnection(ch1, ch2)
        }

        private val logger = LogManager.getLogger(TestChannel::class.java)
    }

    class TestConnection(val ch1: TestChannel, val ch2: TestChannel) {
        fun disconnect() {
            ch1.close()
            ch2.close()
        }
    }
}