# Assets

### Generation
Assets in `generated/` are generated from scratch / from assets in `raw/` by the `generateAssets` gradle task.  
**`generateAssets` only works on Windows.** To support building on other systems, generated assets are Git-tracked.

### Deploy
All assets are "deployed" by the `deployAssets` gradle task:
- Merge files in `static/` and `generated/` into `all/`
- Copy `all/` into `/src/jvmMain/resources` and `/src/jsMain/resources`

`deployAssets` is automatically ran when running / building.

### Credits
- [msdf-atlas-gen.exe](tools/bin/msdf-atlas-gen.exe): https://github.com/Chlumsky/msdf-atlas-gen
- [Planetary Texture Map Sources 1.pdf](Planetary%20Texture%20Map%20Sources%201.pdf): [ProximaCc YouTube](https://www.youtube.com/watch?v=OEmpYnjuFVc)
- [Planetary Texture Map Sources 2.pdf](Planetary%20Texture%20Map%20Sources%202.pdf): [ProximaCc YouTube](https://www.youtube.com/watch?v=rLBfHBFfQfo)
- Celestial body textures: see [this markdown](raw/textures/celestial_body/README.md)
- [starmap_2020_16k.exr](raw/textures/misc/starmap_2020_16k.exr): https://svs.gsfc.nasa.gov/4851/