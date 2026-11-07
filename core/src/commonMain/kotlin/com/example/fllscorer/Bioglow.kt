package com.example.fllscorer

/**
 * All 15 BIOGLOW (FIRST LEGO League 2026-27) missions, based on the official
 * Robot Game Rulebook (V1, 30082603).
 */

data class ScoringOption(
    /** Stable id used in the scorer. */
    val id: String,
    /** What has to be true. */
    val label: String,
    /** Points when satisfied (for a single count of one). */
    val points: Int,
    /** Max items countable (1 = simple yes/no check). */
    val maxCount: Int = 1,
    /** Label shown per item when maxCount > 1, e.g. "per seed". */
    val unitLabel: String? = null,
    /** True when this option must be re-checked by hand ("bonus" rows). */
    val isBonus: Boolean = false,
)

data class Mission(
    val number: Int,
    val name: String,
    val bags: List<Int>,
    val description: String,
    val noEquipmentConstraint: Boolean,
    val notes: List<String>,
    val scoring: List<ScoringOption>,
    /** Official maximum when lower than the naive sum of all options. */
    val maxOverride: Int? = null,
) {
    val maxPoints: Int =
        maxOverride ?: scoring.sumOf { o -> o.points * (if (o.points > 0) o.maxCount else 0) }
    val symbol: String = when (number) {
        1 -> "\\\\"; 2 -> "||"; 3 -> "><"; 4 -> "11"; 5 -> "--"
        6 -> "AA"; 7 -> "~~"; 8 -> "%("; 9 -> "[]"; 10 -> "!!"
        11 -> "%%"; 12 -> "\\\\"; 13 -> "&&"; 14 -> "++"; 15 -> "=="
        else -> "??"
    }
}

/**
 * All 5 BIOGLOW Future Edition missions (+ optional Level Up challenge),
 * based on the official Future Edition 8+ Game Missions and Rulebook (2026-27).
 * The Future Edition game is played on a SHARED field by two teams with four
 * player roles: Driver, Operator, Technician, Specialist.
 */
object BioglowFuture {
    const val inspectionBonus = 0 // no inspection bonus in Future Edition

