#!/usr/bin/env bash
set -euo pipefail

profile="${PJB_JVM_PROFILE:-balanced}"

autodetect_file_value() {
  local path="$1"
  if [ -r "$path" ]; then
    cat "$path"
  fi
}

detect_memory_limit_bytes() {
  local v2
  v2="$(autodetect_file_value /sys/fs/cgroup/memory.max || true)"
  if [ -n "$v2" ] && [ "$v2" != "max" ]; then
    echo "$v2"
    return
  fi
  local v1
  v1="$(autodetect_file_value /sys/fs/cgroup/memory/memory.limit_in_bytes || true)"
  if [ -n "$v1" ] && [ "$v1" -gt 0 ] 2>/dev/null && [ "$v1" -lt 9223372036854771712 ] 2>/dev/null; then
    echo "$v1"
    return
  fi
  echo 0
}

detect_cpu_limit() {
  if [ -n "${PJB_JVM_ACTIVE_PROCESSOR_COUNT:-}" ]; then
    echo "${PJB_JVM_ACTIVE_PROCESSOR_COUNT}"
    return
  fi
  local quota_line quota period cpus
  quota_line="$(autodetect_file_value /sys/fs/cgroup/cpu.max || true)"
  if [ -n "$quota_line" ]; then
    quota="${quota_line%% *}"
    period="${quota_line##* }"
    if [ "$quota" != "max" ] && [ -n "$period" ] && [ "$period" -gt 0 ] 2>/dev/null; then
      cpus=$(( (quota + period - 1) / period ))
      if [ "$cpus" -gt 0 ] 2>/dev/null; then
        echo "$cpus"
        return
      fi
    fi
  fi
  quota="$(autodetect_file_value /sys/fs/cgroup/cpu/cpu.cfs_quota_us || true)"
  period="$(autodetect_file_value /sys/fs/cgroup/cpu/cpu.cfs_period_us || true)"
  if [ -n "$quota" ] && [ -n "$period" ] && [ "$quota" -gt 0 ] 2>/dev/null && [ "$period" -gt 0 ] 2>/dev/null; then
    cpus=$(( (quota + period - 1) / period ))
    if [ "$cpus" -gt 0 ] 2>/dev/null; then
      echo "$cpus"
      return
    fi
  fi
  nproc
}

resolve_native_reserve_percentage() {
  if [ -n "${PJB_JVM_NATIVE_RESERVE_PERCENTAGE:-}" ]; then
    echo "${PJB_JVM_NATIVE_RESERVE_PERCENTAGE}"
    return
  fi
  local limit_bytes
  limit_bytes="$(detect_memory_limit_bytes)"
  if [ "$limit_bytes" -le 0 ] 2>/dev/null; then
    echo 28
  elif [ "$limit_bytes" -le 1073741824 ] 2>/dev/null; then
    echo 34
  elif [ "$limit_bytes" -le 2147483648 ] 2>/dev/null; then
    echo 30
  elif [ "$limit_bytes" -le 4294967296 ] 2>/dev/null; then
    echo 26
  else
    echo 24
  fi
}

resolve_max_ram_percentage() {
  if [ -n "${PJB_JVM_MAX_RAM_PERCENTAGE:-}" ]; then
    echo "${PJB_JVM_MAX_RAM_PERCENTAGE}"
    return
  fi
  local reserve
  reserve="$(resolve_native_reserve_percentage)"
  local percentage=$((100 - reserve))
  if [ "$percentage" -lt 50 ] 2>/dev/null; then
    percentage=50
  fi
  if [ "$percentage" -gt 72 ] 2>/dev/null; then
    percentage=72
  fi
  echo "$percentage"
}

resolve_initial_ram_percentage() {
  if [ -n "${PJB_JVM_INITIAL_RAM_PERCENTAGE:-}" ]; then
    echo "${PJB_JVM_INITIAL_RAM_PERCENTAGE}"
    return
  fi
  local limit_bytes
  limit_bytes="$(detect_memory_limit_bytes)"
  if [ "$limit_bytes" -le 1073741824 ] 2>/dev/null; then
    echo 6
  elif [ "$limit_bytes" -le 4294967296 ] 2>/dev/null; then
    echo 10
  else
    echo 12
  fi
}

resolve_max_metaspace() {
  if [ -n "${PJB_JVM_MAX_METASPACE_SIZE:-}" ]; then
    echo "${PJB_JVM_MAX_METASPACE_SIZE}"
    return
  fi
  local limit_bytes
  limit_bytes="$(detect_memory_limit_bytes)"
  if [ "$limit_bytes" -le 1073741824 ] 2>/dev/null; then
    echo 256m
  elif [ "$limit_bytes" -le 4294967296 ] 2>/dev/null; then
    echo 384m
  elif [ "$limit_bytes" -le 8589934592 ] 2>/dev/null; then
    echo 512m
  else
    echo 768m
  fi
}

