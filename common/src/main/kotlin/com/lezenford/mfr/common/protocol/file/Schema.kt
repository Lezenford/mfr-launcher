package com.lezenford.mfr.common.protocol.file

data class Schema(
    val packages: List<Package>,
    val extra: List<Extra> = emptyList()
) {

    data class Extra(
        val name: String,
        val items: List<Item>
    ) {
        data class Item(
            val path: String,
            val md5: ByteArray
        )
    }

    data class Package(
        val name: String,
        val options: List<Option>
    ) {
        data class Option(
            val name: String,
            val description: String,
            val image: String?,
            val items: List<Item>
        ) {
            data class Item(
                val storagePath: String,
                val gamePath: String,
                val md5: ByteArray
            )
        }
    }
}