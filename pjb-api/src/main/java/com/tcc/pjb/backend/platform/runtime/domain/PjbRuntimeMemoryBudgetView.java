package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbRuntimeMemoryBudgetView(
        long heapMiB,
        long directMemoryMiB,
        long metaspaceMiB,
        long codeCacheMiB,
        long nativeReserveMiB,
        long plannedEnvelopeMiB,
        long baselineMaxMemoryMiB
) {

    public PjbRuntimeMemoryBudgetView {
        heapMiB = nonNegative(heapMiB);
        directMemoryMiB = nonNegative(directMemoryMiB);
        metaspaceMiB = nonNegative(metaspaceMiB);
        codeCacheMiB = nonNegative(codeCacheMiB);
        nativeReserveMiB = nonNegative(nativeReserveMiB);
        plannedEnvelopeMiB = nonNegative(plannedEnvelopeMiB);
        baselineMaxMemoryMiB = nonNegative(baselineMaxMemoryMiB);
    }

    public long totalReservedMiB() {
        return heapMiB + directMemoryMiB + metaspaceMiB + codeCacheMiB + nativeReserveMiB;
    }

    public long headroomMiB() {
        return Math.max(0L, baselineMaxMemoryMiB - plannedEnvelopeMiB);
    }

    public boolean isBaselineExceeded() {
        return plannedEnvelopeMiB > baselineMaxMemoryMiB;
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }
}
