config.optimization = Object.assign(
    {},
    config.optimization || {},
    {minimize: false}
)

config.performance = Object.assign(
    {},
    config.performance || {},
    {maxEntrypointSize: 2147483647, maxAssetSize: 2147483647}
)