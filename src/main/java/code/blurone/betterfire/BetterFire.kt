package code.blurone.betterfire

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockSupport
import org.bukkit.block.data.type.Fire
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Creeper
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

@Suppress("unused")
class BetterFire : JavaPlugin(), Listener {
    private val removeArrowChance = config.getDouble("remove-arrow-chance", 100.0) * 0.01
    private val dropFlintChance = config.getDouble("drop-flint-chance", 80.0) * 0.01

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    private fun onProjectileHit(event: ProjectileHitEvent) {
        val arrow = event.entity as? AbstractArrow ?: return
        if (arrow.fireTicks <= 0) return

        val blockToLight = (event.hitBlock ?: return).getRelative(event.hitBlockFace!!)
        if (!Tag.REPLACEABLE.isTagged(blockToLight.type) || blockToLight.type == Material.WATER || blockToLight.type == Material.LAVA) return

        val belowBlock = blockToLight.getRelative(BlockFace.DOWN)
        val isSoulFire = Tag.SOUL_FIRE_BASE_BLOCKS.isTagged(belowBlock.type)
        blockToLight.type = if (isSoulFire) Material.SOUL_FIRE else Material.FIRE

        if (!isSoulFire && !belowBlock.type.isBurnable && !belowBlock.blockData.isFaceSturdy(BlockFace.UP, BlockSupport.CENTER) && blockToLight.blockData is Fire) {
            val blockData = (blockToLight.blockData as Fire)
            for (face in blockData.allowedFaces)
            {
                val type = blockToLight.getRelative(face).type
                logger.info("$type, ${type.isFlammable}, ${type.isBurnable}")
                blockData.setFace(face, type.isFlammable)
            }
            blockToLight.blockData = blockData
            blockToLight.state
        }

        if (arrow is Trident || Random.nextDouble() >= removeArrowChance) return

        if (Random.nextDouble() < dropFlintChance)
            arrow.world.dropItemNaturally(arrow.location, ItemStack(Material.FLINT))

        arrow.remove()
    }

    @EventHandler
    private fun onEntityCombust(event: EntityCombustEvent) {
        (event.entity as? Creeper)?.ignite()
    }
}
