package mx.loperstack.tokkiphealth.analyzer.core.storage

import mx.loperstack.tokkiphealth.analyzer.core.content.ContentItem
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.VERBOSE
import mx.loperstack.tokkiphealth.common.debug.logging.Logging.Priority.WARN
import mx.loperstack.tokkiphealth.common.debug.logging.asLog
import mx.loperstack.tokkiphealth.common.debug.logging.log
import mx.loperstack.tokkiphealth.common.debug.logging.logTag
import mx.loperstack.tokkiphealth.common.files.APath
import mx.loperstack.tokkiphealth.common.files.APathGateway
import mx.loperstack.tokkiphealth.common.files.APathLookup
import mx.loperstack.tokkiphealth.common.files.FileType
import mx.loperstack.tokkiphealth.common.files.GatewaySwitch
import mx.loperstack.tokkiphealth.common.files.ReadException
import mx.loperstack.tokkiphealth.common.files.Segments
import mx.loperstack.tokkiphealth.common.files.du
import mx.loperstack.tokkiphealth.common.files.walk
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import java.util.LinkedList


fun Collection<ContentItem>.toNestedContent(): Collection<ContentItem> {
    val workList = this.sortedBy { it.path.segments.size }.reversed().toMutableList()
    val topLevelIndices = mutableListOf<Int>()

    // Normalize root segments: [""] and emptyList() both represent root
    fun Segments.normalized(): Segments = if (isEmpty() || singleOrNull() == "") emptyList() else this

    // Pre-build index for O(1) parent lookup instead of O(n) indexOfFirst
    val segmentToIndex = HashMap<Segments, Int>(workList.size)
    workList.forEachIndexed { index, item ->
        segmentToIndex[item.path.segments.normalized()] = index
    }

    // Collect child indices per parent to avoid repeated set copies via .plus()
    val childIndices = arrayOfNulls<MutableList<Int>>(workList.size)

    for ((idx, item) in workList.withIndex()) {
        val childSegs = item.path.segments
        if (childSegs.isEmpty() || childSegs.singleOrNull() == "") {
            topLevelIndices.add(idx)
            continue
        }

        val parentSegs = childSegs.subList(0, childSegs.size - 1).normalized()
        val parentIndex = segmentToIndex[parentSegs]

        if (parentIndex != null) {
            val list = childIndices[parentIndex]
                ?: mutableListOf<Int>().also { childIndices[parentIndex] = it }
            list.add(idx)
        } else {
            topLevelIndices.add(idx)
        }
    }

    // Freeze: merge children into parents (deepest-first, so children are already updated)
    for (i in workList.indices) {
        val indices = childIndices[i] ?: continue
        workList[i] = workList[i].copy(children = workList[i].children + indices.map { workList[it] })
    }

    return topLevelIndices.map { workList[it] }
}

fun Collection<ContentItem>.toFlatContent(): Collection<ContentItem> =
    this.map { it.toFlatContent() }.flatten()

fun ContentItem.toFlatContent(): Collection<ContentItem> {
    val result = mutableListOf<ContentItem>()

    result.add(this.copy(children = emptySet()))

    children
        .map { it.toFlatContent() }
        .flatten()
        .let { result.addAll(it) }

    return result
}


fun Collection<ContentItem>.findContent(filter: (ContentItem) -> Boolean): ContentItem? {
    val queue = LinkedList(this)

    while (!queue.isEmpty()) {
        val item = queue.removeFirst()
        if (filter(item)) return item
        queue.addAll(item.children)
    }

    return null
}


suspend fun APath.walkContentItem(
    gatewaySwitch: GatewaySwitch,
    maxItems: Int = Int.MAX_VALUE,
    followSymlinks: Boolean = false,
    onItem: (suspend (APathLookup<APath>) -> Unit)? = null,
): ContentItem {
    log(TAG, VERBOSE) { "Walking content items for $this" }

    // What ever `this` is , the gatewaySwitch should make sure we end up with something usable
    val lookup = gatewaySwitch.lookup(this, type = GatewaySwitch.Type.AUTO)

    return if (lookup.fileType == FileType.DIRECTORY) {
        val start = System.currentTimeMillis()

        val options = APathGateway.WalkOptions<APath, APathLookup<APath>>(followSymlinks = followSymlinks)
        val children = try {
            lookup.walk(gatewaySwitch, options)
                .onEach { onItem?.invoke(it) }
                .map { ContentItem.fromLookup(it) }
                .let { flow -> if (maxItems < Int.MAX_VALUE) flow.take(maxItems + 1) else flow }
                .toList()
        } catch (e: ReadException) {
            log(TAG, WARN) { "Failed to walk $this: ${e.asLog()}" }
            emptyList()
        }

        val elapsed = System.currentTimeMillis() - start
        log(TAG) { "Walked $this: ${children.size} items in ${elapsed}ms (limit=$maxItems)" }

        if (children.size > maxItems) {
            log(TAG, WARN) { "Walk item limit ($maxItems) exceeded for $this, falling back to du" }
            return this.sizeContentItem(gatewaySwitch)
        }

        children.plus(ContentItem.fromLookup(lookup)).toNestedContent().single()
    } else {
        ContentItem.fromLookup(lookup)
    }
}

suspend fun APath.sizeContentItem(gatewaySwitch: GatewaySwitch): ContentItem {
    log(TAG, VERBOSE) { "Sizing content items for $this" }

    val lookup = gatewaySwitch.lookup(this, type = GatewaySwitch.Type.AUTO)

    val extraSize = if (lookup.fileType == FileType.DIRECTORY) {
        try {
            lookup.du(gatewaySwitch)
        } catch (e: ReadException) {
            log(TAG, WARN) { "Failed to du $this: ${e.asLog()}" }
            0L
        }
    } else {
        0L
    }
    return ContentItem.fromInaccessible(this, lookup.size + extraSize)
}

private val TAG = logTag("Analyzer", "Storage", "Scanner", "Extensions")
