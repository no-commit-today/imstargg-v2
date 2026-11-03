package com.imstargg.core.worker.application.player

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceAutoRegistrar
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceChecker
import com.imstargg.core.worker.config.PlayerRefreshScheduleProperties
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import kotlin.random.Random

private val log = KotlinLogging.logger {}

@Service
class PlayerRefreshService(
    private val properties: PlayerRefreshScheduleProperties,
    private val playerRepository: PlayerJpaRepository,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerUpdater: PlayerUpdater,
    private val playerDeactivator: PlayerDeactivator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    fun refresh() {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            log.info { "[플레이어 갱신] API 점검 중으로 건너뜀" }
            return
        }

        val players = findRandomPlayers()
        log.debug { "[플레이어 갱신] ${players.size}명 플레이어 처리 시작: ${players.map { it.brawlStarsTag }}" }

        for (player in players) {
            try {
                val playerResponse = brawlStarsClient.getPlayerInformation(player.brawlStarsTag)
                playerUpdater.updatePlayer(playerResponse)
            } catch (ex: HttpClientErrorException.NotFound) {
                log.warn { "[플레이어 갱신] 플레이어 삭제됨 tag=${player.brawlStarsTag}" }
                playerDeactivator.deactivate(player.brawlStarsTag)
            } catch (ex: HttpServerErrorException.ServiceUnavailable) {
                log.warn(ex) { "[플레이어 갱신] API 점검 중 감지 tag=${player.brawlStarsTag}" }
                brawlStarsApiMaintenanceAutoRegistrar.register()
                break
            } catch (ex: RestClientResponseException) {
                log.warn(ex) { "[플레이어 갱신] API 오류 status=${ex.statusCode.value()} tag=${player.brawlStarsTag}" }
            } catch (ex: ResourceAccessException) {
                log.warn(ex) { "[플레이어 갱신] API 통신 실패 tag=${player.brawlStarsTag}" }
            } catch (ex: Exception) {
                log.error(ex) { "[플레이어 갱신] 처리 실패 tag=${player.brawlStarsTag}" }
            }
        }

        log.debug { "[플레이어 갱신] 처리 완료" }
    }

    private fun findRandomPlayers(): List<PlayerEntity> {
        val maxId = playerRepository.findTopByStatusOrderByIdDesc(PlayerStatus.ACTIVE)?.id ?: return emptyList()
        val randomId = Random.nextLong(1, maxId + 1)
        return playerRepository.findAllByStatusAndIdGreaterThanEqualOrderByIdAsc(
            status = PlayerStatus.ACTIVE,
            id = randomId,
            pageable = PageRequest.of(0, properties.playerCount),
        )
    }
}