    val missions: List<Mission> = listOf(
        Mission(
            number = 1,
            name = "Mighty Microbiomes",
            bags = listOf(),
            description = "Introduce keystone species into the young forest, grand tree, " +
                "or hollow tree microbiomes to stabilize the threatened ecosystem.",
            noEquipmentConstraint = true,
            notes = listOf(
                "Maximum 3 keystone species score in EACH microbiome (9 total can score for placement).",
                "The 4th keystone in the young forest does NOT score for placement — it triggers " +
                    "the defeat of the invasive queen for the +40 bonus.",
                "All keystone species must be placed loose in the microbiome (not touching equipment).",
            ),
            scoring = listOf(
                ScoringOption("f1.young", "Keystone species in the YOUNG FOREST microbiome", 20, maxCount = 3, unitLabel = "each"),
                ScoringOption("f1.tree", "Keystone species in the GRAND TREE microbiome", 20, maxCount = 3, unitLabel = "each"),
                ScoringOption("f1.hollow", "Keystone species in the HOLLOW TREE microbiome", 20, maxCount = 3, unitLabel = "each"),
                ScoringOption("f1.bonus", "Bonus: 3+ keystone in young forest AND the invasive queen on the opposite side is knocked down", 40, isBonus = true),
            ),
        ),
        Mission(
            number = 2,
            name = "Roots of Renewal",
            bags = listOf(),
            description = "The grand tree has drawn resources from the forest floor for " +
                "hundreds of years. Deliver resources to the grand tree base so they can " +
                "be cycled up to the canopy chamber.",
            noEquipmentConstraint = false,
            notes = listOf(
                "Resources MUST be cycled up the tree — placing them directly into the canopy " +
                    "chamber does not count.",
                "Bags 1-4 build the grand tree; bags 8-11 build the young forest.",
            ),
            scoring = listOf(
                ScoringOption("f2.base", "Resources in the grand tree base", 5, maxCount = 15, unitLabel = "each"),
                ScoringOption("f2.canopy", "Resources carried up into the canopy chamber", 10, maxCount = 15, unitLabel = "each"),
            ),
        ),
        Mission(
            number = 3,
            name = "Cave Waterfall",
            bags = listOf(),
            description = "A hidden cave waterfall sustains the ecosystem. Deliver keystone " +
                "species to the waterfall to recharge and repopulate them.",
            noEquipmentConstraint = false,
            notes = listOf(
                "Species cycled through the waterfall advance a mechanical counter.",
                "You may NOT advance the counter any other way.",
                "Score is read from the counter display at the END of the match (max 50).",
                "Strategic pacing of keystone species through the waterfall is encouraged.",
            ),
            scoring = listOf(
                ScoringOption(
                    "f3.counter",
                    "Keystone species cycled through the cave waterfall (counter display)",
                    5, maxCount = 50, unitLabel = "each",
                ),
            ),
        ),
        Mission(
            number = 4,
            name = "Rainforest Awakening",
            bags = listOf(),
            description = "Hidden stashes of keystone species and resources are buried in " +
                "the dense terrain. Release them to help the rainforest grow healthy and strong.",
            noEquipmentConstraint = false,
            notes = listOf(
                "Bags 16-17 build the hive; the hollow tree is mission model bags 12-15 " +
                    "(Young Forest Microbiome bags 8-11).",
            ),
            scoring = listOf(
                ScoringOption("f4.nest", "All keystone species released from the nest", 30),
                ScoringOption("f4.hollow", "All resources released from the hollow tree", 20),
            ),
        ),
        Mission(
            number = 5,
            name = "Central Haven",
            bags = listOf(),
            description = "The central haven is a wildlife refuge. Work with the other team " +
                "to deliver as many keystone species and resources as you can.",
            noEquipmentConstraint = false,
            notes = listOf(
                "COOPERATIVE mission — scores for BOTH teams at the end of the match.",
                "Any keystone species or resource resting completely in the haven scores, " +
                    "regardless of which team delivered it.",
                "Both teams receive the full shared score. Rewards end-of-match planning!",
            ),
            scoring = listOf(
                ScoringOption(
                    "f5.haven",
                    "Keystone species or resources resting completely in the central haven",
                    5, maxCount = 50, unitLabel = "each",
                ),
            ),
        ),
        Mission(
            number = 6,
            name = "Invasive Attack (Level Up)",
            bags = listOf(),
            description = "Boost the difficulty! Strategically place invasive species during " +
                "pre-match setup, then earn bonus points by removing and delivering them to " +
                "the containment zone.",
            noEquipmentConstraint = false,
            notes = listOf(
                "OPTIONAL challenge — your team decides how many invasive species to add.",
                "The invasive queen is required and never scores pre-match points, but counts " +
                    "for delivery to the containment zone.",
                "Placement options: Grand Tree & Nest, or Young Forest & Grand Tree.",
            ),
            scoring = listOf(
                ScoringOption("f6.added", "Invasive species added in pre-match setup", 20, maxCount = 5, unitLabel = "each"),
                ScoringOption(
                    "f6.zone",
                    "Invasive species delivered to the containment zone",
                    10, maxCount = 6, unitLabel = "each", isBonus = true,
                ),
            ),
        ),
    )

    val totalMaxPoints: Int = missions.sumOf { it.maxPoints }
}

data class TokenRow(val tokensLeft: Int, val points: Int)

/** A loose game item / token (not a fixed mission model). */
data class GameItem(
    val name: String,
    /** What the piece physically is / comes from. */
    val origin: String,
    /** Which missions use it. */
    val usedBy: List<Int>,
    /** Rules to remember about the piece. */
    val notes: List<String>,
    /** How many exist, if officially stated; null = not stated in rulebook. */
    val quantity: String?,
    /** Which official model render to show; use mNN (founders) or fNN (future). */
    val imageTag: String? = null,
)

