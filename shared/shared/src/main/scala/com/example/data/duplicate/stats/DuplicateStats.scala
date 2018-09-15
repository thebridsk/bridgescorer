package com.example.data.duplicate.stats

case class DuplicateStats(
    playerStats: Option[PlayerStats],
    contractStats: Option[ContractStats],
    playerDoubledStats: Option[PlayerStats],
    comparisonStats: Option[PlayerComparisonStats]
) {

  def withPlayerStats( ps: PlayerStats ) = copy( playerStats = Some(ps) )
  def withContractStats( cs: ContractStats ) = copy( contractStats = Some(cs) )
  def withPlayerDoubledStats( pds: PlayerStats ) = copy( playerDoubledStats = Some(pds) )
  def withComparisonStats( cs: PlayerComparisonStats ) = copy( comparisonStats = Some(cs) )

  def update( ds: DuplicateStats ) =
    copy(
        playerStats = ds.playerStats.orElse( playerStats ),
        contractStats = ds.contractStats.orElse(contractStats),
        playerDoubledStats = ds.playerDoubledStats.orElse(playerDoubledStats),
        comparisonStats = ds.comparisonStats.orElse(comparisonStats)
    )
}
