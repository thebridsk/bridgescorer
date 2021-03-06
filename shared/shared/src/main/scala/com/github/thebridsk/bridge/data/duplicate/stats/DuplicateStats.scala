package com.github.thebridsk.bridge.data.duplicate.stats

case class DuplicateStats(
    playerStats: Option[PlayerStats],
    contractStats: Option[ContractStats],
    playerDoubledStats: Option[PlayerStats],
    comparisonStats: Option[PlayerComparisonStats],
    playersOpponentsStats: Option[PlayersOpponentsStats],
    playerPlacesStats: Option[PlayerPlaces]
) {

  def withPlayerStats(ps: PlayerStats): DuplicateStats =
    copy(playerStats = Some(ps))
  def withContractStats(cs: ContractStats): DuplicateStats =
    copy(contractStats = Some(cs))
  def withPlayerDoubledStats(pds: PlayerStats): DuplicateStats =
    copy(playerDoubledStats = Some(pds))
  def withComparisonStats(cs: PlayerComparisonStats): DuplicateStats =
    copy(comparisonStats = Some(cs))
  def withPlayersOpponentsStats(cs: PlayersOpponentsStats): DuplicateStats =
    copy(playersOpponentsStats = Some(cs))
  def withPlayerPlacesStats(cs: PlayerPlaces): DuplicateStats =
    copy(playerPlacesStats = Some(cs))

  def update(ds: DuplicateStats): DuplicateStats =
    copy(
      playerStats = ds.playerStats.orElse(playerStats),
      contractStats = ds.contractStats.orElse(contractStats),
      playerDoubledStats = ds.playerDoubledStats.orElse(playerDoubledStats),
      comparisonStats = ds.comparisonStats.orElse(comparisonStats),
      playersOpponentsStats =
        ds.playersOpponentsStats.orElse(playersOpponentsStats),
      playerPlacesStats = ds.playerPlacesStats.orElse(playerPlacesStats)
    )
}