/** Founders Edition loose items and tokens. */
val foundersItems: List<GameItem> = listOf(
    GameItem(
        name = "Seeds",
        origin = "Bag 3 (on the seed pod stalk) + the loose seed from Bag 15 (research platform tree); loose planting seeds for Bag 21.",
        usedBy = listOf(2, 9, 14),
        notes = listOf(
            "Seeds that are no longer touching the stalk score 10 EACH in Mission 02.",
            "Keep them! They can be planted later: 5 each in the replantation station (+5 each if also touching the mat).",
        ),
        quantity = "set count per Challenge Set",
        imageTag = "m02",
    ),
    GameItem(
        name = "LiDAR map & scan marker",
        origin = "Bag 1-2 (Drone Survey mission)",
        usedBy = listOf(1),
        notes = listOf(
            "Bonus requires the map COMPLETELY flipped AND the scan marker at least partly in the survey area.",
            "No-equipment mission — don't leave equipment touching it.",
        ),
        quantity = null,
        imageTag = "m01",
    ),
    GameItem(
        name = "Rock",
        origin = "Bag 4-5 (Flip the Rock mission)",
        usedBy = listOf(3),
        notes = listOf(
            "Flag down earns 20; the rock must be returned to its ORIGINAL starting position for the +10 bonus.",
        ),
        quantity = null,
        imageTag = "m03",
    ),
    GameItem(
        name = "Leaves & katydid",
        origin = "Bag 6 (lucky leaves habitat)",
        usedBy = listOf(4),
        notes = listOf(
            "Katydid knock-out = ZERO for the mission. Leaves: 10 then 20 bonus.",
            "The katydid's starting position is randomized before each match.",
        ),
        quantity = "2 leaves, 1 katydid",
        imageTag = "m04",
    ),
    GameItem(
        name = "Ant & leaf fragments",
        origin = "Bag 8-10 (Leafcutter Frenzy mission)",
        usedBy = listOf(6),
        notes = listOf(
            "Score 10 PER FRAGMENT when the ant is touching the nest and fragments stay contained within it.",
        ),
        quantity = null,
        imageTag = "m06",
    ),
    GameItem(
        name = "Mycelium & vine",
        origin = "Bag 10 (Humongous Fungus), Bags 11-15 (Tangled tree)",
        usedBy = listOf(7, 8),
        notes = listOf(
            "Mycelium: 20 when fully extended; 10 per connection with the opposing team's fully extended root (both teams can score — Coopertition!).",
            "Vine touching the mat = 30 points.",
        ),
        quantity = null,
        imageTag = "m07",
    ),
    GameItem(
        name = "Camera trap, spider & snail habitats",
        origin = "Bags 11-15 (research platform), Bag 16 (fragile microhabitats)",
        usedBy = listOf(9, 10),
        notes = listOf(
            "Platform up + camera deployed + seed off tree = 30 total.",
            "Spider & snail habitats untouched in original position = 20 total. LEAVE THEM ALONE.",
        ),
        quantity = null,
        imageTag = "m09",
    ),
    GameItem(
        name = "Cane & support tie",
        origin = "Bag 18 (Forest Elder mission)",
        usedBy = listOf(12),
        notes = listOf(
            "Cane fully raised touching the tree = 20; support tie around the post = 10.",
            "No-equipment mission.",
        ),
        quantity = null,
        imageTag = "m12",
    ),
    GameItem(
        name = "Keystone species (bag 20 bricks)",
        origin = "Bag 19-20 — YOUR team designs and builds it",
        usedBy = listOf(13),
        notes = listOf(
            "Counts as EQUIPMENT — it must fit your inspection footprint.",
            "You must identify it at inspection. On the restoration platform + young trees raised = 30.",
        ),
        quantity = "1, team-built",
        imageTag = "m13",
    ),
    GameItem(
        name = "Dock system models",
        origin = "Bags 13-15, 19-21, 22-24 — interchangeable docks: mine / city / farm",
        usedBy = listOf(13, 14, 15),
        notes = listOf(
            "You choose which model sits on each dock before the match — placement is strategy!",
            "Environmental bonus depends on WHERE the model sits: mine→canopy, city→skylight, farm→hatch.",
        ),
        quantity = null,
        imageTag = "m15",
    ),
)

