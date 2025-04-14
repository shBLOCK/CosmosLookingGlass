// @see https://github.com/google/closure-compiler/issues/2776
// @see https://github.com/webpack/webpack/issues/238#issuecomment-174468364
// @see https://webpack.js.org/configuration/output/#output-devtoolfallbackmodulefilenametemplate
config.devtool = 'inline-source-map'
config.output.devtoolModuleFilenameTemplate = '[absolute-resource-path]'
config.output.devtoolFallbackModuleFilenameTemplate = '[absolute-resource-path]?[hash]'