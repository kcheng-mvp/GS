/**
 * split the string by the default tab
 * The \\W means not an alphanumeric character
 * @param line line of string
 */
def split(String line) {
    return line.split("\\t+");
}

/**
 * Split the line of file with filters
 * @param file
 * @param filters
 * @return
 */
def split(File file, String... filters) {
    def dataList = new ArrayList();
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
                dataList.add(split(line))
            }
        } else {
            dataList.add(split(line))
        }

    }
    return dataList;
}