package com.tcc.pjb.backend.core.modularity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PjbModuleDescriptor(
        PjbModuleId id,
        String displayName,
        List<String> ownedPackageRoots,
        Set<String> publicEntryPoints,
        Set<String> tags
) {

    public PjbModuleDescriptor {
        id = Objects.requireNonNull(id, "id");
        displayName = requireText(displayName, "displayName");
        ownedPackageRoots = normalizePackageRoots(ownedPackageRoots);
        publicEntryPoints = normalizeTokens(publicEntryPoints, "publicEntryPoints");
        tags = normalizeTokens(tags, "tags");
    }

    public boolean ownsPackage(String packageName) {
        String normalizedPackage = PjbModuleId.normalizePackageName(packageName);
        if (normalizedPackage == null) {
            return false;
        }
        if (id.ownsNormalizedPackage(normalizedPackage)) {
            return true;
        }
        for (int i = 0; i < ownedPackageRoots.size(); i++) {
            String root = ownedPackageRoots.get(i);
            if (normalizedPackage.equals(root) || normalizedPackage.startsWith(root + ".")) {
                return true;
            }
        }
        return false;
    }

    public boolean exposesEntryPoint(String entryPoint) {
        return containsNormalized(publicEntryPoints, entryPoint);
    }

    public boolean hasTag(String tag) {
        return containsNormalized(tags, tag);
    }

    private static List<String> normalizePackageRoots(List<String> roots) {
        Objects.requireNonNull(roots, "ownedPackageRoots");
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("ownedPackageRoots must not be empty");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (int i = 0; i < roots.size(); i++) {
            String root = PjbModuleId.normalizePackageName(roots.get(i));
            if (root == null) {
                throw new IllegalArgumentException("ownedPackageRoots must not contain blank values");
            }
            normalized.add(root);
        }
        ArrayList<String> ordered = new ArrayList<>(normalized);
        ordered.sort((left, right) -> {
            int lengthComparison = Integer.compare(right.length(), left.length());
            return lengthComparison != 0 ? lengthComparison : left.compareTo(right);
        });
        return List.copyOf(ordered);
    }

    private static Set<String> normalizeTokens(Set<String> values, String fieldName) {
        Objects.requireNonNull(values, fieldName);
        if (values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(requireText(value, fieldName));
        }
        return Set.copyOf(normalized);
    }

    private static boolean containsNormalized(Set<String> values, String candidate) {
        String normalized = PjbModuleId.normalizePackageName(candidate);
        return normalized != null && values.contains(normalized);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
