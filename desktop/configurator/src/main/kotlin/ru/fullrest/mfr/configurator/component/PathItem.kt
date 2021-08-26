package ru.fullrest.mfr.configurator.component

import javafx.scene.control.Label
import java.nio.file.Path
import kotlin.io.path.name

class FileItem(path: Path) : PathItem(path)
class FolderItem(path: Path) : PathItem(path)

sealed class PathItem(val path: Path) : Label(path.name)
