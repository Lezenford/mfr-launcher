package com.lezenford.mfr.launcher.extension

import com.lezenford.mfr.schema.v1.Schema
import com.lezenford.mfr.common.protocol.file.Schema as OldSchema

fun Schema.toOldSchema(): OldSchema = com.lezenford.mfr.common.protocol.file.Schema(
    packages = optionsList.map { option ->
        OldSchema.Package(
            name = option.name,
            options = option.contentsList.map { content ->
                OldSchema.Package.Option(
                    name = content.name,
                    description = content.description,
                    image = content.picturePath.takeIf { content.hasPicturePath() },
                    items = content.partition.filesList.map { partition ->
                        OldSchema.Package.Option.Item(
                            storagePath = partition.mainPath,
                            gamePath = partition.optionalPath,
                            md5 = partition.sha256()
                        )
                    }
                )
            }
        )
    }
)