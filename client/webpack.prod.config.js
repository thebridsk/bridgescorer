var webpack = require('webpack');

// Load the config generated by scalajs-bundler
var config = require('./scalajs.webpack.config');

config.plugins = [
  new webpack.DefinePlugin({
    'process.env': {
      NODE_ENV: JSON.stringify('production')
    }
  }),

  new webpack.optimize.UglifyJsPlugin(),

]

module.exports = config;
