/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.suites

/**
 * @author Denys Bondarenko
 */
class PerfStatsCollector internal constructor(private val clock: Clock) {
  private val metadata: MutableMap<Class<*>?, Any?> = HashMap<Class<*>?, Any?>()
  private val metricMap: MutableMap<MetricKey?, Metric?> =
    HashMap<MetricKey?, Metric?>()
  private var enabled = true

  constructor() : this({ System.nanoTime() })

  /** If not enabled, don't bother retaining perf stats, saving some memory and CPU cycles.  */
  fun setEnabled(isEnabled: Boolean) {
    this.enabled = isEnabled
  }

  fun startEvent(eventName: String?): Event {
    return Event(eventName)
  }

  fun <T, E : java.lang.Exception?> measure(
    eventName: String?,
    supplier: ThrowingSupplier<T?, E?>
  ): T? {
    var success = true
    val event = startEvent(eventName)
    try {
      return supplier.get()
    } catch (e: java.lang.Exception) {
      success = false
      throw e
    } finally {
      event.finished(success)
    }
  }

  fun incrementCount(eventName: String?) {
    synchronized(this@PerfStatsCollector) {
      val key = MetricKey(eventName, true)
      var metric = metricMap[key]
      if (metric == null) {
        metricMap.put(
          key,
          Metric(key.name, key.success).also { metric = it })
      }
      metric?.incrementCount()
    }
  }

  /** Supplier that throws an exception.  */ // @FunctionalInterface -- not available on Android yet...
  interface ThrowingSupplier<T, F : java.lang.Exception?> {
    fun get(): T?
  }

  fun <E : java.lang.Exception?> measure(eventName: String?, runnable: ThrowingRunnable<E?>) {
    var success = true
    val event = startEvent(eventName)
    try {
      runnable.run()
    } catch (e: java.lang.Exception) {
      success = false
      throw e
    } finally {
      event.finished(success)
    }
  }

  /** Runnable that throws an exception.  */ // @FunctionalInterface -- not available on Android yet...
  interface ThrowingRunnable<F : java.lang.Exception?> {
    fun run()
  }

  @get:Synchronized
  val metrics: MutableCollection<Metric>
    get() = java.util.ArrayList<Metric>(metricMap.values)

  @Synchronized
  fun <T> putMetadata(metadataClass: Class<T?>?, metadata: T?) {
    if (!enabled) {
      return
    }

    this.metadata.put(metadataClass, metadata)
  }

  @Synchronized
  fun getMetadata(): Metadata {
    return Metadata(metadata)
  }

  fun reset() {
    metadata.clear()
    metricMap.clear()
  }

  /** Event for perf stats collection.  */
  inner class Event internal constructor(private val name: String?) {
    private val startTimeNs: Long = clock.nanoTime()

    @JvmOverloads
    fun finished(success: Boolean = true) {
      if (!enabled) {
        return
      }

      synchronized(this@PerfStatsCollector) {
        val key = MetricKey(name, success)
        var metric = metricMap[key]
        if (metric == null) {
          metricMap.put(
            key,
            Metric(key.name, key.success).also { metric = it })
        }
        metric?.record(clock.nanoTime() - startTimeNs)
      }
    }
  }

  /** Metric key for perf stats collection.  */
  private class MetricKey(val name: String?, val success: Boolean) {
    override fun equals(o: Any?): Boolean {
      if (this === o) {
        return true
      }
      if (o !is MetricKey) {
        return false
      }

      val metricKey = o

      if (success != metricKey.success) {
        return false
      }
      return if (name != null) (name == metricKey.name) else metricKey.name == null
    }

    override fun hashCode(): Int {
      var result = if (name != null) name.hashCode() else 0
      result = 31 * result + (if (success) 1 else 0)
      return result
    }
  }

  companion object {
    val instance: PerfStatsCollector = PerfStatsCollector()
  }
}