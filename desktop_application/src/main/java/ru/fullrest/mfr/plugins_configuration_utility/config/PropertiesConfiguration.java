package ru.fullrest.mfr.plugins_configuration_utility.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Setter
@Configuration
@PropertySource("classpath:base.properties")
public class PropertiesConfiguration {

    private String gamePath;

    private String gameVersion;

    private boolean extendedMod;

    private boolean refreshSchema;

    private boolean refreshApplies;

    @Value("${forum_link}")
    private String forumLink;

    @Value("${morrowind}")
    private String morrowind_exe;

    @Value("${launcher}")
    private String launcher_exe;

    @Value("${mcp}")
    private String mcp_exe;

    @Value("${mge}")
    private String mge_exe;

    @Value("${readme}")
    private String readme;

    @Value("${optional}")
    private String optional;

    @Value("${version_file}")
    private String versionFileName;

    @Value("${schema_file}")
    private String schemaFileName;

    @Value("${schema_new_file}")
    private String newSchemaFileName;

    @Value("${application_version}")
    private String applicationVersion;

    @Value("${platform}")
    private String platform;

    @Value("${game_platform}")
    private String gamePlatform;

    @Value("${update_link}")
    private String updateLink;
}