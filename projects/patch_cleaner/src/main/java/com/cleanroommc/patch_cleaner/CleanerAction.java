package com.cleanroommc.patch_cleaner;

import io.codechicken.diffpatch.util.Diff;
import io.codechicken.diffpatch.util.Operation;
import io.codechicken.diffpatch.util.Patch;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZZZank
 */
public interface CleanerAction {

    /**
     * @return line number change
     */
    int process(Patch patch);

    static int removeGeneric(Patch patch) {
        var diffs = patch.diffs;
        var ranges = Kit.searchSameLineDiff(diffs);

        var hunksAt = new ArrayList<int[]>();
        for (var range : ranges) {
            int begin = range[0], end = range[1];
            var selected = diffs.subList(begin, end);
            var zipped = Kit.zipSameLineDiff(selected);
            var isHunk = zipped.stream()
                .allMatch(e -> {
                    var from = e.getKey().text;
                    var to = e.getValue().text;
                    var textDiff = Kit.searchDiffSection(from, to);
                    return Kit.isGenericOrCast(textDiff);
                });
            if (isHunk) {
                hunksAt.add(range);
            }
        }

        for (var ints : hunksAt.reversed()) {
            var begin = ints[0];
            var end = ints[1];
            if (end > begin) {
                var contextDiffs = diffs.subList(begin, (begin + end) / 2)
                    .stream()
                    .map(diff -> new Diff(Operation.EQUAL, diff.text))
                    .toList();
                var subList = diffs.subList(begin, end);
                subList.clear();
                subList.addAll(contextDiffs);
            }
        }

        return 0;
    }
}
