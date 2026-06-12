#!/bin/sh
set -e

# ============================================================
# JVM flags — AOT cache (app.aot) compatibility
#
# Override or add flags at runtime via JVM_OPTS:
#   docker run -e JVM_OPTS="-Xmx512m" ...
#
# JVM applies flags left-to-right; JVM_OPTS comes last,
# so it wins over any flag declared below (last-wins rule).
#
# SAFE to override via JVM_OPTS (AOT cache stays valid):
#   Memory: -Xmx, -XX:MaxRAMPercentage, -XX:InitialRAMPercentage
#   Properties: -Dspring.*, -Dserver.*
#
# BREAKS AOT cache if changed via JVM_OPTS (startup increases ~1-2s,
# app still runs — cache is silently ignored, not a hard failure):
#   GC type: e.g., replacing -XX:+UseSerialGC with -XX:+UseZGC
#   -Xss: thread stack size was baked into the training run
# ============================================================

# AOT flag: runtime default reads the cache; training overrides to write it.
# Set AOT_FLAG=-XX:AOTCacheOutput=app.aot externally to run in training mode.
AOT="${AOT_FLAG:--XX:AOTCache=app.aot}"

# JVM reads cgroup memory limits set by --memory in docker run
CONTAINER="-XX:+UseContainerSupport"

# Initial heap = 50% of container RAM; avoids GC pressure from growing heap
# Max heap = 75%; leaves 25% for non-heap (metaspace, I/O, threads)
# Both are safe to override via JVM_OPTS
MEMORY="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0"

# SerialGC: single-threaded collector, lowest memory footprint
# Good fit for low-traffic self-hosted deployments with small heaps
# BREAKS AOT cache if changed to a different GC
GC="-XX:+UseSerialGC"

# Platform thread stack: 256k vs JVM default 512k
# Safe to reduce because virtual threads use separate heap-allocated stacks
# BREAKS AOT cache if changed
STACK="-Xss256k"

# shellcheck disable=SC2086
exec java $AOT $CONTAINER $MEMORY $GC $STACK ${JVM_OPTS:-} -jar application.jar "$@"
