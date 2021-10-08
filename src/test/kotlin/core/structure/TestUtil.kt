package core.structure

import core.layer.immutable.material.*
import core.optics.AdachiBasedPermittivityModel

val layer1 = GaAs(d = 1.0, dampingFactor = 0.0, permittivityModel = AdachiBasedPermittivityModel.ADACHI_SIMPLE)
val layer2 = AlGaAs(d = 1.0, dampingFactor = 0.0, cAl = 0.3, permittivityModel = AdachiBasedPermittivityModel.ADACHI_SIMPLE)
val layer3 = GaN(d = 1.0)
val layer4 = AlGaN(d = 1.0, cAl = 0.3)

val blockWithZeroLayers = Block(repeat = 3, layers = listOf())
val blockWithSingleLayer = Block(repeat = 3, layers = listOf(layer1))
val blockWithMultipleLayers1 = Block(repeat = 3, layers = listOf(layer1, layer2))
val blockWithMultipleLayers2 = Block(repeat = 4, layers = listOf(layer3, layer4))

val structureWithZeroBlocks = Structure(blocks = listOf())
val structureWithSingleBlock = Structure(blocks = listOf(blockWithMultipleLayers1))
val structureWithMultipleBlocks = Structure(blocks = listOf(blockWithSingleLayer, blockWithMultipleLayers1, blockWithMultipleLayers2))
