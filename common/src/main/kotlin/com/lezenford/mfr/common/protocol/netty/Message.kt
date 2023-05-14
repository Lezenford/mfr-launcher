package com.lezenford.mfr.common.protocol.netty

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "id"
)
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(value = RequestGameFilesMessage::class, name = "1"),
        JsonSubTypes.Type(value = UploadFileMessage::class, name = "2"),
        JsonSubTypes.Type(value = EndSessionMessage::class, name = "3"),
        JsonSubTypes.Type(value = ServerExceptionMessage::class, name = "4"),
        JsonSubTypes.Type(value = RequestLauncherFilesMessage::class, name = "5"),
        JsonSubTypes.Type(value = ServerMaintenanceMessage::class, name = "6"),
        JsonSubTypes.Type(value = RequestChangeState::class, name = "7"),
    ]
)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
abstract class Message