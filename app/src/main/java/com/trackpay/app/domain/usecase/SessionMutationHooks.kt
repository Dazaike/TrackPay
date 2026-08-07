package com.trackpay.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 3 seam for goal allocations / wallet refresh after session writes.
 * Default no-op; Phase 3 binds a real implementation.
 */
interface SessionMutationHooks {
    suspend fun onSessionCompleted(sessionId: String) {}
    suspend fun onSessionMutated(sessionId: String) {}
    suspend fun onSessionDeleted(sessionId: String) {}
}

@Singleton
class NoOpSessionMutationHooks @Inject constructor() : SessionMutationHooks
