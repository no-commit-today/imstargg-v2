package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.response.GadgetResponse
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.StarPowerResponse
import com.imstargg.core.admin.response.toNameMap
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Service
@Transactional(readOnly = true)
class BrawlerAdminService(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
    private val imageStorageService: ImageStorageService,
) {
    fun findAll(): List<BrawlerResponse> {
        val brawlers = brawlerJpaRepository.findAll()
        val messageCodes = brawlers.map { it.nameMessageCode() }
        val messagesByCode = messageJpaRepository.findAllByCodeIn(messageCodes).groupBy { it.code }
        val imagesByCode = imageJpaRepository.findAll().associateBy { it.code }

        return brawlers.map { brawler ->
            BrawlerResponse(
                brawler = brawler,
                names = toNameMap(messagesByCode[brawler.nameMessageCode()]),
                image = imagesByCode[brawler.portraitImageCode()],
            )
        }
    }

    fun findDetail(brawlStarsId: Long): BrawlerResponse? {
        val brawler = brawlerJpaRepository.findAll().find { it.brawlStarsId == brawlStarsId } ?: return null

        val starPowers = starPowerJpaRepository.findAll().filter { it.brawlerBrawlStarsId == brawlStarsId }
        val gadgets = gadgetJpaRepository.findAll().filter { it.brawlerBrawlStarsId == brawlStarsId }
        val brawlerGears = brawlerGearJpaRepository.findAllByBrawlerBrawlStarsId(brawlStarsId)
        val allGears = gearJpaRepository.findAll()

        val messageCodes =
            listOf(brawler.nameMessageCode()) +
                starPowers.map { it.nameMessageCode() } +
                gadgets.map { it.nameMessageCode() }
        val messagesByCode = messageJpaRepository.findAllByCodeIn(messageCodes).groupBy { it.code }
        val imagesByCode = imageJpaRepository.findAll().associateBy { it.code }

        return BrawlerResponse(
            brawler = brawler,
            names = toNameMap(messagesByCode[brawler.nameMessageCode()]),
            image = imagesByCode[brawler.portraitImageCode()],
            starPowers =
                starPowers.map { sp ->
                    StarPowerResponse(
                        starPower = sp,
                        names = toNameMap(messagesByCode[sp.nameMessageCode()]),
                        image = imagesByCode[sp.imageCode()],
                    )
                },
            gadgets =
                gadgets.map { gd ->
                    GadgetResponse(
                        gadget = gd,
                        names = toNameMap(messagesByCode[gd.nameMessageCode()]),
                        image = imagesByCode[gd.imageCode()],
                    )
                },
            linkedGearIds = brawlerGears.map { it.gearBrawlStarsId }.toSet(),
            allGears = allGears.map { GearResponse(gear = it, names = emptyMap(), image = null) },
        )
    }

    @Transactional
    fun create(
        brawlStarsId: Long,
        rarity: BrawlerRarity,
        role: BrawlerRole,
    ) {
        brawlerJpaRepository.save(BrawlerEntity(brawlStarsId, rarity, role))
    }

    @Transactional
    fun createStarPower(
        brawlStarsId: Long,
        brawlerBrawlStarsId: Long,
    ) {
        starPowerJpaRepository.save(StarPowerEntity(brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun createGadget(
        brawlStarsId: Long,
        brawlerBrawlStarsId: Long,
    ) {
        gadgetJpaRepository.save(GadgetEntity(brawlStarsId, brawlerBrawlStarsId))
    }

    @Transactional
    fun updateMessages(
        code: MessageCode,
        langMap: Map<Language, String>,
    ) {
        messageJpaRepository.deleteAllByCode(code)
        langMap.filter { it.value.isNotBlank() }.forEach { (lang, content) ->
            messageJpaRepository.save(MessageEntity(code, lang, content))
        }
    }

    @Transactional
    fun updateImage(
        code: BrawlStarsImageCode,
        inputStream: InputStream,
        contentType: String,
    ) {
        val key = code.value.replace(":", "/")
        imageStorageService.upload(key, inputStream, contentType)
        imageJpaRepository.deleteByCode(code)
        imageJpaRepository.save(BrawlStarsImageEntity(code, key))
    }

    @Transactional
    fun updateBrawlerGears(
        brawlerBrawlStarsId: Long,
        gearBrawlStarsIds: Set<Long>,
    ) {
        brawlerGearJpaRepository.deleteAllByBrawlerBrawlStarsId(brawlerBrawlStarsId)
        gearBrawlStarsIds.forEach { gearId ->
            brawlerGearJpaRepository.save(BrawlerGearEntity(brawlerBrawlStarsId, gearId))
        }
    }
}
