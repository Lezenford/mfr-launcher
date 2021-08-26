package ru.fullrest.mfr.common.api.tcp

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "id"
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = DownloadRequest::class, name = "1"),
        JsonSubTypes.Type(value = DownloadResponse::class, name = "2"),
        JsonSubTypes.Type(value = EndSession::class, name = "3"),
        JsonSubTypes.Type(value = ServerException::class, name = "4"),
        JsonSubTypes.Type(value = DownloadLauncherRequest::class, name = "5"),
    ]
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
abstract class Message