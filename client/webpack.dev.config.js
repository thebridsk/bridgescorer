var webpack = require('webpack');

// Load the config generated by scalajs-bundler
var config = require('./scalajs.webpack.config');

config.optimization = config.optimization || {}
config.optimization.minimize = false
config.mode = 'development'

module.exports = config;
