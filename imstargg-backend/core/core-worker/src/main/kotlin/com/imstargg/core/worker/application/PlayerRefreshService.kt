package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.core.worker.config.PlayerRefreshScheduleProperties
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class PlayerRefreshService(
    private val properties: PlayerRefreshScheduleProperties,
    private val playerRepository: PlayerJpaRepository,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerUpdateApplier: PlayerUpdateApplier,
    private val playerDeactivator: PlayerDeactivator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun refresh() {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            log.info("[플레이어 갱신] API 점검 중으로 건너뜀")
            return
        }

        val players = findRandomPlayers()
        log.info("[플레이어 갱신] {}명 플레이어 처리 시작: {}", players.size, players.map { it.brawlStarsTag })

        for (player in players) {
            try {
                val playerResponse = brawlStarsClient.getPlayerInformation(player.brawlStarsTag)
                playerUpdateApplier.apply(
                    player = player,
                    response = playerResponse,
                )
            } catch (ex: BrawlStarsApiClientException.NotFound) {
                log.warn("[플레이어 갱신] 플레이어 삭제됨 tag={}", player.brawlStarsTag)
                playerDeactivator.deactivate(player.brawlStarsTag)
            } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
                log.warn("[플레이어 갱신] API 점검 중 감지 tag={}", player.brawlStarsTag, ex)
                brawlStarsApiMaintenanceAutoRegistrar.register()
                break
            } catch (ex: BrawlStarsApiClientException) {
                log.warn("[플레이어 갱신] API 클라이언트 오류 tag={}", player.brawlStarsTag, ex)
            } catch (ex: BrawlStarsApiServerException) {
                log.warn("[플레이어 갱신] API 서버 오류 tag={}", player.brawlStarsTag, ex)
            } catch (ex: Exception) {
                log.error("[플레이어 갱신] 처리 실패 tag={}", player.brawlStarsTag, ex)
            }
        }

        log.info("[플레이어 갱신] 처리 완료")
    }

    private fun findRandomPlayers(): List<PlayerEntity> {
        val maxId = playerRepository.findTopByStatusOrderByIdDesc(PlayerStatus.ACTIVE)?.id ?: return emptyList()
        val randomId = Random.nextLong(1, maxId + 1)
        return playerRepository.findAllByStatusAndIdGreaterThanEqual(
            status = PlayerStatus.ACTIVE,
            id = randomId,
            pageable = PageRequest.of(0, properties.playerCount),
        )
    }
}
