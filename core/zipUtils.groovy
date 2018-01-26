#!/usr/bin/env groovy
@Grapes(
        @Grab(group = 'org.apache.commons', module = 'commons-compress', version = '1.15')
)
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.*


def updateTarEntry(tar, Closure closure) {
    def asf = new ArchiveStreamFactory()
    def tmp = File.createTempFile("temp_${System.nanoTime()}", '.tar')

    def fis = new FileInputStream(tar);
    def ais = asf.createArchiveInputStream(ArchiveStreamFactory.TAR, fis);

    def fos = new FileOutputStream(tmp);
    def aos = asf.createArchiveOutputStream(ArchiveStreamFactory.TAR, fos);

    aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)

    def nextEntry;
    while ((nextEntry = ais.getNextEntry()) != null) {

        def obj = closure(nextEntry)
        if (obj == null) {
            aos.putArchiveEntry(nextEntry);
            aos << ais
        } else {
            def entries = nextEntry.name.split("/")
            def entryName = new StringBuilder();
            entries.eachWithIndex { String entry, int i ->
                if(i < entries.length-1) {
                    entryName.append(entry).append("/")
                } else {
                    entryName.append(obj.name)
                }
            }
            TarArchiveEntry entry = new TarArchiveEntry(entryName.toString());
            entry.setSize(obj.length());
            aos.putArchiveEntry(entry);
            aos << obj.bytes
        }
        aos.closeArchiveEntry()

    }
    aos.finish()
    aos.close()

    return tmp.absolutePath

}


/*
println updateTarEntry("/Users/kcheng/Downloads/hadoop-1.0.4.tar", { it ->
    if(it.isFile() &&it.name.indexOf("conf/core-site.xml") > -1){
//        println it.name
        return new File("/Users/kcheng/Downloads/data.txt");
    }
})
*/

