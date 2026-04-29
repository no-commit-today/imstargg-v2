import type {
  BattleResponse,
  BattlePlayerResponse,
  BattlePlayerBrawlerResponse,
  BattleEventResponse,
} from "@/api/types";
import { pickName } from "@/domain/i18n";
import { toBrawler } from "@/domain/brawler/mappers";
import type {
  Battle,
  BattlePlayer,
  BattlePlayerBrawler,
  BattleEvent,
} from "@/domain/battle/types";

function toBattlePlayerBrawler(dto: BattlePlayerBrawlerResponse): BattlePlayerBrawler {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    power: dto.power,
    trophies: dto.trophies,
    trophyChange: dto.trophyChange,
  };
}

function toBattlePlayer(dto: BattlePlayerResponse): BattlePlayer {
  return {
    tag: dto.tag,
    name: dto.name,
    soloRankTier: dto.soloRankTier,
    brawler: toBattlePlayerBrawler(dto.brawler),
  };
}

function toBattleEvent(dto: BattleEventResponse): BattleEvent {
  return {
    id: dto.id,
    modeName: dto.mode ? pickName(dto.mode.name) : null,
    modeIconUrl: dto.mode ? dto.mode.iconImageUrl : null,
    modeResultType: dto.mode ? dto.mode.resultType : null,
    mapName: pickName(dto.mapName),
  };
}

export function toBattle(dto: BattleResponse): Battle {
  return {
    battleTime: dto.battleTime,
    event: dto.event ? toBattleEvent(dto.event) : null,
    mode: dto.mode,
    type: dto.type,
    result: dto.result,
    duration: dto.duration,
    starPlayerTag: dto.starPlayerTag,
    rank: dto.rank,
    trophyChange: dto.trophyChange,
    teams: dto.teams.map((team) => team.map(toBattlePlayer)),
    resultType: dto.rank != null ? "RANK" : "RESULT",
  };
}
