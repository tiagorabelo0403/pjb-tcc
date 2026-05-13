package com.tcc.pjb.backend.core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.storage")
public class ObjectStorageProperties {

    private Local local = new Local();
    private Upload upload = new Upload();

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    public static class Local {
        private String baseDir = "./var/pjb/storage";

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }
    }

    public static class Upload {
        private String tokenSecret = "change-me";
        private int maxConcurrentStreams = 128;
        private int tokenTtlSeconds = 900;
        private int bulkFinalizeChunkSize = 500;
        private int maxBatchItems = 64;
        private long maxBatchTotalBytes = 536_870_912L;
        private long maxDocumentBytes = 26_214_400L;
        private long maxImageBytes = 12_582_912L;
        private long maxAudioBytes = 33_554_432L;
        private long maxVideoBytes = 134_217_728L;
        private long maxInlineImageBytes = 10_485_760L;
        private long maxInlineAudioBytes = 25_165_824L;
        private long maxInlineVideoBytes = 100_663_296L;
        private long maxInlineTotalBytes = 167_772_160L;
        private int maxInlineBlocks = 16;
        private int maxPostPetitionReferences = 160;
        private long maxAudioDurationMs = 900_000L;
        private long maxVideoDurationMs = 900_000L;
        private long syncPreviewMaxBytes = 8_388_608L;
        private long deepInspectionThresholdBytes = 50_331_648L;

        public String getTokenSecret() {
            return tokenSecret;
        }

        public void setTokenSecret(String tokenSecret) {
            this.tokenSecret = tokenSecret;
        }

        public int getMaxConcurrentStreams() {
            return maxConcurrentStreams;
        }

        public void setMaxConcurrentStreams(int maxConcurrentStreams) {
            this.maxConcurrentStreams = maxConcurrentStreams;
        }

        public int getTokenTtlSeconds() {
            return tokenTtlSeconds;
        }

        public void setTokenTtlSeconds(int tokenTtlSeconds) {
            this.tokenTtlSeconds = tokenTtlSeconds;
        }

        public int getBulkFinalizeChunkSize() {
            return bulkFinalizeChunkSize;
        }

        public void setBulkFinalizeChunkSize(int bulkFinalizeChunkSize) {
            this.bulkFinalizeChunkSize = bulkFinalizeChunkSize;
        }

        public int getMaxBatchItems() {
            return maxBatchItems;
        }

        public void setMaxBatchItems(int maxBatchItems) {
            this.maxBatchItems = maxBatchItems;
        }

        public long getMaxBatchTotalBytes() {
            return maxBatchTotalBytes;
        }

        public void setMaxBatchTotalBytes(long maxBatchTotalBytes) {
            this.maxBatchTotalBytes = maxBatchTotalBytes;
        }

        public long getMaxDocumentBytes() {
            return maxDocumentBytes;
        }

        public void setMaxDocumentBytes(long maxDocumentBytes) {
            this.maxDocumentBytes = maxDocumentBytes;
        }

        public long getMaxImageBytes() {
            return maxImageBytes;
        }

        public void setMaxImageBytes(long maxImageBytes) {
            this.maxImageBytes = maxImageBytes;
        }

        public long getMaxAudioBytes() {
            return maxAudioBytes;
        }

        public void setMaxAudioBytes(long maxAudioBytes) {
            this.maxAudioBytes = maxAudioBytes;
        }

        public long getMaxVideoBytes() {
            return maxVideoBytes;
        }

        public void setMaxVideoBytes(long maxVideoBytes) {
            this.maxVideoBytes = maxVideoBytes;
        }

        public long getMaxInlineImageBytes() {
            return maxInlineImageBytes;
        }

        public void setMaxInlineImageBytes(long maxInlineImageBytes) {
            this.maxInlineImageBytes = maxInlineImageBytes;
        }

        public long getMaxInlineAudioBytes() {
            return maxInlineAudioBytes;
        }

        public void setMaxInlineAudioBytes(long maxInlineAudioBytes) {
            this.maxInlineAudioBytes = maxInlineAudioBytes;
        }

        public long getMaxInlineVideoBytes() {
            return maxInlineVideoBytes;
        }

        public void setMaxInlineVideoBytes(long maxInlineVideoBytes) {
            this.maxInlineVideoBytes = maxInlineVideoBytes;
        }

        public long getMaxInlineTotalBytes() {
            return maxInlineTotalBytes;
        }

        public void setMaxInlineTotalBytes(long maxInlineTotalBytes) {
            this.maxInlineTotalBytes = maxInlineTotalBytes;
        }

        public int getMaxInlineBlocks() {
            return maxInlineBlocks;
        }

        public void setMaxInlineBlocks(int maxInlineBlocks) {
            this.maxInlineBlocks = maxInlineBlocks;
        }

        public int getMaxPostPetitionReferences() {
            return maxPostPetitionReferences;
        }

        public void setMaxPostPetitionReferences(int maxPostPetitionReferences) {
            this.maxPostPetitionReferences = maxPostPetitionReferences;
        }

        public long getMaxAudioDurationMs() {
            return maxAudioDurationMs;
        }

        public void setMaxAudioDurationMs(long maxAudioDurationMs) {
            this.maxAudioDurationMs = maxAudioDurationMs;
        }

        public long getMaxVideoDurationMs() {
            return maxVideoDurationMs;
        }

        public void setMaxVideoDurationMs(long maxVideoDurationMs) {
            this.maxVideoDurationMs = maxVideoDurationMs;
        }

        public long getSyncPreviewMaxBytes() {
            return syncPreviewMaxBytes;
        }

        public void setSyncPreviewMaxBytes(long syncPreviewMaxBytes) {
            this.syncPreviewMaxBytes = syncPreviewMaxBytes;
        }

        public long getDeepInspectionThresholdBytes() {
            return deepInspectionThresholdBytes;
        }

        public void setDeepInspectionThresholdBytes(long deepInspectionThresholdBytes) {
            this.deepInspectionThresholdBytes = deepInspectionThresholdBytes;
        }
    }
}
