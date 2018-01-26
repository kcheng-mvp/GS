#!/usr/bin/env groovy
@Grapes(
        @Grab(group = 'org.apache.commons', module = 'commons-compress', version = '1.15')
)
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.*
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

def updateTarEntry(tar, Closure closure) {

    def tarGz = tar.name.indexOf("tar.gz") > 0;
    def asf = new ArchiveStreamFactory()
    def tmp = File.createTempFile("temp_${System.nanoTime()}", tarGz ? ".tar.gz" : ".tar")

    def fis = new FileInputStream(tar)
    def fos = new FileOutputStream(tmp);
    def ais = null;
    def aos = null;
    if (tarGz) {
        ais = new TarArchiveInputStream(new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP,fis));
        def gzOut = new GzipCompressorOutputStream(new BufferedOutputStream(fos));
        aos = asf.createArchiveOutputStream(ArchiveStreamFactory.TAR, gzOut);
    } else {
        ais = asf.createArchiveInputStream(ArchiveStreamFactory.TAR, fis);
        aos = asf.createArchiveOutputStream(ArchiveStreamFactory.TAR, fos);
    }

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
                if (i < entries.length - 1) {
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
println updateTarEntry(new File("/Users/kcheng/Downloads/zookeeper-3.4.11.tar.gz"), { it ->
    println it.name
})
println updateTarEntry(new File("/Users/kcheng/Downloads/hadoop-1.0.4.tar"), { it ->
    println it.name
})
*/

