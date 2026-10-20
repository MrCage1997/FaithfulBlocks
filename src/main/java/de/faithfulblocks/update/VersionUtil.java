package de.faithfulblocks.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class VersionUtil {
    private VersionUtil() {
    }

    static int compare(String left, String right) {
        ParsedVersion a = ParsedVersion.parse(left);
        ParsedVersion b = ParsedVersion.parse(right);

        int max = Math.max(a.core().size(), b.core().size());
        for (int i = 0; i < max; i++) {
            int av = i < a.core().size() ? a.core().get(i) : 0;
            int bv = i < b.core().size() ? b.core().get(i) : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }

        if (a.preRelease().isEmpty() && b.preRelease().isEmpty()) {
            return 0;
        }
        if (a.preRelease().isEmpty()) {
            return 1;
        }
        if (b.preRelease().isEmpty()) {
            return -1;
        }

        int preMax = Math.max(a.preRelease().size(), b.preRelease().size());
        for (int i = 0; i < preMax; i++) {
            if (i >= a.preRelease().size()) {
                return -1;
            }
            if (i >= b.preRelease().size()) {
                return 1;
            }

            String av = a.preRelease().get(i);
            String bv = b.preRelease().get(i);
            boolean an = av.matches("\\d+");
            boolean bn = bv.matches("\\d+");

            if (an && bn) {
                int cmp = Integer.compare(Integer.parseInt(av), Integer.parseInt(bv));
                if (cmp != 0) return cmp;
            } else if (an != bn) {
                return an ? -1 : 1;
            } else {
                int cmp = av.compareTo(bv);
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }

    private record ParsedVersion(List<Integer> core, List<String> preRelease) {
        static ParsedVersion parse(String source) {
            String value = source == null ? "0" : source.trim().toLowerCase(Locale.ROOT);
            if (value.startsWith("v")) {
                value = value.substring(1);
            }

            int buildIndex = value.indexOf('+');
            if (buildIndex >= 0) {
                value = value.substring(0, buildIndex);
            }

            String coreText = value;
            String preText = "";
            int dash = value.indexOf('-');
            if (dash >= 0) {
                coreText = value.substring(0, dash);
                preText = value.substring(dash + 1);
            }

            List<Integer> core = new ArrayList<>();
            for (String part : coreText.split("\\.")) {
                try {
                    core.add(Integer.parseInt(part.replaceAll("[^0-9]", "")));
                } catch (NumberFormatException ignored) {
                    core.add(0);
                }
            }

            List<String> pre = new ArrayList<>();
            if (!preText.isBlank()) {
                for (String part : preText.split("[._-]")) {
                    if (!part.isBlank()) {
                        // Split rc2 -> rc, 2 so release-candidate ordering stays intuitive.
                        String[] pieces = part.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                        for (String piece : pieces) {
                            if (!piece.isBlank()) pre.add(piece);
                        }
                    }
                }
            }

            return new ParsedVersion(core, pre);
        }
    }
}
