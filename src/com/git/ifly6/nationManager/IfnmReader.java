package com.git.ifly6.nationManager;

import com.git.ifly6.nationManager.gui.IfnmNation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class IfnmReader {

    /**
     * Reads a list of <code>IfnmNation</code>s from a file in the format created by {@link IfnmWriter}.
     * @param path to read
     * @return the list of nations
     * @throws IOException if thrown by {@link Files#readAllLines(Path)}
     */
    public static List<IfnmNation> read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        lines = lines.stream()
                .filter(s -> !s.startsWith("#"))
                .collect(Collectors.toList());

        List<IfnmNation> nations = new ArrayList<>();
        for (String line : lines) {
            String[] s = line.split(IfnmWriter.DELIMITER);
            if (s.length == 3)
                nations.add(new IfnmNation(s[0], s[1]) // create nation
                        .setExists(Boolean.parseBoolean(s[2]))); // with this existence state
        }

        return dropDuplicates(nations);
    }

    /**
     * Lists the names of the nations. See {@link IfnmNation#toString()}.
     * @param ifnmNations list of nations to turn into a string
     * @return list of strings with nation names
     */
    public static List<String> list(List<IfnmNation> ifnmNations) {
        return ifnmNations.stream()
                .map(IfnmNation::toString)
                .collect(Collectors.toList());
    }

    private static <T> List<T> dropDuplicates(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

}
