package com.example.generator

import com.example.data.local.AnnouncementRuleEntity
import com.example.data.local.StudioConfigEntity
import com.example.data.local.WorldBossProfileEntity

object ForgeModCodeGenerator {
    fun generateAll(
        config: StudioConfigEntity,
        rules: List<AnnouncementRuleEntity>,
        bosses: List<WorldBossProfileEntity>
    ): List<GeneratedFile> = buildList {
        add(GasAndBuildTemplates.generateCodeGs(config, rules, bosses))
        add(ForgeServerTemplates.generateMainMod(config))
        add(ForgeServerTemplates.generateUniversalBossManager(config, bosses))
        add(ForgeServerTemplates.generateAnnouncementHandler(config, rules))
        add(ForgeServerTemplates.generateGasHotReloadManager(config))
        add(ForgeClientTemplates.generateModNetwork(config))
        addAll(ForgeClientTemplates.generatePackets(config))
        addAll(ForgeClientTemplates.generateClientOverlays(config))
        add(GasAndBuildTemplates.generateBuildGradle(config))
        add(GasAndBuildTemplates.generateModsToml(config))
        add(GasAndBuildTemplates.generateReadmeTh(config))
    }
}
