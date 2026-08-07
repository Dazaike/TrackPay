package com.trackpay.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seam for goal allocations / wallet refresh after session writes.
 */
interface SessionMutationHooks {
    suspend fun onSessionCompleted(sessionId: String) {}
    suspend fun onSessionMutated(sessionId: String) {}
    suspend fun onSessionDeleted(sessionId: String) {}
}

@Singleton
class NoOpSessionMutationHooks @Inject constructor() : SessionMutationHooks

/**
 * Phase 3: recompute goal allocations on complete/edit; clear on delete.
 */
@Singleton
class AllocationSessionMutationHooks @Inject constructor(
    private val allocateSession: AllocateSessionUseCase,
    private val removeSessionAllocations: RemoveSessionAllocationsUseCase,
) : SessionMutationHooks {
    override suspend fun onSessionCompleted(sessionId: String) {
        allocateSession(sessionId)
    }

    override suspend fun onSessionMutated(sessionId: String) {
        allocateSession(sessionId)
    }

    override suspend fun onSessionDeleted(sessionId: String) {
        removeSessionAllocations(sessionId)
    }
}
