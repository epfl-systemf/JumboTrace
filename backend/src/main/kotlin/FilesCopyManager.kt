import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class FilesCopyManager(private val tmpDirPath: Path) {

    fun scanAndCopyToTmp(dirToScan: Path) {


        val tmpDir = tmpDirPath.toFile()
        if (tmpDir.exists()){
            throw IOException("path provided for temporary directory already exists")
        }
        Files.createDirectories(tmpDirPath)
        val tmpBinDirPath = tmpDirPath.resolve("bin").resolve("java")
        Files.createDirectories(tmpBinDirPath)
        val tmpBinDir = tmpBinDirPath.toFile()

        fun searchAndCopyRecursively(dir: File){
            require(dir.isDirectory)
            for (sub in dir.listFiles()!!){
                when {
                    sub.isFile && sub.extension == "java" -> {
                        sub.copyTo(tmpDir.resolve(sub.name))
                    }

                    sub.isFile && sub.extension == "class" -> {
                        sub.copyTo(tmpBinDir.resolve(sub.name))
                    }

                    sub.isDirectory ->
                        searchAndCopyRecursively(sub)
                }
            }
        }

        searchAndCopyRecursively(dirToScan.toFile())
    }

    fun deleteTmpDir(){

        fun deleteRecursively(file: File){
            file.listFiles()?.forEach(::deleteRecursively)
            file.delete()
        }

        deleteRecursively(tmpDirPath.toFile())
    }

}