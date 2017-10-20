/**
 * split the string by the default tab
 * The \\W means not an alphanumeric character
 * @param line line of string
 */
def split(String line) {
    return line.split("\\W+");
}

/**
 * Split the line of file with filters
 * @param file
 * @param filters
 * @return
 */
def split(File file, String... filters) {
    def map = new HashMap();
    def lineNum = 0;
    file.eachLine { line ->
        if (filters != null) {
            boolean passed = true;
            for (int i = 0; i < filters.length; i++) {
                if (line.indexOf(filters[i]) < 0) {
                    passed = false;
                    break
                };
            }
            if (passed) {
                lineNum++;
                map.put(lineNum, split(line));
            }
        } else {
            lineNum++;
            map.put(lineNum, split(line));
        }

    }
    return map;
}