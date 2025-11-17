package com.cleanroommc.patch_cleaner;

import io.codechicken.diffpatch.util.Diff;
import io.codechicken.diffpatch.util.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ZZZank
 */
public class Kit {

    /// scan for diffs not affecting line number
    ///
    /// @return list of `[begin, end)` pair
    public static List<int[]> searchSameLineDiff(List<Diff> diffs) {
        var result = new ArrayList<int[]>();

        int begin = -1;
        int operationDiff = 0;

        for (int i = 0; i < diffs.size(); i++) {
            var diff = diffs.get(i);
            var op = diff.op;
            if (op == Operation.EQUAL) {
                if (begin >= 0) {
                    if (operationDiff == 0) {
                        result.add(new int[]{begin, i});
                    }
                    operationDiff = 0;
                    begin = -1;
                }
                continue;
            }
            if (op == Operation.DELETE && begin < 0) {
                begin = i;
            }
            if (begin >= 0) {
                operationDiff += switch (op) {
                    case DELETE -> -1;
                    case INSERT -> 1;
                    default -> throw new IllegalStateException("Unexpected value: " + op);
                };
            }
        }
        return result;
    }

    public static List<Map.Entry<Diff, Diff>> zipSameLineDiff(List<Diff> diffs) {
        assert diffs.size() % 2 == 0;
        var size = diffs.size() / 2;

        var result = new ArrayList<Map.Entry<Diff, Diff>>(size);
        for (int i = 0; i < size; i++) {
            result.add(Map.entry(diffs.get(i), diffs.get(i + size)));
        }
        return result;
    }

    public static String searchDiffSection(String from, String to) {
        int begin = 0;
        while (begin < Math.min(from.length(), to.length()) && from.charAt(begin) == to.charAt(begin)) {
            begin++;
        }

        // aaacc -> aaabcc
        // begin = 3, to.length() = 6, from.length() = 5, lenDiff = 1
        // from.substring(begin) = cc
        // to.substring(lenDiff + begin) = cc
        int lenDiff = to.length() - from.length();
        if (lenDiff > 0 // to > from
            && from.substring(begin).equals(to.substring(lenDiff + begin))
        ) {
            return to.substring(begin, begin + lenDiff);
        }

        return "";
    }

    /// `<Type>` or `(Type)`
    public static boolean isGenericOrCast(String text) {
        if (text.startsWith("<") && text.endsWith(">")) {
            return true;
        }
        if (text.startsWith("(") && text.endsWith(")")) {
            return true;
        }
        return false;
    }

    private static void testSearchDiffSection() {
        var testData = List.of(
            List.of(
                "private final Map<String, ScoreObjective> scoreObjectives = Maps.newHashMap();",
                "private final Map<String, ScoreObjective> scoreObjectives = Maps.<String, ScoreObjective>newHashMap();",
                "<String, ScoreObjective>"
            ),
            List.of(
                "                List<ScoreObjective> list = this.scoreObjectiveCriterias.get(criteria);",
                "                List<ScoreObjective> list = (List)this.scoreObjectiveCriterias.get(criteria);",
                "(List)"
            ),
            List.of(
                "list = Lists.newArrayList();",
                "list = Lists.<ScoreObjective>newArrayList();",
                "<ScoreObjective>"
            )
        );
    }
}