resolve_max_direct_memory() {
  if [ -n "${PJB_JVM_MAX_DIRECT_MEMORY_SIZE:-}" ]; then
    echo "${PJB_JVM_MAX_DIRECT_MEMORY_SIZE}"
    return
  fi
  local limit_bytes
  limit_bytes="$(detect_memory_limit_bytes)"
  if [ "$limit_bytes" -le 1073741824 ] 2>/dev/null; then
    echo 96m
  elif [ "$limit_bytes" -le 2147483648 ] 2>/dev/null; then
    echo 128m
  elif [ "$limit_bytes" -le 4294967296 ] 2>/dev/null; then
    echo 192m
  elif [ "$limit_bytes" -le 8589934592 ] 2>/dev/null; then
    echo 256m
  else
    echo 384m
  fi
}

resolve_reserved_code_cache() {
  if [ -n "${PJB_JVM_RESERVED_CODE_CACHE_SIZE:-}" ]; then
    echo "${PJB_JVM_RESERVED_CODE_CACHE_SIZE}"
    return
  fi
  local limit_bytes
  limit_bytes="$(detect_memory_limit_bytes)"
  if [ "$limit_bytes" -le 2147483648 ] 2>/dev/null; then
    echo 128m
  else
    echo 192m
  fi
}

resolve_gc_thread_flags() {
  local cpus parallel conc
  cpus="$(detect_cpu_limit)"
  if [ "$cpus" -lt 1 ] 2>/dev/null; then
    cpus=1
  fi
  parallel="$cpus"
  if [ "$parallel" -gt 8 ] 2>/dev/null; then
    parallel=8
  fi
  conc=$(( parallel / 4 ))
  if [ "$conc" -lt 1 ] 2>/dev/null; then
    conc=1
  fi
  echo "-XX:ActiveProcessorCount=${cpus} -XX:ParallelGCThreads=${parallel} -XX:ConcGCThreads=${conc}"
}

resolve_gc_log_flags() {
  if [ "${PJB_JVM_GC_LOG_ENABLED:-true}" != "true" ]; then
    echo ""
    return
  fi
  local path
  path="${PJB_JVM_GC_LOG_PATH:-/tmp/pjb-gc.log}"
  echo "-Xlog:os+container=info,gc*=info,safepoint=info:file=${path}:time,uptime,tags:filecount=5,filesize=20M"
}

resolve_native_memory_tracking_flags() {
  if [ "${PJB_JVM_NATIVE_MEMORY_TRACKING:-off}" = "off" ]; then
    echo ""
    return
  fi
  echo "-XX:+UnlockDiagnosticVMOptions -XX:NativeMemoryTracking=${PJB_JVM_NATIVE_MEMORY_TRACKING}"
}

resolve_jfr_flags() {
  if [ "${PJB_JVM_JFR_ENABLED:-false}" != "true" ]; then
    echo ""
    return
  fi
  local filename settings maxage maxsize
  filename="${PJB_JVM_JFR_FILENAME:-/tmp/pjb-runtime.jfr}"
  settings="${PJB_JVM_JFR_SETTINGS:-profile}"
  maxage="${PJB_JVM_JFR_MAXAGE:-30m}"
  maxsize="${PJB_JVM_JFR_MAXSIZE:-256m}"
  echo "-XX:StartFlightRecording=filename=${filename},settings=${settings},disk=true,dumponexit=true,maxage=${maxage},maxsize=${maxsize}"
}

base_flags="-Dfile.encoding=UTF-8 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${PJB_JVM_HEAP_DUMP_PATH:-/tmp} -XX:MaxRAMPercentage=$(resolve_max_ram_percentage) -XX:InitialRAMPercentage=$(resolve_initial_ram_percentage) -XX:MaxMetaspaceSize=$(resolve_max_metaspace) -XX:MaxDirectMemorySize=$(resolve_max_direct_memory) -XX:ReservedCodeCacheSize=$(resolve_reserved_code_cache) -XX:+ParallelRefProcEnabled $(resolve_gc_thread_flags) $(resolve_gc_log_flags) $(resolve_native_memory_tracking_flags) $(resolve_jfr_flags)"
archive_file="${PJB_JVM_SHARED_ARCHIVE_FILE:-/app/cds/app-cds.jsa}"
shared_flags=""
if [ -f "$archive_file" ]; then
  shared_flags="-Xshare:auto -XX:SharedArchiveFile=$archive_file"
fi

case "$profile" in
  latency)
    runtime_flags="-XX:+UseZGC -XX:+ZGenerational $shared_flags"
    ;;
  startup)
    runtime_flags="-XX:+UseG1GC -XX:MaxGCPauseMillis=${PJB_JVM_MAX_GC_PAUSE_MS:-200} -XX:+UseStringDeduplication $shared_flags"
    ;;
  *)
    runtime_flags="-XX:+UseG1GC -XX:MaxGCPauseMillis=${PJB_JVM_MAX_GC_PAUSE_MS:-200} -XX:+UseStringDeduplication $shared_flags"
    ;;
esac

exec java $base_flags $runtime_flags ${JAVA_OPTS:-} -jar /app/app.jar