/** Future Edition loose items and tokens. */
val futureItems: List<GameItem> = listOf(
    GameItem(
        name = "Keystone species tokens",
        origin = "LEGO set + team-placed; small species figures your Operator and Technician place",
        usedBy = listOf(1, 3, 4, 5),
        notes = listOf(
            "20 points each in a microbiome (max 3 per microbiome: young forest / grand tree / hollow tree).",
            "The 4th keystone in the young forest has NO placement score — it is used to knock down the invasive queen (+40 bonus).",
            "Must rest loose in the microbiome — touching equipment does NOT score.",
            "Cycle through the cave waterfall (5 each, counter-driven) or deliver to the central haven (5 each, shared).",
        ),
        quantity = null,
        imageTag = "f03",
    ),
    GameItem(
        name = "Resource tokens",
        origin = "LEGO set; load into the grand tree base or canopy chamber, release from hollow tree, or send to the haven",
        usedBy = listOf(2, 4, 5),
        notes = listOf(
            "5 each in the grand tree base; 10 each when cycled UP the tree into the canopy chamber (max 15).",
            "Placing resources DIRECTLY into the canopy chamber does not count — they must cycle through the mechanism!",
            "All released from the hollow tree = 20; shared haven: 5 each.",
        ),
        quantity = null,
        imageTag = "f02",
    ),
    GameItem(
        name = "Invasive species + invasive queen",
        origin = "LEGO set; placement options: Tree & Nest, or Young Forest & Grand Tree",
        usedBy = listOf(1, 6),
        notes = listOf(
            "OPTIONAL Level Up challenge: 20 points each species YOU choose to add (max 5) — more species = more risk both directions.",
            "The queen is REQUIRED and never scores pre-match — but she can be delivered to the containment zone for 10.",
            "Knock the queen down (with the 4th young-forest keystone) = +40.",
        ),
        quantity = "queen required, up to 5 others added",
        imageTag = "f06",
    ),
    GameItem(
        name = "Connection cards",
        origin = "LEGO Education set cards — color-coded pairing cards for hardware pairing",
        usedBy = listOf(),
        notes = listOf(
            "1 card is REQUIRED and pairs the hive single motor with the young forest color sensor; the referee HOLDS it during the match.",
            "Up to 3 more cards optional for team use (driving base / grand tree).",
            "Hardware limit: 2 controllers, 2 double motors, 2 single motors, 2 color sensors total.",
        ),
        quantity = "1 required + up to 3 optional",
        imageTag = "f05",
    ),
    GameItem(
        name = "Required field hardware",
        origin = "Embedded in models: double motor (grand tree), color sensor (young forest), single motor (hive)",
        usedBy = listOf(2),
        notes = listOf(
            "Counts toward your equipment limits — plan around it!",
            "Everything must be fully charged with NO attachments for inspection.",
        ),
        quantity = "3 pieces",
        imageTag = "f02",
    ),
    GameItem(
        name = "Game / drone tokens",
        origin = "Various mission models on the shared field",
        usedBy = listOf(),
        notes = listOf(
            "The Specialist can return tokens to play via the token recycler when they rest in the Specialist area.",
            "Any token fully resting in the Central Haven scores 5 for BOTH teams, whoever delivered it.",
        ),
        quantity = null,
        imageTag = "f05",
    ),
)

object Bioglow {
    /** Official Precision Token values (Rulebook p.8 / Rule 17). */
    val precisionTokens: List<TokenRow> = listOf(
        TokenRow(1, 10), TokenRow(2, 15), TokenRow(3, 25),
        TokenRow(4, 35), TokenRow(5, 50), TokenRow(6, 50),
    )

