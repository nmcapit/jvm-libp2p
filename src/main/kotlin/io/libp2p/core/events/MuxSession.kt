package io.libp2p.core.events

import io.libp2p.core.mux.StreamMuxer

data class MuxSessionInitialized(val session: StreamMuxer.Session)

data class MuxSessionFailed(val exception: Throwable)