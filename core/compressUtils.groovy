#!/usr/bin/env groovy
@Grapes(
        @Grab(group = 'org.apache.commons', module = 'commons-compress', version = '1.15')
)
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.*
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

def updateCompressedEntry(tar, Closure closure) {


    def zipType = tar.name.lastIndexOf('.').with { it != -1 ? tar.name[it + 1..<tar.name.length()] : tar.name }

    switch (zipType) {
        case "tar": zipType = ArchiveStreamFactory.TAR; break;
        case "gz": zipType = CompressorStreamFactory.GZIP; break;
        case "zip": zipType = ArchiveStreamFactory.ZIP; break;
    }
    def outZipType = CompressorStreamFactory.GZIP.equals(zipType) ? ArchiveStreamFactory.TAR : zipType

    def asf = new ArchiveStreamFactory()
    def tmp = File.createTempFile("temp_${System.nanoTime()}", ".${CompressorStreamFactory.GZIP.equals(zipType) ? "tar.gz" : zipType}")

    def fis = new FileInputStream(tar)
    def fos = new FileOutputStream(tmp);
    def ais = null;
    def aos = null;
    if (CompressorStreamFactory.GZIP.equals(zipType)) {
        ais = new TarArchiveInputStream(new CompressorStreamFactory().createCompressorInputStream(zipType, fis));
        def gzOut = new GzipCompressorOutputStream(new BufferedOutputStream(fos));
        aos = asf.createArchiveOutputStream(outZipType, gzOut);
    } else {
        ais = asf.createArchiveInputStream(outZipType, fis);
        aos = asf.createArchiveOutputStream(outZipType, fos);
    }
    if (ArchiveStreamFactory.TAR.equals(outZipType))
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
            entries.eachWithIndex { entry, int i ->
                if (i < entries.length - 1) {
                    entryName.append(entry).append("/")
                } else {
                    entryName.append(obj.name)
                }
            }
            nextEntry = outZipType.with {
                if (it.equals(ArchiveStreamFactory.ZIP)) {
                    new ZipArchiveEntry(entryName.toString())
                } else if (it.equals(ArchiveStreamFactory.TAR)) {
                    new TarArchiveEntry(entryName.toString())
                }
            }
            entry.setSize(obj.length());
            aos.putArchiveEntry(nextEntry);
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

//println updateTarEntry(new File("/Users/kcheng/Downloads/xlydc1-project.zip"), { it ->
//    println it.name
//})