    const val inspectionBonus = 20

    val missions: List<Mission> = listOf(
        Mission(
            number = 1,
            name = "Drone Survey",
            bags = listOf(1, 2),
            description = "High above the canopy, drones survey the landscape and " +
                "transmit data back in real time. Help the pilot launch the drone " +
                "to create a LiDAR map of this rainforest ecosystem.",
            noEquipmentConstraint = true,
            notes = listOf(
                "The drone must not be touching the mat at the end of the match.",
                "For the bonus, the LiDAR map must be completely flipped over AND the " +
                    "scan marker must be at least partly in the survey area.",
                "This mission has a No Equipment Constraint: the mission model cannot " +
                    "earn points if it is touching equipment at the end of the match.",
            ),
            scoring = listOf(
                ScoringOption("m1.drone", "Drone is no longer touching the mat", 20),
                ScoringOption("m1.map", "Bonus: LiDAR map completely flipped, scan marker partly in the survey area", 10, isBonus = true),
            ),
        ),
        Mission(
            number = 2,
            name = "Exploding Seeds",
            bags = listOf(3),
            description = "Some trees use a unique strategy to disperse their seeds. " +
                "Release the seeds from the seed pod, propelling them to new ground.",
            noEquipmentConstraint = false,
            notes = listOf(
                "Keep the seeds — Mission 14 (Seeds of Renewal) can use them later.",
            ),
            scoring = listOf(
                ScoringOption("m2.seeds", "Seeds no longer touching the stalk", 10, maxCount = 3, unitLabel = "each"),
            ),
        ),
        Mission(
            number = 3,
            name = "Flip the Rock",
            bags = listOf(4, 5),
            description = "Many creatures thrive just below the surface. Flip the rock " +
                "to discover what's living underneath — but leave no trace by placing " +
                "it back the way you found it.",
            noEquipmentConstraint = false,
            notes = listOf(
                "The rock must be returned to its ORIGINAL starting position for the bonus.",
                "Knock the research flag down first; the flag is the easier part.",
            ),
            scoring = listOf(
                ScoringOption("m3.flag", "The research flag is down", 20),
                ScoringOption("m3.rock", "Bonus: and the rock has been returned to its original starting position", 10, isBonus = true),
            ),
        ),
        Mission(
            number = 4,
            name = "Lucky Leaves",
            bags = listOf(6),
            description = "Hidden in plain sight, the katydid camouflages with its leafy " +
                "surroundings. Collect as many leaves as you can without disturbing " +
                "this master of disguise.",
            noEquipmentConstraint = true,
            notes = listOf(
                "The nest is the mission model that holds the leaves and the katydid.",
                "If the katydid is completely removed from the nest at ANY time, the bonus is lost.",
                "If the katydid is outside the leaf habitat at the end of the match — even partly — " +
                    "the team scores ZERO for this mission. The leaf habitat includes the nest " +
                    "and the marked area on the mat surrounding it.",
                "The katydid's starting position is randomized — don't build a strategy for only one setup.",
            ),
            scoring = listOf(
                ScoringOption("m4.leaf1", "First leaf completely removed, no longer touching the nest", 10),
                ScoringOption("m4.leaf2", "Bonus: second leaf also removed, with the katydid in its original starting position", 20, isBonus = true),
            ),
        ),
        Mission(
            number = 5,
            name = "Reaching Roots",
            maxOverride = 20,
            bags = listOf(7),
            description = "Rainforest plants stretch their roots far and wide, reaching " +
                "resources and forming connections with nearby organisms. Help this " +
                "plant cross the forest boundary in search of symbiosis.",
            noEquipmentConstraint = false,
            notes = listOf(
                "Only one scoring condition is possible for this mission.",
                "A completely extended root also helps unlock the Coopertition bonus in Mission 07.",
            ),
            scoring = listOf(
                ScoringOption("m5.partial", "Plant root partially extended", 10),
                ScoringOption("m5.full", "Plant root completely extended", 20),
            ),
        ),
        Mission(
            number = 6,
            name = "Leafcutter Frenzy",
            bags = listOf(8, 9, 10),
            description = "Leafcutter ants work together to feed an underground fungus " +
                "garden. Guide the ants back to their nest — but move too quickly, and " +
                "you may scatter the leaves they collected.",
            noEquipmentConstraint = false,
            notes = listOf(
                "This mission rewards control more than speed.",
                "Both conditions must hold per fragment: the ant touching the nest AND the " +
                    "leaf fragments contained within the nest.",
            ),
            scoring = listOf(
                ScoringOption(
                    "m6.fragments",
                    "Ant is touching the nest and the leaf fragments are contained within the nest",
                        10, maxCount = 3, unitLabel = "per fragment",
                ),
            ),
        ),
        Mission(
            number = 7,
            name = "Humongous Fungus",
            bags = listOf(10),
            description = "Mycelium is the vast, underground network of fungus beneath " +
                "the rainforest floor. Form a new connection between the mycelium and " +
                "a neighboring tree root.",
            noEquipmentConstraint = true,
            notes = listOf(
                "This is the COOPERTITION mission — talk to the other team before the match!",
                "The mycelium and plant root do NOT need to touch to form a connection. " +
                    "If each is fully extended, both teams earn the bonus.",
                "Two bonuses are possible (one per team's extended root).",
                "Bonus is not possible in remote competitions or with no opposing team.",
            ),
            scoring = listOf(
                ScoringOption("m7.myc", "The mycelium is completely extended", 20),
                ScoringOption("m7.conn", "Bonus: connection between your extended mycelium and the opposing team's fully extended plant root", 10, maxCount = 2, unitLabel = "each", isBonus = true),
            ),
        ),
        Mission(
            number = 8,
            name = "Tangled",
            bags = listOf(11, 12, 13, 14, 15),
            description = "Climbing vines have wrapped tightly around this tree. Gently " +
                "untangle them so the tree has space and sunlight to grow again.",
            noEquipmentConstraint = false,
            notes = listOf(
                "Simple mission, no bonus — one of the first missions worth building for.",
            ),
            scoring = listOf(
                ScoringOption("m8.vine", "The vine is touching the mat", 30),
            ),
        ),
        Mission(
            number = 9,
            name = "Research Platform",
            bags = listOf(11, 12, 13, 14, 15),
            description = "Olivia and Michael are studying species that live in the " +
                "rainforest canopy with the help of their forest guide. Help the team " +
                "deploy equipment to monitor activity high above the forest floor.",
            noEquipmentConstraint = true,
            notes = listOf(
                "Three separate scoring actions on one mission.",
                "The seed from this mission can be used later in Mission 14.",
            ),
            scoring = listOf(
                ScoringOption("m9.platform", "The research platform is raised", 10),
                ScoringOption("m9.camera", "The camera trap is deployed", 10),
                ScoringOption("m9.seed", "The seed is no longer touching the tree", 10),
            ),
        ),
        Mission(
            number = 10,
            name = "Fragile Microhabitats",
            maxOverride = 20,
            bags = listOf(16),
            description = "Researchers must work carefully when exploring protected " +
                "ecosystems. Move through the area without damaging these tiny " +
                "creatures' homes.",
            noEquipmentConstraint = false,
            notes = listOf(
                "This mission rewards you for LEAVING THINGS ALONE — plan your routes around them.",
            ),
            scoring = listOf(
                ScoringOption("m10.spider", "The spider habitat is in its original starting position", 10),
                ScoringOption("m10.snail", "The snail habitat is in its original starting position", 10),
            ),
        ),
        Mission(
            number = 11,
            name = "Window to the Past",
            bags = listOf(17),
            description = "Roots help to tell the long history of the rainforest. Open " +
                "the root window and document what you see.",
            noEquipmentConstraint = false,
            notes = listOf(
                "No equipment restriction, which makes this useful to finish at the end of a run.",
            ),
            scoring = listOf(
                ScoringOption("m11.cover", "The root cover is down, touching the mat", 20),
            ),
        ),
        Mission(
            number = 12,
            name = "Forest Elder",
            bags = listOf(18),
            description = "Once a towering giant, this aging tree now bends beneath its " +
                "own weight. Add a support to stabilize the tree for years to come.",
            noEquipmentConstraint = true,
            notes = listOf(
                "The cane is the easier 20 points; treat the support tie as an extra.",
            ),
            scoring = listOf(
                ScoringOption("m12.cane", "The cane is completely raised, touching the tree", 20),
                ScoringOption("m12.tie", "The support tie is around the post", 10),
            ),
        ),
        Mission(
            number = 13,
            name = "Keystone Species",
            bags = listOf(19, 20),
            description = "Keystone species bring balance to their ecosystem and shape " +
                "the forest around them. Your team must build and deliver a keystone " +
                "species to this newly protected habitat.",
            noEquipmentConstraint = false,
            notes = listOf(
                "DOCK MISSION — use the bricks in bag 20 to build your own keystone species.",
                "Your keystone species counts as EQUIPMENT and is included in the pre-match " +
                    "inspection. Build it early and make sure it fits.",
                "You will be asked to identify your keystone species at inspection.",
            ),
            scoring = listOf(
                ScoringOption(
                    "m13.delivered",
                    "Your keystone species is on the restoration platform AND the young trees are raised",
                    30,
                ),
            ),
        ),
        Mission(
            number = 14,
            name = "Seeds of Renewal",
            bags = listOf(21),
            description = "A thriving rainforest relies on its plant diversity. Place " +
                "the seeds your team has collected in the soil and watch as they take root.",
            noEquipmentConstraint = false,
            notes = listOf(
                "DOCK MISSION — this is where the seeds from Missions 02 and 09 can be used.",
                "Bonus per seed: it must BOTH be contained in the replantation station AND " +
                    "touch the mat.",
                "Decide early whether your team wants to spend time collecting and planting seeds.",
            ),
            scoring = listOf(
                ScoringOption(
                    "m14.seeds",
                    "Seeds contained within the replantation station",
                    5, maxCount = 6, unitLabel = "each",
                ),
                ScoringOption(
                    "m14.matBonus",
                    "Bonus: and are touching the mat",
                    5, maxCount = 6, unitLabel = "each", isBonus = true,
                ),
            ),
        ),
        Mission(
            number = 15,
            name = "Biocentric Architecture",
            maxOverride = 40,
            bags = listOf(22, 23, 24),
            description = "Thoughtful design creates harmony between humans and the " +
                "natural world. Modify the building to create homes for wildlife, " +
                "restore soil health, and bring natural light to green spaces.",
            noEquipmentConstraint = true,
            notes = listOf(
                "DOCK MISSION — three upgrades, plus one Environmental Bonus.",
                "Only ONE bonus is possible for this mission.",
                "Environmental Bonus: the greatest ecological need depends on where the model is docked — " +
                    "mine dock = nesting canopy, city dock = garden skylight, farm dock = compost hatch.",
                "The dock matters for the bonus: test all three upgrades and find the one your robot " +
                    "can do most reliably.",
            ),
            scoring = listOf(
                ScoringOption("m15.canopy", "The nesting canopy is raised", 10),
                ScoringOption("m15.skylight", "The garden skylight is completely in", 10),
                ScoringOption("m15.compost", "The compost hatch is completely opened, touching the mat", 10),
                ScoringOption(
                    "m15.ebonus",
                    "Environmental Bonus: you completed the greatest ecological need " +
                        "(mine = canopy, city = skylight, farm = hatch)",
                    10, isBonus = true,
                ),
            ),
        ),
    )

    val totalMaxPoints: Int = missions.sumOf { it.maxPoints } + inspectionBonus + 50
}
