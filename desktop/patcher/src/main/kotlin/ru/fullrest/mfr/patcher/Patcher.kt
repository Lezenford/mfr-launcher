package ru.fullrest.mfr.patcher

import com.fasterxml.jackson.databind.ObjectMapper
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.util.FS
import ru.fullrest.mfr.api.GameUpdate
import ru.fullrest.mfr.api.MoveFile
import java.io.*
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


fun main() {
//    val update: GameUpdate = ObjectMapper().readValue("{\"version\":\"4.0.03\",\"addFiles\":[\"Data Files/Icons/EVA1/c/robe_mage_Apprentice.tga\",\"Data Files/MFR.esm\",\"Data Files/MFR_TR_patch.esp\",\"Data Files/MFR_patch.esp\",\"Data Files/MWSE/config/Companion Health Bars.json\",\"Data Files/MWSE/config/Quick Loot.json\",\"Data Files/MWSE/config/graphicHerbalism.json\",\"Data Files/MWSE/config/pg_ownership_config.json\",\"Data Files/MWSE/mods/FullRestRu/DangerousFire/main.lua\",\"Data Files/MWSE/mods/OperatorJack/EnhancedLight/effects.lua\",\"Data Files/MWSE/mods/OperatorJack/MagickaExpanded-LoreFriendlyPack/effects/darknessEffect.lua\",\"Data Files/Meshes/EVA/msc/daedroth_venom.nif\",\"Data Files/Meshes/EVA/msc/dreugh_skin.nif\",\"Data Files/Meshes/EVA/msc/durable_metal_plate.nif\",\"Data Files/Meshes/EVA/msc/metallic_thread.nif\",\"Data Files/Meshes/f/Flora_emp_parasol_01.nif\",\"Data Files/Meshes/f/Flora_emp_parasol_02.nif\",\"Data Files/Meshes/f/Flora_emp_parasol_03.nif\",\"Data Files/Meshes/f/Flora_tree_01.nif\",\"Data Files/Meshes/f/Flora_tree_02.nif\",\"Data Files/Meshes/f/Flora_tree_03.nif\",\"Data Files/Meshes/f/Flora_tree_04.nif\",\"Data Files/Meshes/f/Flora_tree_AI_05.nif\",\"Data Files/Meshes/f/Flora_tree_AI_06.nif\",\"Data Files/Textures/EVA/msc/daedroth_venom.dds\",\"Data Files/distantland/statics/static_meshes\",\"Data Files/distantland/statics/usage.data\",\"Data Files/distantland/world\",\"Data Files/distantland/world.dds\",\"Data Files/distantland/world_n.dds\",\"Optional/version\"],\"moveFiles\":[],\"removeFiles\":[\"Data Files/Icons/EVA1/w/sm_bloody_lw.dds\",\"Data Files/Meshes/EVA1/w/sm_bloody_ls.nif\",\"Data Files/Meshes/EVA1/w/sm_bloody_ls_sh.nif\",\"Data Files/Textures/EVA/msc/metalic_thread.dds\",\"Data Files/Textures/EVA1/w/sm_bloody_sword.dds\",\"Data Files/Textures/EVA1/w/sm_bloody_sword_g.dds\",\"Data Files/Textures/EVA1/w/sm_bloody_sword_n.dds\",\"Data Files/Textures/EVA1/w/sm_bloody_sword_sh.dds\",\"Data Files/Textures/EVA1/w/sm_bloody_sword_sh_n.dds\"]}")
//    println(update)
//    val reader = BufferedReader(InputStreamReader(System.`in`))
//    println("Введите путь до каталога с репозиторием")
//
//    val factory = object : JschConfigSessionFactory() {
//        override fun createDefaultJSch(fs: FS?): JSch {
//            val defaultJSch = super.createDefaultJSch(fs)
//            defaultJSch.removeAllIdentity()
//            defaultJSch.addIdentity("C:/Users/Famaly/.ssh/mfr-server")
//            return defaultJSch
//        }
//
//        override fun configure(hc: OpenSshConfig.Host?, session: Session?) {
//            session?.setConfig("StrictHostKeyChecking", "no");
//        }
//    }
////    Git.cloneRepository()
////        .setURI("ssh://git@gitlab.lezenford.com/Lezenford/test-jgit-project.git")
////        .setTransportConfigCallback {
////            val sshTransport = it as SshTransport
////            sshTransport.sshSessionFactory = factory
////        }
////        .setDirectory(File("C:/Games/test"))
////        .call()
//    val path = "C:\\Games\\morrowind-fullrest-repack(git)"//reader.use { it.readLine() }
//    val repository = FileRepositoryBuilder().setGitDir(File("$path\\.git")).readEnvironment().build()
//    val git = Git(repository)
////
////    git.checkout()
//////        .setCreateBranch(true)
////        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
////
////        .setName("test-server")
////        .call()
////    git.pull().setTransportConfigCallback {
////            val sshTransport = it as SshTransport
////            sshTransport.sshSessionFactory = factory
////        }.call()
////
////
//    val find = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().find { it.name == "refs/heads/master" }
////
////    val call = git.tagList().call().first()
//    val parseCommit = RevWalk(repository).parseCommit(find!!.objectId)
////
////    git.tag().setName("testTag").setObjectId(parseCommit).setForceUpdate(true).call()
////
////    git.push()
////        .setTransportConfigCallback {
////            val sshTransport = it as SshTransport
////            sshTransport.sshSessionFactory = factory
////        }
////        .setPushTags().call()
//
////    val prevCommit = parseCommit.parents.map { RevWalk(repository).parseCommit(it) }.minBy { it.commitTime }
//    val prevCommit = RevWalk(repository).parseCommit(parseCommit.getParent(0))
//
//    val diffFormatter = DiffFormatter(System.out)
//    diffFormatter.setRepository(repository)
//    val scan = diffFormatter.scan(prevCommit, parseCommit)
//    scan.forEach {
//        println(it.newPath)
//    }
//    return

    val path = "C:\\Games\\morrowind-fullrest-repack(git)"//reader.use { it.readLine() }
//    val path = "C:\\Games\\test"//reader.use { it.readLine() }
    val repository = FileRepositoryBuilder().setGitDir(File("$path\\.git")).build()
    val git = Git(repository)
    val branch =
        git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().find { it.name == "refs/remotes/origin/master" }
    val commit = RevWalk(repository).parseCommit(branch!!.objectId)
    val prev = commit.getParent(0)
    val diffFormatter = DiffFormatter(System.out)
    diffFormatter.setRepository(repository)
    val scan = diffFormatter.scan(prev, commit)
    val version = FileReader(File(path + File.separator + "Optional/version")).use { it.readText() }
    val addFiles = mutableListOf<String>()
    val removeFiles = mutableListOf<String>()
    val moveFiles = mutableListOf<MoveFile>()
    val gameUpdate =
        GameUpdate(version = version, addFiles = addFiles, moveFiles = moveFiles, removeFiles = removeFiles)
    scan.forEach {
        if (it.newPath != "mge3/MGE.ini" && it.newPath != "Morrowind.ini" && it.newPath != ".gitignore") {
            when (it.changeType) {
                DiffEntry.ChangeType.ADD -> addFiles.add(it.newPath)
                DiffEntry.ChangeType.DELETE -> removeFiles.add(it.oldPath)
                DiffEntry.ChangeType.MODIFY -> addFiles.add(it.newPath)
                DiffEntry.ChangeType.RENAME -> moveFiles.add(MoveFile(it.oldPath, it.newPath))
                DiffEntry.ChangeType.COPY -> addFiles.add(it.newPath)
                else -> println(it.newPath)
            }
        }
    }
    val tempDirectory = Files.createTempDirectory("mfr_patch")
    val updateFile = File("${tempDirectory.toFile().absolutePath}${File.separator}${GameUpdate.FILE_NAME}")
    FileWriter(updateFile).use { it.write(ObjectMapper().writeValueAsString(gameUpdate)) }
    val patchFile = File("${tempDirectory.toFile().absolutePath}${File.separator}patch")
    addFiles.forEach {
        val source = File("$path${File.separator}$it")
        val target = File("${patchFile.absolutePath}${File.separator}$it")
        if (target.parentFile.exists().not()) {
            target.parentFile.mkdirs()
        }
        Files.copy(source.toPath(), target.toPath())
    }
    val archive = File(version)
    ZipOutputStream(FileOutputStream(archive)).use { zipOutputStream ->
        tempDirectory.toFile().listAllFiles().forEach {
            zipOutputStream.putNextEntry(ZipEntry(it.absolutePath.removePrefix("${tempDirectory.toFile().absolutePath}${File.separator}")))
            zipOutputStream.write(FileInputStream(it).use { fileInputStream -> fileInputStream.readAllBytes() })
            zipOutputStream.closeEntry()
        }
    }
    tempDirectory.toFile().deleteRecursively()
}

fun File.listAllFiles(): List<File> {
    val result = mutableListOf<File>()
    Files.walk(toPath()).sorted(Comparator.naturalOrder())
        .forEach { path ->
            val file = path.toFile()
            if (file.isFile) {
                result.add(file)
            }
        }
    return result
}