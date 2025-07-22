package com.android.systemui.utils.coroutines.flow

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.experimental.ExperimentalTypeInference

/**
 * Creates an instance of a _cold_ [Flow] with elements that are sent to a [SendChannel] provided to
 * the builder's [block] of code via [ProducerScope]. It allows elements to be produced by code that
 * is running in a different context or concurrently.
 *
 * The resulting flow is _cold_, which means that [block] is called every time a terminal operator
 * is applied to the resulting flow.
 *
 * This builder ensures thread-safety and context preservation, thus the provided [ProducerScope]
 * can be used from any context, e.g. from a callback-based API. The resulting flow completes as
 * soon as the code in the [block] completes. [awaitClose] should be used to keep the flow running,
 * otherwise the channel will be closed immediately when block completes. [awaitClose] argument is
 * called either when a flow consumer cancels the flow collection or when a callback-based API
 * invokes [SendChannel.close] manually and is typically used to cleanup the resources after the
 * completion, e.g. unregister a callback. Using [awaitClose] is mandatory in order to prevent
 * memory leaks when the flow collection is cancelled, otherwise the callback may keep running even
 * when the flow collector is already completed. To avoid such leaks, this method throws
 * [IllegalStateException] if block returns, but the channel is not closed yet.
 *
 * A [conflated][conflate] channel is used. Use the [buffer] operator on the resulting flow to
 * specify a user-defined value and to control what happens when data is produced faster than
 * consumed, i.e. to control the back-pressure behavior.
 *
 * Adjacent applications of [callbackFlow], [flowOn], [buffer], and [produceIn] are always fused so
 * that only one properly configured channel is used for execution.
 *
 * Example of usage that converts a multi-shot callback API to a flow. For single-shot callbacks use
 * [suspendCancellableCoroutine].
 *
 * ```
 * fun flowFrom(api: CallbackBasedApi): Flow<T> = callbackFlow {
 *     val callback = object : Callback { // Implementation of some callback interface
 *         override fun onNextValue(value: T) {
 *             // To avoid blocking you can configure channel capacity using
 *             // either buffer(Channel.CONFLATED) or buffer(Channel.UNLIMITED) to avoid overfill
 *             trySendBlocking(value)
 *                 .onFailure { throwable ->
 *                     // Downstream has been cancelled or failed, can log here
 *                 }
 *         }
 *         override fun onApiError(cause: Throwable) {
 *             cancel(CancellationException("API Error", cause))
 *         }
 *         override fun onCompleted() = channel.close()
 *     }
 *     api.register(callback)
 *     /*
 *      * Suspends until either 'onCompleted'/'onApiError' from the callback is invoked
 *      * or flow collector is cancelled (e.g. by 'take(1)' or because a collector's coroutine was cancelled).
 *      * In both cases, callback will be properly unregistered.
 *      */
 *     awaitClose { api.unregister(callback) }
 * }
 * ```
 * > The callback `register`/`unregister` methods provided by an external API must be thread-safe,
 * > because `awaitClose` block can be called at any time due to asynchronous nature of
 * > cancellation, even concurrently with the call of the callback.
 *
 * This builder is to be preferred over [callbackFlow], due to the latter's default configuration of
 * using an internal buffer, negatively impacting system health.
 *
 * @see callbackFlow
 */
@OptIn(ExperimentalTypeInference::class)
fun <T> conflatedCallbackFlow(
    @BuilderInference block: suspend ProducerScope<T>.() -> Unit,
): Flow<T> = callbackFlow(block).conflate()