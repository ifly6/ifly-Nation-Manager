package com.git.ifly6.nationManager;

import com.git.ifly6.nationManager.gui.IfnmNation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IfnmWriter {

    /**
     * Delimiter to use with generated files
     */
    static final String DELIMITER = "\t";

    /**
     * Defines the structure of the header. See {@link IfnmWriter#createHeader(int)}
     */
    private static final String FORMAT = "# ifly nation manager save file\n" +
            "# created %s with %d nations";

    private IfnmWriter() {
    }

    /**
     * Writes the list of nations to file
     * @param path       to write to
     * @param nationList to get written
     * @throws IOException if thrown by {@link Files#write(Path, Iterable, OpenOption...)}
     */
    public static void write(Path path, List<IfnmNation> nationList) throws IOException {
        // add header
        List<String> lines = new ArrayList<>(Collections.singleton(createHeader(nationList.size())));
        lines.addAll(encode(nationList));

        // force extension
        if (!path.toString().toLowerCase().endsWith(".txt")) {
            String fileName = path.getFileName().toString();
            path = path.getParent().resolve(fileName.substring(0, fileName.lastIndexOf(".")) + ".txt");
            //                                           start ^  ^ end                      ^ force ext
        }

        // do write
        Files.write(path, lines);
    }

    /**
     * Creates headers based on the final format string
     * @param size of the nations stored, which is kept for accounting
     * @return formatted header
     */
    private static String createHeader(int size) {
        return String.format(FORMAT, Instant.now().toString(), size);
    }

    /**
     * Encodes nation list to tab separated file with name and password hash
     * @param nationList to encode
     * @return <code>List&lt;String&gt;</code> with nation's name and password separated by {@link IfnmWriter#DELIMITER}
     */
    private static List<String> encode(List<IfnmNation> nationList) {
        return nationList.stream()
                .map(n -> n.getName() + DELIMITER + n.getPassword() + DELIMITER + n.exists())
                .collect(Collectors.toList());
    }

}
