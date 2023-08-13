rootProject.name = "BattleCards"

include(":battlecards")
project(":battlecards").projectDir = rootDir.resolve("plugin")

listOf("api", "abstract").forEach {
    include(":battlecards-$it")
    project(":battlecards-$it").projectDir = rootDir.resolve(it)
}

listOf(
    "1_8_R1",
    "1_8_R2",
    "1_8_R3",
    "1_9_R1",
    "1_9_R2",
    "1_10_R1",
    "1_11_R1",
    "1_12_R1",
    "1_13_R1",
    "1_13_R2",
    "1_14_R1",
    "1_15_R1",
    "1_16_R1",
    "1_16_R2",
    "1_16_R3",
    "1_17_R1",
    "1_18_R1",
    "1_18_R2",
    "1_19_R1",
    "1_19_R2",
    "1_19_R3",
    "1_20_R1"
).forEach {
    include(":battlecards-$it")
    project(":battlecards-$it").projectDir = rootDir.resolve("nms/$it")
}